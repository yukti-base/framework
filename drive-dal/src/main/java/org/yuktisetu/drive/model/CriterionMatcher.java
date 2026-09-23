package org.yuktisetu.drive.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Compares one custom criterion's configured operand against a student's actual
 * value.
 *
 * Parses the operand out of valueJson with a small hand-rolled reader rather than
 * Jackson, deliberately: drive-dal's only dependencies are JPA, Lombok and the
 * Postgres driver, and this class is the single reason a JSON library would
 * otherwise be dragged in. The shapes accepted are narrow and documented below,
 * and anything outside them is treated as a non-match rather than throwing --
 * a malformed criterion must not take a whole eligibility run down with it.
 *
 * Accepted operand shapes:
 *   NUMBER        7.5
 *   STRING        "AWS Certified"   (quotes optional)
 *   BOOLEAN       true / false
 *   DATE          "2026-06-01"      (compared as a string; ISO order sorts correctly)
 *   NUMBER_LIST   [1, 2, 3]
 *   STRING_LIST   ["A", "B"]
 *   RANGE         [1, 5]            (inclusive, used with BETWEEN)
 */
public final class CriterionMatcher {

    private CriterionMatcher() {}

    public static boolean matches(CriteriaSnapshot.CustomCriterion criterion, String actualValue) {
        if (actualValue == null) {
            // EXISTS/NOT_EXISTS on a null value is decided by the caller's
            // MissingDataPolicy, not here.
            return criterion.operator() == CriterionOperator.NOT_EXISTS;
        }
        String operand = criterion.valueJson() == null ? "" : criterion.valueJson().trim();

        return switch (criterion.operator()) {
            case EXISTS -> !actualValue.isBlank();
            case NOT_EXISTS -> actualValue.isBlank();
            case EQ -> equalsValue(criterion, actualValue, operand);
            case NEQ -> !equalsValue(criterion, actualValue, operand);
            case IN -> parseList(operand).stream().anyMatch(v -> looseEquals(v, actualValue));
            case NOT_IN -> parseList(operand).stream().noneMatch(v -> looseEquals(v, actualValue));
            case GT, GTE, LT, LTE -> compareNumeric(criterion.operator(), actualValue, operand);
            case BETWEEN -> between(actualValue, operand);
        };
    }

    private static boolean equalsValue(CriteriaSnapshot.CustomCriterion criterion,
                                       String actual, String operand) {
        if (criterion.valueType() == CriterionValueType.NUMBER) {
            BigDecimal a = toNumber(actual);
            BigDecimal b = toNumber(operand);
            return a != null && b != null && a.compareTo(b) == 0;
        }
        return looseEquals(operand, actual);
    }

    private static boolean compareNumeric(CriterionOperator op, String actual, String operand) {
        BigDecimal a = toNumber(actual);
        BigDecimal b = toNumber(operand);
        if (a == null || b == null) {
            return false;
        }
        int cmp = a.compareTo(b);
        return switch (op) {
            case GT -> cmp > 0;
            case GTE -> cmp >= 0;
            case LT -> cmp < 0;
            case LTE -> cmp <= 0;
            default -> false;
        };
    }

    private static boolean between(String actual, String operand) {
        List<String> bounds = parseList(operand);
        if (bounds.size() != 2) {
            return false;
        }
        BigDecimal a = toNumber(actual);
        BigDecimal lo = toNumber(bounds.get(0));
        BigDecimal hi = toNumber(bounds.get(1));
        if (a == null || lo == null || hi == null) {
            return false;
        }
        return a.compareTo(lo) >= 0 && a.compareTo(hi) <= 0;
    }

    /** Case- and whitespace-insensitive: TnP-entered operands are hand-typed. */
    private static boolean looseEquals(String a, String b) {
        return unquote(a).equalsIgnoreCase(unquote(b));
    }

    private static List<String> parseList(String operand) {
        String body = operand;
        if (body.startsWith("[") && body.endsWith("]")) {
            body = body.substring(1, body.length() - 1);
        }
        List<String> out = new ArrayList<>();
        for (String part : body.split(",")) {
            String v = unquote(part);
            if (!v.isEmpty()) {
                out.add(v);
            }
        }
        return out;
    }

    private static String unquote(String s) {
        String v = s == null ? "" : s.trim();
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            v = v.substring(1, v.length() - 1);
        }
        return v.trim();
    }

    private static BigDecimal toNumber(String s) {
        try {
            return new BigDecimal(unquote(s));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
