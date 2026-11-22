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

  // Endpoint simple pour vérifier que le serveur Express (et non seulement le site statique) répond.
  app.get('/api/health', (_req, res) => {
    res.json({ ok: true, service: 'lands-of-jail-assistant-api' });
  });

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

      const promptText = `Tu es un assistant d'analyse de captures d'écran pour le jeu mobile "Lands of Jail".
Analyse UNIQUEMENT ce qui est réellement visible sur cette capture d'écran partagée par le joueur.
Règles strictes :
1. Réponds entièrement en français.
2. N'invente JAMAIS de valeurs, niveaux, compétences ou ressources absents ou illisibles.
3. Si une information est floue, coupée, partiellement masquée, ambiguë ou incertaine (par exemple un icône sans nom, un chiffre tronqué, un effet de compétence non affiché), marque impérativement "incertain": true et explique précisément pourquoi dans "raisonIncertitude", et ajoute une entrée dans "champsIncertains".
4. Identifie le type d'écran : "profil", "heros", "ressources", "trial_cage", "crystal_clash", ou "autre".
5. Extrais si visibles :
   - La puissance du compte (texte exact affiché et estimation numérique entière si non ambiguë, sinon null) + niveau QG/Prison et VIP si visibles.
   - Les ressources visibles (Cash/Or, Vivres, Bois, Acier, Pétrole, Gemmes/Diamants, Points d'arène, Hyperalliage/Hyperalloy, Noyaux Forticlad et Noyaux Forticlad avancés) avec leur quantité exacte. Pour chaque icône, lis le libellé et la quantité réellement visibles ; ne confonds pas Hyperalliage et Noyaux Forticlad.
   - Les héros visibles : nom, niveau (entier ou null si illisible), rareté/étoiles si visibles, rôle estimé ou affiché, et toutes les compétences visibles avec leur niveau (ex: "Niv. 3", "Max", "Inconnu") et description courte si visible.
   - Le robot / compagnon / unité de soutien visible sur l'écran : nom exact, niveau, puissance/rareté si affichés, compétences ou bonus visibles. Ne confonds jamais robot et héros.
   - Si aucun robot n'est visible, retourne robots: []. Ne complète jamais avec un robot supposé ni avec des données provenant d'un autre écran.
6. Contexte joueur optionnel : rôle="${playerRole || 'non précisé'}", note="${note || 'aucune'}".
7. Pour chaque héros, lis les informations dans sa propre carte/zone visuelle. N'attribue jamais un niveau, une étoile ou une compétence à partir d'une autre carte ou d'une ligne voisine.
8. Si le nom n'est pas lisible avec certitude, conserve le texte observé et marque le héros incertain ; ne le remplace pas par le nom d'un héros supposé.
9. Ne complète pas les niveaux de compétences avec des niveaux maximum provenant d'un wiki. Les compétences visibles sur la capture et les informations générales du catalogue sont deux sources distinctes.
10. Ne fusionne jamais deux héros et ne déduplique pas des cartes distinctes simplement parce que leurs noms se ressemblent.
11. Pour les compétences, analyse chaque ligne séparément : associe le nom, l'icône et le niveau uniquement lorsqu'ils sont visuellement reliés dans la même carte. Ne déduis jamais un niveau à partir de la couleur, de la puissance du héros, de son nombre d'étoiles ou d'une autre capture.
12. Respecte l'ordre visuel des compétences (de gauche à droite puis de haut en bas) et conserve chaque compétence séparément, même si deux icônes se ressemblent.
13. Si tu vois une icône de compétence mais que son nom ou son niveau ne peut pas être lu, conserve-la avec un libellé descriptif neutre (ex. « Compétence 1 — nom illisible ») et marque-la incertaine. Ne lui attribue pas un nom issu de ta connaissance du jeu.
14. « Max » n'est accepté que si le libellé MAX / MAXIMUM est réellement visible. Un niveau absent doit être « Inconnu » et incertain=true.
15. Ne transforme jamais une supposition en lecture certaine : en cas de doute, préfère un champ vide ou « Inconnu » avec une raison précise.`;

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
              robots: {
                type: Type.ARRAY,
                description: 'Robots ou compagnons visibles uniquement sur cette capture. Tableau vide si aucun robot visible.',
                items: {
                  type: Type.OBJECT,
                  properties: {
                    nom: { type: Type.STRING },
                    niveau: { type: Type.STRING },
                    puissance: { type: Type.STRING },
                    rarete: { type: Type.STRING },
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
                    incertain: { type: Type.BOOLEAN },
                    raisonIncertitude: { type: Type.STRING },
                  },
                  required: ['nom', 'incertain', 'competences'],
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
              'robots',
              'champsIncertains',
            ],
          },
        },
      });

      const rawText = (response.text ?? '{}').trim();
      const parsed = JSON.parse(rawText);

      // Garde-fou déterministe : un nom ou un niveau de compétence absent ne peut jamais être déclaré fiable.
      if (Array.isArray(parsed?.heros)) {
        parsed.heros = parsed.heros.map((hero: Record<string, unknown>) => {
          const heroName = typeof hero.nom === 'string' ? hero.nom.trim() : '';
          const heroLevelText = typeof hero.niveauTexte === 'string' ? hero.niveauTexte.trim() : '';
          const heroLevelUnknown = !heroLevelText || /^(inconnu|non lu|illisible|unknown|n\/?a|-)$/i.test(heroLevelText);
          const heroReasons = typeof hero.raisonIncertitude === 'string' ? hero.raisonIncertitude : '';
          const skills = Array.isArray(hero.competences) ? hero.competences : [];
          const normalizedSkills = skills.map((skill: Record<string, unknown>, index: number) => {
            const skillName = typeof skill.nom === 'string' ? skill.nom.trim() : '';
            const skillLevel = typeof skill.niveau === 'string' ? skill.niveau.trim() : '';
            const missingName = !skillName;
            const missingLevel = !skillLevel || /^(inconnu|non lu|illisible|unknown|n\/?a|-)$/i.test(skillLevel);
            const wasUncertain = skill.incertain === true;
            const reason = typeof skill.raisonIncertitude === 'string' ? skill.raisonIncertitude : '';
            return {
              ...skill,
              nom: skillName || `Compétence ${index + 1} — nom illisible`,
              niveau: skillLevel || 'Inconnu',
              incertain: wasUncertain || missingName || missingLevel,
              raisonIncertitude: reason || (missingName
                ? 'Nom de compétence non lisible sur la capture'
                : missingLevel
                  ? 'Niveau de compétence non lisible sur la capture'
                  : undefined),
            };
          });
          return {
            ...hero,
            nom: heroName || 'Héros — nom illisible',
            incertain: hero.incertain === true || !heroName || heroLevelUnknown,
            raisonIncertitude: heroReasons || (!heroName
              ? 'Nom du héros non lisible sur la capture'
              : heroLevelUnknown
                ? 'Niveau du héros non lisible sur la capture'
                : undefined),
            competences: normalizedSkills,
          };
        });
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
              const existingSkills = Array.isArray(h.competences) ? (h.competences as Array<Record<string, unknown>>) : [];
              // Le wiki enrichit uniquement les compétences effectivement lues.
              // Ne jamais injecter toute la liste théorique dans les compétences du joueur.
              h.competences = existingSkills.map((sk) => {
                const skName = String(sk.nom || '').trim().toLowerCase();
                const matched = wikiInfo.competences.find(
                  (ws) => ws.nom.trim().toLowerCase() === skName
                );
                return {
                  ...sk,
                  description: (typeof sk.description === 'string' && sk.description) || matched?.description,
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
    | 'gold_hunter'
    | 'gorilla_legion';

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
      gorilla_legion: ['/events/gorilla-legion/', '/events/'],
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
      gorilla_legion: 'gorilla legion',
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

    const guideLojCollected: LiveGuideItem[] = [];
    const guideLojUrls = mode === 'trial_cage'
      ? [
          'https://guideloj.com/en/events/alliance/trial-cage-rally-guide',
          'https://www.guideloj.com/fr/tips/fight/troop-guide-shieldbearer-bombardiers-shooters',
        ]
      : [];
    await Promise.all(guideLojUrls.map(async (pageUrl) => {
      try {
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), 7000);
        const resp = await fetch(pageUrl, {
          signal: controller.signal,
          headers: { Accept: 'text/html', 'User-Agent': 'LandsOfJailAssistant/1.5' },
        });
        clearTimeout(timer);
        if (!resp.ok) return;
        const html = await resp.text();
        const title = html.match(/<h1[^>]*>([\s\S]*?)<\/h1>/i)?.[1]
          || html.match(/<title[^>]*>([\s\S]*?)<\/title>/i)?.[1]
          || 'GuideLOJ';
        const mainHtml = html.match(/<(?:main|article)[^>]*>([\s\S]*?)<\/(?:main|article)>/i)?.[1] || html;
        const excerpt = stripHtml(mainHtml).slice(0, 4200);
        guideLojCollected.push({
          title: stripHtml(title),
          url: pageUrl,
          date: new Date().toISOString().slice(0, 10),
          excerpt,
          imageUrl: null,
          langMatch: pageUrl.includes('/fr/'),
        });
      } catch {
        // Une source GuideLOJ indisponible ne bloque pas le coach.
      }
    }));

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

    return [...wikiGuides, ...guideLojCollected, ...lojxlabCollected].filter((item, index, all) => all.findIndex((candidate) => candidate.url === item.url) === index).slice(0, 12);
  }

  app.post('/api/advice', async (req, res) => {
    try {
      const { mode, language, playerRole, playerRoleLabel, eventPosition, gorillaName, heroes, robots, extraQuestion } = req.body as {
        mode?: AdviceEventMode;
        language?: TargetLang;
        playerRole?: string;
        playerRoleLabel?: string;
        eventPosition?: 'leader' | 'joiner' | 'solo';
        gorillaName?: string;
        robots?: Array<{
          nom: string;
          niveau?: string;
          puissance?: string;
          rarete?: string;
          incertain?: boolean;
          competences?: Array<{ nom: string; niveau: string; description?: string; incertain?: boolean }>;
        }>;
        heroes?: Array<{
          nom: string;
          niveau?: number | null;
          niveauTexte: string;
          rarete?: string;
          etoiles?: string;
          roleHero?: string;
          typeTroupe?: string;
          puissanceHero?: string;
          incertain?: boolean;
          competences?: Array<{ nom: string; niveau: string; description?: string; incertain?: boolean }>;
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
        'gorilla_legion',
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
          gorilla_legion: 'Gorilla Legion',
        },
        tr: {
          trial_cage: 'Deneme Kafesi',
          crystal_clash: 'Kristal Çatışması',
          sandsea_conquest: 'Kum Denizi Fethi',
          svs: 'SvS / Yüce Onur',
          turmoil_surge: 'Kargaşa Dalgası',
          gold_hunter: 'Altın Avcısı',
          gorilla_legion: 'Goril Lejyonu',
        },
        ru: {
          trial_cage: 'Испытание клетки',
          crystal_clash: 'Кристальная схватка',
          sandsea_conquest: 'Завоевание Песчаного моря',
          svs: 'SvS / Высшее отличие',
          turmoil_surge: 'Волна смуты',
          gold_hunter: 'Охотник за золотом',
          gorilla_legion: 'Легион горилл',
        },
        zh: {
          trial_cage: '终极试炼',
          crystal_clash: '水晶争夺战',
          sandsea_conquest: '沙海征服',
          svs: '跨服战 / 至高荣耀',
          turmoil_surge: '动乱危机',
          gold_hunter: '黄金猎人',
          gorilla_legion: '大猩猩军团',
        },
        de: {
          trial_cage: 'Trial Cage',
          crystal_clash: 'Kristallkampf',
          sandsea_conquest: 'Sandmeer-Eroberung',
          svs: 'SvS / Höchste Ehre',
          turmoil_surge: 'Turmoil Surge',
          gold_hunter: 'Goldjäger',
          gorilla_legion: 'Gorilla-Legion',
        },
        en: {
          trial_cage: 'Trial Cage',
          crystal_clash: 'Crystal Clash',
          sandsea_conquest: 'Sandsea Conquest',
          svs: 'SvS / Supreme Honor',
          turmoil_surge: 'Turmoil Surge',
          gold_hunter: 'Gold Hunter & Sprint',
          gorilla_legion: 'Gorilla Legion',
        },
      };

      const modeName = modeNamesByLang[targetLang][mode];
      const heroNames = (heroes ?? []).map((h) => h.nom).filter(Boolean);
      const liveGuides = await fetchLiveLojGuides(mode, heroNames, targetLang);

      const communityResearchByMode: Record<AdviceEventMode, string> = {
        trial_cage: `RECHERCHE COMMUNAUTAIRE TRIAL CAGE — comparer les sources et ne pas masquer les divergences :
GuideLOJ (mis à jour le 4 novembre 2026) propose, pour saisons 1–7, des ratios de départ : leader sans Ada 0/10/90, leader avec Ada 1/9/90, participant mixte 0/10/90, participant shooters uniquement 0/0/100 (Shield/Bomber/Shooter). Vérifier le nombre réel de Shield avec Ada (environ 1 000 dans la référence), et distinguer leader et participant.
LOJ XLab (guide observé, 5 octobre 2026) décrit un système différent : leader = 3 héros et toutes leurs compétences de guerre + statistiques ; participants = première compétence de guerre du premier héros, seules les 4 meilleures compétences participantes sont retenues, doublons de bonus potentiellement gaspillés. Il indique que les statistiques des participants et leur type de troupe n'influencent pas directement le score. Il privilégie des bonus offensifs distincts.
Ces descriptions ne concordent pas parfaitement sur le fonctionnement des participants. Présente les ratios comme points de départ communautaires, pas comme loi universelle. Signale explicitement le désaccord sur la sélection des compétences et demande de confirmer par version du jeu, rapport de combat et tests de l'alliance. Le critère du joueur reste les dégâts infligés : ne valorise pas la défense, les PV ou les soins sans preuve d'un effet offensif direct.`,
        crystal_clash: `Pour Crystal Clash, utilise les guides d'événement récupérés en direct. Distingue attaque, défense, contrôle des objectifs, durée de présence et points. Ne transpose pas les ratios de Trial Cage.`,
        sandsea_conquest: `Pour Sandsea Conquest, utilise uniquement les règles et guides propres à cet événement récupérés en direct. Distingue mobilité, contrôle, objectifs et combat selon les règles observées. Ne transpose pas les ratios de Trial Cage.`,
        svs: `Pour SvS / Supreme Honor, distingue préparation, jours de score, combat et récupération. Vérifie les calendriers et règles actuels dans les sources récupérées ; ne conseille pas de dépenser des ressources sans relier cela au jour et à l'objectif.`,
        turmoil_surge: `Pour Turmoil Surge, utilise les guides et règles spécifiques récupérés en direct. Distingue les objectifs et récompenses réels ; ne transpose pas les formations d'un autre événement.`,
        gold_hunter: `Pour Gold Hunter & Sprint, utilise les guides spécifiques récupérés en direct. Prends en compte les marches, les déplacements, les coffres et le timing uniquement si la source les confirme ; ne transpose pas les ratios de combat d'un autre événement.`,
        gorilla_legion: `GORILLA LEGION — boss individuel, classement basé sur le meilleur coup et non un ralliement (confirmer les règles sur l'écran du joueur).
Le calendrier publié par LOJ Strats associe lundi Space/Satellite, mardi Wise/Technology, mercredi Cyber/Power Armor, jeudi Primal/Tome, vendredi Armed/Hero Equipment, samedi Treasure/Trove Collection, dimanche Gorilla Warlord/High Chieftain avec les six bonus. Attention : LOJ XLab affiche un ordre différent pour mercredi/jeudi (Primal et Cyber). Ne déduis donc jamais le gorille du seul jour de la semaine : demande ou utilise le nom et le bonus visibles dans la capture du jeu.
Ratios de départ : GuideLOJ propose 0/15/85 (Shield/Bomber/Shooter) comme base gorille ; LOJ Strats donne des ratios par jour (Space 0/20/80, Wise 0/10/90, Cyber 0/10/90, Primal 0/20/80, Armed 0/15/85, Treasure 0/10/90, Warlord 0/10/90). LOJ XLab publie d'autres ratios et reconnaît que les sources communautaires divergent. Présente-les comme essais, jamais comme optimum garanti. Teste aussi 0/0/100 lorsque le joueur peut remplir sa marche.
Priorité : bonus du jour réellement actifs, compétences de héros, équipement exclusif et modules/armures pertinents. Pour Cyber, la source XLab rapporte un bonus Power Armor +1000% conditionné à l'activation des nœuds Shooter/Bomber ; présente-le comme observation à vérifier en jeu, pas comme règle certaine.
Protocole de test : garder héros, niveaux de compétences, équipement et nombre total de troupes identiques ; ne changer que le ratio, enregistrer les dégâts par escouade et répéter plusieurs attaques. Le classement retient le meilleur coup selon les guides consultés. Ne pas mélanger les consignes de Trial Cage ni de ralliement.`,

      };
      const communityResearchSources = mode === 'gorilla_legion'
        ? [
            { title: 'LOJ Strats — Gorilla Legion : calendrier et ratios quotidiens', url: 'https://lojstrats.com/gorilla', date: '2026-11-22' },
            { title: 'LOJ XLab — Gorilla Legion : tests et divergences de ratios', url: 'https://lojxlab.com/gorilla-legion/', date: '2026-11-22' },
            { title: 'GuideLOJ — ratios de troupes pour boss', url: 'https://www.guideloj.com/fr/tips/fight/troop-guide-shieldbearer-bombardiers-shooters', date: '2026-11-04' },
            { title: 'LOJ Wiki — guides et événements', url: 'https://lojwiki.com/guides/', date: '2026-11-22' },
          ]
        : mode === 'trial_cage'
        ? [
            { title: 'GuideLOJ — Trial Cage : chefs, troupes et participants', url: 'https://www.guideloj.com/fr/events/alliance/trial-cage-rally', date: '2026-11-04' },
            { title: 'GuideLOJ — ratios de troupes', url: 'https://www.guideloj.com/fr/tips/fight/troop-guide-shieldbearer-bombardiers-shooters', date: '2026-11-04' },
            { title: 'LOJ XLab — CAGE Advanced Guide', url: 'https://lojxlab.com/blog/lands-of-jail-cage-advanced-guide/', date: '2026-10-05' },
            { title: 'LOJ Wiki — guides et événements', url: 'https://lojwiki.com/fr/', date: '2026-11-22' },
          ]
        : [
            { title: 'LOJ Wiki — guides et événements', url: 'https://lojwiki.com/fr/', date: '2026-11-22' },
            { title: 'LOJ XLab — guides et calendrier', url: 'https://lojxlab.com/', date: '2026-11-22' },
            { title: 'GuideLOJ — centre de guides', url: 'https://guideloj.com/fr/tips', date: '2026-11-04' },
          ];
      const roleLabel = playerRoleLabel || playerRole || 'Polyvalent / F2P';
      const validEventPositions = ['leader', 'joiner', 'solo'] as const;
      const selectedEventPosition = mode === 'gorilla_legion' ? 'solo' : (eventPosition && validEventPositions.includes(eventPosition) ? eventPosition : 'joiner');
      const eventPositionLabels: Record<typeof selectedEventPosition, string> = {
        leader: 'Leader / lanceur de ralliement',
        joiner: 'Participant / renfort',
        solo: 'Action ou attaque solo',
      };
      const eventPositionLabel = eventPositionLabels[selectedEventPosition];
      const validGorillas = ['Space Gorilla', 'Wise Gorilla', 'Cyber Gorilla', 'Primal Gorilla', 'Armed Gorilla', 'Treasure Gorilla', 'Gorilla Warlord / High Chieftain'];
      const selectedGorilla = gorillaName && validGorillas.includes(gorillaName) ? gorillaName : ''; 
      const researchNotes = communityResearchByMode[mode];
      const leadSummary = liveGuides
        .slice(0, 4)
        .map((g) => g.excerpt)
        .filter(Boolean)
        .join(' ');

      const robotRoster = (robots ?? []).map((robot) => ({
        nom: robot.nom,
        niveau: robot.niveau ?? 'Non capturé',
        puissance: robot.puissance ?? 'Non capturée',
        rarete: robot.rarete ?? 'Inconnue',
        incertain: Boolean(robot.incertain),
        competences: (robot.competences ?? []).map((skill) => ({
          nom: skill.nom,
          niveau: skill.niveau,
          description: skill.description ?? 'Description non disponible',
          incertain: Boolean(skill.incertain),
        })),
      }));

      const roster = (heroes ?? []).map((h) => ({
        nom: h.nom,
        niveau: h.niveau ?? null,
        niveauTexte: h.niveauTexte,
        rarete: h.rarete ?? 'Inconnue',
        etoiles: h.etoiles ?? 'Inconnues',
        role: h.roleHero ?? 'Non identifié',
        typeTroupe: h.typeTroupe ?? 'Non identifié',
        puissance: h.puissanceHero ?? 'Non capturée',
        incertain: Boolean(h.incertain),
        competences: (h.competences ?? []).map((skill) => ({
          nom: skill.nom,
          niveau: skill.niveau,
          description: skill.description ?? 'Description non disponible',
          incertain: Boolean(skill.incertain),
        })),
      }));

      const cageRules = mode === 'trial_cage'
        ? `RÈGLES SPÉCIFIQUES TRIAL CAGE : le critère déterminant est le total de dégâts infligés à l'adversaire. Priorise les héros, compétences, bonus et synergies qui augmentent directement les dégâts (attaque, létalité, dégâts de compétence, réduction de défense ennemie, amplification des dégâts, attaques de zone). Ne valorise pas les bonus de PV, défense, soins ou réduction des dégâts subis s'ils n'augmentent pas les dégâts infligés dans ce mode. Ne recommande pas un héros uniquement parce qu'il est tank ou support défensif. Explique les synergies et l'ordre de priorité.`
        : mode === 'gorilla_legion'
          ? `RÈGLES GORILLA LEGION : événement individuel de dégâts, pas un ralliement. Gorille sélectionné par le joueur : ${selectedGorilla || 'NON CONFIRMÉ'}. Si non confirmé, ne devine jamais lequel est actif et demande une capture ou confirmation. Adapte les conseils au gorille choisi et au bonus réellement affiché. Analyse les compétences de héros, équipement exclusif et modules/armures liés au bonus du jour. Les ratios communautaires sont contradictoires : donne un point de départ et un test comparatif, pas un optimum garanti.`
          : `Pour cet événement, distingue les objectifs réels (dégâts, contrôle, défense, occupation, collecte ou points) et sélectionne les héros/compétences en conséquence. Ne transpose pas automatiquement les critères de Trial Cage aux autres modes.`;

      const ai = getAiClient();
      const tacticalResponse = await ai.models.generateContent({
        model: 'gemini-4-hydrogen',
        contents: `Tu es un coach tactique expert de Lands of Jail. Construis une recommandation personnalisée pour l'événement ${modeName}.
Rôle général du compte : ${roleLabel}.
Gorille choisi (si Gorilla Legion) : ${selectedGorilla || 'non confirmé'}. Position choisie pour cet événement : ${eventPositionLabel}. Adapte les héros, compétences, robots, ratio et conseils à cette position ; ne mélange pas les règles du leader, du participant et du solo.
Données du compte — héros possédés :
${JSON.stringify(roster)}
Robots/compagnons réellement possédés et détectés :
${JSON.stringify(robotRoster)}
Guides externes consultés en direct (à utiliser comme contexte, pas comme vérité absolue) :
${leadSummary || 'Aucun guide externe accessible.'}

Base de connaissances communautaire structurée :
${researchNotes}
Question complémentaire : ${extraQuestion || 'Aucune'}.
${cageRules}

Consignes :
- Choisis uniquement parmi les héros présents dans le roster fourni. Ne propose jamais de héros non possédés comme composition principale.
- Évalue niveau, rareté, étoiles, puissance individuelle, type de troupe et niveaux/descriptions de compétences. Une donnée inconnue ou incertaine ne doit pas être inventée.
- Donne la meilleure composition possible avec les informations disponibles, puis une alternative si pertinente.
- Explique pourquoi chaque héros est retenu et quels héros du compte sont moins prioritaires.
- Robot : choisis uniquement parmi les robots/compagnons transmis dans le profil. Compare leurs niveaux, puissance et bonus/compétences. Ne propose jamais un robot absent de la liste. Si la liste est vide, indique clairement qu'une capture du robot est nécessaire. Si les données sont incertaines, précise-le et n'invente aucun bonus.
- Ratio de troupes : propose des pourcentages uniquement si les règles de l'événement et les données disponibles les justifient. Sinon indique explicitement « ratio non déterminable avec les captures actuelles » et précise la capture requise. Ne donne pas de ratio générique présenté comme optimal.
- Pour chaque événement, fournis un champ robotRecommande et un champ ratioTroupes, avec une explication exploitable ou la mention claire des données manquantes.
- Ne fabrique jamais de pourcentages, statistiques, compétences ou effets non confirmés. Signale les incertitudes.
- Réponds dans la langue demandée (${targetLang}).
- Le résumé doit être directement exploitable en jeu.`,
        config: {
          responseMimeType: 'application/json',
          responseSchema: {
            type: Type.OBJECT,
            properties: {
              synthese: { type: Type.STRING },
              compositionRecommandee: { type: Type.STRING },
              robotRecommande: { type: Type.STRING },
              ratioTroupes: { type: Type.STRING },
              recommandations: {
                type: Type.ARRAY,
                items: {
                  type: Type.OBJECT,
                  properties: {
                    titre: { type: Type.STRING },
                    detail: { type: Type.STRING },
                    herosCibles: { type: Type.ARRAY, items: { type: Type.STRING } },
                    incertain: { type: Type.BOOLEAN },
                    raisonIncertitude: { type: Type.STRING },
                  },
                  required: ['titre', 'detail', 'herosCibles', 'incertain'],
                },
              },
              informationsIncertaines: { type: Type.ARRAY, items: { type: Type.STRING } },
            },
            required: ['synthese', 'compositionRecommandee', 'robotRecommande', 'ratioTroupes', 'recommandations', 'informationsIncertaines'],
          },
        },
      });
      const tacticalText = (tacticalResponse.text ?? '').trim();
      if (!tacticalText) throw new Error('Le coach tactique n’a retourné aucune recommandation.');
      const personalized = JSON.parse(tacticalText) as {
        synthese: string;
        compositionRecommandee: string;
        robotRecommande: string;
        ratioTroupes: string;
        recommandations: Array<{ titre: string; detail: string; herosCibles: string[]; incertain: boolean; raisonIncertitude?: string }>;
        informationsIncertaines: string[];
      };

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
          synthese: personalized.synthese || `${synthPrefix[targetLang]}${leadSummary}`,
          compositionRecommandee: personalized.compositionRecommandee || compTextByLang[targetLang],
          robotRecommande: personalized.robotRecommande || ((robots ?? []).length > 0 ? 'Comparer les robots détectés ; aucune sélection fiable n’a été retournée.' : 'Données du robot non disponibles : capture du robot nécessaire.'),
          ratioTroupes: personalized.ratioTroupes || 'Ratio non déterminable avec les captures actuelles.',
          recommandations: (personalized.recommandations ?? []).map((item, index) => ({
            ...item,
            imageUrl: liveGuides[index]?.imageUrl ?? null,
          })),
          informationsIncertaines: personalized.informationsIncertaines ?? [],
          sources: [...liveGuides.map((g) => ({ title: g.title, url: g.url, detectedDate: g.date, imageUrl: g.imageUrl })), ...communityResearchSources.map((s) => ({ title: s.title, url: s.url, detectedDate: s.date, imageUrl: null }))].filter((source, index, all) => all.findIndex((item) => item.url === source.url) === index),
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
    // Correspondance stricte uniquement : une ressemblance partielle peut associer
    // la capture au mauvais héros et contaminer toute sa fiche.
    const matchedCatalog = catalog.find((c) => {
      const normalizedName = c.nom
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase()
        .replace(/&/g, 'and')
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '');
      return c.slug === normalizedInput || normalizedName === normalizedInput;
    });
    if (!matchedCatalog) return null;

    const slug = matchedCatalog.slug;
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

  // Une route API inconnue doit toujours renvoyer du JSON, jamais la page HTML de la SPA.
  app.use('/api', (req, res) => {
    res.status(404).json({
      error: `Route API introuvable : ${req.method} ${req.originalUrl}`,
      hint: 'Vérifie que la version du serveur API correspond à la version du site.',
    });
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
