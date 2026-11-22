import React, { useMemo, useState } from 'react';
import { AlertTriangle, Check, X } from 'lucide-react';
import { HeroRecord, ScreenshotAnalysis } from '../types/game';

interface AnalysisReviewModalProps {
  analysis: ScreenshotAnalysis;
  onConfirm: (analysis: ScreenshotAnalysis) => void;
  onDiscard: () => void;
}

export const AnalysisReviewModal: React.FC<AnalysisReviewModalProps> = ({
  analysis,
  onConfirm,
  onDiscard,
}) => {
  const [drafts, setDrafts] = useState<HeroRecord[]>(() =>
    analysis.heros.map((hero) => ({ ...hero, competences: hero.competences.map((skill) => ({ ...skill })) }))
  );

  const uncertainCount = useMemo(
    () => drafts.filter((hero) => hero.incertain || hero.competences.some((skill) => skill.incertain)).length,
    [drafts]
  );

  const updateHero = (id: string, patch: Partial<HeroRecord>) => {
    setDrafts((current) => current.map((hero) => hero.id === id ? { ...hero, ...patch } : hero));
  };

  const updateSkill = (heroId: string, skillId: string, niveau: string) => {
    setDrafts((current) => current.map((hero) => hero.id !== heroId ? hero : {
      ...hero,
      competences: hero.competences.map((skill) => skill.id === skillId ? { ...skill, niveau } : skill),
    }));
  };

  const confirm = () => {
    const invalid = drafts.some((hero) => {
      const level = hero.niveau;
      return !hero.nom.trim() || (level !== null && (!Number.isInteger(level) || level < 1 || level > 999));
    });
    if (invalid) return;

    const verifiedHeroes = drafts.map((hero) => ({
      ...hero,
      nom: hero.nom.trim(),
      niveauTexte: hero.niveau !== null ? `Niv. ${hero.niveau}` : hero.niveauTexte,
      incertain: false,
      raisonIncertitude: undefined,
      verifieManuellement: true,
      competences: hero.competences.map((skill) => ({
        ...skill,
        niveau: skill.niveau.trim() || 'Non vérifié',
        incertain: false,
        raisonIncertitude: undefined,
      })),
    }));
    onConfirm({ ...analysis, heros: verifiedHeroes });
  };

  return (
    <div className="fixed inset-0 z-[90] bg-black/80 backdrop-blur-sm flex items-end sm:items-center justify-center p-2 sm:p-5" role="dialog" aria-modal="true" aria-labelledby="analysis-review-title">
      <section className="w-full max-w-3xl max-h-[92dvh] overflow-hidden rounded-2xl border border-amber-500/40 bg-[#101827] shadow-2xl flex flex-col">
        <header className="px-4 sm:px-6 py-4 border-b border-slate-800 flex items-start justify-between gap-3">
          <div>
            <h2 id="analysis-review-title" className="text-lg font-semibold text-slate-50">Vérifier la capture avant enregistrement</h2>
            <p className="text-xs text-slate-400 mt-1">{analysis.fileName} · {drafts.length} héros détecté(s)</p>
          </div>
          <button type="button" onClick={onDiscard} aria-label="Fermer" className="p-2 rounded-lg text-slate-400 hover:bg-slate-800"><X className="w-5 h-5" /></button>
        </header>

        <div className="overflow-y-auto p-4 sm:p-6 space-y-4">
          <div className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-3 text-xs text-amber-100 flex gap-2">
            <AlertTriangle className="w-4 h-4 shrink-0 text-amber-300" />
            <span>Vérifie les noms, niveaux, étoiles et compétences avec la capture originale. Les données ne seront ajoutées au profil qu'après confirmation.</span>
          </div>

          {analysis.thumbnailDataUrl && (
            <details className="rounded-xl border border-slate-700 bg-slate-900/60 p-3">
              <summary className="cursor-pointer text-sm font-medium text-slate-200">Afficher la capture originale</summary>
              <img src={analysis.thumbnailDataUrl} alt="Capture originale à vérifier" className="mt-3 max-h-72 w-full object-contain rounded-lg bg-black" />
            </details>
          )}

          {drafts.length === 0 ? (
            <div className="rounded-xl border border-slate-700 p-4 text-sm text-slate-300">Aucun héros détecté sur cette capture. Tu peux tout de même confirmer l'analyse pour enregistrer les autres informations.</div>
          ) : drafts.map((hero, index) => (
            <article key={hero.id} className="rounded-xl border border-slate-700 bg-slate-900/60 p-3 sm:p-4 space-y-3">
              <div className="flex items-center justify-between gap-2">
                <h3 className="font-semibold text-slate-100 text-sm">Héros {index + 1}</h3>
                {hero.incertain || hero.competences.some((skill) => skill.incertain) ? (
                  <span className="text-[11px] text-amber-300">À vérifier</span>
                ) : <span className="text-[11px] text-emerald-300">Lecture nette</span>}
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <label className="text-xs text-slate-400">Nom du héros
                  <input value={hero.nom} onChange={(e) => updateHero(hero.id, { nom: e.target.value })} className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white" />
                </label>
                <label className="text-xs text-slate-400">Niveau (1–999)
                  <input type="number" min="1" max="999" value={hero.niveau ?? ''} onChange={(e) => updateHero(hero.id, { niveau: e.target.value ? Number(e.target.value) : null })} className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white" placeholder="Non détecté" />
                </label>
                <label className="text-xs text-slate-400">Étoiles
                  <input value={hero.etoiles || ''} onChange={(e) => updateHero(hero.id, { etoiles: e.target.value })} className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white" placeholder="Ex. ★★★★☆" />
                </label>
                <label className="text-xs text-slate-400">Puissance
                  <input value={hero.puissanceHero || ''} onChange={(e) => updateHero(hero.id, { puissanceHero: e.target.value })} className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white" placeholder="Valeur affichée en jeu" />
                </label>
              </div>
              {hero.competences.length > 0 && (
                <div className="border-t border-slate-700 pt-3 space-y-2">
                  <p className="text-xs font-medium text-slate-300">Compétences détectées</p>
                  {hero.competences.map((skill) => (
                    <label key={skill.id} className="grid grid-cols-[minmax(0,1fr)_120px] gap-2 items-center text-xs text-slate-300">
                      <span>{skill.nom}</span>
                      <input value={skill.niveau} onChange={(e) => updateSkill(hero.id, skill.id, e.target.value)} className="w-full rounded-lg border border-slate-700 bg-slate-950 px-2.5 py-2 text-xs text-white" aria-label={`Niveau de ${skill.nom}`} />
                    </label>
                  ))}
                </div>
              )}
              {hero.raisonIncertitude && <p className="text-[11px] text-amber-300">{hero.raisonIncertitude}</p>}
            </article>
          ))}
          {uncertainCount > 0 && <p className="text-xs text-amber-300">{uncertainCount} héros nécessitent une vérification attentive.</p>}
        </div>

        <footer className="border-t border-slate-800 p-4 flex flex-col-reverse sm:flex-row sm:justify-end gap-2">
          <button type="button" onClick={onDiscard} className="min-h-11 px-4 rounded-xl text-sm text-slate-300 hover:bg-slate-800">Annuler et ignorer</button>
          <button type="button" onClick={confirm} disabled={drafts.some((hero) => !hero.nom.trim() || (hero.niveau !== null && (!Number.isInteger(hero.niveau) || hero.niveau < 1 || hero.niveau > 999)))} className="min-h-11 px-4 rounded-xl bg-emerald-500 text-slate-950 font-semibold text-sm hover:bg-emerald-400 disabled:opacity-40 disabled:cursor-not-allowed inline-flex items-center justify-center gap-2"><Check className="w-4 h-4" />Confirmer et enregistrer</button>
        </footer>
      </section>
    </div>
  );
};
