package co.edu.uniquindio.tests.config;

import io.restassured.RestAssured;
import io.qameta.allure.restassured.AllureRestAssured;

public final class TestConfig {

    // Base URIs (puedes sobreescribir por env vars o -D)
    public static final String API_BASE_URL      = getEnvOrProp("API_BASE_URL",      "apiBaseUri",        "http://localhost:8080");
    public static final String KEYCLOAK_BASE_URL = getEnvOrProp("KEYCLOAK_BASE_URL", "keycloakUri",       "http://localhost:8082");
    public static final String KEYCLOAK_REALM    = getEnvOrProp("KEYCLOAK_REALM",    "keycloakRealm",     "taller");

    // Clientes (admin vs. api)
    public static final String ADMIN_CLIENT_ID     = getEnvOrProp("KC_ADMIN_CLIENT_ID",     "kc.admin.clientId",     "taller-api-admin");
    public static final String ADMIN_CLIENT_SECRET = getEnvOrProp("KC_ADMIN_CLIENT_SECRET", "kc.admin.clientSecret", "g1IP83yywb0qGcpP2RJ93wKXTcK4CuXH");

    public static final String API_CLIENT_ID     = getEnvOrProp("API_CLIENT_ID",     "api.clientId",     "taller-api");
    public static final String API_CLIENT_SECRET = getEnvOrProp("API_CLIENT_SECRET", "api.clientSecret", "jx34gvJ7Vo9UwxLwsbLa1K3C58ZbjrLh");

    // Alternativa password grant
    public static final String API_ADMIN_USERNAME = getEnvOrProp("API_ADMIN_USERNAME", "api.admin.username", "");
    public static final String API_ADMIN_PASSWORD = getEnvOrProp("API_ADMIN_PASSWORD", "api.admin.password", "");

    // Maildev para futuros tests
    public static final String MAILDEV_BASE = getEnvOrProp("MAILDEV_BASE_URL", "maildevBaseUri", "http://localhost:1080");

    public static final int TIMEOUT_MS = Integer.parseInt(getEnvOrProp("TEST_TIMEOUT_MS", "test.timeout.ms", "15000"));

    private TestConfig() {}

    public static void configureRestAssured() {
        RestAssured.baseURI = API_BASE_URL;
        RestAssured.useRelaxedHTTPSValidation();

        // Allure – captura request/response sin más configuración
        RestAssured.filters(new AllureRestAssured());

        // Log automático cuando falle una validación
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    public static String tokenEndpoint() {
        return KEYCLOAK_BASE_URL + "/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect/token";
    }

    private static String getEnvOrProp(String envKey, String propKey, String def) {
        String v = System.getenv(envKey);
        if (v != null && !v.isBlank()) return v;
        v = System.getProperty(propKey);
        if (v != null && !v.isBlank()) return v;
        return def;
    }
}