package org.yuktisetu.core.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cloudinary configuration, bound from `yuktisetu.storage.*`.
 *
 * Note `enabled`: with it off the uploader bean is absent, so a service that
 * never stores files (auth, drive, notification) does not need credentials in
 * its properties at all, and a test profile cannot accidentally push a file to
 * the real account.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "yuktisetu.storage")
public class StorageProperties {

    private boolean enabled = false;

    private String cloudName;
    private String apiKey;
    private String apiSecret;

    /** Root folder for everything this application stores. */
    private String rootFolder = "yuktisetu";

    /** Hard ceiling per file. Rejected before a byte leaves the server. */
    private long maxFileSizeBytes = 10L * 1024 * 1024;
}
