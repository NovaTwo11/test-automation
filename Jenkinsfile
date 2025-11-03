pipeline {
    agent any

    tools {
        maven 'Maven3'  // ← CORREGIDO
        jdk 'JDK17'
    }

    environment {
        // URLs internas de Docker
        API_BASE_URL = 'http://host.docker.internal:8080'
        KEYCLOAK_BASE_URL = 'http://keycloak:8080'
        KEYCLOAK_REALM = 'taller'  // ← CORREGIDO
        KEYCLOAK_CLIENT_ID = 'taller-api'  // ← CORREGIDO
        KEYCLOAK_CLIENT_SECRET = 'jx34gvJ7Vo9UwxLwsbLa1K3C58ZbjrLh'  // ← CORREGIDO

        // Configuración de SonarQube
        SONAR_HOST_URL = 'http://sonarqube:9000'

        // Configuración de Allure
        ALLURE_RESULTS = 'target/allure-results'
    }

    stages {
        stage('🔍 Información del Build') {
            steps {
                script {
                    echo "===================================="
                    echo "🚀 Pipeline Test Automation"
                    echo "===================================="
                    echo "Job: ${env.JOB_NAME}"
                    echo "Build: #${env.BUILD_NUMBER}"
                    echo "Branch: ${env.GIT_BRANCH ?: 'N/A'}"
                    echo "===================================="
                }
            }
        }

        stage('📥 Checkout') {
            steps {
                echo "📥 Clonando repositorio..."
                checkout scm
                script {
                    sh 'git log -1 --pretty=format:"%h - %an, %ar : %s" || echo "No git history"'
                }
            }
        }

        stage('🔍 Verificar Servicios') {
            steps {
                echo "🔍 Verificando servicios..."
                script {
                    // Verificar API
                    echo "→ Verificando API en ${API_BASE_URL}..."
                    def apiStatus = sh(
                            script: "curl -f ${API_BASE_URL}/actuator/health 2>&1",
                            returnStatus: true
                    )

                    if (apiStatus != 0) {
                        error("❌ API no disponible en ${API_BASE_URL}. Asegúrate de que taller-api-2 esté corriendo.")
                    }
                    echo "✅ API disponible"

                    // Verificar Keycloak (usando el realm correcto)
                    echo "→ Verificando Keycloak en ${KEYCLOAK_BASE_URL}..."
                    def kcStatus = sh(
                            script: "curl -f ${KEYCLOAK_BASE_URL}/realms/${KEYCLOAK_REALM}/.well-known/openid-configuration 2>&1",
                            returnStatus: true
                    )

                    if (kcStatus != 0) {
                        error("❌ Keycloak realm '${KEYCLOAK_REALM}' no disponible. Verifica la configuración.")
                    }
                    echo "✅ Keycloak disponible (realm: ${KEYCLOAK_REALM})"
                }
            }
        }

        stage('📦 Compilar Proyecto') {
            steps {
                echo "📦 Compilando proyecto..."
                sh 'mvn clean compile -DskipTests'
            }
        }

        stage('🧪 Ejecutar Tests') {
            steps {
                echo "🧪 Ejecutando tests..."
                script {
                    def testStatus = sh(
                            script: """
                            mvn test \
                                -Dapi.baseUrl=${API_BASE_URL} \
                                -Dkeycloak.baseUrl=${KEYCLOAK_BASE_URL} \
                                -Dkeycloak.realm=${KEYCLOAK_REALM} \
                                -Dkeycloak.clientId=${KEYCLOAK_CLIENT_ID} \
                                -Dkeycloak.clientSecret=${KEYCLOAK_CLIENT_SECRET} \
                                -Dcucumber.publish.enabled=false
                        """,
                            returnStatus: true
                    )

                    if (testStatus != 0) {
                        echo "⚠️ Algunos tests fallaron"
                        currentBuild.result = 'UNSTABLE'
                    } else {
                        echo "✅ Todos los tests pasaron"
                    }
                }
            }
        }

        stage('📊 Análisis SonarQube') {
            steps {
                echo '📊 Análisis de calidad...'
                script {
                    try {
                        sh """
                    mvn sonar:sonar \
                        -Dsonar.host.url=http://sonarqube:9000 \
                        -Dsonar.login=598c2e3b2c6ab5065c130cd707475f10 \
                        -Dsonar.projectKey=test-automation \
                        -Dsonar.projectName="Test Automation" \
                        -Dsonar.sources=src/main/java,src/test/java \
                        -Dsonar.tests=src/test/java \
                        -Dsonar.java.binaries=target/classes,target/test-classes
                """
                        echo '✅ Análisis de SonarQube completado'
                    } catch (Exception e) {
                        echo '⚠️ Análisis de SonarQube falló'
                    }
                }
            }
        }

        stage('📈 Generar Reporte Allure') {
            steps {
                echo "📈 Generando reporte Allure..."
                script {
                    def allureExists = fileExists(env.ALLURE_RESULTS)

                    if (allureExists) {
                        allure([
                                includeProperties: false,
                                jdk: '',
                                properties: [],
                                reportBuildPolicy: 'ALWAYS',
                                results: [[path: env.ALLURE_RESULTS]]
                        ])
                        echo "✅ Reporte Allure generado"
                    } else {
                        echo "⚠️ No se encontraron resultados de Allure"
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "===================================="
                echo "📊 RESUMEN"
                echo "===================================="

                // Publicar resultados JUnit
                if (fileExists('target/surefire-reports')) {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                    echo "✅ Reportes JUnit publicados"
                } else {
                    echo "⚠️ No se encontraron reportes JUnit"
                }

                echo "Estado: ${currentBuild.result ?: 'SUCCESS'}"
                echo "Duración: ${currentBuild.durationString}"
                echo "===================================="
            }
        }

        success {
            echo "✅ Pipeline completado exitosamente"
        }

        unstable {
            echo "⚠️ Pipeline completado con advertencias"
        }

        failure {
            echo "❌ Pipeline falló"
        }
    }
}