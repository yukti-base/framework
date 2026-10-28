package org.yuktisetu.profile.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.yuktisetu.profile.model.CodingPlatform;

@Entity
@Table(name = "student_coding_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodingProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    /**
     * Enum-backed since 2026-10-06. It was free text, and the live data held both
     * "LeetCode" and "Leetcode" -- a scraper dispatching on the string would have
     * silently skipped most of the LeetCode profiles on record.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 32)
    private CodingPlatform platform;

    private String username;
    private String profileLink;

    /**
     * The rating the STUDENT typed, kept as-is because the TnP has always been
     * able to see what was claimed. It is not evidence and the scoring engine
     * never reads it -- scoring uses coding_profile_snapshots, which is what the
     * scraper actually observed on the platform.
     */
    private Double rating;
}