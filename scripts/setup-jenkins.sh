#!/bin/bash

echo "🔧 Configurando Jenkins..."

# Esperar a que Jenkins esté listo
echo "Esperando a Jenkins..."
until curl -s http://localhost:8081 > /dev/null; do
    sleep 5
done

echo "✅ Jenkins está listo"

# Obtener password inicial
JENKINS_PASSWORD=$(docker exec jenkins-taller cat /var/jenkins_home/secrets/initialAdminPassword)

echo "================================================"
echo "🔑 Password inicial de Jenkins:"
echo "$JENKINS_PASSWORD"
echo "================================================"
echo ""
echo "📝 Pasos siguientes:"
echo "1. Accede a http://localhost:8081"
echo "2. Usa el password mostrado arriba"
echo "3. Instala los plugins sugeridos"
echo "4. Instala plugins adicionales:"
echo "   - Allure"
echo "   - SonarQube Scanner"
echo "   - Cucumber Reports"
echo "   - Pipeline"
echo "   - Git"
echo "5. Crea un usuario administrador"
echo "================================================"