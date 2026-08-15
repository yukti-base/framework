package org.yuktisetu.core.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds only what's needed to VERIFY an RS256 token someone else signed:
 * publicKeyPath and issuer. Deliberately excludes a private-key path or any
 * token TTL -- those are signing concerns belonging to whichever single
 * service actually issues tokens (auth-service), not to every jar consumer.
 *
 * Multiple @ConfigurationProperties classes CAN bind to the same prefix
 * without conflict -- each class only picks up the fields it declares. So
 * auth-service keeps its own JwtSigningProperties (privateKeyPath,
 * accessTokenTtlSeconds, refreshTokenTtlSeconds) bound to the same
 * "yuktisetu.jwt" prefix in the same properties file, alongside this class,
 * with no clash. Don't merge them into one class -- that would put a
 * private-key field back on a type every non-issuing service also loads.
 */
@ConfigurationProperties(prefix = "yuktisetu.jwt")
public class JwtVerificationProperties {

    private String publicKeyPath;
    private String issuer;

    public String getPublicKeyPath() { return publicKeyPath; }
    public void setPublicKeyPath(String publicKeyPath) { this.publicKeyPath = publicKeyPath; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
}
