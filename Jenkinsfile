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
                    echo "Verificando servicios..."

                    def hosts = [
                            env.API_BASE_URL,
                            "http://localhost:8080",
                            "http://host.docker.internal:8080"
                    ]

                    def effective = null

                    for (h in hosts) {
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

                    if (effective == null) {
                        error "No pude alcanzar la API en ninguno de los hosts probados. Asegúrate que esté arriba."
                    }

                    echo "API disponible en: ${effective}"
                    env.EFFECTIVE_API_BASE = effective
                }
            }
        }

        stage('Compile & Test') {
            steps {
                script {
                    // Pasamos tanto la env var (que TestConfig prioriza) como la system property
                    sh """
            set -e
            echo "Ejecutando tests con API_BASE=${EFFECTIVE_API_BASE}"
            mvn -B -s ci/settings.xml clean test \
              -Dapi.base.url='${EFFECTIVE_API_BASE}' \
              -Dkeycloak.url='${KEYCLOAK_BASE_URL}' \
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
