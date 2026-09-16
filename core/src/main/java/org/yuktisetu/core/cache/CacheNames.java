package org.yuktisetu.core.cache;

/**
 * Cache names as constants, because a typo in a @Cacheable("postingCriteria")
 * string is silent: Caffeine simply creates a second, unused cache and the
 * eviction that was supposed to clear it never fires.
 */
public final class CacheNames {

    private CacheNames() {}

    /** Assembled eligibility criteria for one posting, keyed by posting id. */
    public static final String POSTING_CRITERIA = "postingCriteria";

    /** A posting's re-application policy, keyed by posting id. */
    public static final String POSTING_REAPPLICATION_POLICY = "postingReapplicationPolicy";

    /** The distinct college ids a posting targets -- read on every authorization check. */
    public static final String POSTING_TARGET_COLLEGES = "postingTargetColleges";

    /** Student-facing posting detail, keyed by posting id. */
    public static final String POSTING_DETAIL = "postingDetail";
}
