package com.newuniverse.nusmp.relics.order;

import com.newuniverse.nusmp.relics.order.OrderPlan.Beat;
import com.newuniverse.nusmp.relics.order.OrderPlan.Phase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The state machine of one visit of the God of Order: its timeline, the blows it remembers, the limits that end it. */
class OrderPlanTest {
    /** Ticks until the plan returns a beat other than NONE, and that beat. */
    private static int until(OrderPlan p, Beat want) {
        for (int i = 1; i < 100_000; i++) if (p.tick() == want) return i;
        return -1;
    }

    @Test
    void theQuietVisitFollowsTheDesignTimeline() {
        OrderPlan p = new OrderPlan(false);
        assertEquals(Phase.TELEGRAPH, p.phase());
        assertEquals(120, until(p, Beat.APPEAR), "6 s telegraph, then it appears");
        assertEquals(Phase.READING, p.phase());
        assertTrue(p.invulnerable());
        assertEquals(100, until(p, Beat.BEAM_BEGIN), "5 s reading the verdict");
        assertEquals(Phase.BEAM, p.phase());
        assertTrue(p.invulnerable());
        assertEquals(40, until(p, Beat.STRIP), "the beam, then the duplicate is taken");
        assertEquals(Phase.BOW, p.phase());
        assertTrue(p.stripped());
        assertFalse(p.invulnerable(), "after the verdict it can be struck");
        assertEquals(40, until(p, Beat.LEAVE));
        assertEquals(Phase.DONE, p.phase());
        assertEquals(OrderPlan.TELEGRAPH_TICKS + OrderPlan.READ_TICKS + OrderPlan.BEAM_TICKS + OrderPlan.BOW_TICKS, p.totalTicks());
    }

    @Test
    void aBlowDuringTheReadingIsRememberedAndTheVerdictIsFinishedFirst() {
        OrderPlan p = new OrderPlan(false);
        until(p, Beat.APPEAR);
        p.provoke();
        assertTrue(p.provoked());
        assertEquals(Phase.READING, p.phase(), "it does not break off its reading");
        until(p, Beat.STRIP);
        assertEquals(Phase.COMBAT, p.phase(), "then it fights");
        assertFalse(p.invulnerable());
    }

    @Test
    void aBlowBeforeItAppearedIsNothing() {
        OrderPlan p = new OrderPlan(false);
        p.provoke();
        assertFalse(p.provoked());
        until(p, Beat.STRIP);
        assertEquals(Phase.BOW, p.phase());
    }

    @Test
    void aBlowDuringTheBowStartsTheFightAtOnce() {
        OrderPlan p = new OrderPlan(false);
        until(p, Beat.STRIP);
        assertEquals(Phase.BOW, p.phase());
        p.provoke();
        assertEquals(Phase.COMBAT, p.phase());
        assertEquals(0, p.ticksInPhase());
    }

    @Test
    void aCreativeHolderNeverStartsAFight() {
        OrderPlan p = new OrderPlan(true);
        until(p, Beat.APPEAR);
        p.provoke();
        assertFalse(p.provoked());
        until(p, Beat.STRIP);
        assertEquals(Phase.BOW, p.phase());
        p.provoke();
        assertEquals(Phase.BOW, p.phase(), "not even during the bow");
        assertEquals(40, until(p, Beat.LEAVE));
    }

    @Test
    void theErasureEndsTheFightAndTheGodLeavesAtOnce() {
        OrderPlan p = new OrderPlan(false);
        until(p, Beat.APPEAR);
        p.provoke();
        until(p, Beat.STRIP);
        assertEquals(Phase.COMBAT, p.phase());
        for (int i = 0; i < 300; i++) assertEquals(Beat.NONE, p.tick());
        assertEquals(Beat.LEAVE, p.erased());
        assertEquals(Phase.DONE, p.phase());
        assertEquals(Beat.NONE, p.erased(), "once");
        assertEquals(Beat.NONE, p.tick(), "a finished plan does nothing");
    }

    @Test
    void aFightThatNobodyEndsIsCutShortByTheTimeLimitAndTheGodWithdraws() {
        OrderPlan p = new OrderPlan(false);
        until(p, Beat.APPEAR);
        p.provoke();
        until(p, Beat.STRIP);
        for (int i = 0; i < OrderPlan.COMBAT_LIMIT_TICKS; i++) p.tick();
        assertEquals(Phase.BOW, p.phase(), "it gives up");
        assertTrue(p.noCombat());
        p.provoke();
        assertEquals(Phase.BOW, p.phase(), "and does not fight again");
        assertEquals(OrderPlan.BOW_TICKS, until(p, Beat.LEAVE));
    }

    @Test
    void noVisitOutlastsTheHardLimit() {
        OrderPlan p = new OrderPlan(false);
        until(p, Beat.APPEAR);
        p.provoke();
        until(p, Beat.STRIP);
        int ticks = p.totalTicks();
        Beat last = Beat.NONE;
        while (p.phase() != Phase.DONE && ticks < 100_000) { last = p.tick(); ticks++; if (last == Beat.LEAVE) break; }
        assertEquals(Phase.DONE, p.phase());
        assertTrue(p.totalTicks() <= OrderPlan.CASE_LIMIT_TICKS);
    }

    @Test
    void losingTheGodOrCancellingEndsTheCaseOnceFromAnyPhase() {
        for (Phase stop : new Phase[]{Phase.TELEGRAPH, Phase.READING, Phase.BEAM, Phase.BOW}) {
            OrderPlan p = new OrderPlan(false);
            while (p.phase() != stop) p.tick();
            assertEquals(Beat.LEAVE, p.lost(), stop.name());
            assertEquals(Beat.NONE, p.abort());
            assertEquals(Phase.DONE, p.phase());
        }
    }

    @Test
    void progressRunsFromZeroToOneOverAPhase() {
        OrderPlan p = new OrderPlan(false);
        assertEquals(0f, p.progress());
        for (int i = 0; i < 60; i++) p.tick();
        assertEquals(0.5f, p.progress(), 1e-4);
    }

    @Test
    void theGodIsHurtableOnlyOutsideTheVerdict() {
        OrderPlan p = new OrderPlan(false);
        assertFalse(p.invulnerable(), "it is not even there yet");
        until(p, Beat.APPEAR);
        assertTrue(p.invulnerable());
        until(p, Beat.BEAM_BEGIN);
        assertTrue(p.invulnerable());
        until(p, Beat.STRIP);
        assertFalse(p.invulnerable());
    }
}
