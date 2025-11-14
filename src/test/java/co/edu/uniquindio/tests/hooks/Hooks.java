package co.edu.uniquindio.tests.hooks;

import co.edu.uniquindio.tests.config.TestConfig;
import co.edu.uniquindio.tests.support.TokenClient;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.filter.log.LogDetail;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Hooks {

    private final TestConfig config;
    private final TokenClient tokenClient;

    public Hooks() {
        this.config = TestConfig.getInstance();
        this.tokenClient = TokenClient.getInstance();
    }

    @Before
    public void beforeScenario(Scenario scenario) {
        log.info("CONFIG RESUELTA:");
        log.info(" - apiBaseUrl = {}", config.getApiBaseUrl());
        log.info(" - keycloakUrl = {}", config.getKeycloakUrl());
        log.info(" - keycloakTokenUrl = {}", config.getKeycloakTokenUrl());
        // Intenta obtener token admin sin lanzar (solo loguear)
        try {
            String t = tokenClient.getAdminToken();
            log.info(" - admin token obtenido (longitud): {}", t != null ? t.length() : "null");
        } catch (Exception e) {
            log.warn(" - No se pudo obtener token admin en beforeScenario: {}", e.getMessage());
        }

        log.info("=".repeat(80));
        log.info("Iniciando escenario: {}", scenario.getName());
        log.info("Tags: {}", scenario.getSourceTagNames());
        log.info("=".repeat(80));

        RestAssured.baseURI = config.getApiBaseUrl();
        co.edu.uniquindio.tests.support.ScenarioContext.clearAll();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL);

        RestAssured.config = RestAssured.config()
                .logConfig(LogConfig.logConfig()
                        .enableLoggingOfRequestAndResponseIfValidationFails());
    }

    @After
    public void afterScenario(Scenario scenario) {
        log.info("=".repeat(80));
        log.info("Finalizando escenario: {}", scenario.getName());
        log.info("Estado: {}", scenario.getStatus());
        log.info("=".repeat(80));

        if (scenario.isFailed()) {
            log.warn("Escenario falló, limpiando caché de tokens");
            tokenClient.clearCache();
        }
    }

    @Before("@CleanTokenCache")
    public void cleanTokenCache() {
        log.debug("Limpiando caché de tokens por tag @CleanTokenCache");
        tokenClient.clearCache();
    }
}
