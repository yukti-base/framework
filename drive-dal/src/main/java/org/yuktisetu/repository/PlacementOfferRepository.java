package org.yuktisetu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.db.PlacementOffer;
import org.yuktisetu.model.OfferStatus;

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
}
