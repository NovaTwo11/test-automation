package co.edu.uniquindio.tests.support;

import io.restassured.response.Response;

import java.util.Map;
import java.util.HashMap;


/**
 * Contexto compartido simple para los escenarios de Cucumber.
 * Implementado con ThreadLocal para uso seguro en concurrencia.
 */
public class ScenarioContext {
    private static final ThreadLocal<Response> responseHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> accessTokenHolder = new ThreadLocal<>();
    // Almacena datos clave-valor (ej. "currentPassword", "newUserId") para el escenario
    private static final ThreadLocal<Map<String, Object>> contextHolder =
            ThreadLocal.withInitial(HashMap::new);

    public static void setResponse(Response response) {
        responseHolder.set(response);
    }


    public static Response getResponse() {
        return responseHolder.get();
    }

    public static void store(String key, Object value) {
        contextHolder.get().put(key, value);
    }

    // Método genérico para obtener un valor
    public static <T> T get(String key) {
        return (T) contextHolder.get().get(key);
    }

    // Método específico para obtener la respuesta como String, si es necesario
    public static String getResponseString() {
        Response response = responseHolder.get();
        return response != null ? response.asString() : null;
    }

    public static void clearResponse() {
        responseHolder.remove();
    }


    public static void setAccessToken(String token) {
        accessTokenHolder.set(token);
    }


    public static String getAccessToken() {
        return accessTokenHolder.get();
    }


    public static void clearAll() {
        responseHolder.remove();
        contextHolder.remove();
        accessTokenHolder.remove();
    }
}