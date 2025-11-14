// Jenkinsfile para test-automation
pipeline {
    agent {
        // Usa un agente Docker con Maven y JDK 17
        docker {
            image 'maven:3.9-eclipse-temurin-17'
        }
    }

    environment {
        // URLs internas de Docker (asumiendo que Jenkins se ejecuta en la misma red 'app-network')
        // Si Jenkins se ejecuta fuera de Docker, cambia a 'localhost'
        API_BASE_URL = 'http://taller-api-2:8080'
        KEYCLOAK_BASE_URL = 'http://keycloak:8080'
        KEYCLOAK_REALM = 'taller'
        KEYCLOAK_CLIENT_ID = 'taller-api'
        KEYCLOAK_CLIENT_SECRET = 'jx34gvJ7Vo9UwxLwsbLa1K3C58ZbjrLh'
        SONAR_HOST_URL = 'http://sonarqube:9000'
        ALLURE_RESULTS = 'target/allure-results'
    }

    stages {
        stage('Checkout') {
            steps {
                echo "📥 Clonando repositorio..."
                checkout scm
            }
        }

        stage('Verify Services') {
            steps {
                echo "🔍 Verificando servicios de la API..."
                // Este script ahora corre DENTRO del contenedor de Maven
                // Asegúrate que este contenedor puede ver 'taller-api-2' y 'keycloak' por DNS
                sh "curl -f ${API_BASE_URL}/actuator/health"
                sh "curl -f ${KEYCLOAK_BASE_URL}/realms/${KEYCLOAK_REALM}/.well-known/openid-configuration"
                echo "✅ Servicios disponibles"
            }
        }

        stage('Compile Project') {
            steps {
                echo "📦 Compilando tests..."
                sh 'mvn clean compile -DskipTests'
            }
        }

        stage('Run Tests') {
            steps {
                echo "🧪 Ejecutando tests de Cucumber..."
                script {
                    def testStatus = sh(
                            script: """
                        mvn test \
                            -Dapi.base.url=${API_BASE_URL} \
                            -Dkeycloak.url=${KEYCLOAK_BASE_URL} \
                            -Dkeycloak.realm=${KEYCLOAK_REALM} \
                            -Dkeycloak.client.id=${KEYCLOAK_CLIENT_ID} \
                            -Dkeycloak.client.secret=${KEYCLOAK_CLIENT_SECRET} \
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

        stage('SonarQube Analysis') {
            // Este stage es de tu pipeline original[cite: 626], está bien
            steps {
                echo '📊 Análisis de calidad...'
                sh """
                mvn sonar:sonar \
                    -Dsonar.host.url=${SONAR_HOST_URL} \
                    -Dsonar.login=${env.SONAR_AUTH_TOKEN} \
                    -Dsonar.projectKey=test-automation \
                    -Dsonar.projectName="Test Automation" \
                    -Dsonar.sources=src/test/java \
                    -Dsonar.java.binaries=target/test-classes
                """
            }
        }
    }

    post {
        always {
            echo "📊 Publicando reportes..."

            // Publicar resultados JUnit (para métricas de Jenkins)
            junit allowEmptyResults: true, testResults: 'target/cucumber-reports/cucumber.xml'

            // Generar reporte Allure (de tu pipeline original [cite: 631])
            allure([
                    includeProperties: false,
                    jdk: '',
                    properties: [],
                    reportBuildPolicy: 'ALWAYS',
                    results: [[path: env.ALLURE_RESULTS]]
            ])

            cleanWs()
        }
    }
}