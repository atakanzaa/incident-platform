#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$PROJECT_ROOT/docker-compose.local.yml"

cd "$PROJECT_ROOT"

echo "Starting Incident Platform Services..."
echo "Project Root: $PROJECT_ROOT"

cleanup() {
    echo "Cleaning up existing containers..."
    docker-compose -f "$COMPOSE_FILE" down --remove-orphans
    docker system prune -f
}

wait_for_service() {
    local service_name=$1
    local port=$2
    local max_attempts=60
    local attempt=1
    
    echo "Waiting for $service_name on port $port..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            echo "$service_name is ready"
            return 0
        fi
        
        if [ $attempt -eq $max_attempts ]; then
            echo "Timeout waiting for $service_name"
            return 1
        fi
        
        echo "Attempt $attempt/$max_attempts - $service_name not ready, waiting 5 seconds..."
        sleep 5
        attempt=$((attempt + 1))
    done
}

wait_for_port() {
    local service_name=$1
    local port=$2
    local max_attempts=60
    local attempt=1
    
    echo "Waiting for $service_name on port $port..."
    
    while [ $attempt -le $max_attempts ]; do
        if nc -z localhost $port 2>/dev/null; then
            echo "$service_name is ready"
            return 0
        fi
        
        if [ $attempt -eq $max_attempts ]; then
            echo "Timeout waiting for $service_name"
            return 1
        fi
        
        echo "Attempt $attempt/$max_attempts - $service_name not ready, waiting 5 seconds..."
        sleep 5
        attempt=$((attempt + 1))
    done
}

echo "Step 1: Infrastructure Services"
echo "Starting PostgreSQL..."
docker-compose -f "$COMPOSE_FILE" up -d postgresql
wait_for_port "PostgreSQL" 5432

echo "Starting MongoDB..."
docker-compose -f "$COMPOSE_FILE" up -d mongodb
wait_for_port "MongoDB" 27017

echo "Starting Zookeeper..."
docker-compose -f "$COMPOSE_FILE" up -d zookeeper
wait_for_port "Zookeeper" 2181

echo "Starting Kafka..."
docker-compose -f "$COMPOSE_FILE" up -d kafka
wait_for_port "Kafka" 9092

echo "Starting RabbitMQ..."
docker-compose -f "$COMPOSE_FILE" up -d rabbitmq
wait_for_port "RabbitMQ" 5672

echo "Starting Redis..."
docker-compose -f "$COMPOSE_FILE" up -d redis
wait_for_port "Redis" 6379

echo "Step 2: Core Platform Services"
echo "Starting Config Server..."
docker-compose -f "$COMPOSE_FILE" up -d config-server
wait_for_service "Config Server" 8888

echo "Starting Discovery Server..."
docker-compose -f "$COMPOSE_FILE" up -d discovery-server
wait_for_service "Discovery Server" 8761

echo "Step 3: AI Service"
echo "Starting AI Service..."
docker-compose -f "$COMPOSE_FILE" up -d ai-service
wait_for_port "AI Service" 8000

echo "Step 4: Gateway Service"
echo "Starting Gateway Service..."
docker-compose -f "$COMPOSE_FILE" up -d gateway-service
wait_for_service "Gateway Service" 8080

echo "Step 5: Authentication Service"
echo "Starting Auth Service..."
docker-compose -f "$COMPOSE_FILE" up -d auth-service
wait_for_service "Auth Service" 8081

echo "Step 6: Data Processing Services"
echo "Starting Log Collector Service..."
docker-compose -f "$COMPOSE_FILE" up -d log-collector-service
wait_for_service "Log Collector Service" 8082

echo "Starting Anomaly Detector Service..."
docker-compose -f "$COMPOSE_FILE" up -d anomaly-detector-service
wait_for_service "Anomaly Detector Service" 8083

echo "Starting Alert Manager Service..."
docker-compose -f "$COMPOSE_FILE" up -d alert-manager-service
wait_for_service "Alert Manager Service" 8084

echo "Step 7: Notification Services"
echo "Starting Notification Service..."
docker-compose -f "$COMPOSE_FILE" up -d notification-service
wait_for_service "Notification Service" 8085

echo "Starting Auto Responder Service..."
docker-compose -f "$COMPOSE_FILE" up -d auto-responder-service
wait_for_service "Auto Responder Service" 8086

echo "Step 8: Tracking and Dashboard Services"
echo "Starting Incident Tracker..."
docker-compose -f "$COMPOSE_FILE" up -d incident-tracker
wait_for_service "Incident Tracker" 8087

echo "Starting Dashboard Service..."
docker-compose -f "$COMPOSE_FILE" up -d dashboard-service
wait_for_service "Dashboard Service" 8088

echo "Step 9: Monitoring Services"
echo "Starting Prometheus..."
docker-compose -f "$COMPOSE_FILE" up -d prometheus
wait_for_port "Prometheus" 9090

echo "Starting Grafana..."
docker-compose -f "$COMPOSE_FILE" up -d grafana
wait_for_port "Grafana" 3000

echo "All services started successfully!"
echo ""
echo "Service Status:"
echo "Infrastructure:"
echo "  PostgreSQL: http://localhost:5432"
echo "  MongoDB: http://localhost:27017"
echo "  Kafka: http://localhost:9092"
echo "  RabbitMQ: http://localhost:15672 (admin/admin123)"
echo "  Redis: http://localhost:6379"
echo ""
echo "Platform Services:"
echo "  Config Server: http://localhost:8888"
echo "  Discovery Server: http://localhost:8761"
echo "  Gateway Service: http://localhost:8080"
echo ""
echo "Application Services:"
echo "  Auth Service: http://localhost:8081"
echo "  Log Collector: http://localhost:8082"
echo "  Anomaly Detector: http://localhost:8083"
echo "  Alert Manager: http://localhost:8084"
echo "  Notification Service: http://localhost:8085"
echo "  Auto Responder: http://localhost:8086"
echo "  Incident Tracker: http://localhost:8087"
echo "  Dashboard Service: http://localhost:8088"
echo ""
echo "AI and Monitoring:"
echo "  AI Service: http://localhost:8000"
echo "  Prometheus: http://localhost:9090"
echo "  Grafana: http://localhost:3000 (admin/admin123)"
echo ""
echo "All services are running and ready for use!" 