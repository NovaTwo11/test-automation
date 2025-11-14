package co.edu.uniquindio.tests.support;

import co.edu.uniquindio.tests.config.TestConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import io.restassured.response.Response;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Getter
public class TokenClient {

    private static TokenClient instance;

    private final TestConfig config;
    private final AuthClient authClient;
    private final ConcurrentMap<String, TokenInfo> tokenCache;

    private TokenClient() {
        this.config = TestConfig.getInstance();
        this.authClient = AuthClient.getInstance();
        this.tokenCache = new ConcurrentHashMap<>();
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
        return getToken(username, password);
    }

    /**
     * Obtiene un token de acceso para un usuario (del realm 'taller'),
     * usando la caché si aún no expiró.
     */
    public String getToken(String username, String password) {
        String cacheKey = username;
        TokenInfo tokenInfo = tokenCache.get(cacheKey);

        if (tokenInfo != null && !tokenInfo.isExpired()) {
            log.debug("Reutilizando token en caché para usuario {}", username);
            return tokenInfo.getAccessToken();
        }

        log.debug("Solicitando nuevo token para usuario {}", username);
        String newToken;
        int expiresIn = 240; // fallback (4 min)

        try {
            // Llama al método por defecto de AuthClient (que usa realm 'taller' y client 'taller-api')
            Response response = authClient.requestTokenResponse(username, password);
            if (response.statusCode() == 200) {
                newToken = response.jsonPath().getString("access_token");
                Integer exp = response.jsonPath().getInt("expires_in");

                if (exp != null && exp > 0) {
                    expiresIn = exp;
                }

                tokenCache.put(cacheKey, new TokenInfo(newToken, expiresIn));
                log.debug("Nuevo token guardado en caché para {} (expira en {}s)", username, expiresIn);
                return newToken;
            } else {
                log.warn("No se obtuvo token (status {}). Body: {}", response.statusCode(), response.getBody().asString());
                return null;
            }
        } catch (Exception e) {
            log.error("Error al solicitar token para {}: {}", username, e.getMessage());
            throw new RuntimeException("Error al comunicarse con Keycloak: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el token del usuario administrador configurado en TestConfig.
     * --- MÉTODO CORREGIDO ---
     * Llama al realm 'taller' usando el client_id 'taller-api', que SÍ tiene los scopes.
     */
    public String getAdminToken() {
        // Usamos el username del admin como clave de caché
        String cacheKey = config.getAdminUsername();
        TokenInfo tokenInfo = tokenCache.get(cacheKey);

        if (tokenInfo != null && !tokenInfo.isExpired()) {
            log.debug("Reutilizando token de admin en caché");
            return tokenInfo.getAccessToken();
        }

        log.debug("Solicitando nuevo token de admin al realm '{}' usando el cliente '{}'",
                config.getKeycloakRealm(), config.getKeycloakClientId());
        String newToken;
        int expiresIn = 240; // fallback

        try {
            // =================================================================
            // ⬇️⬇️⬇️ INICIO DE LA CORRECCIÓN ⬇️⬇️⬇️
            // =================================================================

            // Usamos el método que ya sabemos que funciona (el de login normal)
            // Llama al AuthClient con:
            // 1. admin/admin123
            // 2. URL del realm 'taller'
            // 3. Client ID 'taller-api'
            // 4. Client Secret de 'taller-api'
            Response response = authClient.requestTokenResponse(
                    config.getAdminUsername(),
                    config.getAdminPassword()
            );

            // =================================================================
            // ⬆️⬆️⬆️ FIN DE LA CORRECCIÓN ⬆️⬆️⬆️
            // =================================================================

            if (response.statusCode() == 200) {
                newToken = response.jsonPath().getString("access_token");
                Integer exp = response.jsonPath().getInt("expires_in");

                if (exp != null && exp > 0) {
                    expiresIn = exp;
                }

                tokenCache.put(cacheKey, new TokenInfo(newToken, expiresIn));
                log.debug("Nuevo token de ADMIN guardado en caché (expira en {}s)", expiresIn);
                return newToken;
            } else {
                log.error("¡FALLO CRÍTICO! No se pudo obtener el token de administrador.");
                log.error("El realm '{}' de Keycloak respondió con status {}. Body: {}",
                        config.getKeycloakRealm(), response.statusCode(), response.getBody().asString());
                throw new RuntimeException("No se pudo obtener el token de administrador. Revisa la configuración.");
            }
        } catch (Exception e) {
            log.error("Error al solicitar token de admin: {}", e.getMessage());
            throw new RuntimeException("Error al comunicarse con Keycloak (admin): " + e.getMessage(), e);
        }
    }

    /**
     * Invalida un token específico en caché (para forzar su renovación).
     */
    public void invalidateToken(String username) {
        String cacheKey = username;
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
        private static final int EXPIRATION_BUFFER_SECONDS = 30;

        public TokenInfo(String accessToken, int expiresInSeconds) {
            this.accessToken = accessToken;
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