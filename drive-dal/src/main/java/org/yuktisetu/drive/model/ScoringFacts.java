package org.yuktisetu.drive.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * One student's scoring input, flattened out of their profile and snapshots.
 *
 * Follows CandidateFacts exactly: a plain value object with no JPA on it, so
 * CompositeScorer stays a pure function that can be unit-tested without a
 * database. Every DB read happens before any of these are built -- batched by id
 * collection, never one query per student.
 */
public record ScoringFacts(
        Long studentUserId,
        Map<ScoringFactor, FactorValue> values
) {
    public ScoringFacts {
        if (studentUserId == null) {
            throw new IllegalArgumentException("studentUserId is required");
        }
        // Same EnumMap-cannot-infer-from-empty trap as in CompositeScorer.Config.
        // An empty map is legitimate here: every weighted factor then resolves to
        // Unmeasurable, which the scorer handles.
        values = (values == null || values.isEmpty())
                ? Map.of() : Map.copyOf(new EnumMap<>(values));
    }

    /**
     * A factor the drive weights but that nothing supplied at all is Unmeasurable,
     * not MeasuredNone.
     *
     * The distinction is the point: "the TnP weighted certifications but the
     * resolver never populated them" is a gap in OUR data, and guessing that the
     * student has none would put them at the bottom of a distribution for a
     * question nobody asked them.
     */
    public FactorValue valueOf(ScoringFactor factor) {
        return values.getOrDefault(factor,
                new FactorValue.Unmeasurable("no value supplied for " + factor));
    }
}
