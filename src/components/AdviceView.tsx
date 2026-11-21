import React, { useState } from 'react';
import {
  AlertTriangle,
  ExternalLink,
  Globe,
  Loader2,
  RefreshCw,
  Swords,
} from 'lucide-react';
import {
  AdviceMode,
  HeroRecord,
  PLAYER_ROLE_LABELS,
  PlayerRole,
  TacticalAdviceResult,
} from '../types/game';
import { formatFrenchDate, formatFrenchDateTime } from '../utils/storage';

interface AdviceViewProps {
  playerRole: PlayerRole;
  onChangePlayerRole: (role: PlayerRole) => void;
  heroes: HeroRecord[];
  powerText?: string;
  adviceByMode: Partial<Record<AdviceMode, TacticalAdviceResult>>;
  onSaveAdvice: (mode: AdviceMode, advice: TacticalAdviceResult) => void;
}

export const AdviceView: React.FC<AdviceViewProps> = ({
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

  const currentAdvice = adviceByMode[selectedMode] ?? null;

  const fetchAdvice = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await fetch('/api/advice', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          mode: selectedMode,
          playerRole,
          playerRoleLabel: PLAYER_ROLE_LABELS[playerRole].label,
          heroes: heroes.map((h) => ({
            nom: h.nom,
            niveauTexte: h.niveauTexte,
            rarete: h.rarete,
            roleHero: h.roleHero,
            incertain: h.incertain,
            competences: h.competences.map((s) => ({
              nom: s.nom,
              niveau: s.niveau,
              incertain: s.incertain,
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

  return (
    <div className="space-y-6">
      {/* Bannière tactique avec scrim mesuré et fallback CSS */}
      <section className="relative rounded-2xl overflow-hidden border border-slate-800/90 bg-slate-900 min-h-[180px] flex items-end">
        {!bannerFailed ? (
          <img
            src="/src/assets/images/tactical_arena_banner_1795282256499.jpg"
            alt="Arène tactique Trial Cage et Crystal Clash"
            referrerPolicy="no-referrer"
            onError={() => setBannerFailed(true)}
            className="absolute inset-0 w-full h-full object-cover object-center opacity-55"
          />
        ) : (
          <div className="absolute inset-0 bg-gradient-to-br from-slate-900 via-[#151E2E] to-amber-950/30" />
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-[#0B0F17] via-[#0B0F17]/65 to-transparent" />

        <div className="relative z-10 w-full p-6 flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div className="space-y-1.5 max-w-2xl">
            <h1 className="text-2xl font-semibold text-slate-50">
              Conseils Trial Cage & Crystal Clash
            </h1>
            <p className="text-sm text-slate-300 leading-relaxed">
              Stratégies adaptées à vos héros visibles et à votre rôle, enrichies par recherche
              Internet avec date et source affichées.
            </p>
          </div>

          <div className="flex items-center gap-1 p-1 bg-[#0B0F17]/90 border border-slate-700/80 rounded-xl shrink-0 self-start">
            <button
              type="button"
              onClick={() => setSelectedMode('trial_cage')}
              className={`min-h-[40px] px-4 py-1.5 rounded-lg text-xs font-semibold transition-colors whitespace-nowrap ${
                selectedMode === 'trial_cage'
                  ? 'bg-amber-500 text-slate-950'
                  : 'text-slate-300 hover:text-slate-100'
              }`}
            >
              Trial Cage
            </button>
            <button
              type="button"
              onClick={() => setSelectedMode('crystal_clash')}
              className={`min-h-[40px] px-4 py-1.5 rounded-lg text-xs font-semibold transition-colors whitespace-nowrap ${
                selectedMode === 'crystal_clash'
                  ? 'bg-amber-500 text-slate-950'
                  : 'text-slate-300 hover:text-slate-100'
              }`}
            >
              Crystal Clash
            </button>
          </div>
        </div>
      </section>

      {/* Contexte utilisé & Lancement recherche Internet */}
      <section className="p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label
              htmlFor="advice-role-select"
              className="block text-xs font-medium text-slate-400 mb-1.5"
            >
              Rôle du joueur pris en compte
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
              Héros visibles injectés dans la stratégie
            </div>
            <div className="rounded-xl bg-slate-900/70 border border-slate-800 px-3.5 py-2.5 text-sm text-slate-200 truncate">
              {heroes.length > 0
                ? `${heroes.length} héros · ${heroes
                    .slice(0, 4)
                    .map((h) => `${h.nom} (${h.niveauTexte})`)
                    .join(', ')}${heroes.length > 4 ? '…' : ''}`
                : 'Aucun héros extrait (conseils généraux selon le rôle)'}
            </div>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row gap-3">
          <input
            type="text"
            value={extraQuestion}
            onChange={(e) => setExtraQuestion(e.target.value)}
            placeholder={`Précision optionnelle pour ${
              selectedMode === 'trial_cage' ? 'Trial Cage' : 'Crystal Clash'
            } (ex. : ordre des compétences, défense cristal…) `}
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
                <span>Recherche en cours…</span>
              </>
            ) : (
              <>
                <RefreshCw className="w-4 h-4" />
                <span>
                  {currentAdvice
                    ? 'Actualiser les conseils Internet'
                    : 'Rechercher des conseils Internet'}
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
          <p className="text-sm font-medium text-slate-200">
            Aucun conseil généré pour{' '}
            {selectedMode === 'trial_cage' ? 'Trial Cage' : 'Crystal Clash'}.
          </p>
          <p className="text-xs text-slate-400 max-w-md mx-auto">
            Lancez une recherche pour croiser vos héros visibles et votre rôle avec les guides
            Internet datés et sourcés.
          </p>
        </section>
      ) : (
        <div className="space-y-6">
          {currentAdvice.geminiWarning && (
            <section className="p-4 rounded-2xl bg-red-500/10 border border-red-500/35 text-xs text-red-200 flex items-start gap-2.5">
              <AlertTriangle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
              <div className="space-y-1">
                <div className="font-semibold text-red-100">Alerte API Gemini</div>
                <p className="leading-relaxed">{currentAdvice.geminiWarning}</p>
              </div>
            </section>
          )}

          {/* Synthèse & Composition */}
          <section className="p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-slate-400">
              <span>
                Mode : {selectedMode === 'trial_cage' ? 'Trial Cage' : 'Crystal Clash'} · Rôle :{' '}
                {PLAYER_ROLE_LABELS[currentAdvice.playerRole]?.label || currentAdvice.playerRole}
              </span>
              <span className="font-mono-tabular">
                Consulté le {formatFrenchDateTime(currentAdvice.generatedAt)}
              </span>
            </div>

            <div className="space-y-2">
              <h2 className="text-lg font-semibold text-slate-50">Synthèse tactique</h2>
              <p className="text-sm text-slate-200 leading-relaxed">{currentAdvice.synthese}</p>
            </div>

            <div className="pt-4 border-t border-slate-800/80 space-y-1.5">
              <h3 className="text-sm font-semibold text-amber-300">
                Composition & synergie recommandée
              </h3>
              <p className="text-sm text-slate-300 leading-relaxed">
                {currentAdvice.compositionRecommandee}
              </p>
            </div>
          </section>

          {/* Informations incertaines (toujours visibles clairement si présentes) */}
          {currentAdvice.informationsIncertaines.length > 0 && (
            <section className="p-5 rounded-2xl bg-amber-500/5 border border-amber-500/35 space-y-2.5">
              <div className="flex items-center gap-2 text-sm font-semibold text-amber-300">
                <AlertTriangle className="w-4 h-4 shrink-0" />
                <span>Informations incertaines ou à vérifier en jeu</span>
              </div>
              <ul className="space-y-1.5 text-xs text-amber-100/90 list-disc pl-5 leading-relaxed">
                {currentAdvice.informationsIncertaines.map((item, idx) => (
                  <li key={idx}>{item}</li>
                ))}
              </ul>
            </section>
          )}

          {/* Recommandations détaillées */}
          {currentAdvice.recommandations.length > 0 && (
            <section className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {currentAdvice.recommandations.map((rec, idx) => (
                <div
                  key={idx}
                  className={`p-5 rounded-2xl bg-[#121826] border space-y-2.5 ${
                    rec.incertain ? 'border-amber-500/35' : 'border-slate-800/90'
                  }`}
                >
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
                      Héros concernés : {rec.herosCibles.join(' · ')}
                    </div>
                  )}
                  {rec.incertain && rec.raisonIncertitude && (
                    <div className="text-xs text-amber-300 pt-1">
                      Raison d’incertitude : {rec.raisonIncertitude}
                    </div>
                  )}
                </div>
              ))}
            </section>
          )}

          {/* Sources Internet & Dates */}
          <section className="p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-base font-semibold text-slate-100 flex items-center gap-2">
                <Globe className="w-4 h-4 text-amber-400" />
                <span>Sources Internet & Dates</span>
              </h2>
              <span className="text-xs text-slate-400 font-mono-tabular">
                Recherche du {formatFrenchDate(currentAdvice.generatedAt)}
              </span>
            </div>

            {currentAdvice.sources.length === 0 ? (
              <p className="text-xs text-amber-300">
                Aucune URL de source directe retournée pour cette requête. Considérez ces éléments
                comme indicatifs jusqu’à vérification dans le jeu.
              </p>
            ) : (
              <div className="divide-y divide-slate-800/70">
                {currentAdvice.sources.map((src, idx) => (
                  <div
                    key={`${src.url}-${idx}`}
                    className="py-3 first:pt-0 last:pb-0 flex flex-col sm:flex-row sm:items-center justify-between gap-2"
                  >
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
                    <div className="text-xs font-mono-tabular text-slate-300 whitespace-nowrap shrink-0">
                      {src.detectedDate
                        ? `Date source : ${src.detectedDate}`
                        : `Consulté le ${formatFrenchDate(currentAdvice.generatedAt)}`}
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
