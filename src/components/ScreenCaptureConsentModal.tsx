import React, { useMemo, useRef, useState } from 'react';
import { AlertCircle, Camera, Loader2, ShieldAlert, Upload, X } from 'lucide-react';

interface ScreenCaptureConsentModalProps {
  isOpen: boolean;
  onClose: () => void;
  isAnalyzing: boolean;
  onCapturedImage: (payload: {
    imageBase64: string;
    mimeType: string;
    fileName: string;
    thumbnailDataUrl: string;
    note?: string;
  }) => Promise<void>;
  onFallbackToFileUpload: () => void;
}

async function processImageBlobToPayload(
  blob: Blob,
  fileName: string,
  note: string
): Promise<{
  imageBase64: string;
  mimeType: string;
  fileName: string;
  thumbnailDataUrl: string;
  note: string;
}> {
  const dataUrl = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(new Error('Impossible de lire l’image sélectionnée.'));
    reader.readAsDataURL(blob);
  });

  const img = await new Promise<HTMLImageElement>((resolve, reject) => {
    const el = new Image();
    el.onload = () => resolve(el);
    el.onerror = () => reject(new Error('Format d’image non pris en charge.'));
    el.src = dataUrl;
  });

  const maxDim = 1600;
  const scale = Math.min(1, maxDim / Math.max(img.width, img.height));
  const canvas = document.createElement('canvas');
  canvas.width = Math.max(1, Math.round(img.width * scale));
  canvas.height = Math.max(1, Math.round(img.height * scale));
  const ctx = canvas.getContext('2d');
  if (!ctx) throw new Error('Contexte graphique indisponible.');
  ctx.drawImage(img, 0, 0, canvas.width, canvas.height);

  const jpegUrl = canvas.toDataURL('image/jpeg', 0.9);
  const base64 = jpegUrl.split(',')[1] || '';

  const thumbCanvas = document.createElement('canvas');
  const thumbScale = Math.min(1, 320 / Math.max(canvas.width, canvas.height));
  thumbCanvas.width = Math.max(1, Math.round(canvas.width * thumbScale));
  thumbCanvas.height = Math.max(1, Math.round(canvas.height * thumbScale));
  thumbCanvas.getContext('2d')?.drawImage(canvas, 0, 0, thumbCanvas.width, thumbCanvas.height);

  return {
    imageBase64: base64,
    mimeType: 'image/jpeg',
    fileName,
    thumbnailDataUrl: thumbCanvas.toDataURL('image/jpeg', 0.72),
    note,
  };
}

