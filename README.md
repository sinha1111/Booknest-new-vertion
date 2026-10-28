# BookNest - Native Android (Kotlin + Jetpack Compose)

Capacitor/WebView chhara puro native app. **Backend ager moto-i**: same Supabase project (`booknest` schema, RLS, storage bucket
`booknest-avatars`, edge function `booknest-delete-account`). Kono SQL ba server side bodlano lage ni.
Design (rong, layout, font, tab, card) web version er moto-i; shob page ager moto: Home, Explore, Write, Editor, Book Detail,
Reader, Profile, Settings, Auth.

## Build (ek command)
Requirements: **Android Studio** (JDK 21 + SDK 36 shoho). Internet lagbe (prothom bar).

```
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1
```
Result: `BookNest.apk`. **Purono Capacitor BookNest app phone theke UNINSTALL kore** eta install koro (signature alada).

Script ja kore: JDK/SDK khuje ney, font download kore (Playfair Display + Source Sans 3), Gradle wrapper ready kore
(purono `android\` folder theke copy, na hole Gradle download kore), tarpor build kore.

Android Studio diye korte chaile: folder ta **Open** koro -> Sync -> Run. (Prothome ekbar `build-apk.ps1` chalaile wrapper/font ready hoye jay.)

## Play Store AAB (ager app er update)
Same package (`com.tricosoft.booknest`) ar **same keystore** lagbe, noile Play Store update nibe na.
1. `keystore.properties.example` copy kore `keystore.properties` koro, ager upload keystore er value dao.
2. ```
   powershell -ExecutionPolicy Bypass -File .\build-apk.ps1 -Aab -VersionCode 7 -VersionName 7.0.0
   ```
   (`VersionCode` ager release er cheye BORO hote hobe.)
3. `BookNest.aab` Play Console e upload koro.

## Supabase config
`supabase.properties` e URL ar publishable key ache (build er shomoy app e boshe). `service_role` / secret key kokhono ekhane dibe na.

## E2EE draft encryption
Web er shathe byte-compatible: web e likha encrypted draft app e khole, app e likha draft web e khole (same passphrase / recovery code).
AES-256-GCM, PBKDF2-SHA256 (600k), HKDF. Passphrase/key server e jay na; session token Android Keystore diye encrypted.
Algorithm ta web crypto (WebCrypto) er shathe cross-test kora hoyeche.

## Folder
```
app/src/main/java/com/booknest/app/
  data/     Supabase REST client (Auth, PostgREST, Storage, Functions), models, repository
  crypto/   E2EE (VaultCrypto)
  state/    app state (session, vault, theme, language, toast)
  ui/       theme, components, navigation, screens/
app/src/main/res/  icon (booknest-v2 theke), splash, themes
```

## Jodi build fail kore
Error er pura output ta pathao (Gradle er `e:` line gulo). Ei code ta Kotlin compiler diye ami chalate pari ni, tai chhoto-khato
compile error ashte pare - oigulo fix kore dibo.

## Device support (minSdk)
`minSdk 24` (Android 7+), ager Capacitor release er moto. Tai ager release e je device gulo support korto, ekhon-o korbe.
AAB te native debug symbol table dewa ache (Play Console er "no debug symbols" warning er jonno).

## Play Console: "native code, no debug symbols" warning
Eta sudhu ekta suggestion (release block kore na). Karon: Compose er bhitore ekta AndroidX native lib ache (`libandroidx.graphics.path.so`),
jar debug info AndroidX nije-i strip kore dey, tai symbol file toiri kora jay na. App er nijer kono native code nei.
Warning ta ignore korte paro. Chaile native lib bad dite paro (optional, kom risk):
`.\build-apk.ps1 -Aab -NoNative -VersionCode 7 -VersionName 7.0.0`
Jodi kono screen e `UnsatisfiedLinkError` crash hoy, `-NoNative` chhara abar build koro.

## Privacy Policy
`https://app-privacy-center.blogspot.com/2026/08/booknest.html` - Login page, Sign Up page ar Settings (Legal + niche footer) e link ache.
URL bodlate: `app/build.gradle.kts` er `PRIVACY_URL`.
