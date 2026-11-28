-- Reject pin changes from users who are not members of the alliance.
create or replace function private.set_alliance_message_pinned(
  p_message_id uuid,
  p_is_pinned boolean
) returns boolean
language plpgsql
security definer
set search_path to ''
as $function$
declare
  v_alliance_id uuid;
  v_rank text;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if p_message_id is null then raise exception 'Message not found'; end if;

  select m.alliance_id into v_alliance_id
  from public.alliance_messages m
  where m.id = p_message_id
  for update;
  if not found then raise exception 'Message not found'; end if;

  select m.game_rank into v_rank
  from public.alliance_members m
  where m.alliance_id = v_alliance_id and m.user_id = auth.uid();

  if coalesce(v_rank, '') <> 'R5' and not private.is_app_developer() then
    raise exception 'Only the current R5 can pin or unpin alliance announcements';
  end if;

  update public.alliance_messages
  set is_pinned = coalesce(p_is_pinned, false),
      pinned_at = case when coalesce(p_is_pinned, false) then now() else null end
  where id = p_message_id;
  return true;
end;
$function$;
