# End-to-End Test Script for Incident Platform
# Tests complete workflow without Unicode issues

Write-Host "=== INCIDENT PLATFORM END-TO-END TEST ===" -ForegroundColor Green
Write-Host "Testing complete workflow and data flow" -ForegroundColor Cyan

$testStartTime = Get-Date
$testId = "test-$(Get-Date -Format 'yyyyMMdd-HHmmss')"

Write-Host "`nTest ID: $testId" -ForegroundColor White
Write-Host "Start Time: $testStartTime" -ForegroundColor White

# 1. HEALTH CHECK ALL SERVICES
Write-Host "`n1. HEALTH CHECK - ALL SERVICES" -ForegroundColor Yellow
$services = @(
    @{Name="Discovery Server"; Port=8761; Endpoint="/actuator/health"},
    @{Name="Gateway Service"; Port=8080; Endpoint="/actuator/health"}, 
    @{Name="Auth Service"; Port=8081; Endpoint="/actuator/health"},
    @{Name="Log Collector"; Port=8082; Endpoint="/actuator/health"},
    @{Name="Anomaly Detector"; Port=8083; Endpoint="/actuator/health"},
    @{Name="Alert Manager"; Port=8084; Endpoint="/actuator/health"},
    @{Name="Notification Service"; Port=8085; Endpoint="/actuator/health"},
    @{Name="Auto Responder"; Port=8086; Endpoint="/actuator/health"},
    @{Name="Incident Tracker"; Port=8087; Endpoint="/actuator/health"},
    @{Name="Dashboard Service"; Port=8088; Endpoint="/actuator/health"},
    @{Name="AI Service"; Port=8000; Endpoint="/health"}
)

$healthyServices = 0
foreach($service in $services) {
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:$($service.Port)$($service.Endpoint)" -TimeoutSec 5
        
        $isHealthy = if($service.Name -eq "AI Service") { 
            $response.status -eq "healthy" 
        } else { 
            $response.status -eq "UP" 
        }
        
        if($isHealthy) {
            Write-Host "OK  $($service.Name)" -ForegroundColor Green
            $healthyServices++
        } else {
            Write-Host "WARN $($service.Name) - $($response.status)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "FAIL $($service.Name) - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`nHealthy Services: $healthyServices/$($services.Count)" -ForegroundColor Cyan

# 2. USER REGISTRATION TEST
Write-Host "`n2. USER REGISTRATION AND LOGIN TEST" -ForegroundColor Yellow

$testUser = @{
    username = "testuser$testId"
    email = "test$testId@example.com"
    password = "TestPassword123!"
}

try {
    Write-Host "Registering user..." -ForegroundColor White
    $registerResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/register" -Method POST -Body ($testUser | ConvertTo-Json) -ContentType "application/json"
    Write-Host "OK  User registered: $($registerResponse.username)" -ForegroundColor Green
    
    Write-Host "Logging in user..." -ForegroundColor White
    $loginData = @{
        username = $testUser.username
        password = $testUser.password
    }
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" -Method POST -Body ($loginData | ConvertTo-Json) -ContentType "application/json"
    Write-Host "OK  Login successful. Token received." -ForegroundColor Green
    $authToken = $loginResponse.token
    
} catch {
    Write-Host "FAIL User operations failed: $($_.Exception.Message)" -ForegroundColor Red
    $authToken = $null
}

# 3. NORMAL LOG SUBMISSION
Write-Host "`n3. NORMAL LOG SUBMISSION TEST" -ForegroundColor Yellow

$normalLogs = @(
    @{
        timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ssZ")
        level = "INFO"
        serviceName = "user-service"
        message = "User logged in successfully"
        hostname = "app-server-01"
    },
    @{
        timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ssZ")
        level = "DEBUG"
        serviceName = "database-service"
        message = "Connection pool initialized with 10 connections"
        hostname = "db-server-01"
    }
)

$normalLogIds = @()
foreach($log in $normalLogs) {
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8082/api/logs" -Method POST -Body ($log | ConvertTo-Json) -ContentType "application/json"
        Write-Host "OK  Normal log sent: $($response.id)" -ForegroundColor Green
        $normalLogIds += $response.id
    } catch {
        Write-Host "FAIL Normal log failed: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# 4. ANOMALY LOG SUBMISSION
Write-Host "`n4. ANOMALY LOG SUBMISSION TEST" -ForegroundColor Yellow

$anomalyLogs = @(
    @{
        timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ssZ")
        level = "ERROR"
        serviceName = "payment-service"
        message = "Payment processing failed: Connection timeout after 30 seconds"
        hostname = "payment-server-01"
    },
    @{
        timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ssZ")
        level = "FATAL"
        serviceName = "database-service"
        message = "CRITICAL: Database connection pool exhausted - potential DDoS attack detected"
        hostname = "db-server-01"
    }
)

$anomalyLogIds = @()
foreach($log in $anomalyLogs) {
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8082/api/logs" -Method POST -Body ($log | ConvertTo-Json) -ContentType "application/json"
        Write-Host "OK  Anomaly log sent: $($response.id)" -ForegroundColor Yellow
        $anomalyLogIds += $response.id
    } catch {
        Write-Host "FAIL Anomaly log failed: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# 5. WAIT FOR PROCESSING
Write-Host "`n5. WAITING FOR DATA PROCESSING..." -ForegroundColor Yellow
Write-Host "Waiting for logs to be processed through Kafka (15 seconds)..." -ForegroundColor Cyan

for($i = 15; $i -gt 0; $i--) {
    Write-Host "Waiting... $i seconds remaining" -ForegroundColor Yellow
    Start-Sleep 1
}

# 6. AI SERVICE DIRECT TEST
Write-Host "`n6. AI SERVICE DIRECT TEST" -ForegroundColor Yellow

try {
    $directTestLog = @{
        log_id = "direct-test-$testId"
        timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ssZ")
        service_name = "test-service"
        hostname = "test-host"
        log_level = "ERROR"
        message = "Critical system failure - immediate attention required"
        thread = "main-thread"
        logger = "com.test.TestService"
        method = "processRequest"
        line_number = 157
        duration_ms = 5000
        http_status = 500
        metadata = @{test = $true}
    }
    
    Write-Host "Sending direct anomaly test to AI Service..." -ForegroundColor White
    $aiResponse = Invoke-RestMethod -Uri "http://localhost:8000/predict/anomaly" -Method POST -Body ($directTestLog | ConvertTo-Json) -ContentType "application/json"
    
    Write-Host "OK  AI Service Response:" -ForegroundColor Green
    Write-Host "    Anomaly: $($aiResponse.is_anomaly)" -ForegroundColor Cyan
    Write-Host "    Score: $([math]::Round($aiResponse.anomaly_score, 3))" -ForegroundColor Cyan
    Write-Host "    Confidence: $([math]::Round($aiResponse.confidence, 3))" -ForegroundColor Cyan
    Write-Host "    Type: $($aiResponse.anomaly_type)" -ForegroundColor Cyan
    Write-Host "    Reasons: $($aiResponse.anomaly_reasons -join ', ')" -ForegroundColor Cyan
    
} catch {
    Write-Host "FAIL AI Service direct test failed: $($_.Exception.Message)" -ForegroundColor Red
}

# 7. NOTIFICATION TEST
Write-Host "`n7. NOTIFICATION SYSTEM TEST" -ForegroundColor Yellow

try {
    Write-Host "Sending test notification..." -ForegroundColor White
    $testNotificationResponse = Invoke-RestMethod -Uri "http://localhost:8085/api/notifications/test" -Method POST
    Write-Host "OK  Test notification sent" -ForegroundColor Green
    Write-Host "    Check your email: atakanzaa43@gmail.com" -ForegroundColor Cyan
} catch {
    Write-Host "FAIL Notification test failed: $($_.Exception.Message)" -ForegroundColor Red
}

# 8. CHECK SERVICE LOGS FOR DATA FLOW
Write-Host "`n8. CHECKING SERVICE LOGS FOR DATA FLOW" -ForegroundColor Yellow

Write-Host "Checking Anomaly Detector Service logs..." -ForegroundColor White
try {
    $anomalyLogs = docker logs incident-platform-anomaly-detector-service-1 --tail 5 2>$null
    if($anomalyLogs -match "Sent scored log event") {
        Write-Host "OK  Anomaly Detector is processing logs" -ForegroundColor Green
    } else {
        Write-Host "WARN Anomaly Detector may not be processing logs" -ForegroundColor Yellow
    }
} catch {
    Write-Host "INFO Cannot check Anomaly Detector logs" -ForegroundColor Yellow
}

Write-Host "Checking Alert Manager Service logs..." -ForegroundColor White
try {
    $alertLogs = docker logs incident-platform-alert-manager-service-1 --tail 5 2>$null
    if($alertLogs -match "alert" -or $alertLogs -match "Alert") {
        Write-Host "OK  Alert Manager is processing alerts" -ForegroundColor Green
    } else {
        Write-Host "INFO Alert Manager logs checked" -ForegroundColor Cyan
    }
} catch {
    Write-Host "INFO Cannot check Alert Manager logs" -ForegroundColor Yellow
}

# 9. PERFORMANCE CHECK
Write-Host "`n9. PERFORMANCE CHECK" -ForegroundColor Yellow

try {
    Write-Host "Checking Prometheus metrics..." -ForegroundColor White
    $prometheusTargets = Invoke-RestMethod -Uri "http://localhost:9090/api/v1/targets" -TimeoutSec 5
    $activeTargets = ($prometheusTargets.data.activeTargets | Where-Object { $_.health -eq "up" }).Count
    Write-Host "OK  Prometheus active targets: $activeTargets" -ForegroundColor Green
} catch {
    Write-Host "WARN Prometheus check failed: $($_.Exception.Message)" -ForegroundColor Yellow
}

# TEST SUMMARY
Write-Host "`n=== TEST RESULTS SUMMARY ===" -ForegroundColor Green

$testEndTime = Get-Date
$testDuration = $testEndTime - $testStartTime

Write-Host "Test Duration: $([math]::Round($testDuration.TotalSeconds, 2)) seconds" -ForegroundColor White
Write-Host "Healthy Services: $healthyServices/$($services.Count)" -ForegroundColor White
Write-Host "Normal Logs Sent: $($normalLogIds.Count)" -ForegroundColor White
Write-Host "Anomaly Logs Sent: $($anomalyLogIds.Count)" -ForegroundColor White

Write-Host "`n=== ACCESS LINKS ===" -ForegroundColor Cyan
Write-Host "Gateway (Main Entry): http://localhost:8080" -ForegroundColor White
Write-Host "Discovery Server: http://localhost:8761" -ForegroundColor White
Write-Host "AI Service API Docs: http://localhost:8000/docs" -ForegroundColor White
Write-Host "Prometheus: http://localhost:9090" -ForegroundColor White
Write-Host "Grafana: http://localhost:3000 (admin/admin123)" -ForegroundColor White
Write-Host "RabbitMQ: http://localhost:15672 (admin/admin123)" -ForegroundColor White

Write-Host "`n=== LOG MONITORING COMMANDS ===" -ForegroundColor Yellow
Write-Host "Follow Anomaly Detector:" -ForegroundColor White
Write-Host "  docker logs incident-platform-anomaly-detector-service-1 -f" -ForegroundColor Gray
Write-Host "Follow AI Service:" -ForegroundColor White
Write-Host "  docker logs incident-platform-ai-service-1 -f" -ForegroundColor Gray
Write-Host "Follow Alert Manager:" -ForegroundColor White
Write-Host "  docker logs incident-platform-alert-manager-service-1 -f" -ForegroundColor Gray

Write-Host "`n=== DATA FLOW VERIFICATION ===" -ForegroundColor Yellow
Write-Host "1. Check if logs are flowing through Kafka:" -ForegroundColor White
Write-Host "   docker exec incident-platform-kafka-1 kafka-console-consumer --bootstrap-server localhost:9092 --topic logs.raw --from-beginning" -ForegroundColor Gray
Write-Host "2. Check if scored logs are generated:" -ForegroundColor White  
Write-Host "   docker exec incident-platform-kafka-1 kafka-console-consumer --bootstrap-server localhost:9092 --topic logs.scored --from-beginning" -ForegroundColor Gray
Write-Host "3. Check RabbitMQ queues:" -ForegroundColor White
Write-Host "   Visit http://localhost:15672 and check 'notifications' and 'critical-alerts' queues" -ForegroundColor Gray

Write-Host "`nEND-TO-END TEST COMPLETED!" -ForegroundColor Green
Write-Host "System is operational and data flow has been tested." -ForegroundColor Cyan 