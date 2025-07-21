# AI-Driven Incident Detection & Response System

A comprehensive microservices platform for automated incident detection and response using AI-powered log analysis.

## 🏗️ Architecture Overview

This platform implements a complete incident management pipeline:

```
Logs → LogCollector → Kafka → AnomalyDetector (AI) → AlertManager → RabbitMQ
                      ↓            ↓                         ↓
                 Dashboard    IncidentTracker         Notifications
                 (WebSocket)    (MongoDB)            & AutoResponse
```

## 🚀 Services

### Infrastructure Services
- **Config Server** (8888) - Centralized configuration with Git backend
- **Discovery Server** (8761) - Eureka service registry
- **Gateway Service** (8080) - JWT-authenticated API gateway

### Core Platform Services
- **Auth Service** (8081) - JWT authentication with PostgreSQL
- **Log Collector Service** (8082) - Kafka producer for log ingestion
- **Anomaly Detector Service** (8083) - AI-powered anomaly detection
- **Alert Manager Service** (8084) - Smart alerting with duplicate suppression
- **Notification Service** (8085) - Multi-channel notifications (Email/Slack)
- **Auto Responder Service** (8086) - Kubernetes automation
- **Incident Tracker Service** (8087) - MongoDB storage with full lifecycle
- **Dashboard Service** (8088) - Real-time WebSocket dashboard

### AI Service
- **AI Service** (8000) - Python FastAPI with ML models for anomaly detection

## 🛠️ Technology Stack

### Backend Services
- **Java 21** with Spring Boot 3.5.3
- **Spring Cloud 2025.0.0** for microservices
- **Spring Security** with JWT authentication
- **Kafka** for event streaming
- **RabbitMQ** for task queuing
- **PostgreSQL** for relational data
- **MongoDB** for document storage
- **Kubernetes Client** for automation

### AI/ML
- **Python 3.11** with FastAPI
- **Scikit-learn** for ML algorithms
- **Isolation Forest**, **One-Class SVM**, **Local Outlier Factor**
- **TF-IDF** for text feature extraction
- **Ensemble methods** for robust detection

### Monitoring & DevOps
- **Prometheus** metrics
- **Grafana** dashboards
- **Docker** containerization
- **WebSocket** for real-time updates

## 🚀 Quick Start

### Prerequisites
- Docker and Docker Compose
- Java 21 (for development)
- Python 3.11 (for AI service development)
- Maven 3.9+ (for building)
- (Optional) kubectl, helm, minikube/kind (for Kubernetes)

### 🎯 Fastest Start (No Domain Required)

**Using Quick Start Script:**
```bash
chmod +x scripts/quick-start.sh
./scripts/quick-start.sh
```

Script menu options:
- 🐳 **Docker Compose** - Quick start, no domain required
- ☸️ **Kubernetes** - Local cluster with Minikube/Kind
- 🏗️ **CI/CD Setup** - Jenkins + ArgoCD
- 📊 **Infrastructure Only** - Database, Kafka, etc.
- 🧪 **Test Environment** - Automated test execution

### 1. Start with Docker Compose
```bash
# Automatic startup
./scripts/quick-start.sh docker

# Manual startup
docker-compose -f docker-compose.local.yml up -d

# Check service status
docker-compose -f docker-compose.local.yml ps
```

### 2. Start with Kubernetes (Local)
```bash
# Automatic setup
./scripts/quick-start.sh k8s

# Manual setup
chmod +x scripts/local-setup.sh
./scripts/local-setup.sh

# Start port forwarding
./scripts/port-forward.sh
```

### 3. CI/CD Setup (Jenkins + ArgoCD)
```bash
# Install CI/CD tools
./scripts/quick-start.sh cicd

# Manual installation
chmod +x scripts/local-jenkins.sh
./scripts/local-jenkins.sh
```

### 4. Access Information

#### 🌐 Main Services
| Service | URL | Description |
|---------|-----|-------------|
| 🌐 API Gateway | http://localhost:8080 | Main entry point |
| 🔐 Auth Service | http://localhost:8081 | Authentication |
| 📊 Dashboard | http://localhost:8088 | Real-time dashboard |
| 🤖 AI Service | http://localhost:8000 | AI/ML API |
| 🔍 Discovery Server | http://localhost:8761 | Service registry |

#### 🗄️ Infrastructure
| Service | URL/Port | Credentials |
|---------|----------|-------------|
| 🐘 PostgreSQL | localhost:5432 | postgres/123456 |
| 🍃 MongoDB | localhost:27017 | admin/admin123 |
| 📡 Kafka | localhost:9092 | - |
| 🐰 RabbitMQ Management | http://localhost:15672 | admin/admin123 |
| 🔴 Redis | localhost:6379 | - |

#### 📈 Monitoring
| Service | URL | Credentials |
|---------|-----|-------------|
| 📊 Prometheus | http://localhost:9090 | - |
| 📈 Grafana | http://localhost:3000 | admin/admin123 |
| ⚡ Kafka UI | http://localhost:8090 | - |

