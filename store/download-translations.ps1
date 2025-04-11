$downloadDir = ".\temp\"
$destinationDir = "."

Write-Host "Clean up download folder ($downloadDir)..."
Remove-Item -Path $downloadDir -Recurse -ErrorAction SilentlyContinue

# https://crowdin.github.io/crowdin-cli/
crowdin.bat pull

$directories = Get-ChildItem -Path $downloadDir -Directory
foreach ($dir in $directories) {
    if ($dir.Name -like "*sr-rCS") {
        Write-Host "Rename Latin Serbian to values-b+sr+Latn"
        $newName = $dir.Name -replace "sr-rCS$", "b+sr+Latn"
        Rename-Item -NewName $newName $dir
    } elseif ($dir.Name -notlike "*pt*" -and $dir.Name -notlike "*zh*") {
        # Note: keeping region for pt and zh
        Write-Host "Dropping region for $($dir.Name)"
        $newName = $dir.Name -creplace "-r[A-Z]+", ""
        Rename-Item -NewName $newName $dir
    } else {
        Write-Host "Keeping $($dir.Name)"
    }
}

Write-Host "Copying to destination ($destinationDir)..."
Get-ChildItem -Path $downloadDir -Directory | Copy-Item -Destination $destinationDir -Force -Recurse

Write-Host "Clean up download folder ($downloadDir)..."
Remove-Item -Path $downloadDir -Recurse

Write-Host "DONE"
