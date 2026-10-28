# BookNest native (Kotlin) -> APK / AAB
#
#   Test APK (phone e install korar jonno):
#       powershell -ExecutionPolicy Bypass -File .\build-apk.ps1
#
#   Release APK (signed, chhoto, share kora jay; keystore.properties lagbe):
#       powershell -ExecutionPolicy Bypass -File .\build-apk.ps1 -ReleaseApk
#
#   Build + phone e CLEAN install (USB debugging on, purono app uninstall kore):
#       powershell -ExecutionPolicy Bypass -File .\build-apk.ps1 -Install
#
#   AAB theke native lib bad diye (Play er "no debug symbols" warning er jonno, optional):
#       powershell -ExecutionPolicy Bypass -File .\build-apk.ps1 -Aab -NoNative -VersionCode 7 -VersionName 7.0.0
#
#   Play Store AAB (signed, R8 on):
#       powershell -ExecutionPolicy Bypass -File .\build-apk.ps1 -Aab -VersionCode 7 -VersionName 7.0.0
#       (protibar Play Store e notun upload er age VersionCode barabe)
param(
    [switch]$Aab,
    [switch]$ReleaseApk,
    [switch]$Install,
    [switch]$NoNative,
    [switch]$KeepNative,
    [int]$VersionCode = 7,
    [string]$VersionName = "7.0.0"
)
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
function Step($m) { Write-Host ""; Write-Host "==> $m" -ForegroundColor Cyan }
function Fail($m) { Write-Host ""; Write-Host "ERROR: $m" -ForegroundColor Red; exit 1 }
function Check($what) { if ($LASTEXITCODE -ne 0) { Fail "$what fail korechhe (exit code $LASTEXITCODE). Upor er error ta dekho." } }

function Show-IconEntries($file) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path $file).Path)
    try {
        $best = $null
        foreach ($e in $zip.Entries) {
            if ($e.FullName -match 'ic_launcher|splash_icon') { Write-Host "  in package: $($e.FullName)" }
            if ($e.FullName -match 'mipmap-xxxhdpi.*ic_launcher\.png$') { $best = $e }
        }
        if ($best) {
            $dest = Join-Path (Get-Location).Path "icon-in-package.png"
            [System.IO.Compression.ZipFileExtensions]::ExtractToFile($best, $dest, $true)
            Write-Host "  Package er bhitorer asol icon save hoyeche: $dest" -ForegroundColor Green
            Write-Host "  Eta khule dekho: BookNest icon hole build thik; phone e onno icon dekhale purono app/launcher cache."
        }
    } finally { $zip.Dispose() }
}

Set-Location $PSScriptRoot
$GradleVersion = "8.14.3"

# ---------- 0) Tools ----------
Step "Tools check"
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $jbr = "$env:ProgramFiles\Android\Android Studio\jbr"
    if (Test-Path "$jbr\bin\java.exe") { $env:JAVA_HOME = $jbr } else { Fail "JDK (17 ba 21) pawa jay nai. Android Studio install koro, shathe JDK ashe." }
}
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Write-Host "JAVA_HOME = $env:JAVA_HOME"

if (-not $env:ANDROID_HOME -or -not (Test-Path $env:ANDROID_HOME)) {
    $sdk = "$env:LOCALAPPDATA\Android\Sdk"
    if (Test-Path $sdk) { $env:ANDROID_HOME = $sdk } else { Fail "Android SDK pawa jay nai. Android Studio khule SDK install koro (Settings > Android SDK, API 36)." }
}
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
Write-Host "ANDROID_HOME = $env:ANDROID_HOME"

if (-not (Test-Path "supabase.properties")) { Fail "supabase.properties nai (SUPABASE_URL / SUPABASE_KEY)." }
if (($Aab -or $ReleaseApk) -and -not (Test-Path "keystore.properties")) {
    Fail "keystore.properties nai. keystore.properties.example copy kore AGER Play Store keystore er value dao."
}

