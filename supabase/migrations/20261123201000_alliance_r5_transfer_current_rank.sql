-- The active R5 member, not necessarily the original creator, controls leadership transfer.
create or replace function private.transfer_alliance_r5(
  p_alliance_id uuid,
  p_new_r5_user_id uuid
) returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_updated integer;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if p_new_r5_user_id is null or p_new_r5_user_id = auth.uid() then
    raise exception 'Choose another R4 member';
  end if;
  perform 1 from public.alliances where id = p_alliance_id for update;
  if not found then raise exception 'Alliance not found'; end if;
  if not exists (
    select 1 from public.alliance_members
    where alliance_id = p_alliance_id and user_id = auth.uid() and game_rank = 'R5'
  ) then raise exception 'Only the current R5 can transfer alliance leadership'; end if;
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
$$;
