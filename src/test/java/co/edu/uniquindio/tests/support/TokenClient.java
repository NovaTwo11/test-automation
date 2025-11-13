package co.edu.uniquindio.tests.support;

import co.edu.uniquindio.tests.config.TestConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import io.restassured.response.Response;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap; // Usar ConcurrentHashMap para seguridad en hilos
import java.util.concurrent.ConcurrentMap;

/**
 * Cliente responsable de administrar tokens de acceso, cachearlos
 * y obtener nuevos cuando expiran. Usa AuthClient internamente.
 */
@Slf4j
@Getter
public class TokenClient {

    private static TokenClient instance;

    private final TestConfig config;
    private final AuthClient authClient;
    // Usar ConcurrentMap para seguridad en hilos
    private final ConcurrentMap<String, TokenInfo> tokenCache;

    private TokenClient() {
        this.config = TestConfig.getInstance();
        this.authClient = AuthClient.getInstance();
        this.tokenCache = new ConcurrentHashMap<>(); // Usar ConcurrentHashMap
    }

    public static TokenClient getInstance() {
        if (instance == null) {
            synchronized (TokenClient.class) {
                if (instance == null) {
                    instance = new TokenClient();
                }
            }
        }
        return instance;
    }

    // Metodo para obtener el token de cualquier usuario
    public String getTokenForUser(String username, String password) {
        return getToken(username, password); // Reusa la lógica interna
    }
    /**
     * Obtiene un token de acceso para un usuario, usando la caché si aún no expiró.
     * Si el token expiró o no existe, solicita uno nuevo al AuthClient.
     */
    public String getToken(String username, String password) {
        String cacheKey = username; // La clave es SÓLO el username
        TokenInfo tokenInfo = tokenCache.get(cacheKey);

        // Si el token existe y NO ha expirado, lo usamos
        if (tokenInfo != null && !tokenInfo.isExpired()) {
            log.debug("Reutilizando token en caché para usuario {}", username);
            return tokenInfo.getAccessToken();
        }

        // Si no hay token o expiró, pedimos uno nuevo
        log.debug("Solicitando nuevo token para usuario {}", username);
        String newToken;
        int expiresIn = 240; // fallback (4 min)

        try {
            Response response = authClient.requestTokenResponse(username, password);

            if (response.statusCode() == 200) {
                newToken = response.jsonPath().getString("access_token");
                Integer exp = response.jsonPath().getInt("expires_in");

                if (exp != null && exp > 0) {
                    expiresIn = exp;
                }

                // Guardar el token nuevo en caché
                tokenCache.put(cacheKey, new TokenInfo(newToken, expiresIn));
                log.debug("Nuevo token guardado en caché para {} (expira en {}s)", username, expiresIn);
                return newToken;
            } else {
                // Manejar error de autenticación (ej. 401 Unauthorized)
                log.warn("No se obtuvo token (status {}). Body: {}", response.statusCode(), response.getBody().asString());
                // Devolvemos null para que los tests de login fallido funcionen
                return null;
            }
        } catch (Exception e) {
            log.error("Error al solicitar token para {}: {}", username, e.getMessage());
            throw new RuntimeException("Error al comunicarse con Keycloak: " + e.getMessage());
        }
    }

    /**
     * Obtiene el token del usuario administrador configurado en TestConfig.
     */
    public String getAdminToken() {
        // Obtenemos el token usando la lógica de caché
        String token = getToken(config.getAdminUsername(), config.getAdminPassword());

        // Si, aun así, el token es nulo (falló el login del admin), es un error crítico.
        if (token == null) {
            log.error("¡FALLO CRÍTICO! No se pudo obtener el token de administrador.");
            log.error("Verifica las credenciales 'admin.username' y 'admin.password' en test.properties y que Keycloak esté corriendo.");
            throw new RuntimeException("No se pudo obtener el token de administrador. Revisa la configuración.");
        }
        return token;
    }

    /**
     * Invalida un token específico en caché (para forzar su renovación).
     */
    public void invalidateToken(String username) {
        String cacheKey = username; // La clave es SÓLO el username
        tokenCache.remove(cacheKey);
        log.debug("Token invalidado para usuario {}", username);
    }

    /**
     * Limpia completamente la caché de tokens.
     */
    public void clearCache() {
        tokenCache.clear();
        log.debug("Caché de tokens limpiada");
    }

    @Getter
    private static class TokenInfo {
        private final String accessToken;
        private final Instant expirationTime;
        // Buffer de seguridad: consideramos el token expirado 30 segundos antes
        private static final int EXPIRATION_BUFFER_SECONDS = 30;

        public TokenInfo(String accessToken, int expiresInSeconds) {
            this.accessToken = accessToken;
            // Restar un buffer para evitar usar un token que está a punto de expirar
            long effectiveExpiresIn = Math.max(expiresInSeconds - EXPIRATION_BUFFER_SECONDS, 1);
            this.expirationTime = Instant.now().plusSeconds(effectiveExpiresIn);
        }

        public boolean isExpired() {
            boolean expired = Instant.now().isAfter(expirationTime);
            if (expired) {
                log.debug("Token expirado (expiró en {})", expirationTime);
            }
            return expired;
        }
    }
}