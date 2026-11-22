export type HeroClass = 'Shieldbearer' | 'Shooter' | 'Bomber';

export interface BuildingLevelRequirement {
  level: number;
  requiredBuildings: string[];
  food: number;
  wood: number;
  steel: number;
  gasoline: number;
  baseTimeMinutes: number;
  unlocks?: string;
  verifiedWiki: boolean;
  noteIncertitude?: string;
}

export interface BuildingDefinition {
  id: string;
  nomFr: string;
  nomEn: string;
  descriptionFr: string;
  levels: BuildingLevelRequirement[];
}

export interface ForticladStageCost {
  fcLevel: number; // 1..5
  forticladCores: number;
  advancedForticladCores: number;
  prerequisites: string;
  verifiedWiki: boolean;
}

export interface HeroStarTier {
  star: number;
  label: string;
  shardsForTier: number;
  cumulativeFromOneStar: number;
  perSegmentNote: string;
  skillCapUnlocked: string;
}

export interface TroopTomeLevelCost {
  fromLevel: number;
  toLevel: number;
  sealOfWisdom: number;
  sealOfKnowledge: number;
  mysteryTome?: number;
  verifiedWiki: boolean;
}

export interface KnownHeroMeta {
  nom: string;
  rarity: 'SSR' | 'SR' | 'R';
  heroClass: HeroClass;
  roleTrialCage: string;
  roleCrystalClash: string;
  priorityNote: string;
}

