param(
    [string] $Version = '1.21.8',
    [int] $TicksSeconds = 15
)

$ErrorActionPreference = 'Stop'
$projectDir = Split-Path -Parent $PSScriptRoot

function Start-Gradle([string] $Task, [string] $ExtraArguments) {
    $info = [Diagnostics.ProcessStartInfo]::new()
    $info.FileName = 'cmd.exe'
    $info.Arguments = "/d /c gradlew.bat $Task -Ptarget_version=$Version --no-daemon --console=plain $ExtraArguments"
    $info.WorkingDirectory = $projectDir
    $info.UseShellExecute = $false
    $info.RedirectStandardInput = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.CreateNoWindow = $true
    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $info
    if (-not $process.Start()) {
        throw "Could not start Gradle task: $Task"
    }
    return $process
}

function Stop-ProcessTree([Diagnostics.Process] $Process) {
    if (-not $Process.HasExited) {
        & taskkill.exe /PID $Process.Id /T /F | Out-Null
        $Process.WaitForExit()
    }
}

function Wait-ForOutput([Diagnostics.Process] $Process, [string] $Pattern, [int] $TimeoutSeconds) {
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    $readTask = $Process.StandardOutput.ReadLineAsync()
    while ([DateTime]::UtcNow -lt $deadline) {
        if ($readTask.Wait(250)) {
            $line = $readTask.Result
            if ($null -ne $line) {
                Write-Host $line
                if ($line -match $Pattern) {
                    return $true
                }
                $readTask = $Process.StandardOutput.ReadLineAsync()
            }
        }
        if ($Process.HasExited) {
            Write-Host ($Process.StandardError.ReadToEnd())
            return $false
        }
    }
    return $false
}

$server = Start-Gradle 'runServer' ''
if (-not (Wait-ForOutput $server '\bDone \(' 180)) {
    Stop-ProcessTree $server
    throw 'The smoke-test server did not finish starting.'
}

$commands = @(
    'setblock 0 65 0 minecraft:smooth_quartz',
    'setblock 2 65 0 minecraft:smooth_quartz',
    'setblock 4 65 0 minecraft:smooth_quartz',
    'setblock 6 65 0 minecraft:smooth_quartz',
    'setblock 8 65 0 minecraft:smooth_quartz',
    'setblock 10 65 0 minecraft:smooth_quartz',
    'summon minecraft:minecart 0 66 0',
    'summon minecraft:chest_minecart 2 66 0',
    'summon minecraft:hopper_minecart 4 66 0',
    'summon minecraft:furnace_minecart 6 66 0',
    'summon minecraft:tnt_minecart 8 66 0',
    'summon minecraft:command_block_minecart 10 66 0',
    'save-all flush',
    'stop'
)
foreach ($command in $commands) {
    $server.StandardInput.WriteLine($command)
}
$server.StandardInput.Close()
if (-not $server.WaitForExit(90000)) {
    Stop-ProcessTree $server
    throw 'The smoke-test server did not stop cleanly.'
}
if ($server.ExitCode -ne 0) {
    throw "The smoke-test server exited with code $($server.ExitCode)."
}

$client = Start-Gradle 'runClient' '--args="--quickPlaySingleplayer smoke-world --username CloudsdaleTest"'
$clientStartedAt = Get-Date
if (-not (Wait-ForOutput $client 'Handshake ACK received from server' 240)) {
    Stop-ProcessTree $client
    throw 'The integrated singleplayer server did not finish starting.'
}

Start-Sleep -Seconds $TicksSeconds
if ($client.HasExited) {
    throw "The client exited unexpectedly with code $($client.ExitCode)."
}
$gameProcess = Get-Process java, javaw -ErrorAction SilentlyContinue |
    Where-Object { $_.StartTime -ge $clientStartedAt -and $_.MainWindowHandle -ne 0 } |
    Select-Object -First 1
if ($null -eq $gameProcess -or -not $gameProcess.CloseMainWindow()) {
    Stop-ProcessTree $client
    throw 'Could not close the Minecraft test client cleanly.'
}
if (-not $client.WaitForExit(10000)) {
    Stop-ProcessTree $client
}

$latestLog = Join-Path $projectDir 'run\logs\latest.log'
$logText = Get-Content -LiteralPath $latestLog -Raw
if ($logText -match 'IllegalClassLoadError|Mixin transformation .* failed|Description: Ticking entity|This crash report has been saved') {
    throw 'A minecart/Mixin crash was found in the singleplayer log.'
}
if ($logText -notmatch 'SmoothQuartz server initialized' -or $logText -notmatch 'Handshake ACK received from server') {
    throw 'Singleplayer started, but the Fabric handshake was not completed.'
}

Write-Host "Singleplayer smoke test passed for Minecraft $Version."