$sdkEscaped = $env:ANDROID_HOME.Replace("\", "\\").Replace(":", "\:")
[System.IO.File]::WriteAllText((Join-Path (Get-Location).Path "local.properties"), "sdk.dir=$sdkEscaped`n", (New-Object System.Text.UTF8Encoding($false)))

# ---------- 1) Fonts (optional) ----------
Step "Font"
if (-not ((Test-Path "app\src\main\res\font\playfair_display.ttf") -and (Test-Path "app\src\main\res\font\source_sans_3.ttf"))) {
    & "$PSScriptRoot\scripts\get-fonts.ps1"
} else {
    Write-Host "Font ache."
}

# ---------- 2) Gradle wrapper ----------
Step "Gradle wrapper"
if (-not ((Test-Path "gradlew.bat") -and (Test-Path "gradle\wrapper\gradle-wrapper.jar"))) {
    $copied = $false
    # purono Capacitor android folder theke wrapper copy (sibling folder gulo e khuji)
    $parent = Split-Path -Parent (Get-Location).Path
    foreach ($d in Get-ChildItem -Path $parent -Directory -ErrorAction SilentlyContinue) {
        $a = Join-Path $d.FullName "android"
        if ((Test-Path "$a\gradlew.bat") -and (Test-Path "$a\gradle\wrapper\gradle-wrapper.jar")) {
            Write-Host "Wrapper copy korchi: $a"
            Copy-Item "$a\gradlew.bat" "gradlew.bat" -Force
            Copy-Item "$a\gradlew" "gradlew" -Force -ErrorAction SilentlyContinue
            New-Item -ItemType Directory -Force -Path "gradle\wrapper" | Out-Null
            Copy-Item "$a\gradle\wrapper\gradle-wrapper.jar" "gradle\wrapper\gradle-wrapper.jar" -Force
            $copied = $true
            break
        }
    }
    if (-not $copied) {
        Write-Host "Gradle $GradleVersion download korchi (ekbar-i)..."
        New-Item -ItemType Directory -Force -Path ".gradle-dist" | Out-Null
        $zip = ".gradle-dist\gradle-$GradleVersion-bin.zip"
        if (-not (Test-Path $zip)) {
            Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip" -OutFile $zip -UseBasicParsing
        }
        if (-not (Test-Path ".gradle-dist\gradle-$GradleVersion")) { Expand-Archive -Path $zip -DestinationPath ".gradle-dist" -Force }
        & ".gradle-dist\gradle-$GradleVersion\bin\gradle.bat" wrapper --gradle-version $GradleVersion
        Check "gradle wrapper"
    }
}
if (-not (Test-Path "gradlew.bat")) { Fail "gradlew.bat toiri hoy ni." }

# ---------- 3) Build ----------
if ($Aab) {
    Step "AAB build (bundleRelease), versionCode $VersionCode"
    $nn = "true"
    if ($KeepNative) { $nn = "false" }
    .\gradlew.bat clean bundleRelease "-PversionCode=$VersionCode" "-PversionName=$VersionName" "-PnoNative=$nn"
} elseif ($ReleaseApk) {
    Step "Release APK build (assembleRelease), versionCode $VersionCode"
    .\gradlew.bat clean assembleRelease "-PversionCode=$VersionCode" "-PversionName=$VersionName"
} else {
    Step "APK build (assembleDebug) - prothom bar Gradle/dependency download e kichukkhon lagbe"
    .\gradlew.bat assembleDebug "-PversionCode=$VersionCode" "-PversionName=$VersionName"
}
Check "Gradle build"

if ($Aab) {
    $out = "app\build\outputs\bundle\release\app-release.aab"
    if (-not (Test-Path $out)) { Fail "AAB file pawa jay nai: $out" }
    Copy-Item $out "BookNest.aab" -Force
    Write-Host ""; Write-Host "DONE: $(Resolve-Path 'BookNest.aab')  (versionCode $VersionCode, versionName $VersionName)" -ForegroundColor Green
    Write-Host "Play Console e upload koro."
} elseif ($ReleaseApk) {
    $out = "app\build\outputs\apk\release\app-release.apk"
    if (-not (Test-Path $out)) { Fail "Release APK pawa jay nai: $out (keystore.properties thik ache to?)" }
    Copy-Item $out "BookNest-release.apk" -Force
    Write-Host ""; Write-Host "DONE: $(Resolve-Path 'BookNest-release.apk')  (versionCode $VersionCode, versionName $VersionName)" -ForegroundColor Green
} else {
    $out = "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path $out)) { Fail "APK file pawa jay nai: $out" }
    Copy-Item $out "BookNest.apk" -Force
    Write-Host ""; Write-Host "DONE: $(Resolve-Path 'BookNest.apk')" -ForegroundColor Green
    Write-Host "Purono Capacitor BookNest app phone theke UNINSTALL kore eta install koro (signature alada)."
    Step "Icon APK er bhitore ache kina"
    Show-IconEntries "BookNest.apk"

    if ($Install) {
        Step "Phone e clean install (adb)"
        $adb = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
        if (-not (Test-Path $adb)) { Fail "adb pawa jay nai: $adb (Android Studio > SDK Manager > Platform-Tools)" }
        & $adb devices
        & $adb uninstall com.tricosoft.booknest | Out-Null
        & $adb install -r "BookNest.apk"
        Check "adb install"
        & $adb shell monkey -p com.tricosoft.booknest -c android.intent.category.LAUNCHER 1 | Out-Null
        Write-Host "Installed + launched. Settings page er niche 'BookNest v2.0 ... native' lekha thakle ei notun native app-i cholche." -ForegroundColor Green
    }
}
