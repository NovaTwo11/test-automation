package co.edu.uniquindio.tests.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class CommonSteps {

    private static Response lastResponse;

    @When("I GET {string}")
    public void iGET(String path) {
        lastResponse = given()
                .when()
                .get(path)
                .then()
                .extract().response();

        System.out.printf("GET %s -> %d%n%s%n", path, lastResponse.statusCode(), lastResponse.asString());
    }

    @Then("the response status should be {int}")
    public void statusShouldBe(int status) {
        assertThat("Unexpected status", lastResponse.statusCode(), equalTo(status));
    }

    @Then("the field {string} should be {string}")
    public void fieldShouldBe(String jsonPath, String expected) {
        lastResponse.then().body(jsonPath, equalTo(expected));
    }

    @Then("obtengo estado {int}")
    public void obtengoEstado(int status) {
        assertThat("Status inesperado", lastResponse.statusCode(), equalTo(status));
    }

    public static void setLastResponse(Response resp) {
        lastResponse = resp;
    }

    public static Response getLastResponse() {
        return lastResponse;
    }
}