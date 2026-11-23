import type { Express, Request, Response } from 'express';
import { GoogleGenAI, Type } from '@google/genai';

type IntelCategory = 'hero' | 'robot' | 'skill' | 'event' | 'building' | 'equipment' | 'balance' | 'mechanic' | 'other';
type IntelSource = { name: string; url: string; authority: 'official' | 'community'; checkedAt: string; content: string };
type IntelItem = {
  id: string;
  title: string;
  category: IntelCategory;
  summary: string;
  details: string;
  sourceName: string;
  sourceUrl: string;
  sourceAuthority: 'official' | 'community';
  confidence: number;
  status: 'confirmed' | 'to_verify';
  detectedAt: string;
  requiresReview: true;
};
type IntelSnapshot = {
  status: 'up_to_date' | 'updates_found' | 'sources_unavailable' | 'analysis_unavailable';
  checkedAt: string;
  gameVersion: string | null;
  items: IntelItem[];
  sources: Array<{ name: string; url: string; authority: string; checkedAt: string; available: boolean }>;
  message: string;
};

const CACHE_TTL_MS = 6 * 60 * 60 * 1000;
const MIN_REFRESH_INTERVAL_MS = 20 * 60 * 1000;
let cachedSnapshot: IntelSnapshot | null = null;
let lastRefreshAt = 0;
let refreshInFlight: Promise<IntelSnapshot> | null = null;

function cleanText(value: unknown, max = 5000): string {
  return String(value ?? '').replace(/<script[\s\S]*?<\/script>/gi, ' ')
    .replace(/<style[\s\S]*?<\/style>/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&amp;/g, '&').replace(/&quot;/g, '"').replace(/&#39;|&#x27;/g, "'")
    .replace(/&#8217;/g, "'").replace(/&#8211;/g, '–')
    .replace(/\s+/g, ' ').trim().slice(0, max);
}

async function fetchText(url: string, accept = 'text/html'): Promise<string> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 9000);
  try {
    const response = await fetch(url, {
      signal: controller.signal,
      headers: { 'User-Agent': 'LandsOfJailAssistant-GameIntel/1.0', Accept: accept },
    });
    if (!response.ok) throw new Error('HTTP ' + response.status);
    return await response.text();
  } finally {
    clearTimeout(timer);
  }
}

async function collectSources(): Promise<{ sources: IntelSource[]; gameVersion: string | null }> {
  const checkedAt = new Date().toISOString();
  const sources: IntelSource[] = [];
  let gameVersion: string | null = null;

  const officialTasks = [
    (async () => {
      const raw = await fetchText('https://itunes.apple.com/lookup?id=6738469826&country=us', 'application/json');
      const parsed = JSON.parse(raw) as { results?: Array<{ version?: string; currentVersionReleaseDate?: string; releaseNotes?: string; trackViewUrl?: string; trackName?: string }> };
      const app = parsed.results?.[0];
      if (!app) throw new Error('Fiche App Store introuvable');
      gameVersion = app.version || null;
      sources.push({
        name: 'Apple App Store — fiche officielle',
        url: app.trackViewUrl || 'https://apps.apple.com/app/lands-of-jail/id6738469826',
        authority: 'official',
        checkedAt,
        content: cleanText([app.trackName, 'Version ' + app.version, 'Date ' + app.currentVersionReleaseDate, app.releaseNotes].filter(Boolean).join(' — '), 4500),
      });
    })(),
    (async () => {
      const html = await fetchText('https://play.google.com/store/apps/details?id=com.justgame.jails&hl=en_US');
      const text = cleanText(html, 6500);
      const versionMatch = html.match(/(?:Current Version|Version)[:\s<]+([0-9]+\.[0-9]+(?:\.[0-9]+)?)/i);
      sources.push({
        name: 'Google Play — fiche officielle Android',
        url: 'https://play.google.com/store/apps/details?id=com.justgame.jails',
        authority: 'official',
        checkedAt,
        content: cleanText((versionMatch ? 'Version détectée ' + versionMatch[1] + '. ' : '') + text, 4500),
      });
    })(),
  ];

  const communityTasks = [
    (async () => {
      const url = 'https://lojxlab.com/wp-json/wp/v2/posts?per_page=12&orderby=modified&order=desc&_fields=date,modified,link,title,excerpt,content';
      const raw = await fetchText(url, 'application/json');
      const posts = JSON.parse(raw) as Array<{ date?: string; modified?: string; link?: string; title?: { rendered?: string }; excerpt?: { rendered?: string }; content?: { rendered?: string } }>;
      const content = posts.slice(0, 10).map(post =>
        [post.modified || post.date, cleanText(post.title?.rendered, 180), cleanText(post.excerpt?.rendered || post.content?.rendered, 650), post.link].filter(Boolean).join(' | ')
      ).join('\n');
      sources.push({ name: 'LOJ XLab — guides communautaires', url: 'https://lojxlab.com/', authority: 'community', checkedAt, content: content.slice(0, 7000) });
    })(),
    (async () => {
      const url = 'https://lojwiki.com/';
      const html = await fetchText(url);
      sources.push({ name: 'LOJ Wiki — base communautaire', url, authority: 'community', checkedAt, content: cleanText(html, 5000) });
    })(),
  ];

  await Promise.allSettled([...officialTasks, ...communityTasks]);
  return { sources, gameVersion };
}

async function buildSnapshot(): Promise<IntelSnapshot> {
  const checkedAt = new Date().toISOString();
  const { sources, gameVersion } = await collectSources();
  const available = sources.filter(source => source.content.length > 80);
  if (available.length === 0) {
    return {
      status: 'sources_unavailable', checkedAt, gameVersion: null, items: [],
      sources: sources.map(source => ({ name: source.name, url: source.url, authority: source.authority, checkedAt: source.checkedAt, available: false })),
      message: 'Aucune source distante exploitable. Les données locales ne sont pas modifiées.',
    };
  }

  const apiKey = process.env.GEMINI_API_KEY;
  let items: IntelItem[] = [];
  if (apiKey) {
    const ai = new GoogleGenAI({ apiKey });
    const sourceText = available.map(source =>
      'SOURCE: ' + source.name + '\nAUTORITÉ: ' + source.authority + '\nURL: ' + source.url + '\nCONTENU: ' + source.content
    ).join('\n\n').slice(0, 18000);
    const response = await ai.models.generateContent({
      model: 'gemini-4-hydrogen',
      contents: [{
        role: 'user',
        parts: [{ text: `Tu es l'analyste de veille du jeu mobile Lands of Jail. Analyse les sources ci-dessous et détecte uniquement les changements ou nouveautés utiles à une application d'aide aux joueurs : héros, robots, compétences, événements, bâtiments, Forticlad, équipements, troupes, équilibrage et mécaniques.
RÈGLES STRICTES :
- Ne crée aucune information absente des sources.
- Les sources "official" sont les seules pouvant produire status="confirmed". Les sources "community" produisent toujours status="to_verify".
- Une page communautaire ancienne ou sans date de modification claire ne prouve pas une nouveauté récente.
- Ne transforme jamais une puissance maximale théorique en statistique de compte joueur.
- Ne propose jamais d'écraser les niveaux, étoiles, puissance ou compétences personnelles scannés d'un joueur.
- Écarte les publicités, avis joueurs et contenus sans rapport.
- Si aucun changement nouveau et concret n'est prouvé, retourne items=[].
- Chaque item doit citer exactement une URL présente dans les sources.
Retourne uniquement un JSON de forme {"items":[{"title":"","category":"hero|robot|skill|event|building|equipment|balance|mechanic|other","summary":"","details":"","sourceName":"","sourceUrl":"","sourceAuthority":"official|community","confidence":0.0,"status":"confirmed|to_verify"}]}.
Les items restent des propositions et nécessitent toujours validation avant modification du catalogue.
\n\nSOURCES:\n${sourceText}` }],
      }],
      config: {
        responseMimeType: 'application/json',
        responseSchema: {
          type: Type.OBJECT,
          properties: {
            items: { type: Type.ARRAY, items: { type: Type.OBJECT, properties: {
              title: { type: Type.STRING }, category: { type: Type.STRING }, summary: { type: Type.STRING },
              details: { type: Type.STRING }, sourceName: { type: Type.STRING }, sourceUrl: { type: Type.STRING },
              sourceAuthority: { type: Type.STRING }, confidence: { type: Type.NUMBER }, status: { type: Type.STRING },
            }, required: ['title', 'category', 'summary', 'details', 'sourceName', 'sourceUrl', 'sourceAuthority', 'confidence', 'status'] } },
          },
          required: ['items'],
        },
      },
    });
    const parsed = JSON.parse((response.text || '{"items":[]}').trim()) as { items?: Array<Record<string, unknown>> };
    const validCategories: IntelCategory[] = ['hero', 'robot', 'skill', 'event', 'building', 'equipment', 'balance', 'mechanic', 'other'];
    items = (parsed.items || []).flatMap((raw) => {
      const sourceUrl = String(raw.sourceUrl || '');
      const source = available.find(candidate => candidate.url === sourceUrl);
      if (!source || !String(raw.title || '').trim()) return [];
      const authority = source.authority;
      return [{
        id: Buffer.from(sourceUrl + '|' + String(raw.title) + '|' + checkedAt.slice(0, 10)).toString('base64url').slice(0, 32),
        title: String(raw.title).slice(0, 160),
        category: validCategories.includes(String(raw.category) as IntelCategory) ? String(raw.category) as IntelCategory : 'other',
        summary: String(raw.summary || '').slice(0, 500),
        details: String(raw.details || '').slice(0, 1800),
        sourceName: source.name,
        sourceUrl,
        sourceAuthority: authority,
        confidence: Math.max(0, Math.min(1, Number(raw.confidence) || 0)),
        status: authority === 'official' && raw.status === 'confirmed' ? 'confirmed' as const : 'to_verify' as const,
        detectedAt: checkedAt,
        requiresReview: true as const,
      }];
    });
  }

  return {
    status: !apiKey ? 'analysis_unavailable' : items.length ? 'updates_found' : 'up_to_date',
    checkedAt,
    gameVersion,
    items,
    sources: sources.map(source => ({ name: source.name, url: source.url, authority: source.authority, checkedAt: source.checkedAt, available: source.content.length > 80 })),
    message: !apiKey
      ? 'Sources récupérées, mais analyse IA indisponible : la clé GEMINI_API_KEY manque côté serveur. Aucun changement ne peut être confirmé et le catalogue reste inchangé.'
      : items.length
        ? items.length + ' nouveauté(s) ou changement(s) potentiel(s) détecté(s). Aucun catalogue personnel n’a été modifié automatiquement.'
        : 'Analyse IA terminée : aucun changement exploitable détecté dans les sources consultées. Les données de l’application restent inchangées.',
  };
}

