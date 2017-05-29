param (
    [Parameter(Mandatory=$true)][string]$logFileIn,
    [Parameter(Mandatory=$true)][string]$logFileOut
)

function FirstCharOrEmpty
{
    if ($args[0]) { $args[0].Substring(0, 1) } else { "_" }
}

function GetConfig
{
    (FirstCharOrEmpty $args[0].Slicer) + `
    (FirstCharOrEmpty $args[0].Optimizations) + `
    (FirstCharOrEmpty $args[0].Domain) + `
    (FirstCharOrEmpty $args[0].Refinement) + `
    (FirstCharOrEmpty $args[0].Search) + `
    (FirstCharOrEmpty $args[0].PrecGranularity)
}

$data = @(Import-Csv $logFileIn | Select-Object `
    @{Name="Model"; Expression={$_.Model.Replace("../models/", "") + "#" + $_.SliceNo}}, `
    @{Name="Config"; Expression={GetConfig $_}}, `
    Safe, `
    TimeMs)

$data | Export-Csv $logFileOut -NoTypeInformation
