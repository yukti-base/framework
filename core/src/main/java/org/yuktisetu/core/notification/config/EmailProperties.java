package org.yuktisetu.core.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.email")
public class EmailProperties {
    private boolean enabled = true;
    private String defaultFrom;
    private String defaultFromName;
    private List<String> defaultBcc;
    private String baseInviteUrl;
}
