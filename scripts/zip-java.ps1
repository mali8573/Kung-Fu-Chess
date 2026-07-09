Param(
    [string]$SourceDir = "..",
    [string]$OutDir = "..\dist",
    [string]$OutName = "java-sources-$(Get-Date -Format yyyyMMdd-HHmmss).zip"
)

try {
    $sourcePath = Resolve-Path -Path $SourceDir
} catch {
    Write-Error "Source directory '$SourceDir' not found."
    exit 1
}

if (-not (Test-Path -Path $OutDir)) {
    New-Item -ItemType Directory -Path $OutDir | Out-Null
}
$outPath = Resolve-Path -Path $OutDir

$files = Get-ChildItem -Path $sourcePath -Recurse -Include *.java -File | ForEach-Object { $_.FullName }
if (-not $files -or $files.Count -eq 0) {
    Write-Host "No .java files found under $($sourcePath.Path)"
    exit 1
}

$zipPath = Join-Path $outPath $OutName

Compress-Archive -Path $files -DestinationPath $zipPath -Force

Write-Host "Created ZIP: $zipPath"
