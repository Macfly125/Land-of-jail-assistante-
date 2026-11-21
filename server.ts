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

      const ai = getAiClient();
      const modeName = mode === 'trial_cage' ? 'Trial Cage' : 'Crystal Clash';
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

      const prompt = `Tu es un stratège expert du jeu mobile "Lands of Jail".
Nous sommes le ${todayFr}.
Utilise la recherche Google pour trouver les guides, conseils méta et stratégies récentes sur Internet pour le mode "${modeName}" dans "Lands of Jail" (aussi cherché parfois en anglais : "Lands of Jail ${modeName} guide tips heroes").

Contexte actuel du joueur :
- Rôle choisi : ${playerRoleLabel || playerRole || 'Non précisé'}
- Puissance observée : ${powerText || 'Non renseignée'}
- Héros visibles extraits des captures :
${heroesSummary}
${extraQuestion ? `- Question spécifique du joueur : ${extraQuestion}` : ''}

Consignes STRICTES :
1. Réponds en français, de manière concrète et adaptée aux héros visibles ci-dessus et au rôle du joueur.
2. Si la liste des héros est vide ou contient des données marquées [DONNÉE INCERTAINE], signale-le explicitement dans "informationsIncertaines" sans inventer les stats manquantes.
3. Pour les informations trouvées sur Internet, indique honnêtement dans "informationsIncertaines" toute règle ou synergie dont la source est ancienne, contradictoire, non officielle ou incertaine (par exemple si "Lands of Jail" a peu de documentation fiable sur un héros précis).
4. Pour chaque source trouvée dans la recherche, essaie de relever la date de publication ou de mise à jour si elle apparaît sur la page, sinon indique null pour detectedDate.
5. Formate ta réponse UNIQUEMENT sous forme d'un objet JSON valide (sans texte hors du JSON) avec la structure exacte suivante :
{
  "synthese": "Analyse globale adaptée au mode et au rôle (2-4 phrases).",
  "compositionRecommandee": "Composition ou rotation conseillée avec les héros visibles (ou ce qu'il faut prioriser si certains héros manquent).",
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
      "datePublication": "ex: Mars 2025 ou Non précisée sur la page"
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

      const rawText = (response.text ?? '').trim();
      const groundingMetadata = response.candidates?.[0]?.groundingMetadata;
      const chunks = groundingMetadata?.groundingChunks ?? [];
      const searchQueries = groundingMetadata?.webSearchQueries ?? [];

      const webSources = chunks
        .map((c) => c.web)
        .filter((w): w is { uri?: string; title?: string } => Boolean(w && w.uri))
        .map((w) => ({
          title: w.title || w.uri || 'Source Web',
          url: w.uri as string,
        }));

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
              'Le format structuré de la réponse n’a pas pu être découpé automatiquement ; vérifiez les points clés dans la synthèse.',
            ],
          };
        }
      } else {
        parsedAdvice = {
          synthese: rawText || 'Aucune réponse générée.',
          compositionRecommandee: 'Voir la synthèse.',
          recommandations: [],
          informationsIncertaines: [
            'Réponse reçue en texte libre sans structure JSON ; certaines dates ou correspondances peuvent être imprécises.',
          ],
        };
      }

      const detectedDates = parsedAdvice.datesSourcesDetectees ?? [];
      const enrichedSources = webSources.map((src) => {
        const matchedDate = detectedDates.find(
          (d) =>
            src.title.toLowerCase().includes(d.titreOuDomaine.toLowerCase()) ||
            src.url.toLowerCase().includes(d.titreOuDomaine.toLowerCase())
        );
        return {
          title: src.title,
          url: src.url,
          detectedDate: matchedDate?.datePublication || null,
        };
      });

      const uncertainNotes = [...(parsedAdvice.informationsIncertaines ?? [])];
      if (enrichedSources.length === 0) {
        uncertainNotes.push(
          'Aucune source Internet directe n’a été retournée par la recherche web pour cette requête ; ces conseils doivent être vérifiés en jeu.'
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
