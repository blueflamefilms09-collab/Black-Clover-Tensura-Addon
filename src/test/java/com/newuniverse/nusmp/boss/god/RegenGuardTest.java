package com.newuniverse.nusmp.boss.god;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Time Decay: which heals pass, and the tick-to-tick comparison that catches heals that bypass the event. */
class RegenGuardTest {
    @Test
    void regenerationSizedHealsNeverPassEvenInsideAWindow() {
        assertFalse(RegenGuard.allowsHeal(1.0f, true));
        assertFalse(RegenGuard.allowsHeal(0.5f, true));
        assertFalse(RegenGuard.allowsHeal(1.99f, true));
    }

    @Test
    void aBurstHealPassesOnlyWhileAPotionOrFoodIsInUse() {
        assertTrue(RegenGuard.allowsHeal(4.0f, true), "instant health I");
        assertTrue(RegenGuard.allowsHeal(8.0f, true), "instant health II");
        assertFalse(RegenGuard.allowsHeal(4.0f, false), "no window, no heal");
        assertTrue(RegenGuard.allowsHeal(RegenGuard.BURST_HEAL, true));
    }

    @Test
    void aRiseNoAllowedHealExplainsIsRevertedToTheLastHealth() {
        RegenGuard.Tracker t = new RegenGuard.Tracker();
        assertTrue(Float.isNaN(t.check(10f, 20f, true)), "the first look only records");
        assertTrue(Float.isNaN(t.check(10f, 20f, true)));
        assertEquals(10f, t.check(11f, 20f, true), "setHealth upward with no heal event");
    }

    @Test
    void anAllowedHealIsCreditedAndKept() {
        RegenGuard.Tracker t = new RegenGuard.Tracker();
        t.check(10f, 20f, true);
        t.credit(4f);
        assertTrue(Float.isNaN(t.check(14f, 20f, true)), "the potion's 4 points are explained");
        assertEquals(14f, t.check(15f, 20f, true), "but one more point is not");
    }

    @Test
    void aBiggerRiseThanTheCreditIsCutBackToTheCredit() {
        RegenGuard.Tracker t = new RegenGuard.Tracker();
        t.check(10f, 20f, true);
        t.credit(4f);
        assertEquals(14f, t.check(18f, 20f, true));
    }

    @Test
    void fallsAreNeverTouchedAndMoveTheReference() {
        RegenGuard.Tracker t = new RegenGuard.Tracker();
        t.check(18f, 20f, true);
        assertTrue(Float.isNaN(t.check(9f, 20f, true)), "damage");
        assertEquals(9f, t.check(10f, 20f, true), "a rise from the new reference is caught");
    }

    @Test
    void aDeadOrRespawnedPlayerIsNotJudged() {
        RegenGuard.Tracker t = new RegenGuard.Tracker();
        t.check(2f, 20f, true);
        assertTrue(Float.isNaN(t.check(0f, 20f, false)), "dead");
        assertFalse(t.tracking());
        assertTrue(Float.isNaN(t.check(20f, 20f, true)), "respawned at full health: recorded, not reverted");
    }

    @Test
    void healthAboveTheMaximumIsNeverRaisedByTheGuard() {
        RegenGuard.Tracker t = new RegenGuard.Tracker();
        t.check(30f, 20f, true);
        assertTrue(Float.isNaN(t.check(20f, 20f, true)));
    }

    @Test
    void resetForgetsTheCredit() {
        RegenGuard.Tracker t = new RegenGuard.Tracker();
        t.check(10f, 20f, true);
        t.credit(8f);
        t.reset();
        t.check(10f, 20f, true);
        assertEquals(10f, t.check(13f, 20f, true), "the old credit did not survive the reset");
    }
}
