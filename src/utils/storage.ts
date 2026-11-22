import {
  AccountState,
  AppLanguage,
  HeroRecord,
  PlayerRole,
  ResourceRecord,
  ScreenshotAnalysis,
  TacticalAdviceResult,
  AdviceMode,
} from '../types/game';

const STORAGE_KEY = 'lands_of_jail_assistant_state_v1';

export const DEFAULT_ACCOUNT_STATE: AccountState = {
  language: 'fr',
  playerRole: 'polyvalent_f2p',
  currentPower: null,
  powerHistory: [],
  heroes: [],
  resources: [],
  analyses: [],
  adviceByMode: {},
};

export function loadAccountState(): AccountState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return DEFAULT_ACCOUNT_STATE;
    const parsed = JSON.parse(raw) as Partial<AccountState>;
    return {
      ...DEFAULT_ACCOUNT_STATE,
      ...parsed,
      language: parsed.language ?? 'fr',
      powerHistory: Array.isArray(parsed.powerHistory) ? parsed.powerHistory : [],
      heroes: Array.isArray(parsed.heroes) ? parsed.heroes : [],
      resources: Array.isArray(parsed.resources) ? parsed.resources : [],
      analyses: Array.isArray(parsed.analyses) ? parsed.analyses : [],
      adviceByMode: parsed.adviceByMode ?? {},
    };
  } catch {
    return DEFAULT_ACCOUNT_STATE;
  }
}

export function saveAccountState(state: AccountState): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch (err) {
    console.warn('Impossible de sauvegarder dans localStorage (quota dépassé ?):', err);
    // Fallback: trim oldest thumbnails if localStorage quota is tight
    const trimmed: AccountState = {
      ...state,
      analyses: state.analyses.map((a, idx) =>
        idx > 4 ? { ...a, thumbnailDataUrl: '' } : a
      ),
    };
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(trimmed));
    } catch {
      // ignore if still full
    }
  }
}

function normalizeKey(value: string): string {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim();
}

/** Normalize hero/resource names for consistent matching across OCR and catalog data. */
export function normalizeName(value: string): string {
  return normalizeKey(value);
}

