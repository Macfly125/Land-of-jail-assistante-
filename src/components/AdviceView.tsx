import React, { useState } from 'react';
import {
  AlertTriangle,
  ExternalLink,
  Globe,
  Image as ImageIcon,
  Loader2,
  RefreshCw,
  Swords,
} from 'lucide-react';
import {
  ADVICE_EVENT_LIST,
  APP_LANGUAGE_LABELS,
  AdviceMode,
  AppLanguage,
  HeroRecord,
  PLAYER_ROLE_LABELS,
  PlayerRole,
  TacticalAdviceResult,
} from '../types/game';
import { EVENT_BRIEF_BY_LANG, UI_COPY } from '../i18n/translations';
import { formatFrenchDate, formatFrenchDateTime } from '../utils/storage';

interface AdviceViewProps {
  language: AppLanguage;
  onChangeLanguage: (lang: AppLanguage) => void;
  playerRole: PlayerRole;
  onChangePlayerRole: (role: PlayerRole) => void;
  heroes: HeroRecord[];
  powerText?: string;
  adviceByMode: Partial<Record<AdviceMode, TacticalAdviceResult>>;
  onSaveAdvice: (mode: AdviceMode, advice: TacticalAdviceResult) => void;
}

const LANGUAGES_ORDER: AppLanguage[] = ['fr', 'tr', 'ru', 'zh', 'de', 'en'];

