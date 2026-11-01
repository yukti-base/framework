package org.yuktisetu.drive.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the student emails out of whatever the TnP pastes or uploads.
 *
 * A company's list arrives in every shape: one email per line, comma or
 * semicolon separated, "Asha Rao <asha@pccoe.org>", or an Excel sheet with name,
 * PRN and email columns (the portal turns the sheet into text first). So this
 * does not split on a separator -- it finds every email-shaped token anywhere in
 * the text, and ignores everything else.
 *
 * Emails are matched case-insensitively (stored lower-cased), and a list naming
 * the same student twice counts them once and says so.
 */
public final class EmailList {

    /** A company list is a few hundred names. This is a ceiling on abuse, not on use. */
    public static final int MAX_EMAILS = 5000;

    // Deliberately permissive on the local part (dots, plus, hyphens, digits) and
    // strict on the shape: something@domain.tld. A trailing dot or comma from
    // prose is not part of the match.
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+'-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)*\\.[A-Za-z]{2,}");

    // An '@' left over after every email was taken out: almost certainly a typo'd
    // address ("asha@pccoe", "asha @pccoe.org"). Reported, never guessed at.
    private static final Pattern STRAY_AT = Pattern.compile("\\S*@\\S*");

    private EmailList() {}

    /**
     * @param emails     the distinct emails, lower-cased, in the order first seen
     * @param duplicates email -> how many extra times it appeared
     * @param unreadable fragments that look like a broken email, at most 50
     */
    public record Parsed(List<String> emails, Map<String, Integer> duplicates, List<String> unreadable) {}

    public static Parsed parse(String text) {
        if (text == null || text.isBlank()) {
            return new Parsed(List.of(), Map.of(), List.of());
        }
        Set<String> seen = new LinkedHashSet<>();
        Map<String, Integer> duplicates = new LinkedHashMap<>();
        Matcher m = EMAIL.matcher(text);
        StringBuilder rest = new StringBuilder();
        int last = 0;
        while (m.find()) {
            rest.append(text, last, m.start()).append(' ');
            last = m.end();
            String email = m.group().toLowerCase(Locale.ROOT);
            // A leading quote or dot from the surrounding text is not part of an address.
            while (!email.isEmpty() && (email.charAt(0) == '\'' || email.charAt(0) == '.')) {
                email = email.substring(1);
            }
            if (!seen.add(email)) {
                duplicates.merge(email, 1, Integer::sum);
            }
            if (seen.size() > MAX_EMAILS) {
                throw new IllegalArgumentException("A list can name at most " + MAX_EMAILS + " students.");
            }
        }
        rest.append(text.substring(last));

        List<String> unreadable = new ArrayList<>();
        Matcher stray = STRAY_AT.matcher(rest);
        while (stray.find() && unreadable.size() < 50) {
            unreadable.add(stray.group());
        }
        return new Parsed(List.copyOf(seen), duplicates, unreadable);
    }
}