#### 🏗️ CI/CD (After Kubernetes setup)
| Service | URL | Credentials |
|---------|-----|-------------|
| 🏗️ Jenkins | http://localhost:30808 | Shown by script |
| 🚀 ArgoCD | http://localhost:30443 | admin + Shown by script |

#### 📚 API Documentation
| Service | URL | Description |
|---------|-----|-------------|
| 📖 Gateway API Docs | http://localhost:8080/swagger-ui.html | REST API documentation |
| 🤖 AI Service Docs | http://localhost:8000/docs | FastAPI documentation |
| 📡 WebSocket | ws://localhost:8088/ws | Real-time updates |

### 5. System Testing

#### Authentication
```bash
# Register a user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'

# Login (get JWT token)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

#### Send Log Events
```bash
# Send test log event
curl -X POST http://localhost:8080/api/logs/events \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "serviceName": "test-service",
    "logLevel": "ERROR",
    "message": "Database connection failed",
    "hostname": "test-host",
    "timestamp": "2024-01-01T12:00:00Z"
  }'

# Simulate anomaly
curl -X POST http://localhost:8080/api/logs/events \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "serviceName": "critical-service",
    "logLevel": "CRITICAL",
    "message": "SECURITY BREACH DETECTED - Unauthorized access attempt",
    "hostname": "prod-server",
    "timestamp": "2024-01-01T12:00:00Z"
  }'
```

#### Dashboard Monitoring
```bash
# Get dashboard summary
curl http://localhost:8080/api/dashboard/summary \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Real-time WebSocket connection
# Connect to: ws://localhost:8088/ws
```

### 🔧 Useful Commands

#### Docker Compose Commands
```bash
# System status
./scripts/quick-start.sh status

# Start infrastructure only
./scripts/quick-start.sh infra

# Follow logs
docker-compose -f docker-compose.local.yml logs -f

# Restart specific service
docker-compose -f docker-compose.local.yml restart gateway-service

# Stop services
docker-compose -f docker-compose.local.yml down

# Clean with volumes
docker-compose -f docker-compose.local.yml down -v

# Follow specific service logs
docker-compose -f docker-compose.local.yml logs -f gateway-service auth-service
```

#### Kubernetes Commands (For local cluster)
```bash
# Pod status
kubectl get pods -n incident-platform-local

# Service logs
kubectl logs -f deployment/gateway-service -n incident-platform-local

# Port forward (manual)
kubectl port-forward service/gateway-service 8080:8080 -n incident-platform-local

# Clean namespace
kubectl delete namespace incident-platform-local

# Check ConfigMaps and Secrets
kubectl get configmaps,secrets -n incident-platform-local
```

#### Jenkins & ArgoCD Commands
```bash
# Get Jenkins password
kubectl exec -n jenkins $(kubectl get pods -n jenkins -l app=jenkins -o jsonpath='{.items[0].metadata.name}') -- cat /var/jenkins_home/secrets/initialAdminPassword

# Get ArgoCD password
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d

# ArgoCD CLI (if installed)
argocd login localhost:30443 --username admin --password $(kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d)
```

#### Quick Test and Debug
```bash
# Health check all services
for port in 8080 8081 8082 8083 8084 8085 8086 8087 8088 8000; do
  echo "Testing port $port:"
  curl -f http://localhost:$port/actuator/health || curl -f http://localhost:$port/health
  echo -e "\n"
done

# List Kafka topics (inside Kafka container)
docker-compose -f docker-compose.local.yml exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# MongoDB connection test
docker-compose -f docker-compose.local.yml exec mongodb mongosh --eval "db.adminCommand('ping')"

# PostgreSQL connection test
docker-compose -f docker-compose.local.yml exec postgresql psql -U postgres -c "SELECT version();"
```

#### System Cleanup
```bash
# Complete cleanup
./scripts/quick-start.sh clean

# Docker only cleanup
docker-compose -f docker-compose.local.yml down -v
docker system prune -f

# Kubernetes cleanup
kubectl delete namespace incident-platform-local jenkins argocd --ignore-not-found=true
```

## 🔧 Configuration

### Environment Variables

Create a `.env` file in the root directory:

```env
# SMTP Configuration (for notifications)
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password

# Slack Configuration
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL

# Database Passwords (optional, has defaults)
POSTGRES_PASSWORD=123456
MONGODB_PASSWORD=admin123
RABBITMQ_PASSWORD=admin123
```

### Service Configuration

Each service can be configured via environment variables or `application.yml`:

```yaml
# Example: Alert Manager configuration
alert:
  severity:
    thresholds:
      critical: 0.9
      high: 0.7
      medium: 0.5
  suppression:
    window-minutes: 5
    max-duplicates: 3
