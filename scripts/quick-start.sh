#!/bin/bash

# Quick Start Script for Local Development
# Bu script incident platform'u hızlıca başlatır

set -e

echo "🚀 Incident Platform Quick Start"

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

# Docker kontrolü
check_docker() {
    if ! command -v docker >/dev/null 2>&1; then
        print_error "Docker kurulu değil. Lütfen Docker'ı kurun: https://docs.docker.com/get-docker/"
        exit 1
    fi
    
    if ! command -v docker-compose >/dev/null 2>&1; then
        print_error "Docker Compose kurulu değil. Lütfen Docker Compose'u kurun"
        exit 1
    fi
    
    print_success "Docker ve Docker Compose mevcut"
}

# Seçenekleri göster
show_options() {
    print_status "Hangi ortamda çalıştırmak istiyorsunuz?"
    echo ""
    echo "1) 🐳 Docker Compose (Hızlı başlangıç, domain gerekmez)"
    echo "2) ☸️  Kubernetes (Minikube/Kind ile)"
    echo "3) 🏗️  CI/CD Setup (Jenkins + ArgoCD)"
    echo "4) 📊 Sadece Infrastructure (Database, Kafka, vb.)"
    echo "5) 🧪 Test Environment"
    echo ""
    read -p "Seçiminizi yapın (1-5): " choice
    
    case $choice in
        1)
            start_docker_compose
            ;;
        2)
            start_kubernetes
            ;;
        3)
            setup_cicd
            ;;
        4)
            start_infrastructure_only
            ;;
        5)
            run_tests
            ;;
        *)
            print_error "Geçersiz seçim!"
            exit 1
            ;;
    esac
}

# Docker Compose ile başlat
start_docker_compose() {
    print_status "🐳 Docker Compose ile başlatılıyor..."
    
    # İlk önce infrastructure servislerini başlat
    print_status "Infrastructure servisleri başlatılıyor..."
    docker-compose -f docker-compose.local.yml up -d postgresql mongodb kafka zookeeper rabbitmq redis
    
    # Servislerin hazır olmasını bekle
    print_status "Infrastructure servislerinin hazır olması bekleniyor..."
    sleep 30
    
    # AI service'i başlat
    print_status "AI service başlatılıyor..."
    docker-compose -f docker-compose.local.yml up -d ai-service
    sleep 20
    
    # Core servisleri başlat
    print_status "Core servisler başlatılıyor..."
    docker-compose -f docker-compose.local.yml up -d config-server discovery-server
    sleep 30
    
    # Gateway'i başlat
    print_status "Gateway service başlatılıyor..."
    docker-compose -f docker-compose.local.yml up -d gateway-service
    sleep 20
    
    # Diğer application servislerini başlat
    print_status "Application servisler başlatılıyor..."
    docker-compose -f docker-compose.local.yml up -d auth-service log-collector-service anomaly-detector-service alert-manager-service notification-service auto-responder-service incident-tracker dashboard-service
    
    # Monitoring servislerini başlat (isteğe bağlı)
    print_status "Monitoring servisleri başlatılıyor..."
    docker-compose -f docker-compose.local.yml up -d prometheus grafana
    
    show_docker_compose_info
}

# Kubernetes ile başlat
start_kubernetes() {
    print_status "☸️ Kubernetes setup başlatılıyor..."
    
    if [ ! -f "scripts/local-setup.sh" ]; then
        print_error "Local setup script bulunamadı!"
        exit 1
    fi
    
    chmod +x scripts/local-setup.sh
    ./scripts/local-setup.sh
}

# CI/CD setup
setup_cicd() {
    print_status "🏗️ CI/CD setup başlatılıyor..."
    
    if [ ! -f "scripts/local-jenkins.sh" ]; then
        print_error "Jenkins setup script bulunamadı!"
        exit 1
    fi
    
    chmod +x scripts/local-jenkins.sh
    ./scripts/local-jenkins.sh
}

# Sadece infrastructure
start_infrastructure_only() {
    print_status "📊 Sadece infrastructure servisleri başlatılıyor..."
    
    docker-compose -f docker-compose.local.yml up -d postgresql mongodb kafka zookeeper rabbitmq redis prometheus grafana
    
    echo ""
    echo "📌 Infrastructure Servisleri:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "🐘 PostgreSQL: localhost:5432 (postgres/postgres123)"
    echo "🍃 MongoDB: localhost:27017 (admin/admin123)"
    echo "📡 Kafka: localhost:9092"
    echo "🐰 RabbitMQ: http://localhost:15672 (admin/admin123)"
    echo "🔴 Redis: localhost:6379"
    echo "📊 Prometheus: http://localhost:9090"
    echo "📈 Grafana: http://localhost:3000 (admin/admin123)"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

# Test çalıştır
run_tests() {
    print_status "🧪 Test environment hazırlanıyor..."
    
    # Infrastructure servislerini başlat
    docker-compose -f docker-compose.local.yml up -d postgresql mongodb kafka zookeeper rabbitmq redis
    sleep 30
    
    # Unit testleri çalıştır
    print_status "Unit testler çalıştırılıyor..."
    mvn test -q
    
    # Integration testleri çalıştır
    print_status "Integration testler çalıştırılıyor..."
    mvn verify -Pintegration-tests -q
    
    # Infrastructure'ü temizle
    docker-compose -f docker-compose.local.yml down
    
    print_success "Testler tamamlandı!"
}

# Docker Compose bilgilerini göster
show_docker_compose_info() {
    print_success "🎉 Sistem başarıyla başlatıldı!"
    echo ""
    echo "📌 Servis Erişim Bilgileri:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "🌐 Gateway Service: http://localhost:8080"
    echo "🔐 Auth Service: http://localhost:8081"
    echo "📊 Dashboard: http://localhost:8088"
    echo "🤖 AI Service: http://localhost:8000"
    echo ""
    echo "🗄️  Databases:"
    echo "   PostgreSQL: localhost:5432 (postgres/postgres123)"
    echo "   MongoDB: localhost:27017 (admin/admin123)"
    echo ""
    echo "📡 Messaging:"
    echo "   Kafka: localhost:9092"
    echo "   RabbitMQ Management: http://localhost:15672 (admin/admin123)"
    echo "   Redis: localhost:6379"
    echo ""
    echo "📈 Monitoring:"
    echo "   Prometheus: http://localhost:9090"
    echo "   Grafana: http://localhost:3000 (admin/admin123)"
    echo ""
    echo "🔧 Faydalı Komutlar:"
    echo "   Logları izle: docker-compose -f docker-compose.local.yml logs -f"
    echo "   Servisleri durdur: docker-compose -f docker-compose.local.yml down"
    echo "   Sistem durumu: docker-compose -f docker-compose.local.yml ps"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "✅ Sistem hazır! Gateway üzerinden API'leri test edebilirsiniz."
    echo "📖 API Dokümantasyonu: http://localhost:8080/swagger-ui.html"
}

# Sistem durumunu kontrol et
check_system_status() {
    print_status "Sistem durumu kontrol ediliyor..."
    
    if command -v docker-compose >/dev/null 2>&1; then
        echo "Docker Compose Servisleri:"
        docker-compose -f docker-compose.local.yml ps
    fi
    
    if command -v kubectl >/dev/null 2>&1; then
        echo ""
        echo "Kubernetes Pods:"
        kubectl get pods --all-namespaces | grep incident-platform || echo "Kubernetes'te incident-platform pod'u bulunamadı"
    fi
}

# Temizlik fonksiyonu
cleanup() {
    print_status "Sistem temizleniyor..."
    
    read -p "Docker Compose servislerini durdurmak istiyor musunuz? (y/n): " stop_docker
    if [[ $stop_docker =~ ^[Yy]$ ]]; then
        docker-compose -f docker-compose.local.yml down -v
        print_success "Docker Compose servisleri durduruldu"
    fi
    
    read -p "Kubernetes resources'ları temizlemek istiyor musunuz? (y/n): " cleanup_k8s
    if [[ $cleanup_k8s =~ ^[Yy]$ ]] && command -v kubectl >/dev/null 2>&1; then
        kubectl delete namespace incident-platform-local --ignore-not-found=true
        kubectl delete namespace jenkins --ignore-not-found=true
        kubectl delete namespace argocd --ignore-not-found=true
        print_success "Kubernetes resources temizlendi"
    fi
}

# Ana menü
main_menu() {
    while true; do
        echo ""
        echo "🎯 Incident Platform Local Development"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "1) 🚀 Sistemi Başlat"
        echo "2) 📊 Sistem Durumunu Kontrol Et"
        echo "3) 🧹 Sistemi Temizle"
        echo "4) ❌ Çıkış"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        
        read -p "Seçiminizi yapın (1-4): " menu_choice
        
        case $menu_choice in
            1)
                show_options
                ;;
            2)
                check_system_status
                ;;
            3)
                cleanup
                ;;
            4)
                print_success "İyi günler!"
                exit 0
                ;;
            *)
                print_error "Geçersiz seçim!"
                ;;
        esac
    done
}

# Script başlangıcı
echo "🎯 Incident Platform Quick Start"
echo ""

check_docker

# Eğer argüman verilmişse direkt çalıştır
if [ "$1" = "docker" ]; then
    start_docker_compose
elif [ "$1" = "k8s" ]; then
    start_kubernetes
elif [ "$1" = "cicd" ]; then
    setup_cicd
elif [ "$1" = "infra" ]; then
    start_infrastructure_only
elif [ "$1" = "test" ]; then
    run_tests
elif [ "$1" = "status" ]; then
    check_system_status
elif [ "$1" = "clean" ]; then
    cleanup
else
    main_menu
fi 