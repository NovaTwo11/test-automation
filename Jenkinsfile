pipeline {
    agent any

    tools {
        maven 'Maven3'
        jdk 'JDK17'
    }

    environment {
        // URLs internas de Docker (nombres de servicio en ci_net)
        API_BASE_URL = 'http://host.docker.internal:8080'
        KEYCLOAK_BASE_URL = 'http://keycloak:8080'
        KEYCLOAK_REALM = 'taller'
        KEYCLOAK_CLIENT_ID = 'taller-api'
        KEYCLOAK_CLIENT_SECRET = 'jx34gvJ7Vo9UwxLwsbLa1K3C58ZbjrLh'

        // Configuración de SonarQube (sin credenciales por ahora)
        SONAR_HOST_URL = 'http://sonarqube:9000'

        // Configuración de Allure
        ALLURE_RESULTS = 'target/allure-results'
    }

    stages {
        stage('🔍 Información del Build') {
            steps {
                script {
                    echo "===================================="
                    echo "🚀 Iniciando Pipeline de Test Automation"
                    echo "===================================="
                    echo "Job: ${env.JOB_NAME}"
                    echo "Build: #${env.BUILD_NUMBER}"
                    echo "Branch: ${env.GIT_BRANCH ?: 'N/A'}"
                    echo "Workspace: ${env.WORKSPACE}"
                    echo "===================================="
                }
            }
        }

        stage('📥 Checkout') {
            steps {
                echo "📥 Clonando repositorio test-automation..."
                checkout scm
                script {
                    sh 'echo "Último commit:"'
                    sh 'git log -1 --pretty=format:"%h - %an, %ar : %s" || echo "No git history"'
                }
            }
        }

        stage('🔍 Verificar Servicios') {
            steps {
                echo "🔍 Verificando que los servicios estén disponibles..."
                script {
                    // Verificar API
                    echo "Verificando API en ${API_BASE_URL}..."
                    def apiStatus = sh(
                            script: "curl -f ${API_BASE_URL}/actuator/health || echo 'API no disponible'",
                            returnStatus: true
                    )

                    if (apiStatus != 0) {
                        echo "⚠️ ADVERTENCIA: La API no está disponible en ${API_BASE_URL}"
                        echo "Asegúrate de que taller-api-2 esté corriendo en el puerto 8080"
                    } else {
                        echo "✅ API está disponible"
                    }

                    // Verificar Keycloak
                    echo "Verificando Keycloak en ${KEYCLOAK_BASE_URL}..."
                    def kcStatus = sh(
                            script: "curl -f ${KEYCLOAK_BASE_URL}/realms/master/.well-known/openid-configuration || echo 'Keycloak no disponible'",
                            returnStatus: true
                    )

                    if (kcStatus != 0) {
                        echo "⚠️ ADVERTENCIA: Keycloak no está disponible en ${KEYCLOAK_BASE_URL}"
                    } else {
                        echo "✅ Keycloak está disponible"
                    }
                }
            }
        }

        stage('📦 Compilar Proyecto') {
            steps {
                echo "📦 Compilando proyecto test-automation..."
                sh 'mvn clean compile -DskipTests'
            }
        }

        stage('🧪 Ejecutar Tests') {
            steps {
                echo "🧪 Ejecutando pruebas de automatización..."
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
                        echo "⚠️ Algunos tests fallaron, pero continuamos el pipeline"
                        currentBuild.result = 'UNSTABLE'
                    } else {
                        echo "✅ Todos los tests pasaron exitosamente"
                    }
                }
            }
        }

        stage('📊 Análisis SonarQube') {
            steps {
                echo "📊 Ejecutando análisis de calidad con SonarQube..."
                script {
                    // Análisis sin autenticación (para desarrollo)
                    def sonarStatus = sh(
                            script: """
                            mvn sonar:sonar \
                                -Dsonar.host.url=${SONAR_HOST_URL} \
                                -Dsonar.projectKey=test-automation \
                                -Dsonar.projectName='Test Automation' \
                                -Dsonar.sources=src/main/java,src/test/java \
                                -Dsonar.tests=src/test/java \
                                -Dsonar.java.binaries=target/classes,target/test-classes
                        """,
                            returnStatus: true
                    )

                    if (sonarStatus != 0) {
                        echo "⚠️ Análisis de SonarQube falló, pero continuamos"
                        currentBuild.result = 'UNSTABLE'
                    } else {
                        echo "✅ Análisis de SonarQube completado"
                        echo "📊 Ver resultados en: ${SONAR_HOST_URL}/dashboard?id=test-automation"
                    }
                }
            }
        }

        stage('📈 Generar Reporte Allure') {
            steps {
                echo "📈 Generando reporte de Allure..."
                script {
                    // Verificar si existen resultados de Allure
                    def allureExists = sh(
                            script: "test -d ${ALLURE_RESULTS} && echo 'exists' || echo 'not found'",
                            returnStdout: true
                    ).trim()

                    if (allureExists == 'exists') {
                        allure([
                                includeProperties: false,
                                jdk: '',
                                properties: [],
                                reportBuildPolicy: 'ALWAYS',
                                results: [[path: env.ALLURE_RESULTS]]
                        ])
                        echo "✅ Reporte Allure generado"
                    } else {
                        echo "⚠️ No se encontraron resultados de Allure en ${ALLURE_RESULTS}"
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "🧹 Limpiando y generando reportes..."

                // Publicar resultados de JUnit
                def junitFiles = findFiles(glob: '**/target/surefire-reports/*.xml')
                if (junitFiles.length > 0) {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                    echo "✅ Reportes JUnit publicados"
                } else {
                    echo "⚠️ No se encontraron reportes JUnit"
                }

                // Resumen de resultados
                echo "===================================="
                echo "📊 RESUMEN DEL BUILD"
                echo "===================================="
                echo "Estado: ${currentBuild.result ?: 'SUCCESS'}"
                echo "Duración: ${currentBuild.durationString}"
                echo "===================================="
            }
        }

        success {
            echo "✅ ¡Pipeline ejecutado exitosamente!"
        }

        unstable {
            echo "⚠️ Pipeline completado con advertencias"
        }

        failure {
            echo "❌ Pipeline falló. Revisa los logs para más detalles."
        }
    }
}