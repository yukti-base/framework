package org.yuktisetu.profile.model;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The coding platforms YuktiSetu can read a public profile from.
 *
 * This was a free-text column until 2026-10-06, and the live data shows exactly
 * why that could not stand: the same platform was stored as both "LeetCode" and
 * "Leetcode", so a per-platform scraper keyed on the string would have silently
 * skipped three of the five LeetCode profiles on record. A scraper dispatches on
 * this value; it cannot be a value a student or an admin can spell freely.
 *
 * The mechanism differs per platform and that difference is the whole cost model
 * of the ingestion work, so it is recorded here rather than buried in the
 * scraper: CODEFORCES has an official public REST API and ATCODER a public JSON
 * feed, LEETCODE has an undocumented but stable public GraphQL contract, and the
 * rest have nothing and must be parsed out of HTML.
 */
public enum CodingPlatform {

    LEETCODE("LeetCode", Access.STRUCTURED_API),
    CODEFORCES("Codeforces", Access.OFFICIAL_API),
    ATCODER("AtCoder", Access.OFFICIAL_API),
    CODECHEF("CodeChef", Access.HTML),
    GEEKSFORGEEKS("GeeksforGeeks", Access.HTML),
    HACKERRANK("HackerRank", Access.HTML),
    HACKEREARTH("HackerEarth", Access.HTML),

    /**
     * Something the student listed that we have no adapter for. Stored and shown
     * to the TnP, never scraped, and contributes nothing to a score -- which is
     * honest, and is the reason this is a real value rather than a rejection:
     * refusing the link would just mean the TnP loses information it has today.
     */
    OTHER("Other", Access.NONE);

    /** How this platform's data is obtained, which is what its fragility follows from. */
    public enum Access {
        /** Documented, supported, rate-limit-published. Will not break on a redesign. */
        OFFICIAL_API,
        /** Undocumented but structured -- a field rename breaks one query, not a parser. */
        STRUCTURED_API,
        /** No API. Needs the full extraction ladder, golden fixtures and canaries. */
        HTML,
        /** No adapter. */
        NONE
    }

    private final String label;
    private final Access access;

    CodingPlatform(String label, Access access) {
        this.label = label;
        this.access = access;
    }

    /** How the platform's own branding spells it; for display, never for matching. */
    public String label() {
        return label;
    }

    public Access access() {
        return access;
    }

    /** True when an adapter exists, i.e. a snapshot can ever be produced for it. */
    public boolean isScrapable() {
        return access != Access.NONE;
    }

    /**
     * Lookup keyed on a squashed form -- lowercase, with spaces, dots, hyphens and
     * underscores stripped -- so "LeetCode", "Leetcode", "leet code" and
     * "leet-code" all resolve to the same platform.
     */
    private static final Map<String, CodingPlatform> BY_SQUASHED = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(p -> squash(p.label), p -> p));

    private static String squash(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[\\s._\\-]", "");
    }

    /**
     * Tolerant parse for whatever a human typed, used when migrating the old
     * free-text column and when accepting a profile from the student portal.
     *
     * Deliberately returns OTHER rather than throwing on an unknown name: the
     * student has given the TnP a real link to a real profile, and losing it
     * because we have no adapter would be worse than storing it unscraped.
     */
    public static CodingPlatform fromLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return OTHER;
        }
        return Optional.ofNullable(BY_SQUASHED.get(squash(raw))).orElse(OTHER);
    }
}
