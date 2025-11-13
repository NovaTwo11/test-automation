package co.edu.uniquindio.tests.support;

import co.edu.uniquindio.tests.config.TestConfig;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

import static io.restassured.RestAssured.given;

/**
 * Cliente encargado de manejar la autenticación y obtener información del usuario
 * desde Keycloak (u otro servidor compatible con OpenID Connect).
 */
@Slf4j
public class AuthClient {

    private static AuthClient instance;

    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final String userInfoUrl;

    private AuthClient() {
        TestConfig config = TestConfig.getInstance();
        this.tokenUrl = config.getKeycloakTokenUrl();
        this.clientId = config.getKeycloakClientId();
        this.clientSecret = config.getKeycloakClientSecret();
        this.userInfoUrl = config.getKeycloakUserInfoUrl();
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
     * Realiza la petición al token endpoint y devuelve la Response completa.
     * No lanza excepciones: el caller decide cómo tratar status != 200.
     */
    public Response requestTokenResponse(String username, String password) {
        log.debug("Solicitando token para user={}", username);

        Response response = given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "password")
                .formParam("client_id", clientId)
                .formParam("client_secret", clientSecret)
                .formParam("username", username)
                .formParam("password", password)
                .when()
                .post(tokenUrl);

        log.debug("Token endpoint → status={} body={}",
                response.statusCode(), safeBody(response));

        return response;
    }

    /**
     * Obtiene el access_token y lanza excepción si la respuesta no es exitosa (mantiene compatibilidad).
     */
    public String getAccessToken(String username, String password) {
        Response r = requestTokenResponse(username, password);
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
     */
    public Response getUserInfo(String accessToken) {
        log.debug("Llamando al endpoint userinfo");
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(userInfoUrl);

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
