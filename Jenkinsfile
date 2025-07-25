pipeline {
    agent any
    
    tools {
        maven 'Maven-3.9'
        jdk 'OpenJDK-21'
    }
    
    environment {
        // Docker Registry Configuration
        DOCKER_REGISTRY = '${DOCKER_REGISTRY:-your-registry.com}'
        DOCKER_REPO = 'incident-platform'
        
        // Environment Detection
        ENVIRONMENT = getEnvironment()
        TARGET_NAMESPACE = getTargetNamespace()
        
        // Kubernetes Configuration
        KUBECONFIG = credentials('kubeconfig')
        
        // Code Quality
        SONAR_TOKEN = credentials('sonar-token')
        
        // Helm
        HELM_VERSION = '3.12.0'
        
        // ArgoCD Configuration
        ARGOCD_SERVER = getArgocdServer()
        ARGOCD_CREDENTIALS = credentials('argocd-credentials')
        
        // Git Repositories
        GIT_REPO = 'https://github.com/atakanzaa/incident-platform.git'
        GIT_CONFIG_REPO = 'https://github.com/atakanzaa/incident-platform-config.git'
        
        // Slack Notifications
        SLACK_CHANNEL = getSlackChannel()
    }
    
    options {
        buildDiscarder(logRotator(
            numToKeepStr: env.BRANCH_NAME == 'main' ? '20' : '10'
        ))
        timeout(time: 60, unit: 'MINUTES')
        skipStagesAfterUnstable()
        timestamps()
    }
    
    stages {
        stage('Initialization') {
            steps {
                script {
                    echo "🚀 Starting pipeline for ${env.ENVIRONMENT} environment"
                    echo "📋 Branch: ${env.BRANCH_NAME}"
                    echo "🎯 Target Namespace: ${env.TARGET_NAMESPACE}"
                    
                    env.GIT_COMMIT_SHORT = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()
                    
                    env.BUILD_VERSION = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                    env.DOCKER_TAG = getDockerTag()
                    
                    echo "🏷️ Docker Tag: ${env.DOCKER_TAG}"
                    
                    // Set Spring Profile based on environment
                    env.SPRING_PROFILES_ACTIVE = env.ENVIRONMENT
                }
            }
        }
        
        stage('Checkout & Validation') {
            steps {
                checkout scm
                script {
                    // Validate environment configuration
                    sh '''
                        echo "Validating environment configuration..."
                        if [ ! -f "environments/${ENVIRONMENT}/values.yaml" ]; then
                            echo "❌ Environment configuration not found for ${ENVIRONMENT}"
                            exit 1
                        fi
                        echo "✅ Environment configuration validated"
                    '''
                }
            }
        }
        
        stage('Build & Test') {
            parallel {
                stage('Maven Build') {
                    steps {
                        sh '''
                            echo "🔨 Building with Maven..."
                            mvn clean compile -DskipTests -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}
                        '''
                    }
                }
                
                stage('AI Service Setup') {
                    steps {
                        dir('ai-service') {
                            sh '''
                                echo "🐍 Setting up Python environment..."
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
                            echo "🧪 Running Java unit tests..."
                            mvn test -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}
                        '''
                    }
                    post {
                        always {
                            junit '**/target/surefire-reports/*.xml'
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
                            junit 'ai-service/test-results.xml'
                        }
                    }
                }
            }
        }
        
        stage('Integration Tests') {
            when {
                not { environment name: 'ENVIRONMENT', value: 'development' }
            }
            steps {
                sh '''
                    echo "🔗 Starting integration tests..."
                    docker-compose -f docker-compose.${ENVIRONMENT}.yml up -d
                    sleep 60
                    mvn verify -Pintegration-tests -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}
                '''
            }
            post {
                always {
                    sh 'docker-compose -f docker-compose.${ENVIRONMENT}.yml down || true'
                    junit '**/target/failsafe-reports/*.xml'
                }
            }
        }
        
        stage('Code Quality & Security') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                    changeRequest()
                }
            }
            parallel {
                stage('SonarQube Analysis') {
                    steps {
                        withSonarQubeEnv('SonarQube') {
                            sh '''
                                mvn sonar:sonar \
                                  -Dsonar.projectKey=incident-platform-${ENVIRONMENT} \
                                  -Dsonar.projectName="Incident Platform (${ENVIRONMENT})" \
                                  -Dsonar.token=${SONAR_TOKEN}
                            '''
                        }
                    }
                }
                
                stage('Security Scan') {
                    steps {
                        sh '''
                            echo "🔒 Running security scans..."
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
                
                stage('Container Scan') {
                    when {
                        not { environment name: 'ENVIRONMENT', value: 'development' }
                    }
                    steps {
                        sh '''
                            echo "🐳 Running container security scan..."
                            # Add container security scanning tool like Trivy
                            trivy fs --security-checks vuln,config . || true
                        '''
                    }
                }
            }
        }
        
        stage('Build Docker Images') {
            when {
                anyOf {
                    branch 'main'
                    branch 'staging'
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
                                    echo "🔨 Building ${service}..."
                                    cd ${service}
                                    mvn spring-boot:build-image \
                                      -Dspring-boot.build-image.imageName=${DOCKER_REGISTRY}/${DOCKER_REPO}/${service}:${DOCKER_TAG} \
                                      -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}
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
                                echo "🤖 Building AI service..."
                                docker build -t ${DOCKER_REGISTRY}/${DOCKER_REPO}/ai-service:${DOCKER_TAG} \
                                  --build-arg ENVIRONMENT=${ENVIRONMENT} .
                                docker push ${DOCKER_REGISTRY}/${DOCKER_REPO}/ai-service:${DOCKER_TAG}
                            """
                        }
                    }
                }
            }
            post {
                success {
                    echo "✅ Docker images built and pushed successfully"
                }
                failure {
                    echo "❌ Failed to build or push Docker images"
                }
            }
        }
        
        stage('Deploy to Development') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    echo "🚀 Deploying to Development environment..."
                    deployToEnvironment('development', false)
                }
            }
        }
        
        stage('Deploy to Staging') {
            when {
                branch 'staging'
            }
            steps {
                script {
                    echo "🚀 Deploying to Staging environment..."
                    
                    // Update configuration repository
                    updateConfigRepository('staging')
                    
                    // Deploy via ArgoCD
                    deployToEnvironment('staging', false)
                    
                    // Run staging-specific tests
                    runStagingTests()
                }
            }
        }
        
        stage('Production Approval') {
            when {
                branch 'main'
            }
            steps {
                script {
                    echo "⏳ Waiting for production deployment approval..."
                    timeout(time: 24, unit: 'HOURS') {
                        def approvers = ['production-team@company.com', 'devops-lead@company.com']
                        input message: 'Deploy to Production?', 
                              ok: 'Deploy to Production',
                              submitter: approvers.join(','),
                              submitterParameter: 'APPROVER'
                    }
                    echo "✅ Production deployment approved by: ${env.APPROVER}"
                }
            }
        }
        
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                script {
                    echo "🚀 Deploying to Production environment..."
                    
                    // Backup current production state
                    backupProduction()
                    
                    // Update configuration repository
                    updateConfigRepository('production')
                    
                    // Deploy via ArgoCD with manual sync
                    deployToEnvironment('production', true)
                    
                    // Run production smoke tests
                    runProductionSmokeTests()
                }
            }
        }
        
        stage('Post-Deployment Tests') {
            when {
                anyOf {
                    branch 'main'
                    branch 'staging'
                    branch 'develop'
                }
            }
            steps {
                script {
                    echo "🧪 Running post-deployment tests..."
                    runPostDeploymentTests()
                }
            }
        }
    }
    
    post {
        always {
            junit '**/target/surefire-reports/*.xml'
            junit 'ai-service/test-results.xml'
            junit '**/target/failsafe-reports/*.xml'
            archiveArtifacts artifacts: 'target/*.jar, ai-service/dist/*', allowEmptyArchive: true
            cleanWs()
        }
        success {
            script {
                def message = "✅ Pipeline succeeded for ${env.ENVIRONMENT}"
                message += "\n📋 Job: ${env.JOB_NAME} - ${env.BUILD_NUMBER}"
                message += "\n🏷️ Version: ${env.DOCKER_TAG}"
                message += "\n👤 Triggered by: ${env.BUILD_USER ?: 'System'}"
                // slackSend(
                //     channel: env.SLACK_CHANNEL,
                //     color: 'good',
                //     message: message
                // )
            }
        }
        failure {
            script {
                def message = "❌ Pipeline failed for ${env.ENVIRONMENT}"
                message += "\n📋 Job: ${env.JOB_NAME} - ${env.BUILD_NUMBER}"
                message += "\n🔗 Build URL: ${env.BUILD_URL}"
                // slackSend(
                //     channel: env.SLACK_CHANNEL,
                //     color: 'danger',
                //     message: message
                // )
            }
        }
        unstable {
            echo "Pipeline unstable!"
            // slackSend(
            //     channel: env.SLACK_CHANNEL,
            //     color: 'warning',
            //     message: "⚠️ Pipeline unstable for ${env.ENVIRONMENT} - ${env.JOB_NAME} - ${env.BUILD_NUMBER}"
            // )
        }
    }
}

// Helper Functions
def getEnvironment() {
    switch(env.BRANCH_NAME) {
        case 'main':
            return 'production'
        case 'staging':
            return 'staging'
        case 'develop':
            return 'development'
        default:
            return 'development'
    }
}

def getTargetNamespace() {
    switch(getEnvironment()) {
        case 'production':
            return 'incident-platform-prod'
        case 'staging':
            return 'incident-platform-staging'
        case 'development':
            return 'incident-platform-dev'
        default:
            return 'incident-platform-dev'
    }
}

def getDockerTag() {
    def environment = getEnvironment()
    switch(environment) {
        case 'production':
            return "main-${env.BUILD_VERSION}"
        case 'staging':
            return "develop-${env.BUILD_VERSION}"
        case 'development':
            return "dev-${env.BUILD_VERSION}"
        default:
            return "${env.BRANCH_NAME}-${env.BUILD_VERSION}"
    }
}

def getArgocdServer() {
    switch(getEnvironment()) {
        case 'production':
            return 'argocd-prod.your-domain.com'
        case 'staging':
            return 'argocd-staging.your-domain.com'
        case 'development':
            return 'localhost:8080'
        default:
            return 'localhost:8080'
    }
}

def getSlackChannel() {
    switch(getEnvironment()) {
        case 'production':
            return '#production-alerts'
        case 'staging':
            return '#staging-deployments'
        case 'development':
            return '#dev-builds'
        default:
            return '#dev-builds'
    }
}

def updateConfigRepository(environment) {
    withCredentials([gitUsernamePassword(credentialsId: 'git-credentials')]) {
        sh """
            echo "📝 Updating configuration repository for ${environment}..."
            git clone ${GIT_CONFIG_REPO} config-repo
            cd config-repo
            
            # Update image tags in Helm values
            sed -i 's|imageTag: .*|imageTag: "${env.DOCKER_TAG}"|g' environments/${environment}/values.yaml
            
            git config user.email "jenkins@incident-platform.com"
            git config user.name "Jenkins CI"
            git add .
            git commit -m "Update ${environment} image tags to ${env.DOCKER_TAG} [skip ci]" || true
            git push origin main
        """
    }
}

def deployToEnvironment(environment, requiresApproval) {
    def appName = "incident-platform-${environment == 'production' ? 'prod' : environment}"
    
    if (requiresApproval) {
        sh """
            echo "🎯 Syncing ArgoCD application: ${appName}"
            argocd app sync ${appName} --server ${env.ARGOCD_SERVER} --auth-token ${env.ARGOCD_CREDENTIALS} --grpc-web
            argocd app wait ${appName} --server ${env.ARGOCD_SERVER} --auth-token ${env.ARGOCD_CREDENTIALS} --timeout 600
        """
    } else {
        sh """
            echo "🎯 Auto-syncing ArgoCD application: ${appName}"
            argocd app sync ${appName} --server ${env.ARGOCD_SERVER} --auth-token ${env.ARGOCD_CREDENTIALS} --grpc-web
            argocd app wait ${appName} --server ${env.ARGOCD_SERVER} --auth-token ${env.ARGOCD_CREDENTIALS} --timeout 300
        """
    }
}

def backupProduction() {
    sh '''
        echo "💾 Creating production backup..."
        kubectl create backup production-backup-$(date +%Y%m%d-%H%M%S) \
          --namespace incident-platform-prod \
          --include-resources deployments,services,configmaps,secrets || true
    '''
}

def runStagingTests() {
    sh '''
        echo "🧪 Running staging-specific tests..."
        sleep 120  # Wait for services to be ready
        
        # Test all service health endpoints
        for service in gateway auth log-collector anomaly-detector alert-manager notification auto-responder incident-tracker dashboard; do
            echo "Testing ${service}-service health..."
            curl -f http://${service}-service.incident-platform-staging:8080/actuator/health || exit 1
        done
        
        # Run staging end-to-end tests
        mvn test -Pstaging-e2e-tests
    '''
}

def runProductionSmokeTests() {
    sh '''
        echo "🧪 Running production smoke tests..."
        sleep 180  # Wait for services to be ready
        
        # Critical service health checks
        curl -f https://incident-platform.your-domain.com/actuator/health || exit 1
        curl -f https://incident-platform.your-domain.com/api/auth/health || exit 1
        
        # Performance tests
        mvn test -Pperformance-tests -Dtest.environment=production
    '''
}

def runPostDeploymentTests() {
    def environment = getEnvironment()
    sh """
        echo "🧪 Running post-deployment tests for ${environment}..."
        
        # Wait for services to be ready
        sleep 60
        
        # Run environment-specific tests
        mvn test -P${environment}-post-deployment-tests
    """
} 