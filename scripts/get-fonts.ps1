# BookNest er font (Playfair Display + Source Sans 3) download kore app/src/main/res/font e rakhe.
# Font na thakle app system font e chole (kaj kore, kintu design hubohu na).
#   powershell -ExecutionPolicy Bypass -File .\scripts\get-fonts.ps1
$ErrorActionPreference = "Continue"
$ProgressPreference = "SilentlyContinue"
$root = Split-Path -Parent $PSScriptRoot
$dir = Join-Path $root "app\src\main\res\font"
New-Item -ItemType Directory -Force -Path $dir | Out-Null

$fonts = @(
    @{ Name = "playfair_display.ttf"; Urls = @(
        "https://github.com/google/fonts/raw/main/ofl/playfairdisplay/PlayfairDisplay%5Bwght%5D.ttf",
        "https://raw.githubusercontent.com/google/fonts/main/ofl/playfairdisplay/PlayfairDisplay%5Bwght%5D.ttf") },
    @{ Name = "source_sans_3.ttf"; Urls = @(
        "https://github.com/google/fonts/raw/main/ofl/sourcesans3/SourceSans3%5Bwght%5D.ttf",
        "https://raw.githubusercontent.com/google/fonts/main/ofl/sourcesans3/SourceSans3%5Bwght%5D.ttf") }
)

foreach ($f in $fonts) {
    $target = Join-Path $dir $f.Name
    if ((Test-Path $target) -and ((Get-Item $target).Length -gt 50000)) {
        Write-Host "OK (already): $($f.Name)"
        continue
    }
    $done = $false
    foreach ($u in $f.Urls) {
        try {
            Invoke-WebRequest -Uri $u -OutFile $target -UseBasicParsing
            if ((Get-Item $target).Length -gt 50000) { $done = $true; break }
        } catch {
            Write-Host "  failed: $u"
        }
    }
    if ($done) {
        Write-Host "OK: $($f.Name)" -ForegroundColor Green
    } else {
        if (Test-Path $target) { Remove-Item $target -Force }
        Write-Host "WARNING: $($f.Name) download hoy ni - system font e chalbe." -ForegroundColor Yellow
    }
}
