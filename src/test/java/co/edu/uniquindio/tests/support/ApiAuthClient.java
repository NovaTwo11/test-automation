package co.edu.uniquindio.tests.support;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.Map;

public class ApiAuthClient {

    public Response login(String email, String password) {
        Map<String, Object> body = Map.of(
                "email", email,
                "password", password
        );
        return RestAssured.given()
                .contentType("application/json")
                .body(body)
                .when()
                .post("/api/auth/login")
                .then()
                .extract()
                .response();
    }

    // Devuelve token si existe en alguna clave común, si no, null.
    public String extractAccessToken(Response loginResponse) {
        try {
            String t = loginResponse.jsonPath().getString("access_token");
            if (t != null && !t.isBlank()) return t;
        } catch (Exception ignored) {}
        try {
            String t = loginResponse.jsonPath().getString("token");
            if (t != null && !t.isBlank()) return t;
        } catch (Exception ignored) {}
        try {
            String t = loginResponse.jsonPath().getString("jwt");
            if (t != null && !t.isBlank()) return t;
        } catch (Exception ignored) {}
        try {
            String t = loginResponse.jsonPath().getString("accessToken");
            if (t != null && !t.isBlank()) return t;
        } catch (Exception ignored) {}
        return null;
    }
}