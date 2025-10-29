package co.edu.uniquindio.tests.support;

import co.edu.uniquindio.tests.config.TestConfig;
import io.restassured.RestAssured;

public class AuthClient {

    public String getApiAccessToken() {
        // Si hay credenciales de usuario admin, intenta password grant
        if (!TestConfig.API_ADMIN_USERNAME.isBlank() && !TestConfig.API_ADMIN_PASSWORD.isBlank()) {
            return RestAssured.given()
                    .contentType("application/x-www-form-urlencoded")
                    .formParam("client_id", TestConfig.API_CLIENT_ID)
                    .formParam("client_secret", TestConfig.API_CLIENT_SECRET)
                    .formParam("grant_type", "password")
                    .formParam("username", TestConfig.API_ADMIN_USERNAME)
                    .formParam("password", TestConfig.API_ADMIN_PASSWORD)
                    .when()
                    .post(TestConfig.tokenEndpoint())
                    .then()
                    .statusCode(200)
                    .extract().path("access_token");
        }

        // Por defecto: client_credentials
        return RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("client_id", TestConfig.API_CLIENT_ID)
                .formParam("client_secret", TestConfig.API_CLIENT_SECRET)
                .formParam("grant_type", "client_credentials")
                .when()
                .post(TestConfig.tokenEndpoint())
                .then()
                .statusCode(200)
                .extract().path("access_token");
    }
}