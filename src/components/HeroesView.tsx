import React, { useState } from 'react';
import { AlertTriangle, Check, Edit3, Search, Upload } from 'lucide-react';
import { HeroRecord } from '../types/game';
import { formatFrenchDateTime } from '../utils/storage';

interface HeroesViewProps {
  heroes: HeroRecord[];
  onVerifyHero: (heroId: string, updated: Partial<HeroRecord>) => void;
  onOpenShareModal: () => void;
}

export const HeroesView: React.FC<HeroesViewProps> = ({
  heroes,
  onVerifyHero,
  onOpenShareModal,
}) => {
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState<'tous' | 'incertains'>('tous');
  const [editingHeroId, setEditingHeroId] = useState<string | null>(null);
  const [draftLevel, setDraftLevel] = useState('');
  const [draftRole, setDraftRole] = useState('');

  const filtered = heroes.filter((hero) => {
    const matchesQuery =
      hero.nom.toLowerCase().includes(query.toLowerCase()) ||
      (hero.roleHero || '').toLowerCase().includes(query.toLowerCase()) ||
      hero.competences.some((s) => s.nom.toLowerCase().includes(query.toLowerCase()));
    const hasUncertain = hero.incertain || hero.competences.some((s) => s.incertain);
    if (filter === 'incertains' && !hasUncertain) return false;
    return matchesQuery;
  });

  const startEdit = (hero: HeroRecord) => {
    setEditingHeroId(hero.id);
    setDraftLevel(hero.niveau !== null ? String(hero.niveau) : '');
    setDraftRole(hero.roleHero || '');
  };

  const saveEdit = (hero: HeroRecord) => {
    const parsedLvl = draftLevel.trim() ? Number(draftLevel.trim()) : null;
    onVerifyHero(hero.id, {
      niveau: parsedLvl !== null && !Number.isNaN(parsedLvl) ? parsedLvl : hero.niveau,
      niveauTexte:
        parsedLvl !== null && !Number.isNaN(parsedLvl)
          ? `Niv. ${parsedLvl}`
          : hero.niveauTexte,
      roleHero: draftRole.trim() || hero.roleHero,
      incertain: false,
      raisonIncertitude: undefined,
      verifieManuellement: true,
    });
    setEditingHeroId(null);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 border-b border-slate-800/80 pb-5">
        <div>
          <h1 className="text-2xl font-semibold text-slate-50">Héros, niveaux et compétences</h1>
          <p className="text-sm text-slate-400 mt-1">
            Données extraites de vos captures partagées ou analysées par OCR. Les champs flous ou
            incomplets sont signalés comme incertains.
          </p>
        </div>

        <div className="flex items-center gap-1 p-1 bg-slate-900 border border-slate-800 rounded-xl shrink-0 self-start">
          <button
            type="button"
            onClick={() => setFilter('tous')}
            className={`min-h-[38px] px-3.5 py-1.5 rounded-lg text-xs font-medium transition-colors whitespace-nowrap ${
              filter === 'tous'
                ? 'bg-slate-800 text-slate-100'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Tous ({heroes.length})
          </button>
          <button
            type="button"
            onClick={() => setFilter('incertains')}
            className={`min-h-[38px] px-3.5 py-1.5 rounded-lg text-xs font-medium transition-colors whitespace-nowrap ${
              filter === 'incertains'
                ? 'bg-amber-500/20 text-amber-300'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            À vérifier (
            {
              heroes.filter((h) => h.incertain || h.competences.some((s) => s.incertain))
                .length
            }
            )
          </button>
        </div>
      </div>

      {/* Barre de recherche */}
      <div className="relative">
        <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Rechercher un héros, un rôle ou une compétence…"
          className="w-full rounded-xl bg-[#121826] border border-slate-800 pl-10 pr-4 py-2.5 text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-amber-500"
        />
      </div>

      {filtered.length === 0 ? (
        <div className="p-10 rounded-2xl bg-[#121826] border border-slate-800/80 text-center space-y-4">
          <p className="text-sm text-slate-300">
            {heroes.length === 0
              ? 'Aucun héros détecté pour le moment.'
              : 'Aucun héros ne correspond à votre recherche.'}
          </p>
          {heroes.length === 0 && (
            <button
              type="button"
              onClick={onOpenShareModal}
              className="min-h-[44px] px-4 py-2 rounded-xl text-sm font-semibold bg-amber-500 text-slate-950 hover:bg-amber-400 transition-colors inline-flex items-center gap-2"
            >
              <Upload className="w-4 h-4" />
              <span>Partager une capture de fiche héros</span>
            </button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          {filtered.map((hero) => {
            const isEditing = editingHeroId === hero.id;
            const heroHasIssue = hero.incertain || hero.competences.some((s) => s.incertain);

            return (
              <div
                key={hero.id}
                className={`p-5 rounded-2xl bg-[#121826] border space-y-4 ${
                  heroHasIssue ? 'border-amber-500/35' : 'border-slate-800/90'
                }`}
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0">
                    <h2 className="text-lg font-semibold text-slate-50 truncate">{hero.nom}</h2>
                    <div className="text-xs text-slate-400 mt-0.5">
                      {hero.rarete || 'Rareté non précisée'}
                      {hero.etoiles ? ` · ${hero.etoiles}` : ''}
                      {hero.roleHero ? ` · ${hero.roleHero}` : ''}
                      {hero.puissanceHero ? ` · Puissance ${hero.puissanceHero}` : ''}
                    </div>
                  </div>
                  <div className="text-right shrink-0">
                    <div className="text-base font-mono-tabular font-semibold text-amber-400">
                      {hero.niveauTexte}
                    </div>
                    <div className="text-[11px] text-slate-500 mt-0.5">
                      {formatFrenchDateTime(hero.derniereMaj)}
                    </div>
                  </div>
                </div>

                {hero.incertain && (
                  <div className="p-3 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-start gap-2.5 text-xs text-amber-200">
                    <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
                    <span>
                      Information incertaine :{' '}
                      {hero.raisonIncertitude || 'Lisibilité partielle sur la capture.'}
                    </span>
                  </div>
                )}

                {isEditing && (
                  <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-700 space-y-3">
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      <div>
                        <label className="block text-xs text-slate-400 mb-1">Niveau exact</label>
                        <input
                          type="number"
                          value={draftLevel}
                          onChange={(e) => setDraftLevel(e.target.value)}
                          placeholder="ex: 80"
                          className="w-full rounded-lg bg-slate-950 border border-slate-800 px-3 py-2 text-sm font-mono text-slate-100"
                        />
                      </div>
                      <div>
                        <label className="block text-xs text-slate-400 mb-1">Rôle du héros</label>
                        <input
                          type="text"
                          value={draftRole}
                          onChange={(e) => setDraftRole(e.target.value)}
                          placeholder="Tank, DPS, Soutien…"
                          className="w-full rounded-lg bg-slate-950 border border-slate-800 px-3 py-2 text-sm text-slate-100"
                        />
                      </div>
                    </div>
                    <div className="flex items-center justify-end gap-2">
                      <button
                        type="button"
                        onClick={() => setEditingHeroId(null)}
                        className="min-h-[38px] px-3 py-1.5 rounded-lg text-xs text-slate-400 hover:text-slate-100"
                      >
                        Annuler
                      </button>
                      <button
                        type="button"
                        onClick={() => saveEdit(hero)}
                        className="min-h-[38px] px-3.5 py-1.5 rounded-lg text-xs font-semibold bg-emerald-500 text-slate-950 hover:bg-emerald-400 flex items-center gap-1.5"
                      >
                        <Check className="w-3.5 h-3.5" />
                        <span>Confirmer</span>
                      </button>
                    </div>
                  </div>
                )}

                {/* Compétences */}
                <div className="space-y-2 border-t border-slate-800/80 pt-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-medium text-slate-300">Compétences</span>
                    {!isEditing && (
                      <button
                        type="button"
                        onClick={() => startEdit(hero)}
                        className="text-xs text-slate-400 hover:text-amber-300 flex items-center gap-1 py-1"
                      >
                        <Edit3 className="w-3.5 h-3.5" />
                        <span>{heroHasIssue ? 'Vérifier / Corriger' : 'Modifier'}</span>
                      </button>
                    )}
                  </div>

                  {hero.competences.length === 0 ? (
                    <p className="text-xs text-slate-500">
                      Aucune compétence visible sur les captures actuelles de ce héros.
                    </p>
                  ) : (
                    <div className="divide-y divide-slate-800/60">
                      {hero.competences.map((skill) => (
                        <div key={skill.id} className="py-2 first:pt-0 last:pb-0 space-y-1">
                          <div className="flex items-center justify-between gap-3">
                            <span className="text-sm text-slate-200 font-medium">{skill.nom}</span>
                            <span
                              className={`text-xs font-mono-tabular whitespace-nowrap shrink-0 ${
                                skill.incertain ? 'text-amber-400' : 'text-slate-300'
                              }`}
                            >
                              {skill.niveau}
                              {skill.incertain ? ' · Incertain' : ''}
                            </span>
                          </div>
                          {skill.description && (
                            <p className="text-xs text-slate-400 leading-relaxed">
                              {skill.description}
                            </p>
                          )}
                          {skill.incertain && skill.raisonIncertitude && (
                            <p className="text-[11px] text-amber-300/90">
                              Incertitude : {skill.raisonIncertitude}
                            </p>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
