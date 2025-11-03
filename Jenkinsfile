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

        // Configuración de Maven
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'

        // Configuración de proyecto
        PROJECT_NAME = 'test-automation'
        SONAR_PROJECT_KEY = 'test-automation'
    }

    stages {
        stage('🔍 Información del Build') {
            steps {
                script {
                    echo "================================================"
                    echo "🚀 Iniciando Pipeline de Test Automation"
                    echo "================================================"
                    echo "Job: ${env.JOB_NAME}"
                    echo "Build: #${env.BUILD_NUMBER}"
                    echo "Branch: ${env.GIT_BRANCH ?: 'N/A'}"
                    echo "Workspace: ${env.WORKSPACE}"
                    echo "================================================"
                }
            }
        }

        stage('📥 Checkout') {
            steps {
                echo '📥 Clonando repositorio test-automation...'
                checkout scm

                script {
                    // Mostrar información del commit
                    sh '''
                        echo "Último commit:"
                        git log -1 --pretty=format:"%h - %an, %ar : %s"
                    '''
                }
            }
        }

        stage('🔍 Verificar API') {
            steps {
                echo '🔍 Verificando que la API esté disponible...'
                script {
                    def apiAvailable = false
                    def maxRetries = 5
                    def retryCount = 0

                    while (!apiAvailable && retryCount < maxRetries) {
                        try {
                            sh "curl -f ${API_URL}/api/usuarios"
                            apiAvailable = true
                            echo "✅ API está disponible"
                        } catch (Exception e) {
                            retryCount++
                            if (retryCount < maxRetries) {
                                echo "⏳ Intento ${retryCount}/${maxRetries} - Esperando 10 segundos..."
                                sleep(10)
                            } else {
                                error "❌ API no está disponible después de ${maxRetries} intentos"
                            }
                        }
                    }
                }
            }
        }

        stage('📦 Compilar Proyecto') {
            steps {
                echo '📦 Compilando proyecto test-automation...'
                sh 'mvn clean compile'
            }
        }

        stage('🧪 Ejecutar Tests') {
            steps {
                echo '🧪 Ejecutando pruebas de automatización...'
                script {
                    try {
                        sh 'mvn test -Dcucumber.publish.enabled=false'
                    } catch (Exception e) {
                        echo "⚠️ Algunos tests fallaron, pero continuamos para generar reportes"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
            post {
                always {
                    // Publicar resultados JUnit
                    junit allowEmptyResults: true,
                            testResults: 'target/surefire-reports/*.xml'

                    // Publicar resultados Cucumber
                    cucumber buildStatus: 'UNSTABLE',
                            reportTitle: 'Cucumber Report',
                            fileIncludePattern: '**/*.json',
                            jsonReportDirectory: 'target',
                            sortingMethod: 'ALPHABETICAL'
                }
            }
        }

        stage('📊 Análisis SonarQube') {
            steps {
                echo '📊 Analizando calidad del código con SonarQube...'
                script {
                    try {
                        withSonarQubeEnv('SonarQube') {
                            sh """
                                mvn sonar:sonar \
                                  -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                                  -Dsonar.projectName='${PROJECT_NAME}' \
                                  -Dsonar.host.url=${SONAR_HOST_URL}
                            """
                        }
                    } catch (Exception e) {
                        echo "⚠️ Error en análisis de SonarQube: ${e.message}"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }

        stage('🚦 Quality Gate') {
            steps {
                echo '🚦 Verificando Quality Gate de SonarQube...'
                script {
                    try {
                        timeout(time: 5, unit: 'MINUTES') {
                            def qg = waitForQualityGate()
                            if (qg.status != 'OK') {
                                echo "⚠️ Quality Gate falló: ${qg.status}"
                                currentBuild.result = 'UNSTABLE'
                            } else {
                                echo "✅ Quality Gate aprobado"
                            }
                        }
                    } catch (Exception e) {
                        echo "⚠️ Error verificando Quality Gate: ${e.message}"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }

        stage('📈 Generar Reporte Allure') {
            steps {
                echo '📈 Generando reporte Allure...'
                script {
                    try {
                        allure([
                                includeProperties: false,
                                jdk: '',
                                properties: [],
                                reportBuildPolicy: 'ALWAYS',
                                results: [[path: 'target/allure-results']]
                        ])
                        echo "✅ Reporte Allure generado: ${env.BUILD_URL}allure"
                    } catch (Exception e) {
                        echo "⚠️ Error generando reporte Allure: ${e.message}"
                    }
                }
            }
        }

        stage('📋 Resumen de Resultados') {
            steps {
                script {
                    echo "================================================"
                    echo "📋 RESUMEN DE RESULTADOS"
                    echo "================================================"

                    // Leer resultados de tests
                    def testResults = junit testResults: 'target/surefire-reports/*.xml'

                    echo "Total de tests: ${testResults.totalCount}"
                    echo "✅ Exitosos: ${testResults.passCount}"
                    echo "❌ Fallidos: ${testResults.failCount}"
                    echo "⏭️  Omitidos: ${testResults.skipCount}"
                    echo ""
                    echo "🔗 Reportes disponibles:"
                    echo "   - JUnit: ${env.BUILD_URL}testReport"
                    echo "   - Cucumber: ${env.BUILD_URL}cucumber-html-reports"
                    echo "   - Allure: ${env.BUILD_URL}allure"
                    echo "   - SonarQube: ${SONAR_HOST_URL}/dashboard?id=${SONAR_PROJECT_KEY}"
                    echo "================================================"
                }
            }
        }
    }

    post {
        always {
            echo '🧹 Limpiando recursos...'

            // Archivar logs y reportes
            archiveArtifacts artifacts: 'target/surefire-reports/**/*',
                    allowEmptyArchive: true

            archiveArtifacts artifacts: 'target/allure-results/**/*',
                    allowEmptyArchive: true

            // Limpiar workspace si es necesario (comentado por defecto)
            // cleanWs()
        }

        success {
            script {
                echo '================================================'
                echo '✅ PIPELINE EJECUTADO EXITOSAMENTE'
                echo '================================================'

                // Enviar notificación por email (opcional)
                // emailext(
                //     subject: "✅ Build Exitoso: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                //     body: """
                //         <h2>Build Exitoso</h2>
                //         <p><strong>Job:</strong> ${env.JOB_NAME}</p>
                //         <p><strong>Build:</strong> #${env.BUILD_NUMBER}</p>
                //         <p><strong>Duración:</strong> ${currentBuild.durationString}</p>
                //         <p><strong>Reportes:</strong></p>
                //         <ul>
                //             <li><a href="${env.BUILD_URL}allure">Allure Report</a></li>
                //             <li><a href="${env.BUILD_URL}cucumber-html-reports">Cucumber Report</a></li>
                //             <li><a href="${SONAR_HOST_URL}/dashboard?id=${SONAR_PROJECT_KEY}">SonarQube Dashboard</a></li>
                //         </ul>
                //     """,
                //     to: 'equipo@example.com',
                //     mimeType: 'text/html'
                // )
            }
        }

        failure {
            script {
                echo '================================================'
                echo '❌ PIPELINE FALLÓ'
                echo '================================================'

                // Enviar notificación por email (opcional)
                // emailext(
                //     subject: "❌ Build Fallido: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                //     body: """
                //         <h2>Build Fallido</h2>
                //         <p><strong>Job:</strong> ${env.JOB_NAME}</p>
                //         <p><strong>Build:</strong> #${env.BUILD_NUMBER}</p>
                //         <p><strong>Duración:</strong> ${currentBuild.durationString}</p>
                //         <p><strong>Console:</strong> <a href="${env.BUILD_URL}console">Ver Console</a></p>
                //     """,
                //     to: 'equipo@example.com',
                //     mimeType: 'text/html'
                // )
            }
        }

        unstable {
            script {
                echo '================================================'
                echo '⚠️  PIPELINE INESTABLE (algunos tests fallaron)'
                echo '================================================'
            }
        }
    }
}