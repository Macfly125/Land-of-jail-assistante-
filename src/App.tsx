/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useEffect, useState } from 'react';
import {
  Camera,
  History,
  LayoutDashboard,
  Package,
  PowerOff,
  Shield,
  Swords,
  Upload,
  Users,
} from 'lucide-react';
import {
  AccountState,
  AdviceMode,
  AppLanguage,
  HeroRecord,
  PlayerRole,
  SCREEN_CATEGORY_LABELS,
  ScreenCategory,
  ScreenshotAnalysis,
  TacticalAdviceResult,
} from './types/game';
import { UI_COPY } from './i18n/translations';
import {
  loadAccountState,
  mergeAnalysisIntoState,
  saveAccountState,
  saveAdviceInState,
  updateLanguageInState,
  updatePlayerRoleInState,
} from './utils/storage';
import { DashboardView } from './components/DashboardView';
import { HeroesView } from './components/HeroesView';
import { ResourcesPowerView } from './components/ResourcesPowerView';
import { AdviceView } from './components/AdviceView';
import { HistoryView } from './components/HistoryView';
import { ShareCaptureModal } from './components/ShareCaptureModal';
import { ScreenCaptureConsentModal } from './components/ScreenCaptureConsentModal';
import { AndroidKotlinModal } from './components/AndroidKotlinModal';
import { MobileInstallGuideModal } from './components/MobileInstallGuideModal';

type NavTab = 'tableau' | 'heros' | 'ressources' | 'conseils' | 'historique';

const NAV_ICONS: Array<{ id: NavTab; icon: React.ComponentType<{ className?: string }> }> = [
  { id: 'tableau', icon: LayoutDashboard },
  { id: 'heros', icon: Users },
  { id: 'ressources', icon: Package },
  { id: 'conseils', icon: Swords },
  { id: 'historique', icon: History },
];