// Coûts exacts relevés sur miraheze.org & lojxlab.com pour le Warden Office (Bureau du directeur)
// Les niveaux sans tableau complet publié sont marqués verifiedWiki: false pour transparence.
export const WARDEN_OFFICE_LEVELS: BuildingLevelRequirement[] = [
  { level: 1, requiredBuildings: [], food: 0, wood: 0, steel: 0, gasoline: 0, baseTimeMinutes: 0, unlocks: 'Début de la prison', verifiedWiki: true },
  { level: 2, requiredBuildings: ['Cuisine (Kitchen) Niv. 1'], food: 0, wood: 350, steel: 0, gasoline: 0, baseTimeMinutes: 0.1, verifiedWiki: true },
  { level: 3, requiredBuildings: ['Cellule 1 (Cell 1) Niv. 2'], food: 0, wood: 1_600, steel: 0, gasoline: 0, baseTimeMinutes: 1, verifiedWiki: true },
  { level: 4, requiredBuildings: ['Usine (Factory) Niv. 3'], food: 0, wood: 3_600, steel: 700, gasoline: 0, baseTimeMinutes: 3, verifiedWiki: true },
  { level: 5, requiredBuildings: ['Cellule 3 (Cell 3) Niv. 3'], food: 0, wood: 15_000, steel: 3_000, gasoline: 0, baseTimeMinutes: 6, unlocks: 'Système des Héros', verifiedWiki: true },
  { level: 6, requiredBuildings: ['Centre d’approvisionnement Niv. 1', 'Douches 1 Niv. 5'], food: 0, wood: 40_000, steel: 8_000, gasoline: 2_000, baseTimeMinutes: 30, verifiedWiki: true },
  { level: 7, requiredBuildings: ['Cour (Playground) Niv. 5'], food: 0, wood: 140_000, steel: 28_000, gasoline: 2_000, baseTimeMinutes: 60, verifiedWiki: true },
  { level: 8, requiredBuildings: ['Caserne Shieldbearers Niv. 7'], food: 0, wood: 240_000, steel: 48_000, gasoline: 12_000, baseTimeMinutes: 150, unlocks: 'Centre de communications & Arène', verifiedWiki: true },
  { level: 9, requiredBuildings: ['Centre de communications Niv. 8', 'Infirmerie Niv. 1'], food: 0, wood: 500_000, steel: 100_000, gasoline: 25_000, baseTimeMinutes: 270, verifiedWiki: true },
  { level: 10, requiredBuildings: ['Caserne Shooters Niv. 9', 'Laboratoire Niv. 1'], food: 0, wood: 900_000, steel: 180_000, gasoline: 45_000, baseTimeMinutes: 360, verifiedWiki: true },
  { level: 11, requiredBuildings: ['Centre de communications Niv. 10', 'Caserne Bombers Niv. 10'], food: 2_500_000, wood: 2_500_000, steel: 500_000, gasoline: 125_000, baseTimeMinutes: 450, verifiedWiki: true },
  { level: 12, requiredBuildings: ['Centre de communications Niv. 11', 'Centre de commandement Niv. 1'], food: 3_000_000, wood: 3_000_000, steel: 600_000, gasoline: 150_000, baseTimeMinutes: 540, verifiedWiki: true },
  { level: 13, requiredBuildings: ['Centre de communications Niv. 12', 'Caserne Shieldbearers Niv. 12'], food: 4_500_000, wood: 4_500_000, steel: 900_000, gasoline: 225_000, baseTimeMinutes: 660, verifiedWiki: true },
  { level: 14, requiredBuildings: ['Centre de communications Niv. 13', 'Caserne Shooters Niv. 13'], food: 6_000_000, wood: 6_000_000, steel: 1_200_000, gasoline: 300_000, baseTimeMinutes: 840, verifiedWiki: true },
  { level: 15, requiredBuildings: ['Centre de communications Niv. 14', 'Caserne Bombers Niv. 14'], food: 9_000_000, wood: 9_000_000, steel: 1_800_000, gasoline: 450_000, baseTimeMinutes: 1_080, verifiedWiki: true },
  { level: 16, requiredBuildings: ['Centre de communications Niv. 15', 'Caserne Shieldbearers Niv. 15'], food: 18_000_000, wood: 18_000_000, steel: 3_600_000, gasoline: 900_000, baseTimeMinutes: 2_160, unlocks: 'Accès Crystal Clash & Cellule 17', verifiedWiki: true },
  { level: 17, requiredBuildings: ['Centre de communications Niv. 16', 'Caserne Shooters Niv. 16'], food: 25_000_000, wood: 25_000_000, steel: 5_000_000, gasoline: 1_200_000, baseTimeMinutes: 2_580, unlocks: 'Cellule 18', verifiedWiki: true },
  { level: 18, requiredBuildings: ['Centre de communications Niv. 17', 'Caserne Bombers Niv. 17'], food: 30_000_000, wood: 30_000_000, steel: 6_000_000, gasoline: 1_500_000, baseTimeMinutes: 3_900, unlocks: 'Cellule 19', verifiedWiki: true },
  { level: 19, requiredBuildings: ['Centre de communications Niv. 18', 'Caserne / Bâtiment rotatif Niv. 18'], food: 35_000_000, wood: 35_000_000, steel: 7_000_000, gasoline: 1_750_000, baseTimeMinutes: 4_440, verifiedWiki: false, noteIncertitude: 'Prérequis exact du bâtiment secondaire et coût exact à confirmer en jeu.' },
  { level: 20, requiredBuildings: ['Centre de communications Niv. 19', 'Laboratoire Niv. 19'], food: 40_000_000, wood: 40_000_000, steel: 8_000_000, gasoline: 2_000_000, baseTimeMinutes: 4_938, verifiedWiki: true },
  { level: 21, requiredBuildings: ['Centre de communications Niv. 20', 'Caserne Shieldbearers Niv. 20'], food: 55_000_000, wood: 55_000_000, steel: 11_000_000, gasoline: 2_750_000, baseTimeMinutes: 7_200, verifiedWiki: false, noteIncertitude: 'Progression 21-24 estimée (~200+ jours cumulés jusqu’à 30 selon lojxlab).' },
  { level: 22, requiredBuildings: ['Centre de communications Niv. 21', 'Caserne Shooters Niv. 21'], food: 75_000_000, wood: 75_000_000, steel: 15_000_000, gasoline: 3_750_000, baseTimeMinutes: 10_080, verifiedWiki: false, noteIncertitude: 'Coût exact et bâtiment secondaire rotatif à vérifier en jeu.' },
  { level: 23, requiredBuildings: ['Centre de communications Niv. 22', 'Caserne Bombers Niv. 22'], food: 100_000_000, wood: 100_000_000, steel: 20_000_000, gasoline: 5_000_000, baseTimeMinutes: 14_400, verifiedWiki: false, noteIncertitude: 'Coût exact et bâtiment secondaire rotatif à vérifier en jeu.' },
  { level: 24, requiredBuildings: ['Centre de communications Niv. 23', 'Laboratoire / Caserne Niv. 23'], food: 130_000_000, wood: 130_000_000, steel: 26_000_000, gasoline: 6_500_000, baseTimeMinutes: 20_160, verifiedWiki: false, noteIncertitude: 'Coût exact et bâtiment secondaire rotatif à vérifier en jeu.' },
  { level: 25, requiredBuildings: ['Centre de communications Niv. 24', 'Bâtiment militaire/labo Niv. 24'], food: 160_000_000, wood: 160_000_000, steel: 32_000_000, gasoline: 8_000_000, baseTimeMinutes: 26_422, verifiedWiki: true },
  { level: 26, requiredBuildings: ['Centre de communications Niv. 25', 'Caserne Niv. 25'], food: 210_000_000, wood: 210_000_000, steel: 42_000_000, gasoline: 10_500_000, baseTimeMinutes: 34_560, verifiedWiki: false, noteIncertitude: 'Estimation basée sur la courbe 25→30 ; à confirmer sur votre serveur.' },
  { level: 27, requiredBuildings: ['Centre de communications Niv. 26', 'Caserne Niv. 26'], food: 270_000_000, wood: 270_000_000, steel: 54_000_000, gasoline: 13_500_000, baseTimeMinutes: 43_200, unlocks: 'Plafond hors-ligne 12h', verifiedWiki: false, noteIncertitude: 'Déblocage 12h hors-ligne confirmé ; montants exacts estimés.' },
  { level: 28, requiredBuildings: ['Centre de communications Niv. 27', 'Laboratoire Niv. 27'], food: 340_000_000, wood: 340_000_000, steel: 68_000_000, gasoline: 17_000_000, baseTimeMinutes: 51_840, verifiedWiki: false, noteIncertitude: 'Estimation — vérifier en jeu avant de lancer l’événement.' },
  { level: 29, requiredBuildings: ['Centre de communications Niv. 28', 'Caserne Niv. 28'], food: 430_000_000, wood: 430_000_000, steel: 86_000_000, gasoline: 21_500_000, baseTimeMinutes: 64_800, verifiedWiki: false, noteIncertitude: 'Estimation — vérifier en jeu avant de lancer l’événement.' },
  { level: 30, requiredBuildings: ['Centre de communications Niv. 29', 'Bâtiment Niv. 29'], food: 550_000_000, wood: 550_000_000, steel: 110_000_000, gasoline: 27_500_000, baseTimeMinutes: 79_200, unlocks: 'Débloque les niveaux Forticlad (FC1+)', verifiedWiki: false, noteIncertitude: 'Prérequis Comms 29 et transition Forticlad confirmés ; montants exacts variables selon patch.' },
];

