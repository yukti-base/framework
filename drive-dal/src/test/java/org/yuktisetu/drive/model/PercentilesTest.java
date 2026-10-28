package org.yuktisetu.drive.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The statistical properties the percentile has to have.
 *
 * These are not "does the code run" tests. Every student's published score rests
 * on this one function, so the properties below are the contract: an alternative
 * definition that passes compilation but fails these would shift the whole
 * cohort's numbers without anything else noticing.
 */
class PercentilesTest {

    private static List<BigDecimal> bd(double... xs) {
        List<BigDecimal> out = new ArrayList<>();
        for (double x : xs) {
            out.add(BigDecimal.valueOf(x));
        }
        return out;
    }

    @Test
    @DisplayName("the pool's mean percentile is EXACTLY 50 at every sample size")
    void meanIsExactlyFifty() {
        // The property that makes midrank the right choice. b/n has a mean of
        // 100*(n-1)/(2n) -- 25 at n=2, 40 at n=5 -- and (b+e)/n is its mirror
        // image above 50. Either would systematically shift every score on every
        // drive, in a direction that depends on the pool size.
        for (int n : new int[] {2, 3, 5, 10, 97, 500}) {
            List<BigDecimal> values = IntStream.range(0, n)
                    .mapToObj(BigDecimal::valueOf)
                    .collect(Collectors.toList());
            var dist = Percentiles.distribution(values);

            BigDecimal sum = values.stream()
                    .map(dist::percentileOf)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal mean = sum.divide(BigDecimal.valueOf(n), 6, java.math.RoundingMode.HALF_UP);

            assertEquals(0, mean.compareTo(new BigDecimal("50.000000")),
                    "n=" + n + " mean was " + mean + ", expected exactly 50");
        }
    }

    @Test
    @DisplayName("nobody is ever placed at exactly 0 or exactly 100")
    void noDegenerateEndpoints() {
        // A percentile of 0 would silently erase a student's entire weighted
        // contribution for that factor, making "worst in the pool" mean the same
        // as "no data". A 100 would claim they beat themselves.
        var dist = Percentiles.distribution(bd(1, 2, 3, 4, 5));
        assertTrue(dist.percentileOf(BigDecimal.valueOf(1)).signum() > 0,
                "lowest value must be above 0");
        assertTrue(dist.percentileOf(BigDecimal.valueOf(5)).compareTo(BigDecimal.valueOf(100)) < 0,
                "highest value must be below 100");
        assertEquals(0, dist.percentileOf(BigDecimal.valueOf(1)).compareTo(new BigDecimal("10.0000")));
        assertEquals(0, dist.percentileOf(BigDecimal.valueOf(5)).compareTo(new BigDecimal("90.0000")));
    }

    @Test
    @DisplayName("tied students get identical percentiles, and the mean still holds")
    void tiesAreFairAndUnbiased() {
        // The realistic case this is built for: most students have entered no
        // contests. 300 at zero, 100 above it.
        List<BigDecimal> values = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            values.add(BigDecimal.ZERO);
        }
        for (int i = 0; i < 100; i++) {
            values.add(BigDecimal.valueOf(5));
        }
        var dist = Percentiles.distribution(values);

        // b/n would put all 300 at 0.00 and (b+e)/n at 75.00 -- a 75-point swing
        // in what the same data means.
        assertEquals(0, dist.percentileOf(BigDecimal.ZERO).compareTo(new BigDecimal("37.5000")));
        assertEquals(0, dist.percentileOf(BigDecimal.valueOf(5)).compareTo(new BigDecimal("87.5000")));

