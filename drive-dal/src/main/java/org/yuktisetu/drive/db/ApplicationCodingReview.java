package org.yuktisetu.drive.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.yuktisetu.drive.model.CodingReviewDecision;

import java.time.Instant;

/**
 * The TnP's decision on one flagged application, and who made it.
 *
 * This row is the answer to "why was I shortlisted / left out?" for a student
 * whose coding data could not be captured: the decision, the reason in the
 * reviewer's own words, the reviewer, and when. It may be changed while the
 * drive's scoring gate is open; once the gate is READY it is final.
 */
@Entity
@Table(
        name = "application_coding_reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_coding_review_application", columnNames = "application_id"),
        indexes = @Index(name = "idx_coding_review_posting", columnList = "posting_id, decision")
)
@Getter
@Setter
public class ApplicationCodingReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "posting_id", nullable = false)
    private Long postingId;

    @Column(name = "student_user_id", nullable = false)
    private Long studentUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 24)
    private CodingReviewDecision decision;

    @Column(name = "reason", nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "decided_by", nullable = false)
    private Long decidedBy;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
