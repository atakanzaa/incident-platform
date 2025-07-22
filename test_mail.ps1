# Direct Mail Test Script
Write-Host "=== MAİL TEST BAŞLIYOR ===" -ForegroundColor Green

# Test notification endpoint
try {
    $testNotification = @{
        type = "email"
        subject = "Test Anomaly Alert"
        message = "Bu bir test anomaly alert'idir. Sistem çalışıyor."
        recipient = "atakanzaa43@gmail.com"
        priority = "HIGH"
    } | ConvertTo-Json

    Write-Host "Notification service'e test notification gönderiliyor..." -ForegroundColor Yellow
    $response = Invoke-RestMethod -Uri "http://localhost:8085/api/notifications/test" -Method POST
    Write-Host "✅ Mail gönderildi!" -ForegroundColor Green
    Write-Host "Response: $($response | ConvertTo-Json)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Mail gönderimi başarısız: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Response Body: $($_.ErrorDetails.Message)" -ForegroundColor Red
}

Write-Host "`n=== MAİL TEST BİTTİ ===" -ForegroundColor Green 