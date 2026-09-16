package org.yuktisetu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.db.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Excludes soft-deleted rows by default — login/lookup paths must never
    // resolve a deleted account. Callers needing deleted rows (admin restore
    // screens) go through a separate explicit query, not this one.
    Optional<User> findByEmailIgnoreCaseAndIsDeletedFalse(String email);

    boolean existsByEmailIgnoreCaseAndIsDeletedFalse(String email);

    // The "separate explicit query" the note above refers to: admin restore and
    // audit screens legitimately need the rows the default lookups hide.
    Optional<User> findByIdAndIsDeletedTrue(Long id);

    // Cascade guard for hard delete — users.deleted_by points at another user,
    // so whoever soft-deleted someone cannot themselves be removed while that
    // reference still stands.
    long countByDeletedBy(Long actorUserId);
}
