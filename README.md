# BookNest (Supabase self-hosted backend version)

Lovable theke clone kora. Ekhon apnar nijer Supabase project er **`booknest` schema** te kaj kore —
onno 2ta app er table/policy/function/bucket er shathe kono mishamishi nei.

## Setup (ei order e)

### 1. Database
Supabase Dashboard -> **SQL Editor** -> New query -> `supabase/booknest.sql` er pura content paste -> **Run**.
(`supabase db push` / `db reset` chalabe na — shared project e onno app er migration history er shathe somossha hobe.)
Dobara run korle kichu bhenge jay na (idempotent).

### 2. Schema expose koro (khub important)
Dashboard -> **Project Settings -> Data API** (ba API) -> **Exposed schemas** -> `booknest` **add** koro (age ja ache ta rekhe) -> Save.
Eta na korle app e "schema must be one of the following" / 406 error ashbe.

### 3. Environment
```
cp .env.example .env
```
`.env` e `VITE_SUPABASE_URL` ar anon/publishable key dao (Project Settings -> API).
**service_role key kokhono .env / frontend e dibe na.**

### 4. Delete-account function
```
npm i -g supabase          # ba npx supabase
supabase login
supabase link --project-ref YOUR_PROJECT_REF
supabase functions deploy booknest-delete-account
```
Production e: `supabase secrets set ALLOWED_ORIGIN=https://your-site.com`

### 5. Auth settings (project-wide, shared!)
Dashboard -> Authentication -> URL Configuration: BookNest er URL ta **Redirect URLs** e add koro
(onno app er URL muche felo na). Email confirmation on thakle signup er por mail confirm kore sign in korte hobe.

### 6. Run
```
npm install
npm run dev     # http://localhost:8080
```

## Kon jinish alada, kon jinish shared

| Jinish | Status |
|---|---|
| Tables, RLS policies, functions, triggers | **Alada** — `booknest` schema |
| Avatar storage | **Alada** — `booknest-avatars` bucket, nijer policy |
| Edge function | **Alada nam** — `booknest-delete-account` |
| Browser session | **Alada** — storage key `booknest-auth` |
| `auth.users` (email + password login) | **Shared** — Supabase er nijom, project-e ekta-i user table |
| Auth settings (Site URL, email template, SMTP, rate limit) | **Shared** |

**Shared login er mane:** je email diye onno app e account ache, oi email+password diye BookNest e sign in korle chole —
BookNest e tar notun alada profile toiri hoy (data mishe na). Onno app er user ra BookNest e automatic dekha jay na
(profile tokhon-i toiri hoy jokhon se BookNest e login kore), ar `auth.users` e kono trigger lagano hoyni, tai onno app er signup e kichu hoy na.
Jodi login-o puropuri alada chao, tahole alada Supabase project lagbe — eta Supabase er limitation.

Account delete: shudhu BookNest er data muchhe, `auth.users` er user ke na (noile onno app er account-o jeto).

## Security

- Shob table e RLS on; column-level grant — user nijer `user_id`, like/comment/read counter, follower count bodlate pare na (DB trigger maintain kore).
- Reading history shudhu owner dekhe; likes/follows shudhu nijer; published book ar tar comment public.
- Avatar: sudhu jpg/png/webp, max 2MB (bucket level e enforce), listing shudhu nijer folder.
- `replace_chapters` atomic — save fail korle chapter hariye jay na.
- Service-role key kothao frontend e nei; `.env` gitignored.

## End-to-end encryption (draft)

- **Ki encrypt hoy:** unpublished book er chapter title + content. Browser e AES-256-GCM diye encrypt hoy, server e shudhu ciphertext jay.
- **Key:** random 256-bit key; ota "vault passphrase" (PBKDF2-SHA256, 600k) ar ekta **recovery code** diye wrap kore `user_keys` table e rakha hoy.
  Passphrase/recovery code/key server e kokhono jay na. Login password theke alada.
- **Ki encrypt hoy NA:** book er title/description/genre (list e dekhate hoy), ar **published** book (public hobar jonno-i; publish korle chapter plain hoy — DB trigger encrypted chapter publish hote dey na).
- **Hariye gele:** passphrase **ar** recovery code duto-i hariye gele draft uddhar kora jabe na (eta E2EE er nature).
- Dashboard theke tumi/Supabase admin draft chapter porte parbe na — ciphertext dekhbe.

## Verify (SQL Editor e)
```sql
select tablename, rowsecurity from pg_tables where schemaname = 'booknest';           -- shob true
select * from pg_policies where schemaname = 'booknest';
select id, public, file_size_limit from storage.buckets where id = 'booknest-avatars';
```
Supabase Dashboard -> Advisors -> Security Advisor ও ekbar check koro.

## Remove korte chaile
`supabase/booknest_uninstall.sql` (age bucket empty koro).

## Android build (Capacitor 8)
Requirements: **Node 22+**, **Android Studio Otter (2025.2.1+)** (JDK 21 + SDK 36 shoho).

Test APK:
```
powershell -ExecutionPolicy Bypass -File .\build-android.ps1
```
Play Store AAB (signed, R8 on):
1. Ekbar keystore banao: `keytool -genkey -v -keystore booknest-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias booknest`
2. `keystore.properties.example` copy kore `keystore.properties` koro, value boshao.
3. `powershell -ExecutionPolicy Bypass -File .\build-android.ps1 -Aab -VersionCode 2 -VersionName 1.1` (protibar VersionCode barao)

Icon `assets/` ar `android-res/` theke ashe.
