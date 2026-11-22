import React, { useMemo, useState } from 'react';
import { AlertTriangle, Building2, CheckCircle2, Clock, Sparkles, Users } from 'lucide-react';
import {
  BUILDINGS_CATALOG,
  FORTICLAD_BUILDINGS,
  HERO_SHARD_TIERS,
  HERO_SKILL_BOOK_STEPS,
  KNOWN_HEROES_META,
  TROOP_TOME_COSTS,
  formatMinutesToDurationFr,
} from '../data/landsOfJailData';
import { AccountState } from '../types/game';
import { formatCompactNumber } from '../utils/storage';

interface UpgradeCalculatorViewProps {
  state: AccountState;
  onUpdateForticladLevel: (level: number) => void;
  onUpdateForticladBuildings: (progress: AccountState['forticladBuildings']) => void;
}

type CalcSection = 'buildings' | 'forticlad' | 'heroes';

function findScannedResourceAmount(state: AccountState, keywords: string[]): number | null {
  for (const res of state.resources) {
    const lower = res.nom.toLowerCase();
    if (keywords.some((kw) => lower.includes(kw))) {
      if (typeof res.quantiteNumerique === 'number' && !Number.isNaN(res.quantiteNumerique)) {
        return res.quantiteNumerique;
      }
      // Tentative de parsing si texte type "12.5M" ou "450K"
      const cleaned = res.quantiteTexte.replace(/\s+/g, '').replace(',', '.');
      const match = cleaned.match(/^(\d+(?:\.\d+)?)([kmb])?$/i);
      if (match) {
        const val = parseFloat(match[1]);
        const mult =
          match[2]?.toLowerCase() === 'm'
            ? 1_000_000
            : match[2]?.toLowerCase() === 'k'
            ? 1_000
            : match[2]?.toLowerCase() === 'b'
            ? 1_000_000_000
            : 1;
        return Math.round(val * mult);
      }
    }
  }
  return null;
}

function findForticladResourceAmount(state: AccountState, advanced: boolean): number | null {
  const resource = state.resources.find((item) => {
    const name = item.nom.normalize('NFD').replace(/[\\u0300-\\u036f]/g, '').toLowerCase();
    const isForticladCore = name.includes('forticlad') && (name.includes('core') || name.includes('noyau'));
    const isAdvanced = name.includes('advanced') || name.includes('avance');
    return isForticladCore && isAdvanced === advanced;
  });
  if (!resource) return null;
  if (typeof resource.quantiteNumerique === 'number' && Number.isFinite(resource.quantiteNumerique)) {
    return resource.quantiteNumerique;
  }
  const cleaned = resource.quantiteTexte.replace(/\\s+/g, '').replace(',', '.');
  const match = cleaned.match(/^(\\d+(?:\\.\\d+)?)([kmb])?$/i);
  if (!match) return null;
  const multiplier = match[2]?.toLowerCase() === 'm' ? 1_000_000
    : match[2]?.toLowerCase() === 'k' ? 1_000
    : match[2]?.toLowerCase() === 'b' ? 1_000_000_000 : 1;
  return Math.round(Number(match[1]) * multiplier);
}