export const ScreenCaptureConsentModal: React.FC<ScreenCaptureConsentModalProps> = ({
  isOpen,
  onClose,
  isAnalyzing,
  onCapturedImage,
  onFallbackToFileUpload,
}) => {
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isCapturingFrame, setIsCapturingFrame] = useState(false);

  const browserRestrictions = useMemo(() => {
    if (typeof window === 'undefined') {
      return { isMobile: false, isInIframe: false, supportsDisplayMedia: false };
    }
    const ua = navigator.userAgent || '';
    const isMobile = /Android|iPhone|iPad|iPod/i.test(ua);
    const isInIframe = window.self !== window.top;
    const supportsDisplayMedia = Boolean(
      navigator.mediaDevices && typeof navigator.mediaDevices.getDisplayMedia === 'function'
    );
    return { isMobile, isInIframe, supportsDisplayMedia };
  }, []);

  if (!isOpen) return null;

  const canAttemptBrowserCapture =
    browserRestrictions.supportsDisplayMedia && !browserRestrictions.isMobile;

  const handleDirectFilePicked = async (file: File) => {
    setError(null);
    setIsCapturingFrame(true);
    try {
      const payload = await processImageBlobToPayload(
        file,
        file.name || 'capture-lands-of-jail.jpg',
        'Capture d’écran importée depuis l’appareil'
      );
      setIsCapturingFrame(false);
      await onCapturedImage(payload);
    } catch (err) {
      setIsCapturingFrame(false);
      setError(err instanceof Error ? err.message : 'Erreur lors du chargement de la capture.');
    }
  };

  const handleAuthorizeSystemCapture = async () => {
    setError(null);
    if (!canAttemptBrowserCapture) {
      fileInputRef.current?.click();
      return;
    }

    setIsCapturingFrame(true);
    let stream: MediaStream | null = null;
    try {
      // Demande l'autorisation système visible (équivalent MediaProjection sur Android)
      stream = await navigator.mediaDevices.getDisplayMedia({
        video: true,
        audio: false,
      });

      const video = document.createElement('video');
      video.srcObject = stream;
      video.muted = true;
      await video.play();

      await new Promise((resolve) => setTimeout(resolve, 180));

      const width = video.videoWidth || 1280;
      const height = video.videoHeight || 720;
      const maxDim = 1600;
      const scale = Math.min(1, maxDim / Math.max(width, height));

      const canvas = document.createElement('canvas');
      canvas.width = Math.max(1, Math.round(width * scale));
      canvas.height = Math.max(1, Math.round(height * scale));
      const ctx = canvas.getContext('2d');
      if (!ctx) throw new Error('Contexte graphique indisponible.');
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

      // Arrêt immédiat de la capture après UNE seule image (aucun enregistrement continu)
      stream.getTracks().forEach((t) => t.stop());
      stream = null;

      const dataUrl = canvas.toDataURL('image/jpeg', 0.9);
      const base64 = dataUrl.split(',')[1] || '';

      const thumbCanvas = document.createElement('canvas');
      const thumbScale = Math.min(1, 320 / Math.max(canvas.width, canvas.height));
      thumbCanvas.width = Math.max(1, Math.round(canvas.width * thumbScale));
      thumbCanvas.height = Math.max(1, Math.round(canvas.height * thumbScale));
      thumbCanvas.getContext('2d')?.drawImage(canvas, 0, 0, thumbCanvas.width, thumbCanvas.height);

      setIsCapturingFrame(false);

      await onCapturedImage({
        imageBase64: base64,
        mimeType: 'image/jpeg',
        fileName: `capture-autorisee-${new Date().toISOString().slice(11, 19).replace(/:/g, '-')}.jpg`,
        thumbnailDataUrl: thumbCanvas.toDataURL('image/jpeg', 0.72),
        note: 'Capture ponctuelle autorisée après appui sur Analyser l’écran',
      });
    } catch {
      if (stream) {
        stream.getTracks().forEach((t) => t.stop());
      }
      setIsCapturingFrame(false);
      setError(
        'Le navigateur a bloqué la capture d’écran directe (sécurité Android Chrome ou restriction d’aperçu intégré). Touchez « Choisir une capture » ci-dessous pour analyser votre capture d’écran.'
      );
    }
  };

  return (
    <div
      className="fixed inset-0 z-[60] flex items-end md:items-center justify-center bg-black/75 backdrop-blur-sm p-2 sm:p-4 md:p-6 overflow-hidden"
      role="dialog"
      aria-modal="true"
      aria-labelledby="capture-consent-title"
    >
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) void handleDirectFilePicked(file);
          e.target.value = '';
        }}
      />

      <div className="w-full max-w-full md:max-w-lg bg-[#121826] border border-slate-800 rounded-2xl shadow-2xl max-h-[86dvh] flex flex-col overflow-hidden">
        <div className="md:hidden w-10 h-1 bg-slate-700 rounded-full mx-auto mt-2.5 shrink-0" />

        <div className="flex items-center justify-between px-4 sm:px-5 py-3.5 border-b border-slate-800 shrink-0">
          <div className="flex items-center gap-2.5 min-w-0">
            <Camera className="w-5 h-5 text-amber-400 shrink-0" />
            <h2 id="capture-consent-title" className="text-base font-semibold text-slate-100 truncate">
              Capture & Analyse de l’écran
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isAnalyzing || isCapturingFrame}
            aria-label="Fermer"
            className="min-h-[40px] min-w-[40px] flex items-center justify-center rounded-xl text-slate-400 hover:text-slate-100 hover:bg-slate-800 shrink-0"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="flex-1 min-h-0 p-4 sm:p-5 space-y-4 overflow-y-auto">
          {!canAttemptBrowserCapture ? (
            <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/30 space-y-2 text-xs text-amber-100 leading-relaxed">
              <div className="font-semibold text-amber-300 flex items-center gap-2">
                <ShieldAlert className="w-4 h-4 shrink-0" />
                <span>Capture directe bloquée par le navigateur mobile</span>
              </div>
              <p>
                Sur Android, Chrome et les aperçus web ne permettent pas à une page web de capturer
                l’écran d’un autre jeu directement (seul l’APK natif avec <code>MediaProjection</code>{' '}
                le permet).
              </p>
              <p className="text-amber-200/90">
                Solution immédiate : faites une capture d’écran Android classique (Bouton Power +
                Volume Bas), puis appuyez sur <strong>« Choisir une capture (Galerie) »</strong>{' '}
                ci-dessous.
              </p>
            </div>
          ) : (
            <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 space-y-2 text-xs text-slate-300 leading-relaxed">
              <div className="font-semibold text-slate-100 flex items-center gap-2">
                <ShieldAlert className="w-4 h-4 text-amber-400 shrink-0" />
                <span>Autorisation système requise</span>
              </div>
              <p>
                Lands of Jail Assistant demande votre autorisation visible pour capturer{' '}
                <strong>une seule image</strong> de l’écran du jeu et lancer l’analyse OCR.
              </p>
              {browserRestrictions.isInIframe && (
                <p className="text-amber-300">
                  Note : l’aperçu intégré peut bloquer la capture d’écran du navigateur ; en cas de
                  refus, utilisez « Choisir une capture (Galerie) ».
                </p>
              )}
            </div>
          )}

          {error && (
            <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/30 flex items-start gap-2.5 text-xs text-red-200">
              <AlertCircle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}
        </div>

        <div className="px-5 py-4 border-t border-slate-800 bg-[#0E1420] flex flex-wrap items-center justify-between gap-3">
          <button
            type="button"
            disabled={isAnalyzing || isCapturingFrame}
            onClick={() => {
              onClose();
              onFallbackToFileUpload();
            }}
            className="min-h-[44px] px-3.5 py-2 rounded-xl text-xs font-medium text-slate-300 hover:text-slate-100 hover:bg-slate-800 transition-colors whitespace-nowrap"
          >
            Ouvrir l’import détaillé
          </button>

          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={onClose}
              disabled={isAnalyzing || isCapturingFrame}
              className="min-h-[44px] px-3 py-2 rounded-xl text-xs font-medium text-slate-400 hover:text-slate-100"
            >
              Annuler
            </button>

            <button
              type="button"
              disabled={isAnalyzing || isCapturingFrame}
              onClick={() => fileInputRef.current?.click()}
              className={`min-h-[44px] px-4 py-2 rounded-xl text-xs font-semibold transition-colors flex items-center gap-2 whitespace-nowrap ${
                !canAttemptBrowserCapture
                  ? 'bg-amber-500 text-slate-950 hover:bg-amber-400'
                  : 'bg-slate-800 text-slate-100 hover:bg-slate-700'
              }`}
            >
              {isAnalyzing || isCapturingFrame ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Analyse OCR…</span>
                </>
              ) : (
                <>
                  <Upload className="w-4 h-4" />
                  <span>Choisir une capture (Galerie)</span>
                </>
              )}
            </button>

            {canAttemptBrowserCapture && (
              <button
                type="button"
                disabled={isAnalyzing || isCapturingFrame}
                onClick={() => void handleAuthorizeSystemCapture()}
                className="min-h-[44px] px-4 py-2 rounded-xl text-xs font-semibold bg-amber-500 text-slate-950 hover:bg-amber-400 disabled:opacity-50 transition-colors flex items-center gap-2 whitespace-nowrap"
              >
                <Camera className="w-4 h-4" />
                <span>Autoriser capture écran</span>
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
