package co.edu.uniquindio.tests.steps;

import co.edu.uniquindio.tests.support.ApiClient;
import co.edu.uniquindio.tests.support.AuthClient;
import com.github.javafaker.Faker;
import io.cucumber.java.en.*;
import io.restassured.response.Response;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordResetSteps {

    private ApiClient api;
    private String userEmail;
    private String resetToken;
    private Response forgotResponse;
    private Response confirmResponse;
    private final Faker faker = new Faker(new Locale("es"));

    @Given("que existe un usuario con email {string}")
    public void queExisteUnUsuarioConEmail(String email) {
        // Email único para evitar choques
        String uniqueSuffix = System.getProperty("test.uniqueEmail", String.valueOf(System.currentTimeMillis()));
        this.userEmail = email.replace("@", "+" + uniqueSuffix + "@");
        System.out.println("[Given] Email efectivo para la prueba: " + this.userEmail);

        String apiToken = new AuthClient().getApiAccessToken();
        this.api = new ApiClient(apiToken);

        String nombre = faker.name().fullName();
        Response reg = api.createUser(nombre, this.userEmail, "Temporal#123");
        int sc = reg.getStatusCode();
        String body = reg.getBody().asString();
        System.out.println("[Given] Crear usuario -> status=" + sc + " body=" + body);

        assertTrue(sc == 201 || sc == 200 || sc == 409,
                "Fallo creando usuario en la API. Status=" + sc + " Body=" + body);
    }

    @When("solicito el reseteo de contraseña para ese usuario")
    public void solicitoElReseteoDeContrasenaParaEseUsuario() {
        forgotResponse = api.requestPasswordReset(userEmail);
        int status = forgotResponse.getStatusCode();
        String body = forgotResponse.getBody().asString();
        System.out.println("[When] Forgot-password -> status=" + status + " body=" + body);
        assertEquals(200, status, "Fallo solicitud de reset. Status=" + status + " Body=" + body);

        this.resetToken = api.extractResetTokenFromForgotResponse(forgotResponse);
        System.out.println("[When] Token extraído: " + (resetToken == null ? "NULL" : resetToken));
        assertNotNull(resetToken, "No se pudo extraer el token de la respuesta de forgot-password");
        assertFalse(resetToken.isBlank(), "Token vacío");
    }

    @When("cambio la contraseña a {string} usando el token de reseteo")
    public void cambioLaContrasenaAUsandoElTokenDeReseteo(String nuevaPassword) {
        confirmResponse = api.confirmPasswordReset(resetToken, nuevaPassword);
        int status = confirmResponse.getStatusCode();
        String body = confirmResponse.getBody().asString();
        System.out.println("[When] Reset-password -> status=" + status + " body=" + body);
    }

    @Then("el reseteo de contraseña debe ser exitoso")
    public void elReseteoDeContrasenaDebeSerExitoso() {
        assertNotNull(confirmResponse, "La respuesta de confirmación no debe ser nula");
        int status = confirmResponse.getStatusCode();
        String body = confirmResponse.getBody().asString();
        System.out.println("[Then] Resultado final -> status=" + status + " body=" + body);
        assertEquals(200, status, "El reseteo debería ser exitoso. Status=" + status + " Body=" + body);
    }
}