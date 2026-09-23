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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.drive.model.JobType;
import org.yuktisetu.drive.model.PackageBasis;
import org.yuktisetu.drive.model.PostingStatus;
import org.yuktisetu.drive.model.ShortlistingMode;
import org.yuktisetu.drive.model.StipendPeriod;
import org.yuktisetu.drive.model.WorkMode;

import java.math.BigDecimal;
import java.time.Instant;
import org.yuktisetu.persistence.BaseAuditableEntity;

/**
 * One JD, published once, targeting any number of (college, department, batch)
 * combinations through PostingTarget.
 *
 * Money is stored as numerics, never free text, because the re-application
 * uplift rule ("only if this drive pays 20% above your current package") has to
 * be computable. You cannot recover a number from "6-7 LPA + variable" later.
 */
@Entity
@Table(
        name = "job_postings",
        uniqueConstraints = @UniqueConstraint(name = "uk_posting_company_code", columnNames = {"company_id", "posting_code"}),
        indexes = {
                // The deadline sweep's entire query -- it selects PUBLISHED rows
                // whose deadline has passed, once a minute, forever.
                @Index(name = "idx_posting_status_deadline", columnList = "status, application_deadline"),
                @Index(name = "idx_posting_company_id", columnList = "company_id"),
                @Index(name = "idx_posting_academic_year", columnList = "academic_year")
        }
)
@Getter
@Setter
public class JobPosting extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_posting_company"))
    private Company company;

    @Column(name = "posting_code", nullable = false, length = 32)
    private String postingCode;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 24)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", length = 12)
    private WorkMode workMode;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "responsibilities", columnDefinition = "text")
    private String responsibilities;

    // --- Compensation. Canonical numerics; the uplift rule reads these. ---

    @Column(name = "ctc_min", precision = 12, scale = 2)
    private BigDecimal ctcMin;

    @Column(name = "ctc_max", precision = 12, scale = 2)
    private BigDecimal ctcMax;

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

    @Column(name = "internship_duration_months")
    private Integer internshipDurationMonths;

    @Column(name = "ppo_possible", nullable = false)
    private boolean ppoPossible = false;

    @Column(name = "bond_months")
    private Integer bondMonths;

    @Column(name = "bond_amount", precision = 12, scale = 2)
    private BigDecimal bondAmount;

    @Column(name = "vacancy_count")
    private Integer vacancyCount;

    // --- Lifecycle ---

    /** e.g. "2025-2026". The reporting key every placement statistic groups by. */
    @Column(name = "academic_year", nullable = false, length = 9)
    private String academicYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private PostingStatus status = PostingStatus.DRAFT;

    /** Null means applications open the moment the posting is published. */
    @Column(name = "application_opens_at")
    private Instant applicationOpensAt;

    /**
     * The gate. Enforced against the SERVER clock inside the apply transaction,
     * not by a disabled button, and re-checked even after the scheduled status
     * flip has run.
     */
    @Column(name = "application_deadline", nullable = false)
    private Instant applicationDeadline;

    /** When the pool was actually sealed, which may lag the deadline by seconds. */
    @Column(name = "applications_closed_at")
    private Instant applicationsClosedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "published_by")
    private Long publishedBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "closure_reason", length = 512)
    private String closureReason;

    @Column(name = "drive_date")
    private Instant driveDate;

    @Column(name = "drive_venue", length = 255)
    private String driveVenue;

    /**
     * COMPOSITE_SCORE runs the scoring engine at deadline; COMPANY_SIDE sends the
     * hard-gate survivors to the company untouched; MANUAL keeps them with the
     * TnP to pick from in the portal. Recorded here because it decides what a
     * student can later be shown about their own result -- see ShortlistingMode.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "shortlisting_mode", nullable = false, length = 24)
    private ShortlistingMode shortlistingMode = ShortlistingMode.COMPOSITE_SCORE;

    /** Whether clearing this drive marks a student placed for re-application purposes. */
    @Column(name = "marks_student_placed", nullable = false)
    private boolean marksStudentPlaced = true;

    /** Optional hard cap on total applications. Null = uncapped. */
    @Column(name = "max_applications")
    private Integer maxApplications;

    @Column(name = "notify_on_publish", nullable = false)
    private boolean notifyOnPublish = true;

    /**
     * Two writers touch this row: admin-service authors it, and drive-service's
     * deadline sweep flips its status. Optimistic locking makes a lost update
     * impossible rather than unlikely.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
