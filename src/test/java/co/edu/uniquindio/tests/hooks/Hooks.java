package co.edu.uniquindio.tests.hooks;

import co.edu.uniquindio.tests.config.TestConfig;
import io.cucumber.java.After;
import io.cucumber.java.Before;

public class Hooks {

    @Before(order = 0)
    public void setup() {
        TestConfig.configureRestAssured();
    }

    @After(order = 0)
    public void teardown() {
        // Limpieza si aplica en el futuro
    }
}