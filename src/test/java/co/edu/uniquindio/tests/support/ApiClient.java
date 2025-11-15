package co.edu.uniquindio.tests.support;

import co.edu.uniquindio.tests.config.TestConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;

import static io.restassured.RestAssured.given;

@Slf4j
public class ApiClient {

    private static ApiClient instance;
    private final TestConfig config;
    private final TokenClient tokenClient;

    private ApiClient() {
        this.config = TestConfig.getInstance();
        this.tokenClient = TokenClient.getInstance();
    }

    public static ApiClient getInstance() {
        if (instance == null) {
            synchronized (ApiClient.class) {
                if (instance == null) {
                    instance = new ApiClient();
                }
            }
        }
        return instance;
    }

    public RequestSpecification baseRequest() {
        return given()
                .contentType("application/json")
                .accept("application/json")
                .log().all();
    }

    public RequestSpecification authenticatedRequest(String token) {
        return baseRequest()
                .header("Authorization", "Bearer " + token);
    }

    // ⬇️ --- MODIFICACIÓN --- ⬇️
    // ELIMINADO el método profileRequest(String userId)
    // ⬆️ --- FIN DE LA MODIFICACIÓN --- ⬆️

    public RequestSpecification adminRequest() {
        String adminToken = tokenClient.getAdminToken();
        return authenticatedRequest(adminToken);
    }

    public Response get(String endpoint) {
        log.debug("GET: {}", endpoint);
        return baseRequest()
                .when()
                .get(endpoint)
                .then()
                .log().all()
                .extract().response();
    }

    public Response getAuthenticated(String endpoint, String token) {
        log.debug("GET (autenticado): {}", endpoint);
        return authenticatedRequest(token)
                .when()
                .get(endpoint)
                .then()
                .log().all()
                .extract().response();
    }

    public Response post(String endpoint, Object body) {
        log.debug("POST: {}", endpoint);
        return baseRequest()
                .body(body)
                .when()
                .post(endpoint)
                .then()
                .log().all()
                .extract().response();
    }

    public Response postAuthenticated(String endpoint, Object body, String token) {
        log.debug("POST (autenticado): {}", endpoint);
        return authenticatedRequest(token)
                .body(body)
                .when()
                .post(endpoint)
                .then()
                .log().all()
                .extract().response();
    }

    public Response putAuthenticated(String endpoint, Object body, String token) {
        log.debug("PUT (autenticado): {}", endpoint);
        return authenticatedRequest(token)
                .body(body)
                .when()
                .put(endpoint)
                .then()
                .log().all()
                .extract().response();
    }

    public Response patchAuthenticated(String endpoint, Object body, String token) {
        log.debug("PATCH (autenticado): {}", endpoint);
        return authenticatedRequest(token)
                .body(body)
                .when()
                .patch(endpoint)
                .then()
                .log().all()
                .extract().response();
    }

    public Response deleteAuthenticated(String endpoint, String token) {
        log.debug("DELETE (autenticado): {}", endpoint);
        return authenticatedRequest(token)
                .when()
                .delete(endpoint)
                .then()
                .log().all()
                .extract().response();
    }

    // ⬇️ --- MODIFICACIÓN --- ⬇️
    // ELIMINADOS los métodos getProfile(String endpoint, String userId)
    // y putProfile(String endpoint, Object body, String userId)
    // ⬆️ --- FIN DE LA MODIFICACIÓN --- ⬆️
}