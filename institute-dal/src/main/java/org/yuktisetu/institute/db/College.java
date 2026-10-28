package org.yuktisetu.institute.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.institute.model.TenantStatus;
import org.yuktisetu.persistence.BaseAuditableEntity;

import java.util.LinkedHashSet;
import java.util.Set;

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

    /**
     * The degrees this college offers. A department of this college may only
     * belong to one of these, and a drive may only target a degree the college
     * actually offers -- both enforced in the services, since a join table cannot
     * express "department.degree must be among its college's degrees".
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "college_degrees",
            joinColumns = @JoinColumn(name = "college_id", foreignKey = @ForeignKey(name = "fk_college_degree_college")),
            inverseJoinColumns = @JoinColumn(name = "degree_id", foreignKey = @ForeignKey(name = "fk_college_degree_degree")),
            uniqueConstraints = @UniqueConstraint(name = "uk_college_degree", columnNames = {"college_id", "degree_id"}))
    private Set<Degree> degrees = new LinkedHashSet<>();
}
