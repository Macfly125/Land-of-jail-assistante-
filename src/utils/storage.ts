import {
  AccountState,
  AppLanguage,
  HeroRecord,
  PlayerRole,
  ResourceRecord,
  RobotRecord,
  ScreenshotAnalysis,
  TacticalAdviceResult,
  AdviceMode,
} from '../types/game';

const STORAGE_KEY = 'lands_of_jail_assistant_state_v1';

export const DEFAULT_ACCOUNT_STATE: AccountState = {
  language: 'fr',
  playerRole: 'polyvalent_f2p',
  currentPower: null,
  forticladLevel: 0,
  forticladBuildings: {},
  forticladLabProgress: {},
  powerHistory: [],
  heroes: [],
  robots: [],
  resources: [],
  analyses: [],
  adviceByMode: {},
};


export interface PlayerProfile {
  id: string;
  name: string;
  state: AccountState;
  updatedAt?: string;
}

export interface ProfileCollection {
  profiles: PlayerProfile[];
  activeProfileId: string;
}

const PROFILE_STORAGE_KEY = 'lands_of_jail_assistant_profiles_v1';

export function createEmptyAccountState(
  language: AppLanguage = 'fr',
  playerRole: PlayerRole = 'polyvalent_f2p'
): AccountState {
  return {
    ...DEFAULT_ACCOUNT_STATE,
    language,
    playerRole,
    powerHistory: [],
    heroes: [],
    robots: [],
    resources: [],
    analyses: [],
    adviceByMode: {},
  };
}

export function loadProfileCollection(): ProfileCollection {
  try {
    const raw = localStorage.getItem(PROFILE_STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<ProfileCollection>;
      const profiles = Array.isArray(parsed.profiles)
        ? parsed.profiles.filter((p): p is PlayerProfile =>
            Boolean(p && typeof p.id === 'string' && typeof p.name === 'string' && p.state)
          )
        : [];
      if (profiles.length > 0) {
        const activeProfileId = profiles.some((p) => p.id === parsed.activeProfileId)
          ? parsed.activeProfileId as string
          : profiles[0].id;
        return { profiles, activeProfileId };
      }
    }
  } catch {
    // Migrate the previous single-account storage below.
  }

  const migrated: PlayerProfile = {
    id: 'profile-main',
    name: 'Compte principal',
    state: loadAccountState(),
  };
  return { profiles: [migrated], activeProfileId: migrated.id };
}

export function saveProfileCollection(collection: ProfileCollection): void {
  try {
    localStorage.setItem(PROFILE_STORAGE_KEY, JSON.stringify(collection));
  } catch (err) {
    console.warn('Impossible de sauvegarder les profils :', err);
  }
}

export function loadAccountState(): AccountState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return DEFAULT_ACCOUNT_STATE;
    const parsed = JSON.parse(raw) as Partial<AccountState>;
    return {
      ...DEFAULT_ACCOUNT_STATE,
      ...parsed,
      language: parsed.language ?? 'fr',
      forticladLevel: typeof parsed.forticladLevel === 'number' ? Math.min(8, Math.max(0, parsed.forticladLevel)) : 0,
      forticladBuildings: parsed.forticladBuildings && typeof parsed.forticladBuildings === 'object' ? parsed.forticladBuildings : {},
      powerHistory: Array.isArray(parsed.powerHistory) ? parsed.powerHistory : [],
      heroes: Array.isArray(parsed.heroes) ? parsed.heroes : [],
      robots: Array.isArray(parsed.robots) ? parsed.robots : [],
      resources: Array.isArray(parsed.resources) ? parsed.resources : [],
      analyses: Array.isArray(parsed.analyses) ? parsed.analyses.map((analysis) => ({ ...analysis, robots: analysis.robots ?? [] })) : [],
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

export const normalizeName = normalizeKey;

export function mergeAnalysisIntoState(
  prev: AccountState,
  analysis: ScreenshotAnalysis
): AccountState {
  // 1. Update power if present
  let nextPower = prev.currentPower;
  const nextPowerHistory = [...prev.powerHistory];

  if (analysis.puissance && analysis.puissance.puissanceTotaleTexte) {
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
      for (const s of incoming.competences) {
        const sKey = normalizeKey(s.nom);
        if (!sKey) continue;
        const prevSkill = skillMap.get(sKey);
        skillMap.set(sKey, {
          ...prevSkill,
          ...s,
          niveau: s.niveau && s.niveau !== 'Inconnu' ? s.niveau : (prevSkill?.niveau || s.niveau),
          description: s.description || prevSkill?.description,
          iconUrl: s.iconUrl || prevSkill?.iconUrl,
        });
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
        incertain: incoming.incertain,
        raisonIncertitude: incoming.raisonIncertitude,
        verifieManuellement: false,
        derniereMaj: analysis.analyzedAt,
      });
    }
  }

  // 4. Merge robots / companions by exact normalized name.
  const robotMap = new Map<string, RobotRecord>();
  for (const robot of prev.robots ?? []) robotMap.set(normalizeKey(robot.nom), robot);
  for (const incoming of analysis.robots ?? []) {
    const key = normalizeKey(incoming.nom);
    if (!key) continue;
    const existing = robotMap.get(key);
    if (!existing) {
      robotMap.set(key, { ...incoming, derniereMaj: analysis.analyzedAt });
      continue;
    }
    const skillMap = new Map(existing.competences.map((skill) => [normalizeKey(skill.nom), skill]));
    for (const skill of incoming.competences) {
      const skillKey = normalizeKey(skill.nom);
      if (!skillKey) continue;
      const previousSkill = skillMap.get(skillKey);
      skillMap.set(skillKey, {
        ...previousSkill,
        ...skill,
        niveau: skill.niveau && skill.niveau !== 'Inconnu' ? skill.niveau : (previousSkill?.niveau || skill.niveau),
      });
    }
    robotMap.set(key, {
      ...existing,
      ...incoming,
      niveau: incoming.niveau || existing.niveau,
      puissance: incoming.puissance || existing.puissance,
      rarete: incoming.rarete || existing.rarete,
      competences: Array.from(skillMap.values()),
      derniereMaj: analysis.analyzedAt,
    });
  }

  return {
    ...prev,
    currentPower: nextPower,
    powerHistory: nextPowerHistory.slice(-30),
    resources: Array.from(resourceMap.values()),
    heroes: Array.from(heroMap.values()),
    robots: Array.from(robotMap.values()),
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
