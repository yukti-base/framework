package org.yuktisetu.drive.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns a sealed applicant pool into ranked composite scores.
 *
 * A pure function over value objects, like EligibilityEvaluator: no Spring, no
 * JPA, no Logger, no clock. Every database read happens before score() is called,
 * which is also what makes "no DB calls in loops" structurally true rather than a
 * rule somebody has to remember.
 *
 * THE ALGORITHM, and why each step is the way it is.
 *
 * 1. Per factor, build ONE midrank distribution over the pool -- O(n log n) each,
 *    not a count per student, which would be O(n squared) and four million
 *    comparisons per factor at 2000 applicants.
 *
 * 2. Every factor is converted to a percentile before any weight is applied.
 *    This is what makes a weight mean what it says. Percentiles are uniform on
 *    (0,100) with identical variance, so a 30% weight exerts 30% of the influence
 *    whichever factor carries it. Mixing a percentile with, say, a raw 0-100
 *    skill-match ratio would silently give the two different real influence at
 *    equal nominal weight, because their distributions differ.
 *
 * 3. MeasuredNone lands on the distribution's floor percentile, NOT on zero and
 *    NOT excluded. Excluding it would rank a student with no certifications above
 *    a student with one weak certification -- on a 25% weight that is a 12.5-point
 *    inversion, and it rewards not trying. Zero would overstate the gap between
 *    "nothing" and "the least of anything".
 *
 * 4. Unmeasurable factors are dropped for that student and their remaining weights
 *    rescaled to 100%. Available-case analysis, unbiased when the absence is
 *    unrelated to ability -- a platform outage, an unconfigured factor. Scoring
 *    such a student zero would cost them around 16 points on a 30% weight for
 *    something they did not cause.
 *
 * 5. A factor below its floor disqualifies the student outright, independent of
 *    the composite. A floor is a veto, not a penalty, which is why it is not
 *    folded into the weighted sum.
 *
 * 6. Ranking is by composite descending. Ties are NOT broken silently -- students
 *    on an identical composite share a rank and are reported as a tie group, so a
 *    tie straddling the shortlist cut is a decision the TnP makes rather than an
 *    accident of row order.
 *
 * All arithmetic is BigDecimal with explicit scale and HALF_UP. These numbers are
 * shown to students, so a re-run must reproduce them exactly; no double appears
 * anywhere in this class.
 */
public final class CompositeScorer {

    private CompositeScorer() {}

    /** Scale for percentiles and weighted contributions. */
    public static final int SCALE = 4;
    /** Scale for the published composite. Two places is what a student is shown. */
    public static final int COMPOSITE_SCALE = 2;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /**
     * The fewest students a factor must cover before it is used at all.
     *
     * Ten, raised from five on 2026-10-07. The reason is resolution: the finest
     * distinction a group of n can draw is 100/n points, so
     *
     *     group of  5  ->  one place apart = 20.00 points
     *     group of 10  ->  one place apart = 10.00 points
     *     group of 15  ->  one place apart =  6.67 points
     *     group of 20  ->  one place apart =  5.00 points
     *
     * At five, two students whose CGPAs differ by 0.01 are forced 20 points apart,
     * and on a factor weighted 40% that is 8 points of final score invented out of
     * a rounding difference. Ten halves that, and is still reachable by the small
     * drives that actually occur. Fifteen would be better statistically and is a
     * supported setting; it is not the default because it would silently drop
     * factors on a good proportion of real drives.
     *
     * A factor below this becomes unmeasurable for the WHOLE pool, so it rescales
     * out of the drive rather than letting eight students' data decide a ranking.
     */
    public static final int DEFAULT_MIN_SAMPLE = 10;

    /**
     * The fewest applicants a drive must have before composite scoring runs at all.
     *
     * Below this the engine declines and hands the drive back for manual
     * shortlisting, rather than producing numbers that look precise and are not.
     * Two reasons, and the second is the stronger one:
     *
     *  - Arithmetically, nothing can clear the per-factor minimum if the pool
     *    itself does not. A twelve-applicant drive with a minimum of fifteen would
     *    see every factor dropped and every student scored zero, which is a
     *    failure dressed up as a result.
     *
     *  - Practically, composite scoring exists to reduce a mass applicant pool. A
     *    coordinator can read nine profiles. Ranking nine students to two decimal
     *    places adds false confidence, not information, and every one of those
     *    numbers is defensible to a student only if it means something.
     */
    public static final int DEFAULT_MIN_POOL = 10;

