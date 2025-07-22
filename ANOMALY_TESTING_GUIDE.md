# Anomaly Detection Testing Guide

Bu kılavuz Incident Platform'daki anomaly detection sistemini nasıl test edeceğinizi gösterir.

## 🎯 Anomaly Detection Nasıl Çalışıyor?

### 1. AI-Powered Detection
```python
# ai-service/services/anomaly_detector.py
# Gerçek ML algorithms:
- Isolation Forest (Tree-based)
- One-Class SVM (Support Vector)  
- Local Outlier Factor (Density-based)
```

### 2. Scoring System
```yaml
Anomaly Score: 0.0 - 1.0
- 0.0-0.3: Normal
- 0.3-0.7: Warning  
- 0.7-0.9: High
- 0.9-1.0: Critical
```

## 🧪 Test Scenarios

### Scenario 1: Error Spike Detection

**1. Start All Services:**
```powershell
.\scripts\start-all-services.ps1
```

**2. Send Normal Log:**
```bash
curl -X POST http://localhost:8080/api/logs/events \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "payment-service",
    "level": "INFO",
    "message": "Payment processed successfully for order #12345",
    "hostname": "payment-pod-1",
    "metadata": {
      "orderId": "12345",
      "amount": 99.99,
      "duration_ms": 150
    }
  }'
```

**Expected Result:** Anomaly Score ~0.1 (Normal)

**3. Send Error Log:**
```bash
curl -X POST http://localhost:8080/api/logs/events \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "payment-service", 
    "level": "ERROR",
    "message": "Database connection timeout - payment failed",
    "hostname": "payment-pod-1",
    "stackTrace": "java.sql.SQLException: Connection timeout\n    at com.payment.DatabaseService.connect(DatabaseService.java:45)",
    "metadata": {
      "orderId": "12346", 
      "duration_ms": 5000,
      "retryCount": 3
    }
  }'
```

**Expected Result:** Anomaly Score ~0.8 (High)

### Scenario 2: Performance Degradation

**Send Slow Response Log:**
```bash
curl -X POST http://localhost:8080/api/logs/events \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "order-service",
    "level": "WARN", 
    "message": "Slow database query detected",
    "hostname": "order-pod-2",
    "metadata": {
      "queryTime": 8000,
      "operation": "findOrdersByUser",
      "recordCount": 50000
    }
  }'
```

**Expected Result:** Anomaly Score ~0.6 (Warning)

### Scenario 3: Security Threat

**Send Security Alert:**
```bash
curl -X POST http://localhost:8080/api/logs/events \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "auth-service",
    "level": "ERROR",
    "message": "Multiple failed login attempts detected - potential brute force attack",
    "hostname": "auth-pod-1", 
    "metadata": {
      "username": "admin",
      "failedAttempts": 15,
      "sourceIP": "192.168.1.100",
      "timeWindow": "5 minutes"
    }
  }'
```

**Expected Result:** Anomaly Score ~0.9 (Critical)

## 📊 Monitoring & Verification

### 1. Real-time Dashboard
```
http://localhost:8088/dashboard
- WebSocket updates
- Alert timeline
- Service health
```

### 2. Kafka Topics Monitoring
```bash
# Kafka UI (if available)
http://localhost:9092

# Or use kafka-console-consumer
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic logs.scored \
  --from-beginning
```

### 3. RabbitMQ Management
```
http://localhost:15672
Username: admin
Password: admin123

Queues to monitor:
- notifications
- auto-actions  
- critical-alerts
```

### 4. AI Service Metrics
```bash
# Direct AI service test
curl -X POST http://localhost:8000/api/anomaly/detect \
  -H "Content-Type: application/json" \
  -d '{
    "log_id": "test-123",
    "service": "test-service",
    "level": "ERROR",
    "message": "Critical system failure",
    "timestamp": "2024-01-15T10:30:00"
  }'
```

## 🔍 Deep Dive Testing

### Test AI Model Performance

**1. Batch Testing:**
```bash
# Send 100 mixed logs
for i in {1..100}; do
  # 80% normal, 20% anomalous
  if (( i % 5 == 0 )); then
    # Send anomalous log
    curl -X POST http://localhost:8080/api/logs/events -H "Content-Type: application/json" -d "{\"serviceName\":\"test-service\",\"level\":\"ERROR\",\"message\":\"System failure $i\"}"
  else  
    # Send normal log
    curl -X POST http://localhost:8080/api/logs/events -H "Content-Type: application/json" -d "{\"serviceName\":\"test-service\",\"level\":\"INFO\",\"message\":\"Normal operation $i\"}"
  fi
done
```

**2. Feature Analysis:**
```bash
# Test different features
curl -X POST http://localhost:8080/api/logs/events -H "Content-Type: application/json" -d '{
  "serviceName": "feature-test",
  "level": "ERROR",
  "message": "OutOfMemoryError: Java heap space at com.example.Service.process(Service.java:42)",
  "hostname": "pod-123",
  "metadata": {
    "memory_usage": "95%",
    "cpu_usage": "89%", 
    "active_threads": 150,
    "duration_ms": 12000
  }
}'
```

## 🎭 Environment Testing

### Development Environment
```powershell
# Start with Docker Compose
.\scripts\start-all-services.ps1

# Test Features:
- All services running locally
- AI models in memory
- DRY_RUN: true (safe)
- Real-time dashboard
- Local databases
```

### Staging Environment  
```bash
# Deploy to Kubernetes
kubectl apply -f argocd/applications/incident-platform-staging.yaml

# Test Features:
- Real K8s deployment
- Persistent storage  
- External domains
- DRY_RUN: true (still safe)
- Production-like environment
```

### Production Simulation
```bash
# Set environment variables
export AUTO_RESPONSE_DRY_RUN=false
export K8S_ENABLED=true

# Test real responses:
- Actual pod restarts
- Real scaling actions
- Production alerts
```

## 📈 Expected Behaviors

### Normal Logs (Score: 0.0-0.3)
- INFO/DEBUG levels
- Fast response times (<500ms)
- No exceptions
- Expected patterns

### Warning Logs (Score: 0.3-0.7)  
- WARN level
- Slow responses (1-5s)
- Minor issues
- Recoverable errors

### Critical Logs (Score: 0.7-1.0)
- ERROR/FATAL levels
- System failures
- Security issues
- Performance degradation

## 🚨 Alert Flow Testing

**Complete End-to-End Flow:**
```
1. Send Critical Log → Log Collector
2. Kafka Topic: logs.raw → Anomaly Detector  
3. AI Analysis → Score: 0.9
4. Kafka Topic: logs.scored → Alert Manager
5. RabbitMQ: notifications → Email/Slack
6. RabbitMQ: auto-actions → Pod Restart
7. Kafka: alerts.critical → Dashboard
8. WebSocket → Real-time Updates
9. MongoDB → Incident Storage
```

## 🔧 Troubleshooting

### AI Service Not Responding
```bash
# Check AI service logs
docker logs ai-service

# Test direct connection
curl http://localhost:8000/health
```

### Kafka Connection Issues
```bash
# Check topic creation
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

### RabbitMQ Queue Problems  
```bash
# Check queue status
http://localhost:15672/#/queues
```

Bu kılavuzla anomaly detection sistemini tam olarak anlayıp test edebilirsiniz! 