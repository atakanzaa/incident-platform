#!/bin/bash

# Local Development Setup Script for Incident Platform
# Bu script local kubernetes cluster ve gerekli servisleri kurar

set -e

echo "🚀 Incident Platform Local Setup Starting..."

# Renklendirme için
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Gerekli araçları kontrol et
check_prerequisites() {
    print_status "Gerekli araçlar kontrol ediliyor..."
    
    local missing_tools=()
    
    command -v docker >/dev/null 2>&1 || missing_tools+=("docker")
    command -v kubectl >/dev/null 2>&1 || missing_tools+=("kubectl")
    command -v helm >/dev/null 2>&1 || missing_tools+=("helm")
    
    if [ ${#missing_tools[@]} -ne 0 ]; then
        print_error "Eksik araçlar: ${missing_tools[*]}"
        print_error "Lütfen şu araçları kurun: https://docs.docker.com/get-docker/, https://kubernetes.io/docs/tasks/tools/"
        exit 1
    fi
    
    print_success "Tüm gerekli araçlar mevcut"
}

# Kubernetes cluster seçimi
choose_kubernetes_platform() {
    print_status "Kubernetes platformu seçin:"
    echo "1) Minikube"
    echo "2) Kind"
    echo "3) Docker Desktop Kubernetes"
    echo "4) Mevcut cluster kullan"
    
    read -p "Seçiminizi yapın (1-4): " choice
    
    case $choice in
        1)
            setup_minikube
            ;;
        2)
            setup_kind
            ;;
        3)
            setup_docker_desktop
            ;;
        4)
            print_status "Mevcut cluster kullanılıyor..."
            ;;
        *)
            print_error "Geçersiz seçim!"
            exit 1
            ;;
    esac
}

# Minikube setup
setup_minikube() {
    print_status "Minikube kontrol ediliyor..."
    
    if ! command -v minikube >/dev/null 2>&1; then
        print_error "Minikube kurulu değil. Lütfen kurun: https://minikube.sigs.k8s.io/docs/start/"
        exit 1
    fi
    
    print_status "Minikube cluster başlatılıyor..."
    minikube start --memory=8192 --cpus=4 --disk-size=50g --driver=docker
    
    print_status "Minikube addons aktifleştiriliyor..."
    minikube addons enable ingress
    minikube addons enable registry
    minikube addons enable metrics-server
    
    # Docker registry port forward
    kubectl port-forward --namespace kube-system service/registry 5000:80 &
    
    export KUBECONFIG=$(minikube kubectl -- config view --raw)
    print_success "Minikube kuruldu ve hazır!"
}

# Kind setup
setup_kind() {
    print_status "Kind kontrol ediliyor..."
    
    if ! command -v kind >/dev/null 2>&1; then
        print_error "Kind kurulu değil. Lütfen kurun: https://kind.sigs.k8s.io/docs/user/quick-start/"
        exit 1
    fi
    
    print_status "Kind cluster oluşturuluyor..."
    
    cat <<EOF | kind create cluster --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  kubeadmConfigPatches:
  - |
    kind: InitConfiguration
    nodeRegistration:
      kubeletExtraArgs:
        node-labels: "ingress-ready=true"
  extraPortMappings:
  - containerPort: 80
    hostPort: 30080
    protocol: TCP
  - containerPort: 443
    hostPort: 30443
    protocol: TCP
  - containerPort: 5000
    hostPort: 5000
    protocol: TCP
- role: worker
- role: worker
EOF
    
    print_success "Kind cluster oluşturuldu!"
}

# Docker Desktop Kubernetes
setup_docker_desktop() {
    print_status "Docker Desktop Kubernetes kontrol ediliyor..."
    
    if ! kubectl cluster-info | grep -q "docker-desktop"; then
        print_warning "Docker Desktop Kubernetes aktif değil. Lütfen Docker Desktop'ta Kubernetes'i aktifleştirin."
        read -p "Kubernetes aktifleştirdikten sonra devam etmek için Enter'a basın..."
    fi
    
    print_success "Docker Desktop Kubernetes hazır!"
}

# Local Docker Registry
setup_local_registry() {
    print_status "Local Docker Registry kuruluyor..."
    
    if ! docker ps | grep -q "registry:2"; then
        docker run -d -p 5000:5000 --restart=always --name registry registry:2
        print_success "Local Docker Registry başlatıldı (localhost:5000)"
    else
        print_status "Local Docker Registry zaten çalışıyor"
    fi
}

# Helm repositories
setup_helm_repos() {
    print_status "Helm repositories ekleniyor..."
    
    helm repo add bitnami https://charts.bitnami.com/bitnami
    helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
    helm repo add grafana https://grafana.github.io/helm-charts
    helm repo add argo https://argoproj.github.io/argo-helm
    helm repo update
    
    print_success "Helm repositories eklendi"
}

# Build ve push images
build_and_push_images() {
    print_status "Docker images build ediliyor ve push ediliyor..."
    
    # Java servisler için
    local services=(
        "config-server" "discovery-server" "gateway-service"
        "auth-service" "log-collector-service" "anomaly-detector-service"
        "alert-manager-service" "notification-service" "auto-responder-service"
        "incident-tracker" "dashboard-service"
    )
    
    for service in "${services[@]}"; do
        print_status "Building $service..."
        cd "$service"
        mvn spring-boot:build-image -Dspring-boot.build-image.imageName=localhost:5000/incident-platform/$service:latest
        docker push localhost:5000/incident-platform/$service:latest
        cd ..
    done
    
    # AI service için
    if [ -d "ai-service" ]; then
        print_status "Building AI service..."
        cd ai-service
        docker build -t localhost:5000/incident-platform/ai-service:latest .
        docker push localhost:5000/incident-platform/ai-service:latest
        cd ..
    fi
    
    print_success "Tüm images build edildi ve push edildi"
}

# Install infrastructure (databases, messaging)
install_infrastructure() {
    print_status "Infrastructure kurulumu başlıyor..."
    
    # Namespace oluştur
    kubectl create namespace incident-platform-local --dry-run=client -o yaml | kubectl apply -f -
    
    # Helm chart ile infrastructure kur
    helm upgrade --install incident-platform-infra helm/incident-platform \
        --namespace incident-platform-local \
        --values environments/local/values.yaml \
        --set postgresql.enabled=true \
        --set mongodb.enabled=true \
        --set kafka.enabled=true \
        --set rabbitmq.enabled=true \
        --set redis.enabled=true \
        --set prometheus.enabled=true \
        --set grafana.enabled=true \
        --set services.config-server.enabled=false \
        --set services.discovery-server.enabled=false \
        --set services.gateway-service.enabled=false \
        --set services.auth-service.enabled=false \
        --set services.log-collector-service.enabled=false \
        --set services.anomaly-detector-service.enabled=false \
        --set services.alert-manager-service.enabled=false \
        --set services.notification-service.enabled=false \
        --set services.auto-responder-service.enabled=false \
        --set services.incident-tracker.enabled=false \
        --set services.dashboard-service.enabled=false \
        --set services.ai-service.enabled=false
    
    print_status "Infrastructure kurulumunu bekliyor..."
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=postgresql --namespace=incident-platform-local --timeout=300s
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=mongodb --namespace=incident-platform-local --timeout=300s
    
    print_success "Infrastructure kuruldu"
}

# Install applications
install_applications() {
    print_status "Application servisler kuruluyor..."
    
    helm upgrade --install incident-platform helm/incident-platform \
        --namespace incident-platform-local \
        --values environments/local/values.yaml \
        --set postgresql.enabled=false \
        --set mongodb.enabled=false \
        --set kafka.enabled=false \
        --set rabbitmq.enabled=false \
        --set redis.enabled=false \
        --set prometheus.enabled=false \
        --set grafana.enabled=false
    
    print_status "Servisler başlayana kadar bekliyor..."
    kubectl wait --for=condition=ready pod -l app=config-server --namespace=incident-platform-local --timeout=300s
    kubectl wait --for=condition=ready pod -l app=discovery-server --namespace=incident-platform-local --timeout=300s
    kubectl wait --for=condition=ready pod -l app=gateway-service --namespace=incident-platform-local --timeout=300s
    
    print_success "Application servisler kuruldu"
}

# Port forwarding setup
setup_port_forwarding() {
    print_status "Port forwarding kuruluyor..."
    
    # Port forward script oluştur
    cat > scripts/port-forward.sh << 'EOF'
#!/bin/bash
echo "🔌 Port forwarding başlatılıyor..."

# Gateway Service
kubectl port-forward service/gateway-service 8080:8080 -n incident-platform-local &
echo "Gateway Service: http://localhost:8080"

# Dashboard Service
kubectl port-forward service/dashboard-service 8088:8088 -n incident-platform-local &
echo "Dashboard Service: http://localhost:8088"

# Grafana
kubectl port-forward service/incident-platform-infra-grafana 3000:80 -n incident-platform-local &
echo "Grafana: http://localhost:3000 (admin/admin123)"

# PostgreSQL
kubectl port-forward service/incident-platform-infra-postgresql 5432:5432 -n incident-platform-local &
echo "PostgreSQL: localhost:5432"

# MongoDB
kubectl port-forward service/incident-platform-infra-mongodb 27017:27017 -n incident-platform-local &
echo "MongoDB: localhost:27017"

# RabbitMQ Management
kubectl port-forward service/incident-platform-infra-rabbitmq 15672:15672 -n incident-platform-local &
echo "RabbitMQ Management: http://localhost:15672 (admin/admin123)"

echo "✅ Port forwarding aktif! Çıkmak için Ctrl+C"
wait
EOF
    
    chmod +x scripts/port-forward.sh
    print_success "Port forwarding script oluşturuldu: scripts/port-forward.sh"
}

# Status kontrolü
check_status() {
    print_status "Sistem durumu kontrol ediliyor..."
    
    echo ""
    echo "📊 Pod Durumları:"
    kubectl get pods -n incident-platform-local
    
    echo ""
    echo "🌐 Servisler:"
    kubectl get services -n incident-platform-local
    
    echo ""
    echo "🔗 Erişim Bilgileri:"
    echo "Gateway Service: kubectl port-forward service/gateway-service 8080:8080 -n incident-platform-local"
    echo "Dashboard: kubectl port-forward service/dashboard-service 8088:8088 -n incident-platform-local"
    echo "Grafana: kubectl port-forward service/incident-platform-infra-grafana 3000:80 -n incident-platform-local"
    echo ""
    echo "Port forwarding için çalıştırın: ./scripts/port-forward.sh"
}

# Ana fonksiyon
main() {
    print_status "🎯 Incident Platform Local Setup"
    echo ""
    
    check_prerequisites
    choose_kubernetes_platform
    setup_local_registry
    setup_helm_repos
    
    read -p "Docker images build etmek istiyor musunuz? (y/n): " build_images
    if [[ $build_images =~ ^[Yy]$ ]]; then
        build_and_push_images
    fi
    
    install_infrastructure
    install_applications
    setup_port_forwarding
    check_status
    
    print_success "🎉 Local setup tamamlandı!"
    print_status "Port forwarding başlatmak için: ./scripts/port-forward.sh"
}

# Script'i çalıştır
main "$@" 