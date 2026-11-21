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
      const { imageBase64, mimeType, playerRole, note } = req.body as {
        imageBase64?: string;
        mimeType?: string;
        playerRole?: string;
        note?: string;
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
   - Les ressources visibles (Cash/Or, Vivres, Bois, Acier, Pétrole, Gemmes/Diamants, Points d'arène, etc.) avec leur quantité exacte.
   - Les héros visibles : nom, niveau (entier ou null si illisible), rareté/étoiles si visibles, rôle estimé ou affiché, et toutes les compétences visibles avec leur niveau (ex: "Niv. 3", "Max", "Inconnu") et description courte si visible.
6. Contexte joueur optionnel : rôle="${playerRole || 'non précisé'}", note="${note || 'aucune'}".`;

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

  async function fetchLiveLojGuides(
    mode: 'trial_cage' | 'crystal_clash',
    heroNames: string[]
  ): Promise<Array<{ title: string; url: string; date: string; excerpt: string }>> {
    const queryTerm = mode === 'trial_cage' ? 'cage' : 'crystal clash';
    const urls = [
      `https://lojxlab.com/wp-json/wp/v2/posts?per_page=8&search=${encodeURIComponent(queryTerm)}&_fields=date,modified,link,title,excerpt`,
      ...(heroNames.length > 0
        ? [
            `https://lojxlab.com/wp-json/wp/v2/posts?per_page=4&search=${encodeURIComponent(
              heroNames[0]
            )}&_fields=date,modified,link,title,excerpt`,
          ]
        : []),
    ];

    const stripHtml = (html: string) =>
      html
        .replace(/<[^>]*>/g, ' ')
        .replace(/&#8217;/g, "'")
        .replace(/&#8211;/g, '–')
        .replace(/&amp;/g, '&')
        .replace(/&quot;/g, '"')
        .replace(/\s+/g, ' ')
        .trim();

    const collected: Array<{ title: string; url: string; date: string; excerpt: string }> = [];
    const seen = new Set<string>();

    await Promise.all(
      urls.map(async (u) => {
        try {
          const controller = new AbortController();
          const timer = setTimeout(() => controller.abort(), 7000);
          const resp = await fetch(u, {
            signal: controller.signal,
            headers: { Accept: 'application/json', 'User-Agent': 'LandsOfJailAssistant/1.2' },
          });
          clearTimeout(timer);
          if (!resp.ok) return;
          const posts = (await resp.json()) as Array<{
            date?: string;
            modified?: string;
            link?: string;
            title?: { rendered?: string };
            excerpt?: { rendered?: string };
          }>;
          if (!Array.isArray(posts)) return;
          // Privilégier les articles en anglais (/blog/) ou français (/fr/) pour la lisibilité
          const sorted = [...posts].sort((a, b) => {
            const score = (link = '') =>
              link.includes('/fr/') ? 2 : link.includes('/blog/') ? 1 : 0;
            return score(b.link) - score(a.link);
          });
          for (const p of sorted) {
            const url = p.link || '';
            if (!url || seen.has(url)) continue;
            seen.add(url);
            const rawDate = (p.modified || p.date || '').slice(0, 10);
            collected.push({
              title: stripHtml(p.title?.rendered || 'Guide lojxlab.com'),
              url,
              date: rawDate || new Date().toISOString().slice(0, 10),
              excerpt: stripHtml(p.excerpt?.rendered || ''),
            });
          }
        } catch {
          // Ignore single feed failure
        }
      })
    );

    return collected.slice(0, 6);
  }

  app.post('/api/advice', async (req, res) => {
    try {
      const { mode, playerRole, playerRoleLabel, heroes, powerText, extraQuestion } = req.body as {
        mode?: 'trial_cage' | 'crystal_clash';
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

      if (!mode || (mode !== 'trial_cage' && mode !== 'crystal_clash')) {
        res.status(400).json({ error: 'Mode de jeu invalide (trial_cage ou crystal_clash attendu).' });
        return;
      }

      const modeName = mode === 'trial_cage' ? 'Trial Cage' : 'Crystal Clash';
      const heroNames = (heroes ?? []).map((h) => h.nom).filter(Boolean);
      const liveGuidesPromise = fetchLiveLojGuides(mode, heroNames);

      const heroesSummary =
        heroes && heroes.length > 0
          ? heroes
              .map((h) => {
                const skills =
                  h.competences && h.competences.length > 0
                    ? h.competences
                        .map((s) => `${s.nom} (${s.niveau}${s.incertain ? ', incertain' : ''})`)
                        .join(', ')
                    : 'compétences non détaillées';
                return `- ${h.nom} | Niveau: ${h.niveauTexte}${h.rarete ? ` | Rareté: ${h.rarete}` : ''}${h.roleHero ? ` | Rôle: ${h.roleHero}` : ''}${h.incertain ? ' [DONNÉE INCERTAINE]' : ''} | Compétences: ${skills}`;
              })
              .join('\n')
          : 'Aucun héros extrait pour le moment.';

      const todayFr = new Date().toLocaleDateString('fr-FR', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      });

      const liveGuides = await liveGuidesPromise;
      const liveGuidesContext =
        liveGuides.length > 0
          ? liveGuides
              .map((g) => `- [${g.date}] ${g.title} (${g.url}) : ${g.excerpt}`)
              .join('\n')
          : 'Aucun article lojxlab récupéré directement.';

      let geminiWarning: string | undefined;
      let rawText = '';
      let webSources: Array<{ title: string; url: string }> = [];
      let searchQueries: string[] = [];

      try {
        const ai = getAiClient();
        const prompt = `Tu es un stratège expert du jeu mobile "Lands of Jail".
Nous sommes le ${todayFr}.
Utilise la recherche Google ET les extraits en direct ci-dessous pour donner des conseils récents sur Internet pour le mode "${modeName}" dans "Lands of Jail".

Extraits Internet récupérés en direct :
${liveGuidesContext}

Contexte actuel du joueur :
- Rôle choisi : ${playerRoleLabel || playerRole || 'Non précisé'}
- Puissance observée : ${powerText || 'Non renseignée'}
- Héros visibles extraits des captures :
${heroesSummary}
${extraQuestion ? `- Question spécifique du joueur : ${extraQuestion}` : ''}

Consignes STRICTES :
1. Réponds en français, de manière concrète et adaptée aux héros visibles ci-dessus et au rôle du joueur.
2. Si la liste des héros est vide ou contient des données marquées [DONNÉE INCERTAINE], signale-le explicitement dans "informationsIncertaines" sans inventer les stats manquantes.
3. Indique honnêtement dans "informationsIncertaines" toute règle dont la source est ancienne, contradictoire ou incertaine.
4. Pour chaque source trouvée dans la recherche, relève la date de publication ou de mise à jour si elle apparaît, sinon indique null pour detectedDate.
5. Formate ta réponse UNIQUEMENT sous forme d'un objet JSON valide (sans texte hors du JSON) avec la structure exacte suivante :
{
  "synthese": "Analyse globale adaptée au mode et au rôle (2-4 phrases).",
  "compositionRecommandee": "Composition ou rotation conseillée avec les héros visibles.",
  "recommandations": [
    {
      "titre": "Titre actionnable",
      "detail": "Explication tactique concrète adaptée au rôle et aux compétences visibles.",
      "herosCibles": ["Nom héros 1"],
      "incertain": false,
      "raisonIncertitude": ""
    }
  ],
  "informationsIncertaines": [
    "Liste claire des points incertains, manquants ou dépendant de données non vérifiées."
  ],
  "datesSourcesDetectees": [
    {
      "titreOuDomaine": "Nom ou domaine du guide",
      "datePublication": "ex: 2026-11-19 ou Non précisée sur la page"
    }
  ]
}`;

        const response = await ai.models.generateContent({
          model: 'gemini-4-hydrogen',
          contents: prompt,
          config: {
            tools: [{ googleSearch: {} }],
          },
        });

        rawText = (response.text ?? '').trim();
        const groundingMetadata = response.candidates?.[0]?.groundingMetadata;
        const chunks = groundingMetadata?.groundingChunks ?? [];
        searchQueries = groundingMetadata?.webSearchQueries ?? [];
        webSources = chunks
          .map((c) => c.web)
          .filter((w): w is { uri?: string; title?: string } => Boolean(w && w.uri))
          .map((w) => ({
            title: w.title || w.uri || 'Source Web',
            url: w.uri as string,
          }));
      } catch (aiErr: unknown) {
        const msg = aiErr instanceof Error ? aiErr.message : String(aiErr);
        const isQuota = msg.includes('429') || msg.includes('RESOURCE_EXHAUSTED');
        geminiWarning = isQuota
          ? 'Quota API Gemini dépassé (erreur 429 RESOURCE_EXHAUSTED). Vérifiez votre forfait ou clé API dans Settings > Secrets. Les guides Internet en direct récupérés depuis lojxlab.com sont affichés ci-dessous.'
          : `Synthèse Gemini indisponible (${msg}). Affichage des guides Internet en direct récupérés depuis lojxlab.com.`;

        if (liveGuides.length === 0) {
          res.status(isQuota ? 429 : 502).json({ error: geminiWarning });
          return;
        }
      }

      const mergedSourcesMap = new Map<
        string,
        { title: string; url: string; detectedDate: string | null }
      >();

      for (const g of liveGuides) {
        mergedSourcesMap.set(g.url, {
          title: g.title,
          url: g.url,
          detectedDate: g.date,
        });
      }

      let parsedAdvice: {
        synthese: string;
        compositionRecommandee: string;
        recommandations: Array<{
          titre: string;
          detail: string;
          herosCibles?: string[];
          incertain?: boolean;
          raisonIncertitude?: string;
        }>;
        informationsIncertaines: string[];
        datesSourcesDetectees?: Array<{ titreOuDomaine: string; datePublication: string }>;
      };

      const jsonMatch = rawText.match(/\{[\s\S]*\}/);
      if (jsonMatch) {
        try {
          parsedAdvice = JSON.parse(jsonMatch[0]);
        } catch {
          parsedAdvice = {
            synthese: rawText,
            compositionRecommandee: 'Voir le détail ci-dessous.',
            recommandations: [],
            informationsIncertaines: [
              'Le format structuré de la réponse n’a pas pu être découpé automatiquement.',
            ],
          };
        }
      } else if (rawText) {
        parsedAdvice = {
          synthese: rawText,
          compositionRecommandee: 'Voir la synthèse.',
          recommandations: [],
          informationsIncertaines: ['Réponse reçue en texte libre sans structure JSON.'],
        };
      } else {
        // Mode sources Internet en direct (sans synthèse Gemini disponible)
        const roleLabel = playerRoleLabel || playerRole || 'Polyvalent / F2P';
        parsedAdvice = {
          synthese: `Résultats Internet en direct récupérés pour ${modeName} (rôle : ${roleLabel}). ${
            liveGuides[0] ? `Dernier guide pertinent : « ${liveGuides[0].title} » (${liveGuides[0].date}).` : ''
          }`,
          compositionRecommandee:
            heroNames.length > 0
              ? `Héros détectés sur votre compte : ${heroNames.join(', ')}. Consultez les extraits datés ci-dessous pour vérifier l'ordre exact selon votre rôle (${roleLabel}).`
              : `Aucun héros scanné pour l'instant : partagez ou capturez votre page Héros pour croiser vos personnages avec les guides ci-dessous.`,
          recommandations: liveGuides.slice(0, 4).map((g) => ({
            titre: g.title,
            detail: g.excerpt || 'Consultez la source complète via le lien ci-dessous.',
            herosCibles: heroNames.slice(0, 3),
            incertain: heroNames.length === 0,
            raisonIncertitude:
              heroNames.length === 0
                ? 'Héros du compte non encore scannés ; adaptation exacte au compte à vérifier.'
                : undefined,
          })),
          informationsIncertaines: [
            'La synthèse IA personnalisée n’a pas pu s’exécuter (voir Alerte API Gemini ci-dessus) : seuls les extraits et sources Internet en direct sont affichés.',
            ...(heroNames.length === 0
              ? ['Aucun héros scanné sur le compte : partagez une capture de vos héros.']
              : []),
          ],
        };
      }

      const detectedDates = parsedAdvice.datesSourcesDetectees ?? [];
      for (const src of webSources) {
        if (!mergedSourcesMap.has(src.url)) {
          const matchedDate = detectedDates.find(
            (d) =>
              src.title.toLowerCase().includes(d.titreOuDomaine.toLowerCase()) ||
              src.url.toLowerCase().includes(d.titreOuDomaine.toLowerCase())
          );
          mergedSourcesMap.set(src.url, {
            title: src.title,
            url: src.url,
            detectedDate: matchedDate?.datePublication || null,
          });
        }
      }

      const enrichedSources = Array.from(mergedSourcesMap.values());
      const uncertainNotes = [...(parsedAdvice.informationsIncertaines ?? [])];
      if (enrichedSources.length === 0) {
        uncertainNotes.push(
          'Aucune source Internet directe n’a été retournée pour cette requête ; vérifiez ces éléments en jeu.'
        );
      }

      res.json({
        advice: {
          mode,
          playerRole: playerRole || 'polyvalent_f2p',
          generatedAt: new Date().toISOString(),
          synthese: parsedAdvice.synthese || '',
          compositionRecommandee: parsedAdvice.compositionRecommandee || '',
          recommandations: (parsedAdvice.recommandations ?? []).map((r) => ({
            titre: r.titre || 'Conseil tactique',
            detail: r.detail || '',
            herosCibles: r.herosCibles ?? [],
            incertain: Boolean(r.incertain),
            raisonIncertitude: r.raisonIncertitude || undefined,
          })),
          informationsIncertaines: uncertainNotes,
          sources: enrichedSources,
          requetesRecherche: searchQueries,
          geminiWarning,
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
