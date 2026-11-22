export type AppLanguage = 'fr' | 'tr' | 'ru' | 'zh' | 'de' | 'en';

export const APP_LANGUAGE_LABELS: Record<AppLanguage, string> = {
  fr: 'Français',
  tr: 'Türkçe',
  ru: 'Русский',
  zh: '中文',
  de: 'Deutsch',
  en: 'English',
};

export type PlayerRole =
  | 'leader_ralliement'
  | 'assaut_dps'
  | 'defenseur_garnison'
  | 'soutien_controle'
  | 'polyvalent_f2p';

export const PLAYER_ROLE_LABELS: Record<PlayerRole, { label: string; description: string }> = {
  leader_ralliement: {
    label: 'Leader de ralliement',
    description: 'Lance les ralliements principaux et optimise les bonus de capacité et de dégâts de groupe.',
  },
  assaut_dps: {
    label: 'Assaut / DPS',
    description: 'Maximise les dégâts bruts et les compétences offensives en duel ou en escarmouche.',
  },
  defenseur_garnison: {
    label: 'Défenseur / Garnison',
    description: 'Tient les positions et protège la base grâce aux héros tanks, boucliers et contre-attaques.',
  },
  soutien_controle: {
    label: 'Soutien / Contrôle',
    description: 'Apporte soins, réductions de dégâts, étourdissements et debuffs en combat d’équipe.',
  },
  polyvalent_f2p: {
    label: 'Polyvalent / F2P',
    description: 'Progression équilibrée avec optimisation stricte des ressources et des héros accessibles.',
  },
};

export type ConfidenceLevel = 'elevee' | 'moyenne' | 'incertaine';

export type ScreenCategory =
  | 'profil'
  | 'heros'
  | 'ressources'
  | 'trial_cage'
  | 'crystal_clash'
  | 'sandsea_conquest'
  | 'svs'
  | 'turmoil_surge'
  | 'gold_hunter'
  | 'autre';

export const SCREEN_CATEGORY_LABELS: Record<ScreenCategory, string> = {
  profil: 'Profil & Puissance',
  heros: 'Héros & Compétences',
  ressources: 'Ressources & Entrepôt',
  trial_cage: 'Trial Cage',
  crystal_clash: 'Crystal Clash',
  sandsea_conquest: 'Conquête de sable',
  svs: 'SvS / Supreme Honor',
  turmoil_surge: 'Turmoil Surge',
  gold_hunter: 'Gold Hunter / Power Sprint',
  autre: 'Autre écran',
};

export type AdviceMode =
  | 'trial_cage'
  | 'crystal_clash'
  | 'sandsea_conquest'
  | 'svs'
  | 'turmoil_surge'
  | 'gold_hunter'
  | 'gorilla_legion';

export const ADVICE_EVENT_LIST: Array<{
  id: AdviceMode;
  label: Record<AppLanguage, string>;
  searchKeyword: string;
}> = [
  {
    id: 'trial_cage',
    label: {
      fr: 'Trial Cage',
      tr: 'Deneme Kafesi',
      ru: 'Испытание клетки',
      zh: '终极试炼 (Trial Cage)',
      de: 'Trial Cage',
      en: 'Trial Cage',
    },
    searchKeyword: 'cage',
  },
  {
    id: 'crystal_clash',
    label: {
      fr: 'Crystal Clash',
      tr: 'Kristal Çatışması',
      ru: 'Кристальная схватка',
      zh: '水晶争夺战',
      de: 'Kristallkampf',
      en: 'Crystal Clash',
    },
    searchKeyword: 'crystal clash',
  },
  {
    id: 'sandsea_conquest',
    label: {
      fr: 'Conquête de sable',
      tr: 'Kum Denizi Fethi',
      ru: 'Завоевание Песчаного моря',
      zh: '沙海征服',
      de: 'Sandmeer-Eroberung',
      en: 'Sandsea Conquest',
    },
    searchKeyword: 'sandsea',
  },
  {
    id: 'svs',
    label: {
      fr: 'SvS / Supreme Honor',
      tr: 'SvS / Yüce Onur',
      ru: 'SvS / Высшее отличие',
      zh: '跨服战 / 至高荣耀',
      de: 'SvS / Höchste Ehre',
      en: 'SvS / Supreme Honor',
    },
    searchKeyword: 'svs',
  },
  {
    id: 'turmoil_surge',
    label: {
      fr: 'Turmoil Surge',
      tr: 'Kargaşa Dalgası',
      ru: 'Волна смуты',
      zh: '动乱危机',
      de: 'Turmoil Surge',
      en: 'Turmoil Surge',
    },
    searchKeyword: 'turmoil',
  },
  {
    id: 'gorilla_legion',
    label: {
      fr: 'Gorilla Legion',
      tr: 'Goril Lejyonu',
      ru: 'Легион горилл',
      zh: '大猩猩军团',
      de: 'Gorilla-Legion',
      en: 'Gorilla Legion',
    },
    searchKeyword: 'gorilla legion',
  },
  {
    id: 'gold_hunter',
    label: {
      fr: 'Gold Hunter & Sprint',
      tr: 'Altın Avcısı',
      ru: 'Охотник за золотом',
      zh: '黄金猎人',
      de: 'Goldjäger',
      en: 'Gold Hunter & Sprint',
    },
    searchKeyword: 'gold hunter',
  },
];

