package org.yuktisetu.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * A company usually has more than one person the TnP deals with (an HR contact,
 * a campus lead, a technical panel coordinator), so contacts are normalised out
 * of Company rather than flattened into three columns that run out immediately.
 */
@Entity
@Table(
        name = "company_contacts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_company_contact_email", columnNames = {"company_id", "email"}),
                // Exactly one primary contact per company, enforced by the DB.
                // primaryContactLock holds company_id when this row is primary
                // and NULL otherwise; Postgres allows unlimited NULLs in a
                // unique index, so this expresses a partial constraint in a form
                // Hibernate can actually emit under ddl-auto.
                @UniqueConstraint(name = "uk_company_contact_primary", columnNames = "primary_contact_lock")
        },
        indexes = @Index(name = "idx_company_contact_company_id", columnList = "company_id")
)
@Getter
@Setter
public class CompanyContact extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_company_contact_company"))
    private Company company;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "designation", length = 128)
    private String designation;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "phone", length = 24)
    private String phone;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    /** Set to company_id when isPrimary, null otherwise. See uk_company_contact_primary. */
    @Column(name = "primary_contact_lock")
    private Long primaryContactLock;

    @Column(name = "notes", length = 512)
    private String notes;
}
