package co.edu.uniquindio.tests.steps;

import co.edu.uniquindio.tests.config.TestConfig;
import co.edu.uniquindio.tests.support.ApiClient;
import co.edu.uniquindio.tests.support.ScenarioContext;
import co.edu.uniquindio.tests.support.TokenClient;
import co.edu.uniquindio.tests.utils.UsersData;
import io.cucumber.java.es.*;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Slf4j
public class PerfilesSteps {

    private final TestConfig config;
    private final ApiClient apiClient;
    private final TokenClient tokenClient;

    // ⬇️ --- MODIFICADO --- ⬇️
    // Cambiamos userId por adminToken.
    // Necesitamos el token para que el Gateway pueda extraer el 'sub' (userId)
    private String adminToken;
    // ⬆️ --- FIN DE LA MODIFICACIÓN --- ⬆️

    private UsersData.ProfileTestData profileData;
    private Map<String, Object> profileMap;

    public PerfilesSteps() {
        this.config = TestConfig.getInstance();
        this.apiClient = ApiClient.getInstance();
        this.tokenClient = TokenClient.getInstance();
    }

    @Dado("que el servicio de perfiles está disponible")
    public void servicioPerfilesDisponible() {
        // ⬇️ --- MODIFICADO --- ⬇️
        // Hacemos ping al endpoint de health del API Gateway (o del servicio principal)
        // ya que es un prerrequisito para que la ruta de perfiles funcione.
        Response healthCheck = apiClient.get(config.getHealthEndpoint());
        ScenarioContext.setResponse(healthCheck); // Guardar respuesta
        assertThat("API Gateway (prerrequisito) disponible", healthCheck.statusCode(), is(200));
        log.info("✓ Prerrequisito (API Gateway) OK");
        // ⬆️ --- FIN DE LA MODIFICACIÓN --- ⬆️
    }

    @Dado("que tengo un usuario autenticado en el sistema")
    public void usuarioAutenticado() {
        // ⬇️ --- MODIFICADO --- ⬇️
        // Obtenemos el Token de admin, no el UserID
        this.adminToken = tokenClient.getAdminToken();
        assertThat("Token de admin", adminToken, notNullValue());
        log.info("✓ Usuario autenticado (token obtenido)");
        // ⬆️ --- FIN DE LA MODIFICACIÓN --- ⬆️
    }

    @Dado("que tengo datos válidos de un perfil nuevo")
    public void datosValidosPerfil() {
        this.profileData = UsersData.generateRandomProfile();
        this.profileMap = UsersData.profileToMap(profileData);
        log.info("✓ Datos perfil generados");
    }

    @Dado("que el usuario tiene un perfil creado")
    public void perfilCreado() {
        profileData = UsersData.generateRandomProfile();
        profileMap = UsersData.profileToMap(profileData);

        // ⬇️ --- MODIFICADO --- ⬇️
        // Usamos el método autenticado estándar (con token)
        Response creationResponse = apiClient.putAuthenticated(
                config.getProfilesEndpoint(), profileMap, adminToken);
        // ⬆️ --- FIN DE LA MODIFICACIÓN --- ⬆️

        ScenarioContext.setResponse(creationResponse);
        if (creationResponse.statusCode() == 200) { // Go devuelve 200 para PUT
            log.info("✓ Perfil creado/actualizado para el step");
        }
    }

    @Dado("que tengo nuevos datos para actualizar el perfil")
    public void nuevosDatosActualizarPerfil() {
        profileData = UsersData.generateRandomProfile();
        this.profileMap = UsersData.profileToMap(profileData);
        log.info("✓ Datos actualización");
    }

    @Cuando("envío una solicitud para crear el perfil")
    public void crearPerfil() {
        // ⬇️ --- MODIFICADO --- ⬇️
        // Usamos el método autenticado estándar (con token)
        Response apiResponse = apiClient.putAuthenticated(
                config.getProfilesEndpoint(), profileMap, adminToken);
        ScenarioContext.setResponse(apiResponse); // Guardar respuesta
        log.info("✓ Crear/Actualizar perfil (enviando PUT a Gateway)");
        // ⬆️ --- FIN DE LA MODIFICACIÓN --- ⬆️
    }

    @Cuando("envío una solicitud para obtener mi perfil")
    public void obtenerPerfil() {
        // ⬇️ --- MODIFICADO --- ⬇️
        // Usamos el método autenticado estándar (con token)
        Response apiResponse = apiClient.getAuthenticated(
                config.getProfilesEndpoint(), adminToken);
        ScenarioContext.setResponse(apiResponse); // Guardar respuesta
        log.info("✓ Obtener perfil (vía Gateway)");
        // ⬆️ --- FIN DE LA MODIFICACIÓN --- ⬆️
    }

    @Cuando("envío una solicitud para actualizar mi perfil")
    public void actualizarPerfil() {
        // ⬇️ --- MODIFICADO --- ⬇️
        // Usamos el método autenticado estándar (con token)
        Response apiResponse = apiClient.putAuthenticated(
                config.getProfilesEndpoint(), profileMap, adminToken);
        ScenarioContext.setResponse(apiResponse); // Guardar respuesta
        log.info("✓ Actualizar perfil (vía Gateway)");
        // ⬆️ --- FIN DE LA MODIFICACIÓN --- ⬆️
    }

    @Entonces("el perfil debe ser creado exitosamente")
    public void perfilCreadoExitosamente() {
        Response responseFromContext = ScenarioContext.getResponse();
        String id = responseFromContext.jsonPath().getString("userId");
        assertThat("Perfil creado (userId no nulo)", id, notNullValue());

        // ⬇️ --- MODIFICADO --- ⬇️
        // Verificamos que el ID del perfil coincida con el ID del token que usamos.
        String expectedUserId = tokenClient.getAdminUserId(); // Obtenemos el ID real del token
        assertThat("ID coincide con el token", id, is(expectedUserId));
        log.info("✓ Perfil ID: {}", id);
        // ⬆️ --- FIN DE LA MODIFICACIÓN --- ⬆️
    }

    @Entonces("la respuesta debe cumplir con el esquema de perfil")
    public void respuestaCumpleEsquemaPerfil() {
        Response responseFromContext = ScenarioContext.getResponse();
        assertThat("Tiene apodo", responseFromContext.jsonPath().get("apodo"), notNullValue());
        log.info("✓ Esquema perfil OK");
    }

    @Entonces("debo recibir los datos de mi perfil")
    public void datosDelPerfil() {
        Response responseFromContext = ScenarioContext.getResponse();
        assertThat("Datos perfil", responseFromContext.jsonPath().get("apodo"), notNullValue());
        log.info("✓ Datos perfil OK");
    }

    @Entonces("el perfil debe actualizarse con los nuevos datos")
    public void perfilActualizado() {
        Response responseFromContext = ScenarioContext.getResponse();
        String apodo = responseFromContext.jsonPath().getString("apodo");
        assertThat("Actualizado", apodo, notNullValue());
        log.info("✓ Perfil actualizado");
    }
}