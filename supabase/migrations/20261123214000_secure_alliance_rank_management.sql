create or replace function private.can_view_player_profile(p_target_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path to ''
as $function$
  select
    p_target_user_id = (select auth.uid())
    or exists (
      select 1
      from public.alliance_members viewer
      join public.alliance_members target
        on target.alliance_id = viewer.alliance_id
      where viewer.user_id = (select auth.uid())
        and target.user_id = p_target_user_id
        and (
          viewer.game_rank = 'R5'
          or (
            viewer.game_rank = 'R4'
            and (
              target.game_rank in ('R4','R3','R2','R1')
              or private.is_app_developer()
            )
          )
        )
    );
$function$;

-- All effective rank changes must pass through the validated SECURITY DEFINER RPCs.
-- This prevents direct REST updates from bypassing the one-R5 and succession rules.
drop policy if exists "owner or R4 R5 manage member ranks" on public.alliance_members;