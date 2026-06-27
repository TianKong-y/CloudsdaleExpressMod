param(
    [Parameter(Mandatory = $true)]
    [string[]] $Version,
    [int] $Segments = 4
)

$ErrorActionPreference = 'Stop'
$loomCache = Join-Path $env:USERPROFILE '.gradle\caches\fabric-loom'

function Test-Sha1([string] $Path, [string] $Expected) {
    if (-not (Test-Path -LiteralPath $Path)) {
        return $false
    }
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA1).Hash.ToLowerInvariant() -eq $Expected
}

function Get-SegmentedFile([string] $Url, [string] $FallbackUrl, [string] $Target, [long] $Length, [string] $Sha1) {
    if (Test-Sha1 $Target $Sha1) {
        Write-Host "Using verified cache: $Target"
        return
    }

    $partDir = "$Target.parts"
    New-Item -ItemType Directory -Force -Path $partDir | Out-Null
    $processes = @()

    for ($i = 0; $i -lt $Segments; $i++) {
        $start = [math]::Floor($Length * $i / $Segments)
        $end = [math]::Floor($Length * ($i + 1) / $Segments) - 1
        $part = Join-Path $partDir ("part-{0:D2}" -f $i)
        $processes += Start-Process -FilePath 'curl.exe' -ArgumentList @(
            '-fsSL', '--retry', '5', '--range', "$start-$end", '-o', $part, $Url
        ) -WindowStyle Hidden -PassThru
    }

    $processes | Wait-Process
    for ($i = 0; $i -lt $Segments; $i++) {
        $start = [math]::Floor($Length * $i / $Segments)
        $end = [math]::Floor($Length * ($i + 1) / $Segments) - 1
        $part = Join-Path $partDir ("part-{0:D2}" -f $i)
        $expectedPartLength = $end - $start + 1
        if (-not (Test-Path -LiteralPath $part) -or (Get-Item -LiteralPath $part).Length -ne $expectedPartLength) {
            & curl.exe -fsSL --retry 5 --range "$start-$end" -o $part $Url
            if ($LASTEXITCODE -ne 0 -or (Get-Item -LiteralPath $part).Length -ne $expectedPartLength) {
                & curl.exe -fsSL --retry 5 --range "$start-$end" -o $part $FallbackUrl
            }
            if ($LASTEXITCODE -ne 0 -or (Get-Item -LiteralPath $part).Length -ne $expectedPartLength) {
                throw "Mirror and fallback segment download failed: $part"
            }
        }
    }

    $output = [IO.File]::Create($Target)
    try {
        for ($i = 0; $i -lt $Segments; $i++) {
            $part = Join-Path $partDir ("part-{0:D2}" -f $i)
            $input = [IO.File]::OpenRead($part)
            try {
                $input.CopyTo($output)
            } finally {
                $input.Dispose()
            }
        }
    } finally {
        $output.Dispose()
    }

    if (-not (Test-Sha1 $Target $Sha1)) {
        throw "SHA-1 verification failed: $Target"
    }

    Get-ChildItem -LiteralPath $partDir -File | Remove-Item -Force
    Remove-Item -LiteralPath $partDir -Force
    Write-Host "Downloaded and verified: $Target"
}

foreach ($minecraftVersion in $Version) {
    $versionDir = Join-Path $loomCache $minecraftVersion
    New-Item -ItemType Directory -Force -Path $versionDir | Out-Null
    $metadataPath = Join-Path $versionDir 'mojang_minecraft_info.json'
    Invoke-WebRequest -UseBasicParsing `
        -Uri "https://bmclapi2.bangbang93.com/version/$minecraftVersion/json" `
        -OutFile $metadataPath
    $metadata = Get-Content -LiteralPath $metadataPath -Raw | ConvertFrom-Json

    foreach ($category in @('client', 'server')) {
        $download = $metadata.downloads.$category
        Get-SegmentedFile `
            -Url "https://bmclapi2.bangbang93.com/version/$minecraftVersion/$category" `
            -FallbackUrl $download.url `
            -Target (Join-Path $versionDir "minecraft-$category.jar") `
            -Length $download.size `
            -Sha1 $download.sha1
    }
}
