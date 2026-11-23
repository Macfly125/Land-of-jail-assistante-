const SUPABASE_URL = 'https://cegtwxvidiiqgyfqzdix.supabase.co';
const SUPABASE_PUBLISHABLE_KEY = 'sb_publishable_bf7lfL-PK1g5Ct9eWfwrxQ_av8OC4hH';
const SESSION_KEY = 'loj_supabase_session_v1';
const USER_KEY = 'loj_supabase_user_v1';

export interface CloudUser {
  id: string;
  email?: string;
}

export interface CloudGameProfile {
  profile_id: string;
  profile_name: string;
  state: Record<string, unknown>;
  updated_at: string;
}

interface AuthSession {
  access_token: string;
  refresh_token: string;
  expires_at?: number;
  expires_in?: number;
  token_type?: string;
}

export interface LocalGameProfile {
  id: string;
  name: string;
  state: Record<string, unknown>;
  updatedAt?: string;
}

function readSession(): AuthSession | null {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    return raw ? (JSON.parse(raw) as AuthSession) : null;
  } catch {
    return null;
  }
}

function saveSession(session: AuthSession): void {
  const expiresAt = session.expires_at ?? (
    session.expires_in ? Math.floor(Date.now() / 1000) + session.expires_in : undefined
  );
  localStorage.setItem(SESSION_KEY, JSON.stringify({ ...session, expires_at: expiresAt }));
}

function clearSession(): void {
  localStorage.removeItem(SESSION_KEY);
  localStorage.removeItem(USER_KEY);
}

function saveUser(user: CloudUser): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

function readCachedUser(): CloudUser | null {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as CloudUser) : null;
  } catch {
    return null;
  }
}

async function parseResponse<T>(response: Response): Promise<T> {
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    const message = body.msg || body.message || body.error_description || body.error || 'Erreur de connexion au service cloud.';
    throw new Error(String(message));
  }
  return body as T;
}

async function authRequest<T>(path: string, body?: Record<string, unknown>): Promise<T> {
  const response = await fetch(`${SUPABASE_URL}/auth/v1/${path}`, {
    method: 'POST',
    headers: {
      apikey: SUPABASE_PUBLISHABLE_KEY,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body ?? {}),
  });
  return parseResponse<T>(response);
}

async function refreshSession(): Promise<AuthSession> {
  const current = readSession();
  if (!current?.refresh_token) throw new Error('Session expirée. Reconnecte-toi.');
  try {
    const next = await authRequest<AuthSession>('token?grant_type=refresh_token', {
      refresh_token: current.refresh_token,
    });
    saveSession(next);
    return next;
  } catch (error) {
    // Keep the cached session for local/offline access; the next online request can retry.
    throw error;
  }
}

async function getValidSession(): Promise<AuthSession> {
  const session = readSession();
  if (!session?.access_token) throw new Error('Connecte-toi pour synchroniser tes profils.');
  if (session.expires_at && session.expires_at * 1000 < Date.now() + 60_000) {
    return refreshSession();
  }
  return session;
}

export async function signUpWithEmail(email: string, password: string): Promise<{ session: AuthSession | null }> {
  const data = await authRequest<AuthSession & { user?: CloudUser }>('signup', { email, password });
  if (data.access_token && data.refresh_token) {
    saveSession(data);
    if (data.user) saveUser(data.user);
    return { session: data };
  }
  return { session: null };
}

export async function signInWithEmail(email: string, password: string): Promise<CloudUser> {
  const data = await authRequest<AuthSession & { user: CloudUser }>('token?grant_type=password', { email, password });
  saveSession(data);
  saveUser(data.user);
  return data.user;
}

export async function getSignedInUser(): Promise<CloudUser | null> {
  const session = readSession();
  if (!session?.access_token) return null;
  try {
    const valid = await getValidSession();
    const response = await fetch(`${SUPABASE_URL}/auth/v1/user`, {
      headers: {
        apikey: SUPABASE_PUBLISHABLE_KEY,
        Authorization: `Bearer ${valid.access_token}`,
      },
    });
    if (!response.ok) {
      if (response.status === 401) clearSession();
      return readCachedUser();
    }
    const user = (await response.json()) as CloudUser;
    saveUser(user);
    return user;
  } catch {
    // Keep the last authenticated identity so the app remains usable offline.
    return readCachedUser();
  }
}

export async function signOutCloud(): Promise<void> {
  const session = readSession();
  try {
    if (session?.access_token) {
      await fetch(`${SUPABASE_URL}/auth/v1/logout`, {
        method: 'POST',
        headers: {
          apikey: SUPABASE_PUBLISHABLE_KEY,
          Authorization: `Bearer ${session.access_token}`,
        },
      });
    }
  } catch {
    // Local logout must also work without a network connection.
  } finally {
    clearSession();
  }
}

async function databaseRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const session = await getValidSession();
  const headers = new Headers({
    apikey: SUPABASE_PUBLISHABLE_KEY,
    Authorization: `Bearer ${session.access_token}`,
    'Content-Type': 'application/json',
  });
  new Headers(init.headers).forEach((value, key) => headers.set(key, value));
  const response = await fetch(`${SUPABASE_URL}/rest/v1/${path}`, {
    ...init,
    headers,
  });
  if (response.status === 204) return undefined as T;
  return parseResponse<T>(response);
}

export async function loadCloudGameProfiles(): Promise<CloudGameProfile[]> {
  return databaseRequest<CloudGameProfile[]>(
    'game_account_profiles?select=profile_id,profile_name,state,updated_at&order=updated_at.asc'
  );
}

export async function saveCloudGameProfiles(userId: string, profiles: LocalGameProfile[]): Promise<void> {
  if (!profiles.length) return;
  const rows = profiles.map((profile) => {
    const state = { ...profile.state } as Record<string, unknown>;
    const analyses = Array.isArray(state.analyses)
      ? state.analyses.map((item) => {
          if (!item || typeof item !== 'object') return item;
          return { ...(item as Record<string, unknown>), thumbnailDataUrl: '' };
        })
      : state.analyses;
    return {
      user_id: userId,
      profile_id: profile.id,
      profile_name: profile.name.slice(0, 32),
      state: { ...state, analyses },
      updated_at: profile.updatedAt ?? new Date().toISOString(),
    };
  });

  await databaseRequest<void>('game_account_profiles?on_conflict=user_id,profile_id', {
    method: 'POST',
    headers: { Prefer: 'resolution=merge-duplicates,return=minimal' },
    body: JSON.stringify(rows),
  });
}

export async function deleteCloudGameProfile(profileId: string): Promise<void> {
  await databaseRequest<void>(
    `game_account_profiles?profile_id=eq.${encodeURIComponent(profileId)}`,
    { method: 'DELETE', headers: { Prefer: 'return=minimal' } }
  );
}
