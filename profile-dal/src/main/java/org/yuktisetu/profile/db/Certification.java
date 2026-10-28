package org.yuktisetu.profile.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.yuktisetu.profile.model.CertificationVerificationMethod;
import org.yuktisetu.profile.model.CertificationVerificationStatus;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A certificate or credential a student claims, and what we were able to confirm
 * about it.
 *
 * New on 2026-10-06. Before this, certificates lived in Achievement as a title and
 * a free-text description, which cannot be verified and cannot be scored -- and
 * Certifications is one of the five composite-scoring factors, so that gap blocked
 * the factor entirely.
 *
 * Verification runs at profile-update time, not at scoring time. That is a settled
 * decision and it is also the only workable one: the Tier-3 LLM trace is a bounded
 * serial resource, and putting it on the deadline path would make a drive's
 * shortlisting wait on it.
 */
@Entity
@Table(
        name = "student_certifications",
        indexes = {
                @Index(name = "idx_certification_student_profile", columnList = "student_profile_id"),
                // The TnP review queue's only query: everything waiting on a human.
                @Index(name = "idx_certification_verification_status", columnList = "verification_status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_certification_student_profile"))
    private StudentProfile studentProfile;

    /** What the student says it is. */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Who they say issued it: "Amazon Web Services", "NPTEL", "Coursera". */
    @Column(name = "issuer", length = 255)
    private String issuer;

    @Column(name = "issued_on")
    private LocalDate issuedOn;

    /** Null when the credential does not expire. */
    @Column(name = "expires_on")
    private LocalDate expiresOn;

    /** The issuer's own identifier, where there is one. */
    @Column(name = "credential_id", length = 255)
    private String credentialId;

    /**
     * The public verification link. This is the single most important field on the
     * row: with it, Tier 1 or Tier 2 can usually settle the certificate outright,
     * and without it the only paths left are the LLM trace or nothing.
     */
    @Column(name = "credential_url", length = 1024)
    private String credentialUrl;

    // @Builder.Default is load-bearing, not tidiness: without it Lombok's builder
    // ignores the initializer, Certification.builder()...build() leaves this null,
    // and the insert fails the NOT NULL constraint. The whole point of this field
    // is that a certificate starts out unverified by default.
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 24)
    private CertificationVerificationStatus verificationStatus = CertificationVerificationStatus.PENDING;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method", nullable = false, length = 24)
    private CertificationVerificationMethod verificationMethod = CertificationVerificationMethod.NONE;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    /**
     * Whatever the verifying adapter saw, verbatim: the badge assertion, the
     * issuer page's extracted fields, or the LLM's proposal with the source URLs
     * it used.
     *
     * Kept because a student disputing their score is owed the specific evidence,
     * and because an adapter that turns out to be wrong can then be re-run over the
     * stored payload rather than re-fetching every credential.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "verification_payload", columnDefinition = "jsonb")
    private String verificationPayload;

    /** Who decided, when a TnP member confirmed or rejected an LLM proposal. */
    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    /** Why the status is what it is, in words, for the student and the TnP. */
    @Column(name = "verification_note", length = 512)
    private String verificationNote;

    /**
     * True when this credential can contribute to a composite score right now.
     *
     * Expiry is checked here rather than only at verification time: a certificate
     * verified in June and expired by November is no longer a credential, and the
     * scoring engine must not be the place that first notices.
     */
    public boolean isScorable() {
        if (!verificationStatus.countsTowardScore()) {
            return false;
        }
        return expiresOn == null || !expiresOn.isBefore(LocalDate.now());
    }
}
