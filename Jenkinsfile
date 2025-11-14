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
        KEYCLOAK_CLIENT_SECRET = ""

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

                    // Construimos el body del curl; sólo añadimos client_secret si está definido/no vacío
                    def curlBody = "-d 'grant_type=password' -d 'client_id=${KEYCLOAK_CLIENT_ID}' -d 'username=${ADMIN_USERNAME}' -d 'password=${ADMIN_PASSWORD}'"
                    if (env.KEYCLOAK_CLIENT_SECRET?.trim()) {
                        curlBody += " -d 'client_secret=${env.KEYCLOAK_CLIENT_SECRET}'"
                    }

                    def token = sh(
                            script: "curl -s -X POST ${tokenUrl} -H 'Content-Type: application/x-www-form-urlencoded' ${curlBody}",
                            returnStdout: true
                    ).trim()

                    echo "Respuesta Keycloak: ${token.take(300)}"
                }
            }
        }

        stage('Compile & Test') {
            steps {
                script {
                    // Construimos el comando mvn de forma dinámica para incluir el keycloak.client.secret sólo si existe
                    def mvnCmd = """mvn -B clean test \\
                      -Dapi.base.url="${API_BASE_URL}" \\
                      -Dkeycloak.url="${KEYCLOAK_BASE_URL}" """

                    if (env.KEYCLOAK_CLIENT_SECRET?.trim()) {
                        mvnCmd += " \\\n  -Dkeycloak.client.secret=\"${env.KEYCLOAK_CLIENT_SECRET}\""
                    }

                    // Ejecutamos mvn
                    sh """
                        set -e
                        echo "Ejecutando tests"
                        ${mvnCmd}
                    """
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
