package org.yuktisetu.drive.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.persistence.BaseAuditableEntity;

@Entity
@Table(
        name = "posting_locations",
        uniqueConstraints = @UniqueConstraint(name = "uk_posting_location", columnNames = {"posting_id", "city"}),
        indexes = @Index(name = "idx_posting_location_posting_id", columnList = "posting_id")
)
@Getter
@Setter
public class PostingLocation extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posting_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_posting_location_posting"))
    private JobPosting posting;

    @Column(name = "city", nullable = false, length = 96)
    private String city;

    @Column(name = "state", length = 96)
    private String state;

    @Column(name = "country", nullable = false, length = 96)
    private String country = "India";
}
