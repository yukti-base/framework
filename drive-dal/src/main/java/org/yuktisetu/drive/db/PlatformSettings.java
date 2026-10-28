package org.yuktisetu.drive.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Platform-wide switches the TnP Super Admin controls. Exactly one row, id 1.
 *
 * Typed columns rather than key/value rows, so Hibernate's schema validation
 * covers every setting and a typo in a key cannot silently read as "off".
 */
@Entity
@Table(name = "platform_settings")
@Getter
@Setter
public class PlatformSettings {

    public static final short SINGLETON_ID = 1;

    @Id
    @Column(name = "id", nullable = false)
    private Short id = SINGLETON_ID;

    /**
     * Whether students may see their own composite score and its breakdown, on
     * drives whose shortlist has been published. Off by default; when off,
     * nothing about scores reaches a student anywhere.
     */
    @Column(name = "show_scores_to_students", nullable = false)
    private boolean showScoresToStudents = false;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
