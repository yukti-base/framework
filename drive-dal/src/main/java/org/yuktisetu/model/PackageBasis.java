package org.yuktisetu.model;

/**
 * Which number the re-application uplift rule compares against. Without this a
 * "20% above your current package" rule is ambiguous between CTC and base.
 */
public enum PackageBasis {
    CTC,
    BASE,
    STIPEND
}
