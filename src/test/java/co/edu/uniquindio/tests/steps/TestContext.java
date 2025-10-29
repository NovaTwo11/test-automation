package co.edu.uniquindio.tests.steps;

import co.edu.uniquindio.tests.utils.UsersData;
import io.restassured.response.Response;

public class TestContext {
    public UsersData.User user;
    public String createdUserId;
    public Response lastResponse;
}