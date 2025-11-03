pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }

    environment {
        // URLs de servicios
        API_URL = 'http://host.docker.internal:8080'
        SONAR_HOST_URL = 'http://sonarqube:9000'

        // Credenciales de SonarQube
        SONAR_TOKEN = credentials('sonar-token')

        // Configuración de Maven
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
    }

    stages {
        stage('Checkout') {
            steps {
                echo '📥 Clonando repositorio...'
                checkout scm
            }
        }

        stage('Build API') {
            steps {
                echo '🔨 Compilando taller-api-2...'
                dir('taller-api-2') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Unit Tests API') {
            steps {
                echo '🧪 Ejecutando pruebas unitarias de la API...'
                dir('taller-api-2') {
                    sh 'mvn test'
                }
            }
            post {
                always {
                    junit 'taller-api-2/target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis - API') {
            steps {
                echo '📊 Analizando calidad del código de la API...'
                dir('taller-api-2') {
                    withSonarQubeEnv('SonarQube') {
                        sh """
                            mvn sonar:sonar \
                              -Dsonar.projectKey=taller-api-2 \
                              -Dsonar.projectName='Taller API 2' \
                              -Dsonar.host.url=${SONAR_HOST_URL} \
                              -Dsonar.login=${SONAR_TOKEN}
                        """
                    }
                }
            }
        }

        stage('Quality Gate - API') {
            steps {
                echo '🚦 Verificando Quality Gate...'
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: false
                }
            }
        }

        stage('Start API') {
            steps {
                echo '🚀 Iniciando API en background...'
                dir('taller-api-2') {
                    sh '''
                        nohup java -jar target/*.jar > api.log 2>&1 &
                        echo $! > api.pid
                        
                        echo "Esperando a que la API esté lista..."
                        for i in {1..30}; do
                            if curl -f ${API_URL}/api/usuarios 2>/dev/null; then
                                echo "✅ API está lista"
                                exit 0
                            fi
                            echo "Intento $i/30..."
                            sleep 5
                        done
                        
                        echo "❌ API no respondió a tiempo"
                        exit 1
                    '''
                }
            }
        }

        stage('Automation Tests') {
            steps {
                echo '🤖 Ejecutando pruebas de automatización...'
                dir('test-automation') {
                    sh 'mvn clean test -Dcucumber.publish.enabled=false'
                }
            }
            post {
                always {
                    dir('test-automation') {
                        // Publicar resultados JUnit
                        junit 'target/surefire-reports/*.xml'

                        // Publicar resultados Cucumber
                        cucumber buildStatus: 'UNSTABLE',
                                reportTitle: 'Cucumber Report',
                                fileIncludePattern: '**/*.json',
                                jsonReportDirectory: 'target'
                    }
                }
            }
        }

        stage('SonarQube Analysis - Tests') {
            steps {
                echo '📊 Analizando calidad del código de tests...'
                dir('test-automation') {
                    withSonarQubeEnv('SonarQube') {
                        sh """
                            mvn sonar:sonar \
                              -Dsonar.projectKey=test-automation \
                              -Dsonar.projectName='Test Automation' \
                              -Dsonar.host.url=${SONAR_HOST_URL} \
                              -Dsonar.login=${SONAR_TOKEN}
                        """
                    }
                }
            }
        }

        stage('Allure Report') {
            steps {
                echo '📈 Generando reporte Allure...'
                dir('test-automation') {
                    script {
                        allure([
                                includeProperties: false,
                                jdk: '',
                                properties: [],
                                reportBuildPolicy: 'ALWAYS',
                                results: [[path: 'target/allure-results']]
                        ])
                    }
                }
            }
        }
    }

    post {
        always {
            echo '🧹 Limpiando recursos...'
            script {
                // Detener API
                sh '''
                    if [ -f taller-api-2/api.pid ]; then
                        kill $(cat taller-api-2/api.pid) || true
                        rm taller-api-2/api.pid
                    fi
                '''

                // Limpiar workspace si es necesario
                // cleanWs()
            }
        }
        success {
            echo '✅ Pipeline ejecutado exitosamente'
            emailext(
                    subject: "✅ Build Exitoso: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """
                    <h2>Build Exitoso</h2>
                    <p><strong>Job:</strong> ${env.JOB_NAME}</p>
                    <p><strong>Build:</strong> #${env.BUILD_NUMBER}</p>
                    <p><strong>URL:</strong> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
                    <p><strong>Allure Report:</strong> <a href="${env.BUILD_URL}allure">Ver Reporte</a></p>
                """,
                    to: 'equipo@example.com',
                    mimeType: 'text/html'
            )
        }
        failure {
            echo '❌ Pipeline falló'
            emailext(
                    subject: "❌ Build Fallido: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """
                    <h2>Build Fallido</h2>
                    <p><strong>Job:</strong> ${env.JOB_NAME}</p>
                    <p><strong>Build:</strong> #${env.BUILD_NUMBER}</p>
                    <p><strong>URL:</strong> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
                    <p><strong>Console:</strong> <a href="${env.BUILD_URL}console">Ver Console</a></p>
                """,
                    to: 'equipo@example.com',
                    mimeType: 'text/html'
            )
        }
    }
}