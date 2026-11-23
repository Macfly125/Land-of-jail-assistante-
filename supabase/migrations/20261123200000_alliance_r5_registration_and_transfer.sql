-- Make initial R5 registration atomic and support a secure R5 handover.
create or replace function private.create_alliance_as_r5(
  p_server_number text,
  p_alliance_tag text
) returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_alliance_id uuid;
  v_server text := trim(p_server_number);
  v_tag text := trim(p_alliance_tag);
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if length(v_server) not between 1 and 6 or v_server !~ '^[0-9]+$' then
    raise exception 'Invalid server number';
  end if;
  if length(v_tag) not between 1 and 12 then
    raise exception 'Alliance tag must contain 1 to 12 characters';
  end if;
  if exists (select 1 from public.alliance_members where user_id = auth.uid()) then
    raise exception 'You already belong to a cloud alliance';
  end if;
  perform pg_catalog.pg_advisory_xact_lock(
    pg_catalog.hashtext(v_server),
    pg_catalog.hashtext(upper(regexp_replace(v_tag, '[^[:alnum:]]', '', 'g')))
  );
  if exists (
    select 1 from public.alliances
    where server_number = v_server
      and upper(regexp_replace(alliance_name, '[^[:alnum:]]', '', 'g')) =
          upper(regexp_replace(v_tag, '[^[:alnum:]]', '', 'g'))
  ) then
    raise exception 'This alliance already has a cloud space. Join it with an invitation, then ask the current R5 to transfer leadership.';
  end if;
  insert into public.alliances(server_number, alliance_name, created_by)
  values (v_server, v_tag, auth.uid())
  returning id into v_alliance_id;
  insert into public.alliance_members(alliance_id, user_id, game_rank)
  values (v_alliance_id, auth.uid(), 'R5');
  return v_alliance_id;
end;
$$;

create or replace function public.create_alliance_as_r5(p_server_number text, p_alliance_tag text)
returns uuid language sql set search_path = ''
as $$ select private.create_alliance_as_r5(p_server_number, p_alliance_tag); $$;

create or replace function private.transfer_alliance_r5(p_alliance_id uuid, p_new_r5_user_id uuid)
returns boolean language plpgsql security definer set search_path = ''
as $$
declare v_current_owner uuid; v_updated integer;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if p_new_r5_user_id is null or p_new_r5_user_id = auth.uid() then raise exception 'Choose another R4 member'; end if;
  select created_by into v_current_owner from public.alliances where id = p_alliance_id for update;
  if not found then raise exception 'Alliance not found'; end if;
  if v_current_owner <> auth.uid() then raise exception 'Only the current R5 can transfer alliance leadership'; end if;
  if not exists (select 1 from public.alliance_members where alliance_id=p_alliance_id and user_id=auth.uid() and game_rank='R5') then
    raise exception 'Your current alliance rank is not R5';
  end if;
  if not exists (select 1 from public.alliance_members where alliance_id=p_alliance_id and user_id=p_new_r5_user_id and game_rank='R4') then
    raise exception 'The selected player must already be an R4 member';
  end if;
  update public.alliance_members set game_rank='R4' where alliance_id=p_alliance_id and game_rank='R5';
  update public.alliance_members set game_rank='R5'
  where alliance_id=p_alliance_id and user_id=p_new_r5_user_id and game_rank='R4';
  get diagnostics v_updated = row_count;
  if v_updated <> 1 then raise exception 'The selected R4 could not be promoted'; end if;
  update public.alliances set created_by=p_new_r5_user_id where id=p_alliance_id;
  return true;
end;
$$;

create or replace function public.transfer_alliance_r5(p_alliance_id uuid, p_new_r5_user_id uuid)
returns boolean language sql set search_path = ''
as $$ select private.transfer_alliance_r5(p_alliance_id, p_new_r5_user_id); $$;

create or replace function private.review_alliance_join_request(
  p_request_id uuid, p_approve boolean, p_approved_rank text default 'R1'
) returns boolean language plpgsql security definer set search_path = ''
as $$
declare v_req public.alliance_join_requests%rowtype;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  select * into v_req from public.alliance_join_requests where id=p_request_id for update;
  if not found then raise exception 'Join request not found'; end if;
  if v_req.status <> 'pending' then raise exception 'Join request already reviewed'; end if;
  if not private.can_manage_alliance(v_req.alliance_id) then raise exception 'Only R4/R5 can review requests'; end if;
  if p_approve then
    if p_approved_rank not in ('R1','R2','R3','R4') then
      raise exception 'R5 must be transferred by the current R5 after the player joins as R4';
    end if;
    insert into public.alliance_members(alliance_id,user_id,game_rank)
    values(v_req.alliance_id,v_req.user_id,p_approved_rank)
    on conflict (alliance_id,user_id) do nothing;
    update public.alliance_join_requests set status='approved', reviewed_by=auth.uid(), reviewed_at=now()
    where id=p_request_id;
  else
    update public.alliance_join_requests set status='rejected', reviewed_by=auth.uid(), reviewed_at=now()
    where id=p_request_id;
  end if;
  return true;
end;
$$;

revoke all on function private.create_alliance_as_r5(text,text) from public, anon, authenticated;
revoke all on function private.transfer_alliance_r5(uuid,uuid) from public, anon, authenticated;
grant execute on function private.create_alliance_as_r5(text,text) to authenticated;
grant execute on function private.transfer_alliance_r5(uuid,uuid) to authenticated;
revoke all on function public.create_alliance_as_r5(text,text) from public, anon;
revoke all on function public.transfer_alliance_r5(uuid,uuid) from public, anon;
grant execute on function public.create_alliance_as_r5(text,text) to authenticated;
grant execute on function public.transfer_alliance_r5(uuid,uuid) to authenticated;