// Fonction utilitaire pour générer une estimation proportionnelle des autres bâtiments clés
// (clairement signalée comme estimation par rapport au Warden Office)
function scaleBuildingLevels(ratioFood: number, ratioWood: number, ratioTime: number, reqFn: (lvl: number) => string[]): BuildingLevelRequirement[] {
  return WARDEN_OFFICE_LEVELS.map((w) => ({
    level: w.level,
    requiredBuildings: reqFn(w.level),
    food: Math.round(w.food * ratioFood),
    wood: Math.round(w.wood * ratioWood),
    steel: Math.round(w.steel * ratioWood),
    gasoline: Math.round(w.gasoline * ratioWood),
    baseTimeMinutes: Math.max(1, Math.round(w.baseTimeMinutes * ratioTime)),
    verifiedWiki: false,
    noteIncertitude: 'Estimation proportionnelle au Warden Office (les valeurs exactes varient par palier sur chaque serveur).',
  }));
}

export const BUILDINGS_CATALOG: BuildingDefinition[] = [
  {
    id: 'warden_office',
    nomFr: 'Bureau du directeur (Warden Office)',
    nomEn: 'Warden Office',
    descriptionFr: 'Bâtiment central. Applique la règle du Centre de communications (N-1) dès le niveau 9.',
    levels: WARDEN_OFFICE_LEVELS,
  },
  {
    id: 'comms_center',
    nomFr: 'Centre de communications (Comms Center)',
    nomEn: 'Communications Center',
    descriptionFr: 'Prérequis obligatoire à N-1 pour monter le Bureau du directeur de 9 à 30.',
    levels: scaleBuildingLevels(0.55, 0.55, 0.5, (lvl) =>
      lvl <= 1 ? [] : [`Bureau du directeur Niv. ${lvl}`]
    ),
  },
  {
    id: 'shield_barracks',
    nomFr: 'Caserne Shieldbearers',
    nomEn: 'Shieldbearer Barracks',
    descriptionFr: 'Entraîne vos troupes de première ligne (tanks). Souvent requise pour le Warden Office.',
    levels: scaleBuildingLevels(0.45, 0.45, 0.42, (lvl) =>
      lvl <= 1 ? [] : [`Bureau du directeur Niv. ${lvl}`]
    ),
  },
  {
    id: 'shooter_barracks',
    nomFr: 'Caserne Shooters',
    nomEn: 'Shooter Barracks',
    descriptionFr: 'Entraîne les tireurs DPS arrière indispensables en Trial Cage.',
    levels: scaleBuildingLevels(0.45, 0.45, 0.42, (lvl) =>
      lvl <= 1 ? [] : [`Bureau du directeur Niv. ${lvl}`]
    ),
  },
  {
    id: 'bomber_barracks',
    nomFr: 'Caserne Bombers',
    nomEn: 'Bomber Barracks',
    descriptionFr: 'Entraîne les unités de zone et contrôle.',
    levels: scaleBuildingLevels(0.45, 0.45, 0.42, (lvl) =>
      lvl <= 1 ? [] : [`Bureau du directeur Niv. ${lvl}`]
    ),
  },
  {
    id: 'lab',
    nomFr: 'Laboratoire (Lab)',
    nomEn: 'Lab',
    descriptionFr: 'Recherches de combat, vitesse de construction et capacité de marche.',
    levels: scaleBuildingLevels(0.5, 0.5, 0.45, (lvl) =>
      lvl <= 1 ? [] : [`Bureau du directeur Niv. ${lvl}`]
    ),
  },
  {
    id: 'warehouse',
    nomFr: 'Entrepôt (Warehouse)',
    nomEn: 'Warehouse',
    descriptionFr: 'Protège vos ressources du pillage. À monter en parallèle du Bureau du directeur.',
    levels: scaleBuildingLevels(0.35, 0.35, 0.32, (lvl) =>
      lvl <= 1 ? [] : [`Bureau du directeur Niv. ${lvl}`]
    ),
  },
];

