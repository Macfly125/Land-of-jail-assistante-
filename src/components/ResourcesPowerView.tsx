import React, { useState } from 'react';
import { AlertTriangle, Calculator, Check, Clock, Edit3, Package, Upload } from 'lucide-react';
import { AccountState, ResourceRecord } from '../types/game';
import { formatCompactNumber, formatFrenchDateTime } from '../utils/storage';
import { UpgradeCalculatorView } from './UpgradeCalculatorView';
import { OfflineRoutineView } from './OfflineRoutineView';

interface ResourcesPowerViewProps {
  state: AccountState;
  onVerifyResource: (resourceId: string, newQuantityText: string) => void;
  onOpenShareModal: () => void;
}

export const ResourcesPowerView: React.FC<ResourcesPowerViewProps> = ({
  state,
  onVerifyResource,
  onOpenShareModal,
}) => {
  const [subTab, setSubTab] = useState<'inventory' | 'calculator' | 'routine'>('inventory');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [draftQty, setDraftQty] = useState('');

  const numericHistory = state.powerHistory.filter(
    (p): p is typeof p & { puissanceNumerique: number } =>
      p.puissanceNumerique !== null && !Number.isNaN(p.puissanceNumerique)
  );

  return (
    <div className="space-y-8">
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 border-b border-slate-800/80 pb-5">
        <div>
          <h1 className="text-2xl font-semibold text-slate-50">
            Ressources, puissance & calculateurs
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Suivi de votre compte et calcul des objets requis pour passer vos bâtiments et héros au
            niveau suivant.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-3 self-start">
          <div className="flex items-center gap-1 p-1 bg-[#121826] border border-slate-800 rounded-xl">
            <button
              type="button"
              onClick={() => setSubTab('inventory')}
              className={`min-h-[40px] px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-colors inline-flex items-center gap-1.5 whitespace-nowrap ${
                subTab === 'inventory'
                  ? 'bg-amber-500 text-slate-950'
                  : 'text-slate-300 hover:text-slate-100'
              }`}
            >
              <Package className="w-3.5 h-3.5" />
              <span>Inventaire</span>
            </button>
            <button
              type="button"
              onClick={() => setSubTab('calculator')}
              className={`min-h-[40px] px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-colors inline-flex items-center gap-1.5 whitespace-nowrap ${
                subTab === 'calculator'
                  ? 'bg-amber-500 text-slate-950'
                  : 'text-slate-300 hover:text-slate-100'
              }`}
            >
              <Calculator className="w-3.5 h-3.5" />
              <span>Calculateur Bâtiments & Héros</span>
            </button>
            <button
              type="button"
              onClick={() => setSubTab('routine')}
              className={`min-h-[40px] px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-colors inline-flex items-center gap-1.5 whitespace-nowrap ${
                subTab === 'routine'
                  ? 'bg-amber-500 text-slate-950'
                  : 'text-slate-300 hover:text-slate-100'
              }`}
            >
              <Clock className="w-3.5 h-3.5" />
              <span>Routine Hors-Jeu</span>
            </button>
          </div>
          <button
            type="button"
            onClick={onOpenShareModal}
            className="min-h-[44px] px-4 py-2 rounded-xl text-sm font-semibold bg-amber-500 text-slate-950 hover:bg-amber-400 transition-colors inline-flex items-center gap-2 whitespace-nowrap shrink-0"
          >
            <Upload className="w-4 h-4" />
            <span>Mettre à jour par capture</span>
          </button>
        </div>
      </div>

      {subTab === 'calculator' ? (
        <UpgradeCalculatorView state={state} />
      ) : subTab === 'routine' ? (
        <OfflineRoutineView language={state.language} />
      ) : (
        <>
      {/* Bloc Puissance */}
      <section className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-5 p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-4">
          <div className="text-xs text-slate-400">Puissance du compte</div>
          <div className="text-3xl font-semibold font-mono-tabular text-slate-50">
            {state.currentPower?.puissanceTotaleTexte || 'Non mesurée'}
          </div>

          <div className="grid grid-cols-2 gap-4 pt-2 border-t border-slate-800/80 text-xs">
            <div>
              <div className="text-slate-400">Niveau Prison / QG</div>
              <div className="mt-1 text-sm font-mono-tabular text-slate-100">
                {state.currentPower?.niveauPrisonOuQG || '—'}
              </div>
            </div>
            <div>
              <div className="text-slate-400">Niveau VIP</div>
              <div className="mt-1 text-sm font-mono-tabular text-slate-100">
                {state.currentPower?.niveauVip || '—'}
              </div>
            </div>
          </div>

          {state.currentPower?.incertain && (
            <div className="p-3 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-start gap-2.5 text-xs text-amber-200">
              <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
              <span>
                Puissance incertaine :{' '}
                {state.currentPower.raisonIncertitude || 'lecture partielle.'}
              </span>
            </div>
          )}
        </div>

        <div className="lg:col-span-7 p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-slate-100">Évolution de la puissance</h2>
            <span className="text-xs text-slate-400 font-mono-tabular">
              {state.powerHistory.length} relevé(s)
            </span>
          </div>

          {numericHistory.length < 2 ? (
            <div className="py-8 text-center text-xs text-slate-400">
              Partagez au moins deux captures affichant votre puissance numérique pour tracer
              l’évolution.
            </div>
          ) : (
            <PowerSvgChart points={numericHistory} />
          )}
        </div>
      </section>

      {/* Tableau des ressources */}
      <section className="rounded-2xl bg-[#121826] border border-slate-800/90 overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-800/80 flex items-center justify-between">
          <h2 className="text-base font-semibold text-slate-100">Inventaire des ressources</h2>
          <span className="text-xs text-slate-400 font-mono-tabular">
            {state.resources.length} ressource(s)
          </span>
        </div>

        {state.resources.length === 0 ? (
          <div className="p-8 text-center text-sm text-slate-400">
            Aucune ressource extraite pour l’instant. Partagez une capture de votre écran principal
            ou de votre entrepôt.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-800/80 text-xs text-slate-400">
                  <th className="py-3 px-6 font-medium">Ressource</th>
                  <th className="py-3 px-4 font-medium text-right">Quantité</th>
                  <th className="py-3 px-4 font-medium">Fiabilité</th>
                  <th className="py-3 px-4 font-medium">Dernière capture</th>
                  <th className="py-3 px-6 font-medium text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-sm">
                {state.resources.map((res: ResourceRecord) => {
                  const isEditing = editingId === res.id;
                  return (
                    <tr key={res.id} className="hover:bg-slate-900/40 transition-colors">
                      <td className="py-3.5 px-6 font-medium text-slate-100">
                        <div>{res.nom}</div>
                        {res.incertain && res.raisonIncertitude && (
                          <div className="text-xs text-amber-300 mt-0.5">
                            {res.raisonIncertitude}
                          </div>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-right font-mono-tabular text-slate-100">
                        {isEditing ? (
                          <input
                            type="text"
                            value={draftQty}
                            onChange={(e) => setDraftQty(e.target.value)}
                            className="w-32 rounded-lg bg-slate-950 border border-amber-500 px-2.5 py-1 text-right text-sm font-mono text-slate-100"
                          />
                        ) : (
                          <span>
                            {res.quantiteTexte}
                            {res.unite ? ` ${res.unite}` : ''}
                          </span>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-xs">
                        {res.incertain ? (
                          <span className="text-amber-400 font-medium inline-flex items-center gap-1">
                            <AlertTriangle className="w-3.5 h-3.5" />
                            <span>Incertain</span>
                          </span>
                        ) : (
                          <span className="text-emerald-400 font-medium">
                            {res.verifieManuellement ? 'Confirmé' : 'Lisible'}
                          </span>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-xs text-slate-400 font-mono-tabular whitespace-nowrap">
                        {formatFrenchDateTime(res.derniereMaj)}
                      </td>
                      <td className="py-3.5 px-6 text-right whitespace-nowrap">
                        {isEditing ? (
                          <button
                            type="button"
                            onClick={() => {
                              if (draftQty.trim()) {
                                onVerifyResource(res.id, draftQty.trim());
                              }
                              setEditingId(null);
                            }}
                            className="inline-flex items-center gap-1 text-xs font-semibold text-emerald-400 hover:text-emerald-300 py-1 px-2"
                          >
                            <Check className="w-3.5 h-3.5" />
                            <span>Valider</span>
                          </button>
                        ) : (
                          <button
                            type="button"
                            onClick={() => {
                              setEditingId(res.id);
                              setDraftQty(res.quantiteTexte);
                            }}
                            className="inline-flex items-center gap-1 text-xs text-slate-400 hover:text-amber-300 py-1 px-2"
                          >
                            <Edit3 className="w-3.5 h-3.5" />
                            <span>Corriger</span>
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
        </>
      )}
    </div>
  );
};

const PowerSvgChart: React.FC<{
  points: Array<{ date: string; puissanceNumerique: number; puissanceTexte: string }>;
}> = ({ points }) => {
  const width = 560;
  const height = 160;
  const padX = 16;
  const padY = 20;

  const values = points.map((p) => p.puissanceNumerique);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const span = max - min || 1;

  const coords = points.map((p, idx) => {
    const x = padX + (idx / Math.max(1, points.length - 1)) * (width - padX * 2);
    const y = height - padY - ((p.puissanceNumerique - min) / span) * (height - padY * 2);
    return { x, y, ...p };
  });

  const polyline = coords.map((c) => `${c.x},${c.y}`).join(' ');

  return (
    <div className="space-y-2">
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="w-full h-40 overflow-visible"
        role="img"
        aria-label="Graphique d'évolution de puissance"
      >
        <line
          x1={padX}
          y1={height - padY}
          x2={width - padX}
          y2={height - padY}
          stroke="rgba(148,163,184,0.18)"
          strokeWidth="1"
        />
        <polyline
          fill="none"
          stroke="#F59E0B"
          strokeWidth="2.5"
          strokeLinejoin="round"
          strokeLinecap="round"
          points={polyline}
        />
        {coords.map((c, idx) => (
          <circle
            key={`${c.date}-${idx}`}
            cx={c.x}
            cy={c.y}
            r="4"
            fill="#0B0F17"
            stroke="#F59E0B"
            strokeWidth="2"
          />
        ))}
      </svg>
      <div className="flex items-center justify-between text-xs font-mono-tabular text-slate-400">
        <span>Min : {formatCompactNumber(min)}</span>
        <span>Max : {formatCompactNumber(max)}</span>
      </div>
    </div>
  );
};
