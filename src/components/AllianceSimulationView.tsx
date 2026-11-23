import React, { useEffect, useMemo, useState } from 'react';
import { Plus, Users, Trash2, CheckSquare, Swords } from 'lucide-react';
import { formatCompactNumber } from '../utils/storage';

type EventGoal = 'trial_cage' | 'crystal_clash' | 'sandsea' | 'svs';

interface AllianceMember {
  id: string;
  name: string;
  power: number;
  marchCapacity: number;
  firstHero: string;
  shieldbearers: number;
  bombers: number;
  shooters: number;
  selected: boolean;
}

const STORAGE_KEY = 'loj-alliance-simulation-v1';
const emptyMember = (index: number): AllianceMember => ({
  id: `member-${Date.now()}-${index}-${Math.random().toString(36).slice(2, 7)}`,
  name: `Membre ${index + 1}`,
  power: 0,
  marchCapacity: 0,
  firstHero: '',
  shieldbearers: 0,
  bombers: 0,
  shooters: 0,
  selected: true,
});

const numberValue = (value: string) => Math.max(0, Math.floor(Number(value) || 0));

export const AllianceSimulationView: React.FC = () => {
  const [members, setMembers] = useState<AllianceMember[]>(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      return saved ? (JSON.parse(saved) as AllianceMember[]) : [emptyMember(0)];
    } catch {
      return [emptyMember(0)];
    }
  });
  const [goal, setGoal] = useState<EventGoal>('trial_cage');

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(members));
    } catch {
      // Le simulateur reste utilisable même si le stockage local est indisponible.
    }
  }, [members]);

  const selected = useMemo(() => members.filter((member) => member.selected), [members]);
  const totals = useMemo(() => selected.reduce((acc, member) => ({
    power: acc.power + member.power,
    march: acc.march + member.marchCapacity,
    shieldbearers: acc.shieldbearers + member.shieldbearers,
    bombers: acc.bombers + member.bombers,
    shooters: acc.shooters + member.shooters,
  }), { power: 0, march: 0, shieldbearers: 0, bombers: 0, shooters: 0 }), [selected]);
  const troopTotal = totals.shieldbearers + totals.bombers + totals.shooters;
  const ratio = (value: number) => troopTotal > 0 ? Math.round((value / troopTotal) * 100) : 0;

  const updateMember = (id: string, patch: Partial<AllianceMember>) => {
    setMembers((current) => current.map((member) => member.id === id ? { ...member, ...patch } : member));
  };

  const guidance: Record<EventGoal, { title: string; text: string }> = {
    trial_cage: {
      title: 'Objectif : dégâts sur le boss',
      text: 'Comparez d’abord les joueurs capables d’apporter des bonus d’attaque, de dégâts infligés ou de réduction de défense ennemie. Ne sélectionnez pas un membre uniquement pour ses bonus de PV ou de défense : ils ne font pas progresser le score de dégâts.',
    },
    crystal_clash: {
      title: 'Objectif : prise et tenue des points',
      text: 'Construisez une équipe équilibrée entre première ligne et DPS. Comparez les capacités de marche et répartissez les membres entre attaque, renfort et défense selon les objectifs de votre alliance.',
    },
    sandsea: {
      title: 'Objectif : couverture et coordination',
      text: 'Répartissez les membres selon leur disponibilité, leur capacité de marche et leur puissance. Gardez des réserves pour renforcer les objectifs prioritaires au lieu de concentrer tout le monde sur un seul point.',
    },
    svs: {
      title: 'Objectif : préparation et guerre',
      text: 'Pendant la préparation, comparez les ressources et la puissance mobilisable. En phase de guerre, privilégiez les joueurs disponibles, les marches complètes et une répartition claire entre attaque, défense et renfort.',
    },
  };

  return (
    <div className="space-y-6">
      <section className="p-5 md:p-6 rounded-2xl bg-[#121826] border border-slate-800/90 space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-semibold text-slate-50">Simulation multi-membres</h1>
            <p className="text-sm text-slate-400 mt-1">Ajoutez les joueurs de votre alliance, cochez ceux à inclure et comparez leur contribution cumulée.</p>
          </div>
          <button type="button" onClick={() => setMembers((current) => [...current, emptyMember(current.length)])} className="min-h-[42px] px-4 py-2 rounded-xl bg-amber-500 text-slate-950 text-sm font-semibold inline-flex items-center justify-center gap-2">
            <Plus className="w-4 h-4" /> Ajouter un membre
          </button>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <label className="text-xs text-slate-400">Événement à simuler
            <select value={goal} onChange={(event) => setGoal(event.target.value as EventGoal)} className="mt-1.5 w-full min-h-[42px] rounded-xl bg-slate-900 border border-slate-700 px-3 text-sm text-slate-100">
              <option value="trial_cage">Trial Cage</option>
              <option value="crystal_clash">Crystal Clash</option>
              <option value="sandsea">Sandsea Conquest</option>
              <option value="svs">SvS / Supreme Honor</option>
            </select>
          </label>
          <div className="flex items-end gap-2">
            <button type="button" onClick={() => setMembers((current) => current.map((member) => ({ ...member, selected: true })))} className="min-h-[42px] px-3 rounded-xl border border-slate-700 text-xs text-slate-200">Tout sélectionner</button>
            <button type="button" onClick={() => setMembers((current) => current.map((member) => ({ ...member, selected: false })))} className="min-h-[42px] px-3 rounded-xl border border-slate-700 text-xs text-slate-200">Tout désélectionner</button>
          </div>
        </div>
      </section>

      <section className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <div className="p-4 rounded-xl bg-[#121826] border border-slate-800"><div className="text-xs text-slate-400">Membres sélectionnés</div><div className="text-2xl font-semibold text-amber-300">{selected.length} / {members.length}</div></div>
        <div className="p-4 rounded-xl bg-[#121826] border border-slate-800"><div className="text-xs text-slate-400">Puissance cumulée</div><div className="text-xl font-semibold text-slate-50">{formatCompactNumber(totals.power)}</div></div>
        <div className="p-4 rounded-xl bg-[#121826] border border-slate-800"><div className="text-xs text-slate-400">Capacité de marche</div><div className="text-xl font-semibold text-slate-50">{formatCompactNumber(totals.march)}</div></div>
        <div className="p-4 rounded-xl bg-[#121826] border border-slate-800"><div className="text-xs text-slate-400">Troupes renseignées</div><div className="text-xl font-semibold text-slate-50">{formatCompactNumber(troopTotal)}</div></div>
      </section>

      <section className="space-y-3">
        {members.map((member) => (
          <article key={member.id} className={`p-4 md:p-5 rounded-2xl border ${member.selected ? 'border-amber-500/50 bg-[#171f2f]' : 'border-slate-800 bg-[#121826]'} space-y-4`}>
            <div className="flex items-center gap-3">
              <input aria-label={`Inclure ${member.name}`} type="checkbox" checked={member.selected} onChange={(event) => updateMember(member.id, { selected: event.target.checked })} className="w-5 h-5 accent-amber-500" />
              <input aria-label="Nom du membre" value={member.name} onChange={(event) => updateMember(member.id, { name: event.target.value })} className="min-w-0 flex-1 bg-transparent border-b border-slate-700 px-1 py-1 text-sm font-semibold text-slate-100 focus:outline-none focus:border-amber-400" />
              <button type="button" aria-label="Supprimer ce membre" onClick={() => setMembers((current) => current.filter((item) => item.id !== member.id))} className="p-2 text-slate-500 hover:text-red-400"><Trash2 className="w-4 h-4" /></button>
            </div>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              <label className="text-xs text-slate-400">Puissance<input type="number" min="0" value={member.power || ''} onChange={(event) => updateMember(member.id, { power: numberValue(event.target.value) })} placeholder="Ex. 85000000" className="mt-1 w-full min-h-[40px] rounded-lg bg-slate-900 border border-slate-700 px-2.5 text-sm text-slate-100" /></label>
              <label className="text-xs text-slate-400">Capacité marche<input type="number" min="0" value={member.marchCapacity || ''} onChange={(event) => updateMember(member.id, { marchCapacity: numberValue(event.target.value) })} placeholder="Ex. 150000" className="mt-1 w-full min-h-[40px] rounded-lg bg-slate-900 border border-slate-700 px-2.5 text-sm text-slate-100" /></label>
              <label className="text-xs text-slate-400 col-span-2">Héros en premier slot<input value={member.firstHero} onChange={(event) => updateMember(member.id, { firstHero: event.target.value })} placeholder="Ex. Rin, Mireya, Ada..." className="mt-1 w-full min-h-[40px] rounded-lg bg-slate-900 border border-slate-700 px-2.5 text-sm text-slate-100" /></label>
            </div>
            <div className="grid grid-cols-3 gap-3">
              <label className="text-xs text-slate-400">Shieldbearers<input type="number" min="0" value={member.shieldbearers || ''} onChange={(event) => updateMember(member.id, { shieldbearers: numberValue(event.target.value) })} className="mt-1 w-full min-h-[40px] rounded-lg bg-slate-900 border border-slate-700 px-2.5 text-sm text-slate-100" /></label>
              <label className="text-xs text-slate-400">Bombers<input type="number" min="0" value={member.bombers || ''} onChange={(event) => updateMember(member.id, { bombers: numberValue(event.target.value) })} className="mt-1 w-full min-h-[40px] rounded-lg bg-slate-900 border border-slate-700 px-2.5 text-sm text-slate-100" /></label>
              <label className="text-xs text-slate-400">Shooters<input type="number" min="0" value={member.shooters || ''} onChange={(event) => updateMember(member.id, { shooters: numberValue(event.target.value) })} className="mt-1 w-full min-h-[40px] rounded-lg bg-slate-900 border border-slate-700 px-2.5 text-sm text-slate-100" /></label>
            </div>
          </article>
        ))}
        {members.length === 0 && <div className="p-6 rounded-xl border border-dashed border-slate-700 text-center text-sm text-slate-400">Aucun membre. Ajoutez les joueurs à comparer.</div>}
      </section>

      <section className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="p-5 rounded-2xl bg-[#121826] border border-slate-800 space-y-3">
          <h2 className="font-semibold text-slate-100 flex items-center gap-2"><Users className="w-4 h-4 text-amber-400" /> Répartition des troupes sélectionnées</h2>
          {(['shieldbearers', 'bombers', 'shooters'] as const).map((key) => {
            const label = key === 'shieldbearers' ? 'Shieldbearers' : key === 'bombers' ? 'Bombers' : 'Shooters';
            const value = totals[key];
            return <div key={key}><div className="flex justify-between text-xs text-slate-300"><span>{label}</span><span>{formatCompactNumber(value)} · {ratio(value)}%</span></div><div className="h-2 mt-1 rounded-full bg-slate-800 overflow-hidden"><div className="h-full bg-amber-400" style={{ width: `${ratio(value)}%` }} /></div></div>;
          })}
        </div>
        <div className="p-5 rounded-2xl bg-[#121826] border border-slate-800 space-y-3">
          <h2 className="font-semibold text-slate-100 flex items-center gap-2"><Swords className="w-4 h-4 text-amber-400" /> {guidance[goal].title}</h2>
          <p className="text-sm leading-relaxed text-slate-300">{guidance[goal].text}</p>
          <div className="text-xs text-slate-500">La simulation additionne uniquement les valeurs saisies. Elle ne lit pas automatiquement les comptes des autres membres et ne remplace pas les bonus réels des héros.</div>
        </div>
      </section>
    </div>
  );
};
