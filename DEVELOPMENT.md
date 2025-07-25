# Development Guide - Multi-Environment Setup

Bu doküman, Incident Platform'un geliştirme, staging ve production ortamlarında nasıl çalıştırılacağını detaylandırır.

## 🎯 Ortam Stratejisi

### Development (Geliştirme)
- **Amaç**: Yerel geliştirme ve özellik testleri
- **Branch**: `development`
- **Altyapı**: Docker Compose
- **Database**: Yerel PostgreSQL/MongoDB instance'ları
- **Güvenlik**: Minimal güvenlik (geliştirme kolaylığı için)
- **Otomatik Deployment**: Evet, her commit'te

### Staging (Test)
- **Amaç**: Entegrasyon testleri ve üretim öncesi doğrulama
- **Branch**: `develop`
- **Altyapı**: Kubernetes cluster
- **Database**: Paylaşılan staging veritabanları
- **Güvenlik**: Üretim benzeri güvenlik ayarları
- **Otomatik Deployment**: Evet, testler başarılı olduktan sonra

### Production (Üretim)
- **Amaç**: Canlı üretim iş yükleri
- **Branch**: `main`
- **Altyapı**: Yüksek erişilebilirlik Kubernetes
- **Database**: Replike edilmiş, yedeklenmiş üretim veritabanları
- **Güvenlik**: Maksimum güvenlik ve uyumluluk
- **Otomatik Deployment**: Manuel onay gerekli

## 🚀 Hızlı Başlangıç

### Gereksinimler
- Docker & Docker Compose
- Java 21
- Maven 3.9+
- Python 3.8+ (AI service için)
- kubectl (Kubernetes ortamları için)
- Helm 3.12+

### Development Ortamını Çalıştırma

```bash
# Repository'yi klonlayın
git clone https://github.com/atakanzaa/incident-platform.git
cd incident-platform

# Development ortamını başlatın
docker-compose -f docker-compose.development.yml up -d

# Servis durumunu kontrol edin
docker-compose -f docker-compose.development.yml ps

# Logları görüntüleyin
docker-compose -f docker-compose.development.yml logs -f
```

### Staging Ortamını Çalıştırma

```bash
# Kubernetes'e deployment yapın
kubectl apply -f argocd/applications/incident-platform-staging.yaml

# Deployment'ı izleyin
kubectl get pods -n incident-platform-staging -w

# Servis loglarını görüntüleyin
kubectl logs -f deployment/gateway-service -n incident-platform-staging
```

### Production Ortamını Çalıştırma

```bash
# CI/CD pipeline üzerinden (Önerilen)
git checkout main
git merge develop
git push origin main
# Jenkins'de manuel onay gerekli

# Acil durum için manuel deployment
kubectl apply -f argocd/applications/incident-platform-production.yaml
```

## 🔧 Konfigürasyon Detayları

### Spring Boot Profilleri

Her servis üç Spring profile'ı destekler:

#### Development Profile
```yaml
spring:
  profiles:
    active: development
  datasource:
    url: jdbc:postgresql://localhost:5432/incident_auth
    username: postgres
    password: 123456
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update

logging:
  level:
    com.incident: DEBUG
    org.springframework.security: DEBUG
```

#### Staging Profile
```yaml
spring:
  profiles:
    active: staging
  datasource:
    url: jdbc:postgresql://postgresql-staging:5432/incident_auth
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 5
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate

logging:
  level:
    com.incident: INFO
    root: INFO
```

#### Production Profile
```yaml
spring:
  profiles:
    active: production
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/incident_auth
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      leak-detection-threshold: 60000
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate

logging:
  level:
    com.incident: WARN
    root: WARN
  file:
    name: /var/log/service/application.log
```

### Environment Variables

#### Development
```bash
export SPRING_PROFILES_ACTIVE=development
export DB_HOST=localhost
export DB_USERNAME=postgres
export DB_PASSWORD=123456
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export RABBITMQ_HOST=localhost
export REDIS_HOST=localhost
export JWT_SECRET=mySecretKey12345678901234567890
export LOG_LEVEL=DEBUG
```

#### Staging
```bash
export SPRING_PROFILES_ACTIVE=staging
export DB_HOST=postgresql-staging
export DB_USERNAME=postgres
export DB_PASSWORD=${STAGING_DB_PASSWORD}
export KAFKA_BOOTSTRAP_SERVERS=kafka-staging:9092
export RABBITMQ_HOST=rabbitmq-staging
export REDIS_HOST=redis-staging
export JWT_SECRET=${STAGING_JWT_SECRET}
export LOG_LEVEL=INFO
```

#### Production
```bash
export SPRING_PROFILES_ACTIVE=production
export DB_HOST=${PROD_DB_HOST}
export DB_USERNAME=${PROD_DB_USERNAME}
export DB_PASSWORD=${PROD_DB_PASSWORD}
export KAFKA_BOOTSTRAP_SERVERS=${PROD_KAFKA_SERVERS}
export KAFKA_SASL_CONFIG=${PROD_KAFKA_SASL}
export RABBITMQ_HOST=${PROD_RABBITMQ_HOST}
export REDIS_HOST=${PROD_REDIS_HOST}
export JWT_SECRET=${PROD_JWT_SECRET}
export LOG_LEVEL=WARN
```

## 🔄 Git Workflow

### Feature Development
```bash
# Development branch'inden feature branch oluştur
git checkout development
git checkout -b feature/yeni-ozellik

# Değişiklikleri yap ve test et
docker-compose -f docker-compose.development.yml up -d

# Commit ve push
git add .
git commit -m "feat: yeni özellik eklendi"
git push origin feature/yeni-ozellik

# Development branch'ine PR oluştur
```

