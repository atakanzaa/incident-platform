pipeline {
    agent any
    
    tools {
        maven 'Maven-3.9'
        jdk 'OpenJDK-21'
    }
    
    environment {
        // Docker Registry - Development için Docker Hub kullanıyoruz
        DOCKER_REGISTRY = '${DOCKER_REGISTRY:-docker.io}'  // Docker Hub (default)
        DOCKER_REPO = 'incident-platform'
        
        // Kubernetes - Local/Development için
        KUBECONFIG = credentials('kubeconfig')
        
        // Code Quality - Opsiyonel
        SONAR_TOKEN = credentials('sonar-token')
        
        // Helm
        HELM_VERSION = '3.12.0'
        
        // ArgoCD - Development için local
        ARGOCD_SERVER = '${ARGOCD_SERVER:-localhost:8080}'  // Local ArgoCD
        
        // Git Repositories
        GIT_REPO = 'https://github.com/atakanzaa/incident-platform.git'
        GIT_CONFIG_REPO = '${GIT_CONFIG_REPO:-https://github.com/atakanzaa/incident-platform-config.git}'
        
        // Environment
        ENVIRONMENT = '${ENVIRONMENT:-development}'
        NAMESPACE = '${NAMESPACE:-incident-platform-dev}'
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 45, unit: 'MINUTES')
        skipStagesAfterUnstable()
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()
                    env.BUILD_VERSION = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                    env.DOCKER_TAG = "${env.BRANCH_NAME == 'main' ? 'latest' : env.BRANCH_NAME}-${env.BUILD_VERSION}"
                }
            }
        }
        
        stage('Build & Test') {
            parallel {
                stage('Maven Build') {
                    steps {
                        sh '''
                            echo "Building with Maven..."
                            mvn clean compile -DskipTests
                        '''
                    }
                }
                
                stage('AI Service Setup') {
                    steps {
                        dir('ai-service') {
                            sh '''
                                echo "Setting up Python environment..."
                                python -m venv venv
                                source venv/bin/activate || . venv/Scripts/activate
                                pip install -r requirements.txt
                            '''
                        }
                    }
                }
            }
        }
        
        stage('Unit Tests') {
            parallel {
                stage('Java Tests') {
                    steps {
                        sh '''
                            echo "Running Java unit tests..."
                            mvn test -Dtest=*UnitTest
                        '''
                    }
                    post {
                        always {
                            publishTestResults testResultsPattern: '**/target/surefire-reports/*.xml'
                            publishCoverage adapters: [jacoco()], sourceFileResolver: sourceFiles('STORE_LAST_BUILD')
                        }
                    }
                }
                
                stage('Python Tests') {
                    steps {
                        dir('ai-service') {
                            sh '''
                                source venv/bin/activate || . venv/Scripts/activate
                                python -m pytest tests/ --junitxml=test-results.xml --cov=. --cov-report=xml
                            '''
                        }
                    }
                    post {
                        always {
                            publishTestResults testResultsPattern: 'ai-service/test-results.xml'
                        }
                    }
                }
            }
        }
        
        stage('Integration Tests') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                    changeRequest()
                }
            }
            steps {
                sh '''
                    echo "Starting integration tests..."
                    docker-compose -f docker-compose.test.yml up -d
                    sleep 30
                    mvn verify -Pintegration-tests
                '''
            }
            post {
                always {
                    sh 'docker-compose -f docker-compose.test.yml down || true'
                    publishTestResults testResultsPattern: '**/target/failsafe-reports/*.xml'
                }
            }
        }
        
        stage('Code Quality') {
            parallel {
                stage('SonarQube Analysis') {
                    steps {
                        withSonarQubeEnv('SonarQube') {
                            sh '''
                                mvn sonar:sonar \
                                  -Dsonar.projectKey=incident-platform \
                                  -Dsonar.projectName=incident-platform \
                                  -Dsonar.token=${SONAR_TOKEN}
                            '''
                        }
                    }
                }
                
                stage('Security Scan') {
                    steps {
                        sh '''
                            echo "Running security scans..."
                            mvn org.owasp:dependency-check-maven:check
                        '''
                    }
                    post {
                        always {
                            publishHTML([
                                allowMissing: false,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: 'target',
                                reportFiles: 'dependency-check-report.html',
                                reportName: 'OWASP Dependency Check Report'
                            ])
                        }
                    }
                }
            }
        }
        
        stage('Build Docker Images') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            parallel {
                stage('Java Services') {
                    steps {
                        script {
                            def services = [
                                'config-server', 'discovery-server', 'gateway-service',
                                'auth-service', 'log-collector-service', 'anomaly-detector-service',
                                'alert-manager-service', 'notification-service', 'auto-responder-service',
                                'incident-tracker', 'dashboard-service'
                            ]
                            
                            services.each { service ->
                                sh """
                                    echo "Building ${service}..."
                                    cd ${service}
                                    mvn spring-boot:build-image \
                                      -Dspring-boot.build-image.imageName=${DOCKER_REGISTRY}/${DOCKER_REPO}/${service}:${DOCKER_TAG}
                                    docker push ${DOCKER_REGISTRY}/${DOCKER_REPO}/${service}:${DOCKER_TAG}
                                """
                            }
                        }
                    }
                }
                
                stage('AI Service') {
                    steps {
                        dir('ai-service') {
                            sh """
                                echo "Building AI service..."
                                docker build -t ${DOCKER_REGISTRY}/${DOCKER_REPO}/ai-service:${DOCKER_TAG} .
                                docker push ${DOCKER_REGISTRY}/${DOCKER_REPO}/ai-service:${DOCKER_TAG}
                            """
                        }
                    }
                }
            }
        }
        
        stage('Update Manifests') {
            when {
                branch 'main'
            }
            steps {
                script {
                    withCredentials([gitUsernamePassword(credentialsId: 'git-credentials')]) {
                        sh """
                            git clone ${GIT_CONFIG_REPO} config-repo
                            cd config-repo
                            
                            # Update image tags in Helm values or Kustomize
                            find . -name "values.yaml" -o -name "kustomization.yaml" | xargs sed -i 's|tag: .*|tag: ${DOCKER_TAG}|g'
                            
                            git config user.email "jenkins@incident-platform.com"
                            git config user.name "Jenkins CI"
                            git add .
                            git commit -m "Update image tags to ${DOCKER_TAG} [skip ci]" || true
                            git push origin main
                        """
                    }
                }
            }
        }
        
        stage('Deploy to Staging') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    sh """
                        echo "Deploying to staging environment..."
                        argocd app sync incident-platform-staging --server ${ARGOCD_SERVER}
                        argocd app wait incident-platform-staging --server ${ARGOCD_SERVER}
                    """
                }
            }
        }
        
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                script {
                    timeout(time: 5, unit: 'MINUTES') {
                        input message: 'Deploy to Production?', ok: 'Deploy'
                    }
                    
                    sh """
                        echo "Deploying to production environment..."
                        argocd app sync incident-platform-prod --server ${ARGOCD_SERVER}
                        argocd app wait incident-platform-prod --server ${ARGOCD_SERVER}
                    """
                }
            }
        }
        
        stage('Smoke Tests') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                sh '''
                    echo "Running smoke tests..."
                    sleep 60  # Wait for services to be ready
                    
                    # Test gateway health
                    curl -f http://gateway-service:8080/actuator/health || exit 1
                    
                    # Test key services
                    curl -f http://auth-service:8081/actuator/health || exit 1
                    curl -f http://dashboard-service:8088/actuator/health || exit 1
                '''
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            slackSend(
                channel: '#deployments',
                color: 'good',
                message: "✅ Pipeline succeeded for ${env.JOB_NAME} - ${env.BUILD_NUMBER} (${env.GIT_COMMIT_SHORT})"
            )
        }
        failure {
            slackSend(
                channel: '#deployments',
                color: 'danger',
                message: "❌ Pipeline failed for ${env.JOB_NAME} - ${env.BUILD_NUMBER} (${env.GIT_COMMIT_SHORT})"
            )
        }
    }
} 