// Données Forticlad (après niveau 30) : chaque niveau FC comporte 5 paliers (étapes 1 à 5)
export const FORTICLAD_STAGES: ForticladStageCost[] = [
  { fcLevel: 1, forticladCores: 600, advancedForticladCores: 0, prerequisites: 'Warden Office Niv. 30 + 5 paliers d’amélioration', verifiedWiki: false },
  { fcLevel: 2, forticladCores: 900, advancedForticladCores: 40, prerequisites: 'Bâtiment FC1 complet + Comms/Caserne FC1', verifiedWiki: false },
  { fcLevel: 3, forticladCores: 1_350, advancedForticladCores: 90, prerequisites: 'Bâtiment FC2 complet + Comms/Caserne FC2', verifiedWiki: false },
  { fcLevel: 4, forticladCores: 1_900, advancedForticladCores: 160, prerequisites: 'Bâtiment FC3 complet', verifiedWiki: false },
  { fcLevel: 5, forticladCores: 2_600, advancedForticladCores: 250, prerequisites: 'Bâtiment FC4 complet', verifiedWiki: false },
  // FC6–8 : GuideLOJ publie les totaux de 7 bâtiments (hors laboratoire).
  // Moyenne indicative par bâtiment pour le calcul multi-bâtiments ; non vérifiée bâtiment par bâtiment.
  { fcLevel: 6, forticladCores: 764, advancedForticladCores: 49, prerequisites: 'Bâtiment FC5 complet · moyenne indicative GuideLOJ (7 bâtiments)', verifiedWiki: false },
  { fcLevel: 7, forticladCores: 857, advancedForticladCores: 75, prerequisites: 'Bâtiment FC6 complet · moyenne indicative GuideLOJ (7 bâtiments)', verifiedWiki: false },
  { fcLevel: 8, forticladCores: 985, advancedForticladCores: 107, prerequisites: 'Bâtiment FC7 complet · moyenne indicative GuideLOJ (7 bâtiments)', verifiedWiki: false },
];

