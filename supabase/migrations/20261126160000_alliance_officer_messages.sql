-- Persistent alliance announcements/messages.
-- R4 and R5 may publish; only the current R5 or an app developer may delete.

create table if not exists public.alliance_messages (
  id uuid primary key default gen_random_uuid(),
  alliance_id uuid not null references public.alliances(id) on delete cascade,
  author_user_id uuid not null references auth.users(id) on delete restrict,
  author_name text not null,
  body text not null check (char_length(btrim(body)) between 1 and 2000),
  created_at timestamptz not null default now()
);

create index if not exists alliance_messages_alliance_created_idx
  on public.alliance_messages (alliance_id, created_at desc);

alter table public.alliance_messages enable row level security;

drop policy if exists "Alliance members and developers can read messages" on public.alliance_messages;
create policy "Alliance members and developers can read messages"
  on public.alliance_messages for select to authenticated
  using (
    private.is_app_developer()
    or exists (
      select 1 from public.alliance_members m
      where m.alliance_id = alliance_messages.alliance_id
        and m.user_id = (select auth.uid())
    )
  );

revoke all on public.alliance_messages from anon, authenticated;
grant select on public.alliance_messages to authenticated;

create or replace function private.post_alliance_message(
  p_alliance_id uuid,
  p_body text
) returns uuid
language plpgsql
security definer
set search_path to ''
as $function$
declare
  v_rank text;
  v_message_id uuid;
  v_body text := btrim(coalesce(p_body, ''));
  v_author_name text;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if p_alliance_id is null or char_length(v_body) not between 1 and 2000 then
    raise exception 'Message must contain between 1 and 2000 characters';
  end if;

  select m.game_rank into v_rank
  from public.alliance_members m
  where m.alliance_id = p_alliance_id and m.user_id = auth.uid();

  if v_rank not in ('R4', 'R5') then
    raise exception 'Only R4 and R5 members can publish alliance messages';
  end if;

  select coalesce(nullif(p.player_name, ''), 'Membre')
    into v_author_name
  from public.player_profiles p
  where p.user_id = auth.uid();

  insert into public.alliance_messages(alliance_id, author_user_id, author_name, body)
  values (p_alliance_id, auth.uid(), coalesce(v_author_name, 'Membre'), v_body)
  returning id into v_message_id;

  return v_message_id;
end;
$function$;

create or replace function public.post_alliance_message(
  p_alliance_id uuid,
  p_body text
) returns uuid
language sql
security invoker
set search_path to ''
as $function$
  select private.post_alliance_message(p_alliance_id, p_body);
$function$;

create or replace function private.delete_alliance_message(
  p_message_id uuid
) returns boolean
language plpgsql
security definer
set search_path to ''
as $function$
declare
  v_alliance_id uuid;
  v_is_r5 boolean;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if p_message_id is null then raise exception 'Message not found'; end if;

  select m.alliance_id into v_alliance_id
  from public.alliance_messages m
  where m.id = p_message_id
  for update;

  if not found then raise exception 'Message not found'; end if;

  select exists (
    select 1 from public.alliance_members m
    where m.alliance_id = v_alliance_id
      and m.user_id = auth.uid()
      and m.game_rank = 'R5'
  ) into v_is_r5;

  if not (v_is_r5 or private.is_app_developer()) then
    raise exception 'Only the current R5 or an app developer can delete alliance messages';
  end if;

  delete from public.alliance_messages where id = p_message_id;
  return true;
end;
$function$;

create or replace function public.delete_alliance_message(p_message_id uuid)
returns boolean
language sql
security invoker
set search_path to ''
as $function$
  select private.delete_alliance_message(p_message_id);
$function$;

revoke all on function private.post_alliance_message(uuid, text) from public, anon, authenticated;
revoke all on function private.delete_alliance_message(uuid) from public, anon, authenticated;
revoke all on function public.post_alliance_message(uuid, text) from public, anon;
revoke all on function public.delete_alliance_message(uuid) from public, anon;
grant execute on function private.post_alliance_message(uuid, text) to authenticated;
grant execute on function private.delete_alliance_message(uuid) to authenticated;
grant execute on function public.post_alliance_message(uuid, text) to authenticated;
grant execute on function public.delete_alliance_message(uuid) to authenticated;
