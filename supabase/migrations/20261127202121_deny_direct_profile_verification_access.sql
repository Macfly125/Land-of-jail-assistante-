-- Keep verification rows accessible only through the authenticated, membership-checked RPCs.
drop policy if exists "deny direct access to profile verifications" on public.alliance_profile_verifications;
create policy "deny direct access to profile verifications"
  on public.alliance_profile_verifications
  for all
  to authenticated
  using (false)
  with check (false);
