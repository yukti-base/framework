package org.yuktisetu.identity.model;

/**
 * The minimum a notification producer needs about a person: who they are, where
 * to mail them, and what to call them.
 *
 * Exists so that building an audience is one projection query instead of loading
 * whole User entities. A 5000-student drive blast needs 5000 email addresses,
 * not 5000 managed entities with their role assignments and audit columns
 * attached -- and certainly not a findById inside the loop that builds the
 * recipient list.
 *
 * Lives in identity-dal because both identity-dal (staff audiences) and
 * drive-dal (eligible-student audiences) project into it, and drive-dal already
 * depends on identity-dal.
 */
public record NotifyTarget(Long userId, String email, String firstName, String lastName) {

    /** What the templates greet the person by. Never null, never a stray space. */
    public String displayName() {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? "there" : full;
    }

    public boolean isAddressable() {
        return userId != null && email != null && !email.isBlank();
    }
}
