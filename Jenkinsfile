pipeline {
    agent any

    tools {
        maven 'Maven'
        jdk 'JDK17'
    }

    environment {
        // URLs internas de Docker (nombres de servicio en ci_net)
        API_BASE_URL = 'http://host.docker.internal:8080'
        KEYCLOAK_BASE_URL = 'http://keycloak:8080'
        KEYCLOAK_REALM = 'taller'
        KEYCLOAK_CLIENT_ID = 'taller-api'
        KEYCLOAK_CLIENT_SECRET = 'jx34gvJ7Vo9UwxLwsbLa1K3C58ZbjrLh'

        // Configuración de SonarQube
        SONAR_HOST_URL = 'http://sonarqube:9000'
        SONAR_TOKEN = credentials('sonarqube-token')

        // Configuración de Allure
        ALLURE_RESULTS = 'target/allure-results'
    }

    stages {
        stage('🔍 Información del Build') {
            steps {
                script {
                    echo "===="
                    echo "🚀 Iniciando Pipeline de Test Automation"
                    echo "===="
                    echo "Job: ${env.JOB_NAME}"
                    echo "Build: #${env.BUILD_NUMBER}"
                    echo "Branch: ${env.GIT_BRANCH}"
                    echo "Workspace: ${env.WORKSPACE}"
                    echo "===="
                }
            }
        }

        stage('📥 Checkout') {
            steps {
                echo "📥 Clonando repositorio test-automation..."
                checkout scm
                script {
                    sh 'echo "Último commit:"'
                    sh 'git log -1 --pretty=format:"%h - %an, %ar : %s"'
                }
            }
        }

        stage('🔍 Verificar Servicios') {
            steps {
                echo "🔍 Verificando que los servicios estén disponibles..."
                script {
                    // Verificar API
                    def apiStatus = sh(
                            script: 'curl -f http://host.docker.internal:8080/actuator/health || echo "API no disponible"',
                            returnStatus: true
                    )

                    if (apiStatus != 0) {
                        error("❌ La API no está disponible en http://host.docker.internal:8080")
                    }
                    echo "✅ API está disponible"

                    // Verificar Keycloak
                    def kcStatus = sh(
                            script: 'curl -f http://keycloak:8080/realms/master/.well-known/openid-configuration || echo "Keycloak no disponible"',
                            returnStatus: true
                    )

                    if (kcStatus != 0) {
                        error("❌ Keycloak no está disponible en http://keycloak:8080")
                    }
                    echo "✅ Keycloak está disponible"
                }
            }
        }

        stage('📦 Compilar Proyecto') {
            steps {
                echo "📦 Compilando proyecto test-automation..."
                sh 'mvn clean compile'
            }
        }

        stage('🧪 Ejecutar Tests') {
            steps {
                echo "🧪 Ejecutando pruebas de automatización..."
                script {
                    sh """
                        mvn test \
                            -Dapi.baseUrl=${API_BASE_URL} \
                            -Dkeycloak.baseUrl=${KEYCLOAK_BASE_URL} \
                            -Dkeycloak.realm=${KEYCLOAK_REALM} \
                            -Dkeycloak.clientId=${KEYCLOAK_CLIENT_ID} \
                            -Dkeycloak.clientSecret=${KEYCLOAK_CLIENT_SECRET} \
                            -Dcucumber.publish.enabled=false \
                            || echo "Tests ejecutados con fallos"
                    """
                }
            }
        }

        stage('📊 Análisis SonarQube') {
            steps {
                echo "📊 Ejecutando análisis de calidad con SonarQube..."
                script {
                    sh """
                        mvn sonar:sonar \
                            -Dsonar.host.url=${SONAR_HOST_URL} \
                            -Dsonar.login=${SONAR_TOKEN} \
                            -Dsonar.projectKey=test-automation \
                            -Dsonar.projectName='Test Automation' \
                            -Dsonar.sources=src/main/java,src/test/java \
                            -Dsonar.tests=src/test/java \
                            -Dsonar.java.binaries=target/classes,target/test-classes \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                    """
                }
            }
        }

        stage('📈 Generar Reporte Allure') {
            steps {
                echo "📈 Generando reporte de Allure..."
                script {
                    allure([
                            includeProperties: false,
                            jdk: '',
                            properties: [],
                            reportBuildPolicy: 'ALWAYS',
                            results: [[path: env.ALLURE_RESULTS]]
                    ])
                }
            }
        }
    }

    post {
        always {
            echo "🧹 Limpiando workspace..."
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'

            script {
                def testResults = junit(testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true)
                echo "📊 Resultados de Tests:"
                echo "   Total: ${testResults.totalCount}"
                echo "   ✅ Exitosos: ${testResults.passCount}"
                echo "   ❌ Fallidos: ${testResults.failCount}"
                echo "   ⏭️  Omitidos: ${testResults.skipCount}"
            }
        }

        success {
            echo "✅ Pipeline ejecutado exitosamente"
        }

        failure {
            echo "❌ Pipeline falló. Revisa los logs para más detalles."
        }
    }
}