// Éclats de héros exacts vérifiés sur lojxlab.com (Total = 1 075 éclats de 0★ à 5★, + 10 au déblocage)
export const HERO_SHARD_TIERS: HeroStarTier[] = [
  {
    star: 0,
    label: 'Déblocage du héros',
    shardsForTier: 10,
    cumulativeFromOneStar: 0,
    perSegmentNote: '10 éclats pour recruter un héros non débloqué',
    skillCapUnlocked: 'Compétences Niv. 1–2',
  },
  {
    star: 1,
    label: '1 étoile (★)',
    shardsForTier: 20,
    cumulativeFromOneStar: 20,
    perSegmentNote: '6 paliers (paliers 1-2 moins chers, palier 6 plus élevé)',
    skillCapUnlocked: 'Compétences Niv. 2',
  },
  {
    star: 2,
    label: '2 étoiles (★★)',
    shardsForTier: 40,
    cumulativeFromOneStar: 60,
    perSegmentNote: '5×5 éclats + 15 éclats au 6ᵉ segment',
    skillCapUnlocked: 'Compétences Niv. 3',
  },
  {
    star: 3,
    label: '3 étoiles (★★★)',
    shardsForTier: 115,
    cumulativeFromOneStar: 175,
    perSegmentNote: '5×15 éclats + 40 éclats au 6ᵉ segment',
    skillCapUnlocked: 'Compétences Niv. 4',
  },
  {
    star: 4,
    label: '4 étoiles (★★★★) — Seuil clé',
    shardsForTier: 300,
    cumulativeFromOneStar: 475,
    perSegmentNote: '5×40 éclats + 100 éclats au 6ᵉ segment',
    skillCapUnlocked: 'Débloque compétences Niv. 5',
  },
  {
    star: 5,
    label: '5 étoiles (★★★★★) — Max',
    shardsForTier: 600,
    cumulativeFromOneStar: 1_075,
    perSegmentNote: '6×100 éclats (coûte plus cher que ★1 à ★4 réunis)',
    skillCapUnlocked: 'Stats et compétences au maximum',
  },
];

// Coûts exacts des Tomes de Troupes (Troop Tomes) vérifiés sur lojxlab.com
export const TROOP_TOME_COSTS: TroopTomeLevelCost[] = [
  { fromLevel: 0, toLevel: 1, sealOfWisdom: 10, sealOfKnowledge: 10, verifiedWiki: true },
  { fromLevel: 1, toLevel: 2, sealOfWisdom: 50, sealOfKnowledge: 60, verifiedWiki: true },
  { fromLevel: 2, toLevel: 3, sealOfWisdom: 90, sealOfKnowledge: 110, verifiedWiki: true },
  { fromLevel: 3, toLevel: 4, sealOfWisdom: 160, sealOfKnowledge: 200, verifiedWiki: true },
  { fromLevel: 4, toLevel: 5, sealOfWisdom: 270, sealOfKnowledge: 330, verifiedWiki: true },
  { fromLevel: 5, toLevel: 6, sealOfWisdom: 380, sealOfKnowledge: 460, verifiedWiki: true },
  { fromLevel: 6, toLevel: 7, sealOfWisdom: 510, sealOfKnowledge: 620, verifiedWiki: false },
  { fromLevel: 7, toLevel: 8, sealOfWisdom: 660, sealOfKnowledge: 800, verifiedWiki: false },
  { fromLevel: 8, toLevel: 9, sealOfWisdom: 840, sealOfKnowledge: 1_020, verifiedWiki: false },
  { fromLevel: 9, toLevel: 10, sealOfWisdom: 1_050, sealOfKnowledge: 1_280, verifiedWiki: false },
  { fromLevel: 12, toLevel: 13, sealOfWisdom: 1_800, sealOfKnowledge: 2_200, mysteryTome: 1, verifiedWiki: false },
];

