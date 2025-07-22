# Incident Platform Test Script
Write-Host "=== INCIDENT PLATFORM TEST BAŞLIYOR ===" -ForegroundColor Green

# 1. Health Checks
Write-Host "`n1. HEALTH CHECKS" -ForegroundColor Yellow
$services = @(
    @{Name="Discovery Server"; Port=8761},
    @{Name="Gateway Service"; Port=8080}, 
    @{Name="Auth Service"; Port=8081},
    @{Name="Log Collector"; Port=8082},
    @{Name="Anomaly Detector"; Port=8083},
    @{Name="Alert Manager"; Port=8084},
    @{Name="Notification Service"; Port=8085},
    @{Name="Auto Responder"; Port=8086},
    @{Name="AI Service"; Port=8000}
)

foreach($service in $services) {
    try {
        # AI Service uses /health instead of /actuator/health
        $healthEndpoint = if($service.Name -eq "AI Service") { "/health" } else { "/actuator/health" }
        $response = Invoke-RestMethod -Uri "http://localhost:$($service.Port)$healthEndpoint" -TimeoutSec 5
        
        # Check different response formats
        $isHealthy = if($service.Name -eq "AI Service") { 
            $response.status -eq "healthy" 
        } else { 
            $response.status -eq "UP" 
        }
        
        if($isHealthy) {
            Write-Host "✅ $($service.Name) - HEALTHY" -ForegroundColor Green
        } else {
            Write-Host "⚠️  $($service.Name) - $($response.status)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "❌ $($service.Name) - ERROR: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# 2. Log Test
Write-Host "`n2. LOG COLLECTION TEST" -ForegroundColor Yellow
try {
    $logData = @{
        timestamp = "2025-07-22T15:52:00Z"
        level = "ERROR"
        serviceName = "test-service"
        message = "Test error message for anomaly detection"
        hostname = "automated-test"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "http://localhost:8082/api/logs" -Method POST -Body $logData -ContentType "application/json"
    Write-Host "✅ Log submitted successfully" -ForegroundColor Green
    Write-Host "Response: $($response | ConvertTo-Json)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Log submission failed: $($_.Exception.Message)" -ForegroundColor Red
}

# 3. Critical Log Test (for anomaly detection)
Write-Host "`n3. ANOMALY DETECTION TEST" -ForegroundColor Yellow
try {
    $criticalLog = @{
        timestamp = "2025-07-22T15:53:00Z"
        level = "FATAL"
        serviceName = "database-service"
        message = "FATAL: Database connection timeout - potential DDoS attack detected"
        hostname = "security-monitor"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "http://localhost:8082/api/logs" -Method POST -Body $criticalLog -ContentType "application/json"
    Write-Host "✅ Critical log submitted for anomaly detection" -ForegroundColor Green
    Write-Host "Response: $($response | ConvertTo-Json)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Critical log submission failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== TEST TAMAMLANDI ===" -ForegroundColor Green
Write-Host "Mail bildirimlerini kontrol edin: atakanzaa43@gmail.com" -ForegroundColor Cyan 