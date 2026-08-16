package org.yuktisetu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.db.StudentProfile;

import java.util.Optional;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    // Primary lookup — every profile operation starts from the logged-in user's ID (from JWT).
    Optional<StudentProfile> findByUserIdAndUser_IsDeletedFalse(Long userId);

    boolean existsByUserIdAndUser_IsDeletedFalse(Long userId);
}