package co.edu.uniquindio.tests.steps;

import co.edu.uniquindio.tests.support.ObservabilityClient;
import io.cucumber.java.en.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ObservabilitySteps {

    private final TestContext context;
    private final ObservabilityClient client;
    private Response healthResponse;

    // Inyección de dependencias de Cucumber (comparte 'context')
    public ObservabilitySteps(TestContext context) {
        this.context = context;
        this.client = new ObservabilityClient();
    }

    // --- Pasos para el Health Check (Reto 5.1) ---

    @When("consulto el endpoint de salud de la API")
    public void consultoElEndpointDeSaludDeLaAPI() {
        healthResponse = RestAssured.given()
                .baseUri(co.edu.uniquindio.tests.config.TestConfig.API_BASE_URL)
                .when()
                .get("/actuator/health")
                .then()
                .extract().response();
        System.out.println("[Health Check] -> " + healthResponse.statusCode());
        System.out.println(healthResponse.asString());
    }

    @Then("el estado debe ser {string}")
    public void elEstadoDebeSer(String expectedStatus) {
        healthResponse.then().body("status", equalTo(expectedStatus));
    }

    // --- Pasos para Integración (Reto 5.2) ---

    @And("guardo el contador de peticiones POST a {string}")
    public void guardoElContadorDePeticionesPOST(String uri) {
        // Query de Prometheus para contar las peticiones POST a esa URI
        // (Asegúrate que el 'job' coincida con tu prometheus.yml)
        String promQuery = String.format("http_server_requests_seconds_count{job=\"taller-api\", method=\"POST\", uri=\"%s\"}", uri);

        Response r = client.queryPrometheus(promQuery);
        context.initialMetricCount = client.parsePrometheusCount(r);

        System.out.printf("[Prometheus] Conteo inicial para POST %s: %d%n", uri, context.initialMetricCount);
    }

    @And("espero {int} segundos para que Prometheus actualice")
    public void esperoParaPrometheus(int seconds) throws InterruptedException {
        System.out.printf("--- Esperando %d segundos (Prometheus scrape_interval) ---%n", seconds);
        Thread.sleep(seconds * 1000L);
    }

    @And("espero {int} segundos para que Loki actualice")
    public void esperoParaLoki(int seconds) throws InterruptedException {
        System.out.printf("--- Esperando %d segundos (Loki ingestion) ---%n", seconds);
        Thread.sleep(seconds * 1000L);
    }

    @And("el contador de peticiones POST a {string} debe incrementar")
    public void elContadorDebeIncrementar(String uri) {
        String promQuery = String.format("http_server_requests_seconds_count{job=\"taller-api\", method=\"POST\", uri=\"%s\"}", uri);

        Response r = client.queryPrometheus(promQuery);
        int newMetricCount = client.parsePrometheusCount(r);

        System.out.printf("[Prometheus] Conteo final para POST %s: %d (Inicial: %d)%n", uri, newMetricCount, context.initialMetricCount);

        assertThat("La métrica de peticiones POST no incrementó",
                newMetricCount, greaterThan(context.initialMetricCount));
    }

    @And("el log de creación debe existir en Loki para el usuario")
    public void elLogDeCreacionDebeExistirEnLoki() {
        // Asumimos que el 'Given' guardó el usuario en el contexto
        String email = context.user.email();

        // (Asegúrate que 'container' coincida con el nombre en tu docker-compose)
        String lokiQuery = String.format("{container=\"tallerapi2_api\"} |~ \"%s\"", email);

        System.out.println("[Loki] Buscando logs para: " + email);
        Response r = client.queryLoki(lokiQuery);
        System.out.println(r.asString());

        // Verificamos que exista una línea de log que contenga el email Y el mensaje de éxito
        boolean found = client.checkLokiLogs(r, email, "Publicando evento de usuario creado:");

        assertTrue(found, "No se encontró el log de 'Usuario creado exitosamente' con el email " + email + " en Loki.");
    }
}