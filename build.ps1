<#
  ChannelCompat 构建脚本
  用法:
    .\build.ps1                                    # 自动找 %APPDATA%\.minecraft
    .\build.ps1 -MinecraftDir "D:\pcl2\.minecraft" # 指定 .minecraft 目录
    .\build.ps1 -MinecraftDir "D:\pcl2\.minecraft" -ForgeVersion "1.20.1-47.4.26"
#>
param(
    [string]$MinecraftDir = (Join-Path $env:APPDATA '.minecraft'),
    [string]$ForgeVersion = '1.20.1-47.4.26'
)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$lib  = Join-Path $MinecraftDir 'libraries'
$out  = Join-Path $root 'build'
$cls  = Join-Path $out 'classes'

if (-not (Test-Path $lib)) { throw "找不到依赖目录: $lib（用 -MinecraftDir 指定）" }

function Find-Jar([string]$pattern) {
    $j = Get-ChildItem -LiteralPath $lib -Recurse -Filter $pattern -ErrorAction SilentlyContinue |
         Sort-Object FullName -Descending | Select-Object -First 1
    if (-not $j) { throw "找不到依赖: $pattern（检查 -MinecraftDir / -ForgeVersion）" }
    return $j.FullName
}

$mixin  = Find-Jar 'mixin-0.8.5.jar'
$forge  = Join-Path $lib "net\minecraftforge\forge\$ForgeVersion\forge-$ForgeVersion-universal.jar"
$javafml= Join-Path $lib "net\minecraftforge\javafmllanguage\$ForgeVersion\javafmllanguage-$ForgeVersion.jar"
$slf4j  = Find-Jar 'slf4j-api-*.jar'
foreach ($p in $mixin, $forge, $javafml, $slf4j) { if (-not (Test-Path $p)) { throw "缺少: $p" } }

Remove-Item -LiteralPath $out -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $cls | Out-Null

Write-Host '编译中...' -ForegroundColor Cyan
$srcs = Get-ChildItem -LiteralPath (Join-Path $root 'src\main\java') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName
& javac --release 17 -nowarn -cp "$mixin;$forge;$javafml;$slf4j" -d $cls $srcs
if ($LASTEXITCODE -ne 0) { throw '编译失败' }

Copy-Item -LiteralPath (Join-Path $root 'src\main\resources\*') -Destination $cls -Recurse -Force
$mf = Join-Path $out 'MANIFEST.MF'
@"
Manifest-Version: 1.0
MixinConfigs: channelcompat.mixins.json
Implementation-Title: ChannelCompat
Implementation-Version: 1.0.0
"@ | Set-Content -LiteralPath $mf -Encoding ascii

$jarPath = Join-Path $out 'channelcompat-1.0.0.jar'
& jar --create --file $jarPath --manifest $mf -C $cls .
Write-Host "构建完成: $jarPath" -ForegroundColor Green