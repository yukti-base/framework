package org.yuktisetu.model;

/**
 * One (student, matched target) pair from the targeting phase of eligibility
 * resolution -- a student whose college, department, degree and batch match one
 * of a posting's PostingTarget rows.
 *
 * Carrying the matched target id is what lets a multi-college drive say WHICH
 * target admitted a student, which matters when per-college criteria overrides
 * arrive later.
 */
public record TargetedStudent(Long studentUserId, Long targetId) {}
