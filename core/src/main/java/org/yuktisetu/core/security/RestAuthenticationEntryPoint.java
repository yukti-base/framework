package org.yuktisetu.core.security;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.yuktisetu.core.exception.ErrorResponse;

import java.io.IOException;

/**
 * Runs when a request hits a protected endpoint with no valid authentication
 * at all (missing / expired / malformed token). This happens INSIDE the
 * security filter chain, before the request ever reaches a controller --
 * CoreExceptionHandler never sees it. Without wiring this bean in, Spring
 * Security's own default entry point handles it instead: typically a bare
 * 403 with no body, not this app's ErrorResponse JSON shape.
 *
 * Not a @Component -- constructed explicitly by each service's SecurityConfig
 * and wired via:
 *   http.exceptionHandling(ex -> ex.authenticationEntryPoint(new RestAuthenticationEntryPoint()));
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                ErrorResponse.of("UNAUTHENTICATED", "Authentication is required and has failed or has not been provided.")
        ));
    }
}
