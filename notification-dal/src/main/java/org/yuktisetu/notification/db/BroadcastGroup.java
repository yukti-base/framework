package org.yuktisetu.notification.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.notification.model.BroadcastProvider;

/**
 * A Google Group alias the TnP already uses, kept as the deliberate fail-safe
 * channel. The portal mails the alias exactly as a human would, so CPMS never
 * needs broad Workspace domain-admin API access.
 *
 * collegeId / departmentId / batchYear are bare Longs -- this module has no
 * institute-dal dependency by design.
 */
@Entity
@Table(
        name = "broadcast_groups",
        uniqueConstraints = @UniqueConstraint(name = "uk_broadcast_group_alias", columnNames = "alias_email")
)
@Getter
@Setter
public class BroadcastGroup extends NotificationAuditableEntity {

    @Column(name = "alias_email", nullable = false, length = 320)
    private String aliasEmail;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "college_id")
    private Long collegeId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "batch_year")
    private Integer batchYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 16)
    private BroadcastProvider provider = BroadcastProvider.GOOGLE_GROUPS;

    @Column(name = "external_group_id", length = 255)
    private String externalGroupId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
