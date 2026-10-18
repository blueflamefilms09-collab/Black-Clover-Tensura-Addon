package com.newuniverse.nusmp.heroweapon.weapon.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * Why: the per-foe and per-cast limits of the budget are enforced by this counter in every multi-hit technique, so both limits are pinned: a foe may take its
 * allowed number of applications and no more, a new foe is refused once the distinct limit is reached while foes already counted may still take their turns,
 * and a repeat is recognised.
 */
class TallyTest {
    private static final UUID A = new UUID(0, 1), B = new UUID(0, 2), C = new UUID(0, 3);

    @Test
    void aFoeTakesAtMostItsShare() {
        Tally t = new Tally(5, 2);
        assertTrue(t.allows(A));
        assertFalse(t.seen(A));
        t.add(A);
        assertTrue(t.seen(A));
        assertTrue(t.allows(A));
        t.add(A);
        assertFalse(t.allows(A), "a third application is over the per-foe limit");
        assertEquals(2, t.count(A));
        assertEquals(1, t.distinct());
    }

    @Test
    void aNewFoeIsRefusedAtTheDistinctLimitButCountedFoesKeepTheirTurns() {
        Tally t = new Tally(2, 2);
        t.add(A);
        t.add(B);
        assertFalse(t.allows(C), "a third different foe is over the cast limit");
        assertTrue(t.allows(A), "a foe already counted may still take its second application");
        assertEquals(2, t.distinct());
    }

    @Test
    void zeroLimitsAllowNothing() {
        assertFalse(new Tally(0, 3).allows(A));
        assertFalse(new Tally(3, 0).allows(A));
        assertFalse(new Tally(-4, -4).allows(A));
    }
}
