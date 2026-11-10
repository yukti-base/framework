package org.yuktisetu.drive.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.OfferWithdrawalRequest;
import org.yuktisetu.drive.model.WithdrawalRequestStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OfferWithdrawalRequestRepository extends JpaRepository<OfferWithdrawalRequest, Long> {

    Optional<OfferWithdrawalRequest> findByOfferIdAndStatus(Long offerId, WithdrawalRequestStatus status);

    List<OfferWithdrawalRequest> findByOfferIdInOrderByRequestedAtDesc(Collection<Long> offerIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from OfferWithdrawalRequest r where r.id = :id")
    Optional<OfferWithdrawalRequest> lockById(@Param("id") Long id);
}
