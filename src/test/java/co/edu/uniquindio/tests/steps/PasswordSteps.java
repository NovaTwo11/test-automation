package co.edu.uniquindio.tests.steps;

import co.edu.uniquindio.tests.config.TestConfig;
import co.edu.uniquindio.tests.support.ApiClient;
import co.edu.uniquindio.tests.support.ScenarioContext;
import co.edu.uniquindio.tests.support.TokenClient;
import co.edu.uniquindio.tests.utils.UsersData; // Importar UsersData
import io.cucumber.java.es.*;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Slf4j
public class PasswordSteps {

    // Claves para el Contexto
    private static final String TEMP_USER_TOKEN = "tempUserToken";
    private static final String TEMP_USER_EMAIL = "tempUserEmail";
    private static final String CURRENT_PASS = "currentPassword";
    private static final String NEW_PASS = "newPassword";

    private final TestConfig config;
    private final ApiClient apiClient;
    private final TokenClient tokenClient;

    // Constructor (como el original)
    public PasswordSteps() {
        this.config = TestConfig.getInstance();
        this.apiClient = ApiClient.getInstance();
        this.tokenClient = TokenClient.getInstance();
    }

    @Dado("que el servicio de gestión de contraseñas está disponible")
    public void servicioPasswordDisponible() {
        Response healthCheck = apiClient.get(config.getHealthEndpoint());
        ScenarioContext.setResponse(healthCheck); // Guardar respuesta
        assertThat("Servicio OK", healthCheck.statusCode(), is(200));
        log.info("✓ Servicio password OK");
    }

    // --- NUEVOS PASOS ---

    @Dado("que creo un usuario temporal para la prueba de contraseña")
    public void creo_un_usuario_temporal_para_la_prueba_de_contraseña() {
        // 1. Obtener token de admin para crear el usuario
        String adminToken = tokenClient.getAdminToken();

        // 2. Generar datos de usuario
        UsersData.UserTestData tempUser = UsersData.generateRandomUser();
        Map<String, Object> userData = UsersData.userToMap(tempUser);

        // 3. Crear el usuario vía API (esto lo guarda en la DB local y en Keycloak)
        Response createResponse = apiClient.postAuthenticated(
                config.getUsersEndpoint(),
                userData,
                adminToken
        );

        // 4. Verificar que se creó (201)
        assertThat("El usuario temporal debe crearse", createResponse.statusCode(), is(201));

        // 5. Guardar los datos en el contexto para los siguientes pasos
        ScenarioContext.store(TEMP_USER_EMAIL, tempUser.getEmail());
        ScenarioContext.store(CURRENT_PASS, tempUser.getPassword());

        log.info("✓ Usuario temporal creado: {}", tempUser.getEmail());
    }

    @Dado("que inicio sesión como el usuario temporal para obtener su token")
    public void inicio_sesión_como_el_usuario_temporal_para_obtener_su_token() {
        String email = ScenarioContext.get(TEMP_USER_EMAIL);
        String password = ScenarioContext.get(CURRENT_PASS);

        // 1. Obtener el token del *nuevo* usuario
        // Limpiamos la caché por si acaso, para forzar un nuevo login
        tokenClient.clearCache();
        String userToken = tokenClient.getToken(email, password);
        assertThat("Token del usuario temporal no debe ser nulo", userToken, notNullValue());

        // 2. Guardar el token en el contexto
        ScenarioContext.store(TEMP_USER_TOKEN, userToken);

        log.info("✓ Sesión iniciada como usuario temporal");
    }

    @Dado("que tengo la contraseña original del usuario temporal")
    public void tengo_la_contraseña_original_del_usuario_temporal() {
        String currentPass = ScenarioContext.get(CURRENT_PASS);
        assertThat("Contraseña original debe existir en contexto", currentPass, notNullValue());
        log.info("✓ Contraseña original lista");
    }

    @Dado("que tengo una nueva contraseña válida para el usuario temporal")
    public void tengo_una_nueva_contraseña_válida_para_el_usuario_temporal() {
        String newPassword = "NuevaPasswordSuperSegura123!";
        ScenarioContext.store(NEW_PASS, newPassword);
        log.info("✓ Nueva contraseña lista");
    }

