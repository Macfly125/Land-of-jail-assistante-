-- Collaborative verification of the latest synced player profile.
-- Two distinct alliance members mark a profile as verified; at most five
-- distinct confirmations are retained for one profile revision.

create table if not exists public.alliance_profile_verifications (
  id uuid primary key default gen_random_uuid(),
  alliance_id uuid not null references public.alliances(id) on delete cascade,
  member_user_id uuid not null references auth.users(id) on delete cascade,
  verifier_user_id uuid not null references auth.users(id) on delete cascade,
  profile_updated_at timestamptz not null,
  verified_at timestamptz not null default now(),
  constraint alliance_profile_verifications_no_self
    check (member_user_id <> verifier_user_id),
  constraint alliance_profile_verifications_unique
    unique (alliance_id, member_user_id, profile_updated_at, verifier_user_id)
);

create index if not exists alliance_profile_verifications_member_revision_idx
  on public.alliance_profile_verifications
    (alliance_id, member_user_id, profile_updated_at, verified_at desc);

alter table public.alliance_profile_verifications enable row level security;
revoke all on public.alliance_profile_verifications from anon, authenticated;

create or replace function private.verify_alliance_member_profile(
  p_alliance_id uuid,
  p_member_user_id uuid
) returns jsonb
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_profile_updated_at timestamptz;
  v_count integer;
  v_inserted integer;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if p_alliance_id is null or p_member_user_id is null then
    raise exception 'Alliance and member are required';
  end if;
  if p_member_user_id = auth.uid() then
    raise exception 'You cannot verify your own profile';
  end if;
  if not exists (
    select 1 from public.alliance_members m
    where m.alliance_id = p_alliance_id and m.user_id = auth.uid()
  ) then raise exception 'Only alliance members can verify profiles'; end if;
  if not exists (
    select 1 from public.alliance_members m
    where m.alliance_id = p_alliance_id and m.user_id = p_member_user_id
  ) then raise exception 'The selected player is not in this alliance'; end if;

  select p.updated_at into v_profile_updated_at
  from public.player_profiles p
  where p.user_id = p_member_user_id;

  if v_profile_updated_at is null then
    raise exception 'This player has no synchronized profile to verify';
  end if;

  perform pg_catalog.pg_advisory_xact_lock(
    pg_catalog.hashtext(p_alliance_id::text),
    pg_catalog.hashtext(p_member_user_id::text)
  );

  select count(*) into v_count
  from public.alliance_profile_verifications v
  where v.alliance_id = p_alliance_id
    and v.member_user_id = p_member_user_id
    and v.profile_updated_at = v_profile_updated_at;

  if exists (
    select 1 from public.alliance_profile_verifications v
    where v.alliance_id = p_alliance_id
      and v.member_user_id = p_member_user_id
      and v.profile_updated_at = v_profile_updated_at
      and v.verifier_user_id = auth.uid()
  ) then
    return jsonb_build_object('count', v_count, 'verified', v_count >= 2, 'already_confirmed', true);
  end if;

  if v_count >= 5 then
    raise exception 'This profile already has the maximum of five confirmations';
  end if;

  insert into public.alliance_profile_verifications(
    alliance_id, member_user_id, verifier_user_id, profile_updated_at
  ) values (p_alliance_id, p_member_user_id, auth.uid(), v_profile_updated_at)
  on conflict (alliance_id, member_user_id, profile_updated_at, verifier_user_id) do nothing;

  get diagnostics v_inserted = row_count;
  select count(*) into v_count
  from public.alliance_profile_verifications v
  where v.alliance_id = p_alliance_id
    and v.member_user_id = p_member_user_id
    and v.profile_updated_at = v_profile_updated_at;

  return jsonb_build_object(
    'count', v_count,
    'verified', v_count >= 2,
    'already_confirmed', v_inserted = 0
  );
end;
$function$;

create or replace function public.verify_alliance_member_profile(
  p_alliance_id uuid,
  p_member_user_id uuid
) returns jsonb
language sql
security invoker
set search_path = ''
as $function$
  select private.verify_alliance_member_profile(p_alliance_id, p_member_user_id);
$function$;

create or replace function private.fetch_alliance_profile_verifications(
  p_alliance_id uuid
) returns jsonb
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_result jsonb;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if p_alliance_id is null or not exists (
    select 1 from public.alliance_members m
    where m.alliance_id = p_alliance_id and m.user_id = auth.uid()
  ) then raise exception 'Only alliance members can view profile verifications'; end if;

  select coalesce(jsonb_agg(
    jsonb_build_object(
      'member_user_id', m.user_id,
      'profile_updated_at', p.updated_at,
      'verification_count', coalesce(v.confirmation_count, 0),
      'verified', coalesce(v.confirmation_count, 0) >= 2,
      'confirmations', coalesce(v.confirmations, '[]'::jsonb)
    ) order by m.joined_at asc
  ), '[]'::jsonb)
  into v_result
  from public.alliance_members m
  left join public.player_profiles p on p.user_id = m.user_id
  left join lateral (
    select
      count(*)::integer as confirmation_count,
      jsonb_agg(
        jsonb_build_object(
          'verifier_user_id', v.verifier_user_id,
          'verifier_name', coalesce(nullif(vp.player_name, ''), 'Membre'),
          'verified_at', v.verified_at
        ) order by v.verified_at asc
      ) as confirmations
    from public.alliance_profile_verifications v
    left join public.player_profiles vp on vp.user_id = v.verifier_user_id
    where v.alliance_id = p_alliance_id
      and v.member_user_id = m.user_id
      and v.profile_updated_at = p.updated_at
  ) v on true
  where m.alliance_id = p_alliance_id;

  return v_result;
end;
$function$;

create or replace function public.fetch_alliance_profile_verifications(
  p_alliance_id uuid
) returns jsonb
language sql
security invoker
set search_path = ''
as $function$
  select private.fetch_alliance_profile_verifications(p_alliance_id);
$function$;

revoke all on function private.verify_alliance_member_profile(uuid, uuid) from public, anon, authenticated;
revoke all on function private.fetch_alliance_profile_verifications(uuid) from public, anon, authenticated;
revoke all on function public.verify_alliance_member_profile(uuid, uuid) from public, anon;
revoke all on function public.fetch_alliance_profile_verifications(uuid) from public, anon;
grant execute on function private.verify_alliance_member_profile(uuid, uuid) to authenticated;
grant execute on function private.fetch_alliance_profile_verifications(uuid) to authenticated;
grant execute on function public.verify_alliance_member_profile(uuid, uuid) to authenticated;
grant execute on function public.fetch_alliance_profile_verifications(uuid) to authenticated;
