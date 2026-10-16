-- =====================================================================
-- BookNest  —  isolated database setup for a SHARED Supabase project
-- =====================================================================
-- Ki kore: shob table/function/policy "booknest" schema te thake.
-- Tomar onno 2ta app er "public" schema (ba onno kono schema) ke ekdom touch kore na.
--
-- Run korbe kothay: Supabase Dashboard -> SQL Editor -> New query -> paste -> Run
-- (supabase db push / db reset KORBE NA — shared project e onno app er migration history er shathe mishe jete pare)
--
-- Idempotent: dobara run korle bhenge jabe na.
--
-- SHESH e README te bola step ta mone rekho:
--   Dashboard -> Project Settings -> API -> "Exposed schemas" e `booknest` add koro.
-- =====================================================================

create schema if not exists booknest;

grant usage on schema booknest to anon, authenticated, service_role;

-- ---------------------------------------------------------------------
-- Helper: updated_at trigger
-- ---------------------------------------------------------------------
create or replace function booknest.set_updated_at()
returns trigger
language plpgsql
set search_path = booknest, pg_temp
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

-- ---------------------------------------------------------------------
-- TABLES
-- ---------------------------------------------------------------------

-- Profiles (auth.users er shathe 1:1, kintu auto trigger NEI — dekho ensure_profile())
create table if not exists booknest.profiles (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null unique references auth.users(id) on delete cascade,
  name        text not null default '' check (char_length(name) <= 80),
  avatar_url  text default '' check (char_length(avatar_url) <= 500),
  bio         text default '' check (char_length(bio) <= 500),
  category    text default '' check (char_length(category) <= 60),
  followers   integer not null default 0 check (followers >= 0),
  following   integer not null default 0 check (following >= 0),
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);

create table if not exists booknest.books (
  id             uuid primary key default gen_random_uuid(),
  user_id        uuid not null references auth.users(id) on delete cascade,
  title          text not null default '' check (char_length(title) <= 200),
  description    text default '' check (char_length(description) <= 2000),
  genre          text default '' check (char_length(genre) <= 60),
  cover_url      text default '' check (char_length(cover_url) <= 500),
  is_published   boolean not null default false,
  likes_count    integer not null default 0 check (likes_count >= 0),
  reads_count    integer not null default 0 check (reads_count >= 0),
  comments_count integer not null default 0 check (comments_count >= 0),
  created_at     timestamptz not null default now(),
  updated_at     timestamptz not null default now()
);
create index if not exists idx_booknest_books_user      on booknest.books(user_id);
create index if not exists idx_booknest_books_published on booknest.books(is_published, created_at desc);

-- Chapters.  Draft hole is_encrypted = true: title ''(faka), content = client-side ciphertext.
create table if not exists booknest.chapters (
  id            uuid primary key default gen_random_uuid(),
  book_id       uuid not null references booknest.books(id) on delete cascade,
  title         text not null default '' check (char_length(title) <= 200),
  content       text not null default '' check (char_length(content) <= 1500000),
  is_encrypted  boolean not null default false,
  "order"       integer not null default 1,
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now()
);
create index if not exists idx_booknest_chapters_book on booknest.chapters(book_id, "order");

create table if not exists booknest.likes (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null references auth.users(id) on delete cascade,
  book_id     uuid not null references booknest.books(id) on delete cascade,
  created_at  timestamptz not null default now(),
  unique (user_id, book_id)
);
create index if not exists idx_booknest_likes_book on booknest.likes(book_id);

create table if not exists booknest.follows (
  id            uuid primary key default gen_random_uuid(),
  follower_id   uuid not null references auth.users(id) on delete cascade,
  following_id  uuid not null references auth.users(id) on delete cascade,
  created_at    timestamptz not null default now(),
  unique (follower_id, following_id),
  check (follower_id <> following_id)
);
create index if not exists idx_booknest_follows_follower  on booknest.follows(follower_id);
create index if not exists idx_booknest_follows_following on booknest.follows(following_id);

