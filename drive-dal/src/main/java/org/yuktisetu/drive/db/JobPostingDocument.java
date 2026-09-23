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
import org.yuktisetu.drive.model.PostingDocumentType;
import org.yuktisetu.persistence.BaseAuditableEntity;

/**
 * The JD document the company sent, plus anything else attached to the posting.
 * Only the storage key is held here -- the bytes live in object storage, so the
 * backing store can change without touching this table.
 */
@Entity
@Table(
        name = "job_posting_documents",
        uniqueConstraints = @UniqueConstraint(name = "uk_posting_document_storage_key", columnNames = "storage_key"),
        indexes = @Index(name = "idx_posting_document_posting_id", columnList = "posting_id")
)
@Getter
@Setter
public class JobPostingDocument extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posting_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_posting_document_posting"))
    private JobPosting posting;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 24)
    private PostingDocumentType docType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    /**
     * Public, non-expiring delivery URL, mirroring StudentDocument.url.
     *
     * Nullable because rows predating the upload endpoint were registered by
     * pasting a storage key alone and have no URL to recover. Readers must fall
     * back to the key rather than assume this is present.
     */
    @Column(name = "url", length = 1024)
    private String url;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;
}