        BigDecimal mean = values.stream().map(dist::percentileOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 6, java.math.RoundingMode.HALF_UP);
        assertEquals(0, mean.compareTo(new BigDecimal("50.000000")));
    }

    @Test
    @DisplayName("everyone identical means everyone gets 50, not a manufactured order")
    void allEqualCollapsesToFifty() {
        var dist = Percentiles.distribution(bd(7.5, 7.5, 7.5, 7.5));
        assertEquals(0, dist.percentileOf(BigDecimal.valueOf(7.5)).compareTo(new BigDecimal("50.0000")));
        // No none-students, so the none percentile is 0 and simply unused.
        assertEquals(0, dist.nonePercentile().compareTo(new BigDecimal("0.0000")));
    }

    @Test
    @DisplayName("7.5 and 7.50 are the same CGPA and tie")
    void scaleDoesNotSplitTies() {
        // BigDecimal.equals() distinguishes them, so an unnormalised map key would
        // put two students with the same CGPA in different tie groups -- and give
        // one of them a higher percentile than the other for nothing.
        var dist = Percentiles.distribution(
                List.of(new BigDecimal("7.5"), new BigDecimal("7.50"), new BigDecimal("9.0")));
        BigDecimal a = dist.percentileOf(new BigDecimal("7.5"));
        BigDecimal b = dist.percentileOf(new BigDecimal("7.500"));
        assertEquals(0, a.compareTo(b), "same value at different scales must tie");
        assertEquals(0, a.compareTo(new BigDecimal("33.3333")));
    }

    @Test
    @DisplayName("order of input does not change any result")
    void inputOrderIsIrrelevant() {
        var ascending = Percentiles.distribution(bd(1, 2, 3, 4, 5, 6, 7));
        var shuffled = Percentiles.distribution(bd(5, 1, 7, 3, 6, 2, 4));
        for (int v = 1; v <= 7; v++) {
            assertEquals(0,
                    ascending.percentileOf(BigDecimal.valueOf(v))
                            .compareTo(shuffled.percentileOf(BigDecimal.valueOf(v))),
                    "value " + v + " differed by input order");
        }
    }

    @Test
    @DisplayName("the none group sits strictly BELOW every measured value")
    void noneGroupRanksBelowEverything() {
        // The inversion this guards against: with the none group placed at the
        // lowest measured percentile instead, a student with no certifications
        // tied exactly with a student who had one weak certification.
        //
        // 2 none-students and 4 measured, so n=6. The none pair occupies places
        // 0 and 1: 100*(2/2)/6 = 16.6667. The lowest measured value then sits at
        // 100*(2 + 0.5)/6 = 41.6667 -- clear of them.
        var dist = Percentiles.distribution(bd(10, 20, 30, 40), 2);
        assertEquals(6, dist.sampleSize());
        assertEquals(0, dist.nonePercentile().compareTo(new BigDecimal("16.6667")));
        assertEquals(0, dist.percentileOf(BigDecimal.valueOf(10)).compareTo(new BigDecimal("41.6667")));
        assertTrue(dist.nonePercentile().compareTo(dist.percentileOf(BigDecimal.valueOf(10))) < 0,
                "having none must rank below having the least of something");
    }

    @Test
    @DisplayName("adding a none group keeps the pool mean at 50, within rounding")
    void noneGroupPreservesUnbiasedness() {
        // Exactly 50 in rational arithmetic: with m measured and z none the totals
        // come to 50*(z+m)^2/n = 50n, so the unbiasedness that makes midrank the
        // right choice survives the addition of a bottom tie group.
        //
        // Asserted to a bound rather than exactly, and the reason is worth
        // knowing. Percentiles are STORED rounded to 4 places, so each carries at
        // most 0.00005 of error. For distinct values those errors cancel pairwise
        // -- p_i and p_(n-1-i) round in opposite directions -- which is why the
        // all-distinct test above can demand exact equality. A none group breaks
        // that symmetry: its single rounded value is multiplied by the group size
        // with no counterpart, so the error survives into the mean. The bound is
        // still one rounding unit, which is four orders of magnitude below
        // anything any decision is made from.
        final BigDecimal bound = new BigDecimal("0.0001");
        for (int z : new int[] {1, 3, 7, 40}) {
            List<BigDecimal> measured = IntStream.range(0, 10)
                    .mapToObj(BigDecimal::valueOf).collect(Collectors.toList());
            var dist = Percentiles.distribution(measured, z);

            BigDecimal sum = measured.stream().map(dist::percentileOf)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(dist.nonePercentile().multiply(BigDecimal.valueOf(z)));
            BigDecimal mean = sum.divide(BigDecimal.valueOf(dist.sampleSize()), 8,
                    java.math.RoundingMode.HALF_UP);

            assertTrue(mean.subtract(new BigDecimal("50")).abs().compareTo(bound) <= 0,
                    "z=" + z + " gave a pool mean of " + mean + ", outside one rounding unit of 50");
        }
    }

    @Test
    @DisplayName("a pool where everyone has none puts them all at 50")
    void everybodyNone() {
        // Degenerate but real: a drive weighting certifications where no applicant
        // has any. All tie, and the factor separates nobody -- which is correct,
        // and is why this must not be special-cased into zeros.
        var dist = Percentiles.distribution(List.of(), 8);
        assertEquals(8, dist.sampleSize());
        assertEquals(0, dist.nonePercentile().compareTo(new BigDecimal("50.0000")));
    }

    @Test
    @DisplayName("scoring a value the distribution never saw is refused, not guessed")
    void unknownValueThrows() {
        // Silently returning a default here would mean a caller that built the
        // distribution from one set of students and scored another got plausible
        // but wrong percentiles for the whole pool.
        var dist = Percentiles.distribution(bd(1, 2, 3));
        assertThrows(IllegalArgumentException.class,
                () -> dist.percentileOf(BigDecimal.valueOf(99)));
    }

    @Test
    @DisplayName("an empty sample yields an empty distribution rather than dividing by zero")
    void emptySampleIsSafe() {
        var dist = Percentiles.distribution(List.of());
        assertEquals(0, dist.sampleSize());
    }
}