### Staging'e Deploy
```bash
# Feature'ı develop branch'ine merge et
git checkout develop
git merge feature/yeni-ozellik
git push origin develop

# Otomatik staging deployment tetiklenir
# Staging testlerini çalıştır
```

### Production'a Deploy
```bash
# Develop'ı main branch'ine merge et
git checkout main
git merge develop
git push origin main

# Jenkins'de manuel onay bekle
# Production deployment sonrası smoke testler çalışır
```

## 🧪 Test Stratejisi

### Unit Tests
```bash
# Tüm unit testleri çalıştır
mvn test -Dspring.profiles.active=development

# Belirli bir servis için
cd auth-service
mvn test -Dspring.profiles.active=development
```

### Integration Tests
```bash
# Staging ortamında integration testleri
mvn test -Pintegration-tests -Dspring.profiles.active=staging

# Yerel integration testleri
docker-compose -f docker-compose.staging.yml up -d
mvn verify -Pintegration-tests
docker-compose -f docker-compose.staging.yml down
```

### End-to-End Tests
```bash
# Staging E2E testleri
mvn test -Pstaging-e2e-tests

# Production smoke testleri
mvn test -Pperformance-tests -Dtest.environment=production
```

## 📊 Monitoring ve Logging

### Development
```bash
# Docker Compose logları
docker-compose -f docker-compose.development.yml logs -f gateway-service

# Prometheus metrikleri
curl http://localhost:9090/metrics

# Health check'ler
curl http://localhost:8080/actuator/health
```

### Staging/Production
```bash
# Kubernetes logları
kubectl logs -f deployment/gateway-service -n incident-platform-staging

# Prometheus sorguları
kubectl port-forward svc/prometheus 9090:9090 -n incident-platform-staging

# Grafana dashboard'ları
kubectl port-forward svc/grafana 3000:3000 -n incident-platform-staging
```

## 🔒 Güvenlik Konfigürasyonları

### Development
- CORS: Tüm origin'lere açık
- TLS: Devre dışı
- Auth: Basit JWT token
- Secrets: Hardcoded değerler

### Staging
- CORS: Staging domain'lerine kısıtlı
- TLS: Zorunlu
- Auth: Production benzeri JWT
- Secrets: Kubernetes secrets

### Production
- CORS: Sadece production domain'leri
- TLS: mTLS zorunlu
- Auth: Güçlendirilmiş JWT + RBAC
- Secrets: Encrypted secret management
- Network policies: Aktif
- Pod security policies: Aktif

## 🐳 Docker Konfigürasyonları

### Development Compose File
```yaml
version: '3.8'
services:
  gateway-service:
    build: ./gateway-service
    environment:
      SPRING_PROFILES_ACTIVE: development
      EUREKA_URL: http://discovery-server:8761/eureka/
    ports:
      - "8080:8080"
    depends_on:
      - discovery-server
```

### Staging/Production
Kubernetes Helm charts kullanılır:
```yaml
# environments/staging/values.yaml
environment: staging
services:
  gateway-service:
    replicas: 2
    resources:
      requests:
        memory: "512Mi"
        cpu: "250m"
      limits:
        memory: "1Gi"
        cpu: "500m"
```

## 🚨 Troubleshooting

### Yaygın Sorunlar

#### Service Discovery Sorunları
```bash
# Eureka registry'yi kontrol et
curl http://localhost:8761/eureka/apps

# Service registration'ı kontrol et
kubectl exec -it deployment/gateway-service -- curl http://discovery-server:8761/eureka/apps
```

#### Database Bağlantı Sorunları
```bash
# PostgreSQL bağlantısını test et
kubectl exec -it deployment/auth-service -- nc -zv postgresql 5432

# MongoDB bağlantısını test et
kubectl exec -it deployment/incident-tracker -- nc -zv mongodb 27017
```

#### Kafka Sorunları
```bash
# Kafka broker'ları listele
kubectl exec -it deployment/kafka -- kafka-broker-api-versions --bootstrap-server localhost:9092

# Topic'leri listele
kubectl exec -it deployment/kafka -- kafka-topics --list --bootstrap-server localhost:9092
```

### Log Analysis
```bash
# Hata loglarını filtrele
kubectl logs deployment/gateway-service -n incident-platform-staging | grep ERROR

# JSON logları parse et
kubectl logs deployment/auth-service -n incident-platform-staging | jq '.level == "ERROR"'
```

## 📈 Performance Tuning

### Development
- JVM heap: 256MB
- Connection pool: 5
- Cache: Basit in-memory

### Staging
- JVM heap: 512MB
- Connection pool: 10
- Cache: Redis cluster

### Production
- JVM heap: 2GB
- Connection pool: 20
- Cache: Redis cluster with failover
- Auto-scaling: HPA enabled

## 🔄 Deployment Strategies

### Rolling Update (Default)
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 1
    maxSurge: 1
```

### Blue-Green (Production Critical Services)
```bash
# Blue-green deployment için
helm upgrade --install incident-platform-blue ./helm/incident-platform \
  --namespace incident-platform-prod \
  --values environments/production/values.yaml \
  --set global.imageTag=blue-v1.2.3
```

### Canary (Production New Features)
```yaml
# Canary deployment için istio kullan
apiVersion: argoproj.io/v1alpha1
kind: Rollout
spec:
  strategy:
    canary:
      steps:
      - setWeight: 20
      - pause: {duration: 1h}
      - setWeight: 50
      - pause: {duration: 30m}
      - setWeight: 100
```

Bu doküman platform'un farklı ortamlarda nasıl geliştirileceği ve deploy edileceği konusunda kapsamlı bilgi sağlar. 