create table if not exists booknest.comments (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null references auth.users(id) on delete cascade,
  book_id     uuid not null references booknest.books(id) on delete cascade,
  content     text not null check (char_length(btrim(content)) between 1 and 2000),
  created_at  timestamptz not null default now()
);
create index if not exists idx_booknest_comments_book on booknest.comments(book_id);
create index if not exists idx_booknest_comments_user on booknest.comments(user_id);

create table if not exists booknest.read_history (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null references auth.users(id) on delete cascade,
  book_id     uuid not null references booknest.books(id) on delete cascade,
  created_at  timestamptz not null default now(),
  unique (user_id, book_id)
);
create index if not exists idx_booknest_read_history_book on booknest.read_history(book_id);

-- E2EE key material.  Ekhane SHUDHU encrypted jinish thake — server kokhono passphrase/key dekhe na.
create table if not exists booknest.user_keys (
  user_id              uuid primary key references auth.users(id) on delete cascade,
  version              smallint not null default 1,
  kdf_salt             text not null check (char_length(kdf_salt) <= 200),
  kdf_iterations       integer not null check (kdf_iterations between 200000 and 5000000),
  wrapped_dek          text not null check (char_length(wrapped_dek) <= 500),
  wrap_iv              text not null check (char_length(wrap_iv) <= 100),
  recovery_salt        text not null check (char_length(recovery_salt) <= 200),
  recovery_wrapped_dek text not null check (char_length(recovery_wrapped_dek) <= 500),
  recovery_iv          text not null check (char_length(recovery_iv) <= 100),
  created_at           timestamptz not null default now(),
  updated_at           timestamptz not null default now()
);

-- ---------------------------------------------------------------------
-- TRIGGERS: updated_at
-- ---------------------------------------------------------------------
drop trigger if exists booknest_profiles_updated_at on booknest.profiles;
create trigger booknest_profiles_updated_at before update on booknest.profiles
  for each row execute function booknest.set_updated_at();

drop trigger if exists booknest_books_updated_at on booknest.books;
create trigger booknest_books_updated_at before update on booknest.books
  for each row execute function booknest.set_updated_at();

drop trigger if exists booknest_chapters_updated_at on booknest.chapters;
create trigger booknest_chapters_updated_at before update on booknest.chapters
  for each row execute function booknest.set_updated_at();

drop trigger if exists booknest_user_keys_updated_at on booknest.user_keys;
create trigger booknest_user_keys_updated_at before update on booknest.user_keys
  for each row execute function booknest.set_updated_at();

-- ---------------------------------------------------------------------
-- TRIGGERS: counters (client ar counter e hat dite pare na)
-- ---------------------------------------------------------------------
create or replace function booknest.trg_likes_count()
returns trigger
language plpgsql
security definer
set search_path = booknest, pg_temp
as $$
begin
  if tg_op = 'INSERT' then
    update booknest.books set likes_count = likes_count + 1 where id = new.book_id;
  elsif tg_op = 'DELETE' then
    update booknest.books set likes_count = greatest(likes_count - 1, 0) where id = old.book_id;
  end if;
  return null;
end;
$$;

create or replace function booknest.trg_comments_count()
returns trigger
language plpgsql
security definer
set search_path = booknest, pg_temp
as $$
begin
  if tg_op = 'INSERT' then
    update booknest.books set comments_count = comments_count + 1 where id = new.book_id;
  elsif tg_op = 'DELETE' then
    update booknest.books set comments_count = greatest(comments_count - 1, 0) where id = old.book_id;
  end if;
  return null;
end;
$$;

create or replace function booknest.trg_follows_count()
returns trigger
language plpgsql
security definer
set search_path = booknest, pg_temp
as $$
begin
  if tg_op = 'INSERT' then
    update booknest.profiles set followers = followers + 1 where user_id = new.following_id;
    update booknest.profiles set following = following + 1 where user_id = new.follower_id;
  elsif tg_op = 'DELETE' then
    update booknest.profiles set followers = greatest(followers - 1, 0) where user_id = old.following_id;
    update booknest.profiles set following = greatest(following - 1, 0) where user_id = old.follower_id;
  end if;
  return null;
end;
$$;

drop trigger if exists booknest_likes_count on booknest.likes;
create trigger booknest_likes_count after insert or delete on booknest.likes
  for each row execute function booknest.trg_likes_count();

