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
  | 'autre';

export const SCREEN_CATEGORY_LABELS: Record<ScreenCategory, string> = {
  profil: 'Profil & Puissance',
  heros: 'Héros & Compétences',
  ressources: 'Ressources & Entrepôt',
  trial_cage: 'Trial Cage',
  crystal_clash: 'Crystal Clash',
  autre: 'Autre écran',
};

export interface HeroSkill {
  id: string;
  nom: string;
  niveau: string;
  description?: string;
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
  champsIncertains: UncertainItem[];
  noteUtilisateur?: string;
}

export type AdviceMode = 'trial_cage' | 'crystal_clash';

export interface GroundedWebSource {
  title: string;
  url: string;
  detectedDate?: string | null;
}

export interface AdviceRecommendation {
  titre: string;
  detail: string;
  herosCibles: string[];
  incertain: boolean;
  raisonIncertitude?: string;
}

export interface TacticalAdviceResult {
  id: string;
  mode: AdviceMode;
  playerRole: PlayerRole;
  generatedAt: string;
  synthese: string;
  compositionRecommandee: string;
  recommandations: AdviceRecommendation[];
  informationsIncertaines: string[];
  sources: GroundedWebSource[];
  requetesRecherche: string[];
  heroesContextCount: number;
}

export interface AccountState {
  playerRole: PlayerRole;
  currentPower: PowerMetric | null;
  powerHistory: Array<{
    analysisId: string;
    date: string;
    puissanceNumerique: number | null;
    puissanceTexte: string;
    incertain: boolean;
  }>;
  heroes: HeroRecord[];
  resources: ResourceRecord[];
  analyses: ScreenshotAnalysis[];
  adviceByMode: Partial<Record<AdviceMode, TacticalAdviceResult>>;
}