export const UpgradeCalculatorView: React.FC<UpgradeCalculatorViewProps> = ({ state, onUpdateForticladLevel, onUpdateForticladBuildings }) => {
  const [section, setSection] = useState<CalcSection>('buildings');

  // --- Calculateur Bâtiments (1 à 30) ---
  const parsedQgLevel = useMemo(() => {
    const raw = state.currentPower?.niveauPrisonOuQG || '';
    const m = raw.match(/\d+/);
    const n = m ? Number(m[0]) : 10;
    return Number.isFinite(n) ? Math.min(29, Math.max(1, n)) : 10;
  }, [state.currentPower?.niveauPrisonOuQG]);

  const [selectedBuildingId, setSelectedBuildingId] = useState<string>('warden_office');
  const [currentLevel, setCurrentLevel] = useState<number>(parsedQgLevel);
  const [targetLevel, setTargetLevel] = useState<number>(Math.min(30, parsedQgLevel + 1));
  const [constructionBonusPct, setConstructionBonusPct] = useState<number>(35);

  const selectedBuilding = useMemo(
    () => BUILDINGS_CATALOG.find((b) => b.id === selectedBuildingId) || BUILDINGS_CATALOG[0],
    [selectedBuildingId]
  );

  const buildingCalculation = useMemo(() => {
    const start = Math.max(1, Math.min(29, currentLevel));
    const end = Math.max(start + 1, Math.min(30, targetLevel));
    const steps = selectedBuilding.levels.filter((l) => l.level > start && l.level <= end);

    const totals = steps.reduce(
      (acc, step) => ({
        food: acc.food + step.food,
        wood: acc.wood + step.wood,
        steel: acc.steel + step.steel,
        gasoline: acc.gasoline + step.gasoline,
        baseTimeMinutes: acc.baseTimeMinutes + step.baseTimeMinutes,
        allVerified: acc.allVerified && step.verifiedWiki,
        prerequisites: [...acc.prerequisites, ...step.requiredBuildings],
        unlocks: step.unlocks ? [...acc.unlocks, `Niv. ${step.level} : ${step.unlocks}`] : acc.unlocks,
      }),
      {
        food: 0,
        wood: 0,
        steel: 0,
        gasoline: 0,
        baseTimeMinutes: 0,
        allVerified: true,
        prerequisites: [] as string[],
        unlocks: [] as string[],
      }
    );

    const speedFactor = 1 + Math.max(0, constructionBonusPct) / 100;
    const effectiveMinutes = totals.baseTimeMinutes / speedFactor;

    return {
      start,
      end,
      steps,
      ...totals,
      effectiveMinutes,
    };
  }, [selectedBuilding, currentLevel, targetLevel, constructionBonusPct]);

  // Comparaison automatique avec les ressources scannées sur le compte
  const ownedFood = findScannedResourceAmount(state, ['food', 'nourriture', 'vivres']);
  const ownedWood = findScannedResourceAmount(state, ['wood', 'bois']);
  const ownedSteel = findScannedResourceAmount(state, ['steel', 'acier', 'fer']);
  const ownedGas = findScannedResourceAmount(state, ['gas', 'essence', 'pétrole', 'petrole', 'fuel']);

  const ownedForticladCores = findForticladResourceAmount(state, false);
  const ownedAdvancedForticladCores = findForticladResourceAmount(state, true);

  // --- Calculateur Forticlad individualisé par bâtiment ---
  type ForticladProgress = { level: number; completedStages: number; targetLevel: number };
  const [forticladProgress, setForticladProgress] = useState<Record<string, ForticladProgress>>(() => {
    const saved = state.forticladBuildings ?? {};
    return Object.fromEntries(FORTICLAD_BUILDINGS.map((building) => {
      const existing = saved[building.id];
      const level = Math.max(0, Math.min(8, existing?.level ?? state.forticladLevel ?? 0));
      return [building.id, {
        level,
        completedStages: Math.max(0, Math.min(4, existing?.completedStages ?? 0)),
        targetLevel: Math.max(level + 1, Math.min(8, existing?.targetLevel ?? Math.min(8, level + 1))),
      }];
    }));
  });

  const updateForticladBuilding = (buildingId: string, patch: Partial<ForticladProgress>) => {
    const next = { ...forticladProgress[buildingId], ...patch };
    const updated = { ...forticladProgress, [buildingId]: next };
    setForticladProgress(updated);
    onUpdateForticladBuildings(updated);
    if (buildingId === 'warden_office' && patch.level !== undefined) onUpdateForticladLevel(patch.level);
  };

  const forticladCalculation = useMemo(() => {
    const perBuilding = FORTICLAD_BUILDINGS.map((building) => {
      const progress = forticladProgress[building.id];
      const start = Math.max(0, Math.min(7, progress.level));
      const end = Math.max(start + 1, Math.min(8, progress.targetLevel));
      let totalFc = 0;
      let totalAfc = 0;
      for (let level = start + 1; level <= end; level += 1) {
        const cost = building.costsByTargetLevel[level - 1];
        const steps = level === start + 1 ? 5 - progress.completedStages : 5;
        totalFc += cost.fc * steps;
        totalAfc += cost.afc * steps;
      }
      return { ...building, progress, end, totalFc, totalAfc };
    });
    return {
      perBuilding,
      totalFc: perBuilding.reduce((sum, item) => sum + item.totalFc, 0),
      totalAfc: perBuilding.reduce((sum, item) => sum + item.totalAfc, 0),
    };
  }, [forticladProgress]);

  // --- Calculateur Héros & Tomes ---
  const [selectedHeroName, setSelectedHeroName] = useState<string>(
    state.heroes[0]?.nom || KNOWN_HEROES_META[0].nom
  );
  const [starFrom, setStarFrom] = useState<number>(0);
  const [starTo, setStarTo] = useState<number>(4);
  const [ownedShards, setOwnedShards] = useState<number>(0);

  const [tomeFrom, setTomeFrom] = useState<number>(0);
  const [tomeTo, setTomeTo] = useState<number>(5);
  const [skillLevelFrom, setSkillLevelFrom] = useState<number>(1);
  const [skillLevelTo, setSkillLevelTo] = useState<number>(5);
  const [skillRarity, setSkillRarity] = useState<'Rare' | 'Épique' | 'Légendaire'>('Légendaire');

  const shardCalculation = useMemo(() => {
    const from = Math.max(0, Math.min(4, starFrom));
    const to = Math.max(from + 1, Math.min(5, starTo));
    const tiers = HERO_SHARD_TIERS.filter((t) => t.star > from && t.star <= to);
    const unlockShards = from === 0 ? 10 : 0;
    const starShards = tiers.reduce((sum, t) => sum + t.shardsForTier, 0);
    const totalNeeded = unlockShards + starShards;
    const missing = Math.max(0, totalNeeded - Math.max(0, ownedShards));
    return { from, to, unlockShards, starShards, totalNeeded, missing, tiers };
  }, [starFrom, starTo, ownedShards]);

  const tomeCalculation = useMemo(() => {
    const from = Math.max(0, Math.min(9, tomeFrom));
    const to = Math.max(from + 1, Math.min(10, tomeTo));
    const steps = TROOP_TOME_COSTS.filter((s) => s.fromLevel >= from && s.toLevel <= to);
    const wisdom = steps.reduce((s, x) => s + x.sealOfWisdom, 0);
    const knowledge = steps.reduce((s, x) => s + x.sealOfKnowledge, 0);
    const allVerified = steps.every((x) => x.verifiedWiki);
    return { from, to, wisdom, knowledge, allVerified };
  }, [tomeFrom, tomeTo]);

  const skillBookCalculation = useMemo(() => {
    const from = Math.max(1, Math.min(4, skillLevelFrom));
    const to = Math.max(from + 1, Math.min(5, skillLevelTo));
    const steps = HERO_SKILL_BOOK_STEPS.filter((s) => s.fromLevel >= from && s.toLevel <= to);
    const perSkill = steps.reduce((s, x) => s + x.estimatedBooks, 0);
    return { from, to, perSkill, forThreeSkills: perSkill * 3 };
  }, [skillLevelFrom, skillLevelTo]);

  const knownHeroInfo = useMemo(
    () =>
      KNOWN_HEROES_META.find(
        (h) => h.nom.toLowerCase() === selectedHeroName.trim().toLowerCase()
      ) || null,
    [selectedHeroName]
  );

  return (
    <div className="space-y-6">
      {/* Sous-navigation du calculateur */}
      <div className="flex flex-wrap items-center gap-1.5 p-1 bg-[#121826] border border-slate-800/90 rounded-xl w-fit">
        <button
          type="button"
          onClick={() => setSection('buildings')}
          className={`min-h-[40px] px-4 py-2 rounded-lg text-xs font-semibold transition-colors inline-flex items-center gap-2 whitespace-nowrap ${
            section === 'buildings'
              ? 'bg-amber-500 text-slate-950'
              : 'text-slate-300 hover:text-slate-100'
          }`}
        >
          <Building2 className="w-4 h-4" />
          <span>Bâtiments (Niv. 1–30)</span>
        </button>
        <button
          type="button"
          onClick={() => setSection('forticlad')}
          className={`min-h-[40px] px-4 py-2 rounded-lg text-xs font-semibold transition-colors inline-flex items-center gap-2 whitespace-nowrap ${
            section === 'forticlad'
              ? 'bg-amber-500 text-slate-950'
              : 'text-slate-300 hover:text-slate-100'
          }`}
        >
          <Sparkles className="w-4 h-4" />
          <span>Forticlad (FC 1–8)</span>
        </button>
        <button
          type="button"
          onClick={() => setSection('heroes')}
          className={`min-h-[40px] px-4 py-2 rounded-lg text-xs font-semibold transition-colors inline-flex items-center gap-2 whitespace-nowrap ${
            section === 'heroes'
              ? 'bg-amber-500 text-slate-950'
              : 'text-slate-300 hover:text-slate-100'
          }`}
        >
          <Users className="w-4 h-4" />
          <span>Héros, Éclats & Tomes</span>
        </button>
      </div>

      {/* SECTION 1 : BÂTIMENTS */}
      {section === 'buildings' && (
        <div className="space-y-6">
          <section className="p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-5">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
              <div>
                <h2 className="text-lg font-semibold text-slate-50">
                  Calculateur d’objets & prérequis bâtiment
                </h2>
                <p className="text-xs text-slate-400 mt-1">
                  Calcule la nourriture, le bois, l’acier, l’essence, le temps et les bâtiments requis
                  pour atteindre le niveau suivant.
                </p>
              </div>
              <div className="text-xs text-slate-400">
                {buildingCalculation.allVerified ? (
                  <span className="text-emerald-400 inline-flex items-center gap-1.5 font-medium">
                    <CheckCircle2 className="w-4 h-4" />
                    <span>Barème vérifié (Wiki / lojxlab)</span>
                  </span>
                ) : (
                  <span className="text-amber-300 inline-flex items-center gap-1.5 font-medium">
                    <AlertTriangle className="w-4 h-4" />
                    <span>Inclut des niveaux estimés (à confirmer en jeu)</span>
                  </span>
                )}
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="md:col-span-2">
                <label className="block text-xs font-medium text-slate-400 mb-1.5">
                  Bâtiment à améliorer
                </label>
                <select
                  value={selectedBuildingId}
                  onChange={(e) => setSelectedBuildingId(e.target.value)}
                  className="w-full min-h-[44px] rounded-xl bg-slate-900 border border-slate-800 px-3.5 py-2 text-sm text-slate-100 focus:outline-none focus:border-amber-500"
                >
                  {BUILDINGS_CATALOG.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.nomFr}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1.5">
                  Niveau actuel
                </label>
                <input
                  type="number"
                  min={1}
                  max={29}
                  value={currentLevel}
                  onChange={(e) => {
                    const val = Math.max(1, Math.min(29, Number(e.target.value) || 1));
                    setCurrentLevel(val);
                    if (targetLevel <= val) setTargetLevel(Math.min(30, val + 1));
                  }}
                  className="w-full min-h-[44px] rounded-xl bg-slate-900 border border-slate-800 px-3.5 py-2 text-sm font-mono-tabular text-slate-100 focus:outline-none focus:border-amber-500"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1.5">
                  Niveau cible
                </label>
                <input
                  type="number"
                  min={currentLevel + 1}
                  max={30}
                  value={targetLevel}
                  onChange={(e) =>
                    setTargetLevel(Math.max(currentLevel + 1, Math.min(30, Number(e.target.value) || currentLevel + 1)))
                  }
                  className="w-full min-h-[44px] rounded-xl bg-slate-900 border border-slate-800 px-3.5 py-2 text-sm font-mono-tabular text-slate-100 focus:outline-none focus:border-amber-500"
                />
              </div>
            </div>

            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pt-2 border-t border-slate-800/80">
              <div className="text-xs text-slate-300">{selectedBuilding.descriptionFr}</div>
              <div className="flex items-center gap-2 shrink-0">
                <label className="text-xs text-slate-400 whitespace-nowrap">
                  Bonus vitesse construction (%) :
                </label>
                <input
                  type="number"
                  min={0}
                  max={500}
                  value={constructionBonusPct}
                  onChange={(e) => setConstructionBonusPct(Math.max(0, Number(e.target.value) || 0))}
                  className="w-20 min-h-[38px] rounded-lg bg-slate-900 border border-slate-800 px-2.5 py-1 text-right text-sm font-mono-tabular text-slate-100"
                />
              </div>
            </div>
          </section>

          {/* Résultat chiffré & comparaison stock */}
          <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
            <ResourceGapCard
              label="Nourriture (Food)"
              needed={buildingCalculation.food}
              owned={ownedFood}
            />
            <ResourceGapCard
              label="Bois (Wood)"
              needed={buildingCalculation.wood}
              owned={ownedWood}
            />
            <ResourceGapCard
              label="Acier (Steel)"
              needed={buildingCalculation.steel}
              owned={ownedSteel}
            />
            <ResourceGapCard
              label="Essence (Gasoline)"
              needed={buildingCalculation.gasoline}
              owned={ownedGas}
            />
            <div className="p-5 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-1.5">
              <div className="text-xs text-slate-400 flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-amber-400" />
                <span>Temps estimé</span>
              </div>
              <div className="text-xl font-semibold font-mono-tabular text-slate-50">
                {formatMinutesToDurationFr(buildingCalculation.effectiveMinutes)}
              </div>
              <div className="text-xs font-mono-tabular text-slate-400">
                Base : {formatMinutesToDurationFr(buildingCalculation.baseTimeMinutes)}
              </div>
            </div>
          </section>

          {/* Prérequis & paliers détaillés */}
          <section className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            <div className="lg:col-span-5 p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-4">
              <h3 className="text-base font-semibold text-slate-100">
                Bâtiments prérequis & Règle Comms
              </h3>
              {buildingCalculation.prerequisites.length === 0 ? (
                <p className="text-xs text-slate-400">Aucun prérequis recensé sur ce palier.</p>
              ) : (
                <ul className="space-y-2 text-xs text-slate-200 list-disc pl-4">
                  {Array.from(new Set(buildingCalculation.prerequisites)).map((req, idx) => (
                    <li key={idx}>{req}</li>
                  ))}
                </ul>
              )}

              {buildingCalculation.unlocks.length > 0 && (
                <div className="pt-3 border-t border-slate-800/80 space-y-1.5">
                  <div className="text-xs font-semibold text-amber-300">Déblocages majeurs</div>
                  {buildingCalculation.unlocks.map((u, idx) => (
                    <div key={idx} className="text-xs text-slate-300">
                      • {u}
                    </div>
                  ))}
                </div>
              )}

              <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 text-xs text-slate-300 leading-relaxed">
                Astuce événement : conservez vos gros bâtiments et accélérations pour{' '}
                <strong className="text-slate-100">Power Sprint</strong> ou les jours de préparation{' '}
                <strong className="text-slate-100">SvS</strong>, et gardez l’Entrepôt à jour pour
                éviter le pillage.
              </div>
            </div>

            <div className="lg:col-span-7 rounded-2xl bg-[#121826] border border-slate-800/90 overflow-hidden">
              <div className="px-6 py-4 border-b border-slate-800/80 flex items-center justify-between">
                <h3 className="text-base font-semibold text-slate-100">
                  Détail niveau par niveau ({buildingCalculation.start} → {buildingCalculation.end})
                </h3>
                <span className="text-xs text-slate-400 font-mono-tabular">
                  {buildingCalculation.steps.length} palier(s)
                </span>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-xs">
                  <thead>
                    <tr className="border-b border-slate-800/80 text-slate-400">
                      <th className="py-2.5 px-4 font-medium">Niv.</th>
                      <th className="py-2.5 px-3 font-medium text-right">Nourr.</th>
                      <th className="py-2.5 px-3 font-medium text-right">Bois</th>
                      <th className="py-2.5 px-3 font-medium text-right">Acier</th>
                      <th className="py-2.5 px-3 font-medium text-right">Essence</th>
                      <th className="py-2.5 px-4 font-medium text-right">Source</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {buildingCalculation.steps.map((step) => (
                      <tr key={step.level} className="hover:bg-slate-900/40">
                        <td className="py-2.5 px-4 font-mono-tabular font-semibold text-slate-100">
                          {step.level}
                        </td>
                        <td className="py-2.5 px-3 text-right font-mono-tabular text-slate-200">
                          {formatCompactNumber(step.food)}
                        </td>
                        <td className="py-2.5 px-3 text-right font-mono-tabular text-slate-200">
                          {formatCompactNumber(step.wood)}
                        </td>
                        <td className="py-2.5 px-3 text-right font-mono-tabular text-slate-200">
                          {formatCompactNumber(step.steel)}
                        </td>
                        <td className="py-2.5 px-3 text-right font-mono-tabular text-slate-200">
                          {formatCompactNumber(step.gasoline)}
                        </td>
                        <td className="py-2.5 px-4 text-right whitespace-nowrap">
                          {step.verifiedWiki ? (
                            <span className="text-emerald-400">Wiki</span>
                          ) : (
                            <span className="text-amber-300" title={step.noteIncertitude}>
                              Estimé
                            </span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        </div>
      )}

      {/* SECTION 2 : FORTICLAD (FC 1 → FC 8) */}
      {section === 'forticlad' && (
        <div className="space-y-6">
          <section className="p-5 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-5">
            <div>
              <h2 className="text-lg font-semibold text-slate-50">Calculateur Forticlad · FC 1 à FC 8</h2>
              <p className="text-xs text-slate-400 mt-1">
                Renseigne le niveau et les paliers terminés de chacun des sept bâtiments. Les coûts affichés sont calculés par palier, et non avec une moyenne globale.
              </p>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="p-4 rounded-xl bg-slate-900/70 border border-slate-800">
                <div className="text-xs text-slate-400">Forticlad Cores nécessaires</div>
                <div className="text-2xl font-semibold font-mono-tabular text-amber-400 mt-1">{formatCompactNumber(forticladCalculation.totalFc)}</div>
              </div>
              <div className="p-4 rounded-xl bg-slate-900/70 border border-slate-800">
                <div className="text-xs text-slate-400">Advanced Forticlad Cores nécessaires</div>
                <div className="text-2xl font-semibold font-mono-tabular text-slate-50 mt-1">{formatCompactNumber(forticladCalculation.totalAfc)}</div>
              </div>
            </div>
            <div className="space-y-3">
              {forticladCalculation.perBuilding.map((building) => (
                <div key={building.id} className="p-4 rounded-xl bg-slate-900/70 border border-slate-800 space-y-3">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <h3 className="text-sm font-semibold text-slate-100">{building.nomFr}</h3>
                    <span className="text-xs text-slate-400">FC {building.progress.level} → FC {building.end}</span>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    <label className="text-xs text-slate-400 space-y-1">
                      <span>Niveau actuel (0 = niveau 30)</span>
                      <select
                        value={building.progress.level}
                        onChange={(event) => {
                          const level = Math.max(0, Math.min(7, Number(event.target.value)));
                          updateForticladBuilding(building.id, {
                            level,
                            completedStages: 0,
                            targetLevel: Math.min(8, level + 1),
                          });
                        }}
                        className="w-full min-h-[40px] rounded-lg bg-slate-950 border border-slate-700 px-2 text-sm text-slate-100"
                      >
                        <option value={0}>Niveau 30 (avant FC1)</option>
                        {[1,2,3,4,5,6,7].map((level) => <option key={level} value={level}>FC {level}</option>)}
                      </select>
                    </label>
                    <label className="text-xs text-slate-400 space-y-1">
                      <span>Paliers déjà terminés du prochain FC</span>
                      <select
                        value={building.progress.completedStages}
                        onChange={(event) => updateForticladBuilding(building.id, { completedStages: Number(event.target.value) })}
                        className="w-full min-h-[40px] rounded-lg bg-slate-950 border border-slate-700 px-2 text-sm text-slate-100"
                      >
                        {[0,1,2,3,4].map((stage) => <option key={stage} value={stage}>{stage}/5 terminés</option>)}
                      </select>
                    </label>
                    <label className="text-xs text-slate-400 space-y-1">
                      <span>Niveau cible</span>
                      <select
                        value={building.progress.targetLevel}
                        onChange={(event) => updateForticladBuilding(building.id, { targetLevel: Number(event.target.value) })}
                        className="w-full min-h-[40px] rounded-lg bg-slate-950 border border-slate-700 px-2 text-sm text-slate-100"
                      >
                        {Array.from({length: 8 - building.progress.level}, (_, index) => building.progress.level + index + 1)
                          .map((level) => <option key={level} value={level}>FC {level}</option>)}
                      </select>
                    </label>
                  </div>
                  <div className="flex flex-wrap gap-x-5 gap-y-1 text-xs">
                    <span className="text-amber-300">FC : {formatCompactNumber(building.totalFc)}</span>
                    <span className="text-slate-200">AFC : {formatCompactNumber(building.totalAfc)}</span>
                  </div>
                </div>
              ))}
            </div>
            <div className="p-4 rounded-xl bg-slate-900/70 border border-slate-800 space-y-1.5">
              <div className="text-xs font-semibold text-slate-200">Comparaison avec l’inventaire du profil actif</div>
              <div className="text-xs text-slate-400">
                Stock détecté : FC {ownedForticladCores === null ? 'non détecté' : formatCompactNumber(ownedForticladCores)} · AFC {ownedAdvancedForticladCores === null ? 'non détecté' : formatCompactNumber(ownedAdvancedForticladCores)}
              </div>
              <div className="text-sm font-medium text-amber-300">
                Manquant : FC {ownedForticladCores === null ? '—' : formatCompactNumber(Math.max(0, forticladCalculation.totalFc - ownedForticladCores))} · AFC {ownedAdvancedForticladCores === null ? '—' : formatCompactNumber(Math.max(0, forticladCalculation.totalAfc - ownedAdvancedForticladCores))}
              </div>
            </div>
            <p className="text-xs text-slate-500">
              Le laboratoire Forticlad est un système de recherche séparé, payé en Hyperalliage : il n’est pas inclus dans ces coûts de noyaux. Barèmes FC1–8 issus des tableaux joueurs GuideLOJ et LOJ XLab.
            </p>
          </section>
        </div>
      )}

      {/* SECTION 3 : HÉROS, ÉCLATS, TOMES & LIVRES */}
      {section === 'heroes' && (
        <div className="space-y-6">
          {/* Éclats de héros */}
          <section className="p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-5">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
              <div>
                <h2 className="text-lg font-semibold text-slate-50">
                  Calculateur d’éclats de héros (0★ → 5★)
                </h2>
                <p className="text-xs text-slate-400 mt-1">
                  Total exact vérifié : <strong className="text-slate-200">1 075 éclats</strong> de
                  1★ à 5★ (+10 pour débloquer). Chaque étoile compte 6 segments.
                </p>
              </div>
              <span className="text-xs text-emerald-400 font-medium whitespace-nowrap">
                Barème vérifié (lojxlab)
              </span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1.5">
                  Héros ciblé
                </label>
                <select
                  value={selectedHeroName}
                  onChange={(e) => setSelectedHeroName(e.target.value)}
                  className="w-full min-h-[44px] rounded-xl bg-slate-900 border border-slate-800 px-3.5 py-2 text-sm text-slate-100"
                >
                  {state.heroes.map((h) => (
                    <option key={h.id} value={h.nom}>
                      {h.nom} (scanné · {h.niveauTexte})
                    </option>
                  ))}
                  {KNOWN_HEROES_META.filter(
                    (km) => !state.heroes.some((sh) => sh.nom.toLowerCase() === km.nom.toLowerCase())
                  ).map((km) => (
                    <option key={km.nom} value={km.nom}>
                      {km.nom} ({km.rarity} · {km.heroClass})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1.5">
                  Étoiles actuelles
                </label>
                <select
                  value={starFrom}
                  onChange={(e) => {
                    const v = Number(e.target.value);
                    setStarFrom(v);
                    if (starTo <= v) setStarTo(Math.min(5, v + 1));
                  }}
                  className="w-full min-h-[44px] rounded-xl bg-slate-900 border border-slate-800 px-3.5 py-2 text-sm text-slate-100"
                >
                  <option value={0}>Non débloqué (0★)</option>
                  <option value={1}>1 étoile (★)</option>
                  <option value={2}>2 étoiles (★★)</option>
                  <option value={3}>3 étoiles (★★★)</option>
                  <option value={4}>4 étoiles (★★★★)</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1.5">
                  Étoiles cibles
                </label>
                <select
                  value={starTo}
                  onChange={(e) => setStarTo(Number(e.target.value))}
                  className="w-full min-h-[44px] rounded-xl bg-slate-900 border border-slate-800 px-3.5 py-2 text-sm text-slate-100"
                >
                  {[1, 2, 3, 4, 5]
                    .filter((s) => s > starFrom)
                    .map((s) => (
                      <option key={s} value={s}>
                        {s} étoile(s)
                      </option>
                    ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1.5">
                  Éclats déjà en stock
                </label>
                <input
                  type="number"
                  min={0}
                  max={2000}
                  value={ownedShards}
                  onChange={(e) => setOwnedShards(Math.max(0, Number(e.target.value) || 0))}
                  className="w-full min-h-[44px] rounded-xl bg-slate-900 border border-slate-800 px-3.5 py-2 text-sm font-mono-tabular text-slate-100"
                />
              </div>
            </div>

            {knownHeroInfo && (
              <div className="p-4 rounded-xl bg-slate-900/70 border border-slate-800 text-xs text-slate-300 space-y-1">
                <div className="font-semibold text-slate-100">
                  {knownHeroInfo.nom} · {knownHeroInfo.rarity} · Classe {knownHeroInfo.heroClass}
                </div>
                <div>Trial Cage : {knownHeroInfo.roleTrialCage}</div>
                <div>Crystal Clash : {knownHeroInfo.roleCrystalClash}</div>
                <div className="text-amber-300">Priorité : {knownHeroInfo.priorityNote}</div>
              </div>
            )}

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="p-4 rounded-xl bg-slate-900/70 border border-slate-800">
                <div className="text-xs text-slate-400">Éclats requis au total</div>
                <div className="text-2xl font-semibold font-mono-tabular text-slate-50 mt-1">
                  {shardCalculation.totalNeeded}
                </div>
                {shardCalculation.unlockShards > 0 && (
                  <div className="text-xs text-slate-400 mt-0.5">
                    Dont {shardCalculation.unlockShards} pour le recrutement
                  </div>
                )}
              </div>

              <div className="p-4 rounded-xl bg-slate-900/70 border border-slate-800">
                <div className="text-xs text-slate-400">Éclats manquants</div>
                <div
                  className={`text-2xl font-semibold font-mono-tabular mt-1 ${
                    shardCalculation.missing === 0 ? 'text-emerald-400' : 'text-amber-400'
                  }`}
                >
                  {shardCalculation.missing}
                </div>
              </div>

              <div className="p-4 rounded-xl bg-slate-900/70 border border-slate-800">
                <div className="text-xs text-slate-400">Règle d’optimisation</div>
                <div className="text-xs text-slate-200 mt-1 leading-relaxed">
                  Visez d’abord <strong className="text-amber-300">4★ (475 éclats)</strong> sur votre
                  trio principal pour débloquer les compétences Niv. 5 avant de dépenser 600 éclats
                  sur une 5ᵉ étoile.
                </div>
              </div>
            </div>
          </section>

          {/* Tomes de troupes + Livres de compétences */}
          <section className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-4">
              <div className="flex items-center justify-between gap-2">
                <h3 className="text-base font-semibold text-slate-100">
                  Tomes de troupes (Seal of Wisdom / Knowledge)
                </h3>
                <span className="text-xs text-emerald-400 font-medium">
                  {tomeCalculation.allVerified ? 'Exact 0→6' : 'Mixte'}
                </span>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Niveau actuel</label>
                  <input
                    type="number"
                    min={0}
                    max={9}
                    value={tomeFrom}
                    onChange={(e) => {
                      const v = Math.max(0, Math.min(9, Number(e.target.value) || 0));
                      setTomeFrom(v);
                      if (tomeTo <= v) setTomeTo(Math.min(10, v + 1));
                    }}
                    className="w-full min-h-[42px] rounded-xl bg-slate-900 border border-slate-800 px-3 py-1.5 text-sm font-mono-tabular text-slate-100"
                  />
                </div>
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Niveau cible</label>
                  <input
                    type="number"
                    min={tomeFrom + 1}
                    max={10}
                    value={tomeTo}
                    onChange={(e) =>
                      setTomeTo(Math.max(tomeFrom + 1, Math.min(10, Number(e.target.value) || tomeFrom + 1)))
                    }
                    className="w-full min-h-[42px] rounded-xl bg-slate-900 border border-slate-800 px-3 py-1.5 text-sm font-mono-tabular text-slate-100"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3 pt-1">
                <div className="p-4 rounded-xl bg-slate-900/70 border border-slate-800">
                  <div className="text-xs text-slate-400">Seal of Wisdom</div>
                  <div className="text-xl font-semibold font-mono-tabular text-amber-400 mt-1">
                    {tomeCalculation.wisdom}
                  </div>
                </div>
                <div className="p-4 rounded-xl bg-slate-900/70 border border-slate-800">
                  <div className="text-xs text-slate-400">Seal of Knowledge</div>
                  <div className="text-xl font-semibold font-mono-tabular text-slate-100 mt-1">
                    {tomeCalculation.knowledge}
                  </div>
                </div>
              </div>
              <p className="text-xs text-slate-400">
                À partir du palier 12 → 13, le jeu exige également des{' '}
                <strong className="text-slate-200">Mystery Tomes</strong>.
              </p>
            </div>

            <div className="p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-4">
              <div className="flex items-center justify-between gap-2">
                <h3 className="text-base font-semibold text-slate-100">
                  Livres de compétences (Arrest & War Books)
                </h3>
                <span className="text-xs text-amber-300 font-medium">Estimation</span>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Rareté héros</label>
                  <select
                    value={skillRarity}
                    onChange={(e) => setSkillRarity(e.target.value as 'Rare' | 'Épique' | 'Légendaire')}
                    className="w-full min-h-[42px] rounded-xl bg-slate-900 border border-slate-800 px-2.5 py-1.5 text-xs text-slate-100"
                  >
                    <option value="Rare">Rare (Bleu)</option>
                    <option value="Épique">Épique (Violet)</option>
                    <option value="Légendaire">Légendaire (SSR)</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Compétence de</label>
                  <input
                    type="number"
                    min={1}
                    max={4}
                    value={skillLevelFrom}
                    onChange={(e) => {
                      const v = Math.max(1, Math.min(4, Number(e.target.value) || 1));
                      setSkillLevelFrom(v);
                      if (skillLevelTo <= v) setSkillLevelTo(Math.min(5, v + 1));
                    }}
                    className="w-full min-h-[42px] rounded-xl bg-slate-900 border border-slate-800 px-3 py-1.5 text-sm font-mono-tabular text-slate-100"
                  />
                </div>
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Vers niveau</label>
                  <input
                    type="number"
                    min={skillLevelFrom + 1}
                    max={5}
                    value={skillLevelTo}
                    onChange={(e) =>
                      setSkillLevelTo(
                        Math.max(skillLevelFrom + 1, Math.min(5, Number(e.target.value) || skillLevelFrom + 1))
                      )
                    }
                    className="w-full min-h-[42px] rounded-xl bg-slate-900 border border-slate-800 px-3 py-1.5 text-sm font-mono-tabular text-slate-100"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="p-4 rounded-xl bg-slate-900/70 border border-slate-800">
                  <div className="text-xs text-slate-400">Pour 1 compétence ({skillRarity})</div>
                  <div className="text-xl font-semibold font-mono-tabular text-amber-400 mt-1">
                    ~{skillBookCalculation.perSkill} livres
                  </div>
                </div>
                <div className="p-4 rounded-xl bg-slate-900/70 border border-slate-800">
                  <div className="text-xs text-slate-400">Pour 3 compétences de combat</div>
                  <div className="text-xl font-semibold font-mono-tabular text-slate-100 mt-1">
                    ~{skillBookCalculation.forThreeSkills} livres
                  </div>
                </div>
              </div>

              <p className="text-xs text-amber-200/90 leading-relaxed">
                Les livres <strong>Arrest Skill</strong> (gestion/PvE) et <strong>War Skill</strong>{' '}
                (combat) doivent correspondre à la rareté du héros ({skillRarity}). Le nombre exact
                par palier varie selon la génération du héros et est donc signalé comme estimation.
              </p>
            </div>
          </section>
        </div>
      )}
    </div>
  );
};

const ResourceGapCard: React.FC<{
  label: string;
  needed: number;
  owned: number | null;
}> = ({ label, needed, owned }) => {
  const missing = owned !== null ? Math.max(0, needed - owned) : null;
  return (
    <div className="p-5 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-1.5">
      <div className="text-xs text-slate-400">{label}</div>
      <div className="text-xl font-semibold font-mono-tabular text-slate-50">
        {formatCompactNumber(needed)}
      </div>
      {owned === null ? (
        <div className="text-xs text-slate-500">Stock non scanné</div>
      ) : missing === 0 ? (
        <div className="text-xs text-emerald-400 font-mono-tabular">
          Stock OK ({formatCompactNumber(owned)})
        </div>
      ) : (
        <div className="text-xs text-red-300 font-mono-tabular">
          Manque : {formatCompactNumber(missing)}
        </div>
      )}
    </div>
  );
};
