package org.yuktisetu.profile.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.profile.db.DocumentAccessLog;

@Repository
public interface DocumentAccessLogRepository extends JpaRepository<DocumentAccessLog, Long> {

    /** "Who was given access to this student's documents?" -- the compliance question. */
    Page<DocumentAccessLog> findByStudentUserIdOrderByAccessedAtDesc(Long studentUserId, Pageable pageable);

    Page<DocumentAccessLog> findByActorUserIdOrderByAccessedAtDesc(Long actorUserId, Pageable pageable);

    Page<DocumentAccessLog> findByDocumentIdOrderByAccessedAtDesc(Long documentId, Pageable pageable);
}
