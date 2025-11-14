package co.edu.uniquindio.tests.support;

import co.edu.uniquindio.tests.config.TestConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Cliente encargado de manejar la autenticación y obtener información del usuario
 * desde Keycloak (u otro servidor compatible con OpenID Connect).
 */
@Slf4j
public class AuthClient {

    private static AuthClient instance;
    private final TestConfig config; // Guardamos la config completa

    private AuthClient() {
        this.config = TestConfig.getInstance();
    }

    public static AuthClient getInstance() {
        if (instance == null) {
            synchronized (AuthClient.class) {
                if (instance == null) {
                    instance = new AuthClient();
                }
            }
        }
        return instance;
    }

    /**
     * --- MÉTODO 1 (Para usuarios normales del realm 'taller') ---
     * Llama al realm 'taller' usando el client_id y secret de la app.
     */
    public Response requestTokenResponse(String username, String password) {
        log.debug("Solicitando token (realm taller) para user={}", username);
        return requestTokenResponse(
                username,
                password,
                config.getKeycloakTokenUrl(),      // URL de 'taller'
                config.getKeycloakClientId(),    // 'taller-api'
                config.getKeycloakClientSecret() // 'jx34...'
        );
    }

    /**
     * --- MÉTODO 2 (El nuevo método de trabajo principal) ---
     * Llama a CUALQUIER endpoint de token con CUALQUIER credencial.
     * Es usado por getAdminToken y por el método de arriba.
     */
    public Response requestTokenResponse(String username, String password, String tokenUrl, String clientId, String clientSecret) {
        log.debug("Solicitando token para user={} en URL={} con clientId={}", username, tokenUrl, clientId);

        Map<String, String> formParams = new HashMap<>();
        formParams.put("grant_type", "password");
        formParams.put("client_id", clientId);
        formParams.put("username", username);
        formParams.put("password", password);

        // El Client Secret es opcional (admin-cli no lo necesita/usa)
        if (clientSecret != null && !clientSecret.isEmpty()) {
            formParams.put("client_secret", clientSecret);
        }

        Response response = given()
                .contentType(ContentType.URLENC)
                .formParams(formParams)
                .when()
                .post(tokenUrl);

        log.debug("Token endpoint → status={} body={}",
                response.statusCode(), safeBody(response));

        return response;
    }

    /**
     * Obtiene el access_token y lanza excepción si la respuesta no es exitosa.
     * (Este método no cambia, seguirá usando el realm por defecto 'taller')
     */
    public String getAccessToken(String username, String password) {
        Response r = requestTokenResponse(username, password); // Llama al método por defecto
        if (r.statusCode() != 200) {
            log.error("Error obteniendo token. Status: {}, Body: {}", r.statusCode(), safeBody(r));
            throw new RuntimeException("No se pudo obtener el token de acceso. Status: " + r.statusCode());
        }
        String token = r.jsonPath().getString("access_token");
        log.debug("Token obtenido correctamente para {} ({} caracteres)", username, token != null ? token.length() : 0);
        return token;
    }

    /**
     * Llama al endpoint userinfo con el token proporcionado.
     * (Este método no cambia)
     */
    public Response getUserInfo(String accessToken) {
        log.debug("Llamando al endpoint userinfo");
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(config.getKeycloakUserInfoUrl()); // Usa la URL de config

        log.debug("Userinfo → status={} body={}", response.statusCode(), safeBody(response));
        return response;
    }

    private String safeBody(Response response) {
        try {
            return response.getBody() != null ? response.getBody().asString() : "(sin cuerpo)";
        } catch (Exception e) {
            return "(error leyendo body)";
        }
    }
}