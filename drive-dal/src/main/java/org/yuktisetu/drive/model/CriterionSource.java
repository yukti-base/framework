package org.yuktisetu.drive.model;


/**
 * DYNAMIC_FIELD is the important one: criterionKey then matches
 * DynamicFieldDefinition.fieldKey in user-dal and resolves through
 * StudentDynamicFieldValue. Company one-offs plug into the Advanced Fields
 * mechanism that already exists rather than inventing a parallel store.
 */
public enum CriterionSource {
    STUDENT_PROFILE_FIELD,
    DYNAMIC_FIELD,
    CODING_PROFILE,
    MANUAL
}
