package org.yuktisetu.core.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    // Convenience overload for the "X with id Y not found" shape every
    // service ends up writing anyway -- use this directly instead of
    // wrapping it in a service-local NotFoundException subclass.
    public NotFoundException(String entity, Object id) {
        this(entity + " with id " + id + " not found (or already deleted).");
    }
}
