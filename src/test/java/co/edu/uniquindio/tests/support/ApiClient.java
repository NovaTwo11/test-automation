package co.edu.uniquindio.tests.support;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.Map;

public class ApiClient {

    private final String bearerToken;

    public ApiClient(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    private io.restassured.specification.RequestSpecification base() {
        return RestAssured.given()
                .header("Authorization", "Bearer " + bearerToken)
                .contentType("application/json")
                .accept("application/json");
    }

    public Response createUser(String nombre, String email, String password) {
        Map<String, Object> body = Map.of(
                "nombre", nombre,
                "email", email,
                "password", password
        );
        return base()
                .body(body)
                .when()
                .post("/api/usuarios")
                .then()
                .extract()
                .response();
    }

    public Response getUserById(String id) {
        return base()
                .when()
                .get("/api/usuarios/{id}", id)
                .then()
                .extract()
                .response();
    }

    public Response deleteUserById(String id) {
        return base()
                .when()
                .delete("/api/usuarios/{id}", id)
                .then()
                .extract()
                .response();
    }

    // Ya existentes en tu proyecto (referenciados por otros steps):
    public Response requestPasswordReset(String email) {
        Map<String, Object> body = Map.of("email", email);
        return base()
                .body(body)
                .when()
                .post("/api/auth/forgot-password")
                .then()
                .extract()
                .response();
    }

    public String extractResetTokenFromForgotResponse(Response forgot) {
        try {
            String resetUrl = forgot.jsonPath().getString("resetUrl");
            if (resetUrl == null) return null;
            int idx = resetUrl.indexOf("token=");
            return idx > 0 ? resetUrl.substring(idx + "token=".length()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public Response confirmPasswordReset(String token, String newPassword) {
        Map<String, Object> body = Map.of(
                "token", token,
                "newPassword", newPassword
        );
        return base()
                .body(body)
                .when()
                .post("/api/auth/reset-password")
                .then()
                .extract()
                .response();
    }
}