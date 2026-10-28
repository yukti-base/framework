package org.yuktisetu.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    // ------------------------------------------------------------------
    // Client mistakes. Every one of these used to fall into the catch-all below
    // and be logged at ERROR with a full stack trace as "Unhandled exception" --
    // a browser hitting a POST-only URL with GET looked exactly like the server
    // crashing, and buried the real failures. They are the CALLER's error, so they
    // answer with the right 4xx and log one WARN line, no stack.
    // ------------------------------------------------------------------

    /** Wrong HTTP method, e.g. GET on a POST-only endpoint. 405 with an Allow header. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<YuktiSetuResponse<Object>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Method not allowed method={} path={} supported={}",
                ex.getMethod(), request.getRequestURI(), ex.getSupportedHttpMethods());
        HttpHeaders headers = new HttpHeaders();
        if (ex.getSupportedHttpMethods() != null && !ex.getSupportedHttpMethods().isEmpty()) {
            headers.setAllow(ex.getSupportedHttpMethods());
        }
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(headers)
                .body(YuktiSetuResponse.error(HttpStatus.METHOD_NOT_ALLOWED.value(), "METHOD_NOT_ALLOWED",
                        "This endpoint does not accept " + ex.getMethod() + " requests."));
    }

    /**
     * A request parameter or path variable of the wrong type, e.g. ?status=[object Object]
     * for an enum. Names the parameter, never echoes the value back.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<YuktiSetuResponse<Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Bad parameter path={} parameter={} expected={} message={}",
                request.getRequestURI(), ex.getName(),
                ex.getRequiredType() == null ? "?" : ex.getRequiredType().getSimpleName(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(YuktiSetuResponse.error(HttpStatus.BAD_REQUEST.value(), "INVALID_PARAMETER",
                        "The value given for '" + ex.getName() + "' is not valid."));
    }

    /** A body that is not parseable JSON, or whose fields have the wrong types. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<YuktiSetuResponse<Object>> handleUnreadableBody(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Unreadable request body path={} reason={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(YuktiSetuResponse.error(HttpStatus.BAD_REQUEST.value(), "MALFORMED_REQUEST",
                        "The request body could not be read."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<YuktiSetuResponse<Object>> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Every Spring MVC rejection that carries its own status (missing
        // parameter, unsupported media type, unknown URL, oversized upload, ...)
        // implements ErrorResponse. Honour that status instead of calling it a 500:
        // it is the client's request that was wrong, not the server that broke.
        // Catching the interface here also covers the ones Spring adds later.
        if (ex instanceof ErrorResponse er && er.getStatusCode().is4xxClientError()) {
            HttpStatus status = HttpStatus.resolve(er.getStatusCode().value());
            String code = status == null ? "BAD_REQUEST" : status.name();
            String message = status == null ? "The request could not be processed." : status.getReasonPhrase() + ".";
            log.warn("Rejected request status={} path={} reason={}",
                    er.getStatusCode().value(), request.getRequestURI(), ex.getMessage());
            return ResponseEntity.status(er.getStatusCode()).headers(er.getHeaders())
                    .body(YuktiSetuResponse.error(er.getStatusCode().value(), code, message));
        }

        // Never return ex.getMessage() here -- that's how stack traces / SQL
        // fragments leak to API clients. The full exception is logged
        // server-side only.
        log.error("Unhandled exception path={}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(YuktiSetuResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR", "Something went wrong"));
    }
}
