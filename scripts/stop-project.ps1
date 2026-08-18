param([Parameter(Mandatory=$true)][string]$ProjectRoot)
$resolvedRoot = [System.IO.Path]::GetFullPath($ProjectRoot).TrimEnd('\')
foreach ($port in @(8080,5173,5174)) {
    $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($connection in $connections) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($connection.OwningProcess)" -ErrorAction SilentlyContinue
        $isProjectNode = $process -and $process.CommandLine -and $process.CommandLine.IndexOf($resolvedRoot, [StringComparison]::OrdinalIgnoreCase) -ge 0
        $isProjectBackend = $process -and $process.CommandLine -and $process.CommandLine.IndexOf('com.travelshare.platform.TravelShareApplication', [StringComparison]::OrdinalIgnoreCase) -ge 0
        if ($isProjectNode -or $isProjectBackend) {
            Stop-Process -Id $process.ProcessId -ErrorAction SilentlyContinue
            Write-Output "Stopped project process $($process.ProcessId) on port $port."
        } else {
            Write-Output "Port $port is owned by an unrelated process and was not changed."
        }
    }
}