    public record Config(
            WeightProfile weights,
            BigDecimal threshold,
            Map<ScoringFactor, BigDecimal> floors,
            int minSample,
            /** Below this many applicants the engine declines to score at all. */
            int minPool,
            /**
             * Whether the TnP has already reviewed and approved scoring students
             * whose data could not be measured.
             *
             * False by default, and when false the scorer REFUSES to score such a
             * student -- it returns them in needsTnpReview instead. This is a
             * structural gate, not a courtesy: a missing factor can swing a
             * ranking hard in the direction of whoever was weak at the missing
             * thing, so the decision to proceed has to be a human's. Relying on
             * the pipeline to remember to ask would mean one forgotten call site
             * silently scoring people who should have been reviewed.
             */
            boolean unmeasurableApproved
    ) {
        public Config {
            if (weights == null) {
                throw new IllegalArgumentException("weights are required");
            }
            threshold = threshold == null ? BigDecimal.ZERO : threshold;
            // Must check isEmpty as well as null: EnumMap's copy constructor
            // cannot infer the enum class from an empty source map and throws
            // "Specified map is empty".
            floors = (floors == null || floors.isEmpty())
                    ? Map.of() : Map.copyOf(new EnumMap<>(floors));
            if (minSample < 1) {
                throw new IllegalArgumentException("minSample must be at least 1");
            }
            if (minPool < 1) {
                throw new IllegalArgumentException("minPool must be at least 1");
            }
        }

        public static Config of(WeightProfile weights, BigDecimal threshold) {
            return new Config(weights, threshold, Map.of(),
                    DEFAULT_MIN_SAMPLE, DEFAULT_MIN_POOL, false);
        }

        /** After the TnP has reviewed the affected students and said to proceed. */
        public Config approved() {
            return new Config(weights, threshold, floors, minSample, minPool, true);
        }
    }

    /** A scored pool: traces in rank order, plus the shared ranks. */
    public record Result(
            List<ScoringTrace> ranked,
            /** studentUserId -> rank. Tied students share a rank. */
            Map<Long, Integer> ranks,
            /** Factors excluded for the WHOLE pool, and why. Logged at INFO. */
            Map<ScoringFactor, String> poolWideExclusions,
            /**
             * Students who could NOT be scored because something was unmeasurable
             * and the TnP has not approved proceeding. Non-empty means the whole
             * run is on hold: nobody is ranked until the TnP decides, because
             * ranking a pool while some of it is unresolved produces an order that
             * changes once they are.
             */
            Map<Long, Map<ScoringFactor, String>> needsTnpReview,
            /**
             * Set when the engine declined entirely -- the pool was too small for
             * ranking to mean anything. The drive goes to manual shortlisting, and
             * this is the sentence the TnP is shown.
             */
            String declined
    ) {
        public boolean heldForReview() {
            return !needsTnpReview.isEmpty();
        }

        public boolean wasDeclined() {
            return declined != null;
        }
    }

