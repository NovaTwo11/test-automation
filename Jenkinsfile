pipeline {
    agent {
        docker {
            image 'my-ci/maven-git-docker:latest' // construye con Dockerfile.ci abajo
            args '--network app-network -v /var/run/docker.sock:/var/run/docker.sock -u root'
        }
    }

    options {
        skipDefaultCheckout()
        // controla history, timeout, etc si quieres:
        timeout(time: 45, unit: 'MINUTES')
    }

    environment {
        // Ajusta si tu infra usa otras URLs. Por defecto usamos host.docker.internal para reach host services.
        API_BASE_URL = "${env.API_BASE_URL ?: 'http://host.docker.internal:8080'}"
        KEYCLOAK_BASE_URL = "${env.KEYCLOAK_BASE_URL ?: 'http://host.docker.internal:8082'}"
        KEYCLOAK_REALM = 'taller'
        KEYCLOAK_CLIENT_ID = 'taller-api'
        KEYCLOAK_CLIENT_SECRET = 'jx34gvJ7Vo9UwxLwsbLa1K3C58ZbjLh'

        // Allure / Reports
        ALLURE_RESULTS = 'target/allure-results'
    }

    stages {

        stage('Info') {
            steps {
                echo "Job: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
                echo "API_BASE_URL = ${env.API_BASE_URL}"
                echo "KEYCLOAK_BASE_URL = ${env.KEYCLOAK_BASE_URL}"
            }
        }

        stage('Checkout') {
            steps {
                echo "Checkout dentro del contenedor (evita JENKINS-30600)..."
                checkout scm
                sh 'git log -1 --pretty=format:"%h - %an, %ar : %s" || true'
            }
        }

        stage('Verify services') {
            steps {
                script {
                    echo "Verificando API y Keycloak..."

                    // API
                    def apiOk = sh(script: "curl -fsS --max-time 5 ${EFFECTIVE_API_BASE}/actuator/health >/dev/null 2>&1 || echo 'FAIL'", returnStdout: true).trim()
                    if (apiOk == 'FAIL') {
                        error "API no disponible en ${EFFECTIVE_API_BASE}/actuator/health"
                    }
                    echo "API OK en ${EFFECTIVE_API_BASE}"

                    // Keycloak: intentamos pedir token admin (grant_type=password) para validar que realm+cliente+credenciales están OK
                    def keycloakTokenUrl = "${env.KEYCLOAK_BASE_URL}/realms/${env.KEYCLOAK_REALM}/protocol/openid-connect/token"
                    echo "Probando Keycloak token URL: ${keycloakTokenUrl}"

                    // Intento rápido de token (no fallará pipeline si Keycloak no está—capturamos resultado)
                    def cmd = "curl -s -X POST ${keycloakTokenUrl} -H 'Content-Type: application/x-www-form-urlencoded' " +
                            "-d 'grant_type=password' -d 'client_id=${env.KEYCLOAK_CLIENT_ID}' -d 'username=${env.ADMIN_USERNAME ?: 'admin'}' -d 'password=${env.ADMIN_PASSWORD ?: 'admin123'}' --max-time 8"

                    def tokenResp = sh(script: cmd + " || true", returnStdout: true).trim()
                    if (tokenResp == null || tokenResp.isEmpty()) {
                        echo "WARN: No se obtuvo respuesta desde Keycloak token endpoint (posible caída o red)."
                    } else if (tokenResp.contains('access_token')) {
                        echo "Keycloak token endpoint OK (se obtuvo access_token)."
                    } else {
                        echo "WARN: Keycloak token endpoint respondió pero no produjo token. Body: ${tokenResp.take(400)}"
                    }
                }
            }
        }


        stage('Compile & Test') {
            steps {
                script {
                    sh """
                        set -e
                        echo "Ejecutando tests con API_BASE=${EFFECTIVE_API_BASE}"
                        mvn -B clean test \
                          -Dapi.base.url="${EFFECTIVE_API_BASE}" \
                          -Dkeycloak.url="${KEYCLOAK_BASE_URL}" \
                          -Dskip.integration.tests=true
                      """
                }
            }
        }


        stage('Publish reports') {
            steps {
                script {
                    // Publicar JUnit / Cucumber reports
                    junit 'target/cucumber-reports/*.xml'
                    // Archiva resultados de Allure
                    archiveArtifacts artifacts: 'target/allure-results/**', allowEmptyArchive: true
                }
            }
        }
    }

    post {
        always {
            echo 'Post: limpiar workspace'
            cleanWs()
        }
        success {
            echo 'Pipeline finalizó OK 🎉'
        }
        failure {
            echo 'Pipeline falló. Revisa logs y reports en target/'
        }
    }
}
