package org.yuktisetu.core.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Provides the RS256 PublicKey bean every service needs to verify tokens.
 * Every service that includes core gets this bean for free -- no per-service
 * KeyConfig class needed anymore for the verification side.
 *
 * MIGRATION NOTE for auth-service specifically: it currently has its OWN
 * KeyConfig that also defines a `jwtPublicKey` bean. Once auth-service adds
 * core as a dependency, that bean collides with this one (Spring will refuse
 * to start -- "a bean with that name is already defined"). Delete the public
 * -key method from auth-service's KeyConfig and keep only its private-key
 * loading logic there (rename that bean, e.g. `jwtPrivateKey`, so it's
 * unambiguous which one is which once both exist side by side).
 */
@Configuration
@EnableConfigurationProperties(JwtVerificationProperties.class)
public class JwtPublicKeyConfig {

    @Bean
    public PublicKey jwtPublicKey(
            JwtVerificationProperties props,
            ResourceLoader resourceLoader
    ) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        Resource resource = resourceLoader.getResource(props.getPublicKeyPath());

        String pem = stripPemHeaders(
                new String(resource.getInputStream().readAllBytes())
        );

        byte[] decoded = Base64.getDecoder().decode(pem);

        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);

        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    @Bean
    public JwtTokenVerifier jwtTokenVerifier(PublicKey publicKey, JwtVerificationProperties props) {
        return new JwtTokenVerifier(publicKey, props.getIssuer());
    }

    private String stripPemHeaders(String pem) {
        return pem
                .replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
    }
}