export default function App() {
  const [state, setState] = useState<AccountState>(() => loadAccountState());
  const [activeTab, setActiveTab] = useState<NavTab>('tableau');

  // Modales
  const [isShareModalOpen, setIsShareModalOpen] = useState(false);
  const [isConsentModalOpen, setIsConsentModalOpen] = useState(false);
  const [isAndroidCodeOpen, setIsAndroidCodeOpen] = useState(false);
  const [isMobileGuideOpen, setIsMobileGuideOpen] = useState(false);

  // Détection de premier plan (modèle UsageStatsManager)
  const [hasUsagePermission, setHasUsagePermission] = useState(false);
  const [isDetectionActive, setIsDetectionActive] = useState(false);
  const [isGameDetectedForeground, setIsGameDetectedForeground] = useState(false);
  const [packageFilter, setPackageFilter] = useState('com.justgame.jails');

  // État d'analyse OCR / Vision
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [analyzeError, setAnalyzeError] = useState<string | null>(null);
  const [sharedPayload, setSharedPayload] = useState<{
    base64: string;
    mimeType: string;
    fileName: string;
  } | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [crestFailed, setCrestFailed] = useState(false);

  useEffect(() => {
    saveAccountState(state);
  }, [state]);

  // Récupération éventuelle d'une capture envoyée via PWA Web Share Target (/api/share-target)
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const sharedToken = params.get('sharedToken');
    if (sharedToken) {
      window.history.replaceState({}, '', '/');
      void (async () => {
        try {
          const res = await fetch(`/api/shared-capture/${encodeURIComponent(sharedToken)}`);
          if (!res.ok) return;
          const data = (await res.json()) as {
            base64: string;
            mimeType: string;
            fileName: string;
          };
          setSharedPayload(data);
          setIsShareModalOpen(true);
        } catch {
          // ignore
        }
      })();
    }
  }, []);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage((prev) => (prev === msg ? null : prev));
    }, 4500);
  };

  const runAnalysis = async (
    payload: {
      imageBase64: string;
      mimeType: string;
      fileName: string;
      thumbnailDataUrl: string;
      note?: string;
    },
    source: 'share' | 'ocr_screen',
    options?: { keepModalOpen?: boolean }
  ): Promise<{ summary: string }> => {
    setIsAnalyzing(true);
    setAnalyzeError(null);

    try {
      const response = await fetch('/api/analyze-screenshot', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          imageBase64: payload.imageBase64,
          mimeType: payload.mimeType,
          playerRole: state.playerRole,
          note: payload.note,
        }),
      });

      const resultJson = (await response.json()) as {
        analysis?: {
          typeCapture?: string;
          resumeCapture?: string;
          puissance?: {
            presente?: boolean;
            puissanceTotaleTexte?: string;
            puissanceTotaleNumerique?: number;
            niveauPrisonOuQG?: string;
            niveauVip?: string;
            incertain?: boolean;
            raisonIncertitude?: string;
          };
          ressources?: Array<{
            nom: string;
            quantiteTexte: string;
            quantiteNumerique?: number;
            unite?: string;
            incertain: boolean;
            raisonIncertitude?: string;
          }>;
          heros?: Array<{
            nom: string;
            niveau?: number;
            niveauTexte: string;
            rarete?: string;
            etoiles?: string;
            roleHero?: string;
            puissanceHero?: string;
            incertain: boolean;
            raisonIncertitude?: string;
            competences?: Array<{
              nom: string;
              niveau: string;
              description?: string;
              incertain: boolean;
              raisonIncertitude?: string;
            }>;
          }>;
          champsIncertains?: Array<{
            champ: string;
            valeurObservee: string;
            raison: string;
          }>;
        };
        analyzedAt?: string;
        error?: string;
      };

      if (!response.ok || !resultJson.analysis) {
        throw new Error(resultJson.error || 'Échec de l’analyse de la capture.');
      }

      const raw = resultJson.analysis;
      const analyzedAt = resultJson.analyzedAt || new Date().toISOString();
      const validCategories: ScreenCategory[] = [
        'profil',
        'heros',
        'ressources',
        'trial_cage',
        'crystal_clash',
        'sandsea_conquest',
        'svs',
        'turmoil_surge',
        'gold_hunter',
        'autre',
      ];
      const typeCapture: ScreenCategory = validCategories.includes(raw.typeCapture as ScreenCategory)
        ? (raw.typeCapture as ScreenCategory)
        : 'autre';

      const newAnalysis: ScreenshotAnalysis = {
        id: `ana-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
        analyzedAt,
        fileName: payload.fileName,
        thumbnailDataUrl: payload.thumbnailDataUrl,
        typeCapture,
        resumeCapture: raw.resumeCapture || 'Capture analysée.',
        puissance:
          raw.puissance && raw.puissance.presente && raw.puissance.puissanceTotaleTexte
            ? {
                puissanceTotaleTexte: raw.puissance.puissanceTotaleTexte,
                puissanceTotaleNumerique:
                  typeof raw.puissance.puissanceTotaleNumerique === 'number'
                    ? raw.puissance.puissanceTotaleNumerique
                    : null,
                niveauPrisonOuQG: raw.puissance.niveauPrisonOuQG || undefined,
                niveauVip: raw.puissance.niveauVip || undefined,
                incertain: Boolean(raw.puissance.incertain),
                raisonIncertitude: raw.puissance.raisonIncertitude || undefined,
              }
            : null,
        ressources: (raw.ressources ?? []).map((r, idx) => ({
          id: `res-${Date.now()}-${idx}`,
          nom: r.nom,
          quantiteTexte: r.quantiteTexte,
          quantiteNumerique:
            typeof r.quantiteNumerique === 'number' ? r.quantiteNumerique : null,
          unite: r.unite || undefined,
          incertain: Boolean(r.incertain),
          raisonIncertitude: r.raisonIncertitude || undefined,
          derniereMaj: analyzedAt,
        })),
        heros: (raw.heros ?? []).map((h, idx) => ({
          id: `hero-${Date.now()}-${idx}`,
          nom: h.nom,
          niveau: typeof h.niveau === 'number' ? h.niveau : null,
          niveauTexte: h.niveauTexte || (typeof h.niveau === 'number' ? `Niv. ${h.niveau}` : 'Inconnu'),
          rarete: h.rarete || undefined,
          etoiles: h.etoiles || undefined,
          roleHero: h.roleHero || undefined,
          puissanceHero: h.puissanceHero || undefined,
          incertain: Boolean(h.incertain),
          raisonIncertitude: h.raisonIncertitude || undefined,
          derniereMaj: analyzedAt,
          competences: (h.competences ?? []).map((s, sIdx) => ({
            id: `skill-${Date.now()}-${idx}-${sIdx}`,
            nom: s.nom,
            niveau: s.niveau || 'Inconnu',
            description: s.description || undefined,
            incertain: Boolean(s.incertain),
            raisonIncertitude: s.raisonIncertitude || undefined,
          })),
        })),
        champsIncertains: raw.champsIncertains ?? [],
        noteUtilisateur: payload.note,
      };

      setState((prev) => mergeAnalysisIntoState(prev, newAnalysis));
      setSharedPayload(null);

      const summary = `${SCREEN_CATEGORY_LABELS[typeCapture]} · ${newAnalysis.heros.length} héros, ${newAnalysis.ressources.length} ressource(s)`;

      if (!options?.keepModalOpen) {
        setIsShareModalOpen(false);
        setIsConsentModalOpen(false);
        if (source === 'ocr_screen' && (typeCapture === 'trial_cage' || typeCapture === 'crystal_clash')) {
          setActiveTab('conseils');
        } else {
          setActiveTab('tableau');
        }
      }

      showToast(`Analyse terminée (${summary})`);
      return { summary };
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Erreur lors de l’analyse.';
      setAnalyzeError(msg);
      throw err;
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleToggleUsagePermission = () => {
    setHasUsagePermission((prev) => {
      const next = !prev;
      if (!next) {
        setIsDetectionActive(false);
        setIsGameDetectedForeground(false);
      }
      return next;
    });
  };

  const handleStartDetection = () => {
    if (!hasUsagePermission) return;
    setIsDetectionActive(true);
    showToast('Détection de lancement activée (UsageStatsManager).');
  };

  const handleStopDetection = () => {
    setIsDetectionActive(false);
    setIsGameDetectedForeground(false);
    showToast('Détection arrêtée.');
  };

  const handleSimulateGameLaunch = () => {
    if (!isDetectionActive) return;
    setIsGameDetectedForeground(true);
    showToast('Lands of Jail détecté au premier plan : bouton « Analyser l’écran » affiché.');
  };

  const handleVerifyHero = (heroId: string, updated: Partial<HeroRecord>) => {
    setState((prev) => ({
      ...prev,
      heroes: prev.heroes.map((h) => (h.id === heroId ? { ...h, ...updated } : h)),
    }));
  };

  const handleVerifyResource = (resourceId: string, newQuantityText: string) => {
    setState((prev) => ({
      ...prev,
      resources: prev.resources.map((r) =>
        r.id === resourceId
          ? {
              ...r,
              quantiteTexte: newQuantityText,
              incertain: false,
              raisonIncertitude: undefined,
              verifieManuellement: true,
            }
          : r
      ),
    }));
  };

  const handleDeleteAnalysis = (analysisId: string) => {
    setState((prev) => ({
      ...prev,
      analyses: prev.analyses.filter((a) => a.id !== analysisId),
    }));
  };

  const handleClearAllAnalyses = () => {
    setState((prev) => ({
      ...prev,
      analyses: [],
    }));
    showToast('Historique effacé.');
  };

  const language: AppLanguage = state.language ?? 'fr';
  const copy = UI_COPY[language];
  const navItems = NAV_ICONS.map((item) => ({
    ...item,
    label:
      item.id === 'tableau'
        ? copy.navDashboard
        : item.id === 'heros'
          ? copy.navHeroes
          : item.id === 'ressources'
            ? copy.navResources
            : item.id === 'conseils'
              ? copy.navAdvice
              : copy.navHistory,
  }));

  return (
    <div className="app-shell min-h-screen text-slate-100 flex flex-col">
      {/* Barre supérieure (Top Bar Contract : 3 zones) */}
      <header className="app-header sticky top-0 z-30 min-h-16 border-b border-slate-800/80 backdrop-blur-xl">
        <div className="max-w-7xl mx-auto min-h-16 px-4 md:px-6 flex items-center justify-between gap-4 md:gap-8">
          {/* Zone 1 : Marque sur une seule ligne */}
          <button
            type="button"
            onClick={() => setActiveTab('tableau')}
            className="app-brand flex items-center gap-3 text-left whitespace-nowrap shrink-0 focus:outline-none"
          >
            {!crestFailed ? (
              <img
                src="/src/assets/images/app_crest_1795282256452.jpg"
                alt="Emblème Lands of Jail Assistant"
                referrerPolicy="no-referrer"
                onError={() => setCrestFailed(true)}
                className="w-8 h-8 rounded-lg object-cover border border-slate-700/80"
              />
            ) : (
              <div className="w-8 h-8 rounded-lg bg-amber-500/15 border border-amber-500/40 flex items-center justify-center text-amber-400">
                <Shield className="w-4 h-4" />
              </div>
            )}
            <span className="flex flex-col items-start leading-tight">
              <span className="font-display text-sm md:text-base font-bold tracking-tight text-slate-50 whitespace-nowrap shrink-0">Lands of Jail</span>
              <span className="text-[9px] md:text-[10px] uppercase tracking-[0.2em] text-amber-400/90 font-semibold">Assistant tactique</span>
            </span>
          </button>

          {/* Zone 2 : 5 liens texte simples sur bureau */}
          <nav className="app-desktop-nav hidden md:flex items-center gap-1 p-1 rounded-2xl" aria-label="Navigation principale">
            {navItems.map((item) => {
              const active = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => setActiveTab(item.id)}
                  className={`nav-pill px-3.5 py-2 rounded-xl text-sm font-semibold transition-all whitespace-nowrap shrink-0 ${
                    active
                      ? 'is-active text-amber-300'
                      : 'text-slate-400 hover:text-slate-100 hover:bg-white/[0.04]'
                  }`}
                >
                  {item.label}
                </button>
              );
            })}
          </nav>

          {/* Zone 3 : 1 action principale */}
          <div className="shrink-0">
            <button
              type="button"
              onClick={() => {
                setAnalyzeError(null);
                setIsShareModalOpen(true);
              }}
              className="app-primary-action min-h-[42px] px-3.5 md:px-4 py-2 rounded-xl text-xs md:text-sm font-bold text-slate-950 transition-all flex items-center gap-2 whitespace-nowrap shrink-0"
            >
              <Upload className="w-4 h-4" />
              <span className="hidden sm:inline">{copy.shareCapture}</span>
              <span className="sm:hidden">{copy.captureShort}</span>
            </button>
          </div>
        </div>
      </header>

      {/* Notification / Bouton flottant « Analyser l’écran » quand Lands of Jail passe au premier plan */}
      {isDetectionActive && isGameDetectedForeground && (
        <div className="bg-[#161E2E] border-b border-amber-500/35">
          <div className="max-w-7xl mx-auto px-4 md:px-6 py-3 flex flex-wrap items-center justify-between gap-3">
            <div className="text-xs md:text-sm text-slate-200">
              <span className="font-semibold text-amber-300">Lands of Jail au premier plan</span>
              <span className="text-slate-400">
                {' '}
                · Appuyez sur « Analyser l’écran » pour demander l’autorisation de capture OCR.
              </span>
            </div>
            <div className="flex items-center gap-2 shrink-0">
              <button
                type="button"
                onClick={() => setIsConsentModalOpen(true)}
                className="min-h-[40px] px-4 py-1.5 rounded-xl text-xs font-semibold bg-amber-500 text-slate-950 hover:bg-amber-400 transition-colors flex items-center gap-1.5 whitespace-nowrap"
              >
                <Camera className="w-3.5 h-3.5" />
                <span>Analyser l’écran</span>
              </button>
              <button
                type="button"
                onClick={handleStopDetection}
                className="min-h-[40px] px-3 py-1.5 rounded-xl text-xs font-medium bg-slate-800 text-slate-200 hover:bg-slate-700 transition-colors flex items-center gap-1.5 whitespace-nowrap"
              >
                <PowerOff className="w-3.5 h-3.5 text-red-400" />
                <span>Arrêter la détection</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Message de retour discret */}
      {toastMessage && (
        <div className="max-w-7xl w-full mx-auto px-4 md:px-6 pt-4">
          <div className="px-4 py-2.5 rounded-xl bg-slate-900 border border-slate-700 text-xs text-slate-200 flex items-center justify-between gap-3">
            <span>{toastMessage}</span>
            <button
              type="button"
              onClick={() => setToastMessage(null)}
              className="text-slate-400 hover:text-slate-100 text-xs"
            >
              Fermer
            </button>
          </div>
        </div>
      )}

      {/* Contenu principal */}
      <main className="page-frame flex-1 max-w-7xl w-full mx-auto px-4 md:px-6 py-6 md:py-8 pb-28 md:pb-14">
        {activeTab === 'tableau' && (
          <DashboardView
            state={state}
            language={language}
            onChangeLanguage={(lang) => setState((prev) => updateLanguageInState(prev, lang))}
            hasUsagePermission={hasUsagePermission}
            isDetectionActive={isDetectionActive}
            isGameDetectedForeground={isGameDetectedForeground}
            packageFilter={packageFilter}
            onChangePackageFilter={setPackageFilter}
            onToggleUsagePermission={handleToggleUsagePermission}
            onStartDetection={handleStartDetection}
            onStopDetection={handleStopDetection}
            onSimulateGameLaunch={handleSimulateGameLaunch}
            onOpenAnalyzeScreenPrompt={() => setIsConsentModalOpen(true)}
            onOpenShareModal={() => {
              setAnalyzeError(null);
              setIsShareModalOpen(true);
            }}
            onChangePlayerRole={(role: PlayerRole) =>
              setState((prev) => updatePlayerRoleInState(prev, role))
            }
            onNavigateTab={setActiveTab}
            onOpenAndroidCode={() => setIsAndroidCodeOpen(true)}
            onOpenMobileGuide={() => setIsMobileGuideOpen(true)}
          />
        )}

        {activeTab === 'heros' && (
          <HeroesView
            heroes={state.heroes}
            onVerifyHero={handleVerifyHero}
            onOpenShareModal={() => {
              setAnalyzeError(null);
              setIsShareModalOpen(true);
            }}
          />
        )}

        {activeTab === 'ressources' && (
          <ResourcesPowerView
            state={state}
            onVerifyResource={handleVerifyResource}
            onOpenShareModal={() => {
              setAnalyzeError(null);
              setIsShareModalOpen(true);
            }}
          />
        )}

        {activeTab === 'conseils' && (
          <AdviceView
            language={language}
            onChangeLanguage={(lang) => setState((prev) => updateLanguageInState(prev, lang))}
            playerRole={state.playerRole}
            onChangePlayerRole={(role: PlayerRole) =>
              setState((prev) => updatePlayerRoleInState(prev, role))
            }
            heroes={state.heroes}
            powerText={state.currentPower?.puissanceTotaleTexte}
            adviceByMode={state.adviceByMode}
            onSaveAdvice={(mode: AdviceMode, advice: TacticalAdviceResult) =>
              setState((prev) => saveAdviceInState(prev, mode, advice))
            }
          />
        )}

        {activeTab === 'historique' && (
          <HistoryView
            analyses={state.analyses}
            onDeleteAnalysis={handleDeleteAnalysis}
            onClearAllAnalyses={handleClearAllAnalyses}
            onOpenShareModal={() => {
              setAnalyzeError(null);
              setIsShareModalOpen(true);
            }}
          />
        )}
      </main>

      {/* Barre de navigation inférieure sur mobile (zone du pouce) */}
      <nav
        className="mobile-nav md:hidden fixed bottom-0 left-0 right-0 z-40 backdrop-blur-xl border-t border-slate-800/80 pb-safe"
        aria-label="Navigation mobile"
      >
        <div className="grid grid-cols-5 max-w-xl mx-auto h-[68px] items-center px-2">
          {navItems.map((item) => {
            const Icon = item.icon;
            const active = activeTab === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => setActiveTab(item.id)}
                className={`mobile-nav-item min-h-[48px] mx-0.5 rounded-xl flex flex-col items-center justify-center gap-1 transition-all ${
                  active ? 'is-active text-amber-300' : 'text-slate-500 hover:text-slate-200'
                }`}
              >
                <Icon className="w-5 h-5" />
                <span className="text-[10px] font-medium tracking-tight whitespace-nowrap">
                  {item.label}
                </span>
              </button>
            );
          })}
        </div>
      </nav>

      {/* Modale de partage / import manuel de capture */}
      <ShareCaptureModal
        isOpen={isShareModalOpen}
        onClose={() => {
          if (!isAnalyzing) {
            setIsShareModalOpen(false);
            setSharedPayload(null);
          }
        }}
        playerRole={state.playerRole}
        initialBase64={sharedPayload}
        isAnalyzing={isAnalyzing}
        error={analyzeError}
        onAnalyze={(payload) => runAnalysis(payload, 'share', { keepModalOpen: true })}
      />

      {/* Modale d'autorisation système de capture d'écran (MediaProjection / DisplayMedia) */}
      <ScreenCaptureConsentModal
        isOpen={isConsentModalOpen}
        onClose={() => {
          if (!isAnalyzing) setIsConsentModalOpen(false);
        }}
        isAnalyzing={isAnalyzing}
        onCapturedImage={async (payload) => {
          await runAnalysis(payload, 'ocr_screen');
        }}
        onFallbackToFileUpload={() => {
          setAnalyzeError(null);
          setIsShareModalOpen(true);
        }}
      />

      {/* Modale des sources natives Android Kotlin & Jetpack Compose */}
      <AndroidKotlinModal
        isOpen={isAndroidCodeOpen}
        onClose={() => setIsAndroidCodeOpen(false)}
      />

      {/* Guide pas à pas illustré sur téléphone sans PC */}
      <MobileInstallGuideModal
        isOpen={isMobileGuideOpen}
        onClose={() => setIsMobileGuideOpen(false)}
        onOpenAndroidCode={() => setIsAndroidCodeOpen(true)}
      />
    </div>
  );
}
