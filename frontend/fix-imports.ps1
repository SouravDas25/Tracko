Get-ChildItem -Path .\lib -Recurse -Include *.dart | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $newContent = $content -replace 'package:Tracko/', 'package:tracko/'
    Set-Content -Path $_.FullName -Value $newContent -NoNewline
}
Write-Host "Import statements updated successfully!"
