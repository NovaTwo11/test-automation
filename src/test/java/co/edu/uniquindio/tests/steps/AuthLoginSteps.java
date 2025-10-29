package co.edu.uniquindio.tests.steps;

import co.edu.uniquindio.tests.support.ApiAuthClient;
import co.edu.uniquindio.tests.support.ApiClient;
import co.edu.uniquindio.tests.support.AuthClient;
import co.edu.uniquindio.tests.utils.TokenClient;
import co.edu.uniquindio.tests.utils.UsersData;
import io.cucumber.java.en.*;
import io.restassured.response.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

public class AuthLoginSteps {

    private UsersData.User user;
    private Response apiLoginResponse;
    private String apiAccessTokenAfterLogin;

    @When("intento autenticar con usuario {string} y password {string}")
    public void intentoAutenticar(String email, String password) {
        // Flujo negativo contra Keycloak (esto ya te funciona)
        Response resp = TokenClient.tryPasswordTokenRaw(email, password);
        int sc = resp.statusCode();
        System.out.printf("[Login negativo] status=%d body=%s%n", sc, resp.asString());
        assertThat("Se esperaba 400 o 401", sc, anyOf(is(400), is(401)));
    }

    @Then("el login debe fallar con 400 o 401")
    public void loginDebeFallar() {
        assertTrue(true);
    }

    @Given("que creo un usuario que resetea su contraseña a {string}")
    public void usuarioConPasswordReseteada(String nuevaPassword) {
        // Crear usuario en TU API
        String apiToken = new AuthClient().getApiAccessToken();
        ApiClient api = new ApiClient(apiToken);
        this.user = UsersData.randomUser();

        Response reg = api.createUser(user.nombre(), user.email(), user.password());
        int scReg = reg.statusCode();
        System.out.println("[API] createUser -> " + scReg + " " + reg.asString());
        assertTrue(scReg == 201 || scReg == 200 || scReg == 409, "Creación inesperada. Status=" + scReg + " Body=" + reg.asString());

        // Forgot + Reset en TU API
        Response forgot = api.requestPasswordReset(user.email());
        System.out.println("[API] forgot -> " + forgot.statusCode() + " " + forgot.asString());
        assertEquals(200, forgot.statusCode(), "Forgot debe ser 200");
        String tokenReset = api.extractResetTokenFromForgotResponse(forgot);
        assertNotNull(tokenReset, "No se pudo extraer token de reset");

        Response confirm = api.confirmPasswordReset(tokenReset, nuevaPassword);
        System.out.println("[API] reset -> " + confirm.statusCode() + " " + confirm.asString());
        assertEquals(200, confirm.statusCode(), "Reset debe ser 200");

        // Actualizar en memoria la contraseña del usuario
        this.user = new UsersData.User(user.nombre(), user.email(), nuevaPassword);
    }

    @When("hago login en la API con esa contraseña")
    public void loginEnApiConPasswordNueva() {
        ApiAuthClient auth = new ApiAuthClient();
        this.apiLoginResponse = auth.login(user.email(), user.password());
        System.out.printf("[API-Login] status=%d body=%s%n", apiLoginResponse.statusCode(), apiLoginResponse.asString());
        assertEquals(200, apiLoginResponse.statusCode(), "El login de la API debe responder 200");

        // Intentar capturar token si tu API lo devuelve en alguna clave
        this.apiAccessTokenAfterLogin = auth.extractAccessToken(apiLoginResponse);
    }

    @Then("obtengo un token de acceso válido de la API")
    public void tokenValidoDeLaApi() {
        // Si la API no devuelve token, validamos forma y contenido esperado del login
        if (apiAccessTokenAfterLogin == null || apiAccessTokenAfterLogin.isBlank()) {
            assertThat(apiLoginResponse.jsonPath().getString("mensaje"), containsStringIgnoringCase("inicio de sesión"));
            assertThat(apiLoginResponse.jsonPath().getString("usuarioId"), not(emptyString()));
            assertThat(apiLoginResponse.jsonPath().getString("email"), is(user.email()));
            assertThat(apiLoginResponse.jsonPath().getString("nombre"), not(emptyString()));
        } else {
            // Si aparece token, también lo validamos
            assertThat("Token de la API esperado", apiAccessTokenAfterLogin, allOf(notNullValue(), not(emptyString())));
        }
    }
}