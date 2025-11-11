package co.edu.uniquindio.tests.support;

import co.edu.uniquindio.tests.config.TestConfig;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.hamcrest.Matchers;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ObservabilityClient {

    /**
     * Consulta la API de Prometheus
     * @param promQuery La consulta de PromQL (ej. 'up{job="taller-api"}')
     */
    public Response queryPrometheus(String promQuery) {
        return RestAssured.given()
                .baseUri(TestConfig.PROMETHEUS_BASE_URL)
                .accept("application/json")
                .queryParam("query", promQuery)
                .when()
                .get("/api/v1/query")
                .then()
                .extract().response();
    }

    /**
     * Extrae el primer valor numérico de una respuesta de Prometheus
     */
    public int parsePrometheusCount(Response response) {
        try {
            // Valida que la respuesta sea exitosa y tenga datos
            response.then().statusCode(200).body("status", Matchers.equalTo("success"));
            List<Object> results = response.jsonPath().getList("data.result");

            if (results == null || results.isEmpty()) {
                return 0; // No hay métricas = 0
            }
            // Extrae el valor: "result": [ { "metric": {}, "value": [ 1678886400, "1" ] } ]
            String value = response.jsonPath().getString("data.result[0].value[1]");
            return Integer.parseInt(value);

        } catch (Exception e) {
            System.err.println("Error parseando respuesta de Prometheus: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Consulta la API de Loki
     * @param lokiQuery La consulta de LogQL (ej. '{container="tallerapi2_api"} |~ "error"')
     */
    public Response queryLoki(String lokiQuery) {
        // Busca en los últimos 5 minutos
        long end = Instant.now().toEpochMilli() * 1_000_000; // en nanos
        long start = (Instant.now().toEpochMilli() - (5 * 60 * 1000)) * 1_000_000; // 5m en nanos

        return RestAssured.given()
                .baseUri(TestConfig.LOKI_BASE_URL)
                .accept("application/json")
                .queryParam("query", lokiQuery)
                .queryParam("limit", 10)
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("direction", "backward")
                .when()
                .get("/loki/api/v1/query_range")
                .then()
                .extract().response();
    }

    /**
     * Verifica si un log en Loki contiene todos los textos esperados, navegando correctamente la estructura JSON.
     * La lista real de logs de Loki se encuentra en el JSON Path: data.result.[stream].values.[log_entry].
     */
    public boolean checkLokiLogs(Response lokiResponse, String... expectedTexts) {
        try {
            lokiResponse.then().statusCode(200).body("status", Matchers.equalTo("success"));

            // 1. Obtener la lista de Streams (objetos que contienen los logs)
            // JSON Path: data.result -> Lista de Map<String, Object> (streams)
            List<Map<String, Object>> streams = lokiResponse.jsonPath().getList("data.result");

            if (streams == null || streams.isEmpty()) {
                return false; // No hay streams de logs
            }

            // 2. Iterar sobre cada stream encontrado
            for (Map<String, Object> stream : streams) {
                // 3. Obtener el array 'values' de cada stream.
                // values es una List<List<String>>: [[timestamp, log_message_json], ...]
                List<List<String>> values = (List<List<String>>) stream.get("values");

                if (values != null) {
                    // 4. Iterar sobre las entradas de log (logEntry) en cada stream
                    for (List<String> logEntry : values) {
                        if (logEntry.size() > 1) {
                            String logMessage = logEntry.get(1); // El segundo elemento es el mensaje de log JSON

                            boolean allFound = true;
                            // 5. Verificar si el mensaje de log contiene TODOS los textos esperados
                            for (String expected : expectedTexts) {
                                if (!logMessage.contains(expected)) {
                                    allFound = false;
                                    break;
                                }
                            }
                            if (allFound) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        } catch (Exception e) {
            System.err.println("Error parseando respuesta de Loki: " + e.getMessage());
            return false;
        }
    }
}