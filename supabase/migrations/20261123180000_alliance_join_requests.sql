-- Alliance self-registration requests with server-side officer approval.
-- Players can request any displayed rank, but only R4/R5 can approve it.
-- R5 can only be granted by the alliance owner (enforced in the RPC).

create table if not exists public.alliance_join_requests (
  id uuid primary key default gen_random_uuid(),
  alliance_id uuid not null references public.alliances(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  player_name text not null check (char_length(trim(player_name)) between 1 and 40),
  requested_rank text not null check (requested_rank in ('R1','R2','R3','R4','R5')),
  status text not null default 'pending' check (status in ('pending','approved','rejected')),
  reviewed_by uuid references auth.users(id),
  reviewed_at timestamptz,
  created_at timestamptz not null default now()
);

create unique index if not exists alliance_join_requests_one_pending_per_user
  on public.alliance_join_requests(alliance_id,user_id) where status='pending';
create index if not exists alliance_join_requests_officer_inbox
  on public.alliance_join_requests(alliance_id,status,created_at desc);

alter table public.alliance_join_requests enable row level security;
grant select on public.alliance_join_requests to authenticated;
revoke insert, update, delete on public.alliance_join_requests from anon, authenticated;

drop policy if exists "players and officers view join requests" on public.alliance_join_requests;
create policy "players and officers view join requests"
on public.alliance_join_requests for select to authenticated
using (user_id = (select auth.uid()) or (select private.can_manage_alliance(alliance_id)));

create or replace function private.submit_alliance_join_request(
  p_server_number text, p_alliance_tag text, p_player_name text, p_requested_rank text
) returns uuid
language plpgsql security definer set search_path = ''
as $$
declare v_alliance_id uuid; v_matches integer; v_request_id uuid;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if length(trim(p_server_number)) not between 1 and 6 or trim(p_server_number) !~ '^[0-9]+$' then raise exception 'Invalid server number'; end if;
  if length(trim(p_alliance_tag)) not between 1 and 12 then raise exception 'Alliance tag must contain 1 to 12 characters'; end if;
  if length(trim(p_player_name)) not between 1 and 40 then raise exception 'Player name must contain 1 to 40 characters'; end if;
  if p_requested_rank not in ('R1','R2','R3','R4','R5') then raise exception 'Invalid requested rank'; end if;
  select count(*), (array_agg(id))[1] into v_matches, v_alliance_id
  from public.alliances
  where server_number = trim(p_server_number)
    and upper(regexp_replace(alliance_name, '[^[:alnum:]]', '', 'g')) =
        upper(regexp_replace(trim(p_alliance_tag), '[^[:alnum:]]', '', 'g'));
  if v_matches = 0 then raise exception 'Alliance not found on this server. Check server and tag.'; end if;
  if v_matches > 1 then raise exception 'Several alliances match this tag. Ask an R5 for an invitation code.'; end if;
  if exists (select 1 from public.alliance_members m where m.alliance_id=v_alliance_id and m.user_id=auth.uid()) then
    raise exception 'You already belong to this alliance';
  end if;
  insert into public.alliance_join_requests(alliance_id,user_id,player_name,requested_rank)
  values(v_alliance_id,auth.uid(),trim(p_player_name),p_requested_rank)
  on conflict (alliance_id,user_id) where status='pending'
  do update set player_name=excluded.player_name, requested_rank=excluded.requested_rank, created_at=now()
  returning id into v_request_id;
  return v_request_id;
end;
$$;

create or replace function public.submit_alliance_join_request(
  p_server_number text, p_alliance_tag text, p_player_name text, p_requested_rank text
) returns uuid language sql set search_path = ''
as $$ select private.submit_alliance_join_request(p_server_number,p_alliance_tag,p_player_name,p_requested_rank); $$;

create or replace function private.review_alliance_join_request(
  p_request_id uuid, p_approve boolean, p_approved_rank text default 'R1'
) returns boolean
language plpgsql security definer set search_path = ''
as $$
declare v_req public.alliance_join_requests%rowtype; v_owner boolean;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  select * into v_req from public.alliance_join_requests where id=p_request_id for update;
  if not found then raise exception 'Join request not found'; end if;
  if v_req.status <> 'pending' then raise exception 'Join request already reviewed'; end if;
  if not private.can_manage_alliance(v_req.alliance_id) then raise exception 'Only R4/R5 can review requests'; end if;
  select exists(select 1 from public.alliances a where a.id=v_req.alliance_id and a.created_by=auth.uid()) into v_owner;
  if p_approve then
    if p_approved_rank not in ('R1','R2','R3','R4','R5') then raise exception 'Invalid approved rank'; end if;
    if p_approved_rank='R5' and not v_owner then raise exception 'Only alliance owner can assign R5'; end if;
    insert into public.alliance_members(alliance_id,user_id,game_rank)
    values(v_req.alliance_id,v_req.user_id,p_approved_rank)
    on conflict (alliance_id,user_id) do nothing;
    update public.alliance_join_requests set status='approved', reviewed_by=auth.uid(), reviewed_at=now() where id=p_request_id;
  else
    update public.alliance_join_requests set status='rejected', reviewed_by=auth.uid(), reviewed_at=now() where id=p_request_id;
  end if;
  return true;
end;
$$;

create or replace function public.review_alliance_join_request(
  p_request_id uuid, p_approve boolean, p_approved_rank text default 'R1'
) returns boolean language sql set search_path = ''
as $$ select private.review_alliance_join_request(p_request_id,p_approve,p_approved_rank); $$;

revoke all on function private.submit_alliance_join_request(text,text,text,text) from public, anon, authenticated;
revoke all on function private.review_alliance_join_request(uuid,boolean,text) from public, anon, authenticated;
-- Private schema is not exposed through PostgREST; authenticated execution is
-- required because the public SQL wrappers run with invoker privileges.
grant execute on function private.submit_alliance_join_request(text,text,text,text) to authenticated;
grant execute on function private.review_alliance_join_request(uuid,boolean,text) to authenticated;
grant execute on function public.submit_alliance_join_request(text,text,text,text) to authenticated;
grant execute on function public.review_alliance_join_request(uuid,boolean,text) to authenticated;