drop trigger if exists booknest_comments_count on booknest.comments;
create trigger booknest_comments_count after insert or delete on booknest.comments
  for each row execute function booknest.trg_comments_count();

drop trigger if exists booknest_follows_count on booknest.follows;
create trigger booknest_follows_count after insert or delete on booknest.follows
  for each row execute function booknest.trg_follows_count();

-- ---------------------------------------------------------------------
-- TRIGGERS: encrypted draft public hoye jete parbe na
-- ---------------------------------------------------------------------
create or replace function booknest.trg_guard_publish()
returns trigger
language plpgsql
security definer
set search_path = booknest, pg_temp
as $$
begin
  if new.is_published and exists (
    select 1 from booknest.chapters c where c.book_id = new.id and c.is_encrypted
  ) then
    raise exception 'Encrypted chapters must be decrypted before publishing'
      using errcode = '23514';
  end if;
  return new;
end;
$$;

drop trigger if exists booknest_guard_publish on booknest.books;
create trigger booknest_guard_publish before insert or update of is_published on booknest.books
  for each row execute function booknest.trg_guard_publish();

create or replace function booknest.trg_guard_chapter_encryption()
returns trigger
language plpgsql
security definer
set search_path = booknest, pg_temp
as $$
begin
  if new.is_encrypted and exists (
    select 1 from booknest.books b where b.id = new.book_id and b.is_published
  ) then
    raise exception 'Published books cannot contain encrypted chapters'
      using errcode = '23514';
  end if;
  return new;
end;
$$;

drop trigger if exists booknest_guard_chapter_encryption on booknest.chapters;
create trigger booknest_guard_chapter_encryption before insert or update on booknest.chapters
  for each row execute function booknest.trg_guard_chapter_encryption();

-- ---------------------------------------------------------------------
-- RPC functions
-- ---------------------------------------------------------------------

-- auth.users e kono trigger NEI (oita shob app er jonno shared — onno app er signup e amader table e row banate dibo na).
-- Login er por client eta call kore; profile na thakle banay.
create or replace function booknest.ensure_profile()
returns booknest.profiles
language plpgsql
security definer
set search_path = booknest, pg_temp
as $$
declare
  uid  uuid := auth.uid();
  meta jsonb;
  em   text;
  rec  booknest.profiles;
begin
  if uid is null then
    raise exception 'Not authenticated' using errcode = '28000';
  end if;

  select * into rec from booknest.profiles where user_id = uid;
  if found then
    return rec;
  end if;

  select raw_user_meta_data, email into meta, em from auth.users where id = uid;

  insert into booknest.profiles (user_id, name, bio, category)
  values (
    uid,
    left(coalesce(
      nullif(btrim(meta->>'full_name'), ''),
      nullif(btrim(meta->>'name'), ''),
      nullif(split_part(coalesce(em, ''), '@', 1), ''),
      'Reader'
    ), 80),
    left(coalesce(meta->>'booknest_bio', ''), 500),
    left(coalesce(meta->>'booknest_category', ''), 60)
  )
  on conflict (user_id) do nothing;

  select * into rec from booknest.profiles where user_id = uid;
  return rec;
end;
$$;

create or replace function booknest.track_book_read(p_book_id uuid)
returns void
language plpgsql
security definer
set search_path = booknest, pg_temp
as $$
declare
  caller uuid := auth.uid();
  inserted integer;
begin
  if not exists (
    select 1 from booknest.books b
    where b.id = p_book_id and (b.is_published or b.user_id = caller)
  ) then
    raise exception 'Book is not accessible' using errcode = '42501';
  end if;

  if caller is not null then
    insert into booknest.read_history (user_id, book_id) values (caller, p_book_id)
    on conflict (user_id, book_id) do nothing;
    get diagnostics inserted = row_count;
    -- logged-in user er jonno protita book e sudhu 1 bar count hoy
    if inserted = 0 then
      return;
    end if;
  end if;

  update booknest.books set reads_count = reads_count + 1 where id = p_book_id;
end;
$$;

create or replace function booknest.get_follower_count(p_user_id uuid)
returns integer
language sql
stable
security definer
set search_path = booknest, pg_temp
as $$
  select count(*)::integer from booknest.follows where following_id = p_user_id;
