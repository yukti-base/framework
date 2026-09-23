package org.yuktisetu.drive.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.drive.model.ReapplicationStance;

import java.math.BigDecimal;
import org.yuktisetu.persistence.BaseAuditableEntity;

/**
 * Who may sit for this drive given what they already hold. Configured per drive
 * by the TnP at posting time, never hard-coded, because placement policy changes
 * year to year at the TnP Cell's discretion.
 *
 * There is NO one-offer policy. The two halves are independent:
 *
 *  - Internships: a student holding a live internship offer is normally locked
 *    out of another internship drive (interningStudentStance = BLOCKED), but a
 *    full-time conversion drive can override that.
 *  - Full-time: a placed student MAY sit again, typically only where the new
 *    role pays above their current package by minPackageUpliftPercent. On top of
 *    that, the TnP can rule on individual placed students per drive through
 *    DrivePlacedStudentDecision, which overrides everything here.
 */
@Entity
@Table(
        name = "posting_reapplication_policies",
        uniqueConstraints = @UniqueConstraint(name = "uk_reapply_policy_posting", columnNames = "posting_id")
)
@Getter
@Setter
public class PostingReapplicationPolicy extends BaseAuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posting_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_reapply_policy_posting"))
    private JobPosting posting;

    // --- Already placed full-time ---

    @Enumerated(EnumType.STRING)
    @Column(name = "placed_student_stance", nullable = false, length = 24)
    private ReapplicationStance placedStudentStance = ReapplicationStance.BLOCKED;

    /** Required when the stance is ALLOWED_WITH_UPLIFT. e.g. 20.00 = 20% above current. */
    @Column(name = "min_package_uplift_percent", precision = 6, scale = 2)
    private BigDecimal minPackageUpliftPercent;

    /** Absolute floor alternative: "only drives paying at least 12 LPA". */
    @Column(name = "min_absolute_package", precision = 12, scale = 2)
    private BigDecimal minAbsolutePackage;

    // --- Currently interning ---

    @Enumerated(EnumType.STRING)
    @Column(name = "interning_student_stance", nullable = false, length = 24)
    private ReapplicationStance interningStudentStance = ReapplicationStance.BLOCKED;

    @Column(name = "min_stipend_uplift_percent", precision = 6, scale = 2)
    private BigDecimal minStipendUpliftPercent;

    // --- Offered but not yet accepted ---

    @Enumerated(EnumType.STRING)
    @Column(name = "pending_offer_stance", nullable = false, length = 24)
    private ReapplicationStance pendingOfferStance = ReapplicationStance.ALLOWED;

    /** Null = unlimited. Caps how many live offers a student may accumulate. */
    @Column(name = "max_live_offers")
    private Integer maxLiveOffers;

    /** Does a PPO count as "placed" for this drive's purposes? */
    @Column(name = "count_internship_towards_placement", nullable = false)
    private boolean countInternshipTowardsPlacement = false;

    /** A student whose only offer was declined is normally back in the pool. */
    @Column(name = "allow_if_last_offer_declined", nullable = false)
    private boolean allowIfLastOfferDeclined = true;

    /** Cooling-off period after the last offer, in days. Null = none. */
    @Column(name = "min_days_since_last_offer")
    private Integer minDaysSinceLastOffer;

    /**
     * What happens to an already-placed student the TnP has NOT individually
     * ruled on. Explicit because silence is not a policy -- without this the
     * behaviour would be whatever the evaluator happened to do first.
     */
    @Column(name = "require_admin_decision_for_placed", nullable = false)
    private boolean requireAdminDecisionForPlaced = false;
}
