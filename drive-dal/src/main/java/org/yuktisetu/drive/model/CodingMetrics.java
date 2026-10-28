package org.yuktisetu.drive.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * One platform's figures for one student, as the scraper service recorded them.
 *
 * Mirrors the normalised shape written into coding_profile_snapshots.metrics by
 * yuktisetu-scraper. The two must stay in step; a field added on one side and not
 * the other is simply ignored rather than failing loudly, which is the one real
 * hazard of a shared database with two languages over it.
 *
 * EVERY FIELD IS NULLABLE, and that is load-bearing rather than defensive. A
 * figure the platform does not publish, or that could not be read, must be null
 * and never zero: a zero is a measurement that drags a student to the bottom of a
 * ranking, where a null is an absence that can be excluded. Codeforces, for
 * instance, omits rating entirely for an account that has never entered a rated
 * contest -- treating that as a rating of 0 would place a complete beginner below
 * every rated student rather than outside the comparison.
 *
 * The one deliberate exception is a MEASURED zero. An account with genuinely zero
 * contests attended holds 0, not null, because we asked and the true answer is
 * none. Null means "we do not know", zero means "we know it is none".
 */
public record CodingMetrics(
        Integer rating,
        Integer maxRating,
        Integer contestsAttended,
        Integer solvedTotal,
        Integer solvedEasy,
        Integer solvedMedium,
        Integer solvedHard,
        Integer globalRank,
        BigDecimal topPercent,
        Integer stars,
        List<String> languages,
        Map<String, Integer> topics
) {
    public CodingMetrics {
        languages = languages == null ? List.of() : List.copyOf(languages);
        topics = topics == null ? Map.of() : Map.copyOf(topics);
    }

    /** Nothing usable at all. */
    public boolean isEmpty() {
        return rating == null && maxRating == null && contestsAttended == null
                && solvedTotal == null && globalRank == null && stars == null
                && languages.isEmpty() && topics.isEmpty();
    }

    public static CodingMetrics empty() {
        return new CodingMetrics(null, null, null, null, null, null, null,
                null, null, null, List.of(), Map.of());
    }

    /** Builder-ish helper for the common case and for tests. */
    public static CodingMetrics of(Integer rating, Integer contestsAttended, Integer solvedTotal) {
        return new CodingMetrics(rating, null, contestsAttended, solvedTotal,
                null, null, null, null, null, null, List.of(), Map.of());
    }

    int intOr(Integer v, int fallback) {
        return v == null ? fallback : v;
    }
}
