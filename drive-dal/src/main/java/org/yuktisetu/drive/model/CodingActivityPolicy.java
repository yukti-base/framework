package org.yuktisetu.drive.model;

import org.yuktisetu.profile.model.CodingPlatform;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Decides whether a student's account on a platform counts as real activity, and
 * reduces that platform's figures to one strength number.
 *
 * WHY AN ACTIVITY BAR EXISTS. A student is judged on the platform they stand
 * highest on, which is right when the difference between their accounts reflects
 * where they put their effort. It is wrong in exactly one case: an account made
 * last week with five problems solved, which can land high purely because few
 * applicants use that platform. Without a bar, "best effort" quietly becomes
 * "best accident", and the student who practised consistently on one platform
 * loses to one who opened three accounts and got lucky on the quietest.
 *
 * The bar is PER PLATFORM because the platforms publish different things.
 * Codeforces exposes a contest rating and nothing about problems solved;
 * GeeksforGeeks exposes a practice count and no rating. One shared rule would
 * either exclude an entire platform or wave everything through.
 *
 * It is a property of the DATA, not of the drive. A five-problem account is not
 * credible evidence whichever company is hiring, so this is not something the TnP
 * sets per drive -- though the thresholds are constructor arguments so they can be
 * tuned centrally once real cohorts have been seen.
 *
 * FAILING THE BAR IS NOT THE SAME AS BEING UNMEASURABLE. A student whose every
 * account falls below the bar has been measured: the honest finding is that they
 * have no meaningful competitive-coding record, which ranks at the bottom of the
 * factor. Excluding them instead would rescale coding away and leave them ABOVE
 * both a student with no account at all and a student with a real but modest
 * record -- rewarding the token account. The caller must therefore map
 * "no qualifying platform" to a measured absence, never to an outage.
 */
public final class CodingActivityPolicy {

    /**
     * What one platform demands before its figures are allowed to represent a
     * student.
     *
     * A platform qualifies when EITHER of two independent routes is satisfied, so
     * a committed practiser and a committed competitor both get through:
     *
     *   the practice route -- enough problems solved
     *   the contest route  -- a rating AND enough rated contests behind it
     *
     * The contest route insists on both because a rating from one or two contests
     * is provisional on every one of these platforms: it moves hundreds of points
     * on the next result and says more about who happened to turn up than about
     * the student.
     */
    public record Bar(Integer minSolved, Integer minContests, boolean ratingRequiredForContestRoute) {

        public boolean qualifies(CodingMetrics m) {
            if (m == null || m.isEmpty()) {
                return false;
            }
            if (minSolved != null && m.solvedTotal() != null && m.solvedTotal() >= minSolved) {
                return true;
            }
            if (minContests != null && m.contestsAttended() != null
                    && m.contestsAttended() >= minContests) {
                return !ratingRequiredForContestRoute || m.rating() != null;
            }
            return false;
        }
    }

    /**
     * Starting thresholds. Deliberately modest: the bar exists to exclude token
     * accounts, not to exclude ordinary students. A second-year who has solved
     * thirty problems has a real, if early, record and belongs in the comparison.
     *
     * Codeforces carries no solved threshold because the adapter does not read a
     * solved count -- getting one means pulling every submission a user has ever
     * made, which is the slowest call in the pipeline for a figure the rating
     * already reflects. So Codeforces qualifies on the contest route alone.
     */
    public static Map<CodingPlatform, Bar> defaultBars() {
        Map<CodingPlatform, Bar> bars = new EnumMap<>(CodingPlatform.class);
        bars.put(CodingPlatform.LEETCODE, new Bar(30, 3, true));
        bars.put(CodingPlatform.CODEFORCES, new Bar(null, 3, true));
        bars.put(CodingPlatform.CODECHEF, new Bar(30, 3, true));
        bars.put(CodingPlatform.ATCODER, new Bar(null, 3, true));
        bars.put(CodingPlatform.GEEKSFORGEEKS, new Bar(40, null, false));
        bars.put(CodingPlatform.HACKERRANK, new Bar(30, null, false));
        bars.put(CodingPlatform.HACKEREARTH, new Bar(30, null, false));
        // OTHER is never scraped, so it can never qualify. Absent on purpose.
        return Map.copyOf(bars);
    }

