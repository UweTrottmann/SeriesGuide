# https://crowdin.github.io/crowdin-cli/
crowdin.bat pull

$downloadDir = ".\temp\"
$destinationDir = ".\app\src\main\res"

Write-Host "Dropping region specifiers for all but [zh], [pt]..."
Get-ChildItem -Path $downloadDir | Where-Object {$_.PsIsContainer -and $_.Name -notlike "*zh*" -and $_.Name -notlike "*pt*" } |  Rename-Item  -NewName { $_.Name -creplace "-r[A-Z]+", "" }

Write-Host "Copying files..."
Get-ChildItem -Path $downloadDir | Where-Object {$_.PsIsContainer -and $_.FullName -like "*values-*"} | Copy-Item -Destination $destinationDir -Force -Recurse

Write-Host "Removing folder..."
Remove-Item -Path $downloadDir -Recurse

Write-Host "DONE"
