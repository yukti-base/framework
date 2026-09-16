package org.yuktisetu.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.model.CompanyStatus;
import org.yuktisetu.model.CompanyTier;
import org.yuktisetu.model.CompanyType;

import java.time.Instant;

/**
 * A recruiter, onboarded by the TnP before any job can be posted against it.
 *
 * Trust-wide by decision: one Infosys row for all of PCET, not one per college.
 * That is what lets a single posting target PCCOE and PCCOE-Akurdi together, and
 * it is the only way the trust-wide leadership view can say "Infosys visited
 * three colleges" rather than showing three unrelated records. The cost is that
 * the name/code uniqueness is trust-wide too, so onboarding must check before
 * inserting rather than relying on per-college isolation.
 */
@Entity
@Table(
        name = "companies",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_company_code", columnNames = "code")
        },
        indexes = {
                @Index(name = "idx_company_status", columnList = "status"),
                @Index(name = "idx_company_type", columnList = "company_type")
        }
)
@Getter
@Setter
public class Company extends BaseAuditableEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "legal_name", length = 255)
    private String legalName;

    /** Short human key used on posting codes and exports, e.g. "INFY". */
    @Column(name = "code", nullable = false, length = 32)
    private String code;

    /**
     * Drives the composite-scoring weight ceilings later (a product company is
     * allowed to weight coding far higher than a service company). Captured at
     * onboarding because nobody will know it at scoring time.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", length = 16)
    private CompanyType companyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", length = 16)
    private CompanyTier tier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CompanyStatus status = CompanyStatus.PROSPECT;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "industry", length = 64)
    private String industry;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "hq_city", length = 96)
    private String hqCity;

    @Column(name = "hq_state", length = 96)
    private String hqState;

    @Column(name = "hq_country", length = 96)
    private String hqCountry;

    /** CIN / GSTIN, for the TnP's own records. */
    @Column(name = "registration_number", length = 64)
    private String registrationNumber;

    @Column(name = "blacklist_reason", length = 512)
    private String blacklistReason;

    @Column(name = "blacklisted_at")
    private Instant blacklistedAt;
}
