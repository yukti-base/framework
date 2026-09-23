package org.yuktisetu.drive.db;

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
import org.yuktisetu.drive.model.OfferSource;
import org.yuktisetu.drive.model.OfferStatus;
import org.yuktisetu.drive.model.OfferType;
import org.yuktisetu.drive.model.PackageBasis;
import org.yuktisetu.drive.model.StipendPeriod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.yuktisetu.persistence.BaseAuditableEntity;
import org.yuktisetu.institute.db.College;
import org.yuktisetu.identity.db.User;

/**
 * What a student already holds. Without this entity the re-application policy is
 * unenforceable -- nothing else in the system can answer "is this student placed
 * or interning, and at what package".
 *
 * Deliberately NOT one-offer-per-student: multiple live full-time offers are
 * legal, which is the entire point of the CTC-uplift rule. The cap is
 * PostingReapplicationPolicy.maxLiveOffers, enforced in the service.
 */
@Entity
@Table(
        name = "placement_offers",
        uniqueConstraints = @UniqueConstraint(name = "uk_offer_active_internship_lock", columnNames = "active_internship_lock"),
        indexes = {
                // The re-application lookup: every live offer for these students.
                @Index(name = "idx_offer_student_status_type", columnList = "student_user_id, status, offer_type"),
                @Index(name = "idx_offer_student_user_id", columnList = "student_user_id"),
                @Index(name = "idx_offer_company_id", columnList = "company_id"),
                @Index(name = "idx_offer_posting_id", columnList = "posting_id"),
                @Index(name = "idx_offer_college_id", columnList = "college_id")
        }
)
@Getter
@Setter
public class PlacementOffer extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_offer_student"))
    private User student;

    /** Denormalised for per-college placement statistics without a join to roles. */
    /** Denormalised for per-college placement statistics without a join to roles. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "college_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_offer_college"))
    private College college;

    /** Null for an off-campus or TnP-entered offer. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id",
            foreignKey = @ForeignKey(name = "fk_offer_application"))
    private JobApplication application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posting_id",
            foreignKey = @ForeignKey(name = "fk_offer_posting"))
    private JobPosting posting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_offer_company"))
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_type", nullable = false, length = 24)
    private OfferType offerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OfferStatus status = OfferStatus.OFFERED;

    // --- Canonical numerics. The uplift rule is uncomputable against free text. ---

    @Column(name = "ctc_amount", precision = 12, scale = 2)
    private BigDecimal ctcAmount;

    @Column(name = "ctc_currency", nullable = false, length = 3)
    private String ctcCurrency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "package_basis", nullable = false, length = 16)
    private PackageBasis packageBasis = PackageBasis.CTC;

    @Column(name = "stipend_amount", precision = 10, scale = 2)
    private BigDecimal stipendAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "stipend_period", length = 12)
    private StipendPeriod stipendPeriod;

    @Column(name = "role_title", length = 255)
    private String roleTitle;

    @Column(name = "location", length = 128)
    private String location;

    @Column(name = "offer_date", nullable = false)
    private LocalDate offerDate;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "internship_start_date")
    private LocalDate internshipStartDate;

    @Column(name = "internship_end_date")
    private LocalDate internshipEndDate;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "declined_at")
    private Instant declinedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoke_reason", length = 512)
    private String revokeReason;

    /** Whether this offer counts in the college's placement percentage. */
    @Column(name = "counts_toward_placement", nullable = false)
    private boolean countsTowardPlacement = true;

    @Column(name = "offer_letter_storage_key", length = 512)
    private String offerLetterStorageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private OfferSource source = OfferSource.PORTAL;

    /**
     * The internship lock, enforced by the database rather than by Express-style
     * "check then insert" logic that two concurrent requests defeat.
     *
     * Holds student_user_id when this row is a LIVE internship offer
     * (offerType INTERNSHIP or INTERNSHIP_WITH_PPO, status OFFERED/ACCEPTED/JOINED),
     * and NULL otherwise. Postgres permits unlimited NULLs in a unique index, so
     * uk_offer_active_internship_lock means exactly "at most one live internship
     * offer per student".
     *
     * Why this rather than a partial unique index: Hibernate's ddl-auto cannot
     * emit `WHERE ...` on an index, so a partial index would have to be applied
     * by hand and would silently not exist on any fresh database. This form is
     * created automatically and is impossible to forget.
     *
     * Every status transition must recompute this column in the same transaction.
     */
    @Column(name = "active_internship_lock")
    private Long activeInternshipLock;

    public Long getStudentUserId() { return student != null ? student.getId() : null; }
    public Long getCollegeId() { return college != null ? college.getId() : null; }
}
