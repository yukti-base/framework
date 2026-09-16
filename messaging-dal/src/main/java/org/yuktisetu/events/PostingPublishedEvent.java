package org.yuktisetu.events;

import java.time.Instant;
import java.util.List;

public record PostingPublishedEvent(
        Long postingId,
        Long companyId,
        String companyName,
        String title,
        String jobType,
        Instant applicationDeadline,
        List<TargetRef> targets,
        Long publishedBy
) {
    public record TargetRef(Long collegeId, Long departmentId, Integer batchYear, String degree) {}
}
