package org.yuktisetu.drive.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoundRulesTest {

    @Test
    @DisplayName("publishing: advanced stays in (or is offered on the final round); anything else is out")
    void publish() {
        assertEquals(ApplicationStatus.SHORTLISTED, RoundRules.statusAfterPublish(RoundOutcome.ADVANCED, false));
        assertEquals(ApplicationStatus.OFFERED, RoundRules.statusAfterPublish(RoundOutcome.ADVANCED, true));
        for (boolean last : new boolean[]{false, true}) {
            assertEquals(ApplicationStatus.REJECTED, RoundRules.statusAfterPublish(RoundOutcome.NOT_ADVANCED, last));
            assertEquals(ApplicationStatus.REJECTED, RoundRules.statusAfterPublish(RoundOutcome.ABSENT, last));
        }
    }

    @Test
    @DisplayName("correcting not-advanced <-> absent moves nobody")
    void sameSideCorrection() {
        assertTrue(RoundRules.moveForCorrection(RoundOutcome.NOT_ADVANCED, RoundOutcome.ABSENT, false).isEmpty());
        assertTrue(RoundRules.moveForCorrection(RoundOutcome.ABSENT, RoundOutcome.NOT_ADVANCED, true).isEmpty());
        assertTrue(RoundRules.moveForCorrection(RoundOutcome.ADVANCED, RoundOutcome.ADVANCED, true).isEmpty());
    }

    @Test
    @DisplayName("correcting across the line moves the application, and every move is a legal transition")
    void crossingCorrection() {
        assertEquals(new RoundRules.StatusMove(ApplicationStatus.REJECTED, ApplicationStatus.SHORTLISTED),
                RoundRules.moveForCorrection(RoundOutcome.NOT_ADVANCED, RoundOutcome.ADVANCED, false).orElseThrow());
        assertEquals(new RoundRules.StatusMove(ApplicationStatus.SHORTLISTED, ApplicationStatus.REJECTED),
                RoundRules.moveForCorrection(RoundOutcome.ADVANCED, RoundOutcome.ABSENT, false).orElseThrow());
        assertEquals(new RoundRules.StatusMove(ApplicationStatus.REJECTED, ApplicationStatus.OFFERED),
                RoundRules.moveForCorrection(RoundOutcome.ABSENT, RoundOutcome.ADVANCED, true).orElseThrow());
        assertEquals(new RoundRules.StatusMove(ApplicationStatus.OFFERED, ApplicationStatus.REJECTED),
                RoundRules.moveForCorrection(RoundOutcome.ADVANCED, RoundOutcome.NOT_ADVANCED, true).orElseThrow());
    }
}
