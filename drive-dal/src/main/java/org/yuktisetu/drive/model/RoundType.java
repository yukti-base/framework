package org.yuktisetu.drive.model;

/** What kind of round a drive step is. Only a label; no round type behaves differently. */
public enum RoundType {
    APTITUDE_TEST("Aptitude test"),
    CODING_TEST("Coding test"),
    GROUP_DISCUSSION("Group discussion"),
    TECHNICAL_INTERVIEW("Technical interview"),
    HR_INTERVIEW("HR interview"),
    OTHER("Round");

    private final String label;

    RoundType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
