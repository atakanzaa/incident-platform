# Incident Platform

A comprehensive microservices-based incident management platform built with Spring Boot, featuring real-time log analysis, anomaly detection, alert management, and automated response capabilities.

## Architecture

The platform consists of 12 microservices orchestrated using Docker Compose for local development and Kubernetes for production deployment:

### Core Services
- **Config Server** (8888) - Centralized configuration management
- **Discovery Server** (8761) - Service discovery with Eureka
- **Gateway Service** (8080) - API Gateway with routing and security

### Application Services
- **Auth Service** (8081) - Authentication and authorization
- **Log Collector** (8082) - Log ingestion and processing
- **Anomaly Detector** (8083) - ML-based anomaly detection
- **Alert Manager** (8084) - Alert processing and routing
- **Notification Service** (8085) - Multi-channel notifications
- **Auto Responder** (8086) - Automated incident response
- **Incident Tracker** (8087) - Incident lifecycle management
- **Dashboard Service** (8088) - Real-time monitoring dashboard

### AI & Infrastructure
- **AI Service** (8000) - Machine learning models (Python/FastAPI)
- **PostgreSQL** (5432) - Primary database
- **MongoDB** (27017) - Document store for incidents
- **Kafka** (9092) - Event streaming
- **RabbitMQ** (5672/15672) - Message queue
- **Redis** (6379) - Caching
- **Prometheus** (9090) - Metrics collection
- **Grafana** (3000) - Monitoring dashboards

## Quick Start

### Prerequisites
- Docker and Docker Compose
- Java 21
- Maven 3.9+
- Python 3.11+ (for AI service)

### Option 1: Start All Services (Recommended)

**Linux/macOS:**
```bash
# Start all services in correct dependency order
./scripts/start-all-services.sh
```

**Windows:**
```powershell
# Start all services in correct dependency order
.\scripts\start-all-services.ps1

# With cleanup (removes existing containers first)
.\scripts\start-all-services.ps1 -Cleanup
```

### Option 2: Manual Start
```bash
# Start infrastructure first
docker-compose -f docker-compose.local.yml up -d postgresql mongodb kafka rabbitmq redis

# Start core services
docker-compose -f docker-compose.local.yml up -d config-server discovery-server

# Start all application services
docker-compose -f docker-compose.local.yml up -d
```

### Service Startup Order
The scripts ensure proper dependency order:

1. **Infrastructure**: PostgreSQL, MongoDB, Zookeeper, Kafka, RabbitMQ, Redis
2. **Core Platform**: Config Server, Discovery Server
3. **AI Service**: Machine learning capabilities
4. **Gateway**: API Gateway and routing
5. **Authentication**: Auth Service
6. **Data Processing**: Log Collector, Anomaly Detector, Alert Manager
7. **Notifications**: Notification Service, Auto Responder
8. **Tracking**: Incident Tracker, Dashboard Service
9. **Monitoring**: Prometheus, Grafana

## Access URLs

### Application Services
- **API Gateway**: http://localhost:8080
- **Auth Service**: http://localhost:8081
- **Log Collector**: http://localhost:8082
- **Anomaly Detector**: http://localhost:8083
- **Alert Manager**: http://localhost:8084
- **Notification Service**: http://localhost:8085
- **Auto Responder**: http://localhost:8086
- **Incident Tracker**: http://localhost:8087
- **Dashboard**: http://localhost:8088

### Infrastructure
- **Discovery Server**: http://localhost:8761
- **Config Server**: http://localhost:8888
- **RabbitMQ Management**: http://localhost:15672 (admin/admin123)
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin123)
- **AI Service**: http://localhost:8000

## Development

### Environment Configuration
All services are configured for local development with:
- Config Server disabled (native mode)
- Kubernetes integration disabled
- RabbitMQ credentials: admin/admin123
- Database auto-configuration enabled

### Health Checks
All services include health endpoints:
```bash
curl http://localhost:PORT/actuator/health
```

### Logs
View service logs:
```bash
docker-compose -f docker-compose.local.yml logs -f SERVICE_NAME
```

## Production Deployment

### Kubernetes with ArgoCD
The platform supports GitOps deployment using ArgoCD:

```bash
# Deploy to staging
kubectl apply -f argocd/applications/incident-platform-staging.yaml

# Deploy to production  
kubectl apply -f argocd/applications/incident-platform-production.yaml
```

### Helm Charts
```bash
# Install with Helm
helm install incident-platform ./helm/incident-platform

# Upgrade
helm upgrade incident-platform ./helm/incident-platform
```

## Configuration

### Environment Variables
Key configuration options:

#### RabbitMQ
- `RABBITMQ_HOST`: RabbitMQ server (default: localhost)
- `RABBITMQ_USERNAME`: Username (default: admin)
- `RABBITMQ_PASSWORD`: Password (default: admin123)

#### Kafka
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka brokers (default: localhost:9092)

#### Databases
- `DB_USERNAME`: PostgreSQL username (default: postgres)
- `DB_PASSWORD`: PostgreSQL password (default: 123456)
- `MONGODB_HOST`: MongoDB server (default: localhost)

#### AI Service
- `AI_SERVICE_URL`: AI service endpoint (default: http://localhost:8000)

### Multi-Environment Support
- **Local**: docker-compose.local.yml
- **Staging**: environments/staging/
- **Production**: environments/production/

## CI/CD Pipeline

### Jenkins Pipeline
The project includes a comprehensive Jenkins pipeline:
- Automated testing
- Docker image building
- Multi-environment deployment
- Security scanning with SonarQube

### GitOps Workflow
1. Code changes pushed to Git
2. Jenkins builds and tests
3. Docker images pushed to registry
4. ArgoCD syncs Kubernetes deployments
5. Automated rollback on failure

## Monitoring & Observability

### Metrics
- **Prometheus**: Metrics collection
- **Grafana**: Visualization and alerting
- **Custom dashboards**: Per-service monitoring

### Logging
- **Centralized logging**: ELK stack integration
- **Structured logs**: JSON format
- **Log aggregation**: Kafka-based streaming

### Health Checks
- **Spring Boot Actuator**: Health endpoints
- **Kubernetes probes**: Liveness and readiness
- **Custom health indicators**: Database, messaging

## Security

### Authentication
- **JWT-based**: Stateless authentication
- **Role-based access**: RBAC implementation
- **Password encryption**: BCrypt hashing

### Network Security
- **Service mesh**: Istio integration (optional)
- **TLS encryption**: End-to-end security
- **Network policies**: Kubernetes-native

## Performance

### Scalability
- **Horizontal scaling**: Kubernetes HPA
- **Load balancing**: Spring Cloud Gateway
- **Circuit breakers**: Resilience4j integration

### Optimization
- **Caching**: Redis integration
- **Connection pooling**: HikariCP
- **Async processing**: Spring @Async

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## Support

For support and questions:
- Create an issue in the repository
- Check the documentation in `/docs`
- Review the troubleshooting guide

## License

This project is licensed under the MIT License - see the LICENSE file for details.