$$;

create or replace function booknest.get_following_count(p_user_id uuid)
returns integer
language sql
stable
security definer
set search_path = booknest, pg_temp
as $$
  select count(*)::integer from booknest.follows where follower_id = p_user_id;
$$;

-- Atomic: shob chapter ekshathe replace (ager delete-then-insert e insert fail korle shob chapter hariye jeto)
create or replace function booknest.replace_chapters(p_book_id uuid, p_chapters jsonb)
returns void
language plpgsql
security invoker
set search_path = booknest, pg_temp
as $$
begin
  if auth.uid() is null or not exists (
    select 1 from booknest.books where id = p_book_id and user_id = auth.uid()
  ) then
    raise exception 'Not allowed' using errcode = '42501';
  end if;

  if jsonb_typeof(p_chapters) <> 'array' or jsonb_array_length(p_chapters) > 500 then
    raise exception 'Invalid chapters payload' using errcode = '22023';
  end if;

  delete from booknest.chapters where book_id = p_book_id;

  insert into booknest.chapters (book_id, title, content, is_encrypted, "order")
  select
    p_book_id,
    left(coalesce(e->>'title', ''), 200),
    coalesce(e->>'content', ''),
    coalesce((e->>'is_encrypted')::boolean, false),
    ord::integer
  from jsonb_array_elements(p_chapters) with ordinality as t(e, ord);
end;
$$;

-- Shudhu BookNest er data muche; auth.users ke delete kore NA (oita onno app gulor-o login).
create or replace function booknest.delete_my_data()
returns void
language plpgsql
security definer
set search_path = booknest, pg_temp
as $$
declare
  uid uuid := auth.uid();
begin
  if uid is null then
    raise exception 'Not authenticated' using errcode = '28000';
  end if;

  delete from booknest.books        where user_id = uid;   -- chapters/likes/comments cascade
  delete from booknest.comments     where user_id = uid;
  delete from booknest.likes        where user_id = uid;
  delete from booknest.follows      where follower_id = uid or following_id = uid;
  delete from booknest.read_history where user_id = uid;
  delete from booknest.user_keys    where user_id = uid;
  delete from booknest.profiles     where user_id = uid;
end;
$$;

-- ---------------------------------------------------------------------
-- ROW LEVEL SECURITY
-- ---------------------------------------------------------------------
alter table booknest.profiles     enable row level security;
alter table booknest.books        enable row level security;
alter table booknest.chapters     enable row level security;
alter table booknest.likes        enable row level security;
alter table booknest.follows      enable row level security;
alter table booknest.comments     enable row level security;
alter table booknest.read_history enable row level security;
alter table booknest.user_keys    enable row level security;

-- profiles
drop policy if exists "profiles_select" on booknest.profiles;
create policy "profiles_select" on booknest.profiles for select to anon, authenticated using (
  user_id = (select auth.uid())
  or exists (select 1 from booknest.books b where b.user_id = profiles.user_id and b.is_published)
  or exists (
    select 1 from booknest.comments c join booknest.books b on b.id = c.book_id
    where c.user_id = profiles.user_id and (b.is_published or b.user_id = (select auth.uid()))
  )
  or exists (
    select 1 from booknest.follows f
    where (f.follower_id = (select auth.uid()) and f.following_id = profiles.user_id)
       or (f.following_id = (select auth.uid()) and f.follower_id = profiles.user_id)
  )
);
drop policy if exists "profiles_update_own" on booknest.profiles;
create policy "profiles_update_own" on booknest.profiles for update to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

-- books
drop policy if exists "books_select" on booknest.books;
create policy "books_select" on booknest.books for select to anon, authenticated
  using (is_published or user_id = (select auth.uid()));
drop policy if exists "books_insert_own" on booknest.books;
create policy "books_insert_own" on booknest.books for insert to authenticated
  with check (user_id = (select auth.uid()));
drop policy if exists "books_update_own" on booknest.books;
create policy "books_update_own" on booknest.books for update to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
drop policy if exists "books_delete_own" on booknest.books;
create policy "books_delete_own" on booknest.books for delete to authenticated
  using (user_id = (select auth.uid()));

