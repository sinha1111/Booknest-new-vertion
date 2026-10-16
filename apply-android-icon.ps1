# BookNest Android icon fix.
# Run koro project root theke (jekhane "android" ar "android-res" folder ache):
#   powershell -ExecutionPolicy Bypass -File .\apply-android-icon.ps1
param([switch]$SkipClean)
$ErrorActionPreference = "Stop"
$res = "android\app\src\main\res"
$manifest = "android\app\src\main\AndroidManifest.xml"

if (-not (Test-Path $res)) { Write-Host "android folder nai. Age cholao: npx cap add android" -ForegroundColor Red; exit 1 }
if (-not (Test-Path "android-res")) { Write-Host "android-res folder nai (project root e thakte hobe)." -ForegroundColor Red; exit 1 }

# 1) Purono shob launcher icon file muchi (png, webp, xml, drawable) - eigulo-i default icon dekhay
Get-ChildItem -Path $res -Recurse -File -Filter "ic_launcher*" | ForEach-Object {
    Write-Host "delete:" $_.FullName
    Remove-Item $_.FullName -Force
}

# 2) Notun icon boshai
Get-ChildItem -Path "android-res" -Directory -Filter "mipmap-*" | ForEach-Object {
    Copy-Item $_.FullName $res -Recurse -Force
}
New-Item -ItemType Directory -Force -Path "$res\values" | Out-Null
Copy-Item "android-res\values\ic_launcher_background.xml" "$res\values\" -Force

# 3) Adaptive icon XML (Android 8+)
$utf8 = New-Object System.Text.UTF8Encoding($false)
New-Item -ItemType Directory -Force -Path "$res\mipmap-anydpi-v26" | Out-Null
$adaptive = @'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
</adaptive-icon>
'@
[System.IO.File]::WriteAllText((Resolve-Path "$res\mipmap-anydpi-v26").Path + "\ic_launcher.xml", $adaptive, $utf8)
[System.IO.File]::WriteAllText((Resolve-Path "$res\mipmap-anydpi-v26").Path + "\ic_launcher_round.xml", $adaptive, $utf8)

# 4) Manifest e icon thik jaygay point kore kina
$m = [System.IO.File]::ReadAllText((Resolve-Path $manifest).Path)
$m = [regex]::Replace($m, 'android:icon="[^"]*"', 'android:icon="@mipmap/ic_launcher"')
if ($m -match 'android:roundIcon=') {
    $m = [regex]::Replace($m, 'android:roundIcon="[^"]*"', 'android:roundIcon="@mipmap/ic_launcher_round"')
} else {
    $m = $m -replace 'android:icon="@mipmap/ic_launcher"', 'android:icon="@mipmap/ic_launcher" android:roundIcon="@mipmap/ic_launcher_round"'
}
[System.IO.File]::WriteAllText((Resolve-Path $manifest).Path, $m, $utf8)

# 5) Check
Write-Host ""
Write-Host "Manifest:" -ForegroundColor Cyan
Select-String -Path $manifest -Pattern 'android:icon|android:roundIcon' | ForEach-Object { $_.Line.Trim() }
Write-Host ""
Write-Host "Icon files:" -ForegroundColor Cyan
Get-ChildItem -Path $res -Recurse -File -Filter "ic_launcher*" | ForEach-Object { $_.FullName.Replace((Get-Location).Path + "\", "") }

# 6) Clean build
if (-not $SkipClean) {
    Push-Location android
    .\gradlew.bat clean
    Pop-Location
}
Write-Host ""
Write-Host "Done. Ekhon phone theke BookNest UNINSTALL koro, tarpor abar install/run koro." -ForegroundColor Green
