import express from 'express';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import dotenv from 'dotenv';
import { GoogleGenAI, Type } from '@google/genai';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PORT = Number(process.env.PORT) || 3000;

function getAiClient(): GoogleGenAI {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    throw new Error('GEMINI_API_KEY manquante côté serveur.');
  }
  return new GoogleGenAI({
    apiKey,
    httpOptions: {
      headers: {
        'User-Agent': 'aistudio-build',
      },
    },
  });
}

// Temporary in-memory relay for PWA Web Share Target POSTs
const sharedCaptureRelay = new Map<
  string,
  { mimeType: string; base64: string; fileName: string; createdAt: number }
>();

function cleanupRelay() {
  const now = Date.now();
  for (const [token, entry] of sharedCaptureRelay.entries()) {
    if (now - entry.createdAt > 5 * 60 * 1000) {
      sharedCaptureRelay.delete(token);
    }
  }
}

function parseMultipartScreenshot(
  rawBody: Buffer,
  contentType: string
): { mimeType: string; base64: string; fileName: string } | null {
  const boundaryMatch = contentType.match(/boundary=([^;]+)/i);
  if (!boundaryMatch) return null;
  const boundary = boundaryMatch[1].replace(/^"|"$/g, '');
  const boundaryBuf = Buffer.from(`--${boundary}`);
  let pos = rawBody.indexOf(boundaryBuf);
  while (pos !== -1) {
    const nextPos = rawBody.indexOf(boundaryBuf, pos + boundaryBuf.length);
    if (nextPos === -1) break;
    const part = rawBody.subarray(pos + boundaryBuf.length + 2, nextPos - 2);
    const headerEnd = part.indexOf(Buffer.from('\r\n\r\n'));
    if (headerEnd !== -1) {
      const headersStr = part.subarray(0, headerEnd).toString('utf8');
      const bodyBuf = part.subarray(headerEnd + 4);
      if (/name="screenshot"/i.test(headersStr)) {
        const mimeMatch = headersStr.match(/Content-Type:\s*([^\r\n]+)/i);
        const fileMatch = headersStr.match(/filename="([^"]+)"/i);
        const mimeType = mimeMatch ? mimeMatch[1].trim() : 'image/png';
        const fileName = fileMatch ? fileMatch[1].trim() : 'capture-partagee.png';
        return {
          mimeType,
          fileName,
          base64: bodyBuf.toString('base64'),
        };
      }
    }
    pos = nextPos;
  }
  return null;
}

