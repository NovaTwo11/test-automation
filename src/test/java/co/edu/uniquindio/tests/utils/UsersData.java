package co.edu.uniquindio.tests.utils;

import com.github.javafaker.Faker;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UsersData {

    private static final Faker faker = new Faker(new Locale("es"));

    public static UserTestData generateRandomUser() {
        String firstName = faker.name().firstName();
        String lastName = faker.name().lastName();
        String username = generateUsername(firstName, lastName);

        return UserTestData.builder()
                .username(username)
                .email(faker.internet().emailAddress(username))
                .password(generateStrongPassword())
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }

    public static ProfileTestData generateRandomProfile() {
        return ProfileTestData.builder()
                .apodo(faker.funnyName().name())
                .biografia(faker.lorem().paragraph())
                .paginaPersonal("https://" + faker.internet().domainName())
                .direccion(faker.address().fullAddress())
                .organizacion(faker.company().name())
                .paisResidencia(faker.address().country())
                .informacionPublica(faker.bool().bool())
                .twitter("@" + faker.name().username())
                .linkedin("linkedin.com/in/" + faker.name().username())
                .github("github.com/" + faker.name().username())
                .build();
    }

    private static String generateUsername(String firstName, String lastName) {
        String base = firstName.toLowerCase() + "." + lastName.toLowerCase();
        String clean = base.replaceAll("[^a-z.]", "");
        return clean + faker.number().numberBetween(1, 999);
    }

    private static String generateStrongPassword() {
        return faker.internet().password(8, 16, true, true, true);
    }

    public static String generateEmail() {
        return faker.internet().emailAddress();
    }

    public static String generatePhone() {
        return faker.phoneNumber().cellPhone();
    }

    public static Map<String, Object> userToMap(UserTestData user) {
        Map<String, Object> map = new HashMap<>();
        // El backend espera "nombre" no "firstName/lastName"
        String nombreCompleto = user.getFirstName() + " " + user.getLastName();
        map.put("nombre", nombreCompleto);
        map.put("email", user.getEmail());
        map.put("password", user.getPassword());
        return map;
    }

    public static Map<String, Object> profileToMap(ProfileTestData profile) {
        Map<String, Object> map = new HashMap<>();
        map.put("apodo", profile.getApodo());
        map.put("biografia", profile.getBiografia());
        map.put("paginaPersonal", profile.getPaginaPersonal());
        map.put("direccion", profile.getDireccion());
        map.put("organizacion", profile.getOrganizacion());
        map.put("paisResidencia", profile.getPaisResidencia());
        map.put("informacionPublica", profile.getInformacionPublica());

        Map<String, String> redesSociales = new HashMap<>();
        redesSociales.put("twitter", profile.getTwitter());
        redesSociales.put("linkedin", profile.getLinkedin());
        redesSociales.put("github", profile.getGithub());
        map.put("redesSociales", redesSociales);

        return map;
    }

    @Data
    @Builder
    public static class UserTestData {
        private String username;
        private String email;
        private String password;
        private String firstName;
        private String lastName;
    }

    @Data
    @Builder
    public static class ProfileTestData {
        private String apodo;
        private String biografia;
        private String paginaPersonal;
        private String direccion;
        private String organizacion;
        private String paisResidencia;
        private Boolean informacionPublica;
        private String twitter;
        private String linkedin;
        private String github;
    }
}