export function registerGameIntelRoutes(app: Express): void {
  app.get('/api/game-intel', async (req: Request, res: Response) => {
    try {
      const force = req.query.refresh === '1';
      const now = Date.now();
      if (cachedSnapshot && !force && now - lastRefreshAt < CACHE_TTL_MS) {
        res.json({ ...cachedSnapshot, cached: true });
        return;
      }
      if (force && now - lastRefreshAt < MIN_REFRESH_INTERVAL_MS && cachedSnapshot) {
        res.json({ ...cachedSnapshot, cached: true, refreshLimited: true });
        return;
      }
      if (!refreshInFlight) refreshInFlight = buildSnapshot();
      const snapshot = await refreshInFlight;
      cachedSnapshot = snapshot;
      lastRefreshAt = Date.now();
      refreshInFlight = null;
      res.json({ ...snapshot, cached: false });
    } catch (error) {
      refreshInFlight = null;
      console.error('Veille Land of Jail impossible:', error);
      if (cachedSnapshot) {
        res.status(200).json({ ...cachedSnapshot, cached: true, warning: 'La vérification distante a échoué; affichage du dernier résultat.' });
        return;
      }
      res.status(503).json({ error: 'Veille indisponible pour le moment. Aucune donnée du jeu n’a été modifiée.' });
    }
  });
}