async function startServer() {
  const app = express();

  // Web Share Target endpoint for PWA Android Chrome sharing
  app.post(
    '/api/share-target',
    express.raw({ type: 'multipart/form-data', limit: '15mb' }),
    (req, res) => {
      cleanupRelay();
      const contentType = req.headers['content-type'] || '';
      const parsed = Buffer.isBuffer(req.body)
        ? parseMultipartScreenshot(req.body, contentType)
        : null;

      if (!parsed) {
        res.redirect('/?shareError=invalid');
        return;
      }

      const token = `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
      sharedCaptureRelay.set(token, {
        ...parsed,
        createdAt: Date.now(),
      });
      res.redirect(`/?sharedToken=${encodeURIComponent(token)}`);
    }
  );

  app.use(express.json({ limit: '15mb' }));

  app.get('/api/shared-capture/:token', (req, res) => {
    cleanupRelay();
    const entry = sharedCaptureRelay.get(req.params.token);
    if (!entry) {
      res.status(404).json({ error: 'Capture partagée expirée ou introuvable.' });
      return;
    }
    sharedCaptureRelay.delete(req.params.token);
    res.json({
      mimeType: entry.mimeType,
      base64: entry.base64,
      fileName: entry.fileName,
    });
  });

  app.post('/api/analyze-screenshot', async (req, res) => {
    try {
      const { imageBase64, mimeType, playerRole, note, language } = req.body as {
        imageBase64?: string;
        mimeType?: string;
        playerRole?: string;
        note?: string;
        language?: TargetLang;
      };

      if (!imageBase64 || !mimeType) {
        res.status(400).json({ error: 'Image manquante pour lancer l’analyse.' });
        return;
      }

      const ai = getAiClient();

      const promptText = `Tu es un système expert de lecture visuelle des écrans du jeu mobile "Lands of Jail".
Analyse l'image entière à haute attention visuelle. Réponds uniquement en français et n'invente aucune donnée.
CONSIGNE PRIORITAIRE — ÉCRAN DES RESSOURCES :
- Si l'écran affiche l'inventaire, l'entrepôt, les ressources du joueur ou des compteurs de ressources, traite-le comme un écran "ressources" même si aucun mot "Food/Bois/Steel" n'est écrit.
- Les ressources peuvent être représentées uniquement par des icônes. Identifie visuellement chaque icône grâce à son dessin et à sa position, puis associe-lui le nombre placé juste à côté, dessous ou dans le même encadré.
- Parcours toute l'image méthodiquement, ligne par ligne et colonne par colonne. N'ignore aucun compteur lisible, même si le nombre est compact (K/M/B), comporte des séparateurs ou est affiché en blanc sur fond coloré.
- Pour chaque compteur, retourne le nom normalisé de la ressource (nourriture, bois, acier/fer, essence/carburant, diamants/gemmes, cash/or ou autre ressource clairement identifiable), le texte exact affiché et la valeur numérique uniquement si la conversion est certaine.
- Ne confonds pas les compteurs de la barre supérieure, les coûts d'amélioration, les récompenses, les quantités possédées et les gains proposés. Indique le contexte/zone dans "raisonIncertitude" si le type de compteur n'est pas certain.
- Ne rejette jamais une ressource uniquement parce que son nom n'est pas écrit. Si l'icône est identifiable mais le nombre ne l'est pas, retourne quand même la ressource avec quantité vide et une incertitude explicite.
- Si plusieurs nombres sont proches, associe-les uniquement selon leur proximité et leur alignement visuels ; ne devine pas. Si aucun compteur de ressource n'est présent, explique pourquoi dans "resumeCapture".
CONSIGNE PRIORITAIRE — ÉCRAN DES HÉROS :
- Si l'écran montre une équipe ou une liste de 5 héros, tu DOIS examiner les 5 emplacements séparément, de gauche à droite puis de haut en bas. Ne t'arrête jamais au premier ou au deuxième héros.
- Retourne une entrée distincte dans "heros" pour CHAQUE héros dont le portrait ou le nom est visiblement identifiable. Le nombre attendu est 5 lorsque cinq portraits sont présents.
- Pour chaque emplacement, inspecte indépendamment le portrait, le nom sous/près du portrait, le niveau, le nombre d'étoiles/éclats, la rareté, la puissance individuelle si affichée et chaque icône/ligne de compétence avec son niveau.
- Le niveau peut être affiché dans un badge séparé du nom : examine toute la zone autour du portrait, pas uniquement la ligne de texte qui contient le nom.
- Ne confonds jamais la puissance maximale théorique du wiki avec la puissance réelle du héros sur le compte.
- Si un portrait est visible mais son identité est illisible, ne devine pas son nom : ajoute une entrée dans "champsIncertains" décrivant précisément l'emplacement non identifié. N'invente pas un sixième héros.
- Si un niveau, une puissance, une étoile ou un niveau de compétence n'est pas clairement lisible, laisse la valeur vide/null et marque le champ incertain. Ne remplace jamais une valeur illisible par une valeur estimée.
- Pour chaque héros, renseigne aussi "champsIncertains" avec les noms exacts des champs personnels illisibles parmi : "niveau", "rarete", "etoiles", "roleHero", "puissanceHero". Un champ absent de cette liste doit être suffisamment lisible pour être mis à jour. N'ajoute jamais les champs wiki/catalogue à cette liste.
- Si aucun niveau/puissance/compétence n'est affiché sur cette capture, indique-le clairement dans "resumeCapture" et "champsIncertains" plutôt que de prétendre que le héros est niveau 1 ou sans compétence.
RÈGLES GÉNÉRALES :
1. Identifie le type d'écran : "profil", "heros", "ressources", "trial_cage", "crystal_clash", ou "autre".
2. Extrais la puissance du compte, niveau QG/Prison et VIP uniquement s'ils sont visibles.
3. Extrais les ressources visibles avec leur quantité exacte.
4. Pour chaque héros visible, renseigne nom, niveau exact, rareté, étoiles, rôle affiché/identifiable, puissance individuelle et toutes les compétences dont le niveau est réellement visible.
5. Sépare strictement les faits lus sur l'image des données générales du catalogue/wiki. Les données wiki servent uniquement à décrire le héros, jamais à compléter son niveau, sa puissance personnelle, ses étoiles ou ses niveaux de compétences.
6. Pour chaque donnée floue, coupée, masquée ou absente, marque "incertain": true et explique le motif dans "raisonIncertitude" ou "champsIncertains".
7. Contexte facultatif : rôle joueur="${playerRole || 'non précisé'}", note="${note || 'aucune'}".`;

      const response = await ai.models.generateContent({
        model: 'gemini-4-hydrogen',
        contents: {
          parts: [
            {
              inlineData: {
                mimeType,
                data: imageBase64,
              },
            },
            { text: promptText },
          ],
        },
        config: {
          responseMimeType: 'application/json',
          responseSchema: {
            type: Type.OBJECT,
            properties: {
              typeCapture: {
                type: Type.STRING,
                description:
                  'Un parmi: profil, heros, ressources, trial_cage, crystal_clash, autre',
              },
              resumeCapture: {
                type: Type.STRING,
                description: 'Résumé factuel en français de ce que montre la capture.',
              },
              puissance: {
                type: Type.OBJECT,
                properties: {
                  presente: { type: Type.BOOLEAN },
                  puissanceTotaleTexte: { type: Type.STRING },
                  puissanceTotaleNumerique: { type: Type.NUMBER },
                  niveauPrisonOuQG: { type: Type.STRING },
                  niveauVip: { type: Type.STRING },
                  incertain: { type: Type.BOOLEAN },
                  raisonIncertitude: { type: Type.STRING },
                },
                required: ['presente', 'puissanceTotaleTexte', 'incertain'],
              },
              ressources: {
                type: Type.ARRAY,
                items: {
                  type: Type.OBJECT,
                  properties: {
                    nom: { type: Type.STRING },
                    quantiteTexte: { type: Type.STRING },
                    quantiteNumerique: { type: Type.NUMBER },
                    unite: { type: Type.STRING },
                    incertain: { type: Type.BOOLEAN },
                    raisonIncertitude: { type: Type.STRING },
                  },
                  required: ['nom', 'quantiteTexte', 'incertain'],
                },
              },
              heros: {
                type: Type.ARRAY,
                items: {
                  type: Type.OBJECT,
                  properties: {
                    nom: { type: Type.STRING },
                    niveau: { type: Type.NUMBER },
                    niveauTexte: { type: Type.STRING },
                    rarete: { type: Type.STRING },
                    etoiles: { type: Type.STRING },
                    roleHero: { type: Type.STRING },
                    puissanceHero: { type: Type.STRING },
                    champsIncertains: { type: Type.ARRAY, items: { type: Type.STRING } },
                    incertain: { type: Type.BOOLEAN },
                    raisonIncertitude: { type: Type.STRING },
                    competences: {
                      type: Type.ARRAY,
                      items: {
                        type: Type.OBJECT,
                        properties: {
                          nom: { type: Type.STRING },
                          niveau: { type: Type.STRING },
                          description: { type: Type.STRING },
                          incertain: { type: Type.BOOLEAN },
                          raisonIncertitude: { type: Type.STRING },
                        },
                        required: ['nom', 'niveau', 'incertain'],
                      },
                    },
                  },
                  required: ['nom', 'niveauTexte', 'incertain', 'competences'],
                },
              },
              champsIncertains: {
                type: Type.ARRAY,
                items: {
                  type: Type.OBJECT,
                  properties: {
                    champ: { type: Type.STRING },
                    valeurObservee: { type: Type.STRING },
                    raison: { type: Type.STRING },
                  },
                  required: ['champ', 'valeurObservee', 'raison'],
                },
              },
            },
            required: [
              'typeCapture',
              'resumeCapture',
              'ressources',
              'heros',
              'champsIncertains',
            ],
          },
        },
      });

      const rawText = (response.text ?? '{}').trim();
      const parsed = JSON.parse(rawText);

      // Deuxième passe dédiée aux ressources : la première analyse peut reconnaître
      // l'écran sans extraire les compteurs. On relit alors l'image originale avec
      // un schéma isolé pour éviter que les héros/profil prennent la priorité.
      const resourceContext = parsed.typeCapture === 'ressources' ||
        /ressource|inventaire|entrep[oô]t|warehouse|stock/i.test(String(note || ''));
      if (resourceContext && (!Array.isArray(parsed.ressources) || parsed.ressources.length === 0)) {
        try {
          const resourceRetry = await ai.models.generateContent({
            model: 'gemini-4-hydrogen',
            contents: {
              parts: [
                { inlineData: { mimeType, data: imageBase64 } },
                {
                  text: `SECONDE PASSE OBLIGATOIRE — EXTRACTION DES COMPTEURS DE RESSOURCES.
L'analyse générale n'a retourné aucune ressource. Réinspecte cette image originale uniquement pour trouver les ressources et leurs quantités.
Repère les icônes par leur apparence et leur position, même si aucun libellé n'est écrit. Lis les nombres immédiatement associés à chaque icône, y compris les formats K, M, B, les espaces et séparateurs.
Vérifie toute la capture, notamment la barre supérieure, les panneaux d'inventaire et les lignes de ressources. Distingue les quantités possédées des coûts, récompenses et gains.
Retourne une ligne par compteur réellement visible. N'invente jamais une valeur. Si l'icône est identifiable mais le chiffre illisible, conserve la ressource avec quantiteTexte vide et incertain=true. Si aucun compteur n'est visible, retourne une liste vide.`,
                },
              ],
            },
            config: {
              responseMimeType: 'application/json',
              responseSchema: {
                type: Type.OBJECT,
                properties: {
                  ressources: {
                    type: Type.ARRAY,
                    items: {
                      type: Type.OBJECT,
                      properties: {
                        nom: { type: Type.STRING },
                        quantiteTexte: { type: Type.STRING },
                        quantiteNumerique: { type: Type.NUMBER },
                        unite: { type: Type.STRING },
                        incertain: { type: Type.BOOLEAN },
                        raisonIncertitude: { type: Type.STRING },
                      },
                      required: ['nom', 'quantiteTexte', 'incertain'],
                    },
                  },
                },
                required: ['ressources'],
              },
            },
          });
          const retryText = (resourceRetry.text ?? '{}').trim();
          const retryParsed = JSON.parse(retryText);
          if (Array.isArray(retryParsed.ressources) && retryParsed.ressources.length > 0) {
            parsed.ressources = retryParsed.ressources;
            parsed.typeCapture = 'ressources';
            parsed.resumeCapture = [String(parsed.resumeCapture || ''), 'Compteurs relus lors du contrôle visuel ciblé.']
              .filter(Boolean)
              .join(' ');
          }
        } catch (resourceRetryError) {
          console.error('Seconde passe ressources échouée:', resourceRetryError);
        }
      }

      // Deuxième passe ciblée uniquement si l'écran est une fiche/liste de héros
      // et que moins de cinq héros ont été restitués. Cela évite de valider trop
      // vite une lecture partielle de la rangée de portraits.
      if (
        (parsed.typeCapture === 'heros' || /héros|heros|heroes/i.test(String(note || ''))) &&
        Array.isArray(parsed.heros) &&
        parsed.heros.length < 5
      ) {
        try {
          const retry = await ai.models.generateContent({
            model: 'gemini-4-hydrogen',
            contents: {
              parts: [
                { inlineData: { mimeType, data: imageBase64 } },
                {
                  text: `SECONDE PASSE DE CONTRÔLE VISUEL — l'analyse précédente n'a détecté que ${parsed.heros.length} héros.
Recommence l'inspection de l'image entière en te concentrant exclusivement sur l'écran des héros.
Compte les portraits visibles un par un, de gauche à droite et de haut en bas. Si cinq portraits sont visibles, cherche à identifier les cinq, pas seulement les noms les plus faciles à lire.
Pour chaque portrait identifiable, relis séparément le nom, le niveau, la puissance individuelle, la rareté, les étoiles et les niveaux de compétences visibles.
Inspecte les badges de niveau même s'ils sont éloignés du nom. Ne transforme pas les valeurs du wiki en valeurs du joueur.
N'invente aucun nom ni niveau. Si un portrait reste illisible, signale son emplacement dans champsIncertains.
Réponds en français et retourne uniquement le JSON conforme au schéma.`,
                },
              ],
            },
            config: {
              responseMimeType: 'application/json',
              responseSchema: {
                type: Type.OBJECT,
                properties: {
                  heros: {
                    type: Type.ARRAY,
                    items: {
                      type: Type.OBJECT,
                      properties: {
                        nom: { type: Type.STRING },
                        niveau: { type: Type.NUMBER },
                        niveauTexte: { type: Type.STRING },
                        rarete: { type: Type.STRING },
                        etoiles: { type: Type.STRING },
                        roleHero: { type: Type.STRING },
                        puissanceHero: { type: Type.STRING },
                        incertain: { type: Type.BOOLEAN },
                        raisonIncertitude: { type: Type.STRING },
                        competences: {
                          type: Type.ARRAY,
                          items: {
                            type: Type.OBJECT,
                            properties: {
                              nom: { type: Type.STRING },
                              niveau: { type: Type.STRING },
                              description: { type: Type.STRING },
                              incertain: { type: Type.BOOLEAN },
                              raisonIncertitude: { type: Type.STRING },
                            },
                            required: ['nom', 'niveau', 'incertain'],
                          },
                        },
                      },
                      required: ['nom', 'niveauTexte', 'incertain', 'competences'],
                    },
                  },
                  champsIncertains: {
                    type: Type.ARRAY,
                    items: {
                      type: Type.OBJECT,
                      properties: {
                        champ: { type: Type.STRING },
                        valeurObservee: { type: Type.STRING },
                        raison: { type: Type.STRING },
                      },
                      required: ['champ', 'valeurObservee', 'raison'],
                    },
                  },
                },
                required: ['heros', 'champsIncertains'],
              },
            },
          });
          const retryParsed = JSON.parse((retry.text ?? '{}').trim());
          const merged = new Map<string, Record<string, unknown>>();
          for (const hero of [...parsed.heros, ...(Array.isArray(retryParsed.heros) ? retryParsed.heros : [])]) {
            if (!hero || typeof hero.nom !== 'string' || !hero.nom.trim()) continue;
            const key = hero.nom.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]/g, '');
            const previous = merged.get(key);
            if (!previous) {
              merged.set(key, hero);
              continue;
            }
            const previousSkills = Array.isArray(previous.competences)
              ? previous.competences as Array<Record<string, unknown>>
              : [];
            const nextSkills = Array.isArray(hero.competences)
              ? hero.competences as Array<Record<string, unknown>>
              : [];
            const retryIsMoreReliable = previous.incertain === true && hero.incertain === false;
            const mergedFields = Object.fromEntries(
              Object.entries(hero).filter(([field, value]) => {
                if (value === undefined || value === null || value === '') return false;
                if (field === 'competences') return false;
                return retryIsMoreReliable ||
                  previous[field] === undefined ||
                  previous[field] === null ||
                  previous[field] === '';
              })
            );

            // Merge skill-by-skill: a reliable second pass may correct an
            // uncertain first read, while never replacing a reliable skill
            // with an uncertain or empty value.
            const skillMap = new Map<string, Record<string, unknown>>();
            for (const skill of previousSkills) {
              const skillKey = String(skill.nom || '').normalize('NFD')
                .replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]/g, '');
              if (skillKey) skillMap.set(skillKey, skill);
            }
            for (const skill of nextSkills) {
              const skillKey = String(skill.nom || '').normalize('NFD')
                .replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]/g, '');
              if (!skillKey) continue;
              const oldSkill = skillMap.get(skillKey);
              if (!oldSkill || (oldSkill.incertain === true && skill.incertain === false)) {
                skillMap.set(skillKey, skill);
              } else if (oldSkill.incertain !== true && skill.incertain !== true) {
                skillMap.set(skillKey, {
                  ...oldSkill,
                  ...Object.fromEntries(Object.entries(skill).filter(([, value]) =>
                    value !== undefined && value !== null && value !== ''
                  )),
                });
              }
            }

            merged.set(key, {
              ...previous,
              ...mergedFields,
              competences: Array.from(skillMap.values()),
            });
          }
          parsed.heros = [...merged.values()].slice(0, 5);
          if (Array.isArray(retryParsed.champsIncertains)) {
            parsed.champsIncertains = [
              ...(Array.isArray(parsed.champsIncertains) ? parsed.champsIncertains : []),
              ...retryParsed.champsIncertains,
            ].filter((item: Record<string, unknown>, index: number, all: Record<string, unknown>[]) =>
              all.findIndex((other) => other.champ === item.champ && other.valeurObservee === item.valeurObservee) === index
            );
          }
        } catch (retryError) {
          console.warn('Seconde passe héros ignorée :', retryError);
        }
      }

      // Enrichissement automatique des héros détectés avec photo et caractéristiques officielles lojwiki.com
      if (Array.isArray(parsed?.heros) && parsed.heros.length > 0) {
        const targetLang: TargetLang =
          language && ['fr', 'tr', 'ru', 'zh', 'de', 'en'].includes(language)
            ? language
            : 'fr';
        await Promise.all(
          parsed.heros.map(async (h: Record<string, unknown>) => {
            const heroName = typeof h.nom === 'string' ? h.nom : '';
            if (!heroName) return;
            const wikiInfo = await fetchHeroWikiDetailsByName(heroName, targetLang);
            if (!wikiInfo) return;
            h.imageUrl = wikiInfo.imageUrl || wikiInfo.avatarUrl;
            h.avatarUrl = wikiInfo.avatarUrl;
            h.titre = wikiInfo.titre;
            h.rarete = (typeof h.rarete === 'string' && h.rarete) || wikiInfo.rarete;
            h.typeTroupe = wikiInfo.typeTroupe;
            h.categorie = wikiInfo.categorie;
            h.saison = wikiInfo.saison;
            h.roleHero = (typeof h.roleHero === 'string' && h.roleHero) || wikiInfo.typeTroupe || wikiInfo.categorie;
            h.puissanceMax5Etoiles = wikiInfo.puissanceMax5Etoiles;
            h.resumeBio = wikiInfo.resumeBio;
            h.idealPour = wikiInfo.idealPour;
            h.wikiUrl = wikiInfo.wikiUrl;
            if (Array.isArray(wikiInfo.competences) && wikiInfo.competences.length > 0) {
              // Le wiki enrichit uniquement la description et l'icône des compétences
              // réellement lues sur la capture. Il ne doit jamais fabriquer les niveaux
              // personnels du joueur ni ajouter des compétences non visibles à son profil.
              const existingSkills = Array.isArray(h.competences)
                ? (h.competences as Array<Record<string, unknown>>)
                : [];
              h.competences = existingSkills.map((sk) => {
                const skName = String(sk.nom || '').trim().toLowerCase();
                const matched = skName
                  ? wikiInfo.competences.find((ws) => {
                      const wikiName = ws.nom.trim().toLowerCase();
                      return wikiName === skName ||
                        wikiName.includes(skName) ||
                        skName.includes(wikiName);
                    })
                  : undefined;
                return {
                  ...sk,
                  description:
                    (typeof sk.description === 'string' && sk.description) ||
                    matched?.description,
                  iconUrl: matched?.iconUrl,
                };
              });
            }
          })
        );
      }

      res.json({
        analysis: parsed,
        analyzedAt: new Date().toISOString(),
      });
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Erreur inconnue lors de l’analyse.';
      console.error('Erreur /api/analyze-screenshot:', err);
      res.status(500).json({
        error: `Impossible d'analyser la capture d'écran : ${message}`,
      });
    }
  });

  type AdviceEventMode =
    | 'trial_cage'
    | 'crystal_clash'
    | 'sandsea_conquest'
    | 'svs'
    | 'turmoil_surge'
    | 'gold_hunter';

  type TargetLang = 'fr' | 'tr' | 'ru' | 'zh' | 'de' | 'en';

  interface LiveGuideItem {
    title: string;
    url: string;
    date: string;
    excerpt: string;
    imageUrl: string | null;
    langMatch: boolean;
  }

  async function fetchLocalizedLojWikiPages(
    mode: AdviceEventMode,
    language: TargetLang
  ): Promise<LiveGuideItem[]> {
    const langPrefixMap: Record<TargetLang, string> = {
      fr: '/fr',
      tr: '/tr',
      ru: '/ru',
      zh: '/zh-cn',
      de: '/de',
      en: '',
    };
    const prefix = langPrefixMap[language] ?? '';
    const slugsByMode: Record<AdviceEventMode, string[]> = {
      trial_cage: ['/events/ultimate-trial/', '/guides/ultimate-trial/', '/events/'],
      crystal_clash: ['/events/crystal-clash/', '/events/'],
      sandsea_conquest: ['/events/sandsea-conquest/', '/events/'],
      svs: ['/events/supreme-honor/', '/events/'],
      turmoil_surge: ['/events/turmoil-surge/', '/events/'],
      gold_hunter: ['/events/gold-hunter/', '/guides/gold-hunter/', '/events/'],
    };

    const slugs = slugsByMode[mode] ?? ['/events/'];
    const decodeEntities = (s: string) =>
      s
        .replace(/&amp;/g, '&')
        .replace(/&quot;/g, '"')
        .replace(/&#39;/g, "'")
        .replace(/&#x27;/g, "'")
        .replace(/&#8217;/g, "'")
        .replace(/&#8211;/g, '–')
        .replace(/\s+/g, ' ')
        .trim();

    const results: LiveGuideItem[] = [];
    const today = new Date().toISOString().slice(0, 10);

    await Promise.all(
      slugs.map(async (slug) => {
        const pageUrl = `https://lojwiki.com${prefix}${slug}`;
        try {
          const controller = new AbortController();
          const timer = setTimeout(() => controller.abort(), 7000);
          const resp = await fetch(pageUrl, {
            signal: controller.signal,
            headers: {
              'User-Agent': 'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0',
              Accept: 'text/html',
            },
          });
          clearTimeout(timer);
          if (!resp.ok) return;
          const html = await resp.text();
          const titleMatch = html.match(/<title>([^<]+)<\/title>/i);
          const descMatch =
            html.match(/<meta\s+name=["']description["']\s+content=["']([^"']+)["']/i) ||
            html.match(/<meta\s+content=["']([^"']+)["']\s+name=["']description["']/i);
          const imgMatches = Array.from(
            html.matchAll(/https:\/\/img\.lojwiki\.com\/[^\s"'<>]+\.(?:webp|png|jpg|jpeg)/gi)
          ).map((m) => m[0]);
          const ogImgMatch = html.match(
            /<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']/i
          );
          const photo =
            imgMatches.find((u) => u.includes('/banners/')) ||
            imgMatches[0] ||
            ogImgMatch?.[1] ||
            null;

          if (titleMatch) {
            results.push({
              title: decodeEntities(titleMatch[1].replace(/\s*\|\s*Lands of Jail.*$/i, '')),
              url: pageUrl,
              date: today,
              excerpt: decodeEntities(descMatch?.[1] || ''),
              imageUrl: photo,
              langMatch: true,
            });
          }
        } catch {
          // ignore single page timeout
        }
      })
    );

    return results;
  }

  async function fetchLiveLojGuides(
    mode: AdviceEventMode,
    heroNames: string[],
    language: TargetLang
  ): Promise<LiveGuideItem[]> {
    const queryMap: Record<AdviceEventMode, string> = {
      trial_cage: 'cage',
      crystal_clash: 'crystal clash',
      sandsea_conquest: 'sandsea',
      svs: 'svs',
      turmoil_surge: 'turmoil',
      gold_hunter: 'gold hunter',
    };
    const queryTerm = queryMap[mode] || 'lands of jail';
    const lojxlabUrls = [
      `https://lojxlab.com/wp-json/wp/v2/posts?per_page=8&_embed=wp:featuredmedia&search=${encodeURIComponent(
        queryTerm
      )}&_fields=date,modified,link,title,excerpt,content,_links,_embedded`,
      ...(heroNames.length > 0
        ? [
            `https://lojxlab.com/wp-json/wp/v2/posts?per_page=4&_embed=wp:featuredmedia&search=${encodeURIComponent(
              heroNames[0]
            )}&_fields=date,modified,link,title,excerpt,content,_links,_embedded`,
          ]
        : []),
    ];

    const stripHtml = (html: string) =>
      html
        .replace(/<[^>]*>/g, ' ')
        .replace(/&#8217;/g, "'")
        .replace(/&#8211;/g, '–')
        .replace(/&#038;/g, '&')
        .replace(/&amp;/g, '&')
        .replace(/&quot;/g, '"')
        .replace(/\s+/g, ' ')
        .trim();

    const extractFirstPhoto = (post: {
      content?: { rendered?: string };
      _embedded?: {
        'wp:featuredmedia'?: Array<{
          source_url?: string;
          media_details?: {
            sizes?: Record<string, { source_url?: string }>;
          };
        }>;
      };
    }): string | null => {
      const fm = post._embedded?.['wp:featuredmedia']?.[0];
      const sizes = fm?.media_details?.sizes;
      const featured =
        sizes?.['lojxlab-card']?.source_url ||
        sizes?.medium_large?.source_url ||
        sizes?.large?.source_url ||
        sizes?.medium?.source_url ||
        fm?.source_url;
      if (featured && /^https?:\/\//i.test(featured)) return featured;

      const contentHtml = post.content?.rendered || '';
      const imgMatch = contentHtml.match(/https?:\/\/[^\s"'<>]+\.(?:webp|png|jpg|jpeg)/i);
      return imgMatch ? imgMatch[0] : null;
    };

    const [wikiGuides] = await Promise.all([fetchLocalizedLojWikiPages(mode, language)]);

    const lojxlabCollected: LiveGuideItem[] = [];
    const seen = new Set<string>(wikiGuides.map((g) => g.url));

    await Promise.all(
      lojxlabUrls.map(async (u) => {
        try {
          const controller = new AbortController();
          const timer = setTimeout(() => controller.abort(), 7000);
          const resp = await fetch(u, {
            signal: controller.signal,
            headers: { Accept: 'application/json', 'User-Agent': 'LandsOfJailAssistant/1.5' },
          });
          clearTimeout(timer);
          if (!resp.ok) return;
          const posts = (await resp.json()) as Array<{
            date?: string;
            modified?: string;
            link?: string;
            title?: { rendered?: string };
            excerpt?: { rendered?: string };
            content?: { rendered?: string };
            _embedded?: {
              'wp:featuredmedia'?: Array<{
                source_url?: string;
                media_details?: {
                  sizes?: Record<string, { source_url?: string }>;
                };
              }>;
            };
          }>;
          if (!Array.isArray(posts)) return;

          const sorted = [...posts].sort((a, b) => {
            const score = (link = '') => {
              if (language === 'de' && link.includes('/de/')) return 4;
              if (language === 'fr' && link.includes('/fr/')) return 4;
              if (link.includes('/blog/')) return 2;
              return 1;
            };
            return score(b.link) - score(a.link);
          });

          for (const p of sorted) {
            const url = p.link || '';
            if (!url || seen.has(url)) continue;
            seen.add(url);
            const rawDate = (p.modified || p.date || '').slice(0, 10);
            lojxlabCollected.push({
              title: stripHtml(p.title?.rendered || 'Guide lojxlab.com'),
              url,
              date: rawDate || new Date().toISOString().slice(0, 10),
              excerpt: stripHtml(p.excerpt?.rendered || ''),
              imageUrl: extractFirstPhoto(p),
              langMatch:
                (language === 'de' && url.includes('/de/')) ||
                (language === 'en' && url.includes('/blog/')),
            });
          }
        } catch {
          // Ignore single feed failure
        }
      })
    );

    return [...wikiGuides, ...lojxlabCollected].slice(0, 8);
  }

  app.post('/api/advice', async (req, res) => {
    try {
      const { mode, language, playerRole, playerRoleLabel, heroes, extraQuestion } = req.body as {
        mode?: AdviceEventMode;
        language?: TargetLang;
        playerRole?: string;
        playerRoleLabel?: string;
        heroes?: Array<{
          nom: string;
          niveau?: number | null;
          niveauTexte: string;
          rarete?: string;
          etoiles?: string;
          roleHero?: string;
          typeTroupe?: string;
          puissanceHero?: string;
          champsIncertains?: string[];
          incertain?: boolean;
          verifieManuellement?: boolean;
          competences?: Array<{
            nom: string;
            niveau: string;
            description?: string;
            incertain?: boolean;
            raisonIncertitude?: string;
          }>;
        }>;
        powerText?: string;
        extraQuestion?: string;
      };

      const validModes: AdviceEventMode[] = [
        'trial_cage',
        'crystal_clash',
        'sandsea_conquest',
        'svs',
        'turmoil_surge',
        'gold_hunter',
      ];
      if (!mode || !validModes.includes(mode)) {
        res.status(400).json({ error: 'Mode / événement invalide.' });
        return;
      }

      const validLangs: TargetLang[] = ['fr', 'tr', 'ru', 'zh', 'de', 'en'];
      const targetLang: TargetLang =
        language && validLangs.includes(language) ? language : 'fr';

      const modeNamesByLang: Record<TargetLang, Record<AdviceEventMode, string>> = {
        fr: {
          trial_cage: 'Trial Cage',
          crystal_clash: 'Crystal Clash',
          sandsea_conquest: 'Conquête de sable',
          svs: 'SvS / Supreme Honor',
          turmoil_surge: 'Turmoil Surge',
          gold_hunter: 'Gold Hunter & Sprint',
        },
        tr: {
          trial_cage: 'Deneme Kafesi',
          crystal_clash: 'Kristal Çatışması',
          sandsea_conquest: 'Kum Denizi Fethi',
          svs: 'SvS / Yüce Onur',
          turmoil_surge: 'Kargaşa Dalgası',
          gold_hunter: 'Altın Avcısı',
        },
        ru: {
          trial_cage: 'Испытание клетки',
          crystal_clash: 'Кристальная схватка',
          sandsea_conquest: 'Завоевание Песчаного моря',
          svs: 'SvS / Высшее отличие',
          turmoil_surge: 'Волна смуты',
          gold_hunter: 'Охотник за золотом',
        },
        zh: {
          trial_cage: '终极试炼',
          crystal_clash: '水晶争夺战',
          sandsea_conquest: '沙海征服',
          svs: '跨服战 / 至高荣耀',
          turmoil_surge: '动乱危机',
          gold_hunter: '黄金猎人',
        },
        de: {
          trial_cage: 'Trial Cage',
          crystal_clash: 'Kristallkampf',
          sandsea_conquest: 'Sandmeer-Eroberung',
          svs: 'SvS / Höchste Ehre',
          turmoil_surge: 'Turmoil Surge',
          gold_hunter: 'Goldjäger',
        },
        en: {
          trial_cage: 'Trial Cage',
          crystal_clash: 'Crystal Clash',
          sandsea_conquest: 'Sandsea Conquest',
          svs: 'SvS / Supreme Honor',
          turmoil_surge: 'Turmoil Surge',
          gold_hunter: 'Gold Hunter & Sprint',
        },
      };

      const modeName = modeNamesByLang[targetLang][mode];
      const heroNames = (heroes ?? []).map((h) => h.nom).filter(Boolean);
      // Pass only account-observed data to the tactical context. Wiki maximums
      // are intentionally excluded because they are not the player's progression.
      const accountHeroContext = (heroes ?? []).map((hero) => {
        const uncertain = new Set(hero.champsIncertains || []);
        const personalFields = [
          !uncertain.has('niveau') ? hero.niveauTexte : 'niveau à confirmer',
          !uncertain.has('etoiles') ? hero.etoiles : 'étoiles à confirmer',
          !uncertain.has('rarete') ? hero.rarete : 'rareté à confirmer',
          !uncertain.has('typeTroupe') ? hero.typeTroupe : 'type de troupe à confirmer',
          !uncertain.has('puissanceHero') ? hero.puissanceHero : 'puissance à confirmer',
        ].filter(Boolean);
        const skills = (hero.competences || []).map((skill) => {
          const level = skill.incertain
            ? 'niveau à confirmer'
            : skill.niveau || 'niveau non relevé';
          const description = skill.description?.trim()
            ? ` ; effet décrit : ${skill.description.trim()}`
            : ' ; effet non documenté dans la capture';
          const uncertainty = skill.incertain && skill.raisonIncertitude
            ? ` ; incertitude : ${skill.raisonIncertitude}`
            : '';
          return `${skill.nom}: ${level}${description}${uncertainty}`;
        });
        return `${hero.nom} [${personalFields.join(', ')}]${skills.length ? ` | compétences: ${skills.join('; ')}` : ''}`;
      });
      // Event-specific reference notes are deliberately limited to the small
      // curated roster whose roles are already documented in landsOfJailData.ts.
      // They are role hints, not a numerical power ranking.
      const knownEventRoles: Record<string, { cage: string; crystal: string }> = {
        Tyronn: { cage: 'Tank principal de première ligne', crystal: 'Tenue de points stratégiques' },
        Phoenix: { cage: 'DPS polyvalent contre le boss', crystal: 'Soutien offensif sur les prises de points' },
        Rin: { cage: 'DPS arrière élevé', crystal: 'Nettoyage des défenseurs sur points de cristal' },
        Edwin: { cage: 'Dégâts soutenus en ralliement Shooter', crystal: 'Leader de ralliement offensif' },
        Otto: { cage: 'Tank secondaire / bouclier', crystal: 'Garnison et défense de Crystal Ironhold' },
        Marcus: { cage: 'Absorption des dégâts en première ligne', crystal: 'Défenseur de garnison' },
        Ada: { cage: 'Dégâts à distance réguliers', crystal: 'Escarmouches et dépôts de cristal' },
        Lee: { cage: 'Contrôle / dégâts de zone', crystal: 'Perturbation des groupes ennemis' },
        Patrick: { cage: 'Tank F2P de transition', crystal: 'Garnison temporaire / renfort' },
        Mia: { cage: 'Soin et maintien en début de jeu', crystal: 'Soutien / renfort' },
      };
      const eventRoleContext = (heroes ?? []).flatMap((hero) => {
        const match = Object.entries(knownEventRoles).find(([name]) =>
          name.toLocaleLowerCase() === hero.nom.trim().toLocaleLowerCase()
        );
        if (!match) return [];
        const role = mode === 'trial_cage' ? match[1].cage
          : mode === 'crystal_clash' ? match[1].crystal : '';
        return role ? [`${hero.nom}: ${role} (rôle de référence; progression personnelle à considérer séparément)`] : [];
      });
      const eventRoleContextLabel: Record<TargetLang, string> = {
        fr: 'Repères de rôle du catalogue pour cet événement (pas un classement de puissance) :',
        tr: 'Etkinlik için katalog rol notları (güç sıralaması değildir):',
        ru: 'Ролевые заметки каталога для события (это не рейтинг силы):',
        zh: '该活动的图鉴定位参考（并非战力排名）：',
        de: 'Katalog-Rollenhinweise für dieses Event (keine Stärkerangliste):',
        en: 'Catalog role references for this event (not a power ranking):',
      };
      const eventRoleContextText = eventRoleContext.length
        ? ` ${eventRoleContextLabel[targetLang]} ${eventRoleContext.join('; ')}.`
        : '';

      const liveGuides = await fetchLiveLojGuides(mode, heroNames, targetLang);

      const roleLabel = playerRoleLabel || playerRole || 'Polyvalent / F2P';
      const liveGuideUnavailableByLang: Record<TargetLang, string> = {
        fr: 'Aucun guide dynamique n’a pu être récupéré. Les repères enregistrés et les liens de référence restent disponibles; vérifiez les règles affichées dans le jeu.',
        tr: 'Canlı rehber alınamadı. Kayıtlı referanslar ve bağlantılar kullanılabilir; oyun içindeki kuralları kontrol edin.',
        ru: 'Не удалось получить актуальные руководства. Сохраненные ориентиры и ссылки доступны; проверьте правила в игре.',
        zh: '暂时无法获取动态攻略。仍可查看已保存的参考信息和链接；请以游戏内规则为准。',
        de: 'Aktuelle Guides konnten nicht geladen werden. Gespeicherte Hinweise und Links bleiben verfügbar; prüfen Sie die Spielregeln.',
        en: 'Live guides could not be retrieved. Saved references and links remain available; verify the rules shown in-game.',
      };
      const leadSummary = liveGuides
        .slice(0, 2)
        .map((g) => g.excerpt)
        .filter(Boolean)
        .join(' ') || liveGuideUnavailableByLang[targetLang];

      const cageDamagePriorityByLang: Record<TargetLang, string> = {
        fr: 'Règle Cage : privilégier les compétences et bonus qui augmentent directement les dégâts infligés ou réduisent les défenses de la cible. Les bonus uniquement défensifs/soins ne sont pas prioritaires pour le score de dégâts.',
        tr: 'Kafes kuralı: verilen hasarı doğrudan artıran veya hedef savunmasını düşüren beceri ve bonuslara öncelik verin. Yalnızca savunma/iyileştirme bonusları hasar puanı için öncelikli değildir.',
        ru: 'Правило Клетки: отдавайте приоритет навыкам и бонусам, напрямую повышающим наносимый урон или снижающим защиту цели. Чисто защитные бонусы и лечение не являются приоритетом для очков урона.',
        zh: '试炼牢笼规则：优先选择直接提高伤害或降低目标防御的技能与加成。纯防御和治疗加成不是伤害积分的优先项。',
        de: 'Käfig-Regel: Fähigkeiten und Boni priorisieren, die verursachten Schaden direkt erhöhen oder die gegnerische Verteidigung senken. Rein defensive Boni/Heilung haben für den Schadenswert keine Priorität.',
        en: 'Cage rule: prioritize skills and buffs that directly increase damage dealt or reduce the target’s defenses. Purely defensive bonuses/healing are not priorities for a damage score.',
      };
      const compTextByLang: Record<TargetLang, string> = {
        fr:
          (heroNames.length > 0
            ? `Héros du compte intégrés : ${heroNames.join(', ')} (rôle : ${roleLabel}).`
            : `Aucun héros encore scanné : composition personnalisée impossible tant que le roster n’est pas analysé (${roleLabel}).`) +
          (mode === 'trial_cage' ? ` ${cageDamagePriorityByLang[targetLang]}` : ''),
        tr:
          (heroNames.length > 0
            ? `Tespit edilen kahramanlar: ${heroNames.join(', ')} (rol: ${roleLabel}).`
            : 'Henüz kahraman taranmadı; kişisel takım önerisi için önce kahramanları tarayın.') +
          (mode === 'trial_cage' ? ` ${cageDamagePriorityByLang[targetLang]}` : ''),
        ru:
          (heroNames.length > 0
            ? `Обнаруженные герои: ${heroNames.join(', ')} (роль: ${roleLabel}).`
            : 'Герои ещё не отсканированы; для персонального состава сначала просканируйте ростер.') +
          (mode === 'trial_cage' ? ` ${cageDamagePriorityByLang[targetLang]}` : ''),
        zh:
          (heroNames.length > 0
            ? `已纳入账号英雄：${heroNames.join('、')}（定位：${roleLabel}）。`
            : '尚未扫描英雄；请先扫描英雄列表，再生成个性化阵容。') +
          (mode === 'trial_cage' ? ` ${cageDamagePriorityByLang[targetLang]}` : ''),
        de:
          (heroNames.length > 0
            ? `Erkannte Kontohelden: ${heroNames.join(', ')} (Rolle: ${roleLabel}).`
            : 'Noch keine Helden gescannt; für eine persönliche Aufstellung zuerst das Helden-Roster erfassen.') +
          (mode === 'trial_cage' ? ` ${cageDamagePriorityByLang[targetLang]}` : ''),
        en:
          (heroNames.length > 0
            ? `Account heroes included: ${heroNames.join(', ')} (role: ${roleLabel}).`
            : 'No heroes scanned; scan the roster first to generate a personalized lineup.') +
          (mode === 'trial_cage' ? ` ${cageDamagePriorityByLang[targetLang]}` : ''),
      };

      const profileContextLabel: Record<TargetLang, string> = {
        fr: 'Données personnelles détectées (les champs incertains sont signalés) :',
        tr: 'Algılanan hesap verileri (belirsiz alanlar işaretlenmiştir):',
        ru: 'Данные аккаунта (неуверенно распознанные поля отмечены):',
        zh: '识别到的账号数据（不确定字段已标注）：',
        de: 'Erkannte Kontodaten (unsichere Felder sind gekennzeichnet):',
        en: 'Detected account data (uncertain fields are marked):',
      };
      const groundedProfileContext = accountHeroContext.length
        ? ` ${profileContextLabel[targetLang]} ${accountHeroContext.join(' || ')}`
        : '';

      const synthPrefix: Record<TargetLang, string> = {
        fr: `${modeName} · Rôle : ${roleLabel}${extraQuestion ? ` · Question : ${extraQuestion}` : ''}. `,
        tr: `${modeName} · Rol: ${roleLabel}. `,
        ru: `${modeName} · Роль: ${roleLabel}. `,
        zh: `${modeName} · 定位：${roleLabel}。`,
        de: `${modeName} · Rolle: ${roleLabel}. `,
        en: `${modeName} · Role: ${roleLabel}. `,
      };

      // Heuristic Trial Cage assessment: only explicit skill text can qualify a hero.
      // This is a transparent signal detector, not a combat-damage simulation.
      const cageSkillSignals = (heroes ?? []).flatMap((hero) => {
        const uncertainFields = new Set(hero.champsIncertains || []);
        if (uncertainFields.has('etoiles') || uncertainFields.has('niveau')) return [];
        return (hero.competences || [])
          .filter((skill) => !skill.incertain && Boolean(skill.description?.trim()))
          .map((skill) => {
            const effect = skill.description!.toLocaleLowerCase();
            // Do not treat the bare word "damage" as an offensive signal:
            // "reduces damage taken" is defensive, while "deals damage" is offensive.
            const incomingDamageReduction = /damage reduction|reduce[s]? damage taken|reduce[s]? incoming damage|less damage taken|réduction des dégâts reçus|réduit les dégâts reçus|diminue les dégâts subis|уменьш.*получаем.*урон|сниж.*получаем.*урон|受到的伤害降低|降低受到的伤害|alınan hasarı azalt|erlitten schaden.*reduzier|erlittenen schaden.*senk/i.test(effect);
            const directDamage = /(?:deal|deals|dealing|inflict|inflicts|cause|causes)\s+(?:\w+\s+){0,2}(?:damage|dmg)|(?:damage|dmg)\s+(?:to|against)\s+(?:the\s+)?(?:enemy|enemies|opponent)|inflige(?:r|s|nt)?\s+(?:des?\s+)?dégâts?|наносит урон|причиняет урон|造成伤害|对敌人造成伤害|hasar verir|hasar uygular|verursacht schaden|fügt.*schaden zu/i.test(effect);
            const enemyDamageTaken = /enemy.{0,35}(?:take|takes|receive|receives).{0,20}(?:more|increased|additional).{0,20}damage|damage taken by (?:the )?enemy|augmente les dégâts subis par (?:l'ennemi|les ennemis)|dégâts subis par l'ennemi.*augment|увелич.*получаем.*урон.*враг|敌人受到的伤害.*提高|hasar alımını artır.*düşman/i.test(effect);
            const damageBoost = /attack increase|increase[s]? (?:allied |troop )?damage(?: dealt)?|damage increase|bonus damage|augmente.*dégâts infligés|bonus de dégâts|dégâts? infligés.*augment|увелич.*урон|урон.*увелич|伤害.*提高|攻击.*提高|hasar.*art|schaden.*erhöh/i.test(effect);
            const defenseBreak = /defen[cs]e reduc|armor reduc|降低防御|防御降低|défense.*réduit|réduit.*défense|уменьш.*защит|сниж.*защит|savunma.*azal|verteidig.*senk/i.test(effect);
            const hasOffensiveSignal = directDamage || enemyDamageTaken || damageBoost || defenseBreak;
            const purelyDefensive = (incomingDamageReduction || /heal|healing|shield|soin|soigne|bouclier|лечени|щит|лечение|护盾|治疗|iyileştir|kalkan|heilung|schild/i.test(effect))
              && !hasOffensiveSignal;
            const category = directDamage ? 0 : enemyDamageTaken || damageBoost ? 1 : defenseBreak ? 2 : -1;
            return { hero: hero.nom, skill: skill.nom, category, purelyDefensive };
          });
      }).filter((signal) => signal.category >= 0 && !signal.purelyDefensive)
        .sort((a, b) => a.category - b.category);

      const cageAssessmentText: Record<TargetLang, string> = {
        fr: 'Indices offensifs détectés dans les descriptions de compétences (heuristique, pas une simulation) :',
        tr: 'Beceri açıklamalarında bulunan saldırı işaretleri (tahmini sınıflandırma, simülasyon değildir):',
        ru: 'Атакующие признаки в описаниях навыков (эвристика, не симуляция):',
        zh: '技能描述中识别的进攻效果（启发式判断，并非伤害模拟）：',
        de: 'Offensive Hinweise in Fertigkeitsbeschreibungen (Heuristik, keine Simulation):',
        en: 'Offensive signals found in skill descriptions (heuristic, not a simulation):',
      };
      const cageAssessment = mode === 'trial_cage'
        ? (cageSkillSignals.length
            ? ` ${cageAssessmentText[targetLang]} ${cageSkillSignals.map((s) => `${s.hero} — ${s.skill} [${['direct damage', 'damage taken / damage buff', 'defense debuff'][s.category]}]`).join('; ')}.`
            : (targetLang === 'fr'
                ? ' Aucune compétence ne peut être classée de façon fiable : descriptions absentes, incertaines ou sans effet offensif explicite. Classement indisponible.'
                : ' No skill can be classified reliably: descriptions are missing, uncertain, or contain no explicit offensive effect. Ranking unavailable.'))
        : '';
      // Trial Cage rally guidance from the curated in-app reference catalogue.
      // Keep launcher ratios separate from joiner troop composition.
      const cageRallyGuidance: Record<TargetLang, string> = {
        fr: ' Référence communautaire Trial Cage mise à jour le 4 novembre 2026 (à vérifier après chaque mise à jour du jeu) : ratio du chef sans Ada 0% Shieldbearers / 10% Bombers / 90% Shooters; avec Ada 1% / 9% / 90%, en vérifiant aussi le nombre réel de Shieldbearers (environ 1 000 selon le guide). Pour un participant : 0/10/90 ou 0/0/100 selon les consignes du ralliement. Les rôles sont différents : ne transposez pas automatiquement le ratio du chef au participant. Le guide LOJ XLab rapporte que le chef apporte ses 3 héros et leurs compétences de guerre, tandis que chaque participant apporte la première compétence de guerre de son premier héros; seules les quatre compétences de participants retenues comptent selon cette observation communautaire. Vérifiez la compétence réellement affichée dans les bonus du ralliement et évitez les bonus identiques.',
        tr: '4 Kasım 2026 tarihli topluluk Trial Cage referansı (oyun güncellemelerinden sonra tekrar doğrulayın): Ada olmadan lider oranı %0 Shieldbearer / %10 Bomber / %90 Shooter; Ada ile %1 / %9 / %90 ve gerçek Shieldbearer sayısını da kontrol edin (rehbere göre yaklaşık 1.000). Katılan oyuncu için ralli talimatına göre %0/10/90 veya %0/0/100. Roller farklıdır; lider oranını katılana otomatik uygulamayın. LOJ XLab topluluk gözlemine göre lider 3 kahramanının savaş becerilerini sağlar; her katılımcı ilk kahramanının ilk savaş becerisini sağlar ve katılımcı becerilerinden yalnızca seçilen dört tanesi sayılır. Ralli bonuslarında gerçekten uygulanan beceriyi kontrol edin ve aynı bonusları tekrarlamayın.',
        ru: 'Обновленный 4 ноября 2026 года ориентир сообщества для Trial Cage (перепроверяйте после обновлений игры): состав лидера без Ada — 0% Shieldbearer / 10% Bomber / 90% Shooter; с Ada — 1% / 9% / 90%, дополнительно проверьте фактическое число Shieldbearer (около 1 000 по руководству). Для участника — 0/10/90 или 0/0/100 согласно указаниям рейда. Роли различаются: не переносите автоматически соотношение лидера на участника. По наблюдениям сообщества LOJ XLab, лидер дает навыки всех трех героев, а каждый участник — первый боевой навык первого героя; учитываются только четыре выбранных навыка участников. Проверьте фактически примененный навык в бонусах рейда и избегайте дублирования бонусов.',
        zh: '2026 年 11 月 4 日更新的 Trial Cage 社区参考（游戏更新后需重新核实）：队长无 Ada 时比例为 0% Shieldbearer / 10% Bomber / 90% Shooter；有 Ada 时为 1% / 9% / 90%，还要核对 Shieldbearer 实际数量（指南建议约 1,000）。加入者按集结要求使用 0/10/90 或 0/0/100。两种角色不同，不要把队长比例直接套用给加入者。LOJ XLab 的社区观察称，队长提供三名英雄的战争技能，每位加入者提供其第一名英雄的第一项战争技能；加入者技能只计入被选中的四项。请在集结奖励中核对实际生效的技能，并避免重复加成。',
        de: 'Community-Referenz für Trial Cage, aktualisiert am 4. November 2026 (nach Spielupdates erneut prüfen): Verhältnis des Anführers ohne Ada 0% Shieldbearer / 10% Bomber / 90% Shooter; mit Ada 1% / 9% / 90%, zusätzlich die tatsächliche Zahl der Shieldbearer prüfen (laut Guide etwa 1.000). Für Beitretende je nach Rallye-Anweisung 0/10/90 oder 0/0/100. Die Rollen unterscheiden sich; das Verhältnis des Anführers nicht automatisch auf Beitretende übertragen. Laut Community-Beobachtung von LOJ XLab bringt der Anführer die Kriegsfähigkeiten aller drei Helden ein, jeder Beitretende die erste Kriegsfähigkeit seines ersten Helden; nur vier ausgewählte Beitretenden-Fähigkeiten zählen. Prüfen Sie den tatsächlich angewendeten Bonus und vermeiden Sie doppelte Boni.',
        en: 'Trial Cage community reference updated November 4, 2026 (recheck after game updates): leader ratio without Ada is 0% Shieldbearers / 10% Bombers / 90% Shooters; with Ada it is 1% / 9% / 90%, and the actual Shieldbearer count should also be checked (about 1,000 according to the guide). For a joiner, use 0/10/90 or 0/0/100 according to rally instructions. These roles differ; do not automatically apply the leader ratio to joiners. LOJ XLab community observations say the leader contributes the war skills of all three heroes, while each joiner contributes the first war skill of their first hero; only four selected joiner skills count. Verify the skill actually applied in rally bonuses and avoid duplicate buffs.',
      };
      const cageRallyContext = mode === 'trial_cage' ? cageRallyGuidance[targetLang] : '';
      // Sandsea mechanics reference from the independent LOJ Wiki; not a hero-power ranking.
      const sandseaContextByLang: Record<TargetLang, string> = {
        fr: ' Repères Conquête des sables (guide communautaire LOJ Wiki) : la légion compte 6 escouades A–F de 7 membres maximum; les affectations peuvent être modifiées pendant le combat uniquement par le Tacticien. Un seul Tacticien est nommé par la légion par les R4+; ses troupes système n’ont pas de héros, donc son rôle est surtout la coordination. Les bannières donnent létalité et PV aux membres de leur escouade près des bastions; 6 emplacements existent, dont 1/3/4/6 dès l’ouverture et 2/5 en phase 2. Phase 1 : contrôler un maximum de forteresses; phase 2 : disputer les Phares; phase 3 : prioriser la Plateforme de forage centrale, ouverte après 40 min, qui apporte un bonus de 40 000 points au règlement selon le wiki. Le carburant sert aux marches, recrutements, raids, retraites et réapparitions. Aucun ratio universel de troupes ni trio de héros n’est déduit ici : il faut l’adapter à l’escouade, à l’objectif et aux rapports de combat.',
        tr: ' Sandsea Conquest referansı (LOJ Wiki topluluk rehberi): lejyon, en fazla 7 üyeli A–F olmak üzere 6 mangaya ayrılır; savaş sırasında atamaları yalnızca Taktisyen değiştirebilir. R4+ üyeler lejyon için tek bir Taktisyen seçer; Taktisyenin sistem birliklerinde kahraman bulunmaz, bu nedenle görevi esas olarak koordinasyondur. Savaş sancakları, ilgili manganın üyelerine yakın kalelerde ölümcüllük ve HP bonusu verir; 6 nokta vardır, 1/3/4/6 başlangıçta, 2/5 ikinci aşamada açılır. Aşama 1: mümkün olduğunca çok kaleyi kontrol edin; Aşama 2: Fenerleri ele geçirin; Aşama 3: 40. dakikadan sonra açılan merkez Sondaj Platformuna öncelik verin; wikiye göre kontrolü bitişte 40.000 puan bonusu sağlar. Yakıt yürüyüş, asker alımı, baskın, geri çekilme ve yeniden doğma için kullanılır. Burada evrensel birlik oranı veya kahraman üçlüsü uydurulmaz; manga, hedef ve savaş raporlarına göre ayarlanmalıdır.',
        ru: ' Ориентиры Sandsea Conquest (сообщество LOJ Wiki): легион делится на 6 отрядов A–F, максимум по 7 участников; во время боя составы может менять только Тактик. У легиона один Тактик, назначаемый R4+; его системные войска не имеют героев, поэтому его роль прежде всего координационная. Боевые знамена дают отряду бонусы к летальности и HP рядом с соответствующими крепостями; всего 6 точек: 1/3/4/6 доступны в начале, 2/5 — во второй фазе. Фаза 1: захватывайте больше крепостей; фаза 2: боритесь за Маяки; фаза 3: сосредоточьтесь на центральной Буровой платформе, открывающейся через 40 минут и дающей, по данным wiki, бонус 40 000 очков при итоговом подсчете. Топливо расходуется на марши, набор войск, рейды, отступления и возрождение. Универсальное соотношение войск или трио героев здесь не выдумывается: настройка зависит от отряда, цели и боевых отчетов.',
        zh: ' 沙海征服参考（LOJ Wiki 社区指南）：军团分为 A–F 六个小队，每队最多 7 人；战斗期间只有战术官可以调整成员分队。R4+ 成员为军团指定一名战术官；战术官使用的系统部队没有英雄，因此其主要职责是协调。战旗会为附近据点中的对应小队成员提供杀伤和 HP 加成；共有 6 个旗点，1/3/4/6 开场开放，2/5 在第二阶段开放。第一阶段尽可能控制更多据点；第二阶段争夺灯塔；第三阶段优先争夺中央钻井平台，该平台在 40 分钟后开放，据 wiki 控制方结算可获得 40,000 分奖励。燃料用于行军、招募、突袭、撤退和复活。此处不臆造通用兵种比例或英雄三人组，应根据小队、目标和战报调整。',
        de: ' Sandsea-Conquest-Referenz (Community-Guide von LOJ Wiki): Die Legion besteht aus 6 Trupps A–F mit höchstens 7 Mitgliedern; während des Kampfes darf nur der Taktiker die Zuordnung ändern. R4+ ernennen einen Taktiker für die Legion; dessen Systemtruppen haben keine Helden, daher liegt der Schwerpunkt auf Koordination. Kriegsbanner gewähren Mitgliedern des jeweiligen Trupps in der Nähe der Festungen Boni auf Tödlichkeit und HP; es gibt 6 Punkte, 1/3/4/6 ab Beginn und 2/5 in Phase 2. Phase 1: möglichst viele Festungen kontrollieren; Phase 2: Leuchttürme umkämpfen; Phase 3: die zentrale Bohrplattform priorisieren, die nach 40 Minuten öffnet und laut Wiki bei der Abrechnung 40.000 Bonuspunkte bringt. Treibstoff wird für Märsche, Rekrutierung, Raids, Rückzüge und Wiederbelebung benötigt. Es wird kein universelles Truppenverhältnis oder Heldentrio erfunden; die Wahl hängt von Trupp, Ziel und Kampfberichten ab.',
        en: ' Sandsea Conquest reference (LOJ Wiki community guide): the Legion has 6 squads A–F with up to 7 members each; only the Tactician can change squad assignments during battle. R4+ members appoint one Tactician for the Legion; the Tactician’s system-provided troops have no heroes, so the role is mainly coordination. War banners grant Lethality and HP bonuses to squad members near the relevant strongholds; there are 6 banner points, with 1/3/4/6 available at the start and 2/5 in phase 2. Phase 1: control as many strongholds as possible; phase 2: contest the Lighthouses; phase 3: prioritize the central Drilling Platform, which opens after 40 minutes and gives its controller a 40,000-point settlement bonus according to the wiki. Fuel is used for marches, recruitment, raids, retreats and respawns. No universal troop ratio or hero trio is invented here; adapt to squad, objective and battle reports.',
      };
      const sandseaContext = mode === 'sandsea_conquest' ? sandseaContextByLang[targetLang] : '';
      const crystalContextByLang: Record<TargetLang, string> = {
        fr: ' Repères Crystal Clash (guides communautaires actualisés en octobre/novembre 2026) : choisissez la classe du jeu selon votre mission — Commander pour mener les ralliements, Warrior pour rejoindre/renforcer, Engineer pour collecter les cristaux. Priorisez le contrôle durable des objectifs plutôt que les frags isolés : prison centrale, forteresses, prisons de cristal, puis bâtiments utilitaires selon leur effet réel affiché. Le Refinement Center aide la collecte et la prise de contrôle; Pillbox soutient les troupes; Bomber Command affaiblit une défense; Energy Hub réduit le délai du téléport fixe. Vérifiez les valeurs dans l’onglet Règles du jeu : les scores rapportés par des communautés ne sont pas toujours identiques. Aucun ratio universel ni trio de héros n’est inventé sans connaître la mission et les défenses.',
        tr: ' Crystal Clash referansı (Ekim/Kasım 2026 topluluk rehberleri): oyun sınıfını göreve göre seçin — Commander ralli başlatır, Warrior rallilere katılır/takviye eder, Engineer kristal toplar. Tekil öldürmeler yerine hedefleri uzun süre kontrol etmeye öncelik verin: merkez hapishane, kaleler, kristal hapishaneleri, ardından gerçek etkisine göre yardımcı binalar. Refinement Center toplama ve ele geçirme süresine yardım eder; Pillbox birlikleri güçlendirir; Bomber Command savunmayı zayıflatır; Energy Hub sabit ışınlanma bekleme süresini azaltır. Toplulukların bildirdiği puanlar farklı olabileceğinden oyun içi Kurallar sekmesini kontrol edin. Görev ve savunmalar bilinmeden evrensel birlik oranı veya kahraman üçlüsü uydurulmaz.',
        ru: ' Ориентиры Crystal Clash (руководства сообщества за октябрь/ноябрь 2026): выбирайте игровой класс по задаче — Commander ведет рейды, Warrior вступает в рейды и усиливает оборону, Engineer собирает кристаллы. Приоритет — длительный контроль целей, а не отдельные убийства: центральная тюрьма, крепости, кристальные тюрьмы, затем вспомогательные здания по их фактическому эффекту. Refinement Center помогает сбору и захвату; Pillbox усиливает войска; Bomber Command ослабляет защиту; Energy Hub сокращает перезарядку фиксированного телепорта. Проверяйте значения во вкладке правил игры: данные сообществ могут различаться. Универсальное соотношение войск или трио героев не выдумывается без знания задачи и обороны.',
        zh: ' Crystal Clash 参考（2026 年 10/11 月社区指南）：根据任务选择游戏职业 — Commander 负责发起集结，Warrior 加入集结/增援，Engineer 负责采集水晶。优先持续控制目标，而不是单纯追求击杀：中央监狱、堡垒、水晶监狱，然后根据实际效果选择功能建筑。Refinement Center 可帮助采集和缩短占领时间；Pillbox 为部队提供战斗支援；Bomber Command 削弱敌方据点防御；Energy Hub 缩短固定传送冷却。社区报告的分数可能不同，请以游戏内规则页为准。未了解任务和防守情况前，不臆造通用兵种比例或英雄组合。',
        de: ' Crystal-Clash-Referenz (Community-Guides aus Oktober/November 2026): Wählen Sie die Spielklasse nach Aufgabe — Commander startet Rallyes, Warrior tritt Rallyes bei und verstärkt, Engineer sammelt Kristalle. Priorisieren Sie dauerhafte Zielkontrolle statt einzelner Kills: Zentralgefängnis, Festungen, Kristallgefängnisse und danach Hilfsgebäude gemäß ihrer tatsächlich angezeigten Wirkung. Refinement Center unterstützt Sammlung und Eroberung; Pillbox stärkt Truppen; Bomber Command schwächt Verteidigungen; Energy Hub verkürzt die Abklingzeit des festen Teleports. Prüfen Sie Werte im Regelmenü des Spiels, da Community-Angaben abweichen können. Ohne Ziel und Verteidigung werden keine universellen Truppenverhältnisse oder Heldentrios erfunden.',
        en: 'Crystal Clash reference (community guides updated October/November 2026): choose the in-game class for your job — Commander leads rallies, Warrior joins/reinforces, Engineer gathers crystals. Prioritize sustained objective control over isolated kills: central prison, strongholds, Crystal Prisons, then utility buildings based on their displayed effects. Refinement Center helps gathering and capture time; Pillbox supports troops; Bomber Command weakens a stronghold defense; Energy Hub reduces fixed-point teleport cooldown. Check the in-game Rules panel because community-reported scores can differ. No universal troop ratio or hero trio is invented without knowing the task and defenses.',
      };
      const crystalContext = mode === 'crystal_clash' ? crystalContextByLang[targetLang] : '';

      const turmoilContextByLang: Record<TargetLang, string> = {
        fr: ' Repères Afflux de Tumulte (guide communautaire mis à jour le 31 octobre 2026) : préparez les deux Tourelles Sentinelles; les vagues 10 et 20 attaquent les deux tourelles. Les éliminations obtenues en renforçant une prison alliée rapportent des points personnels; une défense réussie de tourelle contribue aux points d’alliance, pas aux points personnels. Coordonnez les renforts avant d’activer « les troupes dans la prison ne défendent pas » : ne retirez pas vos défenseurs sans couverture alliée suffisante. R4/R5 règlent difficulté et heure; vérifiez les seuils de points dans l’événement. Les vagues spéciales visant les joueurs en ligne et les réglages peuvent varier : suivez le compte à rebours et les messages du jeu.',
        tr: ' Turmoil Surge referansı (31 Ekim 2026 topluluk rehberi): iki Sentry Turret hazırlayın; 10. ve 20. dalgalar iki kuleye saldırır. Müttefik hapishanelerine takviye gönderirken alınan öldürmeler kişisel puan verir; başarılı kule savunması kişisel değil ittifak puanına katkı sağlar. “Hapishanedeki birlikler savunmasın” seçeneğini açmadan önce takviyeleri koordine edin; yeterli müttefik desteği olmadan savunmanızı boşaltmayın. Zorluk ve başlangıç saatini R4/R5 belirler; puan eşiklerini etkinlik ekranından kontrol edin. Çevrimiçi oyuncuları hedefleyen özel dalgalar ve ayarlar değişebilir; oyun sayacı ve bildirimlerini izleyin.',
        ru: ' Ориентиры «Натиск смуты» (руководство сообщества от 31 октября 2026): подготовьте обе сторожевые турели; волны 10 и 20 атакуют обе турели. Убийства при подкреплении союзной тюрьмы дают личные очки; успешная защита турели приносит очки альянсу, а не личные очки. Согласуйте подкрепления до включения параметра «войска в тюрьме не защищают»: не оставляйте свою тюрьму без достаточной союзной защиты. R4/R5 выбирают сложность и время; проверяйте пороги очков в событии. Особые волны для игроков онлайн и настройки могут меняться — следите за таймером и сообщениями игры.',
        zh: ' 混乱激增参考（2026 年 10 月 31 日社区指南）：提前准备两座哨戒炮塔；第 10 和第 20 波会攻击两座炮塔。增援盟友监狱时击败敌人可获得个人积分；成功防守炮塔增加的是联盟积分，而非个人积分。启用“监狱部队不参与防守”前先协调增援；没有足够盟友防守时不要撤空自己的防线。R4/R5 负责选择难度和开始时间；积分门槛以活动界面为准。针对在线玩家的特殊波次和设置可能变化，请留意游戏倒计时和提示。',
        de: ' Turmoil-Surge-Referenz (Community-Guide vom 31. Oktober 2026): Bereiten Sie beide Sentry-Türme vor; Welle 10 und 20 greifen beide Türme an. Kills beim Verstärken verbündeter Gefängnisse geben persönliche Punkte; eine erfolgreiche Turmverteidigung zählt für Allianzpunkte, nicht für persönliche Punkte. Stimmen Sie Verstärkungen ab, bevor Sie „Truppen im Gefängnis verteidigen nicht“ aktivieren; lassen Sie Ihre Verteidigung nicht ohne ausreichende Verbündete zurück. R4/R5 legen Schwierigkeit und Startzeit fest; prüfen Sie Punkteschwellen im Event. Sonderwellen gegen Online-Spieler und Einstellungen können sich ändern — beachten Sie Countdown und Spielmeldungen.',
        en: 'Turmoil Surge reference (community guide updated October 31, 2026): prepare both Sentry Turrets; waves 10 and 20 attack both turrets. Kills while reinforcing allied prisons earn personal points; successful turret defense contributes to alliance points, not personal points. Coordinate reinforcements before enabling “troops in prison do not defend”: do not leave your own defense uncovered without enough allied support. R4/R5 choose difficulty and start time; check point thresholds in the event screen. Special waves targeting online players and settings can change, so follow the in-game countdown and notices.',
      };
      const turmoilContext = mode === 'turmoil_surge' ? turmoilContextByLang[targetLang] : '';

      const goldHunterContextByLang: Record<TargetLang, string> = {
        fr: ' Repères Chasseur d’or (guides communautaires 2026) : gardez vos marches actives et évitez les trajets longs; les vagues de trésors souterrains sont signalées autour de 20 puis 10 minutes restantes — interrompez les activités secondaires pour les trésors proches. Boucle de départ : mineurs cupides, mines encore rentables, collecte des butins, rappel immédiat. Le guide communautaire conseille le plan d’équipement Jungle et la règle des 20 clics pour décider d’une relocalisation; vérifiez les minuteries réelles de l’événement. Choisissez les héros pour vitesse de marche, dégâts contre les ennemis neutres et collecte selon leurs compétences visibles; aucun ratio de troupes universel n’est justifié.',
        tr: ' Gold Hunter referansı (2026 topluluk rehberleri): yürüyüşlerinizi sürekli hareket halinde tutun ve uzun yolculuklardan kaçının; yeraltı hazine dalgaları yaklaşık 20 ve 10 dakika kala duyurulur — yakın hazineler için ikincil işleri bırakın. Başlangıç döngüsü: Greedy Miner, hâlâ kârlı madenler, ganimetleri toplama, hemen geri çağırma. Topluluk rehberi Jungle ekipman planını ve yeniden konumlanma için 20 tıklama kuralını önerir; gerçek etkinlik zamanlayıcısını kontrol edin. Kahramanları görünen becerilerine göre yürüyüş hızı, nötr düşmana hasar ve toplama için seçin; evrensel birlik oranı önerilmez.',
        ru: ' Ориентиры «Охотник за золотом» (руководства сообщества 2026): держите марши в движении и избегайте долгих переходов; волны подземных сокровищ объявляются примерно за 20 и 10 минут до конца — переключайтесь на ближайшие сокровища, прекращая второстепенные действия. Начальный цикл: жадные шахтеры, еще выгодные шахты, сбор трофеев, немедленный возврат. Сообщество рекомендует комплект экипировки «Джунгли» и правило 20 кликов для перемещения базы; сверяйтесь с таймером события. Выбирайте героев по видимым навыкам скорости марша, урона нейтральным врагам и сбора; универсальное соотношение войск не обосновано.',
        zh: ' 黄金猎人参考（2026 年社区指南）：保持行军队列持续行动，避免长距离行军；地下宝藏波次大约在剩余 20 分钟和 10 分钟时出现提示，应暂停次要活动优先争夺附近宝藏。开局循环：攻击 Greedy Miners、清理仍有收益的矿点、收集掉落物并立即召回。社区指南建议使用 Jungle 装备方案，并用“20 次点击”规则判断是否迁城；请以活动倒计时为准。根据可见技能选择行军速度、对中立敌人伤害和采集能力，不推荐没有依据的通用兵种比例。',
        de: ' Gold-Hunter-Referenz (Community-Guides 2026): Halten Sie Ihre Märsche in Bewegung und vermeiden Sie lange Wege; Wellen mit unterirdischen Schätzen werden ungefähr bei 20 und 10 Minuten Restzeit angekündigt — wechseln Sie dann zu nahegelegenen Schätzen und stoppen Sie Nebenaktivitäten. Startschleife: gierige Minenarbeiter, noch lohnende Minen, Beute einsammeln, sofort zurückrufen. Der Community-Guide empfiehlt den Jungle-Ausrüstungsplan und die 20-Klick-Regel für einen Standortwechsel; maßgeblich ist der Event-Timer. Wählen Sie Helden anhand sichtbarer Fähigkeiten für Marschtempo, Schaden gegen neutrale Gegner und Sammlung; ein universelles Truppenverhältnis ist nicht belegt.',
        en: 'Gold Hunter reference (2026 community guides): keep marches moving and avoid long travel; Underground Trove waves are announced around 20 and 10 minutes remaining — pause secondary activities and prioritize nearby troves. Opening loop: attack Greedy Miners, clear mines that still have value, collect drops, recall immediately. The community guide recommends the Jungle gear plan and the 20-click relocation rule; follow the actual event timer. Choose heroes by visible march-speed, neutral-enemy damage and gathering skills; no universal troop ratio is justified.',
      };
      const goldHunterContext = mode === 'gold_hunter' ? goldHunterContextByLang[targetLang] : '';
      const svsContextByLang: Record<TargetLang, string> = {
        fr: ' Repères Honneur Suprême / SvS (guides communautaires 2026, à confirmer dans les règles du jeu) : la préparation comporte 5 jours distincts — J1 Forticlad/construction/recherche; J2 collecte; J3 chasse aux fugitifs et ralliements; J4 entraînement/promotion des troupes; J5 journée mixte. Gardez les matériaux et accélérateurs pour le jour où ils rapportent des points et activez les bonus adaptés avant de lancer les améliorations. La guerre finale dure 4 h selon la référence LOJ XLab : la Prison Suprême est l’objectif central; une occupation continue de 2 h gagne, sinon le temps total d’occupation le plus long départage. Préparez les renforts avant la prise pour éviter une perte immédiate après capture. Le niveau du Bureau peut conditionner les points de guerre : vérifiez la règle affichée et l’écart autorisé avant d’envoyer. Pas de ratio de troupes universel sans connaître le rôle, le niveau et la cible.',
        tr: ' Supreme Honor / SvS referansı (2026 topluluk rehberleri; oyun kurallarından doğrulayın): hazırlık 5 ayrı gündür — 1. gün Forticlad/inşaat/araştırma; 2. gün kaynak toplama; 3. gün firari avı ve ralliler; 4. gün birlik eğitimi/yükseltme; 5. gün karma gün. Malzemeleri ve hızlandırmaları puan kazandırdıkları güne saklayın; geliştirmeye başlamadan önce uygun bonusları etkinleştirin. LOJ XLab referansına göre son savaş 4 saattir: ana hedef Supreme Prison; kesintisiz 2 saat kontrol kazanır, aksi halde toplam kontrol süresi en yüksek olan kazanır. Ele geçirdikten hemen sonra kaybetmemek için takviyeleri önceden hazırlayın. Warden Office seviyesi savaş puanlarını etkileyebilir; birlik göndermeden önce etkinlikteki seviye farkı kuralını kontrol edin. Rol, seviye ve hedef bilinmeden evrensel birlik oranı önerilmez.',
        ru: ' Ориентиры Supreme Honor / SvS (руководства сообщества 2026 года; сверяйтесь с правилами игры): подготовка состоит из 5 разных дней — день 1 Forticlad/строительство/исследования; день 2 сбор ресурсов; день 3 охота на беглецов и рейды; день 4 обучение/повышение уровня войск; день 5 смешанный день. Сохраняйте материалы и ускорения для дня, когда они дают очки, и активируйте нужные бонусы до начала улучшений. По данным LOJ XLab, финальная война длится 4 часа: главная цель — Supreme Prison; непрерывное удержание 2 часа приносит победу, иначе побеждает сторона с наибольшим суммарным временем контроля. Подготовьте подкрепления до захвата, чтобы не потерять тюрьму сразу после взятия. Уровень Warden Office может влиять на военные очки — проверьте правило допустимой разницы уровней в событии. Универсальное соотношение войск без роли, уровня и цели не предлагается.',
        zh: ' Supreme Honor / SvS 参考（2026 年社区指南，需以游戏规则核实）：备战分为 5 个不同日期 — 第 1 天 Forticlad/建筑/研究；第 2 天采集；第 3 天追捕逃犯和集结；第 4 天训练/晋升部队；第 5 天混合日。把材料和加速道具留到能获得对应积分的日期，并在开始升级前启用合适的增益。根据 LOJ XLab 参考，最终战争持续 4 小时：核心目标是 Supreme Prison；连续占领 2 小时即可获胜，否则总占领时间最长的一方获胜。占领前预先安排增援，避免刚占领就被夺回。Warden Office 等级可能影响战争积分，出兵前请核对活动中允许的等级差规则。未了解角色、等级和目标前，不提供通用部队比例。',
        de: ' Supreme-Honor-/SvS-Referenz (Community-Guides 2026; im Spielregelwerk prüfen): Die Vorbereitung hat 5 unterschiedliche Tage — Tag 1 Forticlad/Bau/Forschung; Tag 2 Sammeln; Tag 3 Flüchtlingsjagd und Rallyes; Tag 4 Truppenausbildung/-beförderung; Tag 5 gemischter Tag. Bewahren Sie Materialien und Beschleuniger für den passenden Wertungstag auf und aktivieren Sie Boni vor Beginn der Upgrades. Laut LOJ XLab dauert der finale Krieg 4 Stunden: Das Supreme Prison ist das Hauptziel; 2 Stunden ununterbrochene Kontrolle gewinnen, andernfalls entscheidet die längste gesamte Kontrollzeit. Bereiten Sie Verstärkungen vor der Einnahme vor, damit die Position nicht sofort verloren geht. Das Warden-Office-Level kann Kriegspunkte beeinflussen — prüfen Sie die zulässige Leveldifferenz im Event. Ohne Rolle, Level und Ziel wird kein universelles Truppenverhältnis empfohlen.',
        en: 'Supreme Honor / SvS reference (2026 community guides; verify in the in-game rules): preparation has 5 distinct days — Day 1 Forticlad/building/research; Day 2 gathering; Day 3 fugitive hunting and rallies; Day 4 troop training/promotion; Day 5 mixed day. Save materials and speedups for the day when they score, and activate relevant bonuses before starting upgrades. LOJ XLab reports a 4-hour final war: Supreme Prison is the central objective; holding it continuously for 2 hours wins, otherwise the longest total hold wins. Queue reinforcements before capturing so the prison is not immediately lost. Warden Office level can affect war points; check the allowed level gap in the event rules before sending troops. No universal troop ratio is recommended without role, level and target context.',
      };
      const svsContext = mode === 'svs' ? svsContextByLang[targetLang] : '';
      const curatedSourcesByMode: Record<AdviceEventMode, Array<{ title: string; url: string }>> = {
        trial_cage: [
          { title: 'GuideLOJ — Trial Cage : chefs, troupes et participants', url: 'https://www.guideloj.com/fr/events/alliance/trial-cage-rally' },
          { title: 'LOJ XLab — Cage : leaders, joiners et compétences', url: 'https://lojxlab.com/blog/lands-of-jail-cage-advanced-guide/' },
        ],
        crystal_clash: [
          { title: 'GuideLOJ — Crystal Clash : objectifs et rôles', url: 'https://www.guideloj.com/fr/events/alliance/crystal-clash' },
          { title: 'LOJ XLab — Crystal Clash : points et classes', url: 'https://lojxlab.com/blog/crystal-clash-points-776k-to-64k/' },
        ],
        sandsea_conquest: [
          { title: 'LOJ Wiki — Conquête des sables : règles et phases', url: 'https://lojwiki.com/fr/events/sandsea-conquest/' },
        ],
        svs: [
          { title: 'LOJ Strats — SvS : préparation des cinq jours', url: 'https://lojstrats.com/svs-preparation' },
          { title: 'LOJ XLab — État de SvS : règles et guerre finale', url: 'https://lojxlab.com/svs/state-of-svs/' },
        ],
        turmoil_surge: [
          { title: 'GuideLOJ — Afflux de Tumulte : renforts et tourelles', url: 'https://www.guideloj.com/fr/events/alliance/turmoil-surge' },
        ],
        gold_hunter: [
          { title: 'LOJ Wiki — Chasseur d’or : trésors et équipement', url: 'https://lojwiki.com/guides/gold-hunter/' },
          { title: 'LOJ XLab — Gold Hunter : troves et règle des 20/10 minutes', url: 'https://lojxlab.com/blog/lands-of-jail-gold-hunter-guide/' },
        ],
      };
      const ratioLabels: Record<TargetLang, { title: string; leader: string; joiner: string; standard: string; ada: string; mixed: string; shooters: string; standardNote: string; adaNote: string; joinerNote: string; shootersNote: string }> = {
        fr: { title: 'Ratios de troupes — Cage', leader: 'Chef', joiner: 'Participant', standard: 'Chef sans Ada', ada: 'Chef avec Ada', mixed: 'Participant — troupes mixtes', shooters: 'Participant — Shooters uniquement', standardNote: 'Ratio de départ du guide communautaire actualisé le 4 novembre 2026.', adaNote: 'Vérifiez aussi le nombre réel de Shieldbearers : environ 1 000 selon le guide, pas 1% automatiquement.', joinerNote: 'Option publiée pour le participant ; suivez les consignes du chef de ralliement.', shootersNote: 'Option publiée si vous pouvez envoyer uniquement des Shooters.' },
        tr: { title: 'Birlik oranları — Cage', leader: 'Başlatan', joiner: 'Katılan', standard: 'Ada olmadan lider', ada: 'Ada ile lider', mixed: 'Katılan — karma birlikler', shooters: 'Katılan — yalnızca Shooter', standardNote: '4 Kasım 2026 tarihli topluluk rehberindeki başlangıç oranı.', adaNote: 'Gerçek Shieldbearer sayısını da kontrol edin: rehbere göre yaklaşık 1.000; her zaman otomatik olarak %1 değildir.', joinerNote: 'Katılan için yayımlanan seçenek; ralli liderinin talimatlarına uyun.', shootersNote: 'Yalnızca Shooter gönderebiliyorsanız yayımlanan seçenek.' },
        ru: { title: 'Соотношение войск — Cage', leader: 'Лидер', joiner: 'Участник', standard: 'Лидер без Ada', ada: 'Лидер с Ada', mixed: 'Участник — смешанные войска', shooters: 'Участник — только Shooter', standardNote: 'Стартовое соотношение из руководства сообщества от 4 ноября 2026 года.', adaNote: 'Проверьте фактическое число Shieldbearer: около 1 000 по руководству; это не всегда автоматически 1%.', joinerNote: 'Опубликованный вариант для участника; следуйте указаниям лидера рейда.', shootersNote: 'Вариант из руководства, если можно отправить только Shooter.' },
        zh: { title: '部队比例 — Cage', leader: '发起者', joiner: '加入者', standard: '队长不带 Ada', ada: '队长带 Ada', mixed: '加入者 — 混合部队', shooters: '加入者 — 仅 Shooter', standardNote: '社区指南于 2026 年 11 月 4 日更新的起始比例。', adaNote: '还需核对 Shieldbearer 实际数量：指南建议约 1,000，并非任何情况下都固定等于 1%。', joinerNote: '指南发布的加入者方案；请遵循集结队长的要求。', shootersNote: '如果可以只派出 Shooter，这是指南列出的方案。' },
        de: { title: 'Truppenverhältnisse — Cage', leader: 'Anführer', joiner: 'Beitretender', standard: 'Anführer ohne Ada', ada: 'Anführer mit Ada', mixed: 'Beitretender — gemischte Truppen', shooters: 'Beitretender — nur Shooter', standardNote: 'Ausgangsverhältnis laut Community-Guide vom 4. November 2026.', adaNote: 'Prüfen Sie auch die tatsächliche Zahl der Shieldbearer: laut Guide etwa 1.000, nicht automatisch immer 1%.', joinerNote: 'Veröffentlichte Option für Beitretende; folgen Sie den Vorgaben des Rallye-Anführers.', shootersNote: 'Veröffentlichte Option, wenn Sie ausschließlich Shooter senden können.' },
        en: { title: 'Troop ratios — Cage', leader: 'Leader', joiner: 'Joiner', standard: 'Leader without Ada', ada: 'Leader with Ada', mixed: 'Joiner — mixed troops', shooters: 'Joiner — Shooters only', standardNote: 'Starting ratio from the community guide updated November 4, 2026.', adaNote: 'Also check the actual Shieldbearer count: about 1,000 per the guide, not automatically 1% in every march.', joinerNote: 'Published joiner option; follow the rally leader’s instructions.', shootersNote: 'Published option when you can send Shooters only.' },
      };
      const troopRatioProfiles = mode === 'trial_cage' ? [
        { id: 'leader_standard', role: 'lanceur', roleLabel: ratioLabels[targetLang].leader, label: ratioLabels[targetLang].standard, shieldbearer: 0, bomber: 10, shooter: 90, note: ratioLabels[targetLang].standardNote },
        { id: 'leader_ada', role: 'lanceur', roleLabel: ratioLabels[targetLang].leader, label: ratioLabels[targetLang].ada, shieldbearer: 1, bomber: 9, shooter: 90, note: ratioLabels[targetLang].adaNote },
        { id: 'joiner_mixed', role: 'rejoigneur', roleLabel: ratioLabels[targetLang].joiner, label: ratioLabels[targetLang].mixed, shieldbearer: 0, bomber: 10, shooter: 90, note: ratioLabels[targetLang].joinerNote },
        { id: 'joiner_shooters', role: 'rejoigneur', roleLabel: ratioLabels[targetLang].joiner, label: ratioLabels[targetLang].shooters, shieldbearer: 0, bomber: 0, shooter: 100, note: ratioLabels[targetLang].shootersNote },
      ] : [];
      res.json({
        advice: {
          mode,
          language: targetLang,
          playerRole: playerRole || 'polyvalent_f2p',
          generatedAt: new Date().toISOString(),
          synthese: `${synthPrefix[targetLang]}${leadSummary}`,
          compositionRecommandee: `${compTextByLang[targetLang]}${groundedProfileContext}${eventRoleContextText}${cageAssessment}${cageRallyContext}${sandseaContext}${crystalContext}${turmoilContext}${goldHunterContext}${svsContext}`,
          ...(troopRatioProfiles.length ? { troopRatioTitle: ratioLabels[targetLang].title, troopRatioProfiles } : {}),
          recommandations: liveGuides.slice(0, 4).map((g) => ({
            titre: g.title,
            detail: g.excerpt || g.title,
            // Do not label arbitrary first heroes as targets without a source-backed match.\n            herosCibles: [],
            incertain: false,
            imageUrl: g.imageUrl,
          })),
          informationsIncertaines: [
            ...(heroNames.length === 0 ? [compTextByLang[targetLang]] : []),
            ...(liveGuides.length === 0 ? [liveGuideUnavailableByLang[targetLang]] : []),
          ],
          sources: [
            ...liveGuides.map((g) => ({
              title: g.title,
              url: g.url,
              detectedDate: g.date,
              imageUrl: g.imageUrl,
            })),
            ...curatedSourcesByMode[mode]
              .filter((source) => !liveGuides.some((guide) => guide.url === source.url))
              .map((source) => ({
                title: source.title,
                url: source.url,
                detectedDate: null,
                imageUrl: null,
              })),
          ],
          requetesRecherche: [modeName, targetLang],
        },
      });
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Erreur inconnue lors de la recherche de conseils.';
      console.error('Erreur /api/advice:', err);
      res.status(500).json({
        error: `Impossible de récupérer les conseils pour ce mode : ${message}`,
      });
    }
  });

  interface EnrichedHeroCatalogEntry {
    slug: string;
    nom: string;
    avatarUrl: string | null;
    imageUrl: string | null;
    rarete?: string;
    typeTroupe?: string;
    categorie?: string;
    wikiUrl: string;
  }

  interface EnrichedHeroFullDetails {
    slug: string;
    nom: string;
    titre?: string;
    avatarUrl: string | null;
    imageUrl: string | null;
    rarete?: string;
    typeTroupe?: string;
    categorie?: string;
    saison?: string;
    puissanceMax5Etoiles?: string;
    resumeBio?: string;
    idealPour?: string;
    wikiUrl: string;
    competences: Array<{
      nom: string;
      niveau: string;
      description?: string;
      iconUrl?: string;
      incertain: boolean;
    }>;
  }

  const heroCatalogCache = new Map<string, { at: number; items: EnrichedHeroCatalogEntry[] }>();
  const heroDetailCache = new Map<string, { at: number; data: EnrichedHeroFullDetails }>();

  const decodeWikiEntities = (s: string) =>
    s
      .replace(/&amp;/g, '&')
      .replace(/&quot;/g, '"')
      .replace(/&#39;/g, "'")
      .replace(/&#x27;/g, "'")
      .replace(/&#8217;/g, "'")
      .replace(/&#8211;/g, '–')
      .replace(/\s+/g, ' ')
      .trim();

  function prefixForLang(language: TargetLang): string {
    const map: Record<TargetLang, string> = {
      fr: '/fr',
      tr: '/tr',
      ru: '/ru',
      zh: '/zh-cn',
      de: '/de',
      en: '',
    };
    return map[language] ?? '';
  }

  async function fetchLocalizedHeroCatalog(language: TargetLang): Promise<EnrichedHeroCatalogEntry[]> {
    const cacheKey = language;
    const cached = heroCatalogCache.get(cacheKey);
    if (cached && Date.now() - cached.at < 30 * 60 * 1000) {
      return cached.items;
    }

    const prefix = prefixForLang(language);
    const catalogUrl = `https://lojwiki.com${prefix}/heroes/`;
    try {
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), 8000);
      const resp = await fetch(catalogUrl, {
        signal: controller.signal,
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/126.0.0.0 Safari/537.36',
          Accept: 'text/html,application/xhtml+xml',
        },
      });
      clearTimeout(timer);
      if (!resp.ok) return cached?.items ?? [];
      const html = await resp.text();
      const matches = [...html.matchAll(/<a[^>]+href="([^"]*\/heroes\/([a-z0-9-]+)\/)"[^>]*>([\s\S]*?)<\/a>/gi)];
      const seen = new Set<string>();
      const items: EnrichedHeroCatalogEntry[] = [];
      for (const m of matches) {
        const href = m[1];
        const slug = m[2];
        if (!slug || seen.has(slug)) continue;
        seen.add(slug);
        const inner = m[3];
        const avatar = (inner.match(/https:\/\/img\.lojwiki\.com\/(?:heads|heroes|half)\/[^"'\s)>]+\.webp/i) || [])[0] || null;
        const cleanText = decodeWikiEntities(inner.replace(/<[^>]+>/g, ' '));
        const tokens = cleanText.split(' ').filter(Boolean);
        const rarity = tokens.find((t) => /^(SSR|SR|R)$/i.test(t))?.toUpperCase();
        // Extract name roughly from slug or cleaned text between rarity and troop badges
        const slugTitle = slug
          .split('-')
          .map((w) => (w === 'and' ? '&' : w.charAt(0).toUpperCase() + w.slice(1)))
          .join(' ');
        const fullUrl = href.startsWith('http') ? href : `https://lojwiki.com${href}`;
        items.push({
          slug,
          nom: slugTitle,
          avatarUrl: avatar,
          imageUrl: avatar,
          rarete: rarity,
          typeTroupe: cleanText.replace(rarity || '', '').trim() || undefined,
          wikiUrl: fullUrl,
        });
      }
      heroCatalogCache.set(cacheKey, { at: Date.now(), items });
      return items;
    } catch {
      return cached?.items ?? [];
    }
  }

  async function fetchHeroWikiDetailsByName(
    heroName: string,
    language: TargetLang
  ): Promise<EnrichedHeroFullDetails | null> {
    const normalizedInput = heroName
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .replace(/&/g, 'and')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '');
    if (!normalizedInput) return null;

    const catalog = await fetchLocalizedHeroCatalog(language);
    const matchedCatalog =
      catalog.find((c) => c.slug === normalizedInput) ||
      catalog.find((c) => c.slug.includes(normalizedInput) || normalizedInput.includes(c.slug)) ||
      catalog.find((c) => c.nom.toLowerCase().includes(heroName.toLowerCase()));

    const slug = matchedCatalog?.slug || normalizedInput;
    const cacheKey = `${language}:${slug}`;
    const cached = heroDetailCache.get(cacheKey);
    if (cached && Date.now() - cached.at < 30 * 60 * 1000) {
      return cached.data;
    }

    const prefix = prefixForLang(language);
    const heroUrl = `https://lojwiki.com${prefix}/heroes/${slug}/`;
    try {
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), 8000);
      const resp = await fetch(heroUrl, {
        signal: controller.signal,
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/126.0.0.0 Safari/537.36',
          Accept: 'text/html,application/xhtml+xml',
        },
      });
      clearTimeout(timer);
      if (!resp.ok) return null;
      const html = await resp.text();

      const titleMatch = html.match(/<h1[^>]*>([\s\S]*?)<\/h1>/i);
      const nom = titleMatch ? decodeWikiEntities(titleMatch[1].replace(/<[^>]+>/g, '')) : heroName;
      const subMatch = html.match(/<p[^>]+class="[^"]*sub[^"]*"[^>]*>([\s\S]*?)<\/p>/i);
      const titre = subMatch ? decodeWikiEntities(subMatch[1].replace(/<[^>]+>/g, '')) : undefined;

      const halfMatch = html.match(/<img[^>]+class="[^"]*half[^"]*"[^>]+src="(https:\/\/img\.lojwiki\.com\/[^"]+)"/i) ||
        html.match(/https:\/\/img\.lojwiki\.com\/half\/[^"'\s)>]+\.webp/i);
      const avatarMatch = html.match(/https:\/\/img\.lojwiki\.com\/(?:heroes|heads)\/[^"'\s)>]+\.webp/i);
      const imageUrl = halfMatch ? (halfMatch[1] || halfMatch[0]) : (avatarMatch ? avatarMatch[0] : matchedCatalog?.avatarUrl ?? null);
      const avatarUrl = avatarMatch ? avatarMatch[0] : matchedCatalog?.avatarUrl ?? imageUrl;

      const badges = [...html.matchAll(/<span[^>]+class="[^"]*badge[^"]*"[^>]*>([\s\S]*?)<\/span>/gi)].map((b) =>
        decodeWikiEntities(b[1].replace(/<[^>]+>/g, ' '))
      );
      const rarete = badges.find((b) => /^(SSR|SR|R)$/i.test(b)) || matchedCatalog?.rarete;
      const saison = badges.find((b) => /(?:saison|season|sezon|сезон|赛季)\s*\d+/i.test(b));
      const nonRarityBadges = badges.filter((b) => b && b !== rarete && b !== saison);
      const typeTroupe = nonRarityBadges[0];
      const categorie = nonRarityBadges[1];

      const introMatch = html.match(/<p[^>]+class="[^"]*intro[^"]*"[^>]*>([\s\S]*?)<\/p>/i);
      const resumeBio = introMatch ? decodeWikiEntities(introMatch[1].replace(/<[^>]+>/g, ' ')) : undefined;

      // Facts cards (Recruit, 5★ shards, 5★ Power)
      const factsMatches = [...html.matchAll(/<div[^>]+class="card"[^>]*><div[^>]*>([^<]+)<\/div><strong[^>]*>([^<]+)<\/strong>/gi)];
      const powerFact = factsMatches.find((f) => /power|puissance|güç|сила|战力|macht/i.test(f[1]));
      const puissanceMax5Etoiles = powerFact ? decodeWikiEntities(powerFact[2]) : undefined;

      // Ideal / Verdicts tags
      const verdictMatches = [...html.matchAll(/<span[^>]+class="tag[^"]*"[^>]*>([^<]+)<\/span>(?:<span[^>]*>([^<]+)<\/span>)?/gi)];
      const idealPour = verdictMatches
        .slice(0, 4)
        .map((v) => decodeWikiEntities([v[1], v[2]].filter(Boolean).join(' · ')))
        .filter(Boolean)
        .join(' | ') || undefined;

      // Skills (<article class="card skill">)
      const skillBlocks = [...html.matchAll(/<article[^>]+class="[^"]*\bskill\b[^"]*"[^>]*>([\s\S]*?)<\/article>/gi)];
      const competences = skillBlocks.slice(0, 8).map((sb) => {
        const blockHtml = sb[1];
        const skIcon = (blockHtml.match(/https:\/\/img\.lojwiki\.com\/skills\/[^"'\s)>]+\.webp/i) || [])[0];
        const skNameMatch = blockHtml.match(/<strong[^>]*>([\s\S]*?)<\/strong>/i);
        const skName = skNameMatch
          ? decodeWikiEntities(skNameMatch[1].replace(/<[^>]+>/g, ''))
          : 'Compétence';
        const descMatches = [...blockHtml.matchAll(/<p[^>]+class="desc"[^>]*>([\s\S]*?)<\/p>/gi)];
        const maxDesc = descMatches.length > 0
          ? decodeWikiEntities(descMatches[descMatches.length - 1][1].replace(/<[^>]+>/g, ' '))
          : undefined;
        return {
          nom: skName.replace(/^\d+\.\s*/, ''),
          niveau: descMatches.length > 0 ? `Niv. 1–${descMatches.length}` : 'Max',
          description: maxDesc,
          iconUrl: skIcon,
          incertain: false,
        };
      });

      const details: EnrichedHeroFullDetails = {
        slug,
        nom,
        titre,
        avatarUrl,
        imageUrl,
        rarete,
        typeTroupe,
        categorie,
        saison,
        puissanceMax5Etoiles,
        resumeBio,
        idealPour,
        wikiUrl: heroUrl,
        competences,
      };
      heroDetailCache.set(cacheKey, { at: Date.now(), data: details });
      return details;
    } catch {
      return null;
    }
  }

  app.get('/api/heroes/catalog', async (req, res) => {
    const langRaw = String(req.query.lang || 'fr') as TargetLang;
    const targetLang: TargetLang = ['fr', 'tr', 'ru', 'zh', 'de', 'en'].includes(langRaw) ? langRaw : 'fr';
    const items = await fetchLocalizedHeroCatalog(targetLang);
    res.json({ language: targetLang, heroes: items });
  });

  app.post('/api/heroes/enrich', async (req, res) => {
    try {
      const { names, language } = req.body as { names?: string[]; language?: TargetLang };
      const targetLang: TargetLang =
        language && ['fr', 'tr', 'ru', 'zh', 'de', 'en'].includes(language) ? language : 'fr';
      const list = Array.isArray(names) ? names.slice(0, 30) : [];
      const enriched = await Promise.all(
        list.map((name) => fetchHeroWikiDetailsByName(name, targetLang))
      );
      res.json({
        language: targetLang,
        details: enriched.filter(Boolean),
      });
    } catch (err: unknown) {
      res.status(500).json({ error: err instanceof Error ? err.message : 'Erreur enrichissement héros.' });
    }
  });

  if (process.env.NODE_ENV !== 'production') {
    const { createServer: createViteServer } = await import('vite');
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.resolve(__dirname, 'dist');
    app.use(express.static(distPath));
    app.get('*', (_req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Lands of Jail Assistant écoute sur http://0.0.0.0:${PORT}`);
  });
}

startServer();
