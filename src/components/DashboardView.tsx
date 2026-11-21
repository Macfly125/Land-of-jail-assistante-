import React from 'react';
import {
  AlertTriangle,
  Camera,
  Code2,
  Eye,
  EyeOff,
  PowerOff,
  Radio,
  ShieldCheck,
  SlidersHorizontal,
  Swords,
  Upload,
} from 'lucide-react';
import {
  AccountState,
  HeroRecord,
  PLAYER_ROLE_LABELS,
  PlayerRole,
  SCREEN_CATEGORY_LABELS,
} from '../types/game';
import { formatFrenchDateTime } from '../utils/storage';

interface DashboardViewProps {
  state: AccountState;
  hasUsagePermission: boolean;
  isDetectionActive: boolean;
  isGameDetectedForeground: boolean;
  packageFilter: string;
  onChangePackageFilter: (value: string) => void;
  onToggleUsagePermission: () => void;
  onStartDetection: () => void;
  onStopDetection: () => void;
  onSimulateGameLaunch: () => void;
  onOpenAnalyzeScreenPrompt: () => void;
  onOpenShareModal: () => void;
  onChangePlayerRole: (role: PlayerRole) => void;
  onNavigateTab: (tab: 'tableau' | 'heros' | 'ressources' | 'conseils' | 'historique') => void;
  onOpenAndroidCode: () => void;
  onOpenMobileGuide: () => void;
}

