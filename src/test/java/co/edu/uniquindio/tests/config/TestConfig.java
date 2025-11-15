package co.edu.uniquindio.tests.config;

import lombok.Getter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Getter
public class TestConfig {

    private static TestConfig instance;
    private final Properties properties;

    private final String apiBaseUrl;
    private final String keycloakUrl;
    private final String keycloakRealm;
    private final String keycloakClientId;
    private final String keycloakClientSecret;
    private final String adminUsername;
    private final String adminPassword;
    private final String adminClientId;

    private TestConfig() {
        properties = new Properties();
        loadProperties();
        this.apiBaseUrl = getProperty("api.base.url", "http://localhost:8080");
        this.keycloakUrl = getProperty("keycloak.url", "http://localhost:8082");
        this.keycloakRealm = getProperty("keycloak.realm", "taller");
        this.keycloakClientId = getProperty("keycloak.client.id", "taller-api");
        this.keycloakClientSecret = getProperty("keycloak.client.secret", "jx34gvJ7Vo9UwxLwsbLa1K3C58ZbjrLh");
        this.adminUsername = getProperty("admin.username", "admin");
        this.adminPassword = getProperty("admin.password", "admin123");
        this.adminClientId = getProperty("admin.client.id", "admin-cli");
    }

    public static TestConfig getInstance() {
        if (instance == null) {
            synchronized (TestConfig.class) {
                if (instance == null) {
                    instance = new TestConfig();
                }
            }
        }
        return instance;
    }

    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("test.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            System.err.println("No se pudo cargar test.properties");
        }
    }

    private String getProperty(String key, String defaultValue) {
        String envValue = System.getenv(key.toUpperCase().replace(".", "_"));
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        String sysValue = System.getProperty(key);
        if (sysValue != null && !sysValue.isEmpty()) {
            return sysValue;
        }

        return properties.getProperty(key, defaultValue);
    }

    public String getKeycloakTokenUrl() {
        return String.format("%s/realms/%s/protocol/openid-connect/token",
                keycloakUrl, keycloakRealm);
    }

    public String getMasterTokenUrl() {
        return String.format("%s/realms/master/protocol/openid-connect/token",
                keycloakUrl);
    }

    public String getKeycloakUserInfoUrl() {
        return String.format("%s/realms/%s/protocol/openid-connect/userinfo",
                keycloakUrl, keycloakRealm);
    }

    public String getKeycloakLogoutUrl() {
        return String.format("%s/realms/%s/protocol/openid-connect/logout",
                keycloakUrl, keycloakRealm);
    }

    public String getUsersEndpoint() {
        return apiBaseUrl + "/api/usuarios";
    }

    public String getProfilesEndpoint() {
        // ⬇️ --- MODIFICACIÓN --- ⬇️
        // Ahora apuntamos al API Gateway (apiBaseUrl), no al servicio de Go.
        // El Gateway se encargará de enrutar a "http://profile-service-go:8081"
        // y de inyectar el header X-User-ID.
        return apiBaseUrl + "/api/perfiles";
        // ⬆️ --- FIN DE LA MODIFICACIÓN --- ⬆️
    }

    public String getPasswordEndpoint() {
        return apiBaseUrl + "/api/usuarios/password";
    }

    public String getHealthEndpoint() {
        return apiBaseUrl + "/actuator/health";
    }

    public String getMetricsEndpoint() {
        return apiBaseUrl + "/actuator/prometheus";
    }
}