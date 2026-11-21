import React, { useState } from 'react';
import { Check, Copy, Download, X, Smartphone } from 'lucide-react';
import { ANDROID_KOTLIN_FILES } from '../data/androidSourceBundle';

interface AndroidKotlinModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const AndroidKotlinModal: React.FC<AndroidKotlinModalProps> = ({ isOpen, onClose }) => {
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [copied, setCopied] = useState(false);

  if (!isOpen) return null;

  const activeFile = ANDROID_KOTLIN_FILES[selectedIndex];

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(activeFile.content);
      setCopied(true);
      setTimeout(() => setCopied(false), 1800);
    } catch {
      // ignore clipboard errors
    }
  };

  const handleDownload = () => {
    const fileName = activeFile.path.split('/').pop() || 'Source.kt';
    const blob = new Blob([activeFile.content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="android-code-title"
    >
      <div className="w-full max-w-4xl bg-[#121826] border border-slate-800 rounded-2xl shadow-2xl max-h-[88vh] flex flex-col overflow-hidden">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <Smartphone className="w-5 h-5 text-amber-400 shrink-0" />
            <div>
              <h2 id="android-code-title" className="text-base font-semibold text-slate-100">
                Sources natives Android (Kotlin & Jetpack Compose)
              </h2>
              <p className="text-xs text-slate-400">
                Module complet inclus dans le dépôt (/android) avec réception ACTION_SEND uniquement
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fermer"
            className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl text-slate-400 hover:text-slate-100 hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="flex flex-col md:flex-row flex-1 min-h-0">
          <div className="md:w-64 border-b md:border-b-0 md:border-r border-slate-800 p-3 flex md:flex-col gap-1.5 overflow-x-auto md:overflow-y-auto shrink-0">
            {ANDROID_KOTLIN_FILES.map((file, idx) => {
              const shortName = file.path.split('/').pop();
              const active = idx === selectedIndex;
              return (
                <button
                  key={file.path}
                  type="button"
                  onClick={() => setSelectedIndex(idx)}
                  className={`text-left px-3 py-2.5 rounded-xl text-xs transition-colors whitespace-nowrap md:whitespace-normal shrink-0 ${
                    active
                      ? 'bg-amber-500/15 text-amber-300 border border-amber-500/30 font-medium'
                      : 'text-slate-400 hover:text-slate-100 hover:bg-slate-800/60 border border-transparent'
                  }`}
                >
                  <div className="font-mono">{shortName}</div>
                  <div className="text-[11px] text-slate-500 mt-0.5 hidden md:block truncate">
                    {file.path}
                  </div>
                </button>
              );
            })}
          </div>

          <div className="flex-1 flex flex-col min-w-0 min-h-0">
            <div className="px-4 py-3 border-b border-slate-800/80 flex flex-wrap items-center justify-between gap-3 bg-slate-900/40">
              <div className="min-w-0">
                <div className="text-xs font-mono text-slate-300 truncate">{activeFile.path}</div>
                <div className="text-xs text-slate-400 mt-0.5">{activeFile.description}</div>
              </div>
              <div className="flex items-center gap-2 shrink-0">
                <button
                  type="button"
                  onClick={handleCopy}
                  className="min-h-[38px] px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-800 text-slate-200 hover:bg-slate-700 transition-colors flex items-center gap-1.5 whitespace-nowrap"
                >
                  {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  <span>{copied ? 'Copié' : 'Copier'}</span>
                </button>
                <button
                  type="button"
                  onClick={handleDownload}
                  className="min-h-[38px] px-3 py-1.5 rounded-lg text-xs font-medium bg-amber-500 text-slate-950 hover:bg-amber-400 transition-colors flex items-center gap-1.5 whitespace-nowrap"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>Télécharger</span>
                </button>
              </div>
            </div>

            <pre className="flex-1 overflow-auto p-4 text-xs font-mono text-slate-200 leading-relaxed bg-[#090D14]">
              <code>{activeFile.content}</code>
            </pre>
          </div>
        </div>
      </div>
    </div>
  );
};