// Livres de compétences de héros (Arrest Skill Books & War Skill Books)
export interface SkillBookStep {
  fromLevel: number;
  toLevel: number;
  estimatedBooks: number;
  verifiedWiki: boolean;
}

export const HERO_SKILL_BOOK_STEPS: SkillBookStep[] = [
  { fromLevel: 1, toLevel: 2, estimatedBooks: 10, verifiedWiki: false },
  { fromLevel: 2, toLevel: 3, estimatedBooks: 25, verifiedWiki: false },
  { fromLevel: 3, toLevel: 4, estimatedBooks: 50, verifiedWiki: false },
  { fromLevel: 4, toLevel: 5, estimatedBooks: 100, verifiedWiki: false },
];

export const KNOWN_HEROES_META: KnownHeroMeta[] = [
  {
    nom: 'Tyronn',
    rarity: 'SSR',
    heroClass: 'Shieldbearer',
    roleTrialCage: 'Tank principal de première ligne ; priorité d’éclats n°1.',
    roleCrystalClash: 'Tenue de points stratégiques (Ironprison / Pillbox).',
    priorityNote: 'Incontournable en première ligne avant d’investir la 5ᵉ étoile sur d’autres SSR.',
  },
  {
    nom: 'Phoenix',
    rarity: 'SSR',
    heroClass: 'Shooter',
    roleTrialCage: 'DPS polyvalent élevé pour maximiser les dégâts sur le boss.',
    roleCrystalClash: 'Soutien offensif rapide sur les prises de points.',
    priorityNote: 'Très fort rapport polyvalence/coût en F2P et joueurs intermédiaires.',
  },
  {
    nom: 'Rin',
    rarity: 'SSR',
    heroClass: 'Shooter',
    roleTrialCage: 'Très gros DPS arrière (méta S7+ selon lojxlab).',
    roleCrystalClash: 'Nettoyage des défenseurs sur points de cristal.',
    priorityNote: 'À associer avec un tank solide devant.',
  },
  {
    nom: 'Edwin',
    rarity: 'SSR',
    heroClass: 'Shooter',
    roleTrialCage: 'Dégâts soutenus en ralliement Shooter.',
    roleCrystalClash: 'Leader de ralliement offensif.',
    priorityNote: 'Efficace si vos casernes Shooters sont avancées.',
  },
  {
    nom: 'Otto',
    rarity: 'SSR',
    heroClass: 'Shieldbearer',
    roleTrialCage: 'Tank secondaire / bouclier.',
    roleCrystalClash: 'Garnison et défense de Crystal Ironhold.',
    priorityNote: 'Bon complément défensif de Tyronn.',
  },
  {
    nom: 'Marcus',
    rarity: 'SSR',
    heroClass: 'Shieldbearer',
    roleTrialCage: 'Absorption de dégâts en front-line.',
    roleCrystalClash: 'Défenseur de garnison.',
    priorityNote: 'Solide en début/milieu de partie.',
  },
  {
    nom: 'Ada',
    rarity: 'SSR',
    heroClass: 'Shooter',
    roleTrialCage: 'Dégâts à distance réguliers.',
    roleCrystalClash: 'Escarmouches et dépôts de cristal.',
    priorityNote: 'Monter jusqu’à 4★ avant de viser 5★.',
  },
  {
    nom: 'Lee',
    rarity: 'SSR',
    heroClass: 'Bomber',
    roleTrialCage: 'Contrôle / dégâts de zone (moins prioritaire qu’un mono-DPS sur boss).',
    roleCrystalClash: 'Perturbation des packs ennemis au centre.',
    priorityNote: 'Utile en PvP multi-escouades.',
  },
  {
    nom: 'Patrick',
    rarity: 'SR',
    heroClass: 'Shieldbearer',
    roleTrialCage: 'Tank F2P accessible tant que vos SSR ne sont pas à 4★.',
    roleCrystalClash: 'Garnison temporaire ou rejoigneur de ralliement.',
    priorityNote: 'Ne pas dépasser le strict nécessaire si vous avez Tyronn.',
  },
  {
    nom: 'Mia',
    rarity: 'SR',
    heroClass: 'Bomber',
    roleTrialCage: 'Soin et maintien en début de jeu.',
    roleCrystalClash: 'Rejoigneur / soutien économique.',
    priorityNote: 'Bon choix transition F2P.',
  },
];

