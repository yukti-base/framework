package org.yuktisetu.notification.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.notification.model.NotificationChannel;
import org.yuktisetu.notification.model.TemplateEngine;

/**
 * Message bodies as data rather than as files on the classpath, so the TnP can
 * reword a notification after handover without a developer and a deploy -- the
 * same maintainability argument behind the dynamic criteria builder.
 */
@Entity
@Table(
        name = "notification_templates",
        uniqueConstraints = @UniqueConstraint(name = "uk_template", columnNames = {"template_key", "channel", "locale"})
)
@Getter
@Setter
public class NotificationTemplate extends NotificationAuditableEntity {

    @Column(name = "template_key", nullable = false, length = 96)
    private String templateKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private NotificationChannel channel;

    @Column(name = "locale", nullable = false, length = 8)
    private String locale = "en";

    @Column(name = "subject_template", length = 255)
    private String subjectTemplate;

    @Column(name = "body_template", nullable = false, columnDefinition = "text")
    private String bodyTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "engine", nullable = false, length = 16)
    private TemplateEngine engine = TemplateEngine.THYMELEAF;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
