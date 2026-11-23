create or replace function private.set_alliance_member_rank(
  p_alliance_id uuid,
  p_target_user_id uuid,
  p_new_rank text
)
returns boolean
language plpgsql
security definer
set search_path to ''
as $function$
declare
  v_requester_rank text;
  v_target_rank text;
  v_is_developer boolean;
  v_updated integer;
begin
  if auth.uid() is null then
    raise exception 'Authentication required';
  end if;

  if p_alliance_id is null or p_target_user_id is null or p_new_rank not in ('R1','R2','R3','R4','R5') then
    raise exception 'Invalid alliance, member or rank';
  end if;

  if p_target_user_id = auth.uid() then
    raise exception 'You cannot change your own alliance rank';
  end if;

  perform 1 from public.alliances where id = p_alliance_id for update;
  if not found then
    raise exception 'Alliance not found';
  end if;

  select m.game_rank into v_requester_rank
  from public.alliance_members m
  where m.alliance_id = p_alliance_id and m.user_id = auth.uid();

  v_is_developer := private.is_app_developer();

  if not (v_requester_rank = 'R5' or (v_is_developer and v_requester_rank = 'R4')) then
    raise exception 'Only the current R5 or the app developer serving as R4 can change member ranks';
  end if;

  select m.game_rank into v_target_rank
  from public.alliance_members m
  where m.alliance_id = p_alliance_id and m.user_id = p_target_user_id
  for update;

  if not found then
    raise exception 'Target player is not a member of this alliance';
  end if;

  if v_target_rank = 'R5' then
    raise exception 'The current R5 cannot be demoted directly. Assign R5 to an R4 member instead.';
  end if;

  if p_new_rank = 'R5' then
    if v_target_rank <> 'R4' then
      raise exception 'Only an R4 member can be promoted to R5';
    end if;

    update public.alliance_members
    set game_rank = 'R4'
    where alliance_id = p_alliance_id and game_rank = 'R5';

    update public.alliance_members
    set game_rank = 'R5'
    where alliance_id = p_alliance_id
      and user_id = p_target_user_id
      and game_rank = 'R4';
    get diagnostics v_updated = row_count;

    if v_updated <> 1 then
      raise exception 'Unable to promote the selected R4 member';
    end if;

    update public.alliances
    set created_by = p_target_user_id
    where id = p_alliance_id;
  else
    update public.alliance_members
    set game_rank = p_new_rank
    where alliance_id = p_alliance_id
      and user_id = p_target_user_id
      and game_rank <> p_new_rank;

    get diagnostics v_updated = row_count;
    if v_updated <> 1 then
      raise exception 'Rank was not changed';
    end if;
  end if;

  return true;
end;
$function$;

revoke all on function private.set_alliance_member_rank(uuid, uuid, text) from public, anon, authenticated;

create or replace function public.set_alliance_member_rank(
  p_alliance_id uuid,
  p_target_user_id uuid,
  p_new_rank text
)
returns boolean
language sql
security invoker
set search_path to ''
as $function$
  select private.set_alliance_member_rank(p_alliance_id, p_target_user_id, p_new_rank);
$function$;

revoke all on function public.set_alliance_member_rank(uuid, uuid, text) from public, anon;
grant execute on function public.set_alliance_member_rank(uuid, uuid, text) to authenticated;