package org.yuktisetu.profile.db;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.identity.db.User;
import org.yuktisetu.profile.model.StudentDocumentType;

import java.time.Instant;

/**
 * A file a student has uploaded -- resume, cover letter, identity or academic proof.
 *
 * Both the storage key and the delivery URL are persisted. The key is the durable
 * reference used to delete or re-sign the file; the URL is kept so that listing a
 * student's documents, or exporting 120 applicants to a spreadsheet, costs no
 * calls to the storage provider.
 *
 * `isPrimary` marks the CURRENT resume. A student may hold several, but exactly
 * one is the one an application attaches, and the partial unique index below
 * enforces that at the database rather than trusting the service to check.
 */
@Entity
@Table(
        name = "student_documents",
        indexes = {
                @Index(name = "idx_student_document_student", columnList = "student_user_id"),
                @Index(name = "idx_student_document_type", columnList = "student_user_id, doc_type")
        }
)
@Getter
@Setter
public class StudentDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_student_document_user"))
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 32)
    private StudentDocumentType docType;

    /** The name the student's machine gave it -- shown in the UI and used in exports. */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** Cloudinary public_id. The durable handle; delete and replace go through this. */
    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    /** Public, non-expiring delivery URL. See the access-policy note on FileStorageService. */
    @Column(name = "url", nullable = false, length = 1024)
    private String url;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    /** The current resume. At most one per student per type -- see uk_student_document_primary. */
    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Long getStudentUserId() { return student != null ? student.getId() : null; }
}
