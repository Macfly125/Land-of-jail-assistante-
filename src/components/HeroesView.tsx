import React, { useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle,
  Check,
  Edit3,
  Loader2,
  Search,
  Star,
  Upload,
  X,
} from 'lucide-react';
import { HeroRecord } from '../types/game';
import { formatFrenchDateTime, normalizeName } from '../utils/storage';
import { HERO_SHARD_TIERS } from '../data/landsOfJailData';

interface CatalogHero {
  slug: string;
  nom: string;
  avatarUrl: string | null;
  imageUrl: string | null;
  rarete?: string;
  typeTroupe?: string;
  wikiUrl: string;
}

interface EnrichedHeroDetails {
  slug: string;
  nom: string;
  titre?: string;
  avatarUrl: string | null;
  imageUrl: string | null;
  rarete?: string;
  typeTroupe?: string;
  categorie?: string;
  saison?: string;
  puissanceMax5Etoiles?: string;
  resumeBio?: string;
  idealPour?: string;
  wikiUrl: string;
  competences: Array<{
    nom: string;
    niveau: string;
    description?: string;
    iconUrl?: string;
    incertain: boolean;
  }>;
}

interface HeroesViewProps {
  heroes: HeroRecord[];
  onVerifyHero: (heroId: string, updated: Partial<HeroRecord>) => void;
  onOpenShareModal: () => void;
}

function parseStars(etoiles?: string): number | null {
  if (!etoiles) return null;
  const value = etoiles.trim();

  // Formes explicites : 3★, 3/5, 3 étoiles, ou une rangée de 3 étoiles.
  const compact = value.match(/^([1-5])\s*★$/i);
  if (compact) return Number(compact[1]);

  const fraction = value.match(/^([1-5])\s*\/\s*5$/);
  if (fraction) return Number(fraction[1]);

  const labeled = value.match(/^([1-5])\s*(?:étoile|étoiles|star|stars)$/i);
  if (labeled) return Number(labeled[1]);

  const symbols = value.match(/^[★⭐]{1,5}$/u);
  if (symbols) return Array.from(value).length;

  // Ne pas extraire des chiffres d'un texte composite : "3/5 + 20 fragments"
  // ou une progression d'éclats ne représente pas un nombre d'étoiles certain.
  return null;
}