-- chapters
drop policy if exists "chapters_select" on booknest.chapters;
create policy "chapters_select" on booknest.chapters for select to anon, authenticated using (
  exists (select 1 from booknest.books b
          where b.id = chapters.book_id and (b.is_published or b.user_id = (select auth.uid())))
);
drop policy if exists "chapters_insert_own" on booknest.chapters;
create policy "chapters_insert_own" on booknest.chapters for insert to authenticated with check (
  exists (select 1 from booknest.books b where b.id = chapters.book_id and b.user_id = (select auth.uid()))
);
drop policy if exists "chapters_update_own" on booknest.chapters;
create policy "chapters_update_own" on booknest.chapters for update to authenticated
  using (exists (select 1 from booknest.books b where b.id = chapters.book_id and b.user_id = (select auth.uid())))
  with check (exists (select 1 from booknest.books b where b.id = chapters.book_id and b.user_id = (select auth.uid())));
drop policy if exists "chapters_delete_own" on booknest.chapters;
create policy "chapters_delete_own" on booknest.chapters for delete to authenticated using (
  exists (select 1 from booknest.books b where b.id = chapters.book_id and b.user_id = (select auth.uid()))
);

-- likes (shudhu nijer like dekha jay; public count books.likes_count theke ashe)
drop policy if exists "likes_select_own" on booknest.likes;
create policy "likes_select_own" on booknest.likes for select to authenticated
  using (user_id = (select auth.uid()));
drop policy if exists "likes_insert_own" on booknest.likes;
create policy "likes_insert_own" on booknest.likes for insert to authenticated with check (
  user_id = (select auth.uid())
  and exists (select 1 from booknest.books b where b.id = likes.book_id and b.is_published)
);
drop policy if exists "likes_delete_own" on booknest.likes;
create policy "likes_delete_own" on booknest.likes for delete to authenticated
  using (user_id = (select auth.uid()));

-- follows
drop policy if exists "follows_select_involved" on booknest.follows;
create policy "follows_select_involved" on booknest.follows for select to authenticated
  using ((select auth.uid()) in (follower_id, following_id));
drop policy if exists "follows_insert_own" on booknest.follows;
create policy "follows_insert_own" on booknest.follows for insert to authenticated
  with check (follower_id = (select auth.uid()));
drop policy if exists "follows_delete_own" on booknest.follows;
create policy "follows_delete_own" on booknest.follows for delete to authenticated
  using (follower_id = (select auth.uid()));

-- comments
drop policy if exists "comments_select" on booknest.comments;
create policy "comments_select" on booknest.comments for select to anon, authenticated using (
  user_id = (select auth.uid())
  or exists (select 1 from booknest.books b
             where b.id = comments.book_id and (b.is_published or b.user_id = (select auth.uid())))
);
drop policy if exists "comments_insert_own" on booknest.comments;
create policy "comments_insert_own" on booknest.comments for insert to authenticated with check (
  user_id = (select auth.uid())
  and exists (select 1 from booknest.books b
              where b.id = comments.book_id and (b.is_published or b.user_id = (select auth.uid())))
);
drop policy if exists "comments_delete_author_or_book_owner" on booknest.comments;
create policy "comments_delete_author_or_book_owner" on booknest.comments for delete to authenticated using (
  user_id = (select auth.uid())
  or exists (select 1 from booknest.books b where b.id = comments.book_id and b.user_id = (select auth.uid()))
);

-- read_history (insert shudhu track_book_read() diye)
drop policy if exists "read_history_select_own" on booknest.read_history;
create policy "read_history_select_own" on booknest.read_history for select to authenticated
  using (user_id = (select auth.uid()));

-- user_keys
drop policy if exists "user_keys_select_own" on booknest.user_keys;
create policy "user_keys_select_own" on booknest.user_keys for select to authenticated
  using (user_id = (select auth.uid()));
drop policy if exists "user_keys_insert_own" on booknest.user_keys;
create policy "user_keys_insert_own" on booknest.user_keys for insert to authenticated
  with check (user_id = (select auth.uid()));
drop policy if exists "user_keys_update_own" on booknest.user_keys;
create policy "user_keys_update_own" on booknest.user_keys for update to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

