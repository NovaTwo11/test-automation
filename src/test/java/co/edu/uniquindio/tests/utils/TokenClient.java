package co.edu.uniquindio.tests.utils;

import co.edu.uniquindio.tests.config.TestConfig;
import io.restassured.RestAssured;
import io.restassured.response.Response;

public class TokenClient {

    public static String getClientCredentialsToken() {
        Response response = RestAssured
                .given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "client_credentials")
                .formParam("client_id", TestConfig.API_CLIENT_ID)
                .formParam("client_secret", TestConfig.API_CLIENT_SECRET)
                .when()
                .post(TestConfig.tokenEndpoint())
                .then()
                .statusCode(200)
                .extract()
                .response();

        String token = response.jsonPath().getString("access_token");
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("No se pudo obtener el token de Keycloak (client_credentials)");
        }
        return token;
    }

    public static String getPasswordToken(String username, String password) {
        Response response = RestAssured
                .given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "password")
                .formParam("client_id", TestConfig.API_CLIENT_ID)
                .formParam("client_secret", TestConfig.API_CLIENT_SECRET)
                .formParam("username", username)
                .formParam("password", password)
                .when()
                .post(TestConfig.tokenEndpoint())
                .then()
                .statusCode(200)
                .extract()
                .response();

        String token = response.jsonPath().getString("access_token");
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("No se pudo obtener el token con credenciales de usuario");
        }
        return token;
    }

    public static Response tryPasswordTokenRaw(String username, String password) {
        return RestAssured
                .given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "password")
                .formParam("client_id", TestConfig.API_CLIENT_ID)
                .formParam("client_secret", TestConfig.API_CLIENT_SECRET)
                .formParam("username", username)
                .formParam("password", password)
                .when()
                .post(TestConfig.tokenEndpoint())
                .then()
                .extract()
                .response();
    }
}