package org.yuktisetu.model;

/**
 * How a custom criterion's configured operand is compared against a student's
 * value. Persisted as a string with length 12, so a new value must stay within
 * that -- NOT_EXISTS is the longest today at 10 characters.
 */
public enum CriterionOperator {
    GTE,
    LTE,
    GT,
    LT,
    EQ,
    NEQ,
    IN,
    NOT_IN,
    BETWEEN,
    /** The student has any value on record for this key. */
    EXISTS,
    /** The student has NO value on record -- e.g. "must not already hold a PPO". */
    NOT_EXISTS
}
