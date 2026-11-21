import React, { useEffect, useState } from 'react';
import { Bell, CheckCircle2, Clock, Play, RotateCcw } from 'lucide-react';
import { AppLanguage } from '../types/game';

interface RoutineTask {
  id: string;
  title: string;
  category: string;
  description: string;
  defaultMinutes: number;
  enabled: boolean;
  endsAtMs: number | null;
  pausedRemainingMs?: number | null;
  completedCount: number;
}

const ROUTINE_STORAGE_KEY = 'loj_offline_routine_v1';

const DEFAULT_TASKS: RoutineTask[] = [
  {
    id: 'gather_slot_1',
    title: 'Marche Récolte #1 (Nourriture / Bois / Acier / Essence)',
    category: 'Récolte',
    description: 'Lancer avant de quitter le jeu pour garder les files de récolte actives.',
    defaultMinutes: 120,
    enabled: true,
    endsAtMs: null,
    pausedRemainingMs: null,
    completedCount: 0,
  },
  {
    id: 'gather_slot_2',
    title: 'Marche Récolte #2 / Mine d’Alliance',
    category: 'Récolte',
    description: 'Deuxième slot d’exploitation pour ne laisser aucune file inactive.',
    defaultMinutes: 240,
    enabled: true,
    endsAtMs: null,
    pausedRemainingMs: null,
    completedCount: 0,
  },
  {
    id: 'troop_training',
    title: 'Entraînement des Casernes (Troupes)',
    category: 'Armée',
    description: 'Récupérer les troupes terminées et relancer la file selon votre stock.',
    defaultMinutes: 180,
    enabled: true,
    endsAtMs: null,
    pausedRemainingMs: null,
    completedCount: 0,
  },
  {
    id: 'alliance_auto_join',
    title: 'Aide Alliance, Dons Tech & Auto-Join Ralliement',
    category: 'Alliance',
    description: 'Renouveler l’Auto-Join et effectuer les dons technologiques.',
    defaultMinutes: 480,
    enabled: true,
    endsAtMs: null,
    pausedRemainingMs: null,
    completedCount: 0,
  },
  {
    id: 'daily_vip_arrest',
    title: 'VIP, Courrier, Gains d’Arrestation & Distributrice',
    category: 'Quotidien',
    description: 'Pack VIP quotidien, Haven Island / Vending Machine, coffres gratuits et mails.',
    defaultMinutes: 1440,
    enabled: true,
    endsAtMs: null,
    pausedRemainingMs: null,
    completedCount: 0,
  },
  {
    id: 'radar_tasks',
    title: 'Missions Radar',
    category: 'Missions',
    description: 'Actualisation des tâches Radar à vider avant la limite de stockage.',
    defaultMinutes: 360,
    enabled: false,
    endsAtMs: null,
    pausedRemainingMs: null,
    completedCount: 0,
  },
  {
    id: 'shield_alert',
    title: 'Bouclier de protection (Alerte avant expiration)',
    category: 'Défense',
    description: 'Rappel avant la fin du bouclier de 8h ou 24h lors de SvS / événements.',
    defaultMinutes: 480,
    enabled: true,
    endsAtMs: null,
    pausedRemainingMs: null,
    completedCount: 0,
  },
];

function loadRoutineTasks(): RoutineTask[] {
  try {
    const raw = localStorage.getItem(ROUTINE_STORAGE_KEY);
    if (!raw) return DEFAULT_TASKS;
    const parsed = JSON.parse(raw) as RoutineTask[];
    if (!Array.isArray(parsed) || parsed.length === 0) return DEFAULT_TASKS;
    const now = Date.now();
    return parsed.map((t) => {
      if (!t.enabled && t.endsAtMs) {
        return {
          ...t,
          pausedRemainingMs: Math.max(0, t.endsAtMs - now),
          endsAtMs: null,
        };
      }
      return t;
    });
  } catch {
    return DEFAULT_TASKS;
  }
}

function formatCountdown(msRemaining: number): string {
  if (msRemaining <= 0) return '00:00:00';
  const totalSec = Math.floor(msRemaining / 1000);
  const h = Math.floor(totalSec / 3600);
  const m = Math.floor((totalSec % 3600) / 60);
  const s = totalSec % 60;
  return [h, m, s].map((v) => String(v).padStart(2, '0')).join(':');
}