export interface HeroSkill {
  id: string;
  nom: string;
  niveau: string;
  description?: string;
  iconUrl?: string;
  incertain: boolean;
  raisonIncertitude?: string;
}

export interface HeroRecord {
  id: string;
  nom: string;
  niveau: number | null;
  niveauTexte: string;
  rarete?: string;
  etoiles?: string;
  roleHero?: string;
  puissanceHero?: string;
  imageUrl?: string;
  avatarUrl?: string;
  titre?: string;
  typeTroupe?: string;
  categorie?: string;
  saison?: string;
  puissanceMax5Etoiles?: string;
  resumeBio?: string;
  idealPour?: string;
  wikiUrl?: string;
  competences: HeroSkill[];
  incertain: boolean;
  raisonIncertitude?: string;
  verifieManuellement?: boolean;
  derniereMaj: string;
}

export interface ResourceRecord {
  id: string;
  nom: string;
  quantiteTexte: string;
  quantiteNumerique: number | null;
  unite?: string;
  incertain: boolean;
  raisonIncertitude?: string;
  verifieManuellement?: boolean;
  derniereMaj: string;
}

export interface PowerMetric {
  puissanceTotaleTexte: string;
  puissanceTotaleNumerique: number | null;
  niveauPrisonOuQG?: string;
  niveauVip?: string;
  incertain: boolean;
  raisonIncertitude?: string;
}

export interface UncertainItem {
  champ: string;
  valeurObservee: string;
  raison: string;
}

export interface RobotSkill {
  id: string;
  nom: string;
  niveau: string;
  description?: string;
  incertain: boolean;
  raisonIncertitude?: string;
}

export interface RobotRecord {
  id: string;
  nom: string;
  niveau?: string;
  puissance?: string;
  rarete?: string;
  competences: RobotSkill[];
  incertain: boolean;
  raisonIncertitude?: string;
  verifieManuellement?: boolean;
  derniereMaj: string;
}

export interface ScreenshotAnalysis {
  id: string;
  analyzedAt: string;
  fileName: string;
  thumbnailDataUrl: string;
  typeCapture: ScreenCategory;
  resumeCapture: string;
  puissance: PowerMetric | null;
  ressources: ResourceRecord[];
  heros: HeroRecord[];
  robots: RobotRecord[];
  champsIncertains: UncertainItem[];
  noteUtilisateur?: string;
}

export interface GroundedWebSource {
  title: string;
  url: string;
  detectedDate?: string | null;
  imageUrl?: string | null;
}

export interface AdviceRecommendation {
  titre: string;
  detail: string;
  herosCibles: string[];
  incertain: boolean;
  raisonIncertitude?: string;
  imageUrl?: string | null;
}

export interface TacticalAdviceResult {
  id: string;
  mode: AdviceMode;
  playerRole: PlayerRole;
  language?: AppLanguage;
  generatedAt: string;
  synthese: string;
  compositionRecommandee: string;
  robotRecommande?: string;
  ratioTroupes?: string;
  recommandations: AdviceRecommendation[];
  informationsIncertaines: string[];
  sources: GroundedWebSource[];
  requetesRecherche: string[];
  heroesContextCount: number;
  geminiWarning?: string;
}

export type ForticladLabTroopType = 'shieldbearer' | 'bomber' | 'shooter';

export interface ForticladLabProgress {
  tier1: number;
  tier2: number[];
  tier3: number;
  tier4: boolean;
  tier5: number[];
}

export interface AccountState {
  language?: AppLanguage;
  playerRole: PlayerRole;
  currentPower: PowerMetric | null;
  forticladLevel?: number;
  forticladBuildings?: Record<string, { level: number; completedStages: number; targetLevel: number }>;
  forticladLabProgress?: Partial<Record<ForticladLabTroopType, ForticladLabProgress>>;
  powerHistory: Array<{
    analysisId: string;
    date: string;
    puissanceNumerique: number | null;
    puissanceTexte: string;
    incertain: boolean;
  }>;
  heroes: HeroRecord[];
  robots: RobotRecord[];
  resources: ResourceRecord[];
  analyses: ScreenshotAnalysis[];
  adviceByMode: Partial<Record<AdviceMode, TacticalAdviceResult>>;
}
