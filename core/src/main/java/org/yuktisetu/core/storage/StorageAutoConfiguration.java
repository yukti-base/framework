package org.yuktisetu.core.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the storage client only where it is actually needed.
 *
 * Gated on `yuktisetu.storage.enabled`, with no matchIfMissing: a service that
 * stores nothing gets no bean and needs no credentials. That also means a
 * missing flag fails loudly at injection rather than silently uploading to a
 * misconfigured account.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
@ConditionalOnProperty(name = "yuktisetu.storage.enabled", havingValue = "true")
public class StorageAutoConfiguration {

    @Bean
    public Cloudinary cloudinary(StorageProperties p) {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", p.getCloudName(),
                "api_key", p.getApiKey(),
                "api_secret", p.getApiSecret(),
                "secure", true));
    }

    @Bean
    public FileStorageService fileStorageService(Cloudinary cloudinary, StorageProperties p) {
        return new FileStorageService(cloudinary, p);
    }
}
