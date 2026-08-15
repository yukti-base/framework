package org.yuktisetu.core.security;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.yuktisetu.core.exception.ErrorResponse;

import java.io.IOException;

/**
 * Runs when a request IS authenticated but fails a @PreAuthorize check --
 * i.e. Spring Security's own AccessDeniedException, which is a DIFFERENT
 * thing from this codebase's core.exception.ForbiddenException (that one is
 * thrown deliberately by service code and caught by CoreExceptionHandler;
 * this one is thrown by the security framework itself before a controller
 * method even runs, so CoreExceptionHandler never sees it either). Same
 * reasoning as RestAuthenticationEntryPoint -- without wiring this in,
 * Spring Security's default AccessDeniedHandler returns a bare 403, not
 * this app's ErrorResponse JSON shape.
 *
 * Wire via:
 *   http.exceptionHandling(ex -> ex.accessDeniedHandler(new RestAccessDeniedHandler()));
 */
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                ErrorResponse.of("FORBIDDEN", "You do not have permission to perform this action.")
        ));
    }
}
