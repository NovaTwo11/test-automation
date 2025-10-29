package co.edu.uniquindio.tests.steps;

import co.edu.uniquindio.tests.support.ApiClient;
import co.edu.uniquindio.tests.support.AuthClient;
import io.cucumber.java.en.*;
import io.restassured.response.Response;

import static org.junit.jupiter.api.Assertions.*;

public class UserGetDeleteSteps {

    private final TestContext context;
    private ApiClient api;

    public UserGetDeleteSteps(TestContext context) {
        this.context = context;
    }

    @When("consulto el usuario por id {string}")
    public void consultoUsuarioPorId(String id) {
        ensureApi();
        context.lastResponse = api.getUserById(id);
        System.out.printf("[GET by id] %d %s%n", context.lastResponse.statusCode(), context.lastResponse.asString());
        CommonSteps.setLastResponse(context.lastResponse);
    }

    @And("extraigo y guardo el id del usuario desde la última respuesta")
    public void extraigoYGuardoIdDesdeUltimaRespuesta() {
        assertNotNull(context.lastResponse, "No hay respuesta previa para extraer id");
        String id = null;
        try {
            id = context.lastResponse.jsonPath().getString("usuario.id");
            if (id == null || id.isBlank()) id = context.lastResponse.jsonPath().getString("id");
        } catch (Exception ignored) {}
        assertNotNull(id, "No se pudo extraer el id del usuario de la última respuesta");
        context.createdUserId = id;
    }

    @When("elimino el usuario por su id")
    public void eliminoUsuarioPorSuId() {
        assertNotNull(context.createdUserId, "No hay usuario creado para eliminar (createdUserId es null). " +
                "Asegúrate de ejecutar 'Y extraigo y guardo el id del usuario desde la última respuesta' después de crear.");
        ensureApi();
        Response resp = api.deleteUserById(context.createdUserId);
        context.lastResponse = resp;
        System.out.printf("[DELETE] %d %s%n", resp.statusCode(), resp.asString());
        CommonSteps.setLastResponse(context.lastResponse);
    }

    @Then("al consultar nuevamente el usuario por su id obtengo {int}")
    public void consultarNuevamenteLuegoDeDelete(Integer expected) {
        assertNotNull(context.createdUserId, "No hay id de usuario para reconsultar");
        Response resp = api.getUserById(context.createdUserId);
        System.out.printf("[GET after delete] %d %s%n", resp.statusCode(), resp.asString());
        org.junit.jupiter.api.Assertions.assertEquals(expected.intValue(), resp.statusCode());
    }

    private void ensureApi() {
        if (api == null) {
            String apiToken = new AuthClient().getApiAccessToken();
            this.api = new ApiClient(apiToken);
        }
    }
}