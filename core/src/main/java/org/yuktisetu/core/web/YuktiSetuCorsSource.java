package org.yuktisetu.core.web;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * The fleet's CORS policy, in one place.
 *
 * auth-service, admin-service and user-service each carry a hand-copied
 * CorsConfig, and the note in those files says to pull the policy into framework
 * "instead of copy-pasting a third time". drive-service would have been the
 * fourth, so the policy lives here now.
 *
 * Deliberately a static factory rather than a @Configuration in core. Every
 * service does @ComponentScan("org.yuktisetu.core"), so a bean named
 * corsConfigurationSource defined here would collide with the three services
 * that already declare their own and break their startup. Each service keeps a
 * six-line @Configuration that calls into this; only the policy is shared.
 *
 * The existing three can migrate to this whenever convenient -- their current
 * copies keep working untouched until then.
 */
public final class YuktiSetuCorsSource {

    private YuktiSetuCorsSource() {}

    /** Local dev front-ends plus the API console the team actually uses. */
    public static final List<String> DEFAULT_ALLOWED_ORIGINS = List.of(
            "https://hoppscotch.io",
            "http://localhost:3001",
            "http://localhost:3000"
    );

    private static final List<String> ALLOWED_METHODS =
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private static final List<String> ALLOWED_HEADERS =
            List.of("Authorization", "Content-Type", "Accept", "Origin");

    public static CorsConfigurationSource defaultSource() {
        return forOrigins(DEFAULT_ALLOWED_ORIGINS);
    }

    public static CorsConfigurationSource forOrigins(List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setExposedHeaders(List.of("Authorization"));
        // False because every client authenticates with a Bearer token, not a
        // cookie -- allowing credentials would widen the surface for nothing.
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
