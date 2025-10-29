package co.edu.uniquindio.tests.steps;

import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;

public class SecuritySteps {

    private Response resp;

    @When("listo los usuarios sin token")
    public void listoUsuariosSinToken() {
        resp = RestAssured.given()
                .when()
                .get("/api/usuarios")
                .then()
                .extract().response();
        System.out.printf("[NoAuth] %d %s%n", resp.statusCode(), resp.asString());
        CommonSteps.setLastResponse(resp);
    }
}