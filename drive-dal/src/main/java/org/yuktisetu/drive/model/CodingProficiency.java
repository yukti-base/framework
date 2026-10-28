package org.yuktisetu.drive.model;

import org.yuktisetu.profile.model.CodingPlatform;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns a student's coding-platform figures into one coding percentile, by
 * judging them on the platform they are STRONGEST on.
 *
 * The rule, and why it is the max rather than an average: a higher standing on a
 * platform is the product of consistent practice there, not luck. Every one of
 * these platforms is a real platform with a real community, so a student who
 * stands higher among CodeChef users than among LeetCode users genuinely is a
 * CodeChef competitor and belongs in that comparison. Averaging their platforms
 * together would drag their best work down towards whichever account they
 * neglected, which is the opposite of what a shortlist should surface.
 *
 * "Strongest" means the highest PERCENTILE, not the biggest raw number -- the raw
 * numbers are not comparable at all. A student at the 80th percentile of LeetCode
 * and the 90th percentile of CodeChef is judged on CodeChef, whatever the two
 * underlying figures happen to be.
 *
 * COHORT REMOVAL. Once a student is judged on CodeChef they are taken out of the
 * LeetCode cohort entirely, so they no longer affect the percentile of the
 * students who ARE being judged on LeetCode. Without this a strong competitor who
 * is not even being measured on a platform still pushes everyone else down it.
 *
 * The algorithm terminates and is deterministic:
 *
 *   1. Percentiles on the FULL cohort of every pool member who uses each platform.
 *      This is the provisional ranking used to decide where each student is
 *      strongest -- it has to come first, because the choice cannot depend on an
 *      assignment that has not happened yet.
 *   2. Drop any platform too thinly used to mean anything. Then assign each
 *      student to their strongest surviving platform. If that leaves a platform
 *      with too few assigned students, drop it too and assign again. This repeats
 *      at most once per platform, because the surviving set only ever shrinks.
 *   3. Recompute each student's percentile inside their assigned cohort. These are
 *      the figures that count.
 */
public final class CodingProficiency {

    private CodingProficiency() {}

    /**
     * Where one student ended up, and the evidence for it.
     *
     * Carries the platforms that were considered and rejected, because "why am I
     * being judged on CodeChef when LeetCode is my main account?" is a question a
     * student will ask, and the answer is a comparison they are entitled to see.
     */
    public record Assignment(
            Long studentUserId,
            CodingPlatform platform,
            BigDecimal percentile,
            int cohortSize,
            Map<CodingPlatform, BigDecimal> consideredPercentiles
    ) {}

    public record Outcome(
            Map<Long, Assignment> assignments,
            /** Students with a platform listed but nothing usable. Coding is unmeasurable for them. */
            Set<Long> unresolved,
            /** Platforms dropped, and why. Worth an INFO log: this moves everyone. */
            Map<CodingPlatform, String> droppedPlatforms,
            /**
             * True when cohort removal had to be abandoned and percentiles were
             * taken over the full cohorts instead.
             *
             * Happens when removal would leave every platform below the minimum --
             * eight students split four and four across two platforms kills both.
             * Keeping coding with unrefined cohorts is far better than losing the
             * factor altogether, which on a coding-weighted drive would mean
             * scoring nobody. Logged at INFO, because it changes every student's
             * coding percentile.
             */
            boolean fellBackToFullCohorts
    ) {}

    /**
     * @param strengthByStudent studentUserId -> platform -> that platform's single
     *                          strength figure for them. Only platforms where the
     *                          student has genuinely usable activity belong here;
     *                          see the note on minActivity in the resolver that
     *                          builds this map.
     * @param minCohort         the fewest students a platform must have before its
     *                          percentiles mean anything.
     */
    public static Outcome resolve(
            Map<Long, Map<CodingPlatform, BigDecimal>> strengthByStudent,
            int minCohort) {

        Map<CodingPlatform, String> dropped = new EnumMap<>(CodingPlatform.class);
        if (strengthByStudent == null || strengthByStudent.isEmpty()) {
            return new Outcome(Map.of(), Set.of(), Map.of(), false);
        }

        // --- Step 1: full-cohort percentiles, used only to decide assignment. ---
        Map<CodingPlatform, List<BigDecimal>> rawByPlatform = new EnumMap<>(CodingPlatform.class);
        for (Map<CodingPlatform, BigDecimal> perPlatform : strengthByStudent.values()) {
            perPlatform.forEach((p, v) ->
                    rawByPlatform.computeIfAbsent(p, k -> new ArrayList<>()).add(v));
        }

        Map<CodingPlatform, Percentiles.Distribution> fullDist = new EnumMap<>(CodingPlatform.class);
        Set<CodingPlatform> viable = new HashSet<>();
        for (Map.Entry<CodingPlatform, List<BigDecimal>> e : rawByPlatform.entrySet()) {
            if (e.getValue().size() < minCohort) {
                dropped.put(e.getKey(), "only " + e.getValue().size()
                        + " applicants use it; at least " + minCohort + " are needed for a percentile");
                continue;
            }
            fullDist.put(e.getKey(), Percentiles.distribution(e.getValue()));
            viable.add(e.getKey());
        }

        Set<CodingPlatform> viableOnFullCohorts = Set.copyOf(viable);
        if (viable.isEmpty()) {
            // Not one platform has enough users for a percentile to mean anything.
            // Coding is genuinely unmeasurable for this pool, and the scorer
            // rescales it away rather than scoring everybody zero.
            return new Outcome(Map.of(), Set.copyOf(strengthByStudent.keySet()),
                    Map.copyOf(dropped), false);
        }

        // --- Step 2: assign, shrinking the viable set until every surviving
        // --- platform keeps enough assigned students.
        Map<Long, CodingPlatform> chosen = new HashMap<>();
        boolean fellBack = false;
        while (true) {
            chosen = assignBest(strengthByStudent, fullDist, viable);

            Map<CodingPlatform, Integer> counts = new EnumMap<>(CodingPlatform.class);
            for (CodingPlatform p : chosen.values()) {
                counts.merge(p, 1, Integer::sum);
            }
            Set<CodingPlatform> thin = new HashSet<>();
            for (CodingPlatform p : viable) {
                int assigned = counts.getOrDefault(p, 0);
                if (assigned < minCohort) {
                    thin.add(p);
                }
            }
            if (thin.isEmpty()) {
                break;
            }
            if (thin.containsAll(viable)) {
                // Removal has eaten everything. Two platforms sharing a pool
                // evenly do exactly this: eight students splitting four and four
                // leaves neither side viable, even though both platforms were
                // perfectly usable before anyone was moved.
                //
                // Abandon the removal rather than the factor. Percentiles revert
                // to the full cohorts, which means a student not being judged on a
                // platform still sits in its cohort -- less fair than the refined
                // version, and enormously better than losing coding entirely on a
                // drive that may weight it at 100.
                dropped.put(thin.iterator().next(),
                        "cohort removal would have left every platform below " + minCohort
                                + " assigned students, so percentiles were taken over the full "
                                + "cohorts instead and no student was removed from any cohort");
                viable = new HashSet<>(viableOnFullCohorts);
                chosen = assignBest(strengthByStudent, fullDist, viable);
                fellBack = true;
                break;
            }
            for (CodingPlatform p : thin) {
                dropped.put(p, "only " + counts.getOrDefault(p, 0)
                        + " applicants were judged strongest on it; at least " + minCohort
                        + " are needed. Those students fall back to their next-best platform.");
            }
            viable.removeAll(thin);
        }

        // --- Step 3: the percentiles that count. ---
        //
        // Normally over the ASSIGNED cohorts, so a student judged on CodeChef no
        // longer affects the LeetCode students' standing. Under the fallback above
        // the full cohorts are used unchanged.
        Map<CodingPlatform, Percentiles.Distribution> finalDist;
        if (fellBack) {
            finalDist = new EnumMap<>(fullDist);
        } else {
            Map<CodingPlatform, List<BigDecimal>> assignedRaw = new EnumMap<>(CodingPlatform.class);
            for (Map.Entry<Long, CodingPlatform> e : chosen.entrySet()) {
                BigDecimal raw = strengthByStudent.get(e.getKey()).get(e.getValue());
                assignedRaw.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(raw);
            }
            finalDist = new EnumMap<>(CodingPlatform.class);
            assignedRaw.forEach((p, vals) -> finalDist.put(p, Percentiles.distribution(vals)));
        }

        Map<Long, Assignment> assignments = new LinkedHashMap<>();
        Set<Long> unresolved = new HashSet<>();
        for (Long studentId : strengthByStudent.keySet()) {
            CodingPlatform p = chosen.get(studentId);
            if (p == null) {
                // Listed only platforms that got dropped.
                unresolved.add(studentId);
                continue;
            }
            BigDecimal raw = strengthByStudent.get(studentId).get(p);
            Percentiles.Distribution d = finalDist.get(p);

            // What the student was compared against on each platform they use, for
            // the breakdown they are shown.
            Map<CodingPlatform, BigDecimal> considered = new EnumMap<>(CodingPlatform.class);
            strengthByStudent.get(studentId).forEach((plat, v) -> {
                Percentiles.Distribution fd = fullDist.get(plat);
                if (fd != null) {
                    considered.put(plat, fd.percentileOf(v));
                }
            });

            assignments.put(studentId, new Assignment(
                    studentId, p, d.percentileOf(raw), d.sampleSize(), Map.copyOf(considered)));
        }
        return new Outcome(Map.copyOf(assignments), Set.copyOf(unresolved),
                Map.copyOf(dropped), fellBack);
    }

    /**
     * Each student's strongest viable platform by full-cohort percentile.
     *
     * Ties on percentile break on the platform's own name, purely so the result is
     * reproducible. A tie here means the student stands identically on two
     * platforms, so which one is reported changes nothing about their score.
     */
    private static Map<Long, CodingPlatform> assignBest(
            Map<Long, Map<CodingPlatform, BigDecimal>> strengthByStudent,
            Map<CodingPlatform, Percentiles.Distribution> fullDist,
            Set<CodingPlatform> viable) {

        Map<Long, CodingPlatform> chosen = new HashMap<>();
        for (Map.Entry<Long, Map<CodingPlatform, BigDecimal>> e : strengthByStudent.entrySet()) {
            CodingPlatform best = null;
            BigDecimal bestPct = null;
            for (Map.Entry<CodingPlatform, BigDecimal> pe : e.getValue().entrySet()) {
                if (!viable.contains(pe.getKey())) {
                    continue;
                }
                BigDecimal pct = fullDist.get(pe.getKey()).percentileOf(pe.getValue());
                int cmp = bestPct == null ? 1 : pct.compareTo(bestPct);
                if (cmp > 0 || (cmp == 0 && pe.getKey().name().compareTo(best.name()) < 0)) {
                    best = pe.getKey();
                    bestPct = pct;
                }
            }
            if (best != null) {
                chosen.put(e.getKey(), best);
            }
        }
        return chosen;
    }

    /** Orders assignments strongest first; for logs and the TnP's review screen. */
    public static Comparator<Assignment> strongestFirst() {
        return Comparator.comparing(Assignment::percentile).reversed()
                .thenComparing(Assignment::studentUserId);
    }
}
