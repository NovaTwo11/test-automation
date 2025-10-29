package co.edu.uniquindio.tests.utils;

import com.github.javafaker.Faker;

import java.util.Locale;

public class UsersData {
    private static final Faker FAKER = new Faker(new Locale("es", "CO"));

    public record User(String nombre, String email, String password) {}

    public static User randomUser() {
        String nombre = FAKER.name().fullName();
        long ts = System.currentTimeMillis();
        int suffix = FAKER.number().numberBetween(1000, 9999);
        // Evitar '+' en el email (Keycloak search a veces no lo resuelve)
        String email = "user" + ts + "_" + suffix + "@example.com";
        String password = "Temporal#123";
        return new User(nombre, email, password);
    }
}