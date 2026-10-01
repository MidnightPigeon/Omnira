param([Parameter(ValueFromRemainingArguments=$true)][string[]]$GradleArgs)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
$localGradleHome = Join-Path $project '.gradle-local'
$distribution = Get-ChildItem -LiteralPath (Join-Path $localGradleHome 'wrapper/dists/gradle-9.2.1-bin') -Filter 'gradle.bat' -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object { $_.DirectoryName -match '[\\/]gradle-9\.2\.1[\\/]bin$' } |
    Select-Object -First 1
if ($null -eq $distribution) { throw 'Local Gradle 9.2.1 is missing from .gradle-local.' }
$env:GRADLE_USER_HOME = $localGradleHome
& $distribution.FullName --offline --no-daemon @GradleArgs
exit $LASTEXITCODE
