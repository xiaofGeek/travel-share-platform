param(
    [switch]$EmitBatch
)

$ErrorActionPreference = 'SilentlyContinue'

function Find-JavaHome {
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($javaCommand) {
        return (Split-Path (Split-Path $javaCommand.Source -Parent) -Parent)
    }
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        return $env:JAVA_HOME
    }
    $candidates = New-Object System.Collections.Generic.List[string]
    $candidates.Add((Join-Path $env:USERPROFILE '.jdks'))
    $candidates.Add((Join-Path $env:ProgramFiles 'Java'))
    $candidates.Add((Join-Path $env:ProgramFiles 'Eclipse Adoptium'))
    foreach ($drive in Get-PSDrive -PSProvider FileSystem) {
        Get-ChildItem -LiteralPath $drive.Root -Directory -Filter 'IntelliJ IDEA*' -ErrorAction SilentlyContinue |
            ForEach-Object { $candidates.Add((Join-Path $_.FullName 'jbr')) }
    }
    foreach ($candidate in ($candidates | Sort-Object -Descending)) {
        if (Test-Path (Join-Path $candidate 'bin\java.exe')) {
            return $candidate
        }
        $child = Get-ChildItem -LiteralPath $candidate -Directory -ErrorAction SilentlyContinue |
            Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') } |
            Sort-Object Name -Descending |
            Select-Object -First 1
        if ($child) { return $child.FullName }
    }
    return $null
}

function Find-Maven {
    $mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($mavenCommand) { return $mavenCommand.Source }
    foreach ($name in @('MAVEN_HOME', 'M2_HOME')) {
        $home = [Environment]::GetEnvironmentVariable($name)
        if ($home -and (Test-Path (Join-Path $home 'bin\mvn.cmd'))) {
            return (Join-Path $home 'bin\mvn.cmd')
        }
    }
    foreach ($drive in Get-PSDrive -PSProvider FileSystem) {
        $ideaHomes = Get-ChildItem -LiteralPath $drive.Root -Directory -Filter 'IntelliJ IDEA*' -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending
        foreach ($idea in $ideaHomes) {
            $candidate = Join-Path $idea.FullName 'plugins\maven\lib\maven3\bin\mvn.cmd'
            if (Test-Path $candidate) { return $candidate }
        }
    }
    return $null
}

function Find-MySQL {
    $mysqlCommand = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($mysqlCommand) { return $mysqlCommand.Source }

    $mysqlService = Get-CimInstance Win32_Service -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '^MySQL|MariaDB' -or $_.DisplayName -match 'MySQL|MariaDB' } |
        Select-Object -First 1
    if ($mysqlService -and $mysqlService.PathName -match '^"?([^"]+\\mysqld\.exe)"?') {
        $serviceClient = Join-Path (Split-Path $Matches[1] -Parent) 'mysql.exe'
        if (Test-Path $serviceClient) { return $serviceClient }
    }

    $roots = @(
        (Join-Path $env:ProgramFiles 'MySQL'),
        (Join-Path ${env:ProgramFiles(x86)} 'MySQL'),
        (Join-Path $env:ProgramFiles 'MariaDB')
    ) | Where-Object { $_ -and (Test-Path $_) }
    $installedClient = Get-ChildItem -Path $roots -Filter mysql.exe -File -Recurse -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending |
        Select-Object -First 1
    if ($installedClient) { return $installedClient.FullName }
    return $null
}

$javaHome = Find-JavaHome
$maven = Find-Maven
$node = (Get-Command node.exe -ErrorAction SilentlyContinue).Source
$npm = (Get-Command npm.cmd -ErrorAction SilentlyContinue).Source
$mysql = Find-MySQL

if ($EmitBatch) {
    if ($javaHome) {
        Write-Output ('set "JAVA_HOME=' + $javaHome + '"')
        Write-Output ('set "JAVA_EXE=' + (Join-Path $javaHome 'bin\java.exe') + '"')
    }
    if ($maven) { Write-Output ('set "MVN_CMD=' + $maven + '"') }
    if ($node) { Write-Output ('set "NODE_EXE=' + $node + '"') }
    if ($npm) { Write-Output ('set "NPM_CMD=' + $npm + '"') }
    if ($mysql) { Write-Output ('set "MYSQL_EXE=' + $mysql + '"') }
    exit 0
}

[pscustomobject]@{
    JavaHome = $javaHome
    Maven = $maven
    Node = $node
    Npm = $npm
    MySQL = $mysql
} | Format-List
