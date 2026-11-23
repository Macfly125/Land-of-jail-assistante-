create table if not exists public.game_account_profiles (
  user_id uuid not null references auth.users(id) on delete cascade,
  profile_id text not null,
  profile_name text not null check (char_length(trim(profile_name)) between 1 and 32),
  state jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now(),
  primary key (user_id, profile_id)
);

alter table public.game_account_profiles enable row level security;

create policy "users read own game account profiles"
  on public.game_account_profiles for select to authenticated
  using ((select auth.uid()) = user_id);

create policy "users create own game account profiles"
  on public.game_account_profiles for insert to authenticated
  with check ((select auth.uid()) = user_id);

create policy "users update own game account profiles"
  on public.game_account_profiles for update to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

create policy "users delete own game account profiles"
  on public.game_account_profiles for delete to authenticated
  using ((select auth.uid()) = user_id);

grant select, insert, update, delete on public.game_account_profiles to authenticated;

create index if not exists game_account_profiles_updated_at_idx
  on public.game_account_profiles (user_id, updated_at desc);
