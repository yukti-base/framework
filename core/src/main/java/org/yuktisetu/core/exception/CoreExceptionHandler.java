package org.yuktisetu.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.yuktisetu.core.response.YuktiSetuResponse;

/**
 * Handles every ApiException subclass across every service, plus the two
 * generic Spring cases every REST controller eventually hits.
 *
 * A service adds its OWN @RestControllerAdvice ONLY for exception types this
 * class doesn't already cover. Never re-declare a handler for ApiException,
 * MethodArgumentNotValidException, or Exception.class in a service-level
 * advice -- Spring will throw "Ambiguous @ExceptionHandler method mapped"
 * at request time if two advice beans both claim the exact same type.
 *
 * A service-level advice CAN safely declare a handler for one SPECIFIC
 * ApiException subclass if it needs different behaviour than this generic
 * one (e.g. an auth-service lockout exception that also needs to set a
 * Retry-After header) -- Spring resolves to the more specific type
 * automatically. That's a legitimate override, not the ambiguous case above.
 */
@Slf4j
@RestControllerAdvice
public class CoreExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<YuktiSetuResponse<Object>> handleApiException(ApiException ex, HttpServletRequest request) {
        log.warn("Business exception code={} status={} path={} message={}",
                ex.getCode(), ex.getStatus(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(YuktiSetuResponse.error(ex.getStatus().value(), ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<YuktiSetuResponse<Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation failure path={} errorCount={}",
                request.getRequestURI(), ex.getBindingResult().getErrorCount());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(YuktiSetuResponse.error(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", "Request payload failed validation"));
    }

    /**
     * A @PreAuthorize denial.
     *
     * Spring Security 7 throws AuthorizationDeniedException from inside the
     * controller invocation, which means it reaches this advice rather than the
     * filter chain's RestAccessDeniedHandler -- and without this handler the
     * catch-all below reported every permission denial in the fleet as a 500.
     * A client could not tell "you are not allowed to do this" from "the server
     * broke", and the logs filled with stack traces for what is ordinary,
     * expected authorization behaviour.
     *
     * Declared here rather than per service so all four get the fix at once.
     */
    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<YuktiSetuResponse<Object>> handleAccessDenied(RuntimeException ex, HttpServletRequest request) {
        log.warn("Access denied path={} reason={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(YuktiSetuResponse.error(HttpStatus.FORBIDDEN.value(), "ACCESS_DENIED",
                        "You do not have permission to perform this action."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<YuktiSetuResponse<Object>> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Never return ex.getMessage() here -- that's how stack traces / SQL
        // fragments leak to API clients. The full exception is logged
        // server-side only.
        log.error("Unhandled exception path={}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(YuktiSetuResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR", "Something went wrong"));
    }
}
