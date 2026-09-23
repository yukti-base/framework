package org.yuktisetu.profile.db;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.yuktisetu.identity.db.User;

@Entity
@Table(name = "student_profiles",
        indexes = {
                // Drive targeting matches a posting's batch_year against this
                // column for every candidate. Without it the join degrades to a
                // scan of every student profile in the trust; with it a 2027-batch
                // drive touches only that batch. The trailing user_id makes the
                // batch-to-student direction index-only.
                @Index(name = "idx_student_profile_batch",
                        columnList = "graduation_year, user_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Basic Information
    private Date dateOfBirth;
    private String address;

    // Academic Information -- admin-controlled, sourced from the student's
    // College/Department (institute-dal, via UserRoleAssignment) and
    // UserRoleAssignment.degree at read time, NOT stored here. institution/
    // degree/branch used to live as free-text columns on this entity; they
    // were removed once College/Department/UserRoleAssignment.degree became
    // the single source of truth for those facts (see StudentProfileService).
    private Double cgpa;
    private Integer graduationYear;
    private Double tenthPercentage;
    private Double twelfthPercentage;
    private Double sem1Gpa;
    private Double sem2Gpa;
    private Double sem3Gpa;
    private Double sem4Gpa;
    private Double sem5Gpa;
    private Double sem6Gpa;
    private Double sem7Gpa;
    private Double sem8Gpa;
    private Double coCubesScore;
    private Double compositeScore;

    // Backlog / lateral-entry / gap facts. ERP-sourced like the rest of this
    // block. Added because drive eligibility gates cannot be evaluated without
    // them -- "no active backlogs" is the single most common criterion any
    // recruiter sets, and diplomaPercentage is what makes a 12th-percentage
    // gate survivable for diploma-lateral students, who have no 12th marks at
    // all. Nullable throughout: a profile that predates the ERP sync simply has
    // no value, and PostingEligibilityCriteria.missingDataPolicy decides what
    // that means per drive rather than guessing here.
    private Integer activeBacklogs;
    private Integer deadBacklogs;
    private Integer totalBacklogs;
    private Double diplomaPercentage;
    private Integer gapYears;

    // Skills — simple tag list
    @ElementCollection
    @CollectionTable(name = "student_skills", joinColumns = @JoinColumn(name = "student_profile_id"))
    @Column(name = "skill")
    @Builder.Default
    private List<String> skills = new ArrayList<>();

    @OneToMany(mappedBy = "studentProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CodingProfile> codingProfiles = new ArrayList<>();

    @OneToMany(mappedBy = "studentProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProfessionalProfile> professionalProfiles = new ArrayList<>();

    @OneToMany(mappedBy = "studentProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Project> projects = new ArrayList<>();

    @OneToMany(mappedBy = "studentProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkExperience> workExperiences = new ArrayList<>();

    @OneToMany(mappedBy = "studentProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Achievement> achievements = new ArrayList<>();

    private Date createdAt;
    private Date updatedAt;
}