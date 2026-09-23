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
import org.yuktisetu.drive.model.ApplicationSource;
import org.yuktisetu.drive.model.ApplicationStatus;

import java.time.Instant;
import org.yuktisetu.persistence.BaseAuditableEntity;
import org.yuktisetu.identity.db.User;

/**
 * One student's application to one posting. Named JobApplication rather than
 * Application so it does not read as a Spring Boot application class.
 *
 * By decision, this row holds NO snapshot of the student's profile -- CGPA,
 * backlogs and the rest are read live from StudentProfile whenever they are
 * needed. eligibilityRunId records WHICH evaluation authorised the application,
 * which is the one durable trace of why it was accepted.
 *
 * Withdraw-then-reapply REUSES this row (status back to APPLIED) rather than
 * inserting a second one, so uk_application_posting_student stays absolute.
 */
@Entity
@Table(
        name = "job_applications",
        uniqueConstraints = @UniqueConstraint(name = "uk_application_posting_student", columnNames = {"posting_id", "student_user_id"}),
        indexes = {
                @Index(name = "idx_application_student_user_id", columnList = "student_user_id"),
                @Index(name = "idx_application_posting_status", columnList = "posting_id, status"),
                @Index(name = "idx_application_applied_at", columnList = "applied_at")
        }
)
@Getter
@Setter
public class JobApplication extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posting_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_application_posting"))
    private JobPosting posting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_application_student"))
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @Column(name = "disqualified_at")
    private Instant disqualifiedAt;

    @Column(name = "disqualified_by")
    private Long disqualifiedBy;

    @Column(name = "disqualification_reason", length = 512)
    private String disqualificationReason;

    /**
     * EASY_APPLY is the student's own action. TNP_ADDED marks a candidate the
     * TnP added after the fact -- which by definition means already shortlisted
     * -- and keeps that path distinguishable in every count and export.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private ApplicationSource source = ApplicationSource.EASY_APPLY;

    /**
     * Which EligibilityRun authorised this application -- the one durable trace
     * of why it was accepted, given the row holds no profile snapshot.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eligibility_run_id",
            foreignKey = @ForeignKey(name = "fk_application_eligibility_run"))
    private EligibilityRun eligibilityRun;

    /** Set only when the student swapped in a resume other than their profile one. */
    /**
     * The resume this application was submitted with, snapshotted at apply time.
     *
     * Snapshot rather than live reference on purpose: a student who uploads a new
     * resume next month has not changed what a company already received, and the
     * applicant list months later must show the document actually submitted.
     *
     * The URL is stored alongside the key so that listing an applicant pool, or
     * exporting 120 rows to a spreadsheet, costs no calls to the storage
     * provider. Delivery is public by design -- see the access-policy note on
     * FileStorageService.
     */
    @Column(name = "resume_storage_key", length = 512)
    private String resumeStorageKey;

    @Column(name = "resume_url", length = 1024)
    private String resumeUrl;

    @Column(name = "resume_file_name", length = 255)
    private String resumeFileName;

    /** Stamped by the deadline sweep when the applicant pool is sealed. */
    @Column(name = "sealed_at")
    private Instant sealedAt;

    public Long getEligibilityRunId() { return eligibilityRun != null ? eligibilityRun.getId() : null; }
    /** Convenience accessor, matching UserRoleAssignment.getCollegeId(). */
    public Long getStudentUserId() { return student != null ? student.getId() : null; }
}
