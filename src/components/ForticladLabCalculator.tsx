import React, { useEffect, useMemo, useState } from 'react';
import { AlertTriangle, CheckCircle2, FlaskConical } from 'lucide-react';
import { FORTICLAD_LAB_COSTS } from '../data/landsOfJailData';
import { AccountState, ForticladLabProgress, ForticladLabTroopType } from '../types/game';
import { formatCompactNumber } from '../utils/storage';

const TROOPS: Array<{ id: ForticladLabTroopType; label: string }> = [
  { id: 'shieldbearer', label: 'Porte-boucliers' },
  { id: 'bomber', label: 'Bombardiers' },
  { id: 'shooter', label: 'Tireurs' },
];

const EMPTY_PROGRESS: ForticladLabProgress = {
  tier1: 0,
  tier2: [0, 0, 0, 0],
  tier3: 0,
  tier4: false,
  tier5: [0, 0],
};

function normalizeProgress(value?: ForticladLabProgress): ForticladLabProgress {
  return {
    tier1: Math.max(0, Math.min(5, value?.tier1 ?? 0)),
    tier2: Array.from({ length: 4 }, (_, i) => Math.max(0, Math.min(10, value?.tier2?.[i] ?? 0))),
    tier3: Math.max(0, Math.min(12, value?.tier3 ?? 0)),
    tier4: value?.tier4 === true,
    tier5: Array.from({ length: 2 }, (_, i) => Math.max(0, Math.min(10, value?.tier5?.[i] ?? 0))),
  };
}

function sumFrom(costs: readonly number[], level: number): number {
  return costs.slice(Math.max(0, Math.min(costs.length, level))).reduce((sum, value) => sum + value, 0);
}

function parseResourceAmount(state: AccountState): number | null {
  const resource = state.resources.find((item) => {
    const name = item.nom.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    return name.includes('hyperalloy') || name.includes('hyperalloy') || name.includes('hyperalliage');
  });
  if (!resource) return null;
  if (typeof resource.quantiteNumerique === 'number' && Number.isFinite(resource.quantiteNumerique)) {
    return resource.quantiteNumerique;
  }
  const cleaned = resource.quantiteTexte.replace(/\s+/g, '').replace(',', '.');
  const match = cleaned.match(/^(\d+(?:\.\d+)?)([kmb])?$/i);
  if (!match) return null;
  const multiplier = match[2]?.toLowerCase() === 'm' ? 1_000_000
    : match[2]?.toLowerCase() === 'k' ? 1_000
    : match[2]?.toLowerCase() === 'b' ? 1_000_000_000 : 1;
  return Math.round(Number(match[1]) * multiplier);
}

function remainingCost(progress: ForticladLabProgress): number {
  return sumFrom(FORTICLAD_LAB_COSTS.tier1, progress.tier1)
    + progress.tier2.reduce((sum, level) => sum + sumFrom(FORTICLAD_LAB_COSTS.tier2, level), 0)
    + sumFrom(FORTICLAD_LAB_COSTS.tier3, progress.tier3)
    + (progress.tier4 ? 0 : FORTICLAD_LAB_COSTS.tier4)
    + progress.tier5.reduce((sum, level) => sum + sumFrom(FORTICLAD_LAB_COSTS.tier5, level), 0);
}

const NumberField: React.FC<{
  label: string;
  value: number;
  max: number;
  onChange: (value: number) => void;
}> = ({ label, value, max, onChange }) => (
  <label className="block space-y-1">
    <span className="block text-xs text-slate-400">{label} · {value}/{max}</span>
    <input
      type="number"
      min={0}
      max={max}
      value={value}
      onChange={(event) => onChange(Math.max(0, Math.min(max, Number(event.target.value) || 0)))}
      className="w-full min-h-[42px] rounded-xl bg-slate-900 border border-slate-800 px-3 py-2 text-sm text-slate-100"
    />
  </label>
);