-- ---------------------------------------------------------------------
-- GRANTS (least privilege; column-level jate counter/user_id user nije bodlate na pare)
-- ---------------------------------------------------------------------
revoke all on all tables    in schema booknest from anon, authenticated;
revoke all on all functions in schema booknest from public, anon, authenticated;

grant all on all tables in schema booknest to service_role;
grant execute on all functions in schema booknest to service_role;

-- follows ta anon er jonno SELECT grant lagbe karon profiles policy er vitore follows query hoy (RLS e follows er kono
-- policy anon ke dey na, tai anon kono row pay na).
grant select on booknest.profiles, booknest.books, booknest.chapters, booknest.comments, booknest.follows to anon;
grant select on booknest.profiles, booknest.books, booknest.chapters, booknest.comments,
                booknest.likes, booknest.follows, booknest.read_history, booknest.user_keys to authenticated;

grant update (name, avatar_url, bio, category) on booknest.profiles to authenticated;

grant insert (user_id, title, description, genre, cover_url) on booknest.books to authenticated;
grant update (title, description, genre, cover_url, is_published) on booknest.books to authenticated;
grant delete on booknest.books to authenticated;

grant insert (book_id, title, content, is_encrypted, "order") on booknest.chapters to authenticated;
grant update (title, content, is_encrypted, "order") on booknest.chapters to authenticated;
grant delete on booknest.chapters to authenticated;

grant insert (user_id, book_id) on booknest.likes to authenticated;
grant delete on booknest.likes to authenticated;

grant insert (follower_id, following_id) on booknest.follows to authenticated;
grant delete on booknest.follows to authenticated;

grant insert (user_id, book_id, content) on booknest.comments to authenticated;
grant delete on booknest.comments to authenticated;

grant insert, update on booknest.user_keys to authenticated;

grant execute on function booknest.ensure_profile()                    to authenticated;
grant execute on function booknest.track_book_read(uuid)               to anon, authenticated;
grant execute on function booknest.get_follower_count(uuid)            to anon, authenticated;
grant execute on function booknest.get_following_count(uuid)           to anon, authenticated;
grant execute on function booknest.replace_chapters(uuid, jsonb)       to authenticated;
grant execute on function booknest.delete_my_data()                    to authenticated;

-- ---------------------------------------------------------------------
-- STORAGE: alada bucket (onno app er bucket/policy ke chhoy na)
-- ---------------------------------------------------------------------
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('booknest-avatars', 'booknest-avatars', true, 2097152,
        array['image/jpeg', 'image/png', 'image/webp'])
on conflict (id) do update
  set public = excluded.public,
      file_size_limit = excluded.file_size_limit,
      allowed_mime_types = excluded.allowed_mime_types;

-- Public URL diye chobi dekha jay (bucket public), kintu LISTING shudhu nijer folder e.
drop policy if exists "booknest_avatars_select_own" on storage.objects;
create policy "booknest_avatars_select_own" on storage.objects for select to authenticated
  using (bucket_id = 'booknest-avatars' and (storage.foldername(name))[1] = (select auth.uid()::text));

drop policy if exists "booknest_avatars_insert_own" on storage.objects;
create policy "booknest_avatars_insert_own" on storage.objects for insert to authenticated
  with check (bucket_id = 'booknest-avatars' and (storage.foldername(name))[1] = (select auth.uid()::text));

drop policy if exists "booknest_avatars_update_own" on storage.objects;
create policy "booknest_avatars_update_own" on storage.objects for update to authenticated
  using (bucket_id = 'booknest-avatars' and (storage.foldername(name))[1] = (select auth.uid()::text))
  with check (bucket_id = 'booknest-avatars' and (storage.foldername(name))[1] = (select auth.uid()::text));

drop policy if exists "booknest_avatars_delete_own" on storage.objects;
create policy "booknest_avatars_delete_own" on storage.objects for delete to authenticated
  using (bucket_id = 'booknest-avatars' and (storage.foldername(name))[1] = (select auth.uid()::text));

-- ---------------------------------------------------------------------
-- Dashboard e API reload (exposed schema add korar por-o lagte pare)
-- ---------------------------------------------------------------------
notify pgrst, 'reload schema';