    public static Result score(Collection<ScoringFacts> pool, Config config) {
        if (pool == null || pool.isEmpty()) {
            return new Result(List.of(), Map.of(), Map.of(), Map.of(),
                    "no applicants to score");
        }
        List<ScoringFacts> candidates = List.copyOf(pool);

        // Decline rather than produce precise-looking nonsense. See DEFAULT_MIN_POOL.
        if (candidates.size() < config.minPool()) {
            return new Result(List.of(), Map.of(), Map.of(), Map.of(),
                    "only " + candidates.size() + " applicants, which is below the minimum of "
                            + config.minPool() + " for composite scoring. Ranking this few students "
                            + "by percentile would invent differences the data cannot support -- "
                            + "one place apart would be worth "
                            + HUNDRED.divide(BigDecimal.valueOf(candidates.size()), 2,
                                    RoundingMode.HALF_UP).toPlainString()
                            + " points. Shortlist this drive manually.");
        }

        // --- Step 1: one distribution per factor, over measured values only. ---
        //
        // Unmeasurable students are excluded from the sample on purpose. Including
        // a placeholder for them would shift every other student's percentile,
        // which is how an outage on a platform ends up moving the rank of students
        // who do not even use it.
        Map<ScoringFactor, Percentiles.Distribution> distributions = new EnumMap<>(ScoringFactor.class);
        Map<ScoringFactor, String> poolWideExclusions = new EnumMap<>(ScoringFactor.class);

        for (ScoringFactor factor : config.weights().factors()) {
            if (config.weights().rawWeightOf(factor).signum() == 0) {
                // Entirely normal: a service company sets coding to zero. The
                // factor is listed, takes no part, and is not rescaled over.
                poolWideExclusions.put(factor, "weighted zero for this drive");
                continue;
            }
            List<BigDecimal> measured = new ArrayList<>(candidates.size());
            int noneCount = 0;
            for (ScoringFacts facts : candidates) {
                if (facts.valueOf(factor) instanceof FactorValue.Measured m) {
                    measured.add(m.raw());
                } else if (facts.valueOf(factor) instanceof FactorValue.MeasuredNone) {
                    noneCount++;
                }
            }
            // MeasuredNone students are part of the sample -- they WERE measured,
            // and the answer was none -- so they count toward coverage and occupy
            // the bottom places of the distribution as their own tie group.
            int coverage = measured.size() + noneCount;
            if (coverage < config.minSample() || measured.isEmpty()) {
                poolWideExclusions.put(factor,
                        "only " + coverage + " of " + candidates.size()
                                + " applicants have data; minimum sample is " + config.minSample());
                continue;
            }
            distributions.put(factor, Percentiles.distribution(measured, noneCount));
        }

        // --- The TnP gate. Checked BEFORE anything is scored or ranked. ---
        //
        // A factor that is unmeasurable for the WHOLE pool is not a review case --
        // it affects everyone identically and simply rescales out. What needs a
        // human is a factor missing for SOME students, because that is what can
        // move one student's rank relative to the others.
        if (!config.unmeasurableApproved()) {
            Map<Long, Map<ScoringFactor, String>> needsReview = new LinkedHashMap<>();
            for (ScoringFacts facts : candidates) {
                Map<ScoringFactor, String> gaps = new EnumMap<>(ScoringFactor.class);
                for (ScoringFactor factor : config.weights().weightedFactors()) {
                    if (poolWideExclusions.containsKey(factor)) {
                        continue;
                    }
                    if (facts.valueOf(factor) instanceof FactorValue.Unmeasurable u) {
                        gaps.put(factor, u.reason());
                    }
                }
                if (!gaps.isEmpty()) {
                    needsReview.put(facts.studentUserId(), Map.copyOf(gaps));
                }
            }
            if (!needsReview.isEmpty()) {
                return new Result(List.of(), Map.of(),
                        Map.copyOf(poolWideExclusions), Map.copyOf(needsReview), null);
            }
        }

        // --- Steps 2-5: score each candidate. Pure in-memory work from here. ---
        List<ScoringTrace> traces = new ArrayList<>(candidates.size());
        for (ScoringFacts facts : candidates) {
            traces.add(scoreOne(facts, config, distributions, poolWideExclusions));
        }

        // --- Step 6: rank, sharing ranks across ties. ---
        traces.sort((a, b) -> {
            int byScore = b.composite().compareTo(a.composite());
            // Student id only orders the DISPLAY of a tie group; it never breaks
            // the tie for ranking purposes, which is what the shared rank below
            // guarantees.
            return byScore != 0 ? byScore : Long.compare(a.studentUserId(), b.studentUserId());
        });

        Map<Long, Integer> ranks = new LinkedHashMap<>();
        int rank = 0;
        BigDecimal previous = null;
        for (int i = 0; i < traces.size(); i++) {
            ScoringTrace t = traces.get(i);
            if (previous == null || t.composite().compareTo(previous) != 0) {
                rank = i + 1;
                previous = t.composite();
            }
            ranks.put(t.studentUserId(), rank);
        }

        return new Result(List.copyOf(traces), Map.copyOf(ranks),
                Map.copyOf(poolWideExclusions), Map.of(), null);
    }

