package org.yuktisetu.drive.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.yuktisetu.profile.model.CodingPlatform.*;

/**
 * The bar that stops a token account becoming a student's "best" platform.
 *
 * Judging a student on where they stand highest is right when the gap between
 * their accounts reflects where they put their effort. It is wrong for an account
 * made last week with five problems on it, which can land high purely because few
 * applicants use that platform. These tests pin the boundary.
 */
class CodingActivityPolicyTest {

    private static final CodingActivityPolicy POLICY = CodingActivityPolicy.withDefaults();

    private static CodingMetrics m(Integer rating, Integer contests, Integer solved) {
        return CodingMetrics.of(rating, contests, solved);
    }

    @Test
    @DisplayName("a five-problem account does not count as a platform")
    void tokenAccountIsRejected() {
        var tiny = m(null, 0, 5);
        assertFalse(POLICY.qualifies(LEETCODE, tiny));
        assertTrue(POLICY.rejectionReason(LEETCODE, tiny).orElseThrow()
                .contains("below the bar"));
        // The reason names the actual figures, because a student asking why their
        // LeetCode was ignored is owed more than "it did not count".
        assertTrue(POLICY.rejectionReason(LEETCODE, tiny).orElseThrow().contains("solved=5"));
    }

    @Test
    @DisplayName("steady practice qualifies even with no contests at all")
    void practiceRouteQualifies() {
        // Most students never enter a contest. Requiring one would exclude the
        // majority and leave the factor to a handful of competitive programmers.
        assertTrue(POLICY.qualifies(LEETCODE, m(null, 0, 120)));
        assertTrue(POLICY.qualifies(LEETCODE, m(null, null, 30)), "exactly at the bar qualifies");
        assertFalse(POLICY.qualifies(LEETCODE, m(null, null, 29)));
    }

    @Test
    @DisplayName("a rating from one contest does NOT qualify")
    void provisionalRatingRejected() {
        // A rating after one or two contests moves hundreds of points on the next
        // result. It says more about who turned up than about the student.
        assertFalse(POLICY.qualifies(CODEFORCES, m(1500, 1, null)));
        assertFalse(POLICY.qualifies(CODEFORCES, m(1500, 2, null)));
        assertTrue(POLICY.qualifies(CODEFORCES, m(1500, 3, null)));
    }

    @Test
    @DisplayName("contests without a rating do not qualify on a rated platform")
    void contestsAloneAreNotEnoughWhereARatingIsExpected() {
        // Entering unrated practice contests is not the same as having a standing.
        assertFalse(POLICY.qualifies(CODEFORCES, m(null, 10, null)));
    }

    @Test
    @DisplayName("Codeforces qualifies on contests alone, because we never read its solve count")
    void codeforcesHasNoSolvedRoute() {
        // Reading a Codeforces solve count means pulling every submission the user
        // has ever made -- the slowest call in the pipeline, for a figure the
        // rating already reflects. So there is no practice route for it.
        assertFalse(POLICY.qualifies(CODEFORCES, m(null, null, 500)));
        assertTrue(POLICY.qualifies(CODEFORCES, m(1600, 5, null)));
    }

    @Test
    @DisplayName("a platform we do not read can never qualify")
    void unreadPlatformNeverQualifies() {
        assertFalse(POLICY.qualifies(OTHER, m(2000, 50, 1000)));
        assertTrue(POLICY.rejectionReason(OTHER, m(2000, 50, 1000)).orElseThrow()
                .contains("not a platform we read"));
    }

    @Test
    @DisplayName("empty or missing figures never qualify")
    void emptyMetricsRejected() {
        assertFalse(POLICY.qualifies(LEETCODE, CodingMetrics.empty()));
        assertFalse(POLICY.qualifies(LEETCODE, null));
    }

    @Test
    @DisplayName("a rating is the strength measure where the platform has one")
    void strengthPrefersRating() {
        // A rating already embeds difficulty, consistency and the strength of the
        // field, so nothing is gained by recombining it with solve counts.
        assertEquals(0, POLICY.strength(CODEFORCES, m(1687, 12, null)).orElseThrow()
                .compareTo(BigDecimal.valueOf(1687)));
    }

    @Test
    @DisplayName("hard problems count far more than easy ones")
    void strengthWeightsDifficulty() {
        // Otherwise grinding a thousand easy problems outranks genuine depth.
        var grinder = new CodingMetrics(null, null, null, 300, 300, 0, 0,
                null, null, null, List.of(), Map.of());
        var deep = new CodingMetrics(null, null, null, 120, 20, 40, 60,
                null, null, null, List.of(), Map.of());

        BigDecimal g = POLICY.strength(LEETCODE, grinder).orElseThrow();
        BigDecimal d = POLICY.strength(LEETCODE, deep).orElseThrow();
        assertTrue(d.compareTo(g) > 0,
                "120 problems weighted towards hard (" + d + ") must beat 300 easy ones (" + g + ")");
    }

    @Test
    @DisplayName("a rejected platform yields no strength at all")
    void noStrengthWithoutQualifying() {
        assertTrue(POLICY.strength(LEETCODE, m(null, 0, 5)).isEmpty());
    }

    @Test
    @DisplayName("thresholds are tunable without touching the scorer")
    void barsAreConfigurable() {
        var strict = new CodingActivityPolicy(Map.of(
                LEETCODE, new CodingActivityPolicy.Bar(200, 20, true)));
        assertFalse(strict.qualifies(LEETCODE, m(null, null, 120)));
        assertTrue(strict.qualifies(LEETCODE, m(null, null, 250)));
    }

    @Test
    @DisplayName("a measured zero is not the same as a missing figure")
    void zeroVersusNull() {
        // 0 contests is a fact we established; null means we never found out. The
        // bar must not treat them alike, or a platform that simply does not publish
        // contest counts would reject everybody on it.
        assertFalse(POLICY.qualifies(CODEFORCES, m(1500, 0, null)), "0 contests is a real zero");
        assertFalse(POLICY.qualifies(CODEFORCES, m(1500, null, null)), "unknown is not a pass");
    }
}