export const AdviceView: React.FC<AdviceViewProps> = ({
  language,
  onChangeLanguage,
  playerRole,
  onChangePlayerRole,
  heroes,
  powerText,
  adviceByMode,
  onSaveAdvice,
}) => {
  const [selectedMode, setSelectedMode] = useState<AdviceMode>('trial_cage');
  const [extraQuestion, setExtraQuestion] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [bannerFailed, setBannerFailed] = useState(false);
  const [brokenImages, setBrokenImages] = useState<Record<string, boolean>>({});

  const copy = UI_COPY[language];
  const currentAdvice = adviceByMode[selectedMode] ?? null;
  const activeEventInfo =
    ADVICE_EVENT_LIST.find((e) => e.id === selectedMode) ?? ADVICE_EVENT_LIST[0];
  const activeEventLabel = activeEventInfo.label[language];

  const markBrokenImage = (url: string) => {
    setBrokenImages((prev) => ({ ...prev, [url]: true }));
  };

  const fetchAdvice = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await fetch('/api/advice', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          mode: selectedMode,
          language,
          playerRole,
          playerRoleLabel: PLAYER_ROLE_LABELS[playerRole].label,
          heroes: heroes.map((h) => ({
            nom: h.nom,
            niveau: h.niveau,
            niveauTexte: h.niveauTexte,
            rarete: h.rarete,
            etoiles: h.etoiles,
            roleHero: h.roleHero,
            typeTroupe: h.typeTroupe,
            puissanceHero: h.puissanceHero,
            champsIncertains: h.champsIncertains || [],
            incertain: h.incertain,
            verifieManuellement: h.verifieManuellement || false,
            competences: h.competences.map((s) => ({
              nom: s.nom,
              niveau: s.niveau,
              description: s.description,
              incertain: s.incertain,
              raisonIncertitude: s.raisonIncertitude,
            })),
          })),
          powerText,
          extraQuestion: extraQuestion.trim() || undefined,
        }),
      });
      const data = (await response.json()) as {
        advice?: TacticalAdviceResult;
        error?: string;
      };
      if (!response.ok || !data.advice) {
        throw new Error(data.error || 'Échec de la récupération des conseils Internet.');
      }
      onSaveAdvice(selectedMode, {
        ...data.advice,
        id: `${selectedMode}-${Date.now()}`,
        heroesContextCount: heroes.length,
      });
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur réseau inattendue.');
    } finally {
      setIsLoading(false);
    }
  };

  const guidePhotos = (currentAdvice?.sources ?? [])
    .filter((s) => Boolean(s.imageUrl) && !brokenImages[s.imageUrl as string])
    .map((s) => ({
      imageUrl: s.imageUrl as string,
      title: s.title,
      url: s.url,
      date: s.detectedDate,
    }));

  return (
    <div className="space-y-6">
      {/* Bannière tactique + Langue + Sélecteur complet des 6 événements */}
      <section className="relative rounded-2xl overflow-hidden border border-slate-800/90 bg-slate-900 min-h-[210px] flex items-end">
        {!bannerFailed ? (
          <img
            src="/src/assets/images/tactical_arena_banner_1795282256499.jpg"
            alt="Arène tactique Lands of Jail"
            referrerPolicy="no-referrer"
            onError={() => setBannerFailed(true)}
            className="absolute inset-0 w-full h-full object-cover object-center opacity-50"
          />
        ) : (
          <div className="absolute inset-0 bg-gradient-to-br from-slate-900 via-[#151E2E] to-amber-950/30" />
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-[#0B0F17] via-[#0B0F17]/75 to-transparent" />

        <div className="relative z-10 w-full p-6 space-y-4">
          <div className="flex flex-col md:flex-row md:items-start justify-between gap-4">
            <div className="space-y-1.5 max-w-2xl">
              <h1 className="text-2xl font-semibold text-slate-50">{copy.adviceHeroTitle}</h1>
              <p className="text-sm text-slate-300 leading-relaxed">{copy.adviceHeroDesc}</p>
            </div>

            <div className="flex flex-wrap items-center gap-1 bg-[#0B0F17]/85 border border-slate-700/80 rounded-xl p-1 shrink-0 self-start">
              {LANGUAGES_ORDER.map((langCode) => (
                <button
                  key={langCode}
                  type="button"
                  onClick={() => onChangeLanguage(langCode)}
                  title={APP_LANGUAGE_LABELS[langCode]}
                  className={`px-2.5 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                    language === langCode
                      ? 'bg-amber-500 text-slate-950'
                      : 'text-slate-300 hover:text-slate-100'
                  }`}
                >
                  {langCode.toUpperCase()}
                </button>
              ))}
            </div>
          </div>

          {/* Tous les événements Lands of Jail */}
          <div className="flex flex-wrap gap-1.5 p-1.5 bg-[#0B0F17]/90 border border-slate-700/80 rounded-xl">
            {ADVICE_EVENT_LIST.map((ev) => {
              const active = selectedMode === ev.id;
              return (
                <button
                  key={ev.id}
                  type="button"
                  onClick={() => setSelectedMode(ev.id)}
                  className={`min-h-[38px] px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-colors whitespace-nowrap ${
                    active
                      ? 'bg-amber-500 text-slate-950'
                      : 'text-slate-300 hover:text-slate-100 hover:bg-slate-800/70'
                  }`}
                >
                  {ev.label[language]}
                </button>
              );
            })}
          </div>
        </div>
      </section>

      {/* Résumé tactique multilingue intégré pour l'événement sélectionné */}
      <section className="p-5 rounded-2xl bg-[#121826] border border-amber-500/25 space-y-1.5">
        <div className="text-xs font-semibold uppercase tracking-wider text-amber-400">
          {activeEventLabel}
        </div>
        <p className="text-sm text-slate-200 leading-relaxed">
          {EVENT_BRIEF_BY_LANG[selectedMode][language]}
        </p>
      </section>

      {/* Contexte utilisé & Lancement recherche Internet */}
      <section className="p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label
              htmlFor="advice-role-select"
              className="block text-xs font-medium text-slate-400 mb-1.5"
            >
              {copy.roleLabel}
            </label>
            <select
              id="advice-role-select"
              value={playerRole}
              onChange={(e) => onChangePlayerRole(e.target.value as PlayerRole)}
              className="w-full rounded-xl bg-slate-900 border border-slate-800 px-3.5 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-amber-500"
            >
              {(Object.keys(PLAYER_ROLE_LABELS) as PlayerRole[]).map((key) => (
                <option key={key} value={key}>
                  {PLAYER_ROLE_LABELS[key].label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <div className="text-xs font-medium text-slate-400 mb-1.5">
              {copy.heroesContextLabel}
            </div>
            <div className="rounded-xl bg-slate-900/70 border border-slate-800 px-3.5 py-2.5 text-sm text-slate-200 truncate">
              {heroes.length > 0
                ? `${heroes.length} héros · ${heroes
                    .slice(0, 4)
                    .map((h) => `${h.nom} (${h.niveauTexte})`)
                    .join(', ')}${heroes.length > 4 ? '…' : ''}`
                : copy.noHeroesYet}
            </div>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row gap-3">
          <input
            type="text"
            value={extraQuestion}
            onChange={(e) => setExtraQuestion(e.target.value)}
            placeholder={`${activeEventLabel}…`}
            className="flex-1 rounded-xl bg-slate-900 border border-slate-800 px-3.5 py-2.5 text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-amber-500"
          />
          <button
            type="button"
            disabled={isLoading}
            onClick={() => void fetchAdvice()}
            className="min-h-[44px] px-5 py-2.5 rounded-xl text-sm font-semibold bg-amber-500 text-slate-950 hover:bg-amber-400 disabled:opacity-50 transition-colors flex items-center justify-center gap-2 whitespace-nowrap shrink-0"
          >
            {isLoading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>{copy.searchingBtn}</span>
              </>
            ) : (
              <>
                <RefreshCw className="w-4 h-4" />
                <span>
                  {currentAdvice ? copy.refreshInternetBtn : copy.searchInternetBtn}
                </span>
              </>
            )}
          </button>
        </div>

        {error && (
          <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/30 text-xs text-red-200">
            {error}
          </div>
        )}
      </section>

      {/* Résultat des conseils */}
      {!currentAdvice ? (
        <section className="p-10 rounded-2xl bg-[#121826] border border-slate-800/80 text-center space-y-2">
          <Swords className="w-8 h-8 text-amber-400 mx-auto mb-2" />
          <p className="text-sm font-medium text-slate-200">{activeEventLabel}</p>
          <p className="text-xs text-slate-400 max-w-md mx-auto">{copy.adviceHeroDesc}</p>
        </section>
      ) : (
        <div className="space-y-6">
          {/* Galerie photos extraites des guides Internet */}
          {guidePhotos.length > 0 && (
            <section className="p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-4">
              <h2 className="text-base font-semibold text-slate-100 flex items-center gap-2">
                <ImageIcon className="w-4 h-4 text-amber-400" />
                <span>{copy.photosGalleryTitle}</span>
              </h2>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {guidePhotos.map((photo, idx) => (
                  <a
                    key={`${photo.imageUrl}-${idx}`}
                    href={photo.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="group rounded-xl overflow-hidden bg-slate-900 border border-slate-800 hover:border-amber-500/50 transition-colors"
                  >
                    <div className="aspect-video w-full bg-slate-950 overflow-hidden">
                      <img
                        src={photo.imageUrl}
                        alt={photo.title}
                        referrerPolicy="no-referrer"
                        onError={() => markBrokenImage(photo.imageUrl)}
                        className="w-full h-full object-cover group-hover:scale-[1.02] transition-transform"
                      />
                    </div>
                    <div className="p-3 space-y-1">
                      <div className="text-xs font-medium text-slate-100 line-clamp-2">
                        {photo.title}
                      </div>
                      {photo.date && (
                        <div className="text-[11px] font-mono-tabular text-slate-400">
                          {photo.date}
                        </div>
                      )}
                    </div>
                  </a>
                ))}
              </div>
            </section>
          )}

          {/* Synthèse & Composition */}
          <section className="p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-slate-400">
              <span>
                {activeEventLabel} ·{' '}
                {PLAYER_ROLE_LABELS[currentAdvice.playerRole]?.label || currentAdvice.playerRole}
              </span>
              <span className="font-mono-tabular">
                {formatFrenchDateTime(currentAdvice.generatedAt)}
              </span>
            </div>

            <div className="space-y-2">
              <h2 className="text-lg font-semibold text-slate-50">{copy.tacticalSummaryTitle}</h2>
              <p className="text-sm text-slate-200 leading-relaxed">{currentAdvice.synthese}</p>
            </div>

            <div className="pt-4 border-t border-slate-800/80 space-y-1.5">
              <h3 className="text-sm font-semibold text-amber-300">
                {copy.recommendedCompTitle}
              </h3>
              <p className="text-sm text-slate-300 leading-relaxed">
                {currentAdvice.compositionRecommandee}
              </p>
            </div>
            {currentAdvice.troopRatioProfiles && currentAdvice.troopRatioProfiles.length > 0 && (
              <div className="pt-4 border-t border-slate-800/80 space-y-3">
                <h3 className="text-sm font-semibold text-amber-300">
                  {currentAdvice.troopRatioTitle || 'Troop ratios'}
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {currentAdvice.troopRatioProfiles.map((profile) => (
                    <div
                      key={profile.id}
                      className="rounded-xl border border-slate-700/80 bg-slate-950/60 p-4 space-y-3"
                    >
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <span className="text-sm font-semibold text-slate-100">{profile.label}</span>
                        <span className="text-[10px] uppercase tracking-wide text-slate-400">
                          {profile.roleLabel}
                        </span>
                      </div>
                      <div className="grid grid-cols-3 gap-2 text-center">
                        <div className="rounded-lg bg-slate-800/70 p-2">
                          <div className="text-[10px] text-slate-400">Shieldbearer</div>
                          <div className="text-lg font-bold text-slate-100 tabular-nums">{profile.shieldbearer}%</div>
                        </div>
                        <div className="rounded-lg bg-slate-800/70 p-2">
                          <div className="text-[10px] text-slate-400">Bomber</div>
                          <div className="text-lg font-bold text-slate-100 tabular-nums">{profile.bomber}%</div>
                        </div>
                        <div className="rounded-lg bg-slate-800/70 p-2">
                          <div className="text-[10px] text-slate-400">Shooter</div>
                          <div className="text-lg font-bold text-slate-100 tabular-nums">{profile.shooter}%</div>
                        </div>
                      </div>
                      <p className="text-xs text-slate-400 leading-relaxed">{profile.note}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </section>

          {/* Informations incertaines */}
          {currentAdvice.informationsIncertaines.length > 0 && (
            <section className="p-5 rounded-2xl bg-amber-500/5 border border-amber-500/35 space-y-2.5">
              <div className="flex items-center gap-2 text-sm font-semibold text-amber-300">
                <AlertTriangle className="w-4 h-4 shrink-0" />
                <span>{copy.uncertainTitle}</span>
              </div>
              <ul className="space-y-1.5 text-xs text-amber-100/90 list-disc pl-5 leading-relaxed">
                {currentAdvice.informationsIncertaines.map((item, idx) => (
                  <li key={idx}>{item}</li>
                ))}
              </ul>
            </section>
          )}

          {/* Recommandations détaillées avec photos si disponibles */}
          {currentAdvice.recommandations.length > 0 && (
            <section className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {currentAdvice.recommandations.map((rec, idx) => (
                <div
                  key={idx}
                  className={`rounded-2xl bg-[#121826] border overflow-hidden flex flex-col ${
                    rec.incertain ? 'border-amber-500/35' : 'border-slate-800/90'
                  }`}
                >
                  {rec.imageUrl && !brokenImages[rec.imageUrl] && (
                    <div className="aspect-video w-full bg-slate-950 border-b border-slate-800/80">
                      <img
                        src={rec.imageUrl}
                        alt={rec.titre}
                        referrerPolicy="no-referrer"
                        onError={() => markBrokenImage(rec.imageUrl as string)}
                        className="w-full h-full object-cover"
                      />
                    </div>
                  )}
                  <div className="p-5 space-y-2.5 flex-1">
                    <div className="flex items-start justify-between gap-2">
                      <h3 className="text-base font-semibold text-slate-100">{rec.titre}</h3>
                      {rec.incertain && (
                        <span className="text-xs font-medium text-amber-400 whitespace-nowrap shrink-0">
                          Incertain
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-slate-300 leading-relaxed">{rec.detail}</p>
                    {rec.herosCibles.length > 0 && (
                      <div className="text-xs text-slate-400 pt-1">
                        {rec.herosCibles.join(' · ')}
                      </div>
                    )}
                    {rec.incertain && rec.raisonIncertitude && (
                      <div className="text-xs text-amber-300 pt-1">{rec.raisonIncertitude}</div>
                    )}
                  </div>
                </div>
              ))}
            </section>
          )}

          {/* Sources Internet, Photos & Dates */}
          <section className="p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-base font-semibold text-slate-100 flex items-center gap-2">
                <Globe className="w-4 h-4 text-amber-400" />
                <span>{copy.sourcesTitle}</span>
              </h2>
              <span className="text-xs text-slate-400 font-mono-tabular">
                {formatFrenchDate(currentAdvice.generatedAt)}
              </span>
            </div>

            {currentAdvice.sources.length === 0 ? (
              <p className="text-xs text-amber-300">
                Aucune URL de source directe retournée pour cette requête.
              </p>
            ) : (
              <div className="divide-y divide-slate-800/70">
                {currentAdvice.sources.map((src, idx) => (
                  <div
                    key={`${src.url}-${idx}`}
                    className="py-3 first:pt-0 last:pb-0 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      {src.imageUrl && !brokenImages[src.imageUrl] && (
                        <img
                          src={src.imageUrl}
                          alt={src.title}
                          referrerPolicy="no-referrer"
                          onError={() => markBrokenImage(src.imageUrl as string)}
                          className="w-16 h-11 rounded-lg object-cover border border-slate-800 shrink-0 bg-slate-950"
                        />
                      )}
                      <div className="min-w-0">
                        <a
                          href={src.url}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-sm font-medium text-amber-300 hover:text-amber-200 inline-flex items-center gap-1.5 max-w-full"
                        >
                          <span className="truncate">{src.title}</span>
                          <ExternalLink className="w-3.5 h-3.5 shrink-0" />
                        </a>
                        <div className="text-xs text-slate-500 truncate mt-0.5">{src.url}</div>
                      </div>
                    </div>
                    <div className="text-xs font-mono-tabular text-slate-300 whitespace-nowrap shrink-0">
                      {src.detectedDate
                        ? `Date : ${src.detectedDate}`
                        : formatFrenchDate(currentAdvice.generatedAt)}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
};
