package org.yuktisetu.drive.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.yuktisetu.drive.model.ScoringFactor.*;

/**
 * The fairness properties of the composite score.
 *
 * Every test here is about an OUTCOME a student could dispute, not about code
 * coverage. A score is the most contestable thing this platform produces and the
 * breakdown is shown to the student, so each of these asserts a specific way the
 * scoring could be unfair and is not.
 */
class CompositeScorerTest {

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    private static WeightProfile weights(Object... pairs) {
        Map<ScoringFactor, BigDecimal> m = new EnumMap<>(ScoringFactor.class);
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((ScoringFactor) pairs[i], bd((String) pairs[i + 1]));
        }
        return WeightProfile.of(m);
    }

    private static ScoringFacts facts(long id, Object... pairs) {
        Map<ScoringFactor, FactorValue> m = new EnumMap<>(ScoringFactor.class);
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((ScoringFactor) pairs[i], (FactorValue) pairs[i + 1]);
        }
        return new ScoringFacts(id, m);
    }

    private static FactorValue measured(String raw) {
        return new FactorValue.Measured(bd(raw));
    }

    /**
     * A config for the tests that are about scoring MECHANICS rather than about
     * the group-size guards: minimums relaxed to 1, and unmeasurable data
     * pre-approved. The guards have their own tests below with realistic sizes,
     * and keeping them out of the way here means a mechanics test fails for the
     * reason it is named after.
     */
    private static CompositeScorer.Config mechanics(WeightProfile w, BigDecimal threshold) {
        return new CompositeScorer.Config(w, threshold, Map.of(), 1, 1, true);
    }

    private static CompositeScorer.Config mechanics(WeightProfile w, BigDecimal threshold,
                                                    Map<ScoringFactor, BigDecimal> floors) {
        return new CompositeScorer.Config(w, threshold, floors, 1, 1, true);
    }

    private static ScoringTrace traceFor(CompositeScorer.Result r, long id) {
        return r.ranked().stream().filter(t -> t.studentUserId() == id).findFirst().orElseThrow();
    }

    // ----------------------------------------------------------- the core bias

    @Test
    @DisplayName("having nothing must NOT outrank having something weak")
    void measuredNoneDoesNotBeatAWeakMeasurement() {
        // The inversion a nullable column produces by default. Certifications at
        // 25%; A has none, B has one weak one, everything else identical.
        // Renormalising A's absence away would score A 60.00 and B 47.50 --
        // rewarding the student who did nothing by 12.5 points.
        var cfg = mechanics(weights(ACADEMIC_PERFORMANCE, "75", CERTIFICATIONS, "25"),
                BigDecimal.ZERO);

        List<ScoringFacts> pool = new ArrayList<>();
        pool.add(facts(1L, ACADEMIC_PERFORMANCE, measured("8.0"),
                CERTIFICATIONS, new FactorValue.MeasuredNone()));   // A: none
        pool.add(facts(2L, ACADEMIC_PERFORMANCE, measured("8.0"),
                CERTIFICATIONS, measured("1")));                     // B: one weak
        pool.add(facts(3L, ACADEMIC_PERFORMANCE, measured("8.0"),
                CERTIFICATIONS, measured("9")));                     // C: strong
        for (long i = 4; i <= 8; i++) {
            pool.add(facts(i, ACADEMIC_PERFORMANCE, measured("8.0"), CERTIFICATIONS, measured("5")));
        }

        var r = CompositeScorer.score(pool, cfg);
        BigDecimal a = traceFor(r, 1L).composite();
        BigDecimal b = traceFor(r, 2L).composite();
        BigDecimal c = traceFor(r, 3L).composite();

        assertTrue(a.compareTo(b) < 0,
                "a student with NO certifications (" + a + ") must rank below one with a weak "
                        + "certification (" + b + ") -- otherwise the score rewards not trying");
        assertTrue(b.compareTo(c) < 0, "weak must rank below strong");
        // And A's weights were NOT rescaled: the factor was measured, as "none".
        assertFalse(traceFor(r, 1L).weightsRenormalised());
    }

    @Test
    @DisplayName("an unmeasurable factor rescales the weights instead of scoring zero")
    void unmeasurableRenormalises() {
        // A platform outage. The student did nothing wrong and nothing was
        // measured, so scoring them zero would cost ~16 points on a 30% weight
        // for an event outside their control.
        // .approved() because the TnP has already reviewed this student -- without
        // it the scorer refuses to score them at all, which is the subject of its
        // own test below.
        var cfg = mechanics(weights(ACADEMIC_PERFORMANCE, "70", CODING_PROFICIENCY, "30"), BigDecimal.ZERO);

        List<ScoringFacts> pool = new ArrayList<>();
        pool.add(facts(1L, ACADEMIC_PERFORMANCE, measured("8.0"),
                CODING_PROFICIENCY, new FactorValue.Unmeasurable("CodeChef unreachable")));
        for (long i = 2; i <= 8; i++) {
            pool.add(facts(i, ACADEMIC_PERFORMANCE, measured(i + ".0"),
                    CODING_PROFICIENCY, measured(String.valueOf(i * 10))));
        }

        var t = traceFor(CompositeScorer.score(pool, cfg), 1L);
        assertTrue(t.weightsRenormalised());

        var academic = t.factors().stream()
                .filter(f -> f.factor() == ACADEMIC_PERFORMANCE).findFirst().orElseThrow();
        // 70 rescaled over a surviving 70 becomes the whole 100.
        assertEquals(0, academic.weightApplied().compareTo(bd("100")),
                "the surviving factor must carry the full weight, got " + academic.weightApplied());
        // The composite is the academic percentile itself, unchanged in scale.
        assertEquals(0, t.composite().compareTo(academic.percentile().setScale(2, RoundingMode.HALF_UP)));

        var coding = t.factors().stream()
                .filter(f -> f.factor() == CODING_PROFICIENCY).findFirst().orElseThrow();
        assertFalse(coding.contributed());
        assertEquals("CodeChef unreachable", coding.excludedReason());
    }

    @Test
    @DisplayName("rescaled weights still total 100, so composites stay comparable")
    void renormalisedWeightsTotalOneHundred() {
        // Without this, a student missing a factor would be scored on a different
        // scale from their peers and the ranking between them would be meaningless.
        var profile = weights(ACADEMIC_PERFORMANCE, "40", CODING_PROFICIENCY, "30",
                CERTIFICATIONS, "30");
        var shares = profile.sharesOver(java.util.EnumSet.of(ACADEMIC_PERFORMANCE, CERTIFICATIONS));
        BigDecimal total = shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, total.compareTo(bd("100")), "rescaled shares totalled " + total);
        // 40/70 and 30/70 of 100, with the rounding residue absorbed.
        assertEquals(0, shares.get(ACADEMIC_PERFORMANCE).setScale(2, RoundingMode.HALF_UP)
                .compareTo(bd("57.14")));
    }

    @Test
    @DisplayName("a weight means what it says: equal weights give equal influence")
    void weightsAreHonest() {
        // Only true because every factor is converted to a percentile before any
        // weight is applied. Percentiles are uniform on (0,100) with identical
        // variance, so swapping which factor a student is strong in must not
        // change their composite when the weights are equal.
        var cfg = mechanics(weights(ACADEMIC_PERFORMANCE, "50", SKILL_MATCH, "50"),
                BigDecimal.ZERO);

        List<ScoringFacts> pool = new ArrayList<>();
        // Student 1 strong academically, weak on skills; student 2 the mirror.
        pool.add(facts(1L, ACADEMIC_PERFORMANCE, measured("10"), SKILL_MATCH, measured("1")));
        pool.add(facts(2L, ACADEMIC_PERFORMANCE, measured("1"), SKILL_MATCH, measured("10")));
        for (long i = 3; i <= 8; i++) {
            pool.add(facts(i, ACADEMIC_PERFORMANCE, measured(i + ""), SKILL_MATCH, measured(i + "")));
        }

        var r = CompositeScorer.score(pool, cfg);
        assertEquals(0, traceFor(r, 1L).composite().compareTo(traceFor(r, 2L).composite()),
                "mirror-image students must score identically under equal weights");
    }

    // ------------------------------------------------------- sample size guards

    @Test
    @DisplayName("a factor with too little coverage is dropped for the whole pool")
    void thinCoverageIsExcludedPoolWide() {
        // With n=2 the percentile forces the two students 50 points apart however
        // close their real values are. That is noise amplification, not
        // measurement, so the factor must not decide a ranking.
        var cfg = new CompositeScorer.Config(
                weights(ACADEMIC_PERFORMANCE, "50", CERTIFICATIONS, "50"),
                BigDecimal.ZERO, Map.of(), 5, 1, true);   // minSample 5, pool of 8

        List<ScoringFacts> pool = new ArrayList<>();
        pool.add(facts(1L, ACADEMIC_PERFORMANCE, measured("9"), CERTIFICATIONS, measured("5")));
        pool.add(facts(2L, ACADEMIC_PERFORMANCE, measured("8"), CERTIFICATIONS, measured("1")));
        for (long i = 3; i <= 8; i++) {
            pool.add(facts(i, ACADEMIC_PERFORMANCE, measured(i + ""),
                    CERTIFICATIONS, new FactorValue.Unmeasurable("not resolved")));
        }

        var r = CompositeScorer.score(pool, cfg);
        assertTrue(r.poolWideExclusions().containsKey(CERTIFICATIONS),
                "certifications had 2 of 8 applicants covered and should be excluded");
        assertTrue(r.poolWideExclusions().get(CERTIFICATIONS).contains("minimum sample is 5"));
        // Everyone is then scored on academics alone, at full weight.
        assertEquals(0, traceFor(r, 1L).composite()
                .compareTo(traceFor(r, 1L).factors().stream()
                        .filter(f -> f.factor() == ACADEMIC_PERFORMANCE).findFirst().orElseThrow()
                        .percentile().setScale(2, RoundingMode.HALF_UP)));
    }

    @Test
    @DisplayName("a zero-weighted factor never influences anything")
    void zeroWeightIsInert() {
        var cfg = mechanics(weights(ACADEMIC_PERFORMANCE, "100", CODING_PROFICIENCY, "0"),
                BigDecimal.ZERO);
        List<ScoringFacts> pool = new ArrayList<>();
        for (long i = 1; i <= 6; i++) {
            pool.add(facts(i, ACADEMIC_PERFORMANCE, measured("8"),
                    CODING_PROFICIENCY, measured(String.valueOf(i * 100))));
        }
        var r = CompositeScorer.score(pool, cfg);
        // Identical academics, wildly different coding, zero weight -> all tie.
        assertEquals(1, r.ranks().values().stream().distinct().count(),
                "a zero-weighted factor must not separate anyone");
    }

    // ------------------------------------------------------- ranking and floors

    @Test
    @DisplayName("tied students share a rank rather than being split by row order")
    void tiesShareARank() {
        // A tie straddling the shortlist cut has to be the TnP's decision. Breaking
        // it on student id would hand a place to whoever registered first.
        var cfg = mechanics(weights(ACADEMIC_PERFORMANCE, "100"), BigDecimal.ZERO);
        List<ScoringFacts> pool = new ArrayList<>();
        pool.add(facts(10L, ACADEMIC_PERFORMANCE, measured("9")));
        pool.add(facts(20L, ACADEMIC_PERFORMANCE, measured("8")));
        pool.add(facts(30L, ACADEMIC_PERFORMANCE, measured("8")));
        pool.add(facts(40L, ACADEMIC_PERFORMANCE, measured("8")));
        pool.add(facts(50L, ACADEMIC_PERFORMANCE, measured("7")));

        var r = CompositeScorer.score(pool, cfg);
        assertEquals(1, r.ranks().get(10L));
        assertEquals(2, r.ranks().get(20L));
        assertEquals(2, r.ranks().get(30L), "tied students must share rank 2");
        assertEquals(2, r.ranks().get(40L));
        // Competition ranking: the next distinct score takes the rank its position
        // implies, so the gap records that three people were above it.
        assertEquals(5, r.ranks().get(50L));
    }

    @Test
    @DisplayName("a floor is a veto, independent of the composite")
    void floorExcludesRegardlessOfTotal() {
        // A student can be top of the pool overall and still fail a mandatory
        // minimum. Folding the floor into the weighted sum would let a high
        // composite buy its way past a hard requirement.
        Map<ScoringFactor, BigDecimal> floors = new EnumMap<>(ScoringFactor.class);
        floors.put(SKILL_MATCH, bd("30"));
        var cfg = new CompositeScorer.Config(
                weights(ACADEMIC_PERFORMANCE, "80", SKILL_MATCH, "20"),
                BigDecimal.ZERO, floors, 5, 1, true);

        List<ScoringFacts> pool = new ArrayList<>();
        pool.add(facts(1L, ACADEMIC_PERFORMANCE, measured("10"), SKILL_MATCH, measured("0")));
        for (long i = 2; i <= 8; i++) {
            pool.add(facts(i, ACADEMIC_PERFORMANCE, measured("5"), SKILL_MATCH, measured(i + "")));
        }

        var t = traceFor(CompositeScorer.score(pool, cfg), 1L);
        assertEquals(List.of(SKILL_MATCH), t.floorsFailed());
        assertTrue(t.meetsThreshold(), "the composite itself still clears the threshold");
        assertFalse(t.shortlistEligible(), "but the floor vetoes the shortlist");
    }

    @Test
    @DisplayName("a floor is not applied to a factor that could not be measured")
    void floorNotAppliedToUnmeasurableFactor() {
        // Vetoing a student over a platform outage is the same unfairness as
        // scoring them zero for it, in a harsher form.
        Map<ScoringFactor, BigDecimal> floors = new EnumMap<>(ScoringFactor.class);
        floors.put(CODING_PROFICIENCY, bd("40"));
        var cfg = new CompositeScorer.Config(
                weights(ACADEMIC_PERFORMANCE, "60", CODING_PROFICIENCY, "40"),
                BigDecimal.ZERO, floors, 5, 1, true);

        List<ScoringFacts> pool = new ArrayList<>();
        pool.add(facts(1L, ACADEMIC_PERFORMANCE, measured("8"),
                CODING_PROFICIENCY, new FactorValue.Unmeasurable("LeetCode unreachable")));
        for (long i = 2; i <= 8; i++) {
            pool.add(facts(i, ACADEMIC_PERFORMANCE, measured(i + ""),
                    CODING_PROFICIENCY, measured(i + "")));
        }

        var t = traceFor(CompositeScorer.score(pool, cfg), 1L);
        assertTrue(t.floorsFailed().isEmpty(), "an unmeasured factor cannot fail its floor");
        assertTrue(t.shortlistEligible());
    }

    // ------------------------------------------------- determinism and the trace

    @Test
    @DisplayName("the same pool scored twice gives byte-identical results")
    void runsAreReproducible() {
        // Students are shown these numbers. A re-run that produced 72.34 where it
        // once produced 72.33 is a dispute nobody can settle.
        var cfg = mechanics(weights(ACADEMIC_PERFORMANCE, "33.33", CODING_PROFICIENCY, "33.33",
                SKILL_MATCH, "33.34"), bd("50"));

        List<ScoringFacts> poolA = new ArrayList<>();
        List<ScoringFacts> poolB = new ArrayList<>();
        for (long i = 1; i <= 17; i++) {
            poolA.add(facts(i, ACADEMIC_PERFORMANCE, measured(i + ".5"),
                    CODING_PROFICIENCY, measured(String.valueOf(100 - i)),
                    SKILL_MATCH, measured(String.valueOf(i % 4))));
        }
        for (int i = poolA.size() - 1; i >= 0; i--) {
            poolB.add(poolA.get(i));   // same students, reversed input order
        }

        var a = CompositeScorer.score(poolA, cfg);
        var b = CompositeScorer.score(poolB, cfg);
        for (long i = 1; i <= 17; i++) {
            assertEquals(traceFor(a, i).composite().toPlainString(),
                    traceFor(b, i).composite().toPlainString(),
                    "student " + i + " scored differently when the input order changed");
            assertEquals(a.ranks().get(i), b.ranks().get(i));
        }
    }

    @Test
    @DisplayName("the trace explains every factor, including the ones that did not count")
    void traceIsComplete() {
        var cfg = mechanics(weights(ACADEMIC_PERFORMANCE, "50", CODING_PROFICIENCY, "30",
                        CERTIFICATIONS, "20"), bd("40"));

        List<ScoringFacts> pool = new ArrayList<>();
        pool.add(facts(1L, ACADEMIC_PERFORMANCE, measured("9"),
                CODING_PROFICIENCY, new FactorValue.Unmeasurable("CodeChef down"),
                CERTIFICATIONS, new FactorValue.MeasuredNone()));
        for (long i = 2; i <= 8; i++) {
            pool.add(facts(i, ACADEMIC_PERFORMANCE, measured(i + ""),
                    CODING_PROFICIENCY, measured(i + ""), CERTIFICATIONS, measured(i + "")));
        }

        var t = traceFor(CompositeScorer.score(pool, cfg), 1L);
        assertEquals(3, t.factors().size(), "every weighted factor must appear in the trace");

        var coding = t.factors().stream().filter(f -> f.factor() == CODING_PROFICIENCY)
                .findFirst().orElseThrow();
        assertEquals("CodeChef down", coding.excludedReason(),
                "a student asking why their score moved is owed the reason");

        var certs = t.factors().stream().filter(f -> f.factor() == CERTIFICATIONS)
                .findFirst().orElseThrow();
        assertTrue(certs.contributed(), "MeasuredNone still contributes -- at the floor");
        assertNull(certs.raw(), "there is no raw value for 'none'");
        assertTrue(certs.sampleSize() > 0, "the sample it was placed against must be recorded");

        // The contributions must add up to the composite, or the breakdown shown
        // to the student contradicts the number shown to the student.
        BigDecimal sum = t.factors().stream().map(ScoringTrace.FactorTrace::contribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, sum.compareTo(t.composite()),
                "factor contributions " + sum + " must equal the composite " + t.composite());
    }

    @Test
    @DisplayName("a student with nothing measurable scores zero rather than killing the run")
    void totallyUnmeasurableStudentIsSurvivable() {
        var cfg = mechanics(weights(ACADEMIC_PERFORMANCE, "100"), BigDecimal.ZERO);
        List<ScoringFacts> pool = new ArrayList<>();
        pool.add(facts(1L, ACADEMIC_PERFORMANCE, new FactorValue.Unmeasurable("no profile row")));
        for (long i = 2; i <= 8; i++) {
            pool.add(facts(i, ACADEMIC_PERFORMANCE, measured(i + "")));
        }
        var t = traceFor(CompositeScorer.score(pool, cfg), 1L);
        assertEquals(0, t.composite().compareTo(BigDecimal.ZERO));
        assertFalse(t.shortlistEligible());
        assertEquals("no profile row",
                t.factors().get(0).excludedReason(), "the cause must be recorded, not swallowed");
    }

    @Test
    @DisplayName("weights are unconstrained: any factor may be 0 or the maximum")
    void weightsHaveNoLimits() {
        // A service company uninterested in competitive coding sets it to zero.
        assertDoesNotThrow(() -> weights(ACADEMIC_PERFORMANCE, "100", CODING_PROFICIENCY, "0"));
        // A product company that cares only about certifications.
        assertDoesNotThrow(() -> weights(ACADEMIC_PERFORMANCE, "0", CERTIFICATIONS, "100"));
        // They need not total 100 at all -- these are normalised internally.
        assertDoesNotThrow(() -> weights(ACADEMIC_PERFORMANCE, "10", CODING_PROFICIENCY, "5"));
        assertDoesNotThrow(() -> weights(ACADEMIC_PERFORMANCE, "3"));

        // Only two things are refused, and neither is a "limit":
        assertThrows(IllegalArgumentException.class,
                () -> weights(ACADEMIC_PERFORMANCE, "-10"),
                "a negative weight would mean being better makes you worse");
        assertThrows(IllegalArgumentException.class,
                () -> weights(ACADEMIC_PERFORMANCE, "0", CODING_PROFICIENCY, "0"),
                "everything at zero leaves nothing to rank by");
    }

    @Test
    @DisplayName("2/2/1 and 40/40/20 describe the same drive")
    void weightsAreNormalisedNotRequiredToTotalOneHundred() {
        List<ScoringFacts> pool = new ArrayList<>();
        for (long i = 1; i <= 8; i++) {
            pool.add(facts(i, ACADEMIC_PERFORMANCE, measured(i + ""),
                    CODING_PROFICIENCY, measured(String.valueOf(9 - i)),
                    CERTIFICATIONS, measured(String.valueOf(i % 3))));
        }
        var a = CompositeScorer.score(pool, mechanics(weights(ACADEMIC_PERFORMANCE, "40", CODING_PROFICIENCY, "40",
                        CERTIFICATIONS, "20"), BigDecimal.ZERO));
        var b = CompositeScorer.score(pool, mechanics(weights(ACADEMIC_PERFORMANCE, "2", CODING_PROFICIENCY, "2",
                        CERTIFICATIONS, "1"), BigDecimal.ZERO));
        for (long i = 1; i <= 8; i++) {
            assertEquals(traceFor(a, i).composite().toPlainString(),
                    traceFor(b, i).composite().toPlainString(),
                    "student " + i + " scored differently for proportionally identical weights");
        }
    }

    @Test
    @DisplayName("a drive weighting ONE factor at 100 and the rest at 0 works")
    void singleFactorDrive() {
        var cfg = mechanics(weights(CERTIFICATIONS, "100", CODING_PROFICIENCY, "0",
                        ACADEMIC_PERFORMANCE, "0"), BigDecimal.ZERO);
        List<ScoringFacts> pool = new ArrayList<>();
        for (long i = 1; i <= 8; i++) {
            pool.add(facts(i, CERTIFICATIONS, measured(i + ""),
                    // deliberately unmeasurable: a zero-weighted factor must not
                    // trigger a review hold either
                    CODING_PROFICIENCY, new FactorValue.Unmeasurable("platform down"),
                    ACADEMIC_PERFORMANCE, measured("5")));
        }
        var r = CompositeScorer.score(pool, cfg);
        assertFalse(r.heldForReview(), "a zero-weighted factor cannot hold a run");
        // Ranked purely by certifications.
        assertEquals(1, r.ranks().get(8L));
        assertEquals(8, r.ranks().get(1L));
    }

    @Test
    @DisplayName("an unreviewed unmeasurable factor HOLDS the run instead of scoring it")
    void unmeasurableHoldsTheRunUntilTnpApproves() {
        // The Riya case. A missing factor can swing a rank hard towards whoever was
        // weak at the missing thing, so nobody is scored until a human has looked.
        // Approval deliberately NOT given -- that is what is under test. Minimums
        // relaxed so the hold is the only thing that can stop the run.
        var cfg = new CompositeScorer.Config(
                weights(ACADEMIC_PERFORMANCE, "60", CODING_PROFICIENCY, "40"),
                BigDecimal.ZERO, Map.of(), 1, 1, false);

        List<ScoringFacts> pool = new ArrayList<>();
        pool.add(facts(7L, ACADEMIC_PERFORMANCE, measured("8.5"),
                CODING_PROFICIENCY, new FactorValue.Unmeasurable("CodeChef unreachable")));
        for (long i = 1; i <= 7; i++) {
            pool.add(facts(i, ACADEMIC_PERFORMANCE, measured(i + ""), CODING_PROFICIENCY, measured(i + "")));
        }

        var held = CompositeScorer.score(pool, cfg);
        assertTrue(held.heldForReview());
        assertTrue(held.ranked().isEmpty(), "nobody is ranked while part of the pool is unresolved");
        assertEquals(java.util.Set.of(7L), held.needsTnpReview().keySet());
        assertEquals("CodeChef unreachable", held.needsTnpReview().get(7L).get(CODING_PROFICIENCY));

        // Once the TnP has decided, the same pool scores normally.
        var approved = CompositeScorer.score(pool, cfg.approved());
        assertFalse(approved.heldForReview());
        assertEquals(8, approved.ranked().size());
    }

    @Test
    @DisplayName("an empty pool is not an error")
    void emptyPool() {
        var r = CompositeScorer.score(List.of(),
                CompositeScorer.Config.of(weights(ACADEMIC_PERFORMANCE, "100"), BigDecimal.ZERO));
        assertTrue(r.ranked().isEmpty());
        assertTrue(r.wasDeclined());
    }

    // ------------------------------------------------- the group-size guards

    @Test
    @DisplayName("a pool below the minimum is DECLINED, not scored badly")
    void tinyPoolIsDeclinedForManualShortlisting() {
        // Nine applicants. Ranking them to two decimal places would invent
        // differences the data cannot carry -- one place apart is worth 11.11
        // points -- and a coordinator can simply read nine profiles.
        var cfg = CompositeScorer.Config.of(weights(ACADEMIC_PERFORMANCE, "100"), BigDecimal.ZERO);
        List<ScoringFacts> pool = new ArrayList<>();
        for (long i = 1; i <= 9; i++) {
            pool.add(facts(i, ACADEMIC_PERFORMANCE, measured(i + "")));
        }

        var r = CompositeScorer.score(pool, cfg);
        assertTrue(r.wasDeclined(), "a nine-applicant drive must not be composite-scored");
        assertTrue(r.ranked().isEmpty(), "and nobody may be ranked");
        assertTrue(r.declined().contains("Shortlist this drive manually"));
        assertTrue(r.declined().contains("11.11"),
                "the TnP is told what one place would have been worth: " + r.declined());
    }

    @Test
    @DisplayName("exactly at the minimum it runs")
    void atTheMinimumItRuns() {
        var cfg = CompositeScorer.Config.of(weights(ACADEMIC_PERFORMANCE, "100"), BigDecimal.ZERO);
        List<ScoringFacts> pool = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            pool.add(facts(i, ACADEMIC_PERFORMANCE, measured(i + "")));
        }
        var r = CompositeScorer.score(pool, cfg);
        assertFalse(r.wasDeclined());
        assertEquals(10, r.ranked().size());
        // One place apart is 10.00 points at this size -- the coarsest the engine
        // will ever produce, which is why the minimum sits here.
        assertEquals(0, traceFor(r, 10L).composite().compareTo(bd("95.00")));
        assertEquals(0, traceFor(r, 1L).composite().compareTo(bd("5.00")));
    }

    @Test
    @DisplayName("a thinly covered factor drops for everyone; the others expand to fill it")
    void thinFactorDropsAndWeightsExpand() {
        // 12 applicants, all with academics, only 4 with certificates. Certificates
        // cannot be ranked, so the drive is decided on academics alone -- and
        // academics carries the whole 100, not its original 60.
        var cfg = CompositeScorer.Config.of(
                weights(ACADEMIC_PERFORMANCE, "60", CERTIFICATIONS, "40"), BigDecimal.ZERO);
        List<ScoringFacts> pool = new ArrayList<>();
        for (long i = 1; i <= 12; i++) {
            if (i <= 4) {
                pool.add(facts(i, ACADEMIC_PERFORMANCE, measured(i + ""),
                        CERTIFICATIONS, measured(i + "")));
            } else {
                pool.add(facts(i, ACADEMIC_PERFORMANCE, measured(i + ""),
                        CERTIFICATIONS, new FactorValue.Unmeasurable("not resolved")));
            }
        }

        var r = CompositeScorer.score(pool, cfg);
        assertFalse(r.heldForReview(),
                "a factor missing pool-wide affects everyone equally and needs no review");
        assertTrue(r.poolWideExclusions().containsKey(CERTIFICATIONS));

        var t = traceFor(r, 12L);
        var academic = t.factors().stream()
                .filter(f -> f.factor() == ACADEMIC_PERFORMANCE).findFirst().orElseThrow();
        assertEquals(0, academic.weightApplied().compareTo(bd("100")),
                "academics must absorb the dropped factor's weight");
        // The four students who DID have certificates lose that advantage entirely.
        assertEquals(12, r.ranked().size());
    }
}
