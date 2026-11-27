-- Cover both auth-user foreign keys for efficient cascades and lookups.
create index if not exists alliance_profile_verifications_member_user_idx
  on public.alliance_profile_verifications (member_user_id);

create index if not exists alliance_profile_verifications_verifier_user_idx
  on public.alliance_profile_verifications (verifier_user_id);
