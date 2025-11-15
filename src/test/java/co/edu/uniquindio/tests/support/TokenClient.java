package co.edu.uniquindio.tests.support;

import co.edu.uniquindio.tests.config.TestConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import io.restassured.response.Response;

import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Getter
public class TokenClient {

    private static TokenClient instance;

    private final TestConfig config;
    private final AuthClient authClient;
    private final ConcurrentMap<String, TokenInfo> tokenCache;
    private final Gson gson; // 👈 AÑADIDO

    private TokenClient() {
        this.config = TestConfig.getInstance();
        this.authClient = AuthClient.getInstance();
        this.tokenCache = new ConcurrentHashMap<>();
        this.gson = new Gson(); // 👈 AÑADIDO
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

    // ⬇️ --- NUEVO MÉTODO --- ⬇️
    /**
     * Obtiene el UserID del administrador, forzando un login si no está en caché.
     */
    public String getAdminUserId() {
        // 1. Asegurarse de que el token de admin esté logueado y en caché
        getAdminToken();

        // 2. Obtener la info de la caché
        String adminUsername = config.getAdminUsername();
        TokenInfo tokenInfo = tokenCache.get(adminUsername);

        if (tokenInfo == null || tokenInfo.getUserId() == null) {
            log.error("No se pudo obtener el UserID del admin desde el token");
            throw new RuntimeException("Error al extraer UserID del token de admin");
        }

        return tokenInfo.getUserId();
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

                // ⬇️ --- MODIFICADO --- ⬇️
                String userId = parseUserIdFromToken(newToken);
                tokenCache.put(cacheKey, new TokenInfo(newToken, expiresIn, userId));
                log.debug("Nuevo token guardado en caché para {} (UserID: {}, expira en {}s)", username, userId, expiresIn);
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
            // ⬇️⬇️⬇️ CORRECCIÓN APLICADA ⬇️⬇️⬇️
            // =================================================================
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

                // ⬇️ --- MODIFICADO --- ⬇️
                String userId = parseUserIdFromToken(newToken);
                tokenCache.put(cacheKey, new TokenInfo(newToken, expiresIn, userId));
                log.debug("Nuevo token de ADMIN guardado en caché (UserID: {}, expira en {}s)", userId, expiresIn);
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

    // ⬇️ --- NUEVO MÉTODO --- ⬇️
    /**
     * Decodifica el payload de un JWT para extraer el "sub" (Subject),
     * que es el UserID.
     */
    private String parseUserIdFromToken(String jwt) {
        if (jwt == null || jwt.isEmpty()) return null;
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return null;

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonObject jsonPayload = gson.fromJson(payload, JsonObject.class);

            if (jsonPayload.has("sub")) {
                return jsonPayload.get("sub").getAsString();
            }
            return null;
        } catch (Exception e) {
            log.error("Error al decodificar JWT para extraer 'sub': {}", e.getMessage());
            return null;
        }
    }


    @Getter
    private static class TokenInfo {
        private final String accessToken;
        private final Instant expirationTime;
        private final String userId; // 👈 AÑADIDO
        private static final int EXPIRATION_BUFFER_SECONDS = 30;

        // ⬇️ --- MODIFICADO --- ⬇️
        public TokenInfo(String accessToken, int expiresInSeconds, String userId) {
            this.accessToken = accessToken;
            this.userId = userId; // 👈 AÑADIDO
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