#!/bin/bash

# Local Jenkins Setup Script
# Bu script local Kubernetes cluster'da Jenkins kurar

set -e

echo "🏗️ Local Jenkins Setup Starting..."

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

# Jenkins namespace oluştur
create_namespace() {
    print_status "Jenkins namespace oluşturuluyor..."
    kubectl create namespace jenkins --dry-run=client -o yaml | kubectl apply -f -
    print_success "Jenkins namespace oluşturuldu"
}

# Jenkins için gerekli RBAC
setup_rbac() {
    print_status "Jenkins RBAC kuruluyor..."
    
    cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: ServiceAccount
metadata:
  name: jenkins
  namespace: jenkins
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: jenkins
rules:
- apiGroups: [""]
  resources: ["pods", "pods/exec", "pods/log", "services", "secrets", "configmaps"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
- apiGroups: ["apps"]
  resources: ["deployments", "replicasets"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
- apiGroups: [""]
  resources: ["namespaces"]
  verbs: ["get", "list", "watch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: jenkins
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: jenkins
subjects:
- kind: ServiceAccount
  name: jenkins
  namespace: jenkins
EOF
    
    print_success "Jenkins RBAC kuruldu"
}

# Jenkins PVC oluştur
create_pvc() {
    print_status "Jenkins PVC oluşturuluyor..."
    
    cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: jenkins-pvc
  namespace: jenkins
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
EOF
    
    print_success "Jenkins PVC oluşturuldu"
}

# Jenkins deployment
deploy_jenkins() {
    print_status "Jenkins deployment oluşturuluyor..."
    
    cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jenkins
  namespace: jenkins
spec:
  replicas: 1
  selector:
    matchLabels:
      app: jenkins
  template:
    metadata:
      labels:
        app: jenkins
    spec:
      serviceAccountName: jenkins
      containers:
      - name: jenkins
        image: jenkins/jenkins:lts
        ports:
        - containerPort: 8080
        - containerPort: 50000
        volumeMounts:
        - name: jenkins-home
          mountPath: /var/jenkins_home
        - name: docker-sock
          mountPath: /var/run/docker.sock
        env:
        - name: JAVA_OPTS
          value: "-Djenkins.install.runSetupWizard=false"
        - name: CASC_JENKINS_CONFIG
          value: "/var/jenkins_home/casc_configs/jenkins.yaml"
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
      volumes:
      - name: jenkins-home
        persistentVolumeClaim:
          claimName: jenkins-pvc
      - name: docker-sock
        hostPath:
          path: /var/run/docker.sock
---
apiVersion: v1
kind: Service
metadata:
  name: jenkins
  namespace: jenkins
spec:
  type: NodePort
  ports:
  - name: http
    port: 8080
    targetPort: 8080
    nodePort: 30808
  - name: jnlp
    port: 50000
    targetPort: 50000
  selector:
    app: jenkins
EOF
    
    print_success "Jenkins deployment oluşturuldu"
}

# Jenkins konfigürasyon
create_jenkins_config() {
    print_status "Jenkins konfigürasyonu oluşturuluyor..."
    
    cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: jenkins-config
  namespace: jenkins
data:
  jenkins.yaml: |
    jenkins:
      systemMessage: "Jenkins for Incident Platform Local Development"
      numExecutors: 2
      scmCheckoutRetryCount: 3
      mode: NORMAL
      
    security:
      globalJobDslSecurityConfiguration:
        useScriptSecurity: false
        
    unclassified:
      location:
        url: "http://localhost:30808"
        adminAddress: "admin@localhost"
        
    jobs:
      - script: |
          pipelineJob('incident-platform-build') {
            definition {
              cpsScm {
                scm {
                  git {
                    remote {
                      url('https://github.com/atakanzaa/incident-platform.git')
                    }
                    branch('*/main')
                  }
                }
                scriptPath('Jenkinsfile')
              }
            }
            triggers {
              scm('H/5 * * * *')
            }
          }
EOF
    
    print_success "Jenkins konfigürasyonu oluşturuldu"
}

# Jenkins plugins
install_plugins() {
    print_status "Jenkins plugins kurulması bekleniyor..."
    
    # Jenkins'in başlamasını bekle
    print_status "Jenkins'in başlaması bekleniyor..."
    kubectl wait --for=condition=ready pod -l app=jenkins --namespace=jenkins --timeout=300s
    
    # Admin şifresini al
    sleep 30  # Jenkins'in tamamen başlaması için
    JENKINS_POD=$(kubectl get pods -n jenkins -l app=jenkins -o jsonpath='{.items[0].metadata.name}')
    ADMIN_PASSWORD=$(kubectl exec -n jenkins $JENKINS_POD -- cat /var/jenkins_home/secrets/initialAdminPassword)
    
    print_success "Jenkins admin şifresi: $ADMIN_PASSWORD"
    print_success "Jenkins URL: http://localhost:30808"
}

# ArgoCD setup
setup_argocd() {
    print_status "ArgoCD kuruluyor..."
    
    # ArgoCD namespace
    kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
    
    # ArgoCD install
    kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
    
    # ArgoCD service NodePort olarak expose et
    kubectl patch svc argocd-server -n argocd -p '{"spec":{"type":"NodePort","ports":[{"port":80,"targetPort":8080,"nodePort":30443,"name":"http"},{"port":443,"targetPort":8080,"nodePort":30444,"name":"https"}]}}'
    
    print_status "ArgoCD kurulumu tamamlandı. Başlaması bekleniyor..."
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=argocd-server --namespace=argocd --timeout=300s
    
    # ArgoCD admin şifresini al
    ARGOCD_PASSWORD=$(kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d)
    
    print_success "ArgoCD admin şifresi: $ARGOCD_PASSWORD"
    print_success "ArgoCD URL: http://localhost:30443 (admin/$ARGOCD_PASSWORD)"
}

# Erişim bilgilerini göster
show_access_info() {
    print_success "🎉 Local Jenkins ve ArgoCD kurulumu tamamlandı!"
    echo ""
    echo "📌 Erişim Bilgileri:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "🏗️  Jenkins:"
    echo "   URL: http://localhost:30808"
    echo "   Admin şifresi almak için:"
    echo "   kubectl exec -n jenkins \$(kubectl get pods -n jenkins -l app=jenkins -o jsonpath='{.items[0].metadata.name}') -- cat /var/jenkins_home/secrets/initialAdminPassword"
    echo ""
    echo "🚀 ArgoCD:"
    echo "   URL: http://localhost:30443"
    echo "   Username: admin"
    echo "   Password almak için:"
    echo "   kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d"
    echo ""
    echo "🔧 Komutlar:"
    echo "   Jenkins logs: kubectl logs -f deployment/jenkins -n jenkins"
    echo "   ArgoCD logs: kubectl logs -f deployment/argocd-server -n argocd"
    echo "   Port forward Jenkins: kubectl port-forward service/jenkins 8080:8080 -n jenkins"
    echo "   Port forward ArgoCD: kubectl port-forward service/argocd-server 8080:80 -n argocd"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

# Ana fonksiyon
main() {
    print_status "🏗️ Jenkins ve ArgoCD Local Setup"
    echo ""
    
    create_namespace
    setup_rbac
    create_pvc
    deploy_jenkins
    create_jenkins_config
    install_plugins
    setup_argocd
    show_access_info
}

# Script'i çalıştır
main "$@" 