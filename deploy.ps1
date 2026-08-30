<#
    Деплой SlimeRancher на сервер Cristalix.

    Одной командой: сборка -> проверка -> заливка во временные имена -> атомарная
    подмена на сервере -> отчёт. Сервер не трогает: остановка и запуск за тобой.

    Использование:
        .\deploy.ps1                 полный цикл (сборка + заливка)
        .\deploy.ps1 -SkipBuild      залить то, что уже собрано
        .\deploy.ps1 -Check          только посмотреть, что сейчас на сервере
#>

[CmdletBinding()]
param(
    [switch] $SkipBuild,
    [switch] $Check,
    [string] $User = 'lhoopynyashka148',
    [string] $ServerHost = 'mokou.dedi.c7x.dev',
    [string] $Root = '/home/lhoopynyashka148/slimes-server'
)

$ErrorActionPreference = 'Stop'
$project = $PSScriptRoot
$target  = "$User@$ServerHost"

$pluginLocal = Join-Path $project 'build\libs\slimes-1.0-SNAPSHOT.jar'
$bundleLocal = Join-Path $project 'slimehunt-client\build\libs\SlimeHunt-bundle.jar'
$pluginRemote = "$Root/plugins/slimes-1.0-SNAPSHOT.jar"
$bundleRemote = "$Root/plugins/Slimes/SlimeHunt-bundle.jar"
$bundleLimit = 1MB

function Step($text)  { Write-Host "`n== $text" -ForegroundColor Cyan }
function Ok($text)    { Write-Host "   OK  $text" -ForegroundColor Green }
function Warn($text)  { Write-Host "   !!  $text" -ForegroundColor Yellow }
function Die($text)   { Write-Host "`nSTOP: $text" -ForegroundColor Red; exit 1 }

function Invoke-Remote([string] $command) {
    $output = & ssh "$target" $command 2>&1
    if ($LASTEXITCODE -ne 0) { Die "ssh failed: $output" }
    return $output
}

# --- только осмотр ---------------------------------------------------------
if ($Check) {
    Step "What is on the server now"
    Invoke-Remote "ls -lh '$pluginRemote' '$bundleRemote' 2>/dev/null; echo '--- mod.properties ---'; unzip -p '$bundleRemote' mod.properties 2>/dev/null | grep -E '^name=|^version='" |
        ForEach-Object { Write-Host "   $_" }
    exit 0
}

# --- 1. сборка -------------------------------------------------------------
if (-not $SkipBuild) {
    Step "Building"
    & (Join-Path $project 'gradlew.bat') clean test jar :slimehunt-client:bundle
    if ($LASTEXITCODE -ne 0) { Die "build failed" }
    Ok "build finished"
} else {
    Warn "build skipped (-SkipBuild)"
}

# --- 2. проверка локальных артефактов --------------------------------------
Step "Checking local artifacts"
foreach ($file in @($pluginLocal, $bundleLocal)) {
    if (-not (Test-Path $file)) { Die "not found: $file" }
}

$bundleSize = (Get-Item $bundleLocal).Length
if ($bundleSize -gt $bundleLimit) {
    Die "bundle is $bundleSize bytes, over the 1 MB WADA limit: it will not be delivered"
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($bundleLocal)
try {
    $entry = $zip.GetEntry('mod.properties')
    if (-not $entry) { Die "mod.properties missing inside the bundle" }
    $reader = New-Object System.IO.StreamReader($entry.Open())
    $props = $reader.ReadToEnd()
    $reader.Close()
} finally { $zip.Dispose() }

$version = ([regex]::Match($props, 'version=(.+)')).Groups[1].Value.Trim()
if (-not $version) { Die "cannot read version from mod.properties" }

Ok ("plugin  " + [int]((Get-Item $pluginLocal).Length / 1KB) + " KB")
Ok ("bundle  " + [int]($bundleSize / 1KB) + " KB  (limit 1024 KB)")
Ok "client version $version"

# --- 3. сервер должен быть остановлен --------------------------------------
Step "Making sure the server is stopped"
$running = Invoke-Remote "pgrep -f 'craftbukkit|diamondpaper' | wc -l"
if ([int]($running.ToString().Trim()) -gt 0) {
    Die @"
the server is still running.

Replacing a jar under a running server produces a broken archive
and an 'invalid LOC header' error on start.

Open the server console, run:  stop
Wait until the java process exits, then run this script again.
"@
}
Ok "server is down"

# --- 4. заливка во временные имена -----------------------------------------
Step "Uploading"
& scp $pluginLocal "${target}:${pluginRemote}.tmp"
if ($LASTEXITCODE -ne 0) { Die "scp failed for the plugin" }
Ok "plugin uploaded as .tmp"

& scp $bundleLocal "${target}:${bundleRemote}.tmp"
if ($LASTEXITCODE -ne 0) { Die "scp failed for the bundle" }
Ok "bundle uploaded as .tmp"

# --- 5. атомарная подмена ---------------------------------------------------
Step "Swapping files"
$swap = @"
set -e
for f in '$pluginRemote.tmp' '$bundleRemote.tmp'; do
  if [ ! -f "`$f" ]; then echo "NOT_A_FILE `$f"; exit 1; fi
done
mv -f '$pluginRemote.tmp' '$pluginRemote'
mv -f '$bundleRemote.tmp' '$bundleRemote'
ls -lh '$pluginRemote' '$bundleRemote'
echo '--- mod.properties ---'
unzip -p '$bundleRemote' mod.properties | grep -E '^name=|^version='
echo '--- other jars in plugins/ ---'
ls -1 '$Root/plugins/'*.jar
"@
Invoke-Remote $swap | ForEach-Object { Write-Host "   $_" }
Ok "files replaced"

# --- 6. что дальше ----------------------------------------------------------
Step "Done"
Write-Host "   Uploaded client version $version" -ForegroundColor Green
Write-Host ""
Write-Host "   Start the server:" -ForegroundColor Yellow
Write-Host "     cd ~/slimes-server && ./start.sh"
Write-Host ""
Write-Host "   Then look for in the log:" -ForegroundColor Yellow
Write-Host "     [WADA] Mod SlimeHunt $version loaded"
Write-Host "     [Slimes] SlimeHunt client loaded for <player> version=$version"
Write-Host ""
Write-Host "   If the list above shows more than one slimes-*.jar," -ForegroundColor Yellow
Write-Host "   delete the extra ones: Bukkit loads every jar and you get two plugins."
