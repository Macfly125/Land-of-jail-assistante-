create or replace function private.can_manage_alliance(p_alliance_id uuid)
returns boolean
language sql
stable
security definer
set search_path to ''
as $function$
  select
    exists (
      select 1 from public.alliances a
      where a.id = p_alliance_id and a.created_by = (select auth.uid())
    )
    or exists (
      select 1 from public.alliance_members m
      where m.alliance_id = p_alliance_id
        and m.user_id = (select auth.uid())
        and m.game_rank in ('R4', 'R5')
    )
    or (
      private.is_app_developer()
      and exists (
        select 1 from public.alliance_members m
        where m.alliance_id = p_alliance_id
          and m.user_id = (select auth.uid())
          and m.game_rank = 'R4'
      )
    );
$function$;

create or replace function private.transfer_alliance_r5(
  p_alliance_id uuid, p_new_r5_user_id uuid
)
returns boolean
language plpgsql
security definer
set search_path to ''
as $function$
declare
  v_updated integer;
  v_is_current_r5 boolean;
  v_is_developer_r4 boolean;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if p_new_r5_user_id is null or p_new_r5_user_id = auth.uid() then
    raise exception 'Choose another R4 member';
  end if;
  perform 1 from public.alliances where id = p_alliance_id for update;
  if not found then raise exception 'Alliance not found'; end if;

  select exists (
    select 1 from public.alliance_members
    where alliance_id = p_alliance_id and user_id = auth.uid() and game_rank = 'R5'
  ) into v_is_current_r5;

  select private.is_app_developer() and exists (
    select 1 from public.alliance_members
    where alliance_id = p_alliance_id and user_id = auth.uid() and game_rank = 'R4'
  ) into v_is_developer_r4;

  if not (v_is_current_r5 or v_is_developer_r4) then
    raise exception 'Only the current R5 or the app developer (as an R4 member) can transfer alliance leadership';
  end if;

  if not exists (
    select 1 from public.alliance_members
    where alliance_id = p_alliance_id and user_id = p_new_r5_user_id and game_rank = 'R4'
  ) then raise exception 'The selected player must already be an R4 member'; end if;

  update public.alliance_members set game_rank = 'R4'
  where alliance_id = p_alliance_id and game_rank = 'R5';

  update public.alliance_members set game_rank = 'R5'
  where alliance_id = p_alliance_id and user_id = p_new_r5_user_id and game_rank = 'R4';
  get diagnostics v_updated = row_count;
  if v_updated <> 1 then raise exception 'The selected R4 could not be promoted'; end if;

  update public.alliances set created_by = p_new_r5_user_id where id = p_alliance_id;
  return true;
end;
$function$;