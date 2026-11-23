import React, { useState } from 'react';
import {
  AlertTriangle,
  Camera,
  Eye,
  EyeOff,
  Globe,
  PowerOff,
  Radio,
  Settings,
  ShieldCheck,
  SlidersHorizontal,
  Swords,
  Upload,
} from 'lucide-react';
import {
  APP_LANGUAGE_LABELS,
  AccountState,
  AppLanguage,
  HeroRecord,
  PLAYER_ROLE_LABELS,
  PlayerRole,
  SCREEN_CATEGORY_LABELS,
} from '../types/game';
import { UI_COPY } from '../i18n/translations';
import { formatFrenchDateTime } from '../utils/storage';

interface DashboardViewProps {
  state: AccountState;
  profiles: Array<{ id: string; name: string }>;
  activeProfileId: string;
  onSwitchProfile: (profileId: string) => void;
  onCreateProfile: (name: string) => void;
  cloudStatus: string;
  language: AppLanguage;
  onChangeLanguage: (lang: AppLanguage) => void;
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
}

const LANGUAGES_ORDER: AppLanguage[] = ['fr', 'tr', 'ru', 'zh', 'de', 'en'];

export const DashboardView: React.FC<DashboardViewProps> = ({
  state,
  profiles,
  activeProfileId,
  onSwitchProfile,
  onCreateProfile,
  cloudStatus,
  language,
  onChangeLanguage,
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
}) => {
  const [showCaptureSettings, setShowCaptureSettings] = useState(false);
  const [newProfileName, setNewProfileName] = useState('');
  const copy = UI_COPY[language];

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

  const showPermissionPanel = !hasUsagePermission || showCaptureSettings;

  return (
    <div className="space-y-8">
      {/* En-tête synthétique + Sélecteur de langue */}
      <section className="flex flex-col lg:flex-row lg:items-end justify-between gap-4 border-b border-slate-800/80 pb-6">
        <div className="space-y-2 max-w-2xl">
          <div className="flex flex-wrap items-center gap-2">
            <label htmlFor="active-game-profile" className="text-xs text-slate-400">Profil de jeu</label>
            <select
              id="active-game-profile"
              value={activeProfileId}
              onChange={(event) => onSwitchProfile(event.target.value)}
              className="min-h-[36px] max-w-[190px] rounded-lg bg-slate-900 border border-slate-700 px-2.5 text-xs text-slate-100"
            >
              {profiles.map((profile) => (
                <option key={profile.id} value={profile.id}>{profile.name}</option>
              ))}
            </select>
            <input
              aria-label="Nom du nouveau profil"
              value={newProfileName}
              onChange={(event) => setNewProfileName(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' && newProfileName.trim()) {
                  onCreateProfile(newProfileName);
                  setNewProfileName('');
                }
              }}
              placeholder="Nouveau compte…"
              maxLength={32}
              className="min-h-[36px] w-36 rounded-lg bg-slate-900 border border-slate-700 px-2.5 text-xs text-slate-100 placeholder:text-slate-500"
            />
            <button
              type="button"
              disabled={!newProfileName.trim()}
              onClick={() => {
                onCreateProfile(newProfileName);
                setNewProfileName('');
              }}
              className="min-h-[36px] px-3 rounded-lg bg-slate-800 border border-slate-700 text-xs font-semibold text-slate-200 disabled:opacity-40"
            >
              + Ajouter
            </button>
          </div>
          <p className="mt-2 text-[11px] text-slate-500">Synchronisation : {cloudStatus}</p>
          <h1 className="text-2xl md:text-3xl font-semibold tracking-tight text-slate-50">
            {copy.dashboardTitle}
          </h1>
          <p className="text-sm text-slate-400 leading-relaxed">{copy.dashboardSubtitle}</p>
          <div className="flex flex-wrap items-center gap-1.5 pt-1">
            <Globe className="w-3.5 h-3.5 text-amber-400 shrink-0" />
            <span className="text-xs text-slate-400 mr-1">{copy.languageLabel} :</span>
            {LANGUAGES_ORDER.map((langCode) => {
              const active = language === langCode;
              return (
                <button
                  key={langCode}
                  type="button"
                  onClick={() => onChangeLanguage(langCode)}
                  className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-colors ${
                    active
                      ? 'bg-amber-500 text-slate-950'
                      : 'bg-slate-900 border border-slate-800 text-slate-300 hover:border-slate-700'
                  }`}
                >
                  {langCode.toUpperCase()} · {APP_LANGUAGE_LABELS[langCode]}
                </button>
              );
            })}
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-3 shrink-0">
          <button
            type="button"
            onClick={onOpenShareModal}
            className="min-h-[44px] px-4 py-2 rounded-xl text-sm font-semibold bg-amber-500 text-slate-950 hover:bg-amber-400 transition-colors flex items-center gap-2 whitespace-nowrap shrink-0"
          >
            <Upload className="w-4 h-4" />
            <span>{copy.shareCapture}</span>
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
            {totalUncertainCount > 0
              ? 'Héros et ressources à vérifier'
              : 'Toutes les données vérifiées'}
          </div>
        </button>
      </section>

      {/* Grille principale : Contrôle compact (ou autorisations si manquantes) + Rôle joueur */}
      <section className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Colonne gauche */}
        <div className="lg:col-span-7 p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-4">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-100">
                {hasUsagePermission
                  ? 'Capture OCR & Détection au-dessus du jeu'
                  : 'Détection de lancement (UsageStatsManager)'}
              </h2>
              <p className="text-xs text-slate-400 mt-0.5">
                {isDetectionActive
                  ? 'Détection active · Appuyez sur « Analyser l’écran » quand le jeu est ouvert.'
                  : hasUsagePermission
                    ? 'Autorisations validées · Prêt pour analyser votre jeu.'
                    : 'Activez l’autorisation une seule fois : cette section se réduira automatiquement.'}
              </p>
            </div>
            {hasUsagePermission && (
              <button
                type="button"
                onClick={() => setShowCaptureSettings((v) => !v)}
                aria-label="Réglages des autorisations"
                className="min-h-[40px] px-3 py-1.5 rounded-xl text-xs font-medium bg-slate-900 border border-slate-800 text-slate-300 hover:text-slate-100 flex items-center gap-1.5 shrink-0"
              >
                <Settings className="w-3.5 h-3.5" />
                <span>{showCaptureSettings ? 'Masquer' : 'Réglages'}</span>
              </button>
            )}
          </div>

          {/* Panneau des autorisations masqué automatiquement une fois accordées */}
          {showPermissionPanel && (
            <div className="space-y-3 pt-1">
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
            </div>
          )}

          {/* Boutons d'action principaux toujours accessibles */}
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

          {!isDetectionActive && !hasUsagePermission && (
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
            Adapte les recommandations pour tous les événements à votre style de jeu.
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
                <div
                  key={hero.id}
                  className="py-3 first:pt-0 last:pb-0 flex items-center justify-between gap-4"
                >
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
              {latestAnalysis.thumbnailDataUrl && (
                <img
                  src={latestAnalysis.thumbnailDataUrl}
                  alt={latestAnalysis.fileName}
                  className="w-full h-36 object-cover rounded-xl border border-slate-800 bg-slate-950"
                />
              )}
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