export const DashboardView: React.FC<DashboardViewProps> = ({
  state,
  hasUsagePermission,
  isDetectionActive,
  isGameDetectedForeground,
  packageFilter,
  onChangePackageFilter,
  onToggleUsagePermission,
  onStartDetection,
  onStopDetection,
  onSimulateGameLaunch,
  onOpenAnalyzeScreenPrompt,
  onOpenShareModal,
  onChangePlayerRole,
  onNavigateTab,
  onOpenAndroidCode,
  onOpenMobileGuide,
}) => {
  const uncertainHeroesCount = state.heroes.filter(
    (h) => h.incertain || h.competences.some((s) => s.incertain)
  ).length;
  const uncertainResourcesCount = state.resources.filter((r) => r.incertain).length;
  const totalUncertainCount =
    uncertainHeroesCount +
    uncertainResourcesCount +
    (state.currentPower?.incertain ? 1 : 0);

  const latestAnalysis = state.analyses[0] ?? null;
  const topHeroes: HeroRecord[] = [...state.heroes]
    .sort((a, b) => (b.niveau ?? 0) - (a.niveau ?? 0))
    .slice(0, 4);

  return (
    <div className="space-y-8">
      {/* En-tête synthétique */}
      <section className="flex flex-col lg:flex-row lg:items-end justify-between gap-4 border-b border-slate-800/80 pb-6">
        <div className="space-y-1.5 max-w-2xl">
          <h1 className="text-2xl md:text-3xl font-semibold tracking-tight text-slate-50">
            Tableau de bord du compte
          </h1>
          <p className="text-sm text-slate-400 leading-relaxed">
            Suivi par captures partagées ou analyse OCR autorisée à la demande. Aucune action dans
            le jeu, aucune capture sans votre validation.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-3 shrink-0">
          <button
            type="button"
            onClick={onOpenMobileGuide}
            className="min-h-[44px] px-4 py-2 rounded-xl text-sm font-medium bg-slate-900 border border-slate-800 text-slate-200 hover:border-slate-700 hover:bg-slate-800/80 transition-colors whitespace-nowrap shrink-0"
          >
            Guide téléphone (sans PC)
          </button>
          <button
            type="button"
            onClick={onOpenAndroidCode}
            className="min-h-[44px] px-4 py-2 rounded-xl text-sm font-medium bg-slate-900 border border-slate-800 text-slate-200 hover:border-slate-700 hover:bg-slate-800/80 transition-colors flex items-center gap-2 whitespace-nowrap shrink-0"
          >
            <Code2 className="w-4 h-4 text-amber-400" />
            <span>Code Android Kotlin</span>
          </button>
          <button
            type="button"
            onClick={onOpenShareModal}
            className="min-h-[44px] px-4 py-2 rounded-xl text-sm font-semibold bg-amber-500 text-slate-950 hover:bg-amber-400 transition-colors flex items-center gap-2 whitespace-nowrap shrink-0"
          >
            <Upload className="w-4 h-4" />
            <span>Partager une capture</span>
          </button>
        </div>
      </section>

      {/* Bandeau KPI à 4 colonnes */}
      <section className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-5 rounded-2xl bg-[#121826] border border-slate-800/90">
          <div className="text-xs text-slate-400">Puissance actuelle</div>
          <div className="mt-2 text-2xl font-semibold font-mono-tabular text-slate-50 truncate">
            {state.currentPower?.puissanceTotaleTexte || '—'}
          </div>
          <div className="mt-2 text-xs text-slate-400">
            {state.currentPower?.niveauPrisonOuQG
              ? `QG / Prison : ${state.currentPower.niveauPrisonOuQG}`
              : 'Aucune capture profil'}
            {state.currentPower?.incertain ? ' · Incertain' : ''}
          </div>
        </div>

        <button
          type="button"
          onClick={() => onNavigateTab('heros')}
          className="text-left p-5 rounded-2xl bg-[#121826] border border-slate-800/90 hover:border-slate-700 transition-colors"
        >
          <div className="text-xs text-slate-400">Héros suivis</div>
          <div className="mt-2 text-2xl font-semibold font-mono-tabular text-slate-50">
            {state.heroes.length}
          </div>
          <div className="mt-2 text-xs text-slate-400">
            {uncertainHeroesCount > 0
              ? `${uncertainHeroesCount} à vérifier`
              : 'Niveaux & compétences'}
          </div>
        </button>

        <button
          type="button"
          onClick={() => onNavigateTab('ressources')}
          className="text-left p-5 rounded-2xl bg-[#121826] border border-slate-800/90 hover:border-slate-700 transition-colors"
        >
          <div className="text-xs text-slate-400">Ressources suivies</div>
          <div className="mt-2 text-2xl font-semibold font-mono-tabular text-slate-50">
            {state.resources.length}
          </div>
          <div className="mt-2 text-xs text-slate-400">
            {uncertainResourcesCount > 0
              ? `${uncertainResourcesCount} à vérifier`
              : 'Stocks observés'}
          </div>
        </button>

        <button
          type="button"
          onClick={() => onNavigateTab('historique')}
          className="text-left p-5 rounded-2xl bg-[#121826] border border-slate-800/90 hover:border-slate-700 transition-colors"
        >
          <div className="text-xs text-slate-400">Informations incertaines</div>
          <div
            className={`mt-2 text-2xl font-semibold font-mono-tabular ${
              totalUncertainCount > 0 ? 'text-amber-400' : 'text-emerald-400'
            }`}
          >
            {totalUncertainCount}
          </div>
          <div className="mt-2 text-xs text-slate-400">
            {state.analyses.length} analyse(s) enregistrée(s)
          </div>
        </button>
      </section>

      {/* Grille principale : Détection Android UsageStatsManager + Rôle joueur */}
      <section className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Colonne gauche : Détection UsageStatsManager & OCR à la demande */}
        <div className="lg:col-span-7 p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-100">
                Détection de lancement (UsageStatsManager)
              </h2>
              <p className="text-xs text-slate-400 mt-1 leading-relaxed">
                Détecte uniquement l’ouverture de Lands of Jail au premier plan pour proposer le
                bouton « Analyser l’écran ». L’écran n’est jamais capturé sans votre appui et
                l’autorisation système visible.
              </p>
            </div>
            <div className="text-xs font-medium text-slate-300 whitespace-nowrap shrink-0">
              {isDetectionActive ? 'Détection active' : 'Détection arrêtée'}
            </div>
          </div>

          {/* Étape 1 : Permission Statistiques d'utilisation */}
          <div className="p-4 rounded-xl bg-slate-900/70 border border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="space-y-1">
              <div className="text-sm font-medium text-slate-100 flex items-center gap-2">
                <ShieldCheck
                  className={`w-4 h-4 shrink-0 ${
                    hasUsagePermission ? 'text-emerald-400' : 'text-amber-400'
                  }`}
                />
                <span>1. Accès aux statistiques d’utilisation Android</span>
              </div>
              <p className="text-xs text-slate-400">
                {hasUsagePermission
                  ? 'Autorisation accordée (AppOpsManager.OPSTR_GET_USAGE_STATS).'
                  : 'Requis dans Paramètres Android > Accès aux données d’utilisation.'}
              </p>
            </div>
            <button
              type="button"
              onClick={onToggleUsagePermission}
              className="min-h-[44px] px-4 py-2 rounded-xl text-xs font-medium border border-slate-700 bg-slate-800/80 text-slate-100 hover:bg-slate-700 transition-colors whitespace-nowrap shrink-0"
            >
              {hasUsagePermission ? 'Révoquer (test)' : 'Activer dans les paramètres'}
            </button>
          </div>

          {/* Package cible + Contrôles Démarrer / Arrêter */}
          <div className="space-y-3">
            <div>
              <label
                htmlFor="package-filter"
                className="block text-xs font-medium text-slate-300 mb-1.5"
              >
                Identifiant ou mot-clé du package Android
              </label>
              <input
                id="package-filter"
                type="text"
                value={packageFilter}
                onChange={(e) => onChangePackageFilter(e.target.value)}
                placeholder="com.justgame.jails"
                className="w-full rounded-xl bg-slate-900 border border-slate-800 px-3.5 py-2.5 text-sm font-mono text-slate-100 focus:outline-none focus:border-amber-500"
              />
            </div>

            <div className="flex flex-wrap items-center gap-3 pt-1">
              {!isDetectionActive ? (
                <button
                  type="button"
                  disabled={!hasUsagePermission}
                  onClick={onStartDetection}
                  className="min-h-[44px] px-4 py-2 rounded-xl text-sm font-semibold bg-amber-500 text-slate-950 hover:bg-amber-400 disabled:opacity-40 disabled:cursor-not-allowed transition-colors flex items-center gap-2 whitespace-nowrap shrink-0"
                >
                  <Eye className="w-4 h-4" />
                  <span>Démarrer la détection</span>
                </button>
              ) : (
                <button
                  type="button"
                  onClick={onStopDetection}
                  className="min-h-[44px] px-4 py-2 rounded-xl text-sm font-semibold bg-red-500/15 border border-red-500/40 text-red-200 hover:bg-red-500/25 transition-colors flex items-center gap-2 whitespace-nowrap shrink-0"
                >
                  <PowerOff className="w-4 h-4 text-red-400" />
                  <span>Arrêter la détection</span>
                </button>
              )}

              {isDetectionActive && (
                <button
                  type="button"
                  onClick={onSimulateGameLaunch}
                  className="min-h-[44px] px-4 py-2 rounded-xl text-xs font-medium bg-slate-800 text-slate-200 hover:bg-slate-700 transition-colors flex items-center gap-2 whitespace-nowrap shrink-0"
                >
                  <Radio className="w-4 h-4 text-emerald-400" />
                  <span>Simuler l’ouverture du jeu</span>
                </button>
              )}

              <button
                type="button"
                onClick={onOpenAnalyzeScreenPrompt}
                className="min-h-[44px] px-4 py-2 rounded-xl text-xs font-semibold bg-slate-900 border border-amber-500/40 text-amber-300 hover:bg-amber-500/10 transition-colors flex items-center gap-2 whitespace-nowrap shrink-0"
              >
                <Camera className="w-4 h-4" />
                <span>Analyser l’écran maintenant</span>
              </button>
            </div>
          </div>

          {/* État notification / bouton flottant si le jeu est détecté */}
          {isDetectionActive && (
            <div className="p-4 rounded-xl bg-slate-900/90 border border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div className="space-y-1 text-xs">
                <div className="font-medium text-slate-100">
                  {isGameDetectedForeground
                    ? 'Lands of Jail détecté au premier plan — Notification et bouton flottant affichés'
                    : 'Service en attente du passage de Lands of Jail au premier plan'}
                </div>
                <div className="text-slate-400">
                  Aucune action automatique dans le jeu · Capture uniquement sur clic utilisateur.
                </div>
              </div>
              {isGameDetectedForeground && (
                <button
                  type="button"
                  onClick={onOpenAnalyzeScreenPrompt}
                  className="min-h-[44px] px-4 py-2 rounded-xl text-xs font-semibold bg-amber-500 text-slate-950 hover:bg-amber-400 transition-colors whitespace-nowrap shrink-0"
                >
                  Analyser l’écran
                </button>
              )}
            </div>
          )}

          {!isDetectionActive && (
            <div className="flex items-center gap-2 text-xs text-slate-400 pt-1">
              <EyeOff className="w-4 h-4 shrink-0" />
              <span>
                La détection est désactivée. Vous pouvez aussi partager une capture manuellement à
                tout moment.
              </span>
            </div>
          )}
        </div>

        {/* Colonne droite : Rôle du joueur */}
        <div className="lg:col-span-5 p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-slate-100 flex items-center gap-2">
              <SlidersHorizontal className="w-4 h-4 text-amber-400" />
              <span>Rôle du joueur</span>
            </h2>
            <button
              type="button"
              onClick={() => onNavigateTab('conseils')}
              className="text-xs font-medium text-amber-400 hover:text-amber-300 flex items-center gap-1 whitespace-nowrap"
            >
              <Swords className="w-3.5 h-3.5" />
              <span>Voir les conseils</span>
            </button>
          </div>
          <p className="text-xs text-slate-400">
            Adapte les recommandations pour Trial Cage et Crystal Clash à votre style de jeu.
          </p>

          <div className="space-y-2">
            {(Object.keys(PLAYER_ROLE_LABELS) as PlayerRole[]).map((roleKey) => {
              const info = PLAYER_ROLE_LABELS[roleKey];
              const selected = state.playerRole === roleKey;
              return (
                <button
                  key={roleKey}
                  type="button"
                  onClick={() => onChangePlayerRole(roleKey)}
                  className={`w-full text-left p-3.5 rounded-xl border transition-colors ${
                    selected
                      ? 'bg-amber-500/10 border-amber-500/50 text-slate-50'
                      : 'bg-slate-900/50 border-slate-800 text-slate-300 hover:border-slate-700'
                  }`}
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-sm font-medium">{info.label}</span>
                    {selected && (
                      <span className="text-xs font-medium text-amber-400">Actif</span>
                    )}
                  </div>
                  <p className="text-xs text-slate-400 mt-1">{info.description}</p>
                </button>
              );
            })}
          </div>
        </div>
      </section>

      {/* Alerte champs incertains */}
      {totalUncertainCount > 0 && (
        <section className="p-5 rounded-2xl bg-amber-500/5 border border-amber-500/30 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
            <div>
              <h3 className="text-sm font-semibold text-amber-200">
                {totalUncertainCount} information(s) incertaine(s) détectée(s)
              </h3>
              <p className="text-xs text-amber-100/80 mt-1">
                Certains chiffres ou noms étaient partiellement lisibles sur la capture. Vous pouvez
                les vérifier ou les corriger dans Héros et Ressources.
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => onNavigateTab(uncertainHeroesCount > 0 ? 'heros' : 'ressources')}
            className="min-h-[44px] px-4 py-2 rounded-xl text-xs font-semibold bg-amber-500 text-slate-950 hover:bg-amber-400 transition-colors whitespace-nowrap shrink-0"
          >
            Vérifier les champs
          </button>
        </section>
      )}

      {/* Aperçu Héros + Dernière capture */}
      <section className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-7 p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-slate-100">Héros principaux visibles</h2>
            <button
              type="button"
              onClick={() => onNavigateTab('heros')}
              className="text-xs font-medium text-amber-400 hover:text-amber-300 whitespace-nowrap"
            >
              Tous les héros
            </button>
          </div>
          {topHeroes.length === 0 ? (
            <div className="py-8 text-center text-sm text-slate-400">
              Aucun héros extrait pour l’instant. Partagez une capture d’écran de vos héros ou
              utilisez « Analyser l’écran ».
            </div>
          ) : (
            <div className="divide-y divide-slate-800/80">
              {topHeroes.map((hero) => (
                <div key={hero.id} className="py-3 first:pt-0 last:pb-0 flex items-center justify-between gap-4">
                  <div className="min-w-0">
                    <div className="text-sm font-medium text-slate-100 truncate">
                      {hero.nom}
                      {hero.rarete ? ` · ${hero.rarete}` : ''}
                    </div>
                    <div className="text-xs text-slate-400 mt-0.5 truncate">
                      {hero.competences.length} compétence(s)
                      {hero.roleHero ? ` · ${hero.roleHero}` : ''}
                      {hero.incertain ? ' · Incertain' : ''}
                    </div>
                  </div>
                  <div className="text-sm font-mono-tabular font-semibold text-amber-300 whitespace-nowrap shrink-0">
                    {hero.niveauTexte}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="lg:col-span-5 p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-slate-100">Dernière analyse</h2>
            <button
              type="button"
              onClick={() => onNavigateTab('historique')}
              className="text-xs font-medium text-amber-400 hover:text-amber-300 whitespace-nowrap"
            >
              Historique complet
            </button>
          </div>
          {!latestAnalysis ? (
            <div className="py-8 text-center text-sm text-slate-400">
              Aucune analyse effectuée. Votre historique commencera dès votre première capture
              autorisée.
            </div>
          ) : (
            <div className="space-y-3">
              <div className="text-xs text-slate-400">
                {SCREEN_CATEGORY_LABELS[latestAnalysis.typeCapture]} ·{' '}
                {formatFrenchDateTime(latestAnalysis.analyzedAt)}
              </div>
              <p className="text-sm text-slate-200 leading-relaxed">
                {latestAnalysis.resumeCapture}
              </p>
              {latestAnalysis.champsIncertains.length > 0 && (
                <div className="text-xs text-amber-300">
                  {latestAnalysis.champsIncertains.length} élément(s) signalé(s) comme incertain(s)
                </div>
              )}
            </div>
          )}
        </div>
      </section>
    </div>
  );
};