    @Cuando("envío una solicitud para cambiar la contraseña con el token del usuario temporal")
    public void envío_una_solicitud_para_cambiar_la_contraseña_con_el_token_del_usuario_temporal() {
        String userToken = ScenarioContext.get(TEMP_USER_TOKEN);
        String currentPassword = ScenarioContext.get(CURRENT_PASS);
        String newPassword = ScenarioContext.get(NEW_PASS);

        Map<String, String> data = new HashMap<>();
        data.put("currentPassword", currentPassword);
        data.put("newPassword", newPassword);

        Response apiResponse = apiClient.putAuthenticated(
                config.getPasswordEndpoint(), data, userToken);
        ScenarioContext.setResponse(apiResponse); // Guardar respuesta
        log.info("✓ Solicitud de cambio de contraseña enviada");
    }

    @Entonces("el usuario temporal puede iniciar sesión con la nueva contraseña")
    public void el_usuario_temporal_puede_iniciar_sesión_con_la_nueva_contraseña() {
        String email = ScenarioContext.get(TEMP_USER_EMAIL);
        String newPassword = ScenarioContext.get(NEW_PASS);

        // Forzar al TokenClient a no usar caché
        tokenClient.clearCache();

        // Intentar obtener token con la *nueva* contraseña
        String newToken = tokenClient.getToken(email, newPassword);

        assertThat("Se debe obtener un nuevo token con la nueva contraseña", newToken, notNullValue());
        log.info("✓ Login con nueva contraseña exitoso");
    }

    // --- Pasos para el escenario negativo ---

    @Dado("que proporciono una contraseña actual incorrecta para el usuario temporal")
    public void proporciono_una_contraseña_actual_incorrecta_para_el_usuario_temporal() {
        // Sobreescribimos la contraseña actual en el contexto *solo* para esta prueba
        ScenarioContext.store(CURRENT_PASS, "PasswordIncorrecta123!");
        log.info("✓ Contraseña actual incorrecta preparada");
    }

    // --- PASOS ORIGINALES (REUTILIZADOS) ---

    @Entonces("debo recibir un mensaje de confirmación")
    public void mensajeConfirmacion() {
        Response responseFromContext = ScenarioContext.getResponse();
        String message = responseFromContext.jsonPath().getString("mensaje");
        assertThat("Mensaje", message, notNullValue());
        assertThat("Mensaje de éxito", message, containsStringIgnoringCase("actualizada"));
        log.info("✓ Confirmación OK");
    }

    @Entonces("debo recibir un mensaje indicando contraseña actual incorrecta")
    public void mensajePasswordIncorrecta() {
        Response responseFromContext = ScenarioContext.getResponse();
        String message = responseFromContext.jsonPath().getString("mensaje");
        assertThat("Mensaje error", message, notNullValue());
        assertThat("Mensaje de error", message, containsStringIgnoringCase("incorrecta"));
        log.info("✓ Mensaje de contraseña incorrecta OK");
    }

    // --- PASOS ANTIGUOS (YA NO SE USAN EN LOS NUEVOS ESCENARIOS) ---
    // (Los mantenemos por si otros features los usan, pero marcados como obsoletos)

    @Dado("que tengo un usuario autenticado")
    public void usuarioAutenticado() {
        String adminToken = tokenClient.getAdminToken();
        ScenarioContext.store(TEMP_USER_TOKEN, adminToken);
        ScenarioContext.store(CURRENT_PASS, config.getAdminPassword());
        log.warn("✓ Usando 'usuario autenticado' (admin) - Este paso está obsoleto, usar los nuevos Gherkin");
    }

    @Dado("que tengo la contraseña actual correcta")
    public void passwordActualCorrecta() {
        log.debug("✓ Paso 'contraseña actual correcta' (obsoleto) ejecutado");
    }

    @Dado("que tengo una nueva contraseña válida")
    public void nuevaPasswordValida() {
        ScenarioContext.store(NEW_PASS, "NewPassword123!");
        log.debug("✓ Paso 'nueva contraseña válida' (obsoleto) ejecutado");
    }

    @Dado("que proporciono una contraseña actual incorrecta")
    public void passwordActualIncorrecta() {
        ScenarioContext.store(CURRENT_PASS, "PasswordIncorrecto123!");
        log.warn("✓ Usando 'contraseña actual incorrecta' - Este paso está obsoleto");
    }

    @Cuando("envío una solicitud para cambiar la contraseña")
    public void cambiarPassword() {
        envío_una_solicitud_para_cambiar_la_contraseña_con_el_token_del_usuario_temporal();
        log.warn("✓ Usando 'envío una solicitud para cambiar la contraseña' - Este paso está obsoleto");
    }
}