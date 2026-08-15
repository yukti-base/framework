package org.yuktisetu.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Base for "the request is valid but current state won't allow it" --
 * duplicate codes, a parent that isn't ACTIVE, dependent rows blocking a
 * delete, and so on. Each service defines its OWN named subclasses (e.g.
 * admin-service's DuplicateCodeException extends this) rather than core
 * trying to anticipate every domain's conflict cases -- core only fixes the
 * HTTP status; the code string and message are always the caller's to set.
 */
public class ConflictException extends ApiException {
    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
