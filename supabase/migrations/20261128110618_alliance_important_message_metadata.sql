-- Structured alliance announcements: titles, categories and pinned notices.
alter table public.alliance_messages
  add column if not exists title text not null default '',
  add column if not exists category text not null default 'general'
    check (category in ('general', 'event', 'war', 'rules', 'recruitment')),
  add column if not exists is_pinned boolean not null default false,
  add column if not exists pinned_at timestamptz;

create index if not exists alliance_messages_pinned_order_idx
  on public.alliance_messages (alliance_id, is_pinned desc, pinned_at desc nulls last, created_at desc);

create or replace function private.post_alliance_announcement(
  p_alliance_id uuid,
  p_title text,
  p_body text,
  p_category text,
  p_is_pinned boolean default false
) returns uuid
language plpgsql
security definer
set search_path to ''
as $function$
declare
  v_rank text;
  v_message_id uuid;
  v_title text := btrim(coalesce(p_title, ''));
  v_body text := btrim(coalesce(p_body, ''));
  v_category text := lower(btrim(coalesce(p_category, 'general')));
  v_author_name text;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if p_alliance_id is null
     or char_length(v_title) not between 1 and 120
     or char_length(v_body) not between 1 and 2000
     or v_category not in ('general', 'event', 'war', 'rules', 'recruitment') then
    raise exception 'Invalid alliance announcement';
  end if;

  select m.game_rank into v_rank
  from public.alliance_members m
  where m.alliance_id = p_alliance_id and m.user_id = auth.uid();

  if coalesce(v_rank, '') not in ('R4', 'R5') then
    raise exception 'Only R4 and R5 members can publish alliance announcements';
  end if;
  if coalesce(p_is_pinned, false) and v_rank <> 'R5' and not private.is_app_developer() then
    raise exception 'Only the current R5 can pin an alliance announcement';
  end if;

  select coalesce(nullif(p.player_name, ''), 'Membre')
    into v_author_name
  from public.player_profiles p
  where p.user_id = auth.uid();

  insert into public.alliance_messages(
    alliance_id, author_user_id, author_name, title, body, category, is_pinned, pinned_at
  )
  values (
    p_alliance_id, auth.uid(), coalesce(v_author_name, 'Membre'),
    v_title, v_body, v_category, coalesce(p_is_pinned, false),
    case when coalesce(p_is_pinned, false) then now() else null end
  )
  returning id into v_message_id;

  return v_message_id;
end;
$function$;

create or replace function public.post_alliance_announcement(
  p_alliance_id uuid,
  p_title text,
  p_body text,
  p_category text,
  p_is_pinned boolean default false
) returns uuid
language sql
security invoker
set search_path to ''
as $function$
  select private.post_alliance_announcement(
    p_alliance_id, p_title, p_body, p_category, p_is_pinned
  );
$function$;

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

  if v_rank <> 'R5' and not private.is_app_developer() then
    raise exception 'Only the current R5 can pin or unpin alliance announcements';
  end if;

  update public.alliance_messages
  set is_pinned = coalesce(p_is_pinned, false),
      pinned_at = case when coalesce(p_is_pinned, false) then now() else null end
  where id = p_message_id;
  return true;
end;
$function$;

create or replace function public.set_alliance_message_pinned(
  p_message_id uuid,
  p_is_pinned boolean
) returns boolean
language sql
security invoker
set search_path to ''
as $function$
  select private.set_alliance_message_pinned(p_message_id, p_is_pinned);
$function$;

revoke all on function private.post_alliance_announcement(uuid, text, text, text, boolean) from public, anon, authenticated;
revoke all on function private.set_alliance_message_pinned(uuid, boolean) from public, anon, authenticated;
revoke all on function public.post_alliance_announcement(uuid, text, text, text, boolean) from public, anon;
revoke all on function public.set_alliance_message_pinned(uuid, boolean) from public, anon;
grant execute on function private.post_alliance_announcement(uuid, text, text, text, boolean) to authenticated;
grant execute on function private.set_alliance_message_pinned(uuid, boolean) to authenticated;
grant execute on function public.post_alliance_announcement(uuid, text, text, text, boolean) to authenticated;
grant execute on function public.set_alliance_message_pinned(uuid, boolean) to authenticated;
