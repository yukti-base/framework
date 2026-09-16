package org.yuktisetu.model;

/**
 * COMPOSITE_SCORE runs the scoring engine at deadline. COMPANY_SIDE computes no
 * score at all: the hard-gate survivors are sent to the company as-is and the
 * company shortlists. Recorded on the posting because it decides what a student
 * can be shown about their own outcome afterwards.
 */
public enum ShortlistingMode {
    COMPOSITE_SCORE,
    COMPANY_SIDE
}
