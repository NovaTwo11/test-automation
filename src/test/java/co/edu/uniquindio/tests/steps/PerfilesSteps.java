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
    private String userToken;
    private UsersData.ProfileTestData profileData;
    private Map<String, Object> profileMap;
    private String userId;

    public PerfilesSteps() {
        this.config = TestConfig.getInstance();
        this.apiClient = ApiClient.getInstance();
        this.tokenClient = TokenClient.getInstance();
    }

    @Dado("que el servicio de perfiles está disponible")
    public void servicioPerfilesDisponible() {
        Response healthCheck = apiClient.get(config.getHealthEndpoint());
        ScenarioContext.setResponse(healthCheck); // Guardar respuesta
        assertThat("Servicio disponible", healthCheck.statusCode(), is(200));
        log.info("✓ Servicio perfiles OK");
    }

    @Dado("que tengo un usuario autenticado en el sistema")
    public void usuarioAutenticado() {
        this.userToken = tokenClient.getAdminToken();
        assertThat("Token", userToken, notNullValue());
        log.info("✓ Usuario autenticado");
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

        // La respuesta de esta acción se usa para obtener el userId, NO para validaciones posteriores
        Response creationResponse = apiClient.postAuthenticated(
                config.getProfilesEndpoint(), profileMap, userToken);

        // Guardar la respuesta de la creación en el contexto, aunque no se use en el 'Entonces' siguiente.
        ScenarioContext.setResponse(creationResponse);

        if (creationResponse.statusCode() == 201) {
            userId = creationResponse.jsonPath().getString("userId"); // Asegurarse de leer userId
            log.info("✓ Perfil creado");
        }
    }

    @Dado("que tengo nuevos datos para actualizar el perfil")
    public void nuevosDatosActualizarPerfil() {
        profileData = UsersData.generateRandomProfile();
        profileMap = UsersData.profileToMap(profileData);
        log.info("✓ Datos actualización");
    }

    @Cuando("envío una solicitud para crear el perfil")
    public void crearPerfil() {
        Response apiResponse = apiClient.postAuthenticated(
                config.getProfilesEndpoint(), profileMap, userToken);
        ScenarioContext.setResponse(apiResponse); // Guardar respuesta
        log.info("✓ Crear perfil");
    }

    @Cuando("envío una solicitud para obtener mi perfil")
    public void obtenerPerfil() {
        Response apiResponse = apiClient.getAuthenticated(
                config.getProfilesEndpoint(), userToken);
        ScenarioContext.setResponse(apiResponse); // Guardar respuesta
        log.info("✓ Obtener perfil");
    }

    @Cuando("envío una solicitud para actualizar mi perfil")
    public void actualizarPerfil() {
        Response apiResponse = apiClient.putAuthenticated(
                config.getProfilesEndpoint(), profileMap, userToken);
        ScenarioContext.setResponse(apiResponse); // Guardar respuesta
        log.info("✓ Actualizar perfil");
    }

    // ====================================================================
    // FIX: El JSON de respuesta tiene 'userId', no 'id'.
    // ====================================================================
    @Entonces("el perfil debe ser creado exitosamente")
    public void perfilCreadoExitosamente() {
        Response responseFromContext = ScenarioContext.getResponse();
        // Leer respuesta
        String id = responseFromContext.jsonPath().getString("userId"); // <-- CAMBIO DE 'id' a 'userId'
        assertThat("Perfil creado", id, notNullValue());
        log.info("✓ Perfil ID: {}", id);
    }

    @Entonces("la respuesta debe cumplir con el esquema de perfil")
    public void respuestaCumpleEsquemaPerfil() {
        Response responseFromContext = ScenarioContext.getResponse();
        // Leer respuesta
        assertThat("Tiene apodo", responseFromContext.jsonPath().get("apodo"), notNullValue());
        log.info("✓ Esquema perfil OK");
    }

    @Entonces("debo recibir los datos de mi perfil")
    public void datosDelPerfil() {
        Response responseFromContext = ScenarioContext.getResponse();
        // Leer respuesta
        assertThat("Datos perfil", responseFromContext.jsonPath().get("apodo"), notNullValue());
        log.info("✓ Datos perfil OK");
    }

    @Entonces("el perfil debe actualizarse con los nuevos datos")
    public void perfilActualizado() {
        Response responseFromContext = ScenarioContext.getResponse();
        // Leer respuesta
        String apodo = responseFromContext.jsonPath().getString("apodo");
        assertThat("Actualizado", apodo, notNullValue());
        log.info("✓ Perfil actualizado");
    }
}