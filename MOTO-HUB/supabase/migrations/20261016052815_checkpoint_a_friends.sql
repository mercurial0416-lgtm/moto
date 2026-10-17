begin;
create schema if not exists moto_private;
revoke all on schema moto_private from public,anon;
grant usage on schema moto_private to authenticated;
create table public.mh_profiles (
 user_id uuid primary key default auth.uid() references auth.users(id) on delete cascade,
 display_name text not null check(length(trim(display_name)) between 1 and 40),
 bike text not null default '' check(length(bike)<=120)
);
create table public.mh_codes (
 user_id uuid primary key references public.mh_profiles(user_id) on delete cascade,
 code text not null unique default replace(gen_random_uuid()::text,'-','')
);
create table public.mh_requests (
 id uuid primary key default gen_random_uuid(),
 sender uuid not null references public.mh_profiles(user_id) on delete cascade,
 recipient uuid not null references public.mh_profiles(user_id) on delete cascade,
 status text not null default 'pending' check(status in ('pending','accepted')),
 created_at timestamptz not null default now(),check(sender<>recipient)
);
create unique index mh_pair_unique on public.mh_requests(least(sender,recipient),greatest(sender,recipient));
create index mh_recipient on public.mh_requests(recipient);
create index mh_sender on public.mh_requests(sender);
create table moto_private.request_limits(user_id uuid primary key references auth.users(id) on delete cascade, window_at timestamptz not null, attempts integer not null);
alter table public.mh_profiles enable row level security;
alter table public.mh_codes enable row level security;
alter table public.mh_requests enable row level security;
alter table moto_private.request_limits enable row level security;
revoke all on public.mh_profiles,public.mh_codes,public.mh_requests from public,anon,authenticated;
revoke all on moto_private.request_limits from public,anon,authenticated;
grant select,insert on public.mh_profiles to authenticated;
grant update(display_name,bike) on public.mh_profiles to authenticated;
grant select on public.mh_codes to authenticated;
grant select,delete on public.mh_requests to authenticated;
grant update(status) on public.mh_requests to authenticated;
create policy mh_requests_read on public.mh_requests for select to authenticated using ((select auth.uid()) in (sender,recipient));
create policy mh_requests_accept on public.mh_requests for update to authenticated using(recipient=(select auth.uid()) and status='pending') with check(recipient=(select auth.uid()) and status='accepted');
create policy mh_requests_delete on public.mh_requests for delete to authenticated using((select auth.uid()) in(sender,recipient));
create policy mh_profiles_read on public.mh_profiles for select to authenticated using(user_id=(select auth.uid()) or exists(select 1 from public.mh_requests r where (r.sender=(select auth.uid()) and r.recipient=user_id) or(r.recipient=(select auth.uid()) and r.sender=user_id)));
create policy mh_profiles_insert on public.mh_profiles for insert to authenticated with check(user_id=(select auth.uid()));
create policy mh_profiles_update on public.mh_profiles for update to authenticated using(user_id=(select auth.uid())) with check(user_id=(select auth.uid()));
create policy mh_codes_read on public.mh_codes for select to authenticated using(user_id=(select auth.uid()));
-- Private, bounded privileged lookup. No code enumeration endpoint.
create function moto_private.make_code() returns trigger language plpgsql security definer set search_path='' as $$
begin
 if auth.uid() is null or auth.uid()<>new.user_id then raise exception 'not authorized'; end if;
 insert into public.mh_codes(user_id) values(new.user_id);return new;
end;$$;
revoke all on function moto_private.make_code() from public,anon,authenticated;
create trigger mh_profile_code after insert on public.mh_profiles for each row execute function moto_private.make_code();
create function moto_private.request_friend(friend_code text) returns boolean language plpgsql security definer set search_path='' as $$
declare me uuid:=auth.uid(); target uuid; n integer;
begin
 if me is null then raise exception 'authentication required'; end if;
 if not exists(select 1 from public.mh_profiles where user_id=me) then return false; end if;
 insert into moto_private.request_limits(user_id,window_at,attempts) values(me,now(),1)
 on conflict(user_id) do update set
 attempts=case when request_limits.window_at<now()-interval '1 hour' then 1 else request_limits.attempts+1 end,
 window_at=case when request_limits.window_at<now()-interval '1 hour' then now() else request_limits.window_at end
 returning attempts into n;
 if n>20 or friend_code !~ '^[a-f0-9]{32}$' then return false; end if;
 select user_id into target from public.mh_codes where code=friend_code;
 if target is null or target=me then return false; end if;
 insert into public.mh_requests(sender,recipient) values(me,target) on conflict do nothing;
 return true;
end;$$;
revoke all on function moto_private.request_friend(text) from public,anon;
grant execute on function moto_private.request_friend(text) to authenticated;
create function public.mh_request_friend(friend_code text) returns boolean language sql security invoker set search_path='' as $$ select moto_private.request_friend(friend_code); $$;
revoke all on function public.mh_request_friend(text) from public,anon;
grant execute on function public.mh_request_friend(text) to authenticated;
commit;
