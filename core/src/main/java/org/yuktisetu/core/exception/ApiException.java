package org.yuktisetu.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base type for every intentional, expected API failure across every
 * service. CoreExceptionHandler has exactly one method bound to this type --
 * it never needs editing when a service adds a new failure case. A service
 * just extends one of the four subclasses below (or this class directly,
 * for a status none of them cover) and supplies its own status/code/message.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
