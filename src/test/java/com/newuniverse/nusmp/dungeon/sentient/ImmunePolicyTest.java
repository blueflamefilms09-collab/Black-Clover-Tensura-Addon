package com.newuniverse.nusmp.dungeon.sentient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** What the immune system does as threat rises, and when a Juggernaut may be sent. */
class ImmunePolicyTest {
    @Test
    void theDungeonWarnsAtHalfThenAtFourFifthsThenActs() {
        assertEquals(ImmunePolicy.Step.NONE, ImmunePolicy.after(10, 100, 0));
        assertEquals(ImmunePolicy.Step.WARN_FIRST, ImmunePolicy.after(50, 100, 0));
        assertEquals(ImmunePolicy.Step.NONE, ImmunePolicy.after(60, 100, 1), "the first warning is given once");
        assertEquals(ImmunePolicy.Step.WARN_SECOND, ImmunePolicy.after(80, 100, 1));
        assertEquals(ImmunePolicy.Step.NONE, ImmunePolicy.after(90, 100, 2));
        assertEquals(ImmunePolicy.Step.SEND, ImmunePolicy.after(100, 100, 2));
        assertEquals(ImmunePolicy.Step.SEND, ImmunePolicy.after(160, 100, 0), "a massive spell skips the warnings");
        assertEquals(ImmunePolicy.Step.WARN_SECOND, ImmunePolicy.after(85, 100, 0), "a jump to four fifths gives only the second");
    }

    @Test
    void theWarningsComeBackAfterTheThreatFalls() {
        int warned = ImmunePolicy.warned(85, 100);
        assertEquals(2, warned);
        warned = ImmunePolicy.warned(20, 100);
        assertEquals(0, warned);
        assertEquals(ImmunePolicy.Step.WARN_FIRST, ImmunePolicy.after(55, 100, warned));
    }

    @Test
    void aJuggernautIsNotSentBeforeTheCooldownIsOver() {
        assertEquals(ImmunePolicy.Verdict.GO, ImmunePolicy.mayTry(false, 1000, 0, 0, 0, 0));
        assertEquals(ImmunePolicy.Verdict.COOLDOWN, ImmunePolicy.mayTry(false, 1000, 0, 1500, 0, 0));
        assertEquals(ImmunePolicy.Verdict.GO, ImmunePolicy.mayTry(false, 1500, 0, 1500, 0, 0));
    }

    @Test
    void aFailedSearchForAWallIsNotRepeatedEveryBlow() {
        long retry = ImmunePolicy.retryAt(1000);
        assertEquals(1100, retry);
        assertEquals(ImmunePolicy.Verdict.WAIT_RETRY, ImmunePolicy.mayTry(false, 1050, retry, 0, 0, 0));
        assertEquals(ImmunePolicy.Verdict.GO, ImmunePolicy.mayTry(false, 1100, retry, 0, 0, 0));
    }

    @Test
    void threeAliveOrComingIsTheCapEvenForTheAdmin() {
        assertEquals(ImmunePolicy.Verdict.CAP, ImmunePolicy.mayTry(false, 1000, 0, 0, 3, 0));
        assertEquals(ImmunePolicy.Verdict.CAP, ImmunePolicy.mayTry(false, 1000, 0, 0, 2, 1));
        assertEquals(ImmunePolicy.Verdict.CAP, ImmunePolicy.mayTry(true, 1000, 0, 0, 3, 0));
        assertEquals(ImmunePolicy.Verdict.GO, ImmunePolicy.mayTry(false, 1000, 0, 0, 2, 0));
    }

    @Test
    void theAdminSkipsTheCooldownAndTheRetryWaitButNotTheCap() {
        assertEquals(ImmunePolicy.Verdict.GO, ImmunePolicy.mayTry(true, 1000, 5000, 9000, 0, 0));
        assertEquals(ImmunePolicy.Verdict.CAP, ImmunePolicy.mayTry(true, 1000, 5000, 9000, 3, 0));
    }

    @Test
    void aThreatThatCouldNotBeAnsweredIsHeldJustBelowTheLine() {
        assertEquals(95.0, ImmunePolicy.held(100), 1e-9);
        ThreatMeter m = new ThreatMeter();
        m.add(400, 0);
        m.clampTo(ImmunePolicy.held(100), 0);
        assertFalse(m.triggered(100, 0));
        m.add(10, 0);
        assertTrue(m.triggered(100, 0), "the next blow after the cooldown answers at once");
    }

    @Test
    void aWholeStoryOfAPlayerHammeringAWall() {
        ThreatMeter m = new ThreatMeter();
        long cooldownUntil = 0, retryAt = 0;
        int warned = 0, sends = 0;
        int warnings = 0;
        for (long t = 0; t < 20 * 60 * 3; t++) {                                  // three minutes of holding the button down
            if (!m.wallHitDue(t)) continue;
            m.wallHit(t);
            switch (ImmunePolicy.after(m.value(t), 100, warned)) {
                case SEND -> {
                    if (ImmunePolicy.mayTry(false, t, retryAt, cooldownUntil, sends, 0) == ImmunePolicy.Verdict.GO) {
                        sends++;                                                  // (the cap is not tested here: nothing dies)
                        m.reset(t);
                        cooldownUntil = JuggernautPlan.cooldownUntil(t, 10);
                    } else {
                        m.clampTo(ImmunePolicy.held(100), t);
                    }
                }
                case WARN_FIRST, WARN_SECOND -> warnings++;
                default -> { }
            }
            warned = ImmunePolicy.warned(m.value(t), 100);
        }
        assertEquals(1, sends, "one Juggernaut in three minutes: the cooldown is ten");
        assertTrue(warnings >= 2, "the walls tremble first: " + warnings);
    }
}
