package co.edu.uniquindio.tests.steps;

import co.edu.uniquindio.tests.support.ApiClient;
import co.edu.uniquindio.tests.support.AuthClient;
import co.edu.uniquindio.tests.utils.UsersData;
import io.cucumber.java.en.*;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

public class UserCreateSteps {

    private final TestContext context;
    private ApiClient api;

    public UserCreateSteps(TestContext context) {
        this.context = context;
    }

    @Given("que genero un usuario aleatorio válido")
    public void generoUsuarioValido() {
        context.user = UsersData.randomUser();
        assertNotNull(context.user);
    }

    @And("creo el usuario vía API")
    public void creoUsuario() {
        ensureApi();
        Response resp = api.createUser(context.user.nombre(), context.user.email(), context.user.password());
        context.lastResponse = resp;
        System.out.println("[Create] " + resp.statusCode() + " " + resp.asString());
        assertThat(resp.statusCode(), anyOf(is(201), is(200), is(409)));

        if (resp.statusCode() == 201 || resp.statusCode() == 200) {
            String id = null;
            try {
                id = resp.jsonPath().getString("usuario.id");
                if (id == null || id.isBlank()) id = resp.jsonPath().getString("id");
            } catch (Exception ignored) {}
            assertNotNull(id, "No se pudo extraer el id del usuario creado");
            context.createdUserId = id;
        }
    }

    @And("creo el usuario vía API \\(idempotente)")
    public void creoUsuarioIdempotente() {
        ensureApi();
        Response resp = api.createUser(context.user.nombre(), context.user.email(), context.user.password());
        context.lastResponse = resp;
        System.out.println("[Create-Idempotent] " + resp.statusCode() + " " + resp.asString());
        assertThat(resp.statusCode(), anyOf(is(201), is(200), is(409)));
        if (resp.statusCode() == 201 || resp.statusCode() == 200) {
            String id = null;
            try {
                id = resp.jsonPath().getString("usuario.id");
                if (id == null || id.isBlank()) id = resp.jsonPath().getString("id");
            } catch (Exception ignored) {}
            assertNotNull(id, "No se pudo extraer el id del usuario creado");
            context.createdUserId = id;
        }
    }

    @When("intento crear el mismo usuario de nuevo")
    public void intentoCrearElMismoUsuarioDeNuevo() {
        assertNotNull(context.user, "No hay usuario en contexto");
        ensureApi();
        Response resp = api.createUser(context.user.nombre(), context.user.email(), context.user.password());
        context.lastResponse = resp;
        System.out.println("[Create-Duplicate] " + resp.statusCode() + " " + resp.asString());
    }

    @Then("la creación responde {int} y cumple el esquema de usuario creado")
    public void laCreacionRespondeYCumpleEsquema(Integer expected) {
        assertNotNull(context.lastResponse, "No hay respuesta de creación");
        int status = context.lastResponse.statusCode();
        String body = context.lastResponse.asString();
        if (!expected.equals(status)) {
            System.err.println("[Create] Status inesperado. Body: " + body);
        }
        assertEquals(expected.intValue(), status, "Status inesperado al crear");

        // Validar contra el schema
        context.lastResponse.then()
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/usuario_creado_schema.json"));
    }

    @Then("la creación responde {int} conflicto")
    public void laCreacionRespondeConflicto(Integer expected) {
        assertNotNull(context.lastResponse, "No hay respuesta de creación duplicada");
        int status = context.lastResponse.statusCode();
        String body = context.lastResponse.asString();
        if (!expected.equals(status)) {
            System.err.println("[Create-Duplicate] Status inesperado. Body: " + body);
        }
        assertEquals(expected.intValue(), status, "Status inesperado en duplicado");
    }

    private void ensureApi() {
        if (api == null) {
            String apiToken = new AuthClient().getApiAccessToken();
            this.api = new ApiClient(apiToken);
        }
    }
}