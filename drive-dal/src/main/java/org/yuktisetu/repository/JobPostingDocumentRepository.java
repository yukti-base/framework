package org.yuktisetu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.db.JobPostingDocument;
import org.yuktisetu.model.PostingDocumentType;

import java.util.List;

@Repository
public interface JobPostingDocumentRepository extends JpaRepository<JobPostingDocument, Long> {

    List<JobPostingDocument> findByPosting_IdAndIsDeletedFalse(Long postingId);

    /** Publish precondition: a posting must carry at least one JD document. */
    long countByPosting_IdAndDocTypeAndIsDeletedFalse(Long postingId, PostingDocumentType docType);
}