export const HeroesView: React.FC<HeroesViewProps> = ({
  heroes,
  onVerifyHero,
  onOpenShareModal,
}) => {
  const [query, setQuery] = useState('');
  const [catalog, setCatalog] = useState<CatalogHero[]>([]);
  const [isLoadingCatalog, setIsLoadingCatalog] = useState(true);
  const [catalogError, setCatalogError] = useState<string | null>(null);
  const [selectedSlug, setSelectedSlug] = useState<string | null>(null);
  const [details, setDetails] = useState<EnrichedHeroDetails | null>(null);
  const [isLoadingDetails, setIsLoadingDetails] = useState(false);
  const [detailsError, setDetailsError] = useState<string | null>(null);
  const [editingHeroId, setEditingHeroId] = useState<string | null>(null);
  const [draftLevel, setDraftLevel] = useState('');
  const [draftRole, setDraftRole] = useState('');

  const detectedByName = useMemo(() => {
    const map = new Map<string, HeroRecord>();
    for (const h of heroes) map.set(normalizeName(h.nom), h);
    return map;
  }, [heroes]);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const res = await fetch('/api/heroes/catalog?lang=fr');
        const data = (await res.json()) as { heroes?: CatalogHero[] };
        if (!cancelled && Array.isArray(data.heroes)) {
          setCatalog(data.heroes);
        } else if (!cancelled) {
          setCatalogError('Catalogue indisponible pour le moment.');
        }
      } catch {
        if (!cancelled) setCatalogError('Impossible de charger le catalogue des héros.');
      } finally {
        if (!cancelled) setIsLoadingCatalog(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const openDetails = (slug: string) => {
    setSelectedSlug(slug);
    setDetails(null);
    setDetailsError(null);
    setIsLoadingDetails(true);
    void (async () => {
      try {
        const catalogHero = catalog.find((c) => c.slug === slug);
        const res = await fetch('/api/heroes/enrich', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ names: [catalogHero?.nom || slug], language: 'fr' }),
        });
        const data = (await res.json()) as { details?: EnrichedHeroDetails[]; error?: string };
        const first = data.details?.[0];
        if (!res.ok || !first) {
          throw new Error(data.error || 'Fiche indisponible.');
        }
        setDetails(first);
      } catch (e) {
        setDetailsError(e instanceof Error ? e.message : 'Erreur de chargement de la fiche.');
      } finally {
        setIsLoadingDetails(false);
      }
    })();
  };

  const filteredCatalog = catalog.filter((c) => {
    const q = query.toLowerCase();
    return (
      c.nom.toLowerCase().includes(q) ||
      (c.rarete || '').toLowerCase().includes(q) ||
      (c.typeTroupe || '').toLowerCase().includes(q)
    );
  });

  const selectedDetected = details
    ? detectedByName.get(normalizeName(details.nom))
    : undefined;
  const selectedStars = parseStars(selectedDetected?.etoiles);
  const selectedStarTier =
    selectedStars !== null
      ? HERO_SHARD_TIERS.find((t) => t.star === Math.min(selectedStars, 5))
      : undefined;
  // The screenshot gives the star tier, not the partial shard progress inside it.
  // Show the published cost of the next tier; never present it as shards still missing.
  const nextStarTier =
    selectedStars !== null && selectedStars < 5
      ? HERO_SHARD_TIERS.find((tier) => tier.star === selectedStars + 1)
      : undefined;

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
          <h1 className="text-2xl font-semibold text-slate-50">Tous les héros</h1>
          <p className="text-sm text-slate-400 mt-1">
            Catalogue complet du jeu. Les héros gris ne sont pas encore dans votre compte : ceux
            détectés par analyse (auto ou manuelle) apparaissent en couleur avec leur progression.
          </p>
        </div>
        <div className="text-xs text-slate-400 shrink-0 self-start md:text-right">
          <div>
            <span className="font-semibold text-slate-200">{heroes.length}</span> héros détecté(s) ·{' '}
            <span className="font-semibold text-slate-200">{catalog.length}</span> au catalogue
          </div>
        </div>
      </div>

      {/* Barre de recherche */}
      <div className="relative">
        <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Rechercher un héros, une rareté (SSR, SR) ou un type (Shieldbearer…)…"
          className="w-full rounded-xl bg-[#121826] border border-slate-800 pl-10 pr-4 py-2.5 text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-amber-500"
        />
      </div>

      {isLoadingCatalog ? (
        <div className="p-10 rounded-2xl bg-[#121826] border border-slate-800/80 text-center">
          <Loader2 className="w-6 h-6 animate-spin text-amber-400 mx-auto" />
          <p className="text-sm text-slate-400 mt-3">Chargement du catalogue des héros…</p>
        </div>
      ) : catalogError && catalog.length === 0 ? (
        <div className="p-10 rounded-2xl bg-[#121826] border border-slate-800/80 text-center space-y-4">
          <p className="text-sm text-slate-300">{catalogError}</p>
          <button
            type="button"
            onClick={onOpenShareModal}
            className="min-h-[44px] px-4 py-2 rounded-xl text-sm font-semibold bg-amber-500 text-slate-950 hover:bg-amber-400 transition-colors inline-flex items-center gap-2"
          >
            <Upload className="w-4 h-4" />
            <span>Partager une capture de fiche héros</span>
          </button>
        </div>
      ) : filteredCatalog.length === 0 ? (
        <div className="p-10 rounded-2xl bg-[#121826] border border-slate-800/80 text-center">
          <p className="text-sm text-slate-300">Aucun héros ne correspond à votre recherche.</p>
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
          {filteredCatalog.map((entry) => {
            const detected = detectedByName.get(normalizeName(entry.nom));
            return (
              <button
                key={entry.slug}
                type="button"
                onClick={() => openDetails(entry.slug)}
                className={`group relative rounded-2xl border overflow-hidden text-left transition-all ${
                  detected
                    ? 'bg-[#121826] border-amber-500/50 hover:border-amber-400'
                    : 'bg-[#0D1320] border-slate-800/90 hover:border-slate-700'
                }`}
              >
                <div
                  className={`aspect-square w-full overflow-hidden ${
                    detected ? '' : 'opacity-45 saturate-0'
                  }`}
                >
                  {entry.avatarUrl || entry.imageUrl ? (
                    <img
                      src={entry.avatarUrl || entry.imageUrl || ''}
                      alt={entry.nom}
                      loading="lazy"
                      referrerPolicy="no-referrer"
                      className="w-full h-full object-cover object-top group-hover:scale-105 transition-transform duration-200"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-2xl font-semibold text-slate-600">
                      {entry.nom.charAt(0)}
                    </div>
                  )}
                </div>
                <div className="p-3 space-y-1">
                  <div className="text-sm font-semibold text-slate-100 truncate">{entry.nom}</div>
                  <div className="flex items-center justify-between gap-2 text-[11px]">
                    <span
                      className={
                        entry.rarete === 'SSR'
                          ? 'text-amber-400 font-semibold'
                          : entry.rarete === 'SR'
                            ? 'text-sky-400 font-semibold'
                            : 'text-slate-400 font-semibold'
                      }
                    >
                      {entry.rarete || '—'}
                    </span>
                    {detected && (
                      <span className="text-emerald-400 font-medium truncate">
                        {detected.niveauTexte}
                      </span>
                    )}
                  </div>
                  {detected?.etoiles && (
                    <div className="text-amber-400 text-[11px] leading-none">{detected.etoiles}</div>
                  )}
                </div>
              </button>
            );
          })}
        </div>
      )}

      {/* Fiche détaillée du héros sélectionné */}
      {selectedSlug && (
        <div
          className="fixed inset-0 z-[60] flex items-end md:items-center justify-center bg-black/75 backdrop-blur-sm p-2 sm:p-4 md:p-6 overflow-hidden"
          role="dialog"
          aria-modal="true"
          onClick={() => setSelectedSlug(null)}
        >
          <div
            className="w-full max-w-lg bg-[#121826] border border-slate-800 rounded-2xl shadow-2xl max-h-[88dvh] flex flex-col overflow-hidden"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="md:hidden w-10 h-1 bg-slate-700 rounded-full mx-auto mt-2.5 shrink-0" />
            <div className="flex items-center justify-between px-5 py-3.5 border-b border-slate-800 shrink-0">
              <h2 className="text-base font-semibold text-slate-100 truncate">
                {details?.nom || 'Fiche héros'}
              </h2>
              <button
                type="button"
                onClick={() => setSelectedSlug(null)}
                aria-label="Fermer"
                className="min-h-[40px] min-w-[40px] flex items-center justify-center rounded-xl text-slate-400 hover:text-slate-100 hover:bg-slate-800 shrink-0"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="flex-1 min-h-0 overflow-y-auto p-5 space-y-4">
              {isLoadingDetails ? (
                <div className="py-10 text-center">
                  <Loader2 className="w-6 h-6 animate-spin text-amber-400 mx-auto" />
                  <p className="text-xs text-slate-400 mt-3">Chargement de la fiche…</p>
                </div>
              ) : detailsError ? (
                <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/30 text-xs text-red-200">
                  {detailsError}
                </div>
              ) : details ? (
                <>
                  <div className="flex items-start gap-4">
                    {details.imageUrl || details.avatarUrl ? (
                      <img
                        src={details.imageUrl || details.avatarUrl || ''}
                        alt={details.nom}
                        referrerPolicy="no-referrer"
                        className={`w-24 h-24 rounded-xl object-cover object-top border shrink-0 ${
                          detectedByName.has(normalizeName(details.nom))
                            ? 'border-amber-500/60'
                            : 'border-slate-800 opacity-60 saturate-0'
                        }`}
                      />
                    ) : null}
                    <div className="min-w-0 space-y-1.5 text-xs">
                      {details.titre && (
                        <div className="text-sm font-medium text-slate-200">{details.titre}</div>
                      )}
                      <div className="flex flex-wrap gap-1.5">
                        {details.rarete && (
                          <span className="px-2 py-0.5 rounded-lg bg-amber-500/15 border border-amber-500/40 text-amber-300 font-semibold">
                            {details.rarete}
                          </span>
                        )}
                        {details.typeTroupe && (
                          <span className="px-2 py-0.5 rounded-lg bg-slate-800 border border-slate-700 text-slate-200">
                            {details.typeTroupe}
                          </span>
                        )}
                        {details.saison && (
                          <span className="px-2 py-0.5 rounded-lg bg-slate-800 border border-slate-700 text-slate-300">
                            {details.saison}
                          </span>
                        )}
                      </div>
                      {details.puissanceMax5Etoiles && (
                        <div className="text-slate-300">
                          Puissance max 5★ :{' '}
                          <span className="font-semibold text-amber-300 font-mono-tabular">
                            {details.puissanceMax5Etoiles}
                          </span>
                        </div>
                      )}
                      {selectedDetected && (
                        <div className="space-y-1.5">
                          <div className="text-slate-300">
                            Progression de votre compte :{' '}
                            <span className="font-semibold text-emerald-400">
                              {selectedDetected.niveauTexte || 'Niveau non relevé'}
                              {selectedDetected.etoiles ? ` · ${selectedDetected.etoiles}` : ''}
                            </span>
                            {selectedDetected.incertain && (
                              <span className="ml-1 text-amber-300">(lecture à confirmer)</span>
                            )}
                          </div>
                          {selectedDetected.rarete && (
                            <div className="text-slate-300">
                              Rareté relevée sur votre capture :{' '}
                              <span className="font-semibold text-emerald-400">
                                {selectedDetected.rarete}
                              </span>
                              {selectedDetected.incertain && (
                                <span className="ml-1 text-amber-300">(à confirmer)</span>
                              )}
                            </div>
                          )}
                          {selectedDetected.puissanceHero && (
                            <div className="text-slate-300">
                              Puissance réelle relevée :{' '}
                              <span className="font-semibold text-emerald-400 font-mono-tabular">
                                {selectedDetected.puissanceHero}
                              </span>
                            </div>
                          )}
                        </div>
                      )}
                      {selectedStars !== null && (
                        <div className="text-slate-300">
                          Palier d’étoiles détecté :{' '}
                          <span className="font-semibold text-amber-300">
                            {selectedStarTier?.label || `${selectedStars}★`}
                          </span>
                        </div>
                      )}
                      {nextStarTier && (
                        <div className="text-slate-300">
                          Coût théorique du palier suivant :{' '}
                          <span className="font-semibold text-amber-300 font-mono-tabular">
                            {nextStarTier.shardsForTier}
                          </span>{' '}
                          éclats. La progression partielle en fragments n’est pas connue à partir
                          de cette capture.
                        </div>
                      )}
                      {details.wikiUrl && (
                        <a
                          href={details.wikiUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="inline-block text-amber-400 hover:text-amber-300 font-medium"
                        >
                          Voir la page du wiki →
                        </a>
                      )}
                    </div>
                  </div>

                  {details.resumeBio && (
                    <p className="text-xs text-slate-300 leading-relaxed">{details.resumeBio}</p>
                  )}
                  {details.idealPour && (
                    <div className="text-xs text-slate-400">
                      <span className="font-semibold text-slate-300">Idéal pour : </span>
                      {details.idealPour}
                    </div>
                  )}

                  <div className="space-y-2 border-t border-slate-800/80 pt-3">
                    <div className="text-xs font-semibold text-slate-300">
                      Compétences (niveaux maximum)
                    </div>
                    {details.competences.length === 0 ? (
                      <p className="text-xs text-slate-500">
                        Compétences non documentées pour ce héros.
                      </p>
                    ) : (
                      <div className="divide-y divide-slate-800/60">
                        {details.competences.map((skill) => (
                          <div key={skill.nom} className="py-2 first:pt-0 space-y-1">
                            <div className="flex items-center justify-between gap-3">
                              <span className="text-sm text-slate-200 font-medium flex items-center gap-2">
                                {skill.iconUrl && (
                                  <img
                                    src={skill.iconUrl}
                                    alt=""
                                    loading="lazy"
                                    referrerPolicy="no-referrer"
                                    className="w-6 h-6 rounded-md border border-slate-700"
                                  />
                                )}
                                {skill.nom}
                              </span>
                              <span className="text-xs text-slate-400 whitespace-nowrap shrink-0 flex items-center gap-1">
                                <Star className="w-3 h-3 text-amber-400" />
                                {skill.niveau}
                              </span>
                            </div>
                            {skill.description && (
                              <p className="text-xs text-slate-400 leading-relaxed">
                                {skill.description}
                              </p>
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </>
              ) : null}
            </div>
          </div>
        </div>
      )}

      {/* Héros détectés : édition & compétences extraites des captures */}
      {heroes.length > 0 && (
        <div className="space-y-4 pt-2">
          <h2 className="text-lg font-semibold text-slate-100">
            Vos héros détectés ({heroes.length})
          </h2>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
            {heroes.map((hero) => {
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
                    <div className="flex items-center gap-3 min-w-0">
                      {(hero.imageUrl || hero.avatarUrl) && (
                        <img
                          src={hero.imageUrl || hero.avatarUrl || ''}
                          alt={hero.nom}
                          referrerPolicy="no-referrer"
                          className="w-12 h-12 rounded-xl object-cover object-top border border-slate-700 shrink-0"
                        />
                      )}
                      <div className="min-w-0">
                        <h3 className="text-lg font-semibold text-slate-50 truncate">{hero.nom}</h3>
                        <div className="text-xs text-slate-400 mt-0.5">
                          {hero.rarete || 'Rareté non précisée'}
                          {hero.etoiles ? ` · ${hero.etoiles}` : ''}
                          {hero.roleHero ? ` · ${hero.roleHero}` : ''}
                          {hero.puissanceHero ? ` · Puissance ${hero.puissanceHero}` : ''}
                        </div>
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
                              <span className="text-sm text-slate-200 font-medium flex items-center gap-2">
                                {skill.iconUrl && (
                                  <img
                                    src={skill.iconUrl}
                                    alt=""
                                    loading="lazy"
                                    referrerPolicy="no-referrer"
                                    className="w-6 h-6 rounded-md border border-slate-700"
                                  />
                                )}
                                {skill.nom}
                              </span>
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
        </div>
      )}
    </div>
  );
};
