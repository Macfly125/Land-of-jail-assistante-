import fs from 'node:fs/promises';

const catalogPath = 'android/app/src/main/java/com/landsofjail/assistant/model/GameModels.kt';
const source = await fs.readFile(catalogPath, 'utf8');
const entries = [...source.matchAll(/HeroCatalogEntry\("([^"]+)"([^)]*)\)/g)].map(match => {
  const name = match[1];
  const portraitUrl = match[2].match(/portraitUrl\s*=\s*"([^"]+)"/)?.[1] ?? null;
  return { name, portraitUrl };
});

function normalize(value) {
  return String(value).normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .toLowerCase().replace(/&/g, ' ').replace(/[^a-z0-9]+/g, ' ').trim();
}

function slugify(value) {
  return normalize(value).replace(/\s+/g, '-');
}

function attr(tag, name) {
  return tag.match(new RegExp('\\b' + name + '\\s*=\\s*"([^"]+)"', 'i'))?.[1] ?? '';
}

async function request(url, method = 'GET') {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 10000);
  try {
    const response = await fetch(url, {
      method,
      signal: controller.signal,
      headers: { 'User-Agent': 'LandsOfJailAssistant-CatalogAudit/1.0' }
    });
    return { ok: response.ok, status: response.status, response };
  } finally {
    clearTimeout(timeout);
  }
}

async function verifyPortrait(url) {
  if (!url) return { ok: false, status: 'missing' };
  try {
    const result = await request(url, 'HEAD');
    return { ok: result.ok, status: result.status };
  } catch (error) {
    return { ok: false, status: String(error) };
  }
}

async function discoverPortrait(name) {
  const slug = slugify(name);
  const urls = [
    'https://lojwiki.com/heroes/' + slug + '/',
    'https://lojwiki.com/fr/heroes/' + slug + '/'
  ];
  for (const pageUrl of urls) {
    try {
      const result = await request(pageUrl);
      if (!result.ok) continue;
      const html = await result.response.text();
      const tags = html.match(/<img\b[^>]*>/gi) ?? [];
      const wanted = normalize(name);
      const candidates = tags.map(tag => ({
        alt: normalize(attr(tag, 'alt')),
        src: attr(tag, 'src') || attr(tag, 'data-src') || attr(tag, 'data-lazy-src')
      })).filter(image =>
        image.alt && (image.alt === wanted || image.alt.includes(wanted)) &&
        /^https:\/\/img\.lojwiki\.com\/half\/[^\s"'<>]+\.(?:webp|png|jpe?g)(?:\?.*)?$/i.test(image.src)
      );
      if (candidates.length) {
        const candidate = candidates[0].src;
        const checked = await verifyPortrait(candidate);
        if (checked.ok) return { url: candidate, pageUrl, status: checked.status };
      }
    } catch {
      // Une page inaccessible ne doit jamais créer une URL supposée.
    }
  }
  return null;
}

const reportEntries = [];
for (const entry of entries) {
  const current = await verifyPortrait(entry.portraitUrl);
  const discovered = current.ok || entry.portraitUrl ? null : await discoverPortrait(entry.name);
  reportEntries.push({
    name: entry.name,
    currentPortraitUrl: entry.portraitUrl,
    currentPortraitAvailable: current.ok,
    currentPortraitStatus: current.status,
    suggestedPortraitUrl: discovered?.url ?? null,
    suggestionSource: discovered?.pageUrl ?? null,
    suggestionVerified: Boolean(discovered)
  });
}

const report = {
  checkedAt: new Date().toISOString(),
  catalogEntries: entries.length,
  portraitsConfigured: entries.filter(entry => entry.portraitUrl).length,
  portraitsVerified: reportEntries.filter(entry => entry.currentPortraitAvailable).length,
  portraitsMissing: reportEntries.filter(entry => !entry.currentPortraitAvailable).length,
  newSuggestions: reportEntries.filter(entry => entry.suggestionVerified).length,
  note: 'Audit uniquement. Les URL suggérées ne sont jamais injectées dans le catalogue sans revue humaine.',
  heroes: reportEntries
};

await fs.mkdir('reports', { recursive: true });
await fs.writeFile('reports/hero-portrait-audit.json', JSON.stringify(report, null, 2));
const lines = [
  '# Audit des portraits de héros',
  '',
  '- Vérification : ' + report.checkedAt,
  '- Héros dans le catalogue : ' + report.catalogEntries,
  '- Portraits configurés : ' + report.portraitsConfigured,
  '- Portraits existants vérifiés : ' + report.portraitsVerified,
  '- Portraits absents ou indisponibles : ' + report.portraitsMissing,
  '- Suggestions nouvelles vérifiées : ' + report.newSuggestions,
  '',
  '> Les suggestions sont des propositions sourcées. Aucun fichier du catalogue n’est modifié.'
];
for (const hero of reportEntries.filter(item => !item.currentPortraitAvailable)) {
  lines.push('', '## ' + hero.name);
  if (hero.suggestionVerified) {
    lines.push('- Proposition : ' + hero.suggestedPortraitUrl, '- Page source : ' + hero.suggestionSource);
  } else {
    lines.push('- Aucun portrait fiable trouvé automatiquement. Vérification manuelle nécessaire.');
  }
}
await fs.writeFile('reports/hero-portrait-audit.md', lines.join('\n') + '\n');
console.log(JSON.stringify({
  catalogEntries: report.catalogEntries,
  portraitsConfigured: report.portraitsConfigured,
  portraitsVerified: report.portraitsVerified,
  portraitsMissing: report.portraitsMissing,
  newSuggestions: report.newSuggestions,
  verifiedSuggestions: reportEntries.filter(item => item.suggestionVerified).map(item => ({
    name: item.name,
    url: item.suggestedPortraitUrl,
    source: item.suggestionSource
  }))
}));
