import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Upload, ShieldCheck, X, AlertCircle, Loader2, Image as ImageIcon } from 'lucide-react';
import { PLAYER_ROLE_LABELS, PlayerRole } from '../types/game';

interface ShareCaptureModalProps {
  isOpen: boolean;
  onClose: () => void;
  playerRole: PlayerRole;
  initialFile?: File | null;
  initialBase64?: { base64: string; mimeType: string; fileName: string } | null;
  isAnalyzing: boolean;
  error: string | null;
  onAnalyze: (payload: {
    imageBase64: string;
    mimeType: string;
    fileName: string;
    thumbnailDataUrl: string;
    note?: string;
  }) => Promise<void>;
}

async function compressImageForUpload(
  fileOrDataUrl: File | { base64: string; mimeType: string }
): Promise<{ base64: string; mimeType: string; thumbnailDataUrl: string }> {
  const dataUrl = await new Promise<string>((resolve, reject) => {
    if (fileOrDataUrl instanceof File) {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result));
      reader.onerror = () => reject(new Error('Impossible de lire le fichier image.'));
      reader.readAsDataURL(fileOrDataUrl);
    } else {
      resolve(`data:${fileOrDataUrl.mimeType};base64,${fileOrDataUrl.base64}`);
    }
  });

  const img = await new Promise<HTMLImageElement>((resolve, reject) => {
    const el = new Image();
    el.onload = () => resolve(el);
    el.onerror = () => reject(new Error('Format d’image non pris en charge.'));
    el.src = dataUrl;
  });

  // Resize gently for fast analysis while preserving crisp UI text for OCR/Vision
  const maxDim = 1600;
  let targetW = img.width;
  let targetH = img.height;
  if (targetW > maxDim || targetH > maxDim) {
    const scale = maxDim / Math.max(targetW, targetH);
    targetW = Math.round(targetW * scale);
    targetH = Math.round(targetH * scale);
  }

  const canvas = document.createElement('canvas');
  canvas.width = targetW;
  canvas.height = targetH;
  const ctx = canvas.getContext('2d');
  if (!ctx) {
    throw new Error('Contexte graphique indisponible.');
  }
  ctx.drawImage(img, 0, 0, targetW, targetH);

  const highResUrl = canvas.toDataURL('image/jpeg', 0.9);
  const base64 = highResUrl.split(',')[1] || '';

  // Small thumbnail for localStorage history
  const thumbCanvas = document.createElement('canvas');
  const thumbMax = 320;
  const thumbScale = Math.min(1, thumbMax / Math.max(targetW, targetH));
  thumbCanvas.width = Math.max(1, Math.round(targetW * thumbScale));
  thumbCanvas.height = Math.max(1, Math.round(targetH * thumbScale));
  const thumbCtx = thumbCanvas.getContext('2d');
  thumbCtx?.drawImage(img, 0, 0, thumbCanvas.width, thumbCanvas.height);
  const thumbnailDataUrl = thumbCanvas.toDataURL('image/jpeg', 0.72);

  return {
    base64,
    mimeType: 'image/jpeg',
    thumbnailDataUrl,
  };
}