export function mergeAnalysisIntoState(
  prev: AccountState,
  analysis: ScreenshotAnalysis
): AccountState {
  // 1. Update account power only from a dedicated profile screen.
  // Other screens may show partial, contextual or unrelated power values;
  // uncertain OCR must never overwrite the player's trusted total.
  let nextPower = prev.currentPower;
  const nextPowerHistory = [...prev.powerHistory];

  const hasVerifiedProfilePower =
    analysis.typeCapture === 'profil' &&
    analysis.puissance !== null &&
    Boolean(analysis.puissance.puissanceTotaleTexte?.trim()) &&
    analysis.puissance.incertain !== true;

  if (hasVerifiedProfilePower && analysis.puissance) {
    nextPower = analysis.puissance;
    nextPowerHistory.push({
      analysisId: analysis.id,
      date: analysis.analyzedAt,
      puissanceNumerique: analysis.puissance.puissanceTotaleNumerique,
      puissanceTexte: analysis.puissance.puissanceTotaleTexte,
      incertain: analysis.puissance.incertain,
    });
  }

  // 2. Merge resources by normalized name
  const resourceMap = new Map<string, ResourceRecord>();
  for (const r of prev.resources) {
    resourceMap.set(normalizeKey(r.nom), r);
  }
  for (const incoming of analysis.ressources) {
    const key = normalizeKey(incoming.nom);
    if (!key) continue;
    resourceMap.set(key, {
      ...incoming,
      derniereMaj: analysis.analyzedAt,
    });
  }

  // 3. Merge heroes & skills by normalized name
  const heroMap = new Map<string, HeroRecord>();
  for (const h of prev.heroes) {
    heroMap.set(normalizeKey(h.nom), h);
  }
  for (const incoming of analysis.heros) {
    const key = normalizeKey(incoming.nom);
    if (!key) continue;
    const existing = heroMap.get(key);
    if (!existing) {
      heroMap.set(key, {
        ...incoming,
        derniereMaj: analysis.analyzedAt,
      });
    } else {
      // Merge skills by normalized skill name
      const skillMap = new Map(
        existing.competences.map((s) => [normalizeKey(s.nom), s])
      );
      // Never let uncertain OCR overwrite known skill data.
      for (const s of incoming.competences) {
        const sKey = normalizeKey(s.nom);
        if (!sKey) continue;
        const prevSkill = skillMap.get(sKey);
        if (s.incertain && prevSkill) continue;
        skillMap.set(sKey, {
          ...prevSkill,
          ...s,
          niveau: s.niveau && s.niveau !== 'Inconnu' ? s.niveau : (prevSkill?.niveau || s.niveau),
          description: s.description || prevSkill?.description,
          iconUrl: s.iconUrl || prevSkill?.iconUrl,
        });
      }

      // A manually verified hero is authoritative. For other existing heroes,
      // ignore a wholly uncertain OCR result rather than replacing trusted fields.
      if (existing.verifieManuellement || incoming.incertain) {
        heroMap.set(key, {
          ...existing,
          competences: Array.from(skillMap.values()),
        });
        continue;
      }

      heroMap.set(key, {
        ...existing,
        niveau: incoming.niveau ?? existing.niveau,
        niveauTexte:
          incoming.niveauTexte && incoming.niveauTexte !== 'Inconnu'
            ? incoming.niveauTexte
            : existing.niveauTexte,
        rarete: incoming.rarete || existing.rarete,
        etoiles: incoming.etoiles || existing.etoiles,
        roleHero: incoming.roleHero || existing.roleHero,
        puissanceHero: incoming.puissanceHero || existing.puissanceHero,
        imageUrl: incoming.imageUrl || existing.imageUrl,
        avatarUrl: incoming.avatarUrl || existing.avatarUrl,
        titre: incoming.titre || existing.titre,
        typeTroupe: incoming.typeTroupe || existing.typeTroupe,
        categorie: incoming.categorie || existing.categorie,
        saison: incoming.saison || existing.saison,
        puissanceMax5Etoiles: incoming.puissanceMax5Etoiles || existing.puissanceMax5Etoiles,
        resumeBio: incoming.resumeBio || existing.resumeBio,
        idealPour: incoming.idealPour || existing.idealPour,
        wikiUrl: incoming.wikiUrl || existing.wikiUrl,
        competences: Array.from(skillMap.values()),
        incertain: false,
        raisonIncertitude: undefined,
        verifieManuellement: existing.verifieManuellement,
        derniereMaj: analysis.analyzedAt,
      });
    }
  }

  return {
    ...prev,
    currentPower: nextPower,
    powerHistory: nextPowerHistory.slice(-30),
    resources: Array.from(resourceMap.values()),
    heroes: Array.from(heroMap.values()),
    analyses: [analysis, ...prev.analyses].slice(0, 50),
  };
}

export function formatFrenchDateTime(iso: string): string {
  try {
    return new Date(iso).toLocaleString('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return iso;
  }
}

export function formatFrenchDate(iso: string): string {
  try {
    return new Date(iso).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
    });
  } catch {
    return iso;
  }
}

export function formatCompactNumber(num: number | null | undefined): string {
  if (num === null || num === undefined || Number.isNaN(num)) return '—';
  return new Intl.NumberFormat('fr-FR').format(num);
}

export function updatePlayerRoleInState(
  state: AccountState,
  playerRole: PlayerRole
): AccountState {
  return { ...state, playerRole };
}

export function updateLanguageInState(
  state: AccountState,
  language: AppLanguage
): AccountState {
  return { ...state, language };
}

export function saveAdviceInState(
  state: AccountState,
  mode: AdviceMode,
  advice: TacticalAdviceResult
): AccountState {
  return {
    ...state,
    adviceByMode: {
      ...state.adviceByMode,
      [mode]: advice,
    },
  };
}
