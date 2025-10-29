package co.edu.uniquindio.tests.steps;

import co.edu.uniquindio.tests.utils.TokenClient;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class UserSteps {

    private String token;
    private Response response;

    @Given("que tengo un token válido de administración")
    public void queTengoUnTokenValidoDeAdministracion() {
        token = TokenClient.getClientCredentialsToken();
        assertThat("El token no debe ser nulo/ vacío", token, allOf(notNullValue(), not(isEmptyString())));
    }

    @When("listo los usuarios")
    public void listoLosUsuarios() {
        response = RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/usuarios")
                .then()
                .extract().response();
    }

    @Then("obtengo un listado de usuarios con estado {int}")
    public void obtengoUnListadoDeUsuariosConEstado(Integer statusCode) {
        int actualStatus = response.statusCode();
        String responseBody = response.asString();
        if (!statusCode.equals(actualStatus)) {
            System.err.println("Body recibido: " + responseBody);
        }
        org.junit.jupiter.api.Assertions.assertEquals(statusCode.intValue(), actualStatus, "Status inesperado");
        // Validación mínima del payload como array o wrapper conocido
        // Si tu endpoint retorna lista directa:
        // response.then().body("$", not(empty()));
    }
}