package org.yuktisetu.drive.model;


/**
 * Only COCUBES has a student-side column today (StudentProfile.coCubesScore).
 * The others are configurable but inert until user-dal gains a score table --
 * the evaluator records them as unevaluated rather than silently passing them.
 */
public enum AptitudeExam {
    COCUBES,
    AMCAT,
    ELITMUS,
    OTHER
}
