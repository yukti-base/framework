package org.yuktisetu.core.response;

import java.time.Instant;

/**
 * The single response envelope every controller across every service wraps
 * its body in, success or error. HTTP status codes on the wire stay real
 * (200/4xx/5xx) -- this only standardizes the JSON shape, it does not
 * replace status semantics. CoreExceptionHandler and the security entry
 * points (RestAuthenticationEntryPoint / RestAccessDeniedHandler) already
 * build the error side of this for every service for free; a service only
 * needs to wrap its own successful controller returns with {@link #success}.
 */
public record YuktiSetuResponse<T>(
        boolean success,
        int status,
        String message,
        T data,
        ErrorDetail error,
        Instant timestamp
) {
    public static <T> YuktiSetuResponse<T> success(T data) {
        return success(data, 200, "OK");
    }

    public static <T> YuktiSetuResponse<T> success(T data, String message) {
        return success(data, 200, message);
    }

    public static <T> YuktiSetuResponse<T> success(T data, int status, String message) {
        return new YuktiSetuResponse<>(true, status, message, data, null, Instant.now());
    }

    public static <T> YuktiSetuResponse<T> error(int status, String code, String message) {
        return new YuktiSetuResponse<>(false, status, message, null, new ErrorDetail(code, message), Instant.now());
    }

    public record ErrorDetail(String code, String message) {}
}
