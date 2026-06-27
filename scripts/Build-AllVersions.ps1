param(
    [string[]] $Version = @('1.21.8', '1.21.9', '1.21.10', '1.21.11', '26.1', '26.2'),
    [switch] $Prefetch
)

$ErrorActionPreference = 'Stop'
$projectDir = Split-Path -Parent $PSScriptRoot
$distDir = Join-Path $projectDir 'dist'
$java21Home = if ($env:JAVA21_HOME) { $env:JAVA21_HOME } else { $env:JAVA_HOME }
$java25Home = if ($env:JAVA25_HOME) {
    $env:JAVA25_HOME
} elseif (Test-Path (Join-Path $env:USERPROFILE '.gradle\jdks\jdk-25.0.2')) {
    Join-Path $env:USERPROFILE '.gradle\jdks\jdk-25.0.2'
} else {
    $null
}

if ($Prefetch) {
    & (Join-Path $PSScriptRoot 'Prefetch-Minecraft.ps1') -Version $Version
}
New-Item -ItemType Directory -Force -Path $distDir | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem

foreach ($minecraftVersion in $Version) {
    if ($minecraftVersion.StartsWith('26.')) {
        if (-not $java25Home) {
            throw 'Java 25 is required for Minecraft 26.x. Set JAVA25_HOME.'
        }
        $env:JAVA_HOME = $java25Home
    } elseif ($java21Home) {
        $env:JAVA_HOME = $java21Home
    }
    if ($env:JAVA_HOME) {
        $env:Path = "$env:JAVA_HOME\bin;" + (($env:Path -split ';' | Where-Object { $_ -notmatch '\\bin$' }) -join ';')
    }

    Write-Host "Building Minecraft $minecraftVersion"
    & (Join-Path $projectDir 'gradlew.bat') clean build "-Ptarget_version=$minecraftVersion" --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed for Minecraft $minecraftVersion"
    }

    $jar = Get-ChildItem (Join-Path $projectDir 'build\libs') -Filter 'CloudsdaleExpress-*.jar' |
        Where-Object { $_.Name -notmatch '-sources\.jar$' } |
        Select-Object -First 1
    if (-not $jar) {
        throw "No release jar was produced for Minecraft $minecraftVersion"
    }

    $zip = [IO.Compression.ZipFile]::OpenRead($jar.FullName)
    try {
        $entryNames = @($zip.Entries.FullName)
        foreach ($requiredEntry in @(
            'com/smoothquartz/minecart/mixin/MinecartControllerAccessor.class',
            'com/smoothquartz/minecart/mixin/MinecartPhysicsMixin.class',
            'fabric.mod.json',
            'mixins.json'
        )) {
            if ($requiredEntry -notin $entryNames) {
                throw "$requiredEntry is missing from $($jar.Name)"
            }
        }

        $mixinEntry = $zip.GetEntry('mixins.json')
        $reader = [IO.StreamReader]::new($mixinEntry.Open())
        try {
            $mixinConfig = $reader.ReadToEnd() | ConvertFrom-Json
        } finally {
            $reader.Dispose()
        }
        if ('MinecartControllerAccessor' -notin $mixinConfig.mixins -or 'MinecartPhysicsMixin' -notin $mixinConfig.mixins) {
            throw "The required minecart Mixins are not registered in $($jar.Name)"
        }

        $metadataEntry = $zip.GetEntry('fabric.mod.json')
        $reader = [IO.StreamReader]::new($metadataEntry.Open())
        try {
            $metadata = $reader.ReadToEnd() | ConvertFrom-Json
        } finally {
            $reader.Dispose()
        }
        if ($metadata.depends.minecraft -ne $minecraftVersion) {
            throw "$($jar.Name) declares Minecraft $($metadata.depends.minecraft), expected $minecraftVersion"
        }
    } finally {
        $zip.Dispose()
    }

    Copy-Item -LiteralPath $jar.FullName -Destination (Join-Path $distDir $jar.Name) -Force
}

Write-Host "All requested versions built and verified in $distDir"
