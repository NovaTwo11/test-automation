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

                    def hosts = [
                            env.API_BASE_URL,
                            "http://localhost:8080",
                            "http://host.docker.internal:8080"
                    ]

                    def effective = null

                    for (h in hosts) {
                        if (h == null) continue
                        echo "Probando: ${h}/actuator/health"
                        def status = sh(
                                script: "curl -fsS --max-time 5 ${h}/actuator/health >/dev/null 2>&1",
                                returnStatus: true
                        )
                        if (status == 0) {
                            effective = h
                            break
                        }
                    }

                    // fallback: si ninguno responde, usa API_BASE_URL (aunque no responda)
                    if (effective == null) {
                        echo "WARN: Ningún host respondió; usando valor por defecto env.API_BASE_URL = ${env.API_BASE_URL}"
                        effective = env.API_BASE_URL ?: "http://localhost:8080"
                    }

                    // IMPORTANTE: asigna explicitamente a env para que esté disponible en siguientes stages
                    env.EFFECTIVE_API_BASE = effective
                    echo "API disponible/seleccionada: ${env.EFFECTIVE_API_BASE}"

                    // Keycloak quick check (no crítica) - intenta token y lo registra
                    def keycloakTokenUrl = "${env.KEYCLOAK_BASE_URL}/realms/${env.KEYCLOAK_REALM}/protocol/openid-connect/token"
                    echo "Comprobando Keycloak token endpoint: ${keycloakTokenUrl}"
                    def tokenResp = sh(script: "curl -sS --max-time 8 -X POST ${keycloakTokenUrl} -H 'Content-Type: application/x-www-form-urlencoded' -d 'grant_type=password' -d 'client_id=${env.KEYCLOAK_CLIENT_ID}' -d 'username=${env.ADMIN_USERNAME ?: 'admin'}' -d 'password=${env.ADMIN_PASSWORD ?: 'admin123'}' || true", returnStdout: true).trim()
                    if (tokenResp) {
                        if (tokenResp.contains('access_token')) {
                            echo "Keycloak OK: token obtenido."
                        } else {
                            echo "WARN: Keycloak respondió pero no devolvió token (respuesta truncada): ${tokenResp.take(300)}"
                        }
                    } else {
                        echo "WARN: No se obtuvo respuesta desde Keycloak token endpoint (o request falló)."
                    }
                }
            }
        }

        stage('Compile & Test') {
            steps {
                script {
                    // Usamos triple-single-quote para que Groovy NO interpole variables dentro del block,
                    // y dejamos que el shell expanda $EFFECTIVE_API_BASE y $KEYCLOAK_BASE_URL.
                    sh '''
                    set -e
                    echo "Ejecutando tests con API_BASE=$EFFECTIVE_API_BASE"
                    mvn -B clean test \
                      -Dapi.base.url="$EFFECTIVE_API_BASE" \
                      -Dkeycloak.url="$KEYCLOAK_BASE_URL" \
                      -Dskip.integration.tests=true
                  '''
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
