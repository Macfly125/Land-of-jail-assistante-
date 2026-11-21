import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  Upload,
  ShieldCheck,
  X,
  AlertCircle,
  Loader2,
  CheckCircle2,
  Play,
  Plus,
  Trash2,
  Clock,
} from 'lucide-react';
import { PLAYER_ROLE_LABELS, PlayerRole } from '../types/game';

const AUTO_ANALYZE_STORAGE_KEY = 'loj_auto_analyze_import_v1';

interface QueuedCaptureItem {
  id: string;
  fileName: string;
  base64: string;
  mimeType: string;
  thumbnailDataUrl: string;
  previewUrl: string;
  status: 'waiting' | 'analyzing' | 'done' | 'error';
  progress: number;
  summary?: string;
  error?: string;
}

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
  }) => Promise<{ summary: string }>;
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
  const [queue, setQueue] = useState<QueuedCaptureItem[]>([]);
  const queueRef = useRef<QueuedCaptureItem[]>([]);
  const [note, setNote] = useState('');
  const noteRef = useRef('');
  const [isPreparing, setIsPreparing] = useState(false);
  const [isQueueRunning, setIsQueueRunning] = useState(false);
  const isQueueRunningRef = useRef(false);
  const [localError, setLocalError] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [autoAnalyzeOnImport, setAutoAnalyzeOnImport] = useState<boolean>(() => {
    try {
      return localStorage.getItem(AUTO_ANALYZE_STORAGE_KEY) === 'true';
    } catch {
      return false;
    }
  });

  useEffect(() => {
    queueRef.current = queue;
  }, [queue]);

  useEffect(() => {
    noteRef.current = note;
  }, [note]);

  const toggleAutoAnalyze = () => {
    setAutoAnalyzeOnImport((prev) => {
      const next = !prev;
      try {
        localStorage.setItem(AUTO_ANALYZE_STORAGE_KEY, String(next));
      } catch {
        // ignore
      }
      return next;
    });
  };

  const runSequentialQueue = useCallback(async () => {
    if (isQueueRunningRef.current) return;
    isQueueRunningRef.current = true;
    setIsQueueRunning(true);
    setLocalError(null);

    try {
      while (true) {
        const nextItem = queueRef.current.find((item) => item.status === 'waiting');
        if (!nextItem) break;

        // Début d'analyse de cet élément (15%)
        setQueue((prev) =>
          prev.map((q) =>
            q.id === nextItem.id
              ? { ...q, status: 'analyzing', progress: 15, error: undefined }
              : q
          )
        );

        // Progression fluide réaliste pendant l'appel serveur (15% -> 92%)
        const progressTimer = window.setInterval(() => {
          setQueue((prev) =>
            prev.map((q) => {
              if (q.id !== nextItem.id || q.status !== 'analyzing') return q;
              const step = q.progress < 50 ? 11 : q.progress < 78 ? 6 : 2;
              return { ...q, progress: Math.min(92, q.progress + step) };
            })
          );
        }, 420);

        try {
          const result = await onAnalyze({
            imageBase64: nextItem.base64,
            mimeType: nextItem.mimeType,
            fileName: nextItem.fileName,
            thumbnailDataUrl: nextItem.thumbnailDataUrl,
            note: noteRef.current.trim() || undefined,
          });
          window.clearInterval(progressTimer);

          setQueue((prev) =>
            prev.map((q) =>
              q.id === nextItem.id
                ? {
                    ...q,
                    status: 'done',
                    progress: 100,
                    summary: result.summary,
                  }
                : q
            )
          );
        } catch (err) {
          window.clearInterval(progressTimer);
          const message = err instanceof Error ? err.message : 'Échec de l’analyse.';
          setQueue((prev) =>
            prev.map((q) =>
              q.id === nextItem.id
                ? {
                    ...q,
                    status: 'error',
                    progress: 0,
                    error: message,
                  }
                : q
            )
          );
        }
      }
    } finally {
      isQueueRunningRef.current = false;
      setIsQueueRunning(false);
    }
  }, [onAnalyze]);

  const enqueueFiles = useCallback(
    async (files: File[]) => {
      setLocalError(null);
      const imageFiles = files.filter((f) => f.type.startsWith('image/'));
      if (imageFiles.length === 0) {
        setLocalError('Veuillez sélectionner au moins une image (PNG, JPG, WebP).');
        return;
      }

      setIsPreparing(true);
      const added: QueuedCaptureItem[] = [];
      for (const file of imageFiles) {
        try {
          const processed = await compressImageForUpload(file);
          added.push({
            id: `q-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
            fileName: file.name || `capture-${added.length + 1}.jpg`,
            base64: processed.base64,
            mimeType: processed.mimeType,
            thumbnailDataUrl: processed.thumbnailDataUrl,
            previewUrl: `data:${processed.mimeType};base64,${processed.base64}`,
            status: 'waiting',
            progress: 0,
          });
        } catch (e) {
          setLocalError(e instanceof Error ? e.message : 'Erreur lors du chargement d’une capture.');
        }
      }
      setIsPreparing(false);

      if (added.length > 0) {
        const nextQueue = [...queueRef.current, ...added];
        queueRef.current = nextQueue;
        setQueue(nextQueue);
        if (autoAnalyzeOnImport) {
          void runSequentialQueue();
        }
      }
    },
    [autoAnalyzeOnImport, runSequentialQueue]
  );

  useEffect(() => {
    if (!isOpen) {
      setQueue([]);
      queueRef.current = [];
      setNote('');
      setIsPreparing(false);
      setIsQueueRunning(false);
      isQueueRunningRef.current = false;
      setLocalError(null);
      return;
    }

    if (initialFile) {
      void enqueueFiles([initialFile]);
    } else if (initialBase64) {
      setIsPreparing(true);
      void (async () => {
        try {
          const processed = await compressImageForUpload({
            base64: initialBase64.base64,
            mimeType: initialBase64.mimeType,
          });
          const item: QueuedCaptureItem = {
            id: `q-${Date.now()}-share`,
            fileName: initialBase64.fileName,
            base64: processed.base64,
            mimeType: processed.mimeType,
            thumbnailDataUrl: processed.thumbnailDataUrl,
            previewUrl: `data:${processed.mimeType};base64,${processed.base64}`,
            status: 'waiting',
            progress: 0,
          };
          const nextQueue = [...queueRef.current, item];
          queueRef.current = nextQueue;
          setQueue(nextQueue);
          setIsPreparing(false);
          if (autoAnalyzeOnImport) {
            void runSequentialQueue();
          }
        } catch (e) {
          setIsPreparing(false);
          setLocalError(e instanceof Error ? e.message : 'Erreur de lecture du partage.');
        }
      })();
    }
  }, [isOpen, initialFile, initialBase64, enqueueFiles, autoAnalyzeOnImport, runSequentialQueue]);

  // Support Ctrl+V paste when modal is open
  useEffect(() => {
    if (!isOpen) return;
    const handlePaste = (e: ClipboardEvent) => {
      const items = e.clipboardData?.items;
      if (!items) return;
      const files: File[] = [];
      for (const item of Array.from(items)) {
        if (item.type.startsWith('image/')) {
          const file = item.getAsFile();
          if (file) files.push(file);
        }
      }
      if (files.length > 0) {
        void enqueueFiles(files);
      }
    };
    window.addEventListener('paste', handlePaste);
    return () => window.removeEventListener('paste', handlePaste);
  }, [isOpen, enqueueFiles]);

  if (!isOpen) return null;

  const activeError = localError || error;
  const busy = isAnalyzing || isQueueRunning;
  const totalCount = queue.length;
  const waitingCount = queue.filter((q) => q.status === 'waiting').length;
  const doneCount = queue.filter((q) => q.status === 'done').length;
  const errorCount = queue.filter((q) => q.status === 'error').length;
  const overallPercent =
    totalCount > 0
      ? Math.round(
          queue.reduce((acc, q) => acc + (q.status === 'done' ? 100 : q.progress), 0) / totalCount
        )
      : 0;
  const allFinished = totalCount > 0 && waitingCount === 0 && !busy && doneCount > 0 && errorCount === 0;

  const removeQueueItem = (id: string) => {
    setQueue((prev) => {
      const filtered = prev.filter((q) => q.id !== id);
      queueRef.current = filtered;
      return filtered;
    });
  };

  const retryFailedItems = () => {
    setQueue((prev) => {
      const reset = prev.map((q) =>
        q.status === 'error' ? { ...q, status: 'waiting' as const, progress: 0, error: undefined } : q
      );
      queueRef.current = reset;
      return reset;
    });
    void runSequentialQueue();
  };

  return (
    <div
      className="fixed inset-0 z-[60] flex items-end md:items-center justify-center bg-black/75 backdrop-blur-sm p-2 sm:p-4 md:p-6 overflow-hidden"
      role="dialog"
      aria-modal="true"
      aria-labelledby="share-modal-title"
    >
      <div className="w-full max-w-full md:max-w-xl bg-[#121826] border border-slate-800 rounded-2xl shadow-2xl max-h-[88dvh] flex flex-col overflow-hidden">
        <div className="md:hidden w-10 h-1 bg-slate-700 rounded-full mx-auto mt-2.5 shrink-0" />

        <div className="flex items-center justify-between px-4 sm:px-5 py-3 border-b border-slate-800/80 shrink-0 gap-2">
          <div className="min-w-0">
            <h2 id="share-modal-title" className="text-base sm:text-lg font-semibold text-slate-100 truncate">
              Importer & analyser des captures
            </h2>
            <p className="text-xs text-slate-400 mt-0.5 truncate">
              Rôle actif : {PLAYER_ROLE_LABELS[playerRole].label}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={busy}
            aria-label="Fermer"
            className="min-h-[40px] min-w-[40px] flex items-center justify-center rounded-xl text-slate-400 hover:text-slate-100 hover:bg-slate-800/70 transition-colors shrink-0"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="flex-1 min-h-0 p-4 sm:p-5 overflow-y-auto space-y-3.5">
          {/* Choix clair : manuel ou automatique à l'import */}
          <label className="flex items-center justify-between gap-3 p-3 rounded-xl bg-slate-900/90 border border-slate-800 text-xs text-slate-200 cursor-pointer">
            <span className="leading-snug">
              Lancer l’analyse <strong>automatiquement</strong> dès l’ajout des photos
            </span>
            <input
              type="checkbox"
              checked={autoAnalyzeOnImport}
              onChange={toggleAutoAnalyze}
              disabled={busy}
              className="w-4 h-4 accent-amber-500 rounded shrink-0"
            />
          </label>

          <input
            ref={inputRef}
            type="file"
            accept="image/*"
            multiple
            className="hidden"
            onChange={(e) => {
              const files = e.target.files ? Array.from(e.target.files) : [];
              if (files.length > 0) void enqueueFiles(files);
              e.target.value = '';
            }}
          />

          {queue.length === 0 ? (
            <div
              onDragOver={(e) => {
                e.preventDefault();
                setIsDragging(true);
              }}
              onDragLeave={() => setIsDragging(false)}
              onDrop={(e) => {
                e.preventDefault();
                setIsDragging(false);
                const files = e.dataTransfer.files ? Array.from(e.dataTransfer.files) : [];
                if (files.length > 0) void enqueueFiles(files);
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
              className={`cursor-pointer rounded-2xl border-2 border-dashed p-6 sm:p-8 text-center transition-colors ${
                isDragging
                  ? 'border-amber-500 bg-amber-500/10'
                  : 'border-slate-700 hover:border-amber-500/60 bg-slate-900/50'
              }`}
            >
              <div className="w-11 h-11 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center mx-auto mb-3 text-amber-400">
                {isPreparing ? <Loader2 className="w-5 h-5 animate-spin" /> : <Upload className="w-5 h-5" />}
              </div>
              <p className="text-sm font-medium text-slate-100">
                {isPreparing
                  ? 'Préparation des captures…'
                  : 'Touchez ici pour choisir une ou plusieurs captures'}
              </p>
              <p className="text-xs text-slate-400 mt-1">
                Sélection multiple possible : les captures seront traitées l’une après l’autre.
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {/* Barre de progression globale (%) */}
              <div className="p-3.5 rounded-xl bg-slate-900/95 border border-slate-800 space-y-2">
                <div className="flex items-center justify-between gap-2 text-xs">
                  <span className="font-semibold text-slate-100">
                    {busy
                      ? 'Analyse séquentielle en cours…'
                      : allFinished
                        ? 'Toutes les captures sont analysées'
                        : `${waitingCount} capture(s) en attente`}
                  </span>
                  <span className="font-mono-tabular font-bold text-amber-400 text-sm">
                    {overallPercent} %
                  </span>
                </div>

                <div className="w-full h-2.5 rounded-full bg-slate-800 overflow-hidden">
                  <div
                    className={`h-full transition-all duration-300 ${
                      allFinished ? 'bg-emerald-500' : 'bg-amber-500'
                    }`}
                    style={{ width: `${overallPercent}%` }}
                  />
                </div>

                <div className="flex flex-wrap items-center justify-between gap-2 text-[11px] text-slate-400 font-mono-tabular">
                  <span>
                    {doneCount}/{totalCount} terminée(s)
                    {errorCount > 0 ? ` · ${errorCount} échec(s)` : ''}
                  </span>
                  <button
                    type="button"
                    onClick={() => inputRef.current?.click()}
                    disabled={isPreparing}
                    className="inline-flex items-center gap-1 text-amber-400 hover:text-amber-300 font-sans font-semibold"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Ajouter d’autres captures</span>
                  </button>
                </div>
              </div>

              {/* Action immédiate quand des captures attendent */}
              {waitingCount > 0 && !busy && (
                <button
                  type="button"
                  onClick={() => void runSequentialQueue()}
                  className="w-full min-h-[42px] px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold bg-amber-500 text-slate-950 hover:bg-amber-400 inline-flex items-center justify-center gap-2 transition-colors"
                >
                  <Play className="w-4 h-4" />
                  <span>Lancer l’analyse ({waitingCount} capture{waitingCount > 1 ? 's' : ''} en file)</span>
                </button>
              )}

              {/* File d'attente détaillée */}
              <div className="space-y-2">
                {queue.map((item, idx) => (
                  <div
                    key={item.id}
                    className={`p-2.5 rounded-xl border flex items-center gap-3 ${
                      item.status === 'analyzing'
                        ? 'bg-amber-500/10 border-amber-500/45'
                        : item.status === 'done'
                          ? 'bg-emerald-500/10 border-emerald-500/35'
                          : item.status === 'error'
                            ? 'bg-red-500/10 border-red-500/35'
                            : 'bg-slate-900/70 border-slate-800'
                    }`}
                  >
                    <img
                      src={item.previewUrl}
                      alt={item.fileName}
                      className="w-12 h-12 rounded-lg object-cover border border-slate-700/80 shrink-0 bg-slate-950"
                    />
                    <div className="flex-1 min-w-0 space-y-1">
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-xs font-medium text-slate-100 truncate">
                          #{idx + 1} · {item.fileName}
                        </span>
                        <span
                          className={`text-xs font-mono-tabular font-semibold shrink-0 ${
                            item.status === 'done'
                              ? 'text-emerald-400'
                              : item.status === 'analyzing'
                                ? 'text-amber-300'
                                : item.status === 'error'
                                  ? 'text-red-300'
                                  : 'text-slate-400'
                          }`}
                        >
                          {item.status === 'done'
                            ? '100 %'
                            : item.status === 'analyzing'
                              ? `${item.progress} %`
                              : item.status === 'error'
                                ? 'Échec'
                                : 'En attente'}
                        </span>
                      </div>

                      {item.status === 'analyzing' && (
                        <div className="w-full h-1.5 rounded-full bg-slate-800 overflow-hidden">
                          <div
                            className="h-full bg-amber-400 transition-all duration-300"
                            style={{ width: `${item.progress}%` }}
                          />
                        </div>
                      )}

                      <div className="text-[11px] text-slate-400 truncate flex items-center gap-1.5">
                        {item.status === 'waiting' && (
                          <>
                            <Clock className="w-3 h-3 shrink-0" />
                            <span>En attente dans la file…</span>
                          </>
                        )}
                        {item.status === 'analyzing' && (
                          <>
                            <Loader2 className="w-3 h-3 animate-spin text-amber-400 shrink-0" />
                            <span className="text-amber-200">Lecture OCR / Vision en cours…</span>
                          </>
                        )}
                        {item.status === 'done' && (
                          <>
                            <CheckCircle2 className="w-3 h-3 text-emerald-400 shrink-0" />
                            <span className="text-emerald-200 truncate">
                              {item.summary || 'Analyse terminée'}
                            </span>
                          </>
                        )}
                        {item.status === 'error' && (
                          <span className="text-red-300 truncate">{item.error || 'Erreur'}</span>
                        )}
                      </div>
                    </div>

                    {item.status !== 'analyzing' && (
                      <button
                        type="button"
                        onClick={() => removeQueueItem(item.id)}
                        disabled={busy}
                        aria-label="Retirer de la file"
                        className="p-1.5 rounded-lg text-slate-400 hover:text-red-300 hover:bg-slate-800 disabled:opacity-40 shrink-0"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          <div>
            <label htmlFor="capture-note" className="block text-xs font-medium text-slate-300 mb-1">
              Note optionnelle
            </label>
            <input
              id="capture-note"
              type="text"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              disabled={busy}
              placeholder="Ex. : Fiche héros ou inventaire..."
              className="w-full rounded-xl bg-slate-900 border border-slate-800 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-amber-500"
            />
          </div>

          {activeError && (
            <div className="flex items-start gap-2.5 p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-xs text-red-200">
              <AlertCircle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
              <span>{activeError}</span>
            </div>
          )}

          <div className="flex items-start gap-2 p-2.5 rounded-xl bg-slate-900/60 border border-slate-800/70 text-[11px] text-slate-400">
            <ShieldCheck className="w-3.5 h-3.5 text-emerald-400 shrink-0 mt-0.5" />
            <span>
              Les captures ajoutées sont analysées séquentiellement (l’une après l’autre) avec suivi en %.
            </span>
          </div>
        </div>

        <div className="px-4 sm:px-5 py-3 border-t border-slate-800/80 flex items-center justify-end gap-2.5 bg-[#0E1420] shrink-0">
          <button
            type="button"
            onClick={onClose}
            disabled={busy}
            className="min-h-[42px] px-4 py-2 rounded-xl text-sm font-medium text-slate-300 hover:text-slate-100 hover:bg-slate-800 transition-colors whitespace-nowrap shrink-0"
          >
            {allFinished ? 'Fermer (100 %)' : 'Annuler'}
          </button>

          {errorCount > 0 && !busy && (
            <button
              type="button"
              onClick={retryFailedItems}
              className="min-h-[42px] px-4 py-2 rounded-xl text-sm font-semibold bg-slate-800 text-amber-300 hover:bg-slate-700 transition-colors whitespace-nowrap shrink-0"
            >
              Relancer les échecs
            </button>
          )}

          {waitingCount > 0 || busy ? (
            <button
              type="button"
              disabled={waitingCount === 0 || busy}
              onClick={() => void runSequentialQueue()}
              className="min-h-[42px] px-4 sm:px-5 py-2 rounded-xl text-sm font-semibold bg-amber-500 text-slate-950 hover:bg-amber-400 disabled:opacity-40 disabled:cursor-not-allowed transition-colors flex items-center gap-2 whitespace-nowrap shrink-0"
            >
              {busy ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Analyse {overallPercent} %…</span>
                </>
              ) : (
                <>
                  <Play className="w-4 h-4" />
                  <span>Lancer ({waitingCount})</span>
                </>
              )}
            </button>
          ) : null}
        </div>
      </div>
    </div>
  );
};


