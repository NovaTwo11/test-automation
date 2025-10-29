package co.edu.uniquindio.tests.runner;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.*;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(
        key   = PLUGIN_PROPERTY_NAME,
        value = "pretty, summary, json:target/cucumber.json, html:target/cucumber-report.html, io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm")
@ConfigurationParameter(
        key   = GLUE_PROPERTY_NAME,
        value = "co.edu.uniquindio.tests.steps,co.edu.uniquindio.tests.hooks")
@ConfigurationParameter(
        key   = SNIPPET_TYPE_PROPERTY_NAME,
        value = "camelcase")
public class CucumberTestRunner { }