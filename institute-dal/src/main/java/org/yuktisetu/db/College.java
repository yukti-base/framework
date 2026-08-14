package org.yuktisetu.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.model.TenantStatus;

@Entity
@Table(
        name = "colleges",
        uniqueConstraints = @UniqueConstraint(name = "uk_college_trust_code", columnNames = {"trust_id", "code"}),
        indexes = @Index(name = "idx_college_trust_id", columnList = "trust_id")
        // Postgres does NOT auto-index FK columns — this index is load-bearing,
        // not decorative, once "list colleges for trust" / dashboard queries exist.
)
@Getter
@Setter
public class College extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trust_id", nullable = false, foreignKey = @ForeignKey(name = "fk_college_trust"))
    private Trust trust;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    // Short code — e.g. "PCCOE". Used wherever a college needs a stable
    // human-readable handle (group aliases, drive scoping displays, etc.)
    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "address")
    private String address;

    // The "Add College" wizard fields per SSOT Section 8: name, code,
    // logo, coordinator contact. This IS the coordinator contact target
    // for the auto-sent welcome email.
    @Column(name = "primary_contact_name", nullable = false, length = 255)
    private String primaryContactName;

    @Column(name = "primary_contact_email", nullable = false)
    private String primaryContactEmail;

    @Column(name = "primary_contact_phone")
    private String primaryContactPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TenantStatus status = TenantStatus.ACTIVE;
}