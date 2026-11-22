import React, { useEffect, useMemo, useState } from 'react';

type GorillaRun = {
  id: string;
  date: string;
  gorilla: string;
  damage: number;
  shield: number;
  bomber: number;
  shooter: number;
  heroes: string;
  robot: string;
  notes: string;
};

const STORAGE_KEY = 'loj_gorilla_damage_log_v1';
const GORILLAS = [
  'Space Gorilla',
  'Wise Gorilla',
  'Cyber Gorilla',
  'Primal Gorilla',
  'Armed Gorilla',
  'Treasure Gorilla',
  'Gorilla Warlord / High Chieftain',
];

const fieldClass =
  'w-full rounded-xl bg-slate-950 border border-slate-700 px-3 py-2.5 text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-amber-500';

const formatDamage = (value: number) =>
  new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(value);

export const GorillaDamageLog: React.FC = () => {
  const [runs, setRuns] = useState<GorillaRun[]>([]);
  const [gorilla, setGorilla] = useState('');
  const [damage, setDamage] = useState('');
  const [shield, setShield] = useState('0');
  const [bomber, setBomber] = useState('15');
  const [shooter, setShooter] = useState('85');
  const [heroes, setHeroes] = useState('');
  const [robot, setRobot] = useState('');
  const [notes, setNotes] = useState('');
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));

  useEffect(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved) as GorillaRun[];
        if (Array.isArray(parsed)) setRuns(parsed);
      }
    } catch {
      setRuns([]);
    }
  }, []);

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(runs));
    } catch {
      // The log remains usable for this session if browser storage is full.
    }
  }, [runs]);

  const ratioTotal = [shield, bomber, shooter].reduce(
    (sum, value) => sum + (Number(value) || 0),
    0
  );

  const bestByGorilla = useMemo(() => {
    const best = new Map<string, GorillaRun>();
    for (const run of runs) {
      const previous = best.get(run.gorilla);
      if (!previous || run.damage > previous.damage) best.set(run.gorilla, run);
    }
    return Array.from(best.values()).sort((a, b) => b.damage - a.damage);
  }, [runs]);

  const saveRun = (event: React.FormEvent) => {
    event.preventDefault();
    const parsedDamage = Number(damage.replace(/[\s,.]/g, ''));
    const percentages = [Number(shield), Number(bomber), Number(shooter)];
    if (!gorilla || !Number.isFinite(parsedDamage) || parsedDamage <= 0) return;
    if (percentages.some((value) => !Number.isFinite(value) || value < 0 || value > 100) || ratioTotal !== 100) return;

    setRuns((previous) => [
      {
        id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        date,
        gorilla,
        damage: parsedDamage,
        shield: percentages[0],
        bomber: percentages[1],
        shooter: percentages[2],
        heroes: heroes.trim(),
        robot: robot.trim(),
        notes: notes.trim(),
      },
      ...previous,
    ].slice(0, 100));

    setDamage('');
    setNotes('');
  };

  return (
    <section className="p-5 md:p-6 rounded-2xl bg-[#121826] border border-amber-500/25 space-y-5">
      <div>
        <h2 className="text-lg font-semibold text-slate-50">Journal des dégâts — Gorilla Legion</h2>
        <p className="mt-1 text-sm text-slate-400">
          Enregistre chaque essai sur ton appareil. Compare les dégâts pour un même gorille avant de changer de composition.
        </p>
      </div>

      <form onSubmit={saveRun} className="grid grid-cols-1 md:grid-cols-2 gap-3">
        <label className="text-xs text-slate-400 space-y-1">
          Gorille
          <select className={fieldClass} value={gorilla} onChange={(e) => setGorilla(e.target.value)} required>
            <option value="">Sélectionner le gorille</option>
            {GORILLAS.map((name) => <option key={name} value={name}>{name}</option>)}
          </select>
        </label>
        <label className="text-xs text-slate-400 space-y-1">
          Date de l'essai
          <input className={fieldClass} type="date" value={date} onChange={(e) => setDate(e.target.value)} required />
        </label>
        <label className="text-xs text-slate-400 space-y-1 md:col-span-2">
          Dégâts infligés
          <input className={fieldClass} inputMode="numeric" value={damage} onChange={(e) => setDamage(e.target.value)} placeholder="Ex. 125000000" required />
        </label>
        <div className="md:col-span-2">
          <div className="text-xs font-medium text-slate-400 mb-2">Ratio de troupes — total obligatoire : 100 %</div>
          <div className="grid grid-cols-3 gap-2">
            <label className="text-xs text-slate-400">Boucliers %<input className={fieldClass} type="number" min="0" max="100" value={shield} onChange={(e) => setShield(e.target.value)} /></label>
            <label className="text-xs text-slate-400">Bombardiers %<input className={fieldClass} type="number" min="0" max="100" value={bomber} onChange={(e) => setBomber(e.target.value)} /></label>
            <label className="text-xs text-slate-400">Tireurs %<input className={fieldClass} type="number" min="0" max="100" value={shooter} onChange={(e) => setShooter(e.target.value)} /></label>
          </div>
          <p className={`text-xs mt-1 ${ratioTotal === 100 ? 'text-emerald-400' : 'text-amber-400'}`}>Total : {ratioTotal} % {ratioTotal !== 100 && '— ajuste le ratio avant l’enregistrement.'}</p>
        </div>
        <label className="text-xs text-slate-400 space-y-1">
          Héros utilisés
          <input className={fieldClass} value={heroes} onChange={(e) => setHeroes(e.target.value)} placeholder="Ex. Ada, Edwin, ..." />
        </label>
        <label className="text-xs text-slate-400 space-y-1">
          Robot utilisé
          <input className={fieldClass} value={robot} onChange={(e) => setRobot(e.target.value)} placeholder="Nom / niveau du robot" />
        </label>
        <label className="text-xs text-slate-400 space-y-1 md:col-span-2">
          Bonus actifs, équipement ou remarques
          <textarea className={fieldClass} rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Note le bonus du jour et les conditions du test..." />
        </label>
        <button type="submit" disabled={!gorilla || !damage || ratioTotal !== 100} className="md:col-span-2 rounded-xl bg-amber-500 hover:bg-amber-400 disabled:opacity-40 disabled:cursor-not-allowed text-slate-950 font-semibold text-sm py-3">
          Enregistrer cet essai
        </button>
      </form>

      <div className="border-t border-slate-800 pt-4 space-y-3">
        <h3 className="text-sm font-semibold text-slate-100">Meilleur résultat par gorille</h3>
        {bestByGorilla.length === 0 ? (
          <p className="text-sm text-slate-500">Aucun essai enregistré pour le moment.</p>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            {bestByGorilla.map((run) => (
              <div key={run.gorilla} className="rounded-xl bg-slate-950/70 border border-slate-800 p-3">
                <div className="text-sm font-medium text-slate-200">{run.gorilla}</div>
                <div className="text-lg font-semibold text-amber-300 tabular-nums">{formatDamage(run.damage)}</div>
                <div className="text-xs text-slate-400">{run.shield}/{run.bomber}/{run.shooter} · {run.date}</div>
                {(run.heroes || run.robot) && <div className="text-xs text-slate-500 mt-1">{[run.heroes, run.robot].filter(Boolean).join(' · ')}</div>}
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="border-t border-slate-800 pt-4 space-y-2">
        <div className="flex items-center justify-between gap-3">
          <h3 className="text-sm font-semibold text-slate-100">Historique des essais ({runs.length})</h3>
          <button type="button" disabled={runs.length === 0} onClick={() => {
            if (window.confirm('Effacer tout le journal des dégâts Gorilla ?')) setRuns([]);
          }} className="text-xs text-rose-300 hover:text-rose-200 disabled:opacity-40">Effacer le journal</button>
        </div>
        {runs.slice(0, 12).map((run) => (
          <div key={run.id} className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-slate-950/50 px-3 py-2">
            <div className="min-w-0">
              <div className="text-xs font-medium text-slate-200">{run.gorilla} · {run.date}</div>
              <div className="text-[11px] text-slate-500">{run.shield}/{run.bomber}/{run.shooter} · {[run.heroes, run.robot].filter(Boolean).join(' · ') || 'Composition non précisée'}</div>
            </div>
            <div className="text-sm font-semibold text-amber-300 tabular-nums">{formatDamage(run.damage)}</div>
          </div>
        ))}
        {runs.length > 12 && <p className="text-xs text-slate-500">Les 12 essais les plus récents sont affichés. Les {runs.length} essais restent sauvegardés.</p>}
      </div>
      <p className="text-[11px] text-slate-500">Données conservées localement dans ce navigateur, sans envoi au serveur. Compare des essais du même gorille avec le même bonus et des conditions aussi proches que possible.</p>
    </section>
  );
};
