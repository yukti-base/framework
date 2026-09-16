package org.yuktisetu.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.db.JobPosting;
import org.yuktisetu.model.PostingStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    Optional<JobPosting> findByIdAndIsDeletedFalse(Long id);

    Page<JobPosting> findByStatusAndIsDeletedFalse(PostingStatus status, Pageable pageable);

    Page<JobPosting> findByCompany_IdAndIsDeletedFalse(Long companyId, Pageable pageable);

    boolean existsByCompany_IdAndPostingCodeIgnoreCase(Long companyId, String postingCode);

    // Cascade guard for company hard delete.
    long countByCompany_Id(Long companyId);

    /**
     * List pages with the company already fetched.
     *
     * JOIN FETCH is load-bearing, not an optimisation: every row of the response
     * renders the company name, and with open-in-view disabled the lazy proxy
     * cannot initialise outside a transaction -- the plain findAll version threw
     * LazyInitializationException on the very first list request. Fetching it here
     * also removes one query per row.
     *
     * A ToOne fetch paginates in SQL, so unlike a collection fetch this does not
     * drag the whole table into memory.
     */
    @Query(value = """
            select p from JobPosting p
            join fetch p.company
            where p.isDeleted = false
            """,
            countQuery = "select count(p) from JobPosting p where p.isDeleted = false")
    Page<JobPosting> findAllWithCompany(Pageable pageable);

    @Query(value = """
            select p from JobPosting p
            join fetch p.company
            where p.isDeleted = false and p.status = :status
            """,
            countQuery = "select count(p) from JobPosting p where p.isDeleted = false and p.status = :status")
    Page<JobPosting> findByStatusWithCompany(@Param("status") PostingStatus status, Pageable pageable);

    @Query(value = """
            select p from JobPosting p
            join fetch p.company
            where p.isDeleted = false and p.company.id = :companyId
            """,
            countQuery = "select count(p) from JobPosting p where p.isDeleted = false and p.company.id = :companyId")
    Page<JobPosting> findByCompanyWithCompany(@Param("companyId") Long companyId, Pageable pageable);

    /** Single posting with its company, for the detail view. */
    @Query("select p from JobPosting p join fetch p.company where p.id = :id and p.isDeleted = false")
    Optional<JobPosting> findByIdWithCompany(@Param("id") Long id);

    /**
     * Read lock for the apply path.
     *
     * The race this closes: an apply reads status=PUBLISHED a millisecond before
     * the deadline, the sealing sweep then bulk-updates every APPLIED row to
     * IN_REVIEW, and the apply commits afterwards -- leaving one application
     * stranded at APPLIED, outside the sealed pool, invisible to the scoring
     * engine. A shared read lock here against the sweep's exclusive write lock
     * means the apply either commits before sealing starts, or blocks and then
     * fails its own status re-check once sealing has committed.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select p from JobPosting p where p.id = :id and p.isDeleted = false")
    Optional<JobPosting> findForApply(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from JobPosting p where p.id = :id and p.isDeleted = false")
    Optional<JobPosting> findForSealing(@Param("id") Long id);

    /**
     * The deadline sweep's query, served entirely by idx_posting_status_deadline.
     * Ids only -- each posting is then sealed in its own transaction, so one
     * failure cannot roll back the rest.
     */
    @Query("""
            select p.id from JobPosting p
            where p.status = :status
              and p.applicationDeadline <= :now
              and p.isDeleted = false
            order by p.applicationDeadline
            """)
    List<Long> findIdsDueForSealing(@Param("status") PostingStatus status,
                                    @Param("now") Instant now);
}
