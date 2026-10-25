begin;
create table public.mh_groups(id uuid primary key default gen_random_uuid(),owner uuid not null references public.mh_profiles(user_id) on delete cascade,name text not null check(length(trim(name)) between 1 and 60),meetup text not null default '' check(length(meetup)<=200),destination text not null default '' check(length(destination)<=200),active boolean not null default true);
create table public.mh_members(group_id uuid not null references public.mh_groups(id) on delete cascade,user_id uuid not null references public.mh_profiles(user_id) on delete cascade,status text not null check(status in('invited','accepted')),primary key(group_id,user_id));
create index mh_members_user on public.mh_members(user_id,group_id);
create table public.mh_shares(id uuid primary key default gen_random_uuid(),user_id uuid not null references public.mh_profiles(user_id) on delete cascade,group_id uuid not null references public.mh_groups(id) on delete cascade,active boolean not null default true);
create unique index mh_one_active_share on public.mh_shares(user_id) where active;
create table public.mh_positions(user_id uuid primary key references public.mh_profiles(user_id) on delete cascade,session_id uuid not null references public.mh_shares(id) on delete cascade,group_id uuid not null references public.mh_groups(id) on delete cascade,lat double precision not null check(lat between -90 and 90),lon double precision not null check(lon between -180 and 180),bearing double precision check(bearing>=0 and bearing<360),state text not null check(state in('moving','stopped','fuel','rest')),received_at timestamptz not null,expires_at timestamptz not null);
create index mh_positions_group on public.mh_positions(group_id,expires_at);
alter table public.mh_groups enable row level security;
alter table public.mh_members enable row level security;
alter table public.mh_shares enable row level security;
alter table public.mh_positions enable row level security;
revoke all on public.mh_groups,public.mh_members,public.mh_shares,public.mh_positions from public,anon,authenticated;
grant select on public.mh_groups,public.mh_members,public.mh_shares,public.mh_positions to authenticated;
-- Internal helpers bypass recursive membership RLS only after checking the JWT caller.
create function moto_private.member_of(g uuid,accepted_only boolean) returns boolean language sql stable security definer set search_path='' as $$ select auth.uid() is not null and exists(select 1 from public.mh_members m join public.mh_groups r on r.id=m.group_id where r.active and m.group_id=g and m.user_id=auth.uid() and (not accepted_only or m.status='accepted')); $$;
create function moto_private.share_visible(s uuid,g uuid) returns boolean language sql stable security definer set search_path='' as $$ select moto_private.member_of(g,true) and exists(select 1 from public.mh_shares x join public.mh_members m on m.group_id=x.group_id and m.user_id=x.user_id where x.id=s and x.group_id=g and x.active and m.status='accepted'); $$;
create function moto_private.group_peer(u uuid) returns boolean language sql stable security definer set search_path='' as $$ select auth.uid() is not null and exists(select 1 from public.mh_members a join public.mh_members b on a.group_id=b.group_id join public.mh_groups g on g.id=a.group_id where g.active and a.user_id=auth.uid() and b.user_id=u and a.status='accepted' and b.status='accepted'); $$;
revoke all on function moto_private.member_of(uuid,boolean),moto_private.share_visible(uuid,uuid),moto_private.group_peer(uuid) from public,anon;
grant execute on function moto_private.member_of(uuid,boolean),moto_private.share_visible(uuid,uuid),moto_private.group_peer(uuid) to authenticated;
create policy mh_groups_read on public.mh_groups for select to authenticated using(moto_private.member_of(id,false));
create policy mh_members_read on public.mh_members for select to authenticated using(user_id=(select auth.uid()) or moto_private.member_of(group_id,true));
create policy mh_shares_read on public.mh_shares for select to authenticated using(user_id=(select auth.uid()));
create policy mh_positions_read on public.mh_positions for select to authenticated using(expires_at>now() and moto_private.share_visible(session_id,group_id));
create policy mh_profiles_group_peer on public.mh_profiles for select to authenticated using(moto_private.group_peer(user_id));
create function moto_private.group_action(action text,group_id uuid,value text) returns jsonb language plpgsql security definer set search_path='' as $$
declare me uuid:=auth.uid(); g uuid:=group_id; own uuid; v jsonb; target uuid;
begin
 if me is null or not exists(select 1 from public.mh_profiles where user_id=me) then raise exception 'profile required'; end if;
 if action='create' then
  if (select count(*) from public.mh_groups where owner=me and active)>=10 then raise exception 'group limit'; end if;
  v:=value::jsonb;insert into public.mh_groups(owner,name,meetup,destination) values(me,v->>'name',coalesce(v->>'meetup',''),coalesce(v->>'destination','')) returning id into g;
  insert into public.mh_members values(g,me,'accepted');return jsonb_build_object('id',g);
 end if;
 select owner into own from public.mh_groups r where r.id=g and r.active for update;
 if own is null then raise exception 'not available'; end if;
 if action='invite' then
  target:=value::uuid;
  if own<>me or not exists(select 1 from public.mh_requests f where f.status='accepted' and ((f.sender=me and f.recipient=target) or(f.recipient=me and f.sender=target))) then raise exception 'not authorized'; end if;
  if (select count(*) from public.mh_members m where m.group_id=g)>=32 then raise exception 'member limit'; end if;
  insert into public.mh_members values(g,target,'invited') on conflict do nothing;
 elsif action='accept' then
  update public.mh_members m set status='accepted' where m.group_id=g and m.user_id=me and m.status='invited';if not found then raise exception 'invitation required'; end if;
 elsif action='leave' then
  if own=me then raise exception 'owner must close group'; end if;
  delete from public.mh_members m where m.group_id=g and m.user_id=me;
  update public.mh_shares s set active=false where s.group_id=g and s.user_id=me;
  delete from public.mh_positions p where p.group_id=g and p.user_id=me;
 elsif action='close' then
  if own<>me then raise exception 'not authorized'; end if;
  update public.mh_groups set active=false where id=g;
  update public.mh_shares s set active=false where s.group_id=g;
  delete from public.mh_positions p where p.group_id=g;
 else raise exception 'unknown action'; end if;
 return jsonb_build_object('id',g);
end;$$;
create function moto_private.share_start(group_id uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare me uuid:=auth.uid(); s uuid;
begin
 if not moto_private.member_of(group_id,true) then raise exception 'membership required'; end if;
 update public.mh_shares set active=false where user_id=me;
 delete from public.mh_positions where user_id=me;
 insert into public.mh_shares(user_id,group_id) values(me,group_id) returning id into s;
 return jsonb_build_object('id',s);
end;$$;
create function moto_private.share_end(session_id uuid) returns boolean language plpgsql security definer set search_path='' as $$
begin
 if auth.uid() is null then raise exception 'authentication required'; end if;
 update public.mh_shares s set active=false where s.id=session_id and s.user_id=auth.uid();
 delete from public.mh_positions p where p.session_id=share_end.session_id and p.user_id=auth.uid();return true;
end;$$;
create function moto_private.share_publish(session_id uuid,latitude double precision,longitude double precision,bearing double precision,ride_state text,captured_ms bigint) returns boolean language plpgsql security definer set search_path='' as $$
declare s public.mh_shares%rowtype; t timestamptz:=clock_timestamp();
begin
 if auth.uid() is null then raise exception 'authentication required'; end if;
 select * into s from public.mh_shares r where r.id=session_id and r.user_id=auth.uid() and r.active for update;
 if not found or not moto_private.member_of(s.group_id,true) then return false; end if;
 if latitude is null or longitude is null or not(latitude between -90 and 90) or not(longitude between -180 and 180) or (bearing is not null and not(bearing>=0 and bearing<360)) or ride_state is null or ride_state not in('moving','stopped','fuel','rest') or captured_ms is null or abs(extract(epoch from t)*1000-captured_ms)>30000 then return false; end if;
 if exists(select 1 from public.mh_positions p where p.user_id=s.user_id and p.received_at>t-interval '4 seconds') then return true; end if;
 insert into public.mh_positions as p(user_id,session_id,group_id,lat,lon,bearing,state,received_at,expires_at) values(s.user_id,s.id,s.group_id,latitude,longitude,bearing,ride_state,t,t+interval '30 seconds') on conflict(user_id) do update set session_id=excluded.session_id,group_id=excluded.group_id,lat=excluded.lat,lon=excluded.lon,bearing=excluded.bearing,state=excluded.state,received_at=excluded.received_at,expires_at=excluded.expires_at;return true;
end;$$;
revoke all on function moto_private.group_action(text,uuid,text),moto_private.share_start(uuid),moto_private.share_end(uuid),moto_private.share_publish(uuid,double precision,double precision,double precision,text,bigint) from public,anon;
grant execute on function moto_private.group_action(text,uuid,text),moto_private.share_start(uuid),moto_private.share_end(uuid),moto_private.share_publish(uuid,double precision,double precision,double precision,text,bigint) to authenticated;
create function public.mh_group_action(action text,group_id uuid,value text) returns jsonb language sql security invoker set search_path='' as $$ select moto_private.group_action(action,group_id,value); $$;
create function public.mh_share_start(group_id uuid) returns jsonb language sql security invoker set search_path='' as $$ select moto_private.share_start(group_id); $$;
create function public.mh_share_end(session_id uuid) returns boolean language sql security invoker set search_path='' as $$ select moto_private.share_end(session_id); $$;
create function public.mh_share_publish(session_id uuid,latitude double precision,longitude double precision,bearing double precision,ride_state text,captured_ms bigint) returns boolean language sql security invoker set search_path='' as $$ select moto_private.share_publish(session_id,latitude,longitude,bearing,ride_state,captured_ms); $$;
revoke all on function public.mh_group_action(text,uuid,text),public.mh_share_start(uuid),public.mh_share_end(uuid),public.mh_share_publish(uuid,double precision,double precision,double precision,text,bigint) from public,anon;
grant execute on function public.mh_group_action(text,uuid,text),public.mh_share_start(uuid),public.mh_share_end(uuid),public.mh_share_publish(uuid,double precision,double precision,double precision,text,bigint) to authenticated;
commit;
