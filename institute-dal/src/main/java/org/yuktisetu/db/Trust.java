package org.yuktisetu.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.model.TenantStatus;

@Entity
@Table(
        name = "trusts",
        uniqueConstraints = @UniqueConstraint(name = "uk_trust_code", columnNames = "code")
)
@Getter
@Setter
public class Trust extends BaseAuditableEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "primary_contact_email")
    private String primaryContactEmail;

    @Column(name = "primary_contact_phone")
    private String primaryContactPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TenantStatus status = TenantStatus.ACTIVE;

}
