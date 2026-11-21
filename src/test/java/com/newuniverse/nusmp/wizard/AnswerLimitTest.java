package com.newuniverse.nusmp.wizard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 4.89.1: the Tower Wizard's questions have no time limit by default; a limit in minutes is the old behaviour. */
class AnswerLimitTest {
    @Test
    void noLimitNeverExpires() {
        assertFalse(WizardSystem.expired(10_000_000L, 0L, 0));
    }

    @Test
    void aLimitExpiresAfterItsMinutes() {
        assertFalse(WizardSystem.expired(6000L, 0L, 5));
        assertTrue(WizardSystem.expired(6001L, 0L, 5));
    }
}
