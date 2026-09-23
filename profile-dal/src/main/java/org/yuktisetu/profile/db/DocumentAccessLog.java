package org.yuktisetu.profile.db;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.profile.model.DocumentAccessReason;

import java.time.Instant;

/**
 * Records every time a document URL was HANDED OUT, and to whom.
 *
 * This is issuance logging, not fetch logging, and the distinction is
 * deliberate. Delivery URLs point straight at the storage provider, so the
 * actual download never touches this application and cannot be observed here.
 * What the backend does see -- and what this table captures -- is every request
 * that caused a URL to be produced: which document, which actor, under what
 * role, and why.
 *
 * In practice that answers the question the TnP Cell actually gets asked: who
 * was given access to this student's documents, and when. It cannot answer
 * whether the recipient opened the file, and nothing in the current design can.
 *
 * Bare Long ids rather than associations: this table is append-only and is read
 * for audit, never joined into a request path, and it must survive the document
 * row being deleted.
 */
@Entity
@Table(
        name = "document_access_logs",
        indexes = {
                @Index(name = "idx_doc_access_document", columnList = "document_id"),
                @Index(name = "idx_doc_access_student", columnList = "student_user_id, accessed_at"),
                @Index(name = "idx_doc_access_actor", columnList = "actor_user_id, accessed_at")
        }
)
@Getter
@Setter
public class DocumentAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Kept even after the document row is gone, which is when an audit matters most. */
    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "student_user_id", nullable = false)
    private Long studentUserId;

    @Column(name = "storage_key", length = 512)
    private String storageKey;

    /** Who asked. Null only for an unauthenticated path, which none currently is. */
    @Column(name = "actor_user_id")
    private Long actorUserId;

    /** The actor's role at the moment of issue -- roles change, the log must not. */
    @Column(name = "actor_role", length = 40)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 32)
    private DocumentAccessReason reason;

    /** For APPLICANT_EXPORT: which posting the download was for. */
    @Column(name = "context", length = 128)
    private String context;

    @Column(name = "accessed_at", nullable = false)
    private Instant accessedAt;
}
