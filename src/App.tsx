/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useEffect, useRef, useState } from 'react';
import { readApiJson } from './utils/api';
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
  loadProfileCollection,
  saveProfileCollection,
  createEmptyAccountState,
  PlayerProfile,
  ProfileCollection,
  mergeAnalysisIntoState,
  saveAccountState,
  saveAdviceInState,
  updateLanguageInState,
  updatePlayerRoleInState,
} from './utils/storage';
import { DashboardView } from './components/DashboardView';
import { HeroesView } from './components/HeroesView';
import { AnalysisReviewModal } from './components/AnalysisReviewModal';
import { ResourcesPowerView } from './components/ResourcesPowerView';
import { AdviceView } from './components/AdviceView';
import { HistoryView } from './components/HistoryView';
import { ShareCaptureModal } from './components/ShareCaptureModal';
import { ScreenCaptureConsentModal } from './components/ScreenCaptureConsentModal';
import { AccountAccess } from './components/AccountAccess';
import { CloudUser, deleteCloudGameProfile, getSignedInUser, loadCloudGameProfiles, saveCloudGameProfiles, signOutCloud } from './utils/cloudSync';

type NavTab = 'tableau' | 'heros' | 'ressources' | 'conseils' | 'historique';

type FailedCapture = {
  id: string;
  payload: {
    imageBase64: string;
    mimeType: string;
    fileName: string;
    thumbnailDataUrl: string;
    note?: string;
  };
  source: 'share' | 'ocr_screen';
  error: string;
  createdAt: number;
};

const PENDING_CAPTURE_DB = 'loj-pending-captures';
const PENDING_CAPTURE_STORE = 'captures';

function openPendingCaptureDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(PENDING_CAPTURE_DB, 1);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(PENDING_CAPTURE_STORE)) {
        db.createObjectStore(PENDING_CAPTURE_STORE, { keyPath: 'id' });
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function getPendingCaptures(): Promise<FailedCapture[]> {
  const db = await openPendingCaptureDb();
  return new Promise((resolve, reject) => {
    const request = db.transaction(PENDING_CAPTURE_STORE, 'readonly')
      .objectStore(PENDING_CAPTURE_STORE).getAll();
    request.onsuccess = () => {
      db.close();
      resolve((request.result as FailedCapture[]).sort((a, b) => a.createdAt - b.createdAt));
    };
    request.onerror = () => {
      db.close();
      reject(request.error);
    };
  });
}

async function savePendingCapture(capture: FailedCapture): Promise<void> {
  const db = await openPendingCaptureDb();
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction(PENDING_CAPTURE_STORE, 'readwrite');
    tx.objectStore(PENDING_CAPTURE_STORE).put(capture);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
    tx.onabort = () => reject(tx.error);
  });
  db.close();

  // Conserve au maximum cinq captures en attente pour éviter d'encombrer le stockage du téléphone.
  const all = await getPendingCaptures();
  for (const oldCapture of all.slice(0, Math.max(0, all.length - 5))) {
    await removePendingCapture(oldCapture.id);
  }
}

async function removePendingCapture(id: string): Promise<void> {
  const db = await openPendingCaptureDb();
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction(PENDING_CAPTURE_STORE, 'readwrite');
    tx.objectStore(PENDING_CAPTURE_STORE).delete(id);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
    tx.onabort = () => reject(tx.error);
  });
  db.close();
}


const NAV_ICONS: Array<{ id: NavTab; icon: React.ComponentType<{ className?: string }> }> = [
  { id: 'tableau', icon: LayoutDashboard },
  { id: 'heros', icon: Users },
  { id: 'ressources', icon: Package },
  { id: 'conseils', icon: Swords },
  { id: 'historique', icon: History },
];

