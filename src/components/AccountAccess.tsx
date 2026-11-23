import React, { useState } from 'react';
import { LockKeyhole, Mail, ShieldCheck } from 'lucide-react';
import { getSignedInUser, signInWithEmail, signUpWithEmail, CloudUser } from '../utils/cloudSync';

interface AccountAccessProps {
  onAuthenticated: (user: CloudUser) => void;
}

export const AccountAccess: React.FC<AccountAccessProps> = ({ onAuthenticated }) => {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setMessage('');
    setError('');
    try {
      if (mode === 'register') {
        const result = await signUpWithEmail(email.trim(), password);
        if (result.session) {
          const user = await getSignedInUser();
          if (user) onAuthenticated(user);
          else setMessage('Compte créé. Connecte-toi pour continuer.');
        } else {
          setMessage('Compte créé. Vérifie ta boîte mail pour confirmer ton adresse, puis connecte-toi.');
          setMode('login');
          setPassword('');
        }
      } else {
        const user = await signInWithEmail(email.trim(), password);
        onAuthenticated(user);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Connexion impossible. Réessaie.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="min-h-screen bg-[#0B0F17] text-slate-100 flex items-center justify-center px-4 py-8">
      <section className="w-full max-w-md rounded-3xl border border-slate-800 bg-[#121826] p-6 sm:p-8 shadow-2xl">
        <div className="mx-auto mb-5 flex h-14 w-14 items-center justify-center rounded-2xl border border-amber-500/30 bg-amber-500/10 text-amber-300">
          <ShieldCheck className="h-7 w-7" />
        </div>
        <div className="text-center">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-amber-300">Warden Coach</p>
          <h1 className="mt-2 text-2xl font-semibold text-white">
            {mode === 'login' ? 'Retrouver mes profils' : 'Créer mon compte'}
          </h1>
          <p className="mt-2 text-sm leading-relaxed text-slate-400">
            Un seul compte pour retrouver tes profils de jeu sur tes différents appareils.
          </p>
        </div>

        <form onSubmit={submit} className="mt-7 space-y-4">
          <label className="block">
            <span className="mb-1.5 block text-xs font-medium text-slate-300">Adresse e-mail</span>
            <span className="flex items-center gap-2 rounded-xl border border-slate-700 bg-slate-950 px-3">
              <Mail className="h-4 w-4 text-slate-500" />
              <input
                type="email"
                required
                autoComplete="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="toi@exemple.com"
                className="min-h-12 w-full bg-transparent text-sm text-white outline-none placeholder:text-slate-600"
              />
            </span>
          </label>

          <label className="block">
            <span className="mb-1.5 block text-xs font-medium text-slate-300">Mot de passe</span>
            <span className="flex items-center gap-2 rounded-xl border border-slate-700 bg-slate-950 px-3">
              <LockKeyhole className="h-4 w-4 text-slate-500" />
              <input
                type="password"
                required
                minLength={8}
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="8 caractères minimum"
                className="min-h-12 w-full bg-transparent text-sm text-white outline-none placeholder:text-slate-600"
              />
            </span>
          </label>

          {error && <p role="alert" className="rounded-xl border border-red-500/30 bg-red-500/10 p-3 text-xs text-red-200">{error}</p>}
          {message && <p role="status" className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 p-3 text-xs text-emerald-200">{message}</p>}

          <button
            type="submit"
            disabled={busy || !email.trim() || password.length < 8}
            className="min-h-12 w-full rounded-xl bg-amber-500 px-4 py-3 text-sm font-bold text-slate-950 transition hover:bg-amber-400 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {busy ? 'Patiente…' : mode === 'login' ? 'Se connecter' : 'Créer mon compte gratuit'}
          </button>
        </form>

        <button
          type="button"
          onClick={() => {
            setMode(mode === 'login' ? 'register' : 'login');
            setMessage('');
            setError('');
          }}
          className="mt-5 w-full py-2 text-sm text-slate-400 transition hover:text-white"
        >
          {mode === 'login' ? 'Pas encore de compte ? Créer un compte' : 'Déjà inscrit ? Se connecter'}
        </button>

        <p className="mt-5 text-center text-[11px] leading-relaxed text-slate-500">
          Synchronisation hébergée sur l’offre gratuite existante. Aucun abonnement ni paiement n’est activé.
        </p>
      </section>
    </main>
  );
};
