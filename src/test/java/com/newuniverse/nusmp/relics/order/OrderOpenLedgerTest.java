package com.newuniverse.nusmp.relics.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 1.35.0: the God of Order's Open Ledger (the Gates, the 100% mode) and the Erasure rule that now lets it be reached. */
class OrderOpenLedgerTest {
    @Test
    void healthAloneNoLongerEndsTheMatterBeforeTheLedgerOpens() {
        assertFalse(OrderTable.forcedErasureDue(0, 1f, false));
        assertFalse(OrderTable.forcedErasureDue(0, 0.30f, false), "35% used to end the matter: that was above the 25% of the Gates");
        assertFalse(OrderTable.forcedErasureDue(10, 0.02f, false));
        assertFalse(OrderTable.forcedErasureDue(OrderTable.ERASURE_AFTER_TICKS - 1, 0.9f, false));
        assertTrue(OrderTable.forcedErasureDue(OrderTable.ERASURE_AFTER_TICKS, 0.9f, false), "the time limit of 75 s still ends it");
    }

    @Test
    void withTheLedgerOpenItEndsByTimeOrWhenTheRefilledBarIsLow() {
        assertFalse(OrderTable.forcedErasureDue(OrderTable.ERASURE_AFTER_TICKS, 0.9f, true), "the open ledger buys time");
        assertFalse(OrderTable.forcedErasureDue(OrderTable.ERASURE_AFTER_TICKS + OrderTable.GATES_EXTRA_TICKS - 1, 0.9f, true));
        assertTrue(OrderTable.forcedErasureDue(OrderTable.ERASURE_AFTER_TICKS + OrderTable.GATES_EXTRA_TICKS, 0.9f, true));
        assertTrue(OrderTable.forcedErasureDue(10, OrderTable.GATES_ERASURE_FRACTION, true));
        assertFalse(OrderTable.forcedErasureDue(10, OrderTable.GATES_ERASURE_FRACTION + 0.05f, true));
    }

    @Test
    void theTimingsFitTheVisitAndTheGatesCanBeReached() {
        assertTrue(OrderTable.GATES_ERASURE_FRACTION < 0.25f, "below the health at which the Gates open, so the old 35% trap is gone");
        assertTrue(OrderTable.ERASURE_AFTER_TICKS + OrderTable.GATES_EXTRA_TICKS < OrderPlan.COMBAT_LIMIT_TICKS, "the Erasure still comes before the combat limit");
        // the 2-argument rule of 1.34 is unchanged (old callers and tests keep working)
        assertTrue(OrderTable.forcedErasureDue(10, 0.3f));
        assertFalse(OrderTable.forcedErasureDue(0, 1f));
    }

    @Test
    void theRingAndTheCircleAreWiderAtTheGatesAndTheBuildIsTougher() {
        assertEquals(OrderTable.DECREE_RADIUS, OrderTable.decreeRadius(false));
        assertEquals(OrderTable.JUDGEMENT_RADIUS, OrderTable.judgementRadius(false));
        assertEquals(OrderTable.DECREE_RADIUS * 1.3, OrderTable.decreeRadius(true), 1e-9);
        assertEquals(OrderTable.JUDGEMENT_RADIUS * 1.25, OrderTable.judgementRadius(true), 1e-9);
        assertTrue(OrderTable.judgementRadius(true) < 6.0 && OrderTable.decreeRadius(true) < 8.0, "still leavable in the wind-up");
        assertTrue(OrderTable.HEALTH > 1200 && OrderTable.ATTACK >= 14);
        assertEquals(OrderTable.ERASURE, OrderTable.specs().get(OrderTable.specs().size() - 1).id());
    }
}
