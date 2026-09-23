package org.yuktisetu.drive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.DrivePlacedStudentDecision;

import java.util.List;
import java.util.Optional;

@Repository
public interface DrivePlacedStudentDecisionRepository extends JpaRepository<DrivePlacedStudentDecision, Long> {

    Optional<DrivePlacedStudentDecision> findByPosting_IdAndStudent_IdAndIsDeletedFalse(Long postingId, Long studentUserId);

    /** Loaded once per evaluation run rather than per student. */
    List<DrivePlacedStudentDecision> findByPosting_IdAndIsDeletedFalse(Long postingId);
}
