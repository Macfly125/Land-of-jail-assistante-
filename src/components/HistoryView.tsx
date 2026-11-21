import React, { useState } from 'react';
import { AlertTriangle, ChevronDown, ChevronUp, Trash2, Upload } from 'lucide-react';
import { SCREEN_CATEGORY_LABELS, ScreenshotAnalysis } from '../types/game';
import { formatFrenchDateTime } from '../utils/storage';

interface HistoryViewProps {
  analyses: ScreenshotAnalysis[];
  onDeleteAnalysis: (id: string) => void;
  onOpenShareModal: () => void;
}

export const HistoryView: React.FC<HistoryViewProps> = ({
  analyses,
  onDeleteAnalysis,
  onOpenShareModal,
}) => {
  const [expandedId, setExpandedId] = useState<string | null>(analyses[0]?.id ?? null);

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 border-b border-slate-800/80 pb-5">
        <div>
          <h1 className="text-2xl font-semibold text-slate-50">Historique des analyses</h1>
          <p className="text-sm text-slate-400 mt-1">
            Journal complet des captures partagées ou analysées par OCR avec votre autorisation.
          </p>
        </div>
        <button
          type="button"
          onClick={onOpenShareModal}
          className="min-h-[44px] px-4 py-2 rounded-xl text-sm font-semibold bg-amber-500 text-slate-950 hover:bg-amber-400 transition-colors inline-flex items-center gap-2 whitespace-nowrap shrink-0 self-start"
        >
          <Upload className="w-4 h-4" />
          <span>Nouvelle capture</span>
        </button>
      </div>

      {analyses.length === 0 ? (
        <div className="p-10 rounded-2xl bg-[#121826] border border-slate-800/80 text-center space-y-4">
          <p className="text-sm text-slate-300">
            Aucune capture n’a encore été analysée.
          </p>
          <button
            type="button"
            onClick={onOpenShareModal}
            className="min-h-[44px] px-4 py-2 rounded-xl text-sm font-semibold bg-amber-500 text-slate-950 hover:bg-amber-400 transition-colors inline-flex items-center gap-2"
          >
            <Upload className="w-4 h-4" />
            <span>Partager une première capture</span>
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          {analyses.map((item) => {
            const isExpanded = expandedId === item.id;
            return (
              <div
                key={item.id}
                className="rounded-2xl bg-[#121826] border border-slate-800/90 overflow-hidden"
              >
                <div className="p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                  <div className="flex items-start gap-4 min-w-0">
                    {item.thumbnailDataUrl ? (
                      <img
                        src={item.thumbnailDataUrl}
                        alt={item.fileName}
                        className="w-16 h-16 rounded-xl object-cover border border-slate-800 shrink-0 bg-slate-950"
                      />
                    ) : (
                      <div className="w-16 h-16 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-center text-xs text-slate-500 shrink-0">
                        Aperçu
                      </div>
                    )}
                    <div className="min-w-0 space-y-1">
                      <div className="text-xs text-slate-400 font-mono-tabular">
                        {SCREEN_CATEGORY_LABELS[item.typeCapture]} ·{' '}
                        {formatFrenchDateTime(item.analyzedAt)}
                      </div>
                      <h2 className="text-base font-semibold text-slate-100 truncate">
                        {item.fileName}
                      </h2>
                      <p className="text-sm text-slate-300 leading-relaxed">
                        {item.resumeCapture}
                      </p>
                      <div className="text-xs text-slate-400 pt-0.5">
                        {item.heros.length} héros · {item.ressources.length} ressource(s)
                        {item.puissance ? ` · Puissance ${item.puissance.puissanceTotaleTexte}` : ''}
                        {item.champsIncertains.length > 0 && (
                          <span className="text-amber-400 font-medium">
                            {' '}
                            · {item.champsIncertains.length} incertain(s)
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0 self-end sm:self-center">
                    <button
                      type="button"
                      onClick={() => setExpandedId(isExpanded ? null : item.id)}
                      className="min-h-[40px] px-3 py-1.5 rounded-xl text-xs font-medium bg-slate-800 text-slate-200 hover:bg-slate-700 transition-colors flex items-center gap-1.5 whitespace-nowrap"
                    >
                      <span>{isExpanded ? 'Masquer' : 'Détails'}</span>
                      {isExpanded ? (
                        <ChevronUp className="w-4 h-4" />
                      ) : (
                        <ChevronDown className="w-4 h-4" />
                      )}
                    </button>
                    <button
                      type="button"
                      onClick={() => onDeleteAnalysis(item.id)}
                      aria-label="Supprimer cette analyse"
                      className="min-h-[40px] min-w-[40px] flex items-center justify-center rounded-xl text-slate-400 hover:text-red-400 hover:bg-red-500/10 transition-colors"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                {isExpanded && (
                  <div className="px-5 pb-5 pt-4 border-t border-slate-800/80 bg-slate-900/40 space-y-4">
                    {item.noteUtilisateur && (
                      <div className="text-xs text-slate-300">
                        Note du joueur : {item.noteUtilisateur}
                      </div>
                    )}

                    {item.champsIncertains.length > 0 && (
                      <div className="p-3.5 rounded-xl bg-amber-500/10 border border-amber-500/30 space-y-1.5">
                        <div className="text-xs font-semibold text-amber-300 flex items-center gap-1.5">
                          <AlertTriangle className="w-3.5 h-3.5" />
                          <span>Champs signalés comme incertains</span>
                        </div>
                        <ul className="space-y-1 text-xs text-amber-100/90">
                          {item.champsIncertains.map((u, idx) => (
                            <li key={idx}>
                              {u.champ} ({u.valeurObservee}) — {u.raison}
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                      <div className="space-y-1.5">
                        <div className="font-semibold text-slate-200">Héros extraits</div>
                        {item.heros.length === 0 ? (
                          <div className="text-slate-500">Aucun héros sur cette capture.</div>
                        ) : (
                          item.heros.map((h) => (
                            <div key={h.id} className="text-slate-300">
                              • {h.nom} ({h.niveauTexte}) — {h.competences.length} compétence(s)
                              {h.incertain ? ' [Incertain]' : ''}
                            </div>
                          ))
                        )}
                      </div>

                      <div className="space-y-1.5">
                        <div className="font-semibold text-slate-200">Ressources extraites</div>
                        {item.ressources.length === 0 ? (
                          <div className="text-slate-500">Aucune ressource sur cette capture.</div>
                        ) : (
                          item.ressources.map((r) => (
                            <div key={r.id} className="text-slate-300">
                              • {r.nom} : {r.quantiteTexte}
                              {r.incertain ? ' [Incertain]' : ''}
                            </div>
                          ))
                        )}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
