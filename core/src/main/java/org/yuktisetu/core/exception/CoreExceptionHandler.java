package org.yuktisetu.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
@RestControllerAdvice
public class CoreExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("VALIDATION_ERROR", "Request payload failed validation"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        // Never return ex.getMessage() here -- that's how stack traces / SQL
        // fragments leak to API clients. Wire in a logger once this
        // codebase has a shared logging convention decided; for now this at
        // least guarantees the CLIENT never sees internal detail.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "Something went wrong"));
    }
}
