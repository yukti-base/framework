package org.yuktisetu.profile.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_work_experiences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkExperience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    private String company;
    private String role;

    /**
     * Structured dates, added 2026-10-06 because the Experience Relevance scoring
     * factor needs months of experience and that is not derivable from the free
     * text below. endDate is null while isCurrent is true.
     */
    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Builder.Default
    @Column(name = "is_current", nullable = false)
    private boolean isCurrent = false;

    /**
     * The original free text, e.g. "Jun 2025 - July 2025".
     *
     * Kept rather than dropped: it is what every existing row actually holds, it
     * is what the student typed, and the structured dates above cannot be
     * back-filled from it reliably. Display falls back to this when startDate is
     * null, so no existing profile loses information.
     */
    private String duration;

    @Column(length = 2000)
    private String description;
}