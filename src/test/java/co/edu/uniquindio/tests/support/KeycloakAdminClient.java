package co.edu.uniquindio.tests.support;

import co.edu.uniquindio.tests.config.TestConfig;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;

public class KeycloakAdminClient {

    private String realmAdminBase() {
        return TestConfig.KEYCLOAK_BASE_URL + "/admin/realms/" + TestConfig.KEYCLOAK_REALM;
    }

    public String getAdminToken() {
        return RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("client_id", TestConfig.ADMIN_CLIENT_ID)
                .formParam("client_secret", TestConfig.ADMIN_CLIENT_SECRET)
                .formParam("grant_type", "client_credentials")
                .when()
                .post(TestConfig.tokenEndpoint())
                .then()
                .statusCode(200)
                .extract()
                .path("access_token");
    }

    public String ensureUserExistsByEmail(String adminToken, String email) {
        String safeUsername = email.replace("+", ".").toLowerCase();

        // 1) Buscar por search (username/email)
        String id = findUserIdFlexible(adminToken, email, safeUsername);
        if (id != null) return id;

        // 2) Crear usuario con username seguro
        Map<String, Object> body = Map.of(
                "username", safeUsername,
                "email", email,
                "enabled", true,
                "emailVerified", true
        );

        Response create = RestAssured.given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body(body)
                .when()
                .post(realmAdminBase() + "/users")
                .then()
                .statusCode(anyOf(equalTo(201), equalTo(409)))
                .extract().response();

        // 3) Si 201, intentar usar Location
        if (create.statusCode() == 201) {
            String location = create.getHeader("Location");
            String idFromLocation = extractIdFromLocation(location);
            if (idFromLocation != null) return idFromLocation;
        }

        // 4) Reintentar búsqueda con el username safe
        return retryFindUserId(adminToken, email, safeUsername, 5, Duration.ofMillis(400));
    }

    public void setUserPassword(String adminToken, String userId, String newPassword, boolean temporary) {
        Map<String, Object> body = Map.of(
                "type", "password",
                "value", newPassword,
                "temporary", temporary
        );

        RestAssured.given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body(body)
                .when()
                .put(realmAdminBase() + "/users/" + userId + "/reset-password")
                .then()
                .statusCode(204);
    }

    // ---------------------------------------
    // Búsquedas robustas
    // ---------------------------------------

    private String retryFindUserId(String adminToken, String email, String username, int attempts, Duration backoff) {
        for (int i = 1; i <= attempts; i++) {
            String id = findUserIdFlexible(adminToken, email, username);
            if (id != null) return id;
            sleep(backoff);
        }
        return null;
    }

    private String findUserIdFlexible(String adminToken, String email, String username) {
        // Prioridad 1: search por username safe
        String bySearchUser = realmAdminBase() + "/users?search=" + url(username);
        String id = queryFirstId(adminToken, bySearchUser);
        if (id != null) return id;

        // Prioridad 2: search por email
        String bySearchEmail = realmAdminBase() + "/users?search=" + url(email);
        id = queryFirstId(adminToken, bySearchEmail);
        if (id != null) return id;

        // Prioridad 3: email exacto
        String byEmailExact = realmAdminBase() + "/users?email=" + url(email) + "&exact=true";
        id = queryFirstId(adminToken, byEmailExact);
        if (id != null) return id;

        // Prioridad 4: username exacto
        String byUsername = realmAdminBase() + "/users?username=" + url(username) + "&exact=true";
        return queryFirstId(adminToken, byUsername);
    }

    private String queryFirstId(String adminToken, String url) {
        try {
            Response r = RestAssured.given()
                    .header("Authorization", "Bearer " + adminToken)
                    .accept("application/json")
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .extract().response();

            return extractFirstId(r);
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------------------------------
    // Helpers
    // ---------------------------------------

    private static void sleep(Duration d) {
        try { Thread.sleep(d.toMillis()); } catch (InterruptedException ignored) {}
    }

    private static String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private static String extractFirstId(Response r) {
        try {
            List<Map<String, Object>> list = r.jsonPath().getList("$");
            if (list == null || list.isEmpty()) return null;
            Object id = list.get(0).get("id");
            return id == null ? null : id.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractIdFromLocation(String location) {
        if (location == null || location.isBlank()) return null;
        try {
            URI uri = URI.create(location);
            String path = uri.getPath(); // .../users/{id}
            int idx = path.lastIndexOf('/');
            if (idx >= 0 && idx + 1 < path.length()) {
                return path.substring(idx + 1);
            }
        } catch (Exception ignored) {}
        return null;
    }
}