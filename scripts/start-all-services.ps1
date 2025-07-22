param(
    [switch]$Cleanup
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$ComposeFile = Join-Path $ProjectRoot "docker-compose.local.yml"

Set-Location $ProjectRoot

Write-Host "Starting Incident Platform Services..." -ForegroundColor Green
Write-Host "Project Root: $ProjectRoot" -ForegroundColor Cyan

function Cleanup-Containers {
    Write-Host "Cleaning up existing containers..." -ForegroundColor Yellow
    docker-compose -f $ComposeFile down --remove-orphans
    docker system prune -f
}

function Wait-ForService {
    param(
        [string]$ServiceName,
        [int]$Port,
        [int]$MaxAttempts = 60
    )
    
    Write-Host "Waiting for $ServiceName on port $Port..." -ForegroundColor Yellow
    
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        try {
            $response = Invoke-RestMethod -Uri "http://localhost:$Port/actuator/health" -TimeoutSec 2 -ErrorAction SilentlyContinue
            if ($response) {
                Write-Host "$ServiceName is ready" -ForegroundColor Green
                return $true
            }
        }
        catch {
            # Service not ready yet
        }
        
        if ($attempt -eq $MaxAttempts) {
            Write-Host "Timeout waiting for $ServiceName" -ForegroundColor Red
            return $false
        }
        
        Write-Host "Attempt $attempt/$MaxAttempts - $ServiceName not ready, waiting 5 seconds..." -ForegroundColor Gray
        Start-Sleep 5
    }
}

function Wait-ForPort {
    param(
        [string]$ServiceName,
        [int]$Port,
        [int]$MaxAttempts = 60
    )
    
    Write-Host "Waiting for $ServiceName on port $Port..." -ForegroundColor Yellow
    
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        try {
            $tcpClient = New-Object System.Net.Sockets.TcpClient
            $tcpClient.ConnectAsync("localhost", $Port).Wait(1000)
            if ($tcpClient.Connected) {
                $tcpClient.Close()
                Write-Host "$ServiceName is ready" -ForegroundColor Green
                return $true
            }
            $tcpClient.Close()
        }
        catch {
            # Port not ready yet
        }
        
        if ($attempt -eq $MaxAttempts) {
            Write-Host "Timeout waiting for $ServiceName" -ForegroundColor Red
            return $false
        }
        
        Write-Host "Attempt $attempt/$MaxAttempts - $ServiceName not ready, waiting 5 seconds..." -ForegroundColor Gray
        Start-Sleep 5
    }
}

if ($Cleanup) {
    Cleanup-Containers
}

Write-Host "`nStep 1: Infrastructure Services" -ForegroundColor Magenta

Write-Host "Starting PostgreSQL..." -ForegroundColor White
docker-compose -f $ComposeFile up -d postgresql
Wait-ForPort "PostgreSQL" 5432

Write-Host "Starting MongoDB..." -ForegroundColor White
docker-compose -f $ComposeFile up -d mongodb
Wait-ForPort "MongoDB" 27017

Write-Host "Starting Zookeeper..." -ForegroundColor White
docker-compose -f $ComposeFile up -d zookeeper
Wait-ForPort "Zookeeper" 2181

Write-Host "Starting Kafka..." -ForegroundColor White
docker-compose -f $ComposeFile up -d kafka
Wait-ForPort "Kafka" 9092

Write-Host "Starting RabbitMQ..." -ForegroundColor White
docker-compose -f $ComposeFile up -d rabbitmq
Wait-ForPort "RabbitMQ" 5672

Write-Host "Starting Redis..." -ForegroundColor White
docker-compose -f $ComposeFile up -d redis
Wait-ForPort "Redis" 6379

Write-Host "`nStep 2: Core Platform Services" -ForegroundColor Magenta

Write-Host "Starting Config Server..." -ForegroundColor White
docker-compose -f $ComposeFile up -d config-server
Wait-ForService "Config Server" 8888

Write-Host "Starting Discovery Server..." -ForegroundColor White
docker-compose -f $ComposeFile up -d discovery-server
Wait-ForService "Discovery Server" 8761

Write-Host "`nStep 3: AI Service" -ForegroundColor Magenta

Write-Host "Starting AI Service..." -ForegroundColor White
docker-compose -f $ComposeFile up -d ai-service
Wait-ForPort "AI Service" 8000

Write-Host "`nStep 4: Gateway Service" -ForegroundColor Magenta

Write-Host "Starting Gateway Service..." -ForegroundColor White
docker-compose -f $ComposeFile up -d gateway-service
Wait-ForService "Gateway Service" 8080

Write-Host "`nStep 5: Authentication Service" -ForegroundColor Magenta

Write-Host "Starting Auth Service..." -ForegroundColor White
docker-compose -f $ComposeFile up -d auth-service
Wait-ForService "Auth Service" 8081

Write-Host "`nStep 6: Data Processing Services" -ForegroundColor Magenta

Write-Host "Starting Log Collector Service..." -ForegroundColor White
docker-compose -f $ComposeFile up -d log-collector-service
Wait-ForService "Log Collector Service" 8082

Write-Host "Starting Anomaly Detector Service..." -ForegroundColor White
docker-compose -f $ComposeFile up -d anomaly-detector-service
Wait-ForService "Anomaly Detector Service" 8083

Write-Host "Starting Alert Manager Service..." -ForegroundColor White
docker-compose -f $ComposeFile up -d alert-manager-service
Wait-ForService "Alert Manager Service" 8084

Write-Host "`nStep 7: Notification Services" -ForegroundColor Magenta

Write-Host "Starting Notification Service..." -ForegroundColor White
docker-compose -f $ComposeFile up -d notification-service
Wait-ForService "Notification Service" 8085

Write-Host "Starting Auto Responder Service..." -ForegroundColor White
docker-compose -f $ComposeFile up -d auto-responder-service
Wait-ForService "Auto Responder Service" 8086

Write-Host "`nStep 8: Tracking and Dashboard Services" -ForegroundColor Magenta

Write-Host "Starting Incident Tracker..." -ForegroundColor White
docker-compose -f $ComposeFile up -d incident-tracker
Wait-ForService "Incident Tracker" 8087

Write-Host "Starting Dashboard Service..." -ForegroundColor White
docker-compose -f $ComposeFile up -d dashboard-service
Wait-ForService "Dashboard Service" 8088

Write-Host "`nStep 9: Monitoring Services" -ForegroundColor Magenta

Write-Host "Starting Prometheus..." -ForegroundColor White
docker-compose -f $ComposeFile up -d prometheus
Wait-ForPort "Prometheus" 9090

Write-Host "Starting Grafana..." -ForegroundColor White
docker-compose -f $ComposeFile up -d grafana
Wait-ForPort "Grafana" 3000

Write-Host "`nAll services started successfully!" -ForegroundColor Green

Write-Host "`nService Status:" -ForegroundColor Cyan
Write-Host "Infrastructure:" -ForegroundColor Yellow
Write-Host "  PostgreSQL: http://localhost:5432" -ForegroundColor White
Write-Host "  MongoDB: http://localhost:27017" -ForegroundColor White
Write-Host "  Kafka: http://localhost:9092" -ForegroundColor White
Write-Host "  RabbitMQ: http://localhost:15672 (admin/admin123)" -ForegroundColor White
Write-Host "  Redis: http://localhost:6379" -ForegroundColor White

Write-Host "`nPlatform Services:" -ForegroundColor Yellow
Write-Host "  Config Server: http://localhost:8888" -ForegroundColor White
Write-Host "  Discovery Server: http://localhost:8761" -ForegroundColor White
Write-Host "  Gateway Service: http://localhost:8080" -ForegroundColor White

Write-Host "`nApplication Services:" -ForegroundColor Yellow
Write-Host "  Auth Service: http://localhost:8081" -ForegroundColor White
Write-Host "  Log Collector: http://localhost:8082" -ForegroundColor White
Write-Host "  Anomaly Detector: http://localhost:8083" -ForegroundColor White
Write-Host "  Alert Manager: http://localhost:8084" -ForegroundColor White
Write-Host "  Notification Service: http://localhost:8085" -ForegroundColor White
Write-Host "  Auto Responder: http://localhost:8086" -ForegroundColor White
Write-Host "  Incident Tracker: http://localhost:8087" -ForegroundColor White
Write-Host "  Dashboard Service: http://localhost:8088" -ForegroundColor White

Write-Host "`nAI and Monitoring:" -ForegroundColor Yellow
Write-Host "  AI Service: http://localhost:8000" -ForegroundColor White
Write-Host "  Prometheus: http://localhost:9090" -ForegroundColor White
Write-Host "  Grafana: http://localhost:3000 (admin/admin123)" -ForegroundColor White

Write-Host "`nAll services are running and ready for use!" -ForegroundColor Green 