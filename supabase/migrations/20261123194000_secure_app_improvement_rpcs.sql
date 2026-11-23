create index if not exists app_improvement_requests_reviewed_by_idx on public.app_improvement_requests(reviewed_by);
create or replace function private.submit_app_improvement(
 p_title text,p_description text,p_category text default 'other',p_ai_summary text default '',p_ai_category text default 'other',p_ai_questions text[] default '{}'
) returns uuid language plpgsql security definer set search_path=''
as $$
declare v_id uuid;
begin
 if auth.uid() is null then raise exception 'Authentication required'; end if;
 if char_length(trim(coalesce(p_title,''))) not between 5 and 120 then raise exception 'Le titre doit contenir entre 5 et 120 caractères.'; end if;
 if char_length(trim(coalesce(p_description,''))) not between 15 and 4000 then raise exception 'La description doit contenir entre 15 et 4000 caractères.'; end if;
 if p_category not in ('bug','idea','game_data','interface','performance','other') then raise exception 'Catégorie invalide.'; end if;
 if p_ai_category not in ('bug','idea','game_data','interface','performance','other') then p_ai_category:='other'; end if;
 if (select count(*) from public.app_improvement_requests where user_id=auth.uid() and created_at>now()-interval '1 hour')>=5 then raise exception 'Limite atteinte : maximum 5 propositions par heure.'; end if;
 insert into public.app_improvement_requests(user_id,title,description,category,ai_summary,ai_category,ai_questions)
 values(auth.uid(),trim(p_title),trim(p_description),p_category,left(coalesce(p_ai_summary,''),1200),p_ai_category,(coalesce(p_ai_questions,'{}'))[1:5])
 returning id into v_id;
 return v_id;
end;
$$;
create or replace function private.review_app_improvement(p_request_id uuid,p_status text,p_developer_reply text)
returns boolean language plpgsql security definer set search_path=''
as $$
begin
 if auth.uid() is null or not private.is_app_developer() then raise exception 'Developer access required'; end if;
 if p_status not in ('accepted','rejected','needs_info','in_progress','implemented') then raise exception 'Statut invalide.'; end if;
 if char_length(coalesce(p_developer_reply,''))>3000 then raise exception 'La réponse est trop longue.'; end if;
 update public.app_improvement_requests set status=p_status,developer_reply=trim(coalesce(p_developer_reply,'')),reviewed_by=auth.uid(),reviewed_at=now(),updated_at=now() where id=p_request_id;
 if not found then raise exception 'Proposition introuvable.'; end if;
 return true;
end;
$$;
create or replace function public.submit_app_improvement(p_title text,p_description text,p_category text default 'other',p_ai_summary text default '',p_ai_category text default 'other',p_ai_questions text[] default '{}')
returns uuid language sql security invoker set search_path='' as $$ select private.submit_app_improvement(p_title,p_description,p_category,p_ai_summary,p_ai_category,p_ai_questions); $$;
create or replace function public.review_app_improvement(p_request_id uuid,p_status text,p_developer_reply text)
returns boolean language sql security invoker set search_path='' as $$ select private.review_app_improvement(p_request_id,p_status,p_developer_reply); $$;
revoke all on function public.submit_app_improvement(text,text,text,text,text,text[]) from public,anon;
revoke all on function public.review_app_improvement(uuid,text,text) from public,anon;
grant execute on function public.submit_app_improvement(text,text,text,text,text,text[]) to authenticated;
grant execute on function public.review_app_improvement(uuid,text,text) to authenticated;
revoke all on function private.submit_app_improvement(text,text,text,text,text,text[]) from public,anon,authenticated;
revoke all on function private.review_app_improvement(uuid,text,text) from public,anon,authenticated;
grant execute on function private.submit_app_improvement(text,text,text,text,text,text[]) to authenticated;
grant execute on function private.review_app_improvement(uuid,text,text) to authenticated;
