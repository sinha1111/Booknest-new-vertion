-- BookNest puropuri muche fela (SHUDHU booknest schema + booknest-avatars bucket policy).
-- Onno app er kichu chhoy na. AGE Dashboard -> Storage -> booknest-avatars bucket ta empty kore delete koro
-- (SQL diye storage.objects delete korle file orphan thake).

drop policy if exists "booknest_avatars_select_own" on storage.objects;
drop policy if exists "booknest_avatars_insert_own" on storage.objects;
drop policy if exists "booknest_avatars_update_own" on storage.objects;
drop policy if exists "booknest_avatars_delete_own" on storage.objects;

drop schema if exists booknest cascade;
notify pgrst, 'reload schema';