export const ForticladLabCalculator: React.FC<{
  state: AccountState;
  onUpdate: (progress: AccountState['forticladLabProgress']) => void;
}> = ({ state, onUpdate }) => {
  const [selectedTroop, setSelectedTroop] = useState<ForticladLabTroopType>('shieldbearer');
  const [progressByTroop, setProgressByTroop] = useState<Partial<Record<ForticladLabTroopType, ForticladLabProgress>>>(
    () => state.forticladLabProgress ?? {}
  );

  useEffect(() => {
    setProgressByTroop(state.forticladLabProgress ?? {});
  }, [state.forticladLabProgress]);

  const progress = normalizeProgress(progressByTroop[selectedTroop]);
  const remaining = remainingCost(progress);
  const totalRemaining = TROOPS.reduce((sum, troop) => sum + remainingCost(normalizeProgress(progressByTroop[troop.id])), 0);
  const ownedHyperalloy = parseResourceAmount(state);
  const selectedLabel = TROOPS.find((troop) => troop.id === selectedTroop)?.label ?? 'Troupes';

  const update = (patch: Partial<ForticladLabProgress>) => {
    const nextTroop = normalizeProgress({ ...progress, ...patch });
    const next = { ...progressByTroop, [selectedTroop]: nextTroop };
    setProgressByTroop(next);
    onUpdate(next);
  };

  const tiers = useMemo(() => [
    { title: 'Palier 1 · Capacité d’expédition', description: '5 niveaux', cost: sumFrom(FORTICLAD_LAB_COSTS.tier1, progress.tier1) },
    { title: 'Palier 2 · Recherches avancées', description: '4 technologies indépendantes · 10 niveaux chacune', cost: progress.tier2.reduce((sum, level) => sum + sumFrom(FORTICLAD_LAB_COSTS.tier2, level), 0) },
    { title: 'Palier 3 · Capacité de ralliement', description: '12 niveaux', cost: sumFrom(FORTICLAD_LAB_COSTS.tier3, progress.tier3) },
    { title: 'Palier 4 · Déblocage Bastion', description: 'Déblocage unique', cost: progress.tier4 ? 0 : FORTICLAD_LAB_COSTS.tier4 },
    { title: 'Palier 5 · Soins et entraînement', description: '2 technologies indépendantes · 10 niveaux chacune', cost: progress.tier5.reduce((sum, level) => sum + sumFrom(FORTICLAD_LAB_COSTS.tier5, level), 0) },
  ], [progress]);

  return (
    <div className="space-y-5">
      <section className="p-5 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-amber-500/10 flex items-center justify-center">
            <FlaskConical className="w-5 h-5 text-amber-400" />
          </div>
          <div>
            <h2 className="text-lg font-semibold text-slate-50">Laboratoire Forticlad · Hyperalliage</h2>
            <p className="text-xs text-slate-400">Recherche séparée des noyaux Forticlad des sept bâtiments</p>
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          {TROOPS.map((troop) => (
            <button
              key={troop.id}
              type="button"
              onClick={() => setSelectedTroop(troop.id)}
              className={`min-h-[40px] px-4 py-2 rounded-lg text-xs font-semibold ${selectedTroop === troop.id ? 'bg-amber-500 text-slate-950' : 'bg-slate-900 text-slate-300 border border-slate-700'}`}
            >
              {troop.label}
            </button>
          ))}
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="p-4 rounded-xl bg-slate-900/70 border border-slate-800">
            <div className="text-xs text-slate-400">Hyperalliage restant · {selectedLabel}</div>
            <div className="text-xl font-semibold text-amber-400 mt-1">{formatCompactNumber(remaining)}</div>
            <div className="text-xs text-slate-500 mt-1">Sur un total de 48 150 par type de troupe</div>
          </div>
          <div className="p-4 rounded-xl bg-slate-900/70 border border-slate-800">
            <div className="text-xs text-slate-400">Reste pour les 3 types</div>
            <div className="text-xl font-semibold text-slate-100 mt-1">{formatCompactNumber(totalRemaining)}</div>
            <div className="text-xs text-slate-500 mt-1">Progression cumulée du profil</div>
          </div>
          <div className="p-4 rounded-xl bg-slate-900/70 border border-slate-800">
            <div className="text-xs text-slate-400">Stock Hyperalliage détecté</div>
            {ownedHyperalloy === null ? (
              <div className="text-sm text-slate-400 mt-2">Non scanné</div>
            ) : (
              <>
                <div className="text-xl font-semibold text-slate-100 mt-1">{formatCompactNumber(ownedHyperalloy)}</div>
                <div className={`text-xs mt-1 ${ownedHyperalloy >= remaining ? 'text-emerald-400' : 'text-red-300'}`}>
                  {ownedHyperalloy >= remaining ? 'Stock suffisant pour ce type' : `Manque ${formatCompactNumber(remaining - ownedHyperalloy)}`}
                </div>
              </>
            )}
          </div>
        </div>
      </section>

      <section className="p-5 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-4">
        <div>
          <h3 className="text-base font-semibold text-slate-100">Progression de {selectedLabel}</h3>
          <p className="text-xs text-slate-400 mt-1">Saisis les niveaux réellement terminés. Chaque type de troupe conserve son propre avancement dans le profil sélectionné.</p>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <NumberField label="Palier 1" value={progress.tier1} max={5} onChange={(value) => update({ tier1: value })} />
          {progress.tier2.map((value, index) => (
            <NumberField key={index} label={`Palier 2 · Technologie ${index + 1}`} value={value} max={10} onChange={(next) => update({ tier2: progress.tier2.map((v, i) => i === index ? next : v) })} />
          ))}
          <NumberField label="Palier 3" value={progress.tier3} max={12} onChange={(value) => update({ tier3: value })} />
          <div className="p-3 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-between gap-3">
            <div>
              <div className="text-xs text-slate-300">Palier 4 · Bastion</div>
              <div className="text-xs text-slate-500">Déblocage · 4 500 Hyperalliage</div>
            </div>
            <input type="checkbox" checked={progress.tier4} onChange={(event) => update({ tier4: event.target.checked })} className="w-5 h-5 accent-amber-500" />
          </div>
          {progress.tier5.map((value, index) => (
            <NumberField key={index} label={`Palier 5 · Recherche ${index + 1}`} value={value} max={10} onChange={(next) => update({ tier5: progress.tier5.map((v, i) => i === index ? next : v) })} />
          ))}
        </div>
      </section>

      <section className="p-5 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-3">
        <h3 className="text-base font-semibold text-slate-100">Coût restant par palier</h3>
        {tiers.map((tier) => (
          <div key={tier.title} className="flex flex-col sm:flex-row sm:items-center justify-between gap-1 py-2 border-b border-slate-800 last:border-0">
            <div>
              <div className="text-sm text-slate-200">{tier.title}</div>
              <div className="text-xs text-slate-500">{tier.description}</div>
            </div>
            <div className="text-sm font-semibold text-amber-300">{formatCompactNumber(tier.cost)} Hyperalliage</div>
          </div>
        ))}
        <div className="text-xs text-slate-400 flex items-start gap-2 pt-2">
          <AlertTriangle className="w-4 h-4 shrink-0 text-amber-400" />
          <span>Le coût complet estimé par type est de 48 150 Hyperalliage ; le déblocage Bastion représente 25 730 au total avec les paliers 1 à 4. Vérifie les montants affichés dans ton laboratoire avant de dépenser.</span>
        </div>
        {remaining === 0 && (
          <div className="text-sm text-emerald-400 flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4" /> Toutes les recherches de ce type sont terminées.
          </div>
        )}
      </section>
    </div>
  );
};
