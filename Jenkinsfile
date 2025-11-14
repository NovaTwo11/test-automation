pipeline {

    agent {
        docker {
            image 'my-ci/maven-git-docker:latest'
            args '--network app-network -v /var/run/docker.sock:/var/run/docker.sock -u root'
        }
    }

    environment {
        API_BASE_URL        = "http://taller-api-2:8080"
        KEYCLOAK_BASE_URL   = "http://keycloak:8080"
        KEYCLOAK_REALM      = "taller"
        KEYCLOAK_CLIENT_ID  = "taller-api"
        KEYCLOAK_CLIENT_SECRET = "jx34gvJ7Vo9UwxLwsbLa1K3C58ZbjLh"

        ADMIN_USERNAME = "admin"
        ADMIN_PASSWORD = "admin123"

        ALLURE_RESULTS = "target/allure-results"
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
                checkout scm
                sh 'git log -1 --pretty=format:"%h - %an, %ar : %s" || true'
            }
        }

        stage('Verify services') {
            steps {
                script {
                    echo "Verificando API y Keycloak..."

                    sh """
                        echo "PROBANDO API: ${API_BASE_URL}/actuator/health"
                        curl -f ${API_BASE_URL}/actuator/health
                    """

                    def tokenUrl = "${env.KEYCLOAK_BASE_URL}/realms/${env.KEYCLOAK_REALM}/protocol/openid-connect/token"

                    echo "Probando Keycloak token URL: ${tokenUrl}"

                    def token = sh(
                            script: """
                          curl -s -X POST ${tokenUrl} \
                            -H 'Content-Type: application/x-www-form-urlencoded' \
                            -d 'grant_type=password' \
                            -d 'client_id=${KEYCLOAK_CLIENT_ID}' \
                            -d 'client_secret=${KEYCLOAK_CLIENT_SECRET}' \
                            -d 'username=${ADMIN_USERNAME}' \
                            -d 'password=${ADMIN_PASSWORD}'
                        """,
                            returnStdout: true
                    ).trim()

                    echo "Respuesta Keycloak: ${token.take(300)}"
                }
            }
        }

        stage('Compile & Test') {
            steps {
                script {
                    sh '''
                        set -e
                        echo "Ejecutando tests"
                        mvn -B clean test \
                          -Dapi.base.url="$API_BASE_URL" \
                          -Dkeycloak.url="$KEYCLOAK_BASE_URL" \
                          -Dkeycloak.client.secret="$KEYCLOAK_CLIENT_SECRET"
                    '''
                }
            }
        }

        stage('Publish reports') {
            steps {
                junit 'target/cucumber-reports/*.xml'
                archiveArtifacts artifacts: 'target/allure-results/**', allowEmptyArchive: true
            }
        }
    }

    post {
        always { cleanWs() }
        success { echo "Pipeline finalizó OK 🎉" }
        failure { echo "Pipeline falló. Revisa logs y reports" }
    }
}