    private static ScoringTrace scoreOne(
            ScoringFacts facts,
            Config config,
            Map<ScoringFactor, Percentiles.Distribution> distributions,
            Map<ScoringFactor, String> poolWideExclusions) {

        // Pass one: resolve each factor to a percentile, or decide it cannot
        // contribute for this student. Weights are NOT applied yet, because the
        // rescale in pass two depends on which factors survived here.
        Map<ScoringFactor, BigDecimal> percentiles = new EnumMap<>(ScoringFactor.class);
        Map<ScoringFactor, Integer> samples = new EnumMap<>(ScoringFactor.class);
        Map<ScoringFactor, String> excluded = new EnumMap<>(ScoringFactor.class);
        Map<ScoringFactor, BigDecimal> raws = new EnumMap<>(ScoringFactor.class);

        for (ScoringFactor factor : config.weights().factors()) {
            String poolWide = poolWideExclusions.get(factor);
            if (poolWide != null) {
                excluded.put(factor, poolWide);
                continue;
            }
            Percentiles.Distribution dist = distributions.get(factor);
            FactorValue value = facts.valueOf(factor);

            switch (value) {
                case FactorValue.Measured m -> {
                    raws.put(factor, m.raw());
                    percentiles.put(factor, dist.percentileOf(m.raw()));
                    samples.put(factor, dist.sampleSize());
                }
                case FactorValue.MeasuredNone ignored -> {
                    // The honest answer is "none", which is comparable data and
                    // ranks strictly below every student who has any amount.
                    percentiles.put(factor, dist.nonePercentile());
                    samples.put(factor, dist.sampleSize());
                }
                case FactorValue.Unmeasurable u -> excluded.put(factor, u.reason());
            }
        }

        Set<ScoringFactor> available = percentiles.isEmpty()
                ? EnumSet.noneOf(ScoringFactor.class)
                : EnumSet.copyOf(percentiles.keySet());

        // Nothing could be measured at all. Scored zero rather than crashing the
        // whole run for one student, and the trace says why -- a pool where this
        // happens often is a data problem the TnP needs to see, not a ranking.
        if (available.isEmpty()) {
            List<ScoringTrace.FactorTrace> factorTraces = new ArrayList<>();
            for (ScoringFactor f : config.weights().factors()) {
                factorTraces.add(new ScoringTrace.FactorTrace(
                        f, facts.valueOf(f), null, null, 0, BigDecimal.ZERO,
                        BigDecimal.ZERO, config.floors().get(f), false,
                        excluded.getOrDefault(f, "not measurable")));
            }
            return new ScoringTrace(facts.studentUserId(), List.copyOf(factorTraces), true,
                    BigDecimal.ZERO.setScale(COMPOSITE_SCALE), config.threshold(), false, List.of());
        }

        boolean renormalised = available.size() < config.weights().weightedFactors().size();
        Map<ScoringFactor, BigDecimal> shares = config.weights().sharesOver(available);

        // Pass two: weight, sum, and check floors.
        List<ScoringTrace.FactorTrace> factorTraces = new ArrayList<>();
        List<ScoringFactor> floorsFailed = new ArrayList<>();
        BigDecimal composite = BigDecimal.ZERO;

        for (ScoringFactor factor : config.weights().factors()) {
            BigDecimal floor = config.floors().get(factor);
            String reason = excluded.get(factor);

            if (reason != null) {
                // A floor cannot be judged on a factor that was not measured, so
                // it is not applied. Vetoing a student over an outage would be the
                // same unfairness in a harsher form.
                factorTraces.add(new ScoringTrace.FactorTrace(
                        factor, facts.valueOf(factor), null, null, 0,
                        BigDecimal.ZERO, BigDecimal.ZERO, floor, false, reason));
                continue;
            }

            BigDecimal percentile = percentiles.get(factor);
            BigDecimal weight = shares.getOrDefault(factor, BigDecimal.ZERO);
            // contribution = percentile * weight / 100
            BigDecimal contribution = percentile.multiply(weight)
                    .divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
            composite = composite.add(contribution);

            boolean floorFailed = floor != null && percentile.compareTo(floor) < 0;
            if (floorFailed) {
                floorsFailed.add(factor);
            }

            factorTraces.add(new ScoringTrace.FactorTrace(
                    factor, facts.valueOf(factor), raws.get(factor), percentile,
                    samples.getOrDefault(factor, 0), weight, contribution,
                    floor, floorFailed, null));
        }

        composite = composite.setScale(COMPOSITE_SCALE, RoundingMode.HALF_UP);
        boolean meets = composite.compareTo(config.threshold()) >= 0;

        return new ScoringTrace(facts.studentUserId(), List.copyOf(factorTraces),
                renormalised, composite, config.threshold(), meets, List.copyOf(floorsFailed));
    }
}
