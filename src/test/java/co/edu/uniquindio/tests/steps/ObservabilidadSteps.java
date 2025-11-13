package co.edu.uniquindio.tests.steps;

import co.edu.uniquindio.tests.config.TestConfig;
import co.edu.uniquindio.tests.support.ApiClient;
import co.edu.uniquindio.tests.support.ScenarioContext;
import co.edu.uniquindio.tests.support.TokenClient;
import io.cucumber.java.es.*;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

import static io.restassured.RestAssured.given; // 👈 Asegúrate de tener esta importación
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@Slf4j
public class ObservabilidadSteps {

    private final TestConfig config;
    private final ApiClient apiClient;
    private final TokenClient tokenClient;
    private String userToken;

    public ObservabilidadSteps() {
        this.config = TestConfig.getInstance();
        this.apiClient = ApiClient.getInstance();
        this.tokenClient = TokenClient.getInstance();
        // Usamos el token del administrador para la prueba de métricas
        this.userToken = tokenClient.getAdminToken();
    }

    @Dado("que los servicios del sistema están desplegados")
    public void serviciosDesplegados() {
        Response healthCheck = apiClient.get(config.getHealthEndpoint());
        ScenarioContext.setResponse(healthCheck);
        assertThat("Servicio disponible", healthCheck.statusCode(), is(200));
        log.info("✓ Servicios desplegados");
    }

    @Cuando("envío una solicitud al endpoint de health")
    public void envioUnaSolicitudAlEndpointDeHealth() {
        // El paso 'Dado que los servicios del sistema están desplegados' ya hace la llamada
        // al endpoint /actuator/health y guarda la respuesta.
        // Si quieres forzar una nueva llamada aquí:
        Response healthCheck = apiClient.get(config.getHealthEndpoint());
        ScenarioContext.setResponse(healthCheck);
    }

    @Entonces("la respuesta debe indicar status UP")
    public void laRespuestaDebeIndicarStatusUP() {
        Response response = ScenarioContext.getResponse();
        // Verifica que el campo 'status' en la raíz del JSON sea "UP"
        response.then().body("status", is("UP"));
        log.info("✓ Health Status UP");
    }

    @Cuando("envío una solicitud al endpoint de métricas")
    public void solicitudMetricas() {
        // ====================================================================
        // FIX: La API de Prometheus requiere el header Accept: text/plain
        // para evitar el error 406 Not Acceptable.
        // ====================================================================
        Response apiResponse = given()
                .header("Authorization", "Bearer " + userToken)
                // Solicitamos el tipo de contenido estándar de Prometheus
                .header("Accept", "text/plain;version=0.0.4;charset=utf-8")
                .when()
                .get(config.getMetricsEndpoint())
                .then()
                .extract().response();

        ScenarioContext.setResponse(apiResponse);
        log.info("✓ Solicitud Métricas enviada");
    }

    @Entonces("la respuesta debe estar en formato Prometheus")
    public void formatoPrometheus() {
        Response responseFromContext = ScenarioContext.getResponse();
        // Verificar que el contenido sea texto plano de métricas
        assertThat("Formato Prometheus (texto plano)", responseFromContext.contentType(), containsString("text/plain"));
        // Buscar una métrica común para verificar el contenido
        assertThat("Contenido de Métricas (ej. JVM)", responseFromContext.asString(), containsString("jvm_memory_used_bytes"));
        log.info("✓ Formato Prometheus OK");
    }
}