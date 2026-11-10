package org.yuktisetu.drive.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.PlacementOffer;
import org.yuktisetu.drive.model.OfferStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlacementOfferRepository extends JpaRepository<PlacementOffer, Long> {

    Optional<PlacementOffer> findByIdAndIsDeletedFalse(Long id);

    List<PlacementOffer> findByStudent_IdAndIsDeletedFalse(Long studentUserId);

    /**
     * The re-application lookup, batched across a whole cohort so evaluating a
     * 2000-student drive is one query rather than 2000. Served by
     * idx_offer_student_status_type.
     */
    @Query("""
            select o from PlacementOffer o
            where o.student.id in :studentUserIds
              and o.status in :liveStatuses
              and o.isDeleted = false
            """)
    List<PlacementOffer> findLiveOffersForStudents(@Param("studentUserIds") Collection<Long> studentUserIds,
                                                   @Param("liveStatuses") Collection<OfferStatus> liveStatuses);

    long countByStudent_IdAndStatusInAndIsDeletedFalse(Long studentUserId, Collection<OfferStatus> statuses);

    // Cascade guards.
    long countByCompany_Id(Long companyId);
    long countByPosting_Id(Long postingId);

    /** The offers a drive's rounds recorded for these applications, live or not. */
    List<PlacementOffer> findByApplication_IdInAndIsDeletedFalse(Collection<Long> applicationIds);

    /**
     * Of these students, the ones whose most recent offer (by offer date, then id)
     * they declined or withdrew from. One query for a whole cohort; feeds
     * ReapplicationRules' allowIfLastOfferDeclined.
     */
    @Query(value = """
            SELECT student_user_id FROM (
                SELECT DISTINCT ON (student_user_id) student_user_id, status
                  FROM placement_offers
                 WHERE student_user_id IN (:studentUserIds) AND is_deleted = false
                 ORDER BY student_user_id, offer_date DESC, id DESC
            ) latest
            WHERE status IN ('DECLINED', 'WITHDRAWN')
            """, nativeQuery = true)
    List<Long> findStudentsWhoseLastOfferWasDeclined(@Param("studentUserIds") Collection<Long> studentUserIds);

    /** The offer, row-locked: every answer, withdrawal and revoke goes through this. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PlacementOffer o where o.id = :id and o.isDeleted = false")
    Optional<PlacementOffer> lockById(@Param("id") Long id);

    List<PlacementOffer> findByStudent_IdAndIsDeletedFalseOrderByOfferDateDesc(Long studentUserId);

    List<PlacementOffer> findByPosting_IdAndIsDeletedFalse(Long postingId);
}
