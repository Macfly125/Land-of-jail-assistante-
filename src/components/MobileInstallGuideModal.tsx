import React, { useState } from 'react';
import { BookOpen, CheckCircle2, Smartphone, X } from 'lucide-react';

interface MobileInstallGuideModalProps {
  isOpen: boolean;
  onClose: () => void;
  onOpenAndroidCode: () => void;
}

interface GuideStep {
  id: string;
  title: string;
  subtitle: string;
  imagePath: string;
  imageAlt: string;
  points: string[];
}

const GUIDE_STEPS: GuideStep[] = [
  {
    id: 'pwa',
    title: 'Option A — Installer tout de suite sans APK (Chrome Android)',
    subtitle: 'Idéal pour analyser vos captures et consulter vos conseils immédiatement.',
    imagePath: '/src/assets/images/guide_step1_pwa_1795289217529.jpg',
    imageAlt: 'Installer l’application depuis le menu trois points de Chrome sur Android',
    points: [
      '1. Ouvrez l’URL de l’application dans Chrome sur votre téléphone Android.',
      '2. Appuyez sur le menu ⋮ en haut à droite de Chrome.',
      '3. Appuyez sur « Installer l’application » (ou « Ajouter à l’écran d’accueil »).',
      '4. Lancez l’icône depuis votre écran d’accueil puis utilisez « Partager une capture ».',
    ],
  },
  {
    id: 'github',
    title: 'Option B (Étape 1) — Compiler l’APK sur GitHub depuis votre téléphone',
    subtitle: 'Permet de générer app-debug.apk dans le cloud gratuitement sans aucun PC.',
    imagePath: '/src/assets/images/guide_step2_github_apk_1795289218228.jpg',
    imageAlt: 'Exporter sur GitHub et lancer le workflow Compiler APK Android dans Actions',
    points: [
      '1. Dans AI Studio, exportez ce projet vers votre compte GitHub.',
      '2. Ouvrez votre dépôt sur github.com dans le navigateur de votre téléphone.',
      '3. Ouvrez l’onglet Actions → « Compiler APK Android » → appuyez sur « Run workflow ».',
      '4. Une fois le voyant vert apparu, téléchargez l’artefact « lands-of-jail-assistant-debug-apk ».',
    ],
  },
  {
    id: 'android',
    title: 'Option B (Étape 2) — Installer l’APK et activer UsageStatsManager',
    subtitle: 'Débloque la détection au premier plan et le bouton « Analyser l’écran ».',
    imagePath: '/src/assets/images/guide_step3_android_install_1795289218572.jpg',
    imageAlt: 'Installer app-debug.apk, activer Usage Access et appuyer sur Analyser l’écran',
    points: [
      '1. Décompressez le fichier .zip dans l’app Fichiers de votre téléphone et ouvrez app-debug.apk.',
      '2. Autorisez l’installation depuis cette source si Android le demande.',
      '3. Dans Lands of Jail Assistant, appuyez sur « Activer dans les paramètres » et activez l’accès aux statistiques d’utilisation.',
      '4. Lancez le jeu : appuyez sur « Analyser l’écran » puis acceptez la boîte de dialogue système de capture.',
    ],
  },
];

export const MobileInstallGuideModal: React.FC<MobileInstallGuideModalProps> = ({
  isOpen,
  onClose,
  onOpenAndroidCode,
}) => {
  const [failedImages, setFailedImages] = useState<Record<string, boolean>>({});

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-end md:items-center justify-center bg-black/75 backdrop-blur-sm p-0 md:p-6"
      role="dialog"
      aria-modal="true"
      aria-labelledby="mobile-guide-title"
    >
      <div className="w-full md:max-w-4xl bg-[#121826] border border-slate-800 rounded-t-3xl md:rounded-2xl shadow-2xl max-h-[90vh] flex flex-col overflow-hidden">
        <div className="md:hidden w-10 h-1.5 bg-slate-700 rounded-full mx-auto mt-3 shrink-0" />

        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-800">
          <div className="flex items-center gap-2.5">
            <BookOpen className="w-5 h-5 text-amber-400 shrink-0" />
            <div>
              <h2 id="mobile-guide-title" className="text-base font-semibold text-slate-100">
                Guide pas à pas sur téléphone (sans PC)
              </h2>
              <p className="text-xs text-slate-400">
                Installation rapide en PWA ou compilation d’APK via GitHub Actions
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fermer"
            className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl text-slate-400 hover:text-slate-100 hover:bg-slate-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-5 overflow-y-auto space-y-6">
          {GUIDE_STEPS.map((step) => (
            <section
              key={step.id}
              className="rounded-2xl bg-slate-900/60 border border-slate-800 overflow-hidden grid grid-cols-1 md:grid-cols-12"
            >
              <div className="md:col-span-6 bg-[#0B0F17] border-b md:border-b-0 md:border-r border-slate-800 flex items-center justify-center">
                {!failedImages[step.id] ? (
                  <img
                    src={step.imagePath}
                    alt={step.imageAlt}
                    referrerPolicy="no-referrer"
                    onError={() =>
                      setFailedImages((prev) => ({ ...prev, [step.id]: true }))
                    }
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="p-8 text-center space-y-2 text-slate-400">
                    <Smartphone className="w-8 h-8 text-amber-400 mx-auto" />
                    <div className="text-xs">{step.imageAlt}</div>
                  </div>
                )}
              </div>

              <div className="md:col-span-6 p-5 space-y-3 flex flex-col justify-center">
                <div>
                  <h3 className="text-base font-semibold text-slate-100">{step.title}</h3>
                  <p className="text-xs text-slate-400 mt-1">{step.subtitle}</p>
                </div>
                <ul className="space-y-2 text-xs text-slate-200 leading-relaxed">
                  {step.points.map((pt, idx) => (
                    <li key={idx} className="flex items-start gap-2">
                      <CheckCircle2 className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
                      <span>{pt}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </section>
          ))}
        </div>

        <div className="px-5 py-4 border-t border-slate-800 bg-[#0E1420] flex flex-wrap items-center justify-between gap-3">
          <button
            type="button"
            onClick={() => {
              onClose();
              onOpenAndroidCode();
            }}
            className="min-h-[44px] px-4 py-2 rounded-xl text-xs font-medium bg-slate-800 text-slate-200 hover:bg-slate-700 transition-colors whitespace-nowrap"
          >
            Voir les fichiers Kotlin / Gradle
          </button>
          <button
            type="button"
            onClick={onClose}
            className="min-h-[44px] px-5 py-2 rounded-xl text-xs font-semibold bg-amber-500 text-slate-950 hover:bg-amber-400 transition-colors whitespace-nowrap"
          >
            Compris
          </button>
        </div>
      </div>
    </div>
  );
};
