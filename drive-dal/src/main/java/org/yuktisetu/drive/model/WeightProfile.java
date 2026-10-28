package org.yuktisetu.drive.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * One drive's factor weights, exactly as the TnP entered them.
 *
 * THERE ARE NO DEFAULTS AND NO LIMITS. Any factor may be set to zero or to the
 * maximum, and this class imposes no minimum, no maximum, no "sensible range"
 * and no required total.
 *
 * That is not laxity, it is the requirement. A service company may be entirely
 * uninterested in competitive coding and set it to zero; a product company may
 * care about nothing but certifications and experience and set everything else to
 * zero. Both are normal configurations. Any check that nudged a coordinator
 * towards a balanced spread would quietly become the policy for every drive
 * nobody bothered to adjust.
 *
 * The one thing required is that SOMETHING is weighted above zero, since a drive
 * weighting nothing cannot rank anybody.
 *
 * Weights do not have to total 100. They are normalised internally, so 40/40/20
 * and 2/2/1 and 80/80/40 all describe the same drive. This is what lets a
 * coordinator type "coding 10, everything else 0" without having to do arithmetic
 * to make it add up.
 */
public record WeightProfile(Map<ScoringFactor, BigDecimal> weights) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    /** Internal precision for normalised shares. */
    private static final int SHARE_SCALE = 6;

    public WeightProfile {
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("a drive must weight at least one factor");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<ScoringFactor, BigDecimal> e : weights.entrySet()) {
            BigDecimal w = e.getValue();
            if (w == null) {
                throw new IllegalArgumentException("weight for " + e.getKey() + " is null");
            }
            if (w.signum() < 0) {
                // The one genuinely meaningless value: a negative weight would
                // mean "being better at this makes you worse", which no company
                // is expressing.
                throw new IllegalArgumentException(
                        "weight for " + e.getKey() + " is negative: " + w.toPlainString());
            }
            total = total.add(w);
        }
        if (total.signum() == 0) {
            throw new IllegalArgumentException(
                    "every factor is weighted zero; nothing can be scored");
        }
        weights = Map.copyOf(new EnumMap<>(weights));
    }

    /** Factors the TnP listed, including any set to zero. */
    public Set<ScoringFactor> factors() {
        return weights.keySet();
    }

    /** The raw figure the TnP typed, before normalisation. */
    public BigDecimal rawWeightOf(ScoringFactor factor) {
        return weights.getOrDefault(factor, BigDecimal.ZERO);
    }

    public BigDecimal total() {
        return weights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * The factors that actually carry weight. A factor at zero is listed but takes
     * no part, so it is excluded here -- which is also what stops it being counted
     * when weights are rescaled for a student with missing data.
     */
    public Set<ScoringFactor> weightedFactors() {
        EnumMap<ScoringFactor, BigDecimal> nonZero = new EnumMap<>(ScoringFactor.class);
        weights.forEach((f, w) -> {
            if (w.signum() > 0) {
                nonZero.put(f, w);
            }
        });
        return nonZero.keySet();
    }

    /**
     * Each factor's share out of 100, over the subset of factors that could
     * actually be measured for one student.
     *
     * Two jobs in one method, because they are the same arithmetic:
     *
     *  - Normalisation. The TnP's figures need not total 100, so they are
     *    converted to shares. 10/0/0 becomes 100/0/0; 2/2/1 becomes 40/40/20.
     *
     *  - Rescaling for missing data. When a factor could not be MEASURED for a
     *    student -- a platform outage, not a genuine absence -- it is left out of
     *    `available` and the remaining shares still total 100, so this student's
     *    composite stays on the same 0-100 scale as everybody else's.
     *
     * Rescaling is only ever correct when the absence has nothing to do with the
     * student's ability. A genuine "they have none" must never reach here; it is
     * scored at the bottom of its factor instead, because rescaling it away makes
     * having nothing outrank having something small.
     */
    public Map<ScoringFactor, BigDecimal> sharesOver(Set<ScoringFactor> available) {
        BigDecimal surviving = BigDecimal.ZERO;
        for (ScoringFactor f : available) {
            surviving = surviving.add(rawWeightOf(f));
        }
        if (surviving.signum() == 0) {
            throw new IllegalStateException(
                    "none of the weighted factors could be measured for this student");
        }

        Map<ScoringFactor, BigDecimal> shares = new EnumMap<>(ScoringFactor.class);
        for (ScoringFactor f : available) {
            BigDecimal w = rawWeightOf(f);
            if (w.signum() > 0) {
                shares.put(f, w.multiply(HUNDRED).divide(surviving, SHARE_SCALE, RoundingMode.HALF_UP));
            }
        }

        // Rounding a rescale can leave the total a hair off 100. The residue goes
        // onto the heaviest share, where it is proportionally smallest, so the
        // shares always total exactly 100 and a student's composite is never
        // quietly computed against 99.999998.
        BigDecimal total = shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal drift = HUNDRED.subtract(total);
        if (drift.signum() != 0 && !shares.isEmpty()) {
            ScoringFactor heaviest = shares.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).orElseThrow().getKey();
            shares.merge(heaviest, drift, BigDecimal::add);
        }
        return Map.copyOf(shares);
    }

    public static WeightProfile of(Map<ScoringFactor, BigDecimal> weights) {
        return new WeightProfile(weights);
    }
}
