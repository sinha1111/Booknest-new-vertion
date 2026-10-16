# BookNest -> Android build
#
#   Test APK (phone e install korar jonno):
#       powershell -ExecutionPolicy Bypass -File .\build-android.ps1
#
#   Play Store AAB (signed, R8 on):
#       powershell -ExecutionPolicy Bypass -File .\build-android.ps1 -Aab -VersionCode 2 -VersionName 1.1
#       (protibar Play Store e notun upload er age VersionCode barabe)
param(
    [switch]$Aab,
    [int]$VersionCode = 2,
    [string]$VersionName = "1.1"
)
$ErrorActionPreference = "Stop"
function Step($m) { Write-Host ""; Write-Host "==> $m" -ForegroundColor Cyan }
function Fail($m) { Write-Host ""; Write-Host "ERROR: $m" -ForegroundColor Red; exit 1 }
function Check($what) { if ($LASTEXITCODE -ne 0) { Fail "$what fail korechhe (exit code $LASTEXITCODE). Upor er error ta dekho." } }

Set-Location $PSScriptRoot

# ---------- 0) Tools ----------
Step "Tools check"
if (-not (Get-Command node -ErrorAction SilentlyContinue)) { Fail "Node.js nai. https://nodejs.org theke LTS (22+) install koro." }
$nodeMajor = [int]((node -v).TrimStart("v").Split(".")[0])
if ($nodeMajor -lt 22) { Fail "Capacitor 8 er jonno Node 22+ lagbe (ekhon $(node -v)). https://nodejs.org theke LTS install koro." }

if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $jbr = "$env:ProgramFiles\Android\Android Studio\jbr"
    if (Test-Path "$jbr\bin\java.exe") { $env:JAVA_HOME = $jbr } else { Fail "JDK 21 pawa jay nai. Android Studio (Otter 2025.2.1+) install koro, shathe JDK ashe." }
}
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Write-Host "JAVA_HOME = $env:JAVA_HOME"

if (-not $env:ANDROID_HOME -or -not (Test-Path $env:ANDROID_HOME)) {
    $sdk = "$env:LOCALAPPDATA\Android\Sdk"
    if (Test-Path $sdk) { $env:ANDROID_HOME = $sdk } else { Fail "Android SDK pawa jay nai. Android Studio khule SDK install koro (Settings > Android SDK, API 36)." }
}
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
Write-Host "ANDROID_HOME = $env:ANDROID_HOME"

if (-not (Test-Path ".env")) { Fail ".env file nai (VITE_SUPABASE_URL / VITE_SUPABASE_PUBLISHABLE_KEY)." }
if ($Aab -and -not (Test-Path "keystore.properties")) {
    Fail "keystore.properties nai. keystore.properties.example copy kore nijer keystore er value dao (README dekho)."
}

# ---------- 1) Purono Capacitor 7 android folder thakle (kichu delete kora hoy na) ----------
if (Test-Path "android\variables.gradle") {
    $t = Select-String -Path "android\variables.gradle" -Pattern 'targetSdkVersion\s*=\s*(\d+)' | Select-Object -First 1
    if ($t -and [int]$t.Matches[0].Groups[1].Value -lt 36) {
        Fail "android folder er targetSdkVersion $($t.Matches[0].Groups[1].Value) (36 lagbe). Folder ta ami delete korbo na. Nijer hate rename koro (jemon android-old) tarpor script abar chalao, ba 'npx cap migrate' chalao."
    }
}

# ---------- 2) Web build ----------
Step "npm install"
npm install
Check "npm install"

Step "Web build (npm run build)"
npm run build
Check "npm run build"

# ---------- 3) Android project ----------
if (-not (Test-Path "android")) {
    Step "Android project toiri (npx cap add android)"
    npx cap add android
    Check "cap add android"
}

$sdkEscaped = $env:ANDROID_HOME.Replace("\", "\\").Replace(":", "\:")
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "android\local.properties"), "sdk.dir=$sdkEscaped`n", (New-Object System.Text.UTF8Encoding($false)))

# ---------- 4) Icon ----------
Step "BookNest icon boshano"
& "$PSScriptRoot\apply-android-icon.ps1" -SkipClean

# ---------- 5) R8 + version + signing ----------
Step "Gradle config (R8, version, signing)"
node scripts/patch-android.mjs --version-code $VersionCode --version-name $VersionName
Check "patch-android"

# ---------- 6) Sync ----------
Step "npx cap sync android"
npx cap sync android
Check "cap sync"

# ---------- 7) Build ----------
Push-Location android
if ($Aab) {
    Step "AAB build (bundleRelease)"
    .\gradlew.bat clean bundleRelease
} else {
    Step "APK build (assembleDebug) - prothom bar Gradle download e kichukkhon lagbe"
    .\gradlew.bat assembleDebug
}
$code = $LASTEXITCODE
Pop-Location
if ($code -ne 0) { Fail "Gradle build fail korechhe. Upor er error ta amake pathao." }

if ($Aab) {
    $out = "android\app\build\outputs\bundle\release\app-release.aab"
    if (-not (Test-Path $out)) { Fail "AAB file pawa jay nai: $out" }
    Copy-Item $out "BookNest.aab" -Force
    Write-Host ""; Write-Host "DONE: $(Resolve-Path 'BookNest.aab')  (versionCode $VersionCode, versionName $VersionName)" -ForegroundColor Green
    Write-Host "Play Console e ei file ta upload koro. R8 er mapping file AAB er bhitor-i thake."
} else {
    $out = "android\app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path $out)) { Fail "APK file pawa jay nai: $out" }
    Copy-Item $out "BookNest.apk" -Force
    Write-Host ""; Write-Host "DONE: $(Resolve-Path 'BookNest.apk')" -ForegroundColor Green
    Write-Host "Eta test APK. Play Store er jonno -Aab diye build koro."
}