```

## 📊 Key Features

### AI-Powered Anomaly Detection
- **Multiple ML Algorithms**: Isolation Forest, One-Class SVM, Local Outlier Factor
- **Feature Engineering**: Text analysis, temporal patterns, categorical encoding
- **Ensemble Methods**: Combines multiple models for robust detection
- **Real-time Processing**: FastAPI with async processing
- **Model Management**: Health monitoring, retraining, versioning

### Smart Alerting
- **Duplicate Suppression**: Prevents alert spam with fingerprinting
- **Severity Classification**: Automatic severity assignment based on ML scores
- **Correlation**: Groups related alerts using correlation IDs
- **Maintenance Windows**: Scheduled suppression during maintenance

### Multi-Channel Notifications
- **Email**: HTML templates with rich formatting
- **Slack**: Rich formatting with action buttons
- **Async Processing**: Non-blocking notification delivery
- **Dead Letter Queues**: Handles failed notifications

### Kubernetes Integration
- **Pod Management**: Restart, scale, health checks
- **Dry-run Mode**: Test actions without execution
- **Action Results**: Track automation outcomes
- **Resource Monitoring**: CPU, memory, disk usage

### Historical Analysis
- **MongoDB Storage**: Scalable document storage
- **Full-text Search**: Search across incident history
- **Time-series Data**: Temporal analysis and trends
- **Data Retention**: Configurable TTL policies

### Real-time Dashboard
- **WebSocket Streaming**: Live updates for alerts and metrics
- **System Health**: Overall platform health scoring
- **Service Status**: Individual service health indicators
- **Alert Trends**: Time-based analysis and visualization

## 🔒 Security

### Authentication & Authorization
- **JWT Tokens**: Stateless authentication
- **Role-based Access**: User roles and permissions
- **Token Refresh**: Automatic token renewal
- **Password Hashing**: Bcrypt with salt

### API Security
- **CORS Configuration**: Cross-origin request handling
- **Rate Limiting**: Prevents API abuse
- **Input Validation**: Request validation with constraints
- **Security Headers**: HTTPS enforcement, CSRF protection

### Infrastructure Security
- **Network Isolation**: Docker network segmentation
- **Secret Management**: Environment-based secrets
- **Database Security**: Connection encryption, authentication
- **Service Communication**: Internal service authentication

## 📈 Monitoring & Observability

### Metrics
- **Prometheus Integration**: Custom metrics for all services
- **Business Metrics**: Alert rates, resolution times, ML accuracy
- **System Metrics**: JVM, database, messaging stats
- **Custom Dashboards**: Grafana visualizations

### Logging
- **Structured Logging**: JSON format with correlation IDs
- **Log Levels**: Configurable per service
- **Centralized Logs**: Aggregated log collection
- **Log Analysis**: Built-in anomaly detection

### Health Checks
- **Endpoint Monitoring**: `/actuator/health` for all services
- **Dependency Checks**: Database, messaging, external service health
- **Circuit Breakers**: Fault tolerance patterns
- **Graceful Degradation**: Fallback mechanisms

## 🚀 Deployment

### Development
```bash
# Build all services
mvn clean package

# Start infrastructure
docker-compose up -d kafka postgresql mongodb rabbitmq redis

# Run services locally
# Each service can be started with: mvn spring-boot:run
```

### Production
```bash
# Build Docker images
mvn clean package -Pdocker

# Deploy with production profile
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Or use Kubernetes
kubectl apply -f k8s/
```

### Scaling
```bash
# Scale specific services
docker-compose up -d --scale anomaly-detector-service=3
docker-compose up -d --scale notification-service=2
```

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify -Pintegration-tests
```

### Load Testing
```bash
# Test log ingestion
for i in {1..1000}; do
  curl -X POST http://localhost:8080/api/logs/events \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -d "{\"serviceName\":\"load-test\",\"logLevel\":\"INFO\",\"message\":\"Test message $i\",\"hostname\":\"test-host\",\"timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"}"
done
```

## 📚 API Documentation

### REST APIs
- **Gateway Service**: http://localhost:8080/swagger-ui.html
- **AI Service**: http://localhost:8000/docs
- **Individual Services**: Each service exposes OpenAPI docs at `/swagger-ui.html`

### WebSocket APIs
- **Dashboard Service**: `ws://localhost:8088/ws`
  - Subscribe to `/topic/alerts` for real-time alerts
  - Subscribe to `/topic/metrics` for system metrics

## 🤝 Contributing

### Development Setup
1. Clone the repository
2. Install Java 21 and Maven
3. Install Python 3.11 and pip
4. Install Docker and Docker Compose
5. Start infrastructure: `docker-compose up -d kafka postgresql mongodb`
6. Run tests: `mvn test`

### Code Style
- **Java**: Google Java Style Guide
- **Python**: PEP 8 with Black formatter
- **Git**: Conventional Commits

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

### Troubleshooting

**Service won't start:**
```bash
# Check logs
docker-compose logs service-name

# Check health
curl http://localhost:8080/actuator/health
```

**Database connection issues:**
```bash
# Reset volumes
docker-compose down -v
docker-compose up -d
```

**High memory usage:**
```bash
# Adjust JVM heap size
export JAVA_OPTS="-Xmx512m"
```

### Getting Help
- **Documentation**: Check service-specific README files
- **Logs**: Enable DEBUG logging for detailed information
- **Metrics**: Monitor Grafana dashboards for system health
- **Health Checks**: Use actuator endpoints for service status
