package org.yuktisetu.core.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {

    public ForbiddenException() {
        this("Actor is not permitted to perform this action.");
    }

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