export const ShareCaptureModal: React.FC<ShareCaptureModalProps> = ({
  isOpen,
  onClose,
  playerRole,
  initialFile,
  initialBase64,
  isAnalyzing,
  error,
  onAnalyze,
}) => {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [prepared, setPrepared] = useState<{
    base64: string;
    mimeType: string;
    fileName: string;
    thumbnailDataUrl: string;
  } | null>(null);
  const [note, setNote] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);

  const loadFile = useCallback(async (file: File) => {
    setLocalError(null);
    if (!file.type.startsWith('image/')) {
      setLocalError('Veuillez sélectionner un fichier image (PNG, JPG, WebP).');
      return;
    }
    try {
      const processed = await compressImageForUpload(file);
      setPrepared({
        ...processed,
        fileName: file.name || 'capture-ecran.jpg',
      });
      setPreviewUrl(`data:${processed.mimeType};base64,${processed.base64}`);
    } catch (e) {
      setLocalError(e instanceof Error ? e.message : 'Erreur lors du chargement de la capture.');
    }
  }, []);

  useEffect(() => {
    if (!isOpen) {
      setPreviewUrl(null);
      setPrepared(null);
      setNote('');
      setLocalError(null);
      return;
    }

    if (initialFile) {
      void loadFile(initialFile);
    } else if (initialBase64) {
      void (async () => {
        try {
          const processed = await compressImageForUpload({
            base64: initialBase64.base64,
            mimeType: initialBase64.mimeType,
          });
          setPrepared({
            ...processed,
            fileName: initialBase64.fileName,
          });
          setPreviewUrl(`data:${processed.mimeType};base64,${processed.base64}`);
        } catch (e) {
          setLocalError(e instanceof Error ? e.message : 'Erreur de lecture du partage.');
        }
      })();
    }
  }, [isOpen, initialFile, initialBase64, loadFile]);

  // Support Ctrl+V paste when modal is open
  useEffect(() => {
    if (!isOpen) return;
    const handlePaste = (e: ClipboardEvent) => {
      const items = e.clipboardData?.items;
      if (!items) return;
      for (const item of Array.from(items)) {
        if (item.type.startsWith('image/')) {
          const file = item.getAsFile();
          if (file) {
            void loadFile(file);
            break;
          }
        }
      }
    };
    window.addEventListener('paste', handlePaste);
    return () => window.removeEventListener('paste', handlePaste);
  }, [isOpen, loadFile]);

  if (!isOpen) return null;

  const activeError = error || localError;

  return (
    <div
      className="fixed inset-0 z-50 flex items-end md:items-center justify-center bg-black/70 backdrop-blur-sm p-0 md:p-6"
      role="dialog"
      aria-modal="true"
      aria-labelledby="share-modal-title"
    >
      <div className="w-full md:max-w-xl bg-[#121826] border border-slate-800 rounded-t-3xl md:rounded-2xl shadow-2xl max-h-[92vh] flex flex-col overflow-hidden">
        <div className="md:hidden w-10 h-1.5 bg-slate-700 rounded-full mx-auto mt-3 shrink-0" />

        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-800/80">
          <div>
            <h2 id="share-modal-title" className="text-lg font-semibold text-slate-100">
              Partager une capture d’écran
            </h2>
            <p className="text-xs text-slate-400 mt-0.5">
              Rôle actif : {PLAYER_ROLE_LABELS[playerRole].label}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isAnalyzing}
            aria-label="Fermer"
            className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl text-slate-400 hover:text-slate-100 hover:bg-slate-800/70 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-5 overflow-y-auto space-y-4">
          <div className="flex items-start gap-3 p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 text-xs text-slate-300 leading-relaxed">
            <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
            <span>
              L’analyse démarre uniquement lorsque vous partagez ou importez manuellement une capture
              ci-dessous. L’application ne lit jamais votre écran automatiquement en arrière-plan.
            </span>
          </div>

          <input
            ref={inputRef}
            type="file"
            accept="image/*"
            className="hidden"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) void loadFile(file);
              e.target.value = '';
            }}
          />

          {!previewUrl ? (
            <div
              onDragOver={(e) => {
                e.preventDefault();
                setIsDragging(true);
              }}
              onDragLeave={() => setIsDragging(false)}
              onDrop={(e) => {
                e.preventDefault();
                setIsDragging(false);
                const file = e.dataTransfer.files?.[0];
                if (file) void loadFile(file);
              }}
              onClick={() => inputRef.current?.click()}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  inputRef.current?.click();
                }
              }}
              className={`cursor-pointer rounded-2xl border-2 border-dashed p-8 text-center transition-colors ${
                isDragging
                  ? 'border-amber-500 bg-amber-500/10'
                  : 'border-slate-700 hover:border-amber-500/60 bg-slate-900/50'
              }`}
            >
              <div className="w-12 h-12 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center mx-auto mb-3 text-amber-400">
                <Upload className="w-6 h-6" />
              </div>
              <p className="text-sm font-medium text-slate-100">
                Touchez pour choisir une capture, glissez-déposez ou collez (Ctrl+V)
              </p>
              <p className="text-xs text-slate-400 mt-1.5">
                Profil, fiche héros, compétences, ressources, Trial Cage ou Crystal Clash
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              <div className="relative rounded-xl overflow-hidden border border-slate-800 bg-slate-950 flex items-center justify-center max-h-64">
                <img
                  src={previewUrl}
                  alt="Aperçu de la capture à analyser"
                  className="max-h-64 w-auto object-contain"
                />
              </div>
              <div className="flex items-center justify-between text-xs text-slate-400">
                <span className="truncate max-w-[70%]">{prepared?.fileName}</span>
                <button
                  type="button"
                  onClick={() => {
                    setPreviewUrl(null);
                    setPrepared(null);
                  }}
                  disabled={isAnalyzing}
                  className="text-amber-400 hover:text-amber-300 font-medium py-1 px-2 whitespace-nowrap shrink-0"
                >
                  Changer de capture
                </button>
              </div>
            </div>
          )}

          <div>
            <label htmlFor="capture-note" className="block text-xs font-medium text-slate-300 mb-1.5">
              Contexte optionnel (ex. : « Héros principal pour Trial Cage »)
            </label>
            <input
              id="capture-note"
              type="text"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              disabled={isAnalyzing}
              placeholder="Précision utile pour l’extraction..."
              className="w-full rounded-xl bg-slate-900 border border-slate-800 px-3.5 py-2.5 text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-amber-500"
            />
          </div>

          {activeError && (
            <div className="flex items-start gap-2.5 p-3.5 rounded-xl bg-red-500/10 border border-red-500/30 text-xs text-red-200">
              <AlertCircle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
              <span>{activeError}</span>
            </div>
          )}
        </div>

        <div className="px-5 py-4 border-t border-slate-800/80 flex items-center justify-end gap-3 bg-[#0E1420]">
          <button
            type="button"
            onClick={onClose}
            disabled={isAnalyzing}
            className="min-h-[44px] px-4 py-2 rounded-xl text-sm font-medium text-slate-300 hover:text-slate-100 hover:bg-slate-800 transition-colors whitespace-nowrap shrink-0"
          >
            Annuler
          </button>
          <button
            type="button"
            disabled={!prepared || isAnalyzing}
            onClick={() => {
              if (!prepared) return;
              void onAnalyze({
                imageBase64: prepared.base64,
                mimeType: prepared.mimeType,
                fileName: prepared.fileName,
                thumbnailDataUrl: prepared.thumbnailDataUrl,
                note: note.trim() || undefined,
              });
            }}
            className="min-h-[44px] px-5 py-2 rounded-xl text-sm font-semibold bg-amber-500 text-slate-950 hover:bg-amber-400 disabled:opacity-40 disabled:cursor-not-allowed transition-colors flex items-center gap-2 whitespace-nowrap shrink-0"
          >
            {isAnalyzing ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Analyse en cours…</span>
              </>
            ) : (
              <>
                <ImageIcon className="w-4 h-4" />
                <span>Lancer l’analyse</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
