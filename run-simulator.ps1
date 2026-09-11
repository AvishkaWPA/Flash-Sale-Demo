param (
    [int]$users = 1000,
    [int]$productId = 8
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  Starting FlashSale Load Simulator" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Concurrent Users: $users" -ForegroundColor Yellow
Write-Host "Target Product ID: $productId" -ForegroundColor Yellow
Write-Host ""

Set-Location "$PSScriptRoot\simulator"
mvn compile exec:java "-Dexec.mainClass=com.avishka.simulator.LoadTestSimulator" "-Dexec.args=$users $productId"