export const OfflineRoutineView: React.FC<{ language?: AppLanguage }> = () => {
  const [tasks, setTasks] = useState<RoutineTask[]>(loadRoutineTasks);
  const [nowMs, setNowMs] = useState(() => Date.now());
  const [notifPerm, setNotifPerm] = useState<string>(() =>
    typeof Notification !== 'undefined' ? Notification.permission : 'unsupported'
  );

  useEffect(() => {
    try {
      localStorage.setItem(ROUTINE_STORAGE_KEY, JSON.stringify(tasks));
    } catch {
      // ignore
    }
  }, [tasks]);

  useEffect(() => {
    const id = setInterval(() => {
      const current = Date.now();
      setNowMs(current);
      if (typeof Notification !== 'undefined' && Notification.permission === 'granted') {
        tasks.forEach((t) => {
          if (t.enabled && t.endsAtMs && t.endsAtMs <= current && t.endsAtMs > current - 1500) {
            try {
              new Notification('Lands of Jail · Rappel hors-jeu', {
                body: `${t.title} est prêt à être relancé / collecté !`,
              });
            } catch {
              // ignore
            }
          }
        });
      }
    }, 1000);
    return () => clearInterval(id);
  }, [tasks]);

  const requestNotifs = async () => {
    if (typeof Notification === 'undefined') return;
    const res = await Notification.requestPermission();
    setNotifPerm(res);
  };

  const toggleTask = (id: string) => {
    const now = Date.now();
    setTasks((prev) =>
      prev.map((t) => {
        if (t.id !== id) return t;
        if (t.enabled) {
          const frozenRemaining =
            t.endsAtMs !== null
              ? Math.max(0, t.endsAtMs - now)
              : (t.pausedRemainingMs ?? null);
          return {
            ...t,
            enabled: false,
            endsAtMs: null,
            pausedRemainingMs: frozenRemaining,
          };
        }
        if (t.pausedRemainingMs !== null && t.pausedRemainingMs !== undefined) {
          return {
            ...t,
            enabled: true,
            endsAtMs: now + t.pausedRemainingMs,
            pausedRemainingMs: null,
          };
        }
        return {
          ...t,
          enabled: true,
          pausedRemainingMs: null,
        };
      })
    );
  };

  const startTimer = (id: string, minutes?: number) => {
    setTasks((prev) =>
      prev.map((t) => {
        if (t.id !== id) return t;
        const mins = minutes ?? t.defaultMinutes;
        return {
          ...t,
          enabled: true,
          defaultMinutes: mins,
          endsAtMs: Date.now() + mins * 60_000,
          pausedRemainingMs: null,
        };
      })
    );
  };

  const completeAndRestart = (id: string) => {
    setTasks((prev) =>
      prev.map((t) =>
        t.id === id
          ? {
              ...t,
              enabled: true,
              completedCount: t.completedCount + 1,
              endsAtMs: Date.now() + t.defaultMinutes * 60_000,
              pausedRemainingMs: null,
            }
          : t
      )
    );
  };

  const startAllEnabled = () => {
    const base = Date.now();
    setTasks((prev) =>
      prev.map((t) =>
        t.enabled
          ? { ...t, endsAtMs: base + t.defaultMinutes * 60_000, pausedRemainingMs: null }
          : t
      )
    );
  };

  const activeTimersCount = tasks.filter((t) => t.enabled && t.endsAtMs && t.endsAtMs > nowMs).length;
  const expiredCount = tasks.filter((t) => t.enabled && t.endsAtMs && t.endsAtMs <= nowMs).length;

  return (
    <div className="space-y-4">
      <div className="p-4 rounded-2xl bg-[#121826] border border-slate-800/90 flex flex-wrap items-center justify-between gap-3">
        <div className="text-xs text-slate-300 font-mono-tabular">
          {activeTimersCount} minuteur(s) actif(s) · {expiredCount} tâche(s) à collecter
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {notifPerm !== 'granted' && notifPerm !== 'unsupported' && (
            <button
              type="button"
              onClick={() => void requestNotifs()}
              className="min-h-[40px] px-3.5 py-1.5 rounded-xl text-xs font-medium bg-slate-800 text-slate-100 hover:bg-slate-700 inline-flex items-center gap-1.5"
            >
              <Bell className="w-3.5 h-3.5 text-amber-400" />
              <span>Activer alertes</span>
            </button>
          )}
          <button
            type="button"
            onClick={startAllEnabled}
            className="min-h-[40px] px-4 py-1.5 rounded-xl text-xs font-semibold bg-amber-500 text-slate-950 hover:bg-amber-400 inline-flex items-center gap-1.5"
          >
            <Play className="w-3.5 h-3.5" />
            <span>Démarrer tous les cycles actifs</span>
          </button>
        </div>
      </div>

      {/* Liste des tâches & minuteurs */}
      <section className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {tasks.map((task) => {
          const isPaused = !task.enabled;
          const remaining = task.enabled
            ? task.endsAtMs
              ? task.endsAtMs - nowMs
              : null
            : (task.pausedRemainingMs ?? null);
          const isDue = task.enabled && remaining !== null && remaining <= 0;
          const isRunning = task.enabled && remaining !== null && remaining > 0;

          return (
            <div
              key={task.id}
              className={`p-5 rounded-2xl bg-[#121826] border space-y-3 ${
                isDue
                  ? 'border-emerald-500/60'
                  : isRunning
                    ? 'border-amber-500/40'
                    : 'border-slate-800/90'
              }`}
            >
              <div className="flex items-start justify-between gap-3">
                <div className="space-y-1">
                  <div className="text-[11px] font-semibold uppercase tracking-wider text-amber-400">
                    {task.category}
                  </div>
                  <h3 className="text-sm font-semibold text-slate-100">{task.title}</h3>
                  <p className="text-xs text-slate-400 leading-relaxed">{task.description}</p>
                </div>
                <button
                  type="button"
                  onClick={() => toggleTask(task.id)}
                  className={`px-3 py-1 rounded-lg text-xs font-semibold shrink-0 transition-colors ${
                    task.enabled
                      ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                      : 'bg-slate-800 text-amber-300 border border-amber-500/40'
                  }`}
                >
                  {task.enabled ? 'Actif' : 'Pause'}
                </button>
              </div>

              <div className="flex items-center justify-between gap-3 pt-2 border-t border-slate-800/70">
                <div className="flex items-center gap-2 font-mono-tabular text-base font-semibold">
                  <Clock
                    className={`w-4 h-4 ${
                      isDue ? 'text-emerald-400' : isRunning ? 'text-amber-400' : 'text-slate-500'
                    }`}
                  />
                  <span
                    className={
                      isDue ? 'text-emerald-300' : isRunning ? 'text-slate-100' : 'text-slate-400'
                    }
                  >
                    {isDue
                      ? 'PRÊT / À COLLECTER'
                      : isPaused && remaining !== null
                        ? `${formatCountdown(remaining)} · EN PAUSE`
                        : remaining !== null
                          ? formatCountdown(remaining)
                          : `${Math.round((task.defaultMinutes / 60) * 10) / 10}h (${task.defaultMinutes} min)`}
                  </span>
                </div>
                <div className="text-[11px] font-mono-tabular text-slate-400">
                  {task.completedCount}× validé
                </div>
              </div>

              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex flex-wrap items-center gap-1">
                  {[30, 120, 240, 480, 1440].map((mins) => (
                    <button
                      key={mins}
                      type="button"
                      onClick={() => startTimer(task.id, mins)}
                      className={`px-2 py-1 rounded-lg text-[11px] font-mono-tabular border transition-colors ${
                        task.defaultMinutes === mins
                          ? 'bg-amber-500/15 border-amber-500/50 text-amber-300'
                          : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-slate-200'
                      }`}
                    >
                      {mins >= 60 ? `${mins / 60}h` : `${mins}m`}
                    </button>
                  ))}
                </div>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => startTimer(task.id)}
                    className="min-h-[34px] px-2.5 py-1 rounded-lg text-xs font-medium bg-slate-800 text-slate-200 hover:bg-slate-700 inline-flex items-center gap-1"
                  >
                    <RotateCcw className="w-3 h-3" />
                    <span>Relancer</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => completeAndRestart(task.id)}
                    className="min-h-[34px] px-2.5 py-1 rounded-lg text-xs font-semibold bg-emerald-500/20 border border-emerald-500/40 text-emerald-300 hover:bg-emerald-500/30 inline-flex items-center gap-1"
                  >
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    <span>Collecté</span>
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </section>
    </div>
  );
};
