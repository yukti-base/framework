package org.yuktisetu.drive.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yuktisetu.profile.model.CodingPlatform;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.yuktisetu.profile.model.CodingPlatform.*;

/** Judging a student on the platform they are strongest on. */
class CodingProficiencyTest {

    private static Map<Long, Map<CodingPlatform, BigDecimal>> pool() {
        return new HashMap<>();
    }

    private static void put(Map<Long, Map<CodingPlatform, BigDecimal>> p, long id, Object... pairs) {
        Map<CodingPlatform, BigDecimal> m = new EnumMap<>(CodingPlatform.class);
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((CodingPlatform) pairs[i], BigDecimal.valueOf(((Number) pairs[i + 1]).doubleValue()));
        }
        p.put(id, m);
    }

    @Test
    @DisplayName("the platform with the higher PERCENTILE wins, not the bigger raw number")
    void bestPlatformIsByPercentileNotRawValue() {
        // The case stated in the requirement: a student near the top of CodeChef
        // but mid-table on LeetCode is a CodeChef competitor, whatever the two
        // underlying numbers look like. Raw LeetCode figures here are ten times
        // the CodeChef ones and must not decide anything.
        var p = pool();
        put(p, 1L, LEETCODE, 15000, CODECHEF, 1900);   // mid on LC, top on CC
        for (long i = 2; i <= 7; i++) {
            put(p, i, LEETCODE, 10000 + i * 2000, CODECHEF, 1200 + i * 50);
        }
        for (long i = 8; i <= 12; i++) {
            put(p, i, CODECHEF, 1100 + i * 10);
        }

        var out = CodingProficiency.resolve(p, 5);
        var a = out.assignments().get(1L);
        assertEquals(CODECHEF, a.platform(),
                "student 1 stands higher among CodeChef users and must be judged there");
        assertTrue(a.consideredPercentiles().get(CODECHEF)
                        .compareTo(a.consideredPercentiles().get(LEETCODE)) > 0,
                "the chosen platform must be the higher percentile");
    }

    @Test
    @DisplayName("a student judged elsewhere is removed from the cohort they left")
    void cohortRemovalProtectsTheStudentsWhoStay() {
        // The fairness point: a strong competitor who is NOT being measured on
        // LeetCode must stop pushing down the LeetCode percentiles of those who
        // are. Student 1 is the strongest CodeChef user and is assigned there, so
        // LeetCode's cohort shrinks and the remaining LeetCode students rise.
        var p = pool();
        // Student 1 tops CodeChef and is only mid-table on LeetCode.
        put(p, 1L, LEETCODE, 1045, CODECHEF, 3000);
        // Ten LeetCode-leaning students, five CodeChef-leaning ones, so both
        // cohorts stay above the minimum after student 1 moves.
        for (long i = 2; i <= 11; i++) {
            put(p, i, LEETCODE, 1000 + i * 10, CODECHEF, 100 + i);
        }
        for (long i = 12; i <= 16; i++) {
            put(p, i, LEETCODE, 500 + i, CODECHEF, 1500 + i * 10);
        }

        var out = CodingProficiency.resolve(p, 5);
        assertFalse(out.fellBackToFullCohorts(), "both cohorts should survive removal here");
        assertEquals(CODECHEF, out.assignments().get(1L).platform());

        int leetcodeCohort = out.assignments().values().stream()
                .filter(a -> a.platform() == LEETCODE)
                .mapToInt(CodingProficiency.Assignment::cohortSize)
                .findFirst().orElseThrow();
        long leetcodeAssigned = out.assignments().values().stream()
                .filter(a -> a.platform() == LEETCODE).count();
        assertEquals(leetcodeAssigned, leetcodeCohort,
                "the LeetCode cohort must be exactly the students judged on LeetCode");
        assertTrue(out.assignments().get(1L).consideredPercentiles().containsKey(LEETCODE),
                "the LeetCode standing they left behind is still reported to them");
    }

    @Test
    @DisplayName("a platform too thinly used is dropped and its students fall back")
    void thinPlatformIsDroppedAndStudentsReassigned() {
        // Two GeeksforGeeks users cannot produce a meaningful percentile -- the
        // method would force them 50 points apart however close they really are.
        var p = pool();
        put(p, 1L, LEETCODE, 1500, GEEKSFORGEEKS, 900);
        put(p, 2L, LEETCODE, 1600, GEEKSFORGEEKS, 800);
        for (long i = 3; i <= 9; i++) {
            put(p, i, LEETCODE, 1000 + i * 50);
        }

        var out = CodingProficiency.resolve(p, 5);
        assertTrue(out.droppedPlatforms().containsKey(GEEKSFORGEEKS));
        assertEquals(LEETCODE, out.assignments().get(1L).platform(),
                "a student on a dropped platform falls back to their next-best");
        assertTrue(out.unresolved().isEmpty());
    }

    @Test
    @DisplayName("a student whose only platform is dropped has coding unmeasurable")
    void studentWithOnlyAThinPlatformIsUnresolved() {
        // Not scored zero: nothing was measured about their coding, so the scorer
        // rescales it away rather than punishing them for it.
        var p = pool();
        put(p, 99L, HACKEREARTH, 500);
        for (long i = 1; i <= 8; i++) {
            put(p, i, LEETCODE, 1000 + i * 50);
        }
        var out = CodingProficiency.resolve(p, 5);
        assertTrue(out.unresolved().contains(99L));
        assertFalse(out.assignments().containsKey(99L));
    }

    @Test
    @DisplayName("no platform viable at all leaves every student unresolved")
    void nothingViable() {
        var p = pool();
        put(p, 1L, LEETCODE, 1000);
        put(p, 2L, CODECHEF, 1000);
        var out = CodingProficiency.resolve(p, 5);
        assertEquals(2, out.unresolved().size());
        assertTrue(out.assignments().isEmpty());
    }

    @Test
    @DisplayName("the result is identical however the students are iterated")
    void deterministic() {
        var p1 = pool();
        var p2 = pool();
        for (long i = 1; i <= 12; i++) {
            put(p1, i, LEETCODE, 1000 + i * 37 % 400, CODECHEF, 1000 + i * 53 % 400);
        }
        p1.keySet().stream().sorted(java.util.Comparator.reverseOrder())
                .forEach(k -> p2.put(k, p1.get(k)));

        var a = CodingProficiency.resolve(p1, 5);
        var b = CodingProficiency.resolve(p2, 5);
        for (Long id : p1.keySet()) {
            assertEquals(a.assignments().get(id).platform(), b.assignments().get(id).platform());
            assertEquals(a.assignments().get(id).percentile().toPlainString(),
                    b.assignments().get(id).percentile().toPlainString());
        }
    }

    @Test
    @DisplayName("the breakdown names every platform considered, not just the winner")
    void traceShowsWhatWasRejected() {
        // "Why am I judged on CodeChef when LeetCode is my main account?" is a
        // question a student will ask, and the comparison is the answer.
        var p = pool();
        for (long i = 1; i <= 14; i++) {
            put(p, i, LEETCODE, 1000 + i * 10, CODECHEF, 2000 - i * 7);
        }
        var out = CodingProficiency.resolve(p, 5);
        var a = out.assignments().get(3L);
        assertNotNull(a);
        assertEquals(2, a.consideredPercentiles().size());
        assertTrue(a.consideredPercentiles().containsKey(LEETCODE));
        assertTrue(a.consideredPercentiles().containsKey(CODECHEF));
    }

    @Test
    @DisplayName("when removal would kill every platform, keep coding and skip the removal")
    void fallsBackToFullCohortsRatherThanLosingCoding() {
        // Eight students across two perfectly opposed platforms split four and
        // four. Both platforms were usable before anyone moved, and neither is
        // afterwards. Dropping coding here would score nobody on a drive that may
        // weight coding at 100, so the removal is abandoned instead of the factor.
        var p = pool();
        for (long i = 1; i <= 8; i++) {
            put(p, i, LEETCODE, 1000 + i * 10, CODECHEF, 2000 - i * 10);
        }
        var out = CodingProficiency.resolve(p, 5);

        assertTrue(out.fellBackToFullCohorts());
        assertEquals(8, out.assignments().size(), "every student must still be scored");
        assertTrue(out.unresolved().isEmpty());
        // Cohorts are the full ones, so nobody was removed from anything.
        out.assignments().values().forEach(a ->
                assertEquals(8, a.cohortSize(), "the full cohort should be used under fallback"));
    }
}