export default function App() {
  const [profileData, setProfileData] = useState<ProfileCollection>(() => loadProfileCollection());
  const [activeProfileId, setActiveProfileId] = useState(() => profileData.activeProfileId);
  const [state, setState] = useState<AccountState>(() =>
    profileData.profiles.find((profile) => profile.id === profileData.activeProfileId)?.state ?? loadAccountState()
  );
  const [activeTab, setActiveTab] = useState<NavTab>('tableau');
  const [authUser, setAuthUser] = useState<CloudUser | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [cloudReady, setCloudReady] = useState(false);
  const [cloudStatus, setCloudStatus] = useState('Connexion au compte…');
  const lastStateSnapshot = useRef(JSON.stringify(state));

  // Modales
  const [isShareModalOpen, setIsShareModalOpen] = useState(false);
  const [isConsentModalOpen, setIsConsentModalOpen] = useState(false);

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
  const [pendingAnalysis, setPendingAnalysis] = useState<ScreenshotAnalysis | null>(null);
  const [pendingAnalysisSource, setPendingAnalysisSource] = useState<'share' | 'ocr_screen' | null>(null);
  const [failedCaptures, setFailedCaptures] = useState<FailedCapture[]>([]);
  const [retryingCaptureId, setRetryingCaptureId] = useState<string | null>(null);

  useEffect(() => {
    void getPendingCaptures().then(setFailedCaptures).catch(() => {
      // Le stockage local peut être indisponible sur certains navigateurs : l'analyse reste utilisable.
    });
  }, []);

  useEffect(() => {
    let cancelled = false;
    void getSignedInUser().then((user) => {
      if (cancelled) return;
      setAuthUser(user);
      setAuthLoading(false);
      if (!user) setCloudStatus('Connecte-toi pour activer la synchronisation');
    });
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    if (!authUser) {
      setCloudReady(false);
      return;
    }
    let cancelled = false;
    const hydrateProfiles = async () => {
      setCloudStatus('Récupération des profils cloud…');
      try {
        const cloudRows = await loadCloudGameProfiles();
        if (cancelled) return;
        const local = loadProfileCollection();
        const localById = new Map(local.profiles.map((profile) => [profile.id, profile]));
        const mergedById = new Map<string, PlayerProfile>();

        for (const row of cloudRows) {
          const localProfile = localById.get(row.profile_id);
          const localTime = localProfile?.updatedAt ? Date.parse(localProfile.updatedAt) : 0;
          const cloudTime = Date.parse(row.updated_at);
          if (localProfile && localTime > cloudTime) {
            mergedById.set(localProfile.id, localProfile);
          } else {
            mergedById.set(row.profile_id, {
              id: row.profile_id,
              name: row.profile_name,
              state: { ...createEmptyAccountState(), ...(row.state as Partial<AccountState>) },
              updatedAt: row.updated_at,
            });
          }
          localById.delete(row.profile_id);
        }

        for (const localProfile of localById.values()) {
          mergedById.set(localProfile.id, {
            ...localProfile,
            updatedAt: localProfile.updatedAt ?? new Date().toISOString(),
          });
        }

        const mergedProfiles = Array.from(mergedById.values());
        const nextActiveId = mergedProfiles.some((profile) => profile.id === local.activeProfileId)
          ? local.activeProfileId
          : mergedProfiles[0]?.id;
        if (!nextActiveId) throw new Error('Aucun profil disponible.');
        const nextCollection = { profiles: mergedProfiles, activeProfileId: nextActiveId };
        setProfileData(nextCollection);
        setActiveProfileId(nextActiveId);
        setState(mergedProfiles.find((profile) => profile.id === nextActiveId)!.state);
        setCloudReady(true);
        setCloudStatus('Synchronisation active');
        try {
          await saveCloudGameProfiles(authUser.id, mergedProfiles.map((profile) => ({
            ...profile,
            state: profile.state as unknown as Record<string, unknown>,
          })));
          if (!cancelled) setCloudStatus('Profils synchronisés');
        } catch {
          if (!cancelled) setCloudStatus('Synchronisation en attente — données locales conservées');
        }
      } catch (error) {
        if (!cancelled) {
          setCloudReady(false);
          setCloudStatus('Cloud indisponible — données locales conservées');
          console.warn('Synchronisation cloud indisponible :', error);
        }
      }
    };
    void hydrateProfiles();
    return () => { cancelled = true; };
  }, [authUser?.id]);

  useEffect(() => {
    saveAccountState(state);
    const snapshot = JSON.stringify(state);
    const changed = lastStateSnapshot.current !== snapshot;
    if (changed) lastStateSnapshot.current = snapshot;

    const nextProfiles = profileData.profiles.map((profile) =>
      profile.id === activeProfileId
        ? { ...profile, state, ...(changed ? { updatedAt: new Date().toISOString() } : {}) }
        : profile
    );
    const nextCollection = { profiles: nextProfiles, activeProfileId };
    saveProfileCollection(nextCollection);
    if (changed) {
      setProfileData((current) => ({ ...current, profiles: nextProfiles, activeProfileId }));
    }
  }, [state, profileData, activeProfileId]);

  useEffect(() => {
    if (!authUser || !cloudReady) return;
    const profiles = profileData.profiles.map((profile) =>
      profile.id === activeProfileId ? { ...profile, state } : profile
    );
    const timer = window.setTimeout(() => {
      void saveCloudGameProfiles(authUser.id, profiles.map((profile) => ({
        ...profile,
        state: profile.state as unknown as Record<string, unknown>,
      }))).then(() => {
        setCloudStatus('Profils synchronisés');
      }).catch((error) => {
        console.warn('Sauvegarde cloud en attente :', error);
        setCloudStatus('Synchronisation en attente — données locales conservées');
      });
    }, 900);
    return () => window.clearTimeout(timer);
  }, [authUser, cloudReady, profileData, activeProfileId, state]);

  const handleSignOut = async () => {
    await signOutCloud();
    setAuthUser(null);
    setCloudReady(false);
    setCloudStatus('Déconnecté');
  };

  const handleSwitchProfile = (profileId: string) => {
    const target = profileData.profiles.find((profile) => profile.id === profileId);
    if (!target || target.id === activeProfileId) return;
    setProfileData((current) => ({
      ...current,
      profiles: current.profiles.map((profile) =>
        profile.id === activeProfileId ? { ...profile, state } : profile
      ),
    }));
    setActiveProfileId(target.id);
    setState(target.state);
    setActiveTab('tableau');
  };

  const handleRenameProfile = (profileId: string, name: string) => {
    const cleanName = name.trim().slice(0, 32);
    if (!cleanName) return;
    setProfileData((current) => ({
      ...current,
      profiles: current.profiles.map((profile) =>
        profile.id === profileId
          ? { ...profile, name: cleanName, updatedAt: new Date().toISOString() }
          : profile
      ),
    }));
  };

  const handleDeleteProfile = async (profileId: string) => {
    if (profileData.profiles.length <= 1) {
      showToast('Conserve au moins un profil de jeu.');
      return;
    }
    try {
      if (authUser && !cloudReady) {
        showToast('Connexion cloud requise pour supprimer sans risque de réapparition du profil.');
        return;
      }
      if (authUser && cloudReady) await deleteCloudGameProfile(profileId);
      const remaining = profileData.profiles.filter((profile) => profile.id !== profileId);
      const nextActive = profileId === activeProfileId ? remaining[0] : remaining.find((p) => p.id === activeProfileId)!;
      const nextCollection = { profiles: remaining, activeProfileId: nextActive.id };
      setProfileData(nextCollection);
      if (profileId === activeProfileId) {
        setActiveProfileId(nextActive.id);
        setState(nextActive.state);
      }
      showToast('Profil supprimé.');
    } catch (error) {
      setCloudStatus('Suppression cloud impossible — profil conservé');
      showToast(error instanceof Error ? error.message : 'Impossible de supprimer ce profil du cloud.');
    }
  };

  const handleCreateProfile = (name: string) => {
    const cleanName = name.trim().slice(0, 32);
    if (!cleanName) return;
    const newProfile: PlayerProfile = {
      id: `profile-${Date.now()}`,
      name: cleanName,
      state: createEmptyAccountState(state.language ?? 'fr', state.playerRole),
      updatedAt: new Date().toISOString(),
    };
    setProfileData((current) => ({
      ...current,
      profiles: [
        ...current.profiles.map((profile) =>
          profile.id === activeProfileId ? { ...profile, state } : profile
        ),
        newProfile,
      ],
    }));
    setActiveProfileId(newProfile.id);
    setState(newProfile.state);
    setActiveTab('tableau');
  };

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
          const data = await readApiJson<{
            base64: string;
            mimeType: string;
            fileName: string;
          }>(res, '/api/shared-capture');
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
    pendingId?: string,
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

      const resultJson = await readApiJson<{
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
          robots?: Array<{
            nom: string;
            niveau?: string;
            puissance?: string;
            rarete?: string;
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
      }>(response, '/api/analyze-screenshot');

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
        robots: (raw.robots ?? []).map((robot, idx) => ({
          id: `robot-${Date.now()}-${idx}`,
          nom: robot.nom,
          niveau: robot.niveau || undefined,
          puissance: robot.puissance || undefined,
          rarete: robot.rarete || undefined,
          incertain: Boolean(robot.incertain),
          raisonIncertitude: robot.raisonIncertitude || undefined,
          derniereMaj: analyzedAt,
          competences: (robot.competences ?? []).map((skill, skillIdx) => ({
            id: `robot-skill-${Date.now()}-${idx}-${skillIdx}`,
            nom: skill.nom,
            niveau: skill.niveau || 'Inconnu',
            description: skill.description || undefined,
            incertain: Boolean(skill.incertain),
            raisonIncertitude: skill.raisonIncertitude || undefined,
          })),
        })),
        champsIncertains: raw.champsIncertains ?? [],
        noteUtilisateur: payload.note,
      };

      setPendingAnalysis(newAnalysis);
      setPendingAnalysisSource(source);
      setSharedPayload(null);
      setIsShareModalOpen(false);
      setIsConsentModalOpen(false);

      const summary = `${SCREEN_CATEGORY_LABELS[typeCapture]} · ${newAnalysis.heros.length} héros, ${newAnalysis.ressources.length} ressource(s)`;
      showToast('Analyse terminée : vérifie les héros avant de confirmer.');
      if (pendingId) {
        await removePendingCapture(pendingId).catch(() => undefined);
        setFailedCaptures((current) => current.filter((item) => item.id !== pendingId));
      }
      return { summary };
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Erreur lors de l’analyse.';
      setAnalyzeError(msg);
      if (pendingId) {
        const existing = failedCaptures.find((item) => item.id === pendingId);
        if (existing) {
          const updated = { ...existing, error: msg };
          await savePendingCapture(updated).catch(() => undefined);
          setFailedCaptures((current) => current.map((item) => item.id === pendingId ? updated : item));
        }
      } else {
        const failed: FailedCapture = {
          id: `capture-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
          payload,
          source,
          error: msg,
          createdAt: Date.now(),
        };
        try {
          await savePendingCapture(failed);
          setFailedCaptures(await getPendingCaptures());
          showToast('Capture conservée sur cet appareil : tu pourras relancer son analyse.');
        } catch {
          showToast('Échec de l’analyse et sauvegarde locale indisponible. Garde la capture originale.');
        }
      }
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

  const handleConfirmPendingAnalysis = (analysis: ScreenshotAnalysis) => {
    setState((prev) => mergeAnalysisIntoState(prev, analysis));
    const source = pendingAnalysisSource;
    setPendingAnalysis(null);
    setPendingAnalysisSource(null);
    if (source === 'ocr_screen' && (analysis.typeCapture === 'trial_cage' || analysis.typeCapture === 'crystal_clash')) {
      setActiveTab('conseils');
    } else {
      setActiveTab('tableau');
    }
    showToast('Capture vérifiée et enregistrée dans ton profil.');
  };

  const handleDiscardPendingAnalysis = () => {
    setPendingAnalysis(null);
    setPendingAnalysisSource(null);
    showToast('Analyse ignorée : aucune donnée n’a été enregistrée.');
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

  if (authLoading) {
    return <div className="min-h-screen bg-[#0B0F17] text-slate-200 flex items-center justify-center text-sm">Chargement du compte Warden Coach…</div>;
  }
  if (!authUser) {
    return <AccountAccess onAuthenticated={(user) => {
      setAuthUser(user);
      setCloudStatus('Récupération des profils cloud…');
    }} />;
  }

  return (
    <div className="min-h-screen bg-[#0B0F17] text-slate-100 flex flex-col">
      {/* Barre supérieure (Top Bar Contract : 3 zones) */}
      <header className="sticky top-0 z-30 h-16 border-b border-slate-800/80 bg-[#0B0F17]/90 backdrop-blur-md">
        <div className="max-w-7xl mx-auto h-full px-4 md:px-6 flex items-center justify-between gap-8">
          {/* Zone 1 : Marque sur une seule ligne */}
          <button
            type="button"
            onClick={() => setActiveTab('tableau')}
            className="flex items-center gap-2.5 text-left whitespace-nowrap shrink-0 focus:outline-none"
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
            <span className="font-display text-base md:text-lg font-semibold tracking-tight text-slate-50 whitespace-nowrap shrink-0">
              Lands of Jail Assistant
            </span>
          </button>

          {/* Zone 2 : 5 liens texte simples sur bureau */}
          <nav className="hidden md:flex items-center gap-6" aria-label="Navigation principale">
            {navItems.map((item) => {
              const active = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => setActiveTab(item.id)}
                  className={`py-2 text-sm font-medium transition-colors border-b-2 whitespace-nowrap shrink-0 ${
                    active
                      ? 'text-amber-400 border-amber-400'
                      : 'text-slate-400 border-transparent hover:text-slate-100'
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
              className="min-h-[40px] px-3.5 md:px-4 py-2 rounded-xl text-xs md:text-sm font-semibold bg-amber-500 text-slate-950 hover:bg-amber-400 transition-colors flex items-center gap-2 whitespace-nowrap shrink-0"
            >
              <Upload className="w-4 h-4" />
              <span className="hidden sm:inline">{copy.shareCapture}</span>
              <span className="sm:hidden">{copy.captureShort}</span>
            </button>
            <button
              type="button"
              onClick={() => void handleSignOut()}
              className="ml-2 min-h-[40px] px-2.5 py-2 rounded-xl text-xs font-medium text-slate-400 hover:text-white border border-slate-800"
            >
              Déconnexion
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
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 md:px-6 py-6 pb-24 md:pb-12">
        {failedCaptures.length > 0 && (
          <section className="mb-5 rounded-2xl border border-amber-500/35 bg-amber-500/5 p-4 space-y-3" aria-label="Captures en attente">
            <div className="flex items-center justify-between gap-3">
              <div>
                <h2 className="text-sm font-semibold text-amber-200">Captures à réanalyser ({failedCaptures.length}/5)</h2>
                <p className="text-xs text-slate-400 mt-1">Elles sont conservées sur cet appareil, même après fermeture de l’application.</p>
              </div>
            </div>
            {failedCaptures.map((item) => (
              <div key={item.id} className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-t border-slate-700/60 pt-3">
                <div className="min-w-0">
                  <p className="text-xs font-medium text-slate-200 truncate">{item.payload.fileName}</p>
                  <p className="text-xs text-slate-400">{item.error}</p>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <button
                    type="button"
                    disabled={isAnalyzing || retryingCaptureId !== null}
                    onClick={async () => {
                      setRetryingCaptureId(item.id);
                      setAnalyzeError(null);
                      try {
                        await runAnalysis(item.payload, item.source, item.id);
                      } catch {
                        // L'erreur est affichée dans la file et la capture reste disponible.
                      } finally {
                        setRetryingCaptureId(null);
                      }
                    }}
                    className="min-h-[38px] px-3 py-2 rounded-lg bg-amber-500 text-slate-950 text-xs font-semibold disabled:opacity-50"
                  >
                    {retryingCaptureId === item.id ? 'Analyse…' : 'Réessayer'}
                  </button>
                  <button
                    type="button"
                    onClick={async () => {
                      await removePendingCapture(item.id).catch(() => undefined);
                      setFailedCaptures((current) => current.filter((capture) => capture.id !== item.id));
                    }}
                    className="min-h-[38px] px-3 py-2 rounded-lg bg-slate-800 text-slate-300 text-xs"
                  >
                    Supprimer
                  </button>
                </div>
              </div>
            ))}
          </section>
        )}

        {activeTab === 'tableau' && (
          <DashboardView
            state={state}
            profiles={profileData.profiles.map(({ id, name }) => ({ id, name }))}
            activeProfileId={activeProfileId}
            onSwitchProfile={handleSwitchProfile}
            onCreateProfile={handleCreateProfile}
            onRenameProfile={handleRenameProfile}
            onDeleteProfile={(profileId) => { void handleDeleteProfile(profileId); }}
            cloudStatus={cloudStatus}
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
            onUpdateForticladLevel={(level) => setState((prev) => ({ ...prev, forticladLevel: level }))}
            onUpdateForticladBuildings={(progress) => setState((prev) => ({ ...prev, forticladBuildings: progress }))}
            onUpdateForticladLabProgress={(progress) => setState((prev) => ({ ...prev, forticladLabProgress: progress }))}
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
            robots={state.robots ?? []}
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
        className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-[#0B0F17]/95 backdrop-blur-md border-t border-slate-800 pb-safe"
        aria-label="Navigation mobile"
      >
        <div className="grid grid-cols-5 h-16 items-center">
          {navItems.map((item) => {
            const Icon = item.icon;
            const active = activeTab === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => setActiveTab(item.id)}
                className={`min-h-[44px] flex flex-col items-center justify-center gap-1 transition-colors ${
                  active ? 'text-amber-400' : 'text-slate-400 hover:text-slate-200'
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

      {pendingAnalysis && (
        <AnalysisReviewModal
          key={pendingAnalysis.id}
          analysis={pendingAnalysis}
          onConfirm={handleConfirmPendingAnalysis}
          onDiscard={handleDiscardPendingAnalysis}
        />
      )}

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
        onAnalyze={(payload) => runAnalysis(payload, 'share')}
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

    </div>
  );
}
