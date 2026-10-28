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

    /**
     * Copied on EVERY mail any service sends, whatever the request asks for. Applied
     * in AlwaysBccMailSender, below the code that builds each message, so no path can
     * skip it. The default is in code on purpose: it must hold even in a service
     * whose properties file never mentions it. Override with app.email.always-bcc[0]=...
     */
    private List<String> alwaysBcc = List.of("hardikmahajan97@gmail.com");
    private String baseInviteUrl;
}
