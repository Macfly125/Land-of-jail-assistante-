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
          niveauTexte: string;
          rarete?: string;
          roleHero?: string;
          incertain?: boolean;
          competences?: Array<{ nom: string; niveau: string; incertain?: boolean }>;
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
      const liveGuides = await fetchLiveLojGuides(mode, heroNames, targetLang);

      if (liveGuides.length === 0) {
        res.status(502).json({
          error: 'Aucun guide Internet accessible pour le moment. Vérifiez votre connexion.',
        });
        return;
      }

      const roleLabel = playerRoleLabel || playerRole || 'Polyvalent / F2P';
      const leadSummary = liveGuides
        .slice(0, 2)
        .map((g) => g.excerpt)
        .filter(Boolean)
        .join(' ');

      const compTextByLang: Record<TargetLang, string> = {
        fr:
          heroNames.length > 0
            ? `Héros du compte intégrés : ${heroNames.join(', ')} (rôle : ${roleLabel}).`
            : `Aucun héros encore scanné : utilisez vos meilleurs Shieldbearers devant et Shooters derrière (${roleLabel}).`,
        tr:
          heroNames.length > 0
            ? `Tespit edilen kahramanlar: ${heroNames.join(', ')} (rol: ${roleLabel}).`
            : `Henüz kahraman taranmadı: ön safta Shieldbearers, arkada Shooters kullanın.`,
        ru:
          heroNames.length > 0
            ? `Обнаруженные герои: ${heroNames.join(', ')} (роль: ${roleLabel}).`
            : `Герои ещё не отсканированы: ставьте Shieldbearers вперёд, Shooters назад.`,
        zh:
          heroNames.length > 0
            ? `已加入账号英雄：${heroNames.join('、')}（定位：${roleLabel}）。`
            : `暂未扫描到英雄：前排推荐 Shieldbearers，后排配置 Shooters。`,
        de:
          heroNames.length > 0
            ? `Erkannte Helden: ${heroNames.join(', ')} (Rolle: ${roleLabel}).`
            : `Noch keine Helden gescannt: Shieldbearers vorne, Shooters hinten einsetzen.`,
        en:
          heroNames.length > 0
            ? `Account heroes included: ${heroNames.join(', ')} (role: ${roleLabel}).`
            : `No heroes scanned yet: pair frontline Shieldbearers with backline Shooters (${roleLabel}).`,
      };

      const synthPrefix: Record<TargetLang, string> = {
        fr: `${modeName} · Rôle : ${roleLabel}${extraQuestion ? ` · Question : ${extraQuestion}` : ''}. `,
        tr: `${modeName} · Rol: ${roleLabel}. `,
        ru: `${modeName} · Роль: ${roleLabel}. `,
        zh: `${modeName} · 定位：${roleLabel}。`,
        de: `${modeName} · Rolle: ${roleLabel}. `,
        en: `${modeName} · Role: ${roleLabel}. `,
      };

      res.json({
        advice: {
          mode,
          language: targetLang,
          playerRole: playerRole || 'polyvalent_f2p',
          generatedAt: new Date().toISOString(),
          synthese: `${synthPrefix[targetLang]}${leadSummary}`,
          compositionRecommandee: compTextByLang[targetLang],
          recommandations: liveGuides.slice(0, 4).map((g) => ({
            titre: g.title,
            detail: g.excerpt || g.title,
            herosCibles: heroNames.slice(0, 3),
            incertain: false,
            imageUrl: g.imageUrl,
          })),
          informationsIncertaines:
            heroNames.length === 0
              ? [compTextByLang[targetLang]]
              : [],
          sources: liveGuides.map((g) => ({
            title: g.title,
            url: g.url,
            detectedDate: g.date,
            imageUrl: g.imageUrl,
          })),
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
