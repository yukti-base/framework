package org.yuktisetu.drive.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Midrank percentiles over an applicant pool.
 *
 * WHY MIDRANK, and not either of the two obvious alternatives. For a value v in a
 * sample of size n, write b for the count strictly below it and e for the count
 * equal to it. The three candidate definitions behave very differently:
 *
 *   b / n           the pool's mean percentile is 100*(n-1)/(2n), which is BELOW
 *                   50 -- at n=2 it is 25, at n=5 it is 40. It also gives the
 *                   lowest student exactly 0, which silently erases their entire
 *                   weighted contribution for that factor.
 *
 *   (b + e) / n     the mirror-image fault: the pool mean is ABOVE 50 (75 at n=2,
 *                   60 at n=5) and the top student gets exactly 100.
 *
 *   (b + e/2) / n   the pool mean is EXACTLY 50 at every n, with no value at 0 or
 *                   100. This is the one used here.
 *
 * Ties matter more than the formula suggests. Take 300 students who have entered
 * no contests and 100 who have: b/n puts all 300 at 0.00, (b+e)/n puts them at
 * 75.00, and midrank puts them at 37.50 with a pool mean of exactly 50. The first
 * two are not rounding differences, they are a 75-point swing in what the same
 * data means -- and student scores are published, so the choice has to be
 * defensible rather than incidental.
 *
 * Determinism: every division is BigDecimal with an explicit scale and HALF_UP.
 * These numbers are shown to students and must reproduce byte-for-byte on a
 * re-run, so no double arithmetic appears anywhere in this class.
 */
public final class Percentiles {

    private Percentiles() {}

    /** Working precision. Four places is far finer than any decision made from it. */
    public static final int SCALE = 4;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    /**
     * A precomputed percentile lookup over one factor's values.
     *
     * Built once per factor per run in O(n log n) -- sort, then one pass over the
     * distinct values -- and queried in O(1). The naive form, counting the pool
     * for each student in turn, is O(n squared): at 2000 applicants that is four
     * million comparisons per factor to produce the same answer.
     */
    public static final class Distribution {

        private final Map<BigDecimal, BigDecimal> byValue;
        private final int sampleSize;
        private final BigDecimal floorPercentile;

        private Distribution(Map<BigDecimal, BigDecimal> byValue, int sampleSize,
                             BigDecimal floorPercentile) {
            this.byValue = byValue;
            this.sampleSize = sampleSize;
            this.floorPercentile = floorPercentile;
        }

        /** How many students this distribution was built from. */
        public int sampleSize() {
            return sampleSize;
        }

        /**
         * The percentile of a value that was part of the sample.
         *
         * Throws rather than returning a default for a value that was not in the
         * sample: that can only mean the caller built the distribution from one
         * set of students and is scoring another, which would produce quietly
         * wrong percentiles for everyone.
         */
        public BigDecimal percentileOf(BigDecimal value) {
            BigDecimal p = byValue.get(normalise(value));
            if (p == null) {
                throw new IllegalArgumentException(
                        "value " + value + " was not in the sample this distribution was built from");
            }
            return p;
        }

        /**
         * Where the MeasuredNone students sit: their own tie group BELOW every
         * measured value.
         *
         * Strictly below, not equal to the lowest measured value. Placing them at
         * the lowest measured percentile instead made "no certifications" tie
         * exactly with "one weak certification", which is the inversion this whole
         * design exists to avoid -- having nothing must rank below having
         * something, however little.
         *
         * Also not zero: a zero would erase the factor's weighted contribution
         * entirely, overstating the gap between "nothing" and "the least of
         * anything". With z none-students in a pool of n the group sits at
         * 50*z/n, which is exactly the midrank of a tie group occupying the
         * bottom z places.
         */
        public BigDecimal nonePercentile() {
            return floorPercentile;
        }
    }

    /** Convenience for a factor where nobody reported a genuine absence. */
    public static Distribution distribution(Collection<BigDecimal> values) {
        return distribution(values, 0);
    }

    /**
     * Build a distribution for one factor.
     *
     * @param values    the raw values of students who HAVE a number
     * @param noneCount how many students were measured as having NONE of this
     *                  factor -- no certifications, no work experience. They are
     *                  part of the sample and occupy the bottom noneCount places,
     *                  as a single tie group.
     *
     * Students whose value could not be MEASURED at all are deliberately absent
     * from both arguments. Counting them anywhere would shift everybody else's
     * percentile, which is the subtle way an outage on one platform ends up
     * changing the rank of students who do not even use that platform.
     *
     * The mean over the whole sample is still exactly 50. With m measured and z
     * none, the none group sits at 50z/n and the measured values start above it;
     * the totals work out to 50(z+m)^2/n = 50n, so the property that makes
     * midrank unbiased survives the addition.
     */
    public static Distribution distribution(Collection<BigDecimal> values, int noneCount) {
        if (noneCount < 0) {
            throw new IllegalArgumentException("noneCount cannot be negative");
        }
        List<BigDecimal> sorted = new ArrayList<>(values.size());
        for (BigDecimal v : values) {
            if (v != null) {
                sorted.add(normalise(v));
            }
        }
        Collections.sort(sorted);

        int measured = sorted.size();
        int n = measured + noneCount;
        Map<BigDecimal, BigDecimal> byValue = new HashMap<>();
        if (n == 0) {
            return new Distribution(byValue, 0, BigDecimal.ZERO.setScale(SCALE));
        }

        BigDecimal nBd = BigDecimal.valueOf(n);

        // The none group occupies places 0..noneCount-1, so below=0 and
        // equal=noneCount: 100 * (noneCount/2) / n.
        BigDecimal nonePercentile = BigDecimal.valueOf(noneCount).multiply(HUNDRED)
                .divide(TWO.multiply(nBd), SCALE, RoundingMode.HALF_UP);
        // One pass over runs of equal values. Every member of a run gets the
        // SAME percentile, which is the property that makes ties fair: no
        // student gains from an arbitrary ordering inside a tie group.
        int i = 0;
        while (i < measured) {
            BigDecimal value = sorted.get(i);
            int runStart = i;
            while (i < measured && sorted.get(i).compareTo(value) == 0) {
                i++;
            }
            // Every none-student ranks below every measured value, so they all
            // count toward `below` here. This is what lifts a single weak
            // certification clear of having none at all.
            int below = noneCount + runStart;
            int equal = i - runStart;

            // 100 * (below + equal/2) / n, kept as (2*below + equal) / (2*n) so
            // the halving is exact rather than a rounded intermediate.
            BigDecimal numerator = BigDecimal.valueOf(2L * below + equal);
            BigDecimal percentile = numerator.multiply(HUNDRED)
                    .divide(TWO.multiply(nBd), SCALE, RoundingMode.HALF_UP);

            byValue.put(value, percentile);
        }
        return new Distribution(Map.copyOf(byValue), n, nonePercentile);
    }

    /**
     * Scale-insensitive key. 7.5 and 7.50 are the same CGPA and must land in the
     * same tie group; BigDecimal's equals() and hashCode() disagree, so every
     * value is stripped of trailing zeros before being used as a map key.
     */
    private static BigDecimal normalise(BigDecimal v) {
        return v.stripTrailingZeros();
    }
}
