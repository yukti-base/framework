package org.yuktisetu.drive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.ApplicationStatusHistory;

import java.util.List;

/**
 * Append-only by convention: this trail is what answers a student's dispute
 * about a status they screenshotted, so nothing here updates or deletes rows.
 */
@Repository
public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, Long> {

    List<ApplicationStatusHistory> findByApplication_IdOrderByChangedAtAsc(Long applicationId);
}
