package org.yuktisetu.drive.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.JobApplication;
import org.yuktisetu.drive.model.ApplicationStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    /**
     * Returns the row whether the student is currently APPLIED or WITHDRAWN --
     * re-applying revives this row rather than inserting a second one, which is
     * what lets uk_application_posting_student stay an absolute constraint.
     */
    Optional<JobApplication> findByPosting_IdAndStudent_IdAndIsDeletedFalse(Long postingId, Long studentUserId);

    boolean existsByPosting_IdAndStudent_IdAndIsDeletedFalse(Long postingId, Long studentUserId);

    Page<JobApplication> findByPosting_IdAndIsDeletedFalse(Long postingId, Pageable pageable);

    Page<JobApplication> findByPosting_IdAndStatusAndIsDeletedFalse(Long postingId, ApplicationStatus status, Pageable pageable);

    List<JobApplication> findByStudent_IdAndIsDeletedFalse(Long studentUserId);

    /**
     * The student's application status across many postings in ONE query.
     *
     * This replaces the N+1 in the drive list: rendering "Applied" or "Easy Apply"
     * per row used to cost one query per drive, so a student seeing 30 open drives
     * issued 31 queries to draw one screen. During a drive-day burst that multiplies
     * by every student refreshing at once.
     *
     * Returns (postingId, status) pairs for the caller to index into a map.
     */
    @Query("""
            select a.posting.id, a.status from JobApplication a
            where a.student.id = :studentUserId
              and a.posting.id in :postingIds
              and a.isDeleted = false
            """)
    List<Object[]> findStatusesForStudent(@Param("studentUserId") Long studentUserId,
                                         @Param("postingIds") Collection<Long> postingIds);

    /**
     * Batch lookup for bulk applicant add: every existing application for a set of
     * students on one posting, in a single query. The alternative is one findBy per
     * student inside the loop, which for a 200-row CSV is 200 round trips.
     */
    List<JobApplication> findByPosting_IdAndStudent_IdInAndIsDeletedFalse(Long postingId,
                                                                         Collection<Long> studentUserIds);

    long countByPosting_IdAndStatusAndIsDeletedFalse(Long postingId, ApplicationStatus status);

    /** Enforces JobPosting.maxApplications without loading the rows. */
    long countByPosting_IdAndIsDeletedFalseAndStatusNot(Long postingId, ApplicationStatus status);

    /**
     * One page of the applicant pool, fully joined.
     *
     * Native SQL returning a flat row rather than JobApplication entities, because
     * the TnP screen needs the student's name, email, college, department, batch,
     * CGPA and backlog count alongside each application. Walking lazy associations
     * for that would fire four or five extra SELECTs per applicant -- 250 queries
     * for a 50-row page -- which is exactly the N+1 this join replaces.
     *
     * LEFT joins on profile and role assignment on purpose: an applicant with an
     * incomplete profile must still appear in the pool, greyed out, rather than
     * silently vanishing from the coordinator's list.
     *
     * Columns: 0 applicationId, 1 studentUserId, 2 firstName, 3 lastName, 4 email,
     * 5 phone, 6 collegeId, 7 deptId, 8 batchYear, 9 cgpa, 10 activeBacklogs,
     * 11 status, 12 source, 13 appliedAt, 14 disqualificationReason
     */
    @Query(value = """
            SELECT a.id, u.id, u.first_name, u.last_name, u.email, u.phone,
                   ura.college_id, ura.dept_id,
                   sp.graduation_year, sp.cgpa, sp.active_backlogs,
                   a.status, a.source, a.applied_at, a.disqualification_reason,
                   a.resume_url, a.resume_file_name
            FROM job_applications a
            JOIN users u ON u.id = a.student_user_id
            LEFT JOIN student_profiles sp ON sp.user_id = u.id
            LEFT JOIN user_role_assignments ura
                   ON ura.user_id = u.id AND ura.role = 'STUDENT' AND ura.is_active = true
            WHERE a.posting_id = :postingId
              AND a.is_deleted = false
            ORDER BY a.applied_at, a.id
            LIMIT :pageSize OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findApplicantRows(@Param("postingId") Long postingId,
                                    @Param("pageSize") int pageSize,
                                    @Param("offset") long offset);

    /** Same, narrowed to one status. Two queries rather than a nullable parameter,
     *  which in native Postgres needs an explicit CAST and reads worse than this. */
    @Query(value = """
            SELECT a.id, u.id, u.first_name, u.last_name, u.email, u.phone,
                   ura.college_id, ura.dept_id,
                   sp.graduation_year, sp.cgpa, sp.active_backlogs,
                   a.status, a.source, a.applied_at, a.disqualification_reason,
                   a.resume_url, a.resume_file_name
            FROM job_applications a
            JOIN users u ON u.id = a.student_user_id
            LEFT JOIN student_profiles sp ON sp.user_id = u.id
            LEFT JOIN user_role_assignments ura
                   ON ura.user_id = u.id AND ura.role = 'STUDENT' AND ura.is_active = true
            WHERE a.posting_id = :postingId
              AND a.is_deleted = false
              AND a.status = :status
            ORDER BY a.applied_at, a.id
            LIMIT :pageSize OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findApplicantRowsByStatus(@Param("postingId") Long postingId,
                                            @Param("status") String status,
                                            @Param("pageSize") int pageSize,
                                            @Param("offset") long offset);

    /**
     * Status counts for a drive in ONE query, so the pool header does not issue a
     * count per status tab.
     */
    @Query("""
            select a.status, count(a) from JobApplication a
            where a.posting.id = :postingId and a.isDeleted = false
            group by a.status
            """)
    List<Object[]> countByStatusGrouped(@Param("postingId") Long postingId);

    long countByPosting_IdAndIsDeletedFalse(Long postingId);

    /**
     * Seals the pool in one statement rather than loading every applicant.
     * A drive with 2000 applicants must not become 2000 entity loads inside the
     * sweep's transaction while it holds a write lock on the posting.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update JobApplication a
               set a.status = :toStatus,
                   a.sealedAt = :sealedAt
             where a.posting.id = :postingId
               and a.status = :fromStatus
               and a.isDeleted = false
            """)
    int sealPool(@Param("postingId") Long postingId,
                 @Param("fromStatus") ApplicationStatus fromStatus,
                 @Param("toStatus") ApplicationStatus toStatus,
                 @Param("sealedAt") Instant sealedAt);
}
