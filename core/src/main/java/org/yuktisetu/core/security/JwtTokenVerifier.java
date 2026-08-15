package org.yuktisetu.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import java.security.PublicKey;

/**
 * Verify-only counterpart to auth-service's token-issuing provider. Nothing
 * in core ever holds a private key or exposes a way to sign a token -- see
 * core/build.gradle's comment on why that boundary is load-bearing.
 */
public class JwtTokenVerifier {

    private final PublicKey publicKey;
    private final String expectedIssuer;

    public JwtTokenVerifier(PublicKey publicKey, String expectedIssuer) {
        if (publicKey == null) {
            throw new IllegalArgumentException("PublicKey must not be null");
        }
        if (expectedIssuer == null || expectedIssuer.isBlank()) {
            throw new IllegalArgumentException("Expected issuer must not be null or blank");
        }
        this.publicKey = publicKey;
        this.expectedIssuer = expectedIssuer;
    }

    public Claims verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Validate issuer
            String issuer = claims.getIssuer();
            if (issuer == null || !issuer.equals(expectedIssuer)) {
                throw new JwtVerificationException("Token issuer is invalid. Expected: " + expectedIssuer + ", got: " + issuer);
            }

            return claims;
        } catch (ExpiredJwtException e) {
            throw new JwtVerificationException("Token has expired.", e);
        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException e) {
            throw new JwtVerificationException("Token is invalid.", e);
        }
    }

    public static class JwtVerificationException extends RuntimeException {
        public JwtVerificationException(String message, Throwable cause) { super(message, cause); }
        public JwtVerificationException(String message) { super(message); }
    }
}