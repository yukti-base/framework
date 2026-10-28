package org.yuktisetu.drive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.JobPosting;
import org.yuktisetu.drive.db.PostingEligibleStudent;
import org.yuktisetu.drive.model.PostingStatus;
import org.yuktisetu.identity.model.NotifyTarget;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostingEligibleStudentRepository extends JpaRepository<PostingEligibleStudent, Long> {

    /** The visibility check behind both GET /postings/{id} and the apply endpoint. */
    boolean existsByPosting_IdAndStudent_IdAndIsDeletedFalse(Long postingId, Long studentUserId);

    List<PostingEligibleStudent> findByPosting_IdAndIsDeletedFalse(Long postingId);

    /** The notification audience -- by construction the same set that can see the drive. */
    List<PostingEligibleStudent> findByPosting_IdAndNotifiedFalseAndIsDeletedFalse(Long postingId);

    long countByPosting_IdAndIsDeletedFalse(Long postingId);

    /**
     * The student's drive list.
     *
     * A JOIN through this table, never a client-side or service-side filter over
     * all postings: eligibility is enforced by the query shape itself, so there is
     * no code path in which an ineligible drive can reach a student's screen.
     * Served by idx_eligible_student_user_id.
     *
     * applicationOpensAt gates early visibility -- a drive can be published and
     * resolved before students are meant to see it.
     */
    @Query("""
            select e.posting from PostingEligibleStudent e
            join fetch e.posting.company
            where e.student.id = :studentUserId
              and e.isDeleted = false
              and e.posting.isDeleted = false
              and e.posting.status in :statuses
              and (e.posting.applicationOpensAt is null or e.posting.applicationOpensAt <= :now)
            order by e.posting.applicationDeadline asc
            """)
    List<JobPosting> findVisiblePostingsForStudent(@Param("studentUserId") Long studentUserId,
                                                   @Param("statuses") Collection<PostingStatus> statuses,
                                                   @Param("now") Instant now);

    /**
     * One visible posting for one student, fetched directly.
     *
     * Exists so the detail and apply paths do not have to pull the student's whole
     * visible list and filter it in memory -- which is O(drives) work and O(drives)
     * rows over the wire to answer a single-row question.
     *
     * JOIN FETCH on the company because every caller renders the company name, and
     * without it Hibernate issues a second SELECT the moment that field is touched.
     */
    @Query("""
            select e.posting from PostingEligibleStudent e
            join fetch e.posting.company
            where e.student.id = :studentUserId
              and e.posting.id = :postingId
              and e.isDeleted = false
              and e.posting.isDeleted = false
            """)
    Optional<JobPosting> findVisiblePostingForStudent(@Param("studentUserId") Long studentUserId,
                                                      @Param("postingId") Long postingId);

    /** A re-evaluation replaces the index wholesale rather than diffing it. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PostingEligibleStudent e where e.posting.id = :postingId")
    int deleteAllForPosting(@Param("postingId") Long postingId);

    /**
     * The drive blast audience, as addresses rather than entities.
     *
     * Projects straight into NotifyTarget so a 5000-student cohort costs one
     * query and 5000 four-field records -- not 5000 managed User entities whose
     * lazy associations the notification builder would then touch one at a time.
     *
     * notified = false is what makes the blast resumable: a re-run after a crash
     * mid-send picks up exactly the students who have not been told yet, and a
     * re-evaluation that adds newly-eligible students notifies only the new ones.
     *
     * The status filters repeat the standing rule -- an account that has since
     * been suspended or soft-deleted must not be mailed, even though its
     * eligibility row is still valid.
     */
    @Query("""
            select new org.yuktisetu.identity.model.NotifyTarget(
                       e.student.id, e.student.email, e.student.firstName, e.student.lastName)
              from PostingEligibleStudent e
             where e.posting.id = :postingId
               and e.notified = false
               and e.isDeleted = false
               and e.student.isDeleted = false
               and e.student.status = org.yuktisetu.identity.model.UserStatus.ACTIVE
             order by e.student.id
            """)
    List<NotifyTarget> findUnnotifiedRecipients(@Param("postingId") Long postingId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PostingEligibleStudent e
               set e.notified = true
             where e.posting.id = :postingId
               and e.student.id in :studentUserIds
            """)
    int markNotified(@Param("postingId") Long postingId,
                     @Param("studentUserIds") List<Long> studentUserIds);
}