    private final Map<CodingPlatform, Bar> bars;

    public CodingActivityPolicy(Map<CodingPlatform, Bar> bars) {
        this.bars = Map.copyOf(new EnumMap<>(bars));
    }

    public static CodingActivityPolicy withDefaults() {
        return new CodingActivityPolicy(defaultBars());
    }

    /** Why a platform did not count, for the student's breakdown. Empty when it did. */
    public Optional<String> rejectionReason(CodingPlatform platform, CodingMetrics metrics) {
        Bar bar = bars.get(platform);
        if (bar == null) {
            return Optional.of(platform.label() + " is not a platform we read");
        }
        if (metrics == null || metrics.isEmpty()) {
            return Optional.of("no usable figures were read from " + platform.label());
        }
        if (bar.qualifies(metrics)) {
            return Optional.empty();
        }
        StringBuilder needed = new StringBuilder();
        if (bar.minSolved() != null) {
            needed.append(bar.minSolved()).append(" problems solved");
        }
        if (bar.minContests() != null) {
            if (needed.length() > 0) {
                needed.append(", or ");
            }
            needed.append(bar.minContests()).append(" rated contests");
        }
        return Optional.of(platform.label() + " activity is below the bar ("
                + "solved=" + metrics.solvedTotal()
                + ", contests=" + metrics.contestsAttended()
                + ", rating=" + metrics.rating()
                + "); needs " + needed);
    }

    public boolean qualifies(CodingPlatform platform, CodingMetrics metrics) {
        return rejectionReason(platform, metrics).isEmpty();
    }

    /**
     * One number standing for a student's strength on a platform, used only to
     * rank them against the other users of that same platform.
     *
     * It is never compared across platforms -- that is what the per-platform
     * percentile is for -- so it does not need to mean anything in absolute
     * terms. It only has to order the users of one platform sensibly.
     *
     * Built from contest rating where the platform has one, because a rating
     * already embeds difficulty, consistency and the strength of the field.
     * Difficulty-weighted solve counts are the fallback for the practice
     * platforms, with hard problems weighted far above easy ones so that grinding
     * a thousand easy problems does not outrank genuine depth.
     */
    public Optional<BigDecimal> strength(CodingPlatform platform, CodingMetrics m) {
        if (!qualifies(platform, m)) {
            return Optional.empty();
        }

        // Rated platforms: the rating is the measure. maxRating is used only as a
        // fallback, since current form matters more than a peak from two years ago.
        if (m.rating() != null) {
            return Optional.of(BigDecimal.valueOf(m.rating()));
        }
        if (m.maxRating() != null) {
            return Optional.of(BigDecimal.valueOf(m.maxRating()));
        }

        // Practice platforms: weight by difficulty where it is published.
        if (m.solvedEasy() != null || m.solvedMedium() != null || m.solvedHard() != null) {
            BigDecimal weighted = BigDecimal.ZERO
                    .add(BigDecimal.valueOf(m.intOr(m.solvedEasy(), 0)))
                    .add(BigDecimal.valueOf(m.intOr(m.solvedMedium(), 0) * 3L))
                    .add(BigDecimal.valueOf(m.intOr(m.solvedHard(), 0) * 8L));
            return Optional.of(weighted.setScale(2, RoundingMode.HALF_UP));
        }
        if (m.solvedTotal() != null) {
            return Optional.of(BigDecimal.valueOf(m.solvedTotal()));
        }
        // Qualified on something we cannot turn into a number. Should not happen
        // with the bars above, and is reported rather than guessed at.
        return Optional.empty();
    }

    public Map<CodingPlatform, Bar> bars() {
        return bars;
    }
}
