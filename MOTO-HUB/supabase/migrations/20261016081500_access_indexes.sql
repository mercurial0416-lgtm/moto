-- Speed up group/friend queries and avoid redundant permissive profile RLS policies.
create index if not exists mh_groups_owner on public.mh_groups(owner);
create index if not exists mh_shares_group on public.mh_shares(group_id);
create index if not exists mh_positions_session on public.mh_positions(session_id);

-- Both friend-request peers and accepted ride group peers may view minimal rider profiles.
drop policy if exists mh_profiles_read on public.mh_profiles;
drop policy if exists mh_profiles_group_peer on public.mh_profiles;
create policy mh_profiles_read_peers on public.mh_profiles for select to authenticated using (
 user_id=(select auth.uid())
 or exists(select 1 from public.mh_requests r
    where (r.sender=(select auth.uid()) and r.recipient=user_id)
       or (r.recipient=(select auth.uid()) and r.sender=user_id))
 or moto_private.group_peer(user_id)
);
