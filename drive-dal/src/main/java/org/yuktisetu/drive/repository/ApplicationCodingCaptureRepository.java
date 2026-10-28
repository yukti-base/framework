package org.yuktisetu.drive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.ApplicationCodingCapture;
import org.yuktisetu.drive.model.CaptureState;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApplicationCodingCaptureRepository extends JpaRepository<ApplicationCodingCapture, Long> {

    /**
     * The captures a finished scrape job fills in. Plain read -- used only to
     * learn which drives' gates to lock before touching anything.
     */
    List<ApplicationCodingCapture> findByJobIdAndState(Long jobId, CaptureState state);

    /** The same, re-read under the gate locks, so two consumers never settle one capture twice. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "10000"))
    @Query("select c from ApplicationCodingCapture c where c.jobId = :jobId and c.state = :state")
    List<ApplicationCodingCapture> lockByJobIdAndState(@Param("jobId") Long jobId,
                                                      @Param("state") CaptureState state);

    /**
     * Retire every live capture of an application, on re-apply. One statement, so
     * a re-apply costs the same however many platforms the student lists.
     *
     * Deliberately NOT clearAutomatically. This runs in the middle of the apply
     * transaction, and clearing the persistence context there detaches the posting
     * and company the apply goes on to read -- every apply then fails with a
     * LazyInitializationException. No capture entity is loaded at this point, so
     * there is nothing stale to clear.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update ApplicationCodingCapture c
               set c.state = org.yuktisetu.drive.model.CaptureState.SUPERSEDED
             where c.applicationId = :applicationId
               and c.state <> org.yuktisetu.drive.model.CaptureState.SUPERSEDED
            """)
    int supersedeForApplication(@Param("applicationId") Long applicationId);

    /**
     * The sealed pool's captures that are still PENDING, LOCKED.
     *
     * The lock is the point, not a side effect. Under READ COMMITTED a plain count
     * cannot see a scrape result that another transaction is committing at this
     * very moment, so the deadline seal could count a capture as pending, the
     * finishing scrape could fail to see the not-yet-committed gate, and neither
     * would move the drive on. FOR UPDATE waits for that other transaction and
     * then re-checks the row, so a capture it settled is correctly no longer
     * pending here.
     *
     * Only applications still IN_REVIEW belong to the pool -- one the TnP has
     * disqualified no longer holds the drive up.
     */
    @Query(value = """
            SELECT c.id FROM application_coding_captures c
              JOIN job_applications a ON a.id = c.application_id
             WHERE c.posting_id = :postingId
               AND c.state = 'PENDING'
               AND a.status = 'IN_REVIEW'
               AND a.is_deleted = false
               FOR UPDATE OF c
            """, nativeQuery = true)
    List<Long> lockPendingInPool(@Param("postingId") Long postingId);

    /**
     * Pool-wide counts in one query: applicants, captured, failed, awaiting review.
     *
     * Columns: 0 poolSize, 1 captured, 2 failed, 3 needsReview.
     */
    @Query(value = """
            SELECT count(DISTINCT a.id),
                   count(c.id) FILTER (WHERE c.state = 'CAPTURED'),
                   count(c.id) FILTER (WHERE c.state = 'FAILED'),
                   count(c.id) FILTER (WHERE c.review_state = 'NEEDS_REVIEW')
              FROM job_applications a
              LEFT JOIN application_coding_captures c
                     ON c.application_id = a.id AND c.state <> 'SUPERSEDED'
             WHERE a.posting_id = :postingId
               AND a.status = 'IN_REVIEW'
               AND a.is_deleted = false
            """, nativeQuery = true)
    List<Object[]> poolCounts(@Param("postingId") Long postingId);

    /**
     * What the TnP is being asked to review, grouped for the notice.
     *
     * Columns: 0 platform, 1 outcome, 2 count.
     */
    @Query(value = """
            SELECT c.platform, c.outcome, count(*)
              FROM application_coding_captures c
              JOIN job_applications a ON a.id = c.application_id
             WHERE c.posting_id = :postingId
               AND c.review_state = 'NEEDS_REVIEW'
               AND c.state <> 'SUPERSEDED'
               AND a.status = 'IN_REVIEW'
               AND a.is_deleted = false
             GROUP BY c.platform, c.outcome
             ORDER BY 3 DESC
            """, nativeQuery = true)
    List<Object[]> reviewBreakdown(@Param("postingId") Long postingId);

    /**
     * The reconciler's repair query: pending captures in a pool whose scrape job
     * has already finished -- i.e. whose completion event was lost or not yet
     * delivered. Locked, for the same reason as lockPendingInPool.
     *
     * Columns: 0 captureId, 1 jobStatus, 2 lastStatus, 3 snapshotId, 4 lastError.
     */
    @Query(value = """
            SELECT c.id, j.status, j.last_status, j.snapshot_id, j.last_error
              FROM application_coding_captures c
              JOIN coding_scrape_jobs j ON j.id = c.job_id
             WHERE c.posting_id = :postingId
               AND c.state = 'PENDING'
               AND j.status IN ('DONE','FAILED')
               FOR UPDATE OF c
            """, nativeQuery = true)
    List<Object[]> lockSettledButPending(@Param("postingId") Long postingId);

    List<ApplicationCodingCapture> findByIdIn(Collection<Long> ids);

    List<ApplicationCodingCapture> findByApplicationIdAndStateNot(Long applicationId, CaptureState state);

    /**
     * Applications in the sealed pool with a flagged capture (open or already
     * resolved) -- the rows of the TnP's review screen.
     *
     * Columns: 0 applicationId, 1 studentUserId.
     */
    @Query(value = """
            SELECT DISTINCT a.id, a.student_user_id
              FROM application_coding_captures c
              JOIN job_applications a ON a.id = c.application_id
             WHERE c.posting_id = :postingId
               AND c.state <> 'SUPERSEDED'
               AND c.review_state IN ('NEEDS_REVIEW','RESOLVED')
               AND a.status = 'IN_REVIEW'
               AND a.is_deleted = false
             ORDER BY a.id
            """, nativeQuery = true)
    List<Object[]> flaggedApplicationsInPool(@Param("postingId") Long postingId);

    /** Live captures of the given applications, for the review screen. */
    @Query("""
            select c from ApplicationCodingCapture c
             where c.applicationId in :applicationIds
               and c.state <> org.yuktisetu.drive.model.CaptureState.SUPERSEDED
             order by c.applicationId, c.platform
            """)
    List<ApplicationCodingCapture> findLiveByApplicationIdIn(@Param("applicationIds") Collection<Long> applicationIds);

    /**
     * Mark the open flags on these applications resolved. Called under the gate
     * lock, after the decisions are written. Not clearAutomatically, for the
     * reason given on supersedeForApplication.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update ApplicationCodingCapture c
               set c.reviewState = org.yuktisetu.drive.model.CaptureReviewState.RESOLVED
             where c.applicationId in :applicationIds
               and c.reviewState = org.yuktisetu.drive.model.CaptureReviewState.NEEDS_REVIEW
               and c.state <> org.yuktisetu.drive.model.CaptureState.SUPERSEDED
            """)
    int resolveFlags(@Param("applicationIds") Collection<Long> applicationIds);

    // ------------------------------------------------------- student re-check

    /**
     * Pool captures still waiting on the student when the deadline sealed it.
     * Locked: the gate escalates them to the TnP (NO_RECHECK) under its own lock.
     */
    @Query(value = """
            SELECT c.id FROM application_coding_captures c
              JOIN job_applications a ON a.id = c.application_id
             WHERE c.posting_id = :postingId
               AND c.state = 'AWAITING_STUDENT'
               AND a.status = 'IN_REVIEW'
               AND a.is_deleted = false
             ORDER BY c.id
               FOR UPDATE OF c
            """, nativeQuery = true)
    List<Long> lockAwaitingInPool(@Param("postingId") Long postingId);

    /**
     * Pool captures that failed without a flag (the re-check failed on the handle
     * too, so the student is scored on their other platforms) whose application
     * has NO captured platform at all. There is nothing to score those students
     * on, so the gate escalates them to the TnP (NOTHING_LEFT). An application
     * already flagged for another platform is skipped: the TnP sees every one of
     * its captures anyway, and the student has already been told.
     *
     * Only meaningful once nothing in the pool is pending, which is the only time
     * the gate runs it.
     */
    @Query(value = """
            SELECT c.id FROM application_coding_captures c
              JOIN job_applications a ON a.id = c.application_id
             WHERE c.posting_id = :postingId
               AND c.state = 'FAILED'
               AND c.review_state = 'NOT_REQUIRED'
               AND a.status = 'IN_REVIEW'
               AND a.is_deleted = false
               AND NOT EXISTS (SELECT 1 FROM application_coding_captures o
                                WHERE o.application_id = c.application_id
                                  AND o.state = 'CAPTURED')
               AND NOT EXISTS (SELECT 1 FROM application_coding_captures o
                                WHERE o.application_id = c.application_id
                                  AND o.state <> 'SUPERSEDED'
                                  AND o.review_state IN ('NEEDS_REVIEW','RESOLVED'))
             ORDER BY c.id
               FOR UPDATE OF c
            """, nativeQuery = true)
    List<Long> lockNothingLeftInPool(@Param("postingId") Long postingId);

    /**
     * What a student is asked to re-check on one platform: captures awaiting them
     * on applications still open (APPLIED -- not yet sealed, not withdrawn).
     * Plain read, used to learn which postings to lock first.
     */
    @Query(value = """
            SELECT c.* FROM application_coding_captures c
              JOIN job_applications a ON a.id = c.application_id
             WHERE c.student_user_id = :studentUserId
               AND c.platform = :platform
               AND c.state = 'AWAITING_STUDENT'
               AND a.status = 'APPLIED'
               AND a.is_deleted = false
             ORDER BY c.id
            """, nativeQuery = true)
    List<ApplicationCodingCapture> findAwaitingForStudent(@Param("studentUserId") Long studentUserId,
                                                         @Param("platform") String platform);

    /** The same, re-read under the posting locks and limited to the postings still open. */
    @Query(value = """
            SELECT c.* FROM application_coding_captures c
              JOIN job_applications a ON a.id = c.application_id
             WHERE c.student_user_id = :studentUserId
               AND c.platform = :platform
               AND c.state = 'AWAITING_STUDENT'
               AND c.posting_id IN (:postingIds)
               AND a.status = 'APPLIED'
               AND a.is_deleted = false
             ORDER BY c.id
               FOR UPDATE OF c
            """, nativeQuery = true)
    List<ApplicationCodingCapture> lockAwaitingForStudent(@Param("studentUserId") Long studentUserId,
                                                         @Param("platform") String platform,
                                                         @Param("postingIds") Collection<Long> postingIds);

    /**
     * Every live capture on a student's live applications (applied, or in a sealed
     * pool), newest first -- the source of the student's coding-check banner.
     */
    @Query(value = """
            SELECT c.* FROM application_coding_captures c
              JOIN job_applications a ON a.id = c.application_id
             WHERE c.student_user_id = :studentUserId
               AND c.state <> 'SUPERSEDED'
               AND a.status IN ('APPLIED','IN_REVIEW')
               AND a.is_deleted = false
             ORDER BY c.requested_at DESC, c.id DESC
            """, nativeQuery = true)
    List<ApplicationCodingCapture> findLiveForStudent(@Param("studentUserId") Long studentUserId);
}