export type RallyRole = 'lanceur' | 'rejoigneur';

export interface RallyHeroPick {
  nom: string;
  rallyRole: RallyRole;
  heroClass: HeroClass;
  note: string;
}

// Compositions de ralliement Trial Cage vérifiées sur guideloj.com, lojxlab.com,
// allclash.com et guides YouTube spécialisés (méta saisons 1-2).
export const RALLY_HERO_PICKS: RallyHeroPick[] = [
  {
    nom: 'Tyron',
    rallyRole: 'lanceur',
    heroClass: 'Shieldbearer',
    note: 'Meilleur lanceur de ralliement : énorme bonus de dégâts de groupe dès 2★.',
  },
  {
    nom: 'Phoenix',
    rallyRole: 'lanceur',
    heroClass: 'Shieldbearer',
    note: 'Alternative solide si Tyron n’est pas débloqué.',
  },
  {
    nom: 'Vivian',
    rallyRole: 'lanceur',
    heroClass: 'Bomber',
    note: 'Meilleur Bombier de ralliement en saisons 1-2.',
  },
  {
    nom: 'Devillian',
    rallyRole: 'lanceur',
    heroClass: 'Shooter',
    note: 'Tireur d’ouverture de ralliement, dégâts concentrés sur le boss.',
  },
  {
    nom: 'Whisper',
    rallyRole: 'lanceur',
    heroClass: 'Bomber',
    note: 'Remplace Vivian à partir de la saison 3.',
  },
  {
    nom: 'Rin',
    rallyRole: 'rejoigneur',
    heroClass: 'Shooter',
    note: 'Bonus d’attaque globale : idéale en 1er slot de renfort.',
  },
  {
    nom: 'Mireya',
    rallyRole: 'rejoigneur',
    heroClass: 'Shooter',
    note: '+25% attaque des troupes et +20% dégâts : ne booster que depuis le 1er slot.',
  },
  {
    nom: 'Edwin',
    rallyRole: 'rejoigneur',
    heroClass: 'Shooter',
    note: 'Bon rejoigneur offensif si vos casernes Shooters sont avancées.',
  },
  {
    nom: 'Ada',
    rallyRole: 'rejoigneur',
    heroClass: 'Shooter',
    note: 'Dégâts à distance réguliers en renfort de ralliement.',
  },
];

// Ratio de troupes recommandé pour un ralliement Trial Cage (55/5/40)
export const RALLY_TROOP_RATIOS: Array<{ classe: HeroClass; pourcentage: number; note: string }> = [
  {
    classe: 'Shieldbearer',
    pourcentage: 55,
    note: 'Absorbe les ripostes du boss et protège les lignes arrière.',
  },
  {
    classe: 'Bomber',
    pourcentage: 5,
    note: 'Quota minimal : sert de déclencheur de compétences.',
  },
  {
    classe: 'Shooter',
    pourcentage: 40,
    note: 'Cœur des dégâts sur le boss de la Cage.',
  },
];

export const RALLY_JOIN_TIPS: string[] = [
  'Seul le 1er slot de héros compte pour booster un ralliement que vous rejoignez.',
  'En rejoigneur, privilégiez les héros à bonus d’attaque et de dégâts (pas de létalité, réservée aux attaques de prison).',
  'Si possible, rejoignez avec 100% de Shooters : ce sont eux qui infligent le plus de dégâts sur un ralliement par défaut.',
  'Préparez des formations enregistrées dans le jeu pour rejoindre rapidement avec le bon héros et le bon ratio.',
];

export function formatMinutesToDurationFr(totalMinutes: number): string {
  if (totalMinutes <= 0) return 'Instantané';
  if (totalMinutes < 1) return `${Math.round(totalMinutes * 60)} s`;
  const days = Math.floor(totalMinutes / 1440);
  const hours = Math.floor((totalMinutes % 1440) / 60);
  const mins = Math.round(totalMinutes % 60);
  const parts: string[] = [];
  if (days > 0) parts.push(`${days}j`);
  if (hours > 0) parts.push(`${hours}h`);
  if (mins > 0 && days === 0) parts.push(`${mins}m`);
  return parts.join(' ') || '< 1m';
}
