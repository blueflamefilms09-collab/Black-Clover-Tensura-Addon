package com.newuniverse.nusmp.heroweapon.weapon.glassworld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Why: the budget checker is what every weapon's number table is judged by, so the checker itself must be right: a row that obeys the owner's limits passes,
 * and each limit (hit size, total on one foe, rest, cost, foes touched, duration, ultimate, repeated id) is shown to be caught, so a weapon test that says
 * "no violations" means something.
 */
class BudgetTest {
    private static Budget.Spec ok(Budget.Kind k, int cooldown, double cost, double raw, int hits) {
        return new Budget.Spec("x.t", k, cooldown, cost, raw, hits, 6, 20);
    }

    @Test
    void aRowInsideEveryLimitPasses() {
        assertTrue(Budget.violations(ok(Budget.Kind.ORDINARY, 60, 0.015, 6.0, 2)).isEmpty());
        assertTrue(Budget.violations(ok(Budget.Kind.HEAVY, 140, 0.025, 10.0, 2)).isEmpty());
        assertTrue(Budget.violations(ok(Budget.Kind.ULTIMATE, 700, 0.06, 5.0, 6)).isEmpty());
        assertTrue(Budget.violations(ok(Budget.Kind.UTILITY, 100, 0.01, 0.0, 0)).isEmpty());
    }

    @Test
    void eachLimitIsCaught() {
        assertFalse(Budget.violations(ok(Budget.Kind.ORDINARY, 60, 0.015, 12.5, 1)).isEmpty(), "a hit over 12");
        assertFalse(Budget.violations(ok(Budget.Kind.ORDINARY, 60, 0.015, 4.0, 4)).isEmpty(), "16 on one foe is not ordinary");
        assertFalse(Budget.violations(ok(Budget.Kind.HEAVY, 60, 0.025, 10.0, 3)).isEmpty(), "30 on one foe is not heavy");
        assertFalse(Budget.violations(ok(Budget.Kind.ULTIMATE, 700, 0.06, 6.0, 6)).isEmpty(), "36 on one foe is over the ultimate limit");
        assertFalse(Budget.violations(ok(Budget.Kind.ORDINARY, 59, 0.015, 6.0, 1)).isEmpty(), "rests too briefly");
        assertFalse(Budget.violations(ok(Budget.Kind.UTILITY, 99, 0.015, 0.0, 0)).isEmpty(), "a utility rests at least 100");
        assertFalse(Budget.violations(ok(Budget.Kind.ULTIMATE, 599, 0.06, 5.0, 2)).isEmpty(), "an ultimate rests at least 600");
        assertFalse(Budget.violations(ok(Budget.Kind.ORDINARY, 60, 0.009, 6.0, 1)).isEmpty(), "under 1 percent");
        assertFalse(Budget.violations(ok(Budget.Kind.ORDINARY, 60, 0.031, 6.0, 1)).isEmpty(), "over 3 percent");
        assertFalse(Budget.violations(ok(Budget.Kind.ULTIMATE, 700, 0.03, 5.0, 2)).isEmpty(), "an ultimate costs 5 to 8 percent");
        assertFalse(Budget.violations(ok(Budget.Kind.ULTIMATE, 700, 0.081, 5.0, 2)).isEmpty(), "over 8 percent");
        assertFalse(Budget.violations(new Budget.Spec("x.t", Budget.Kind.ORDINARY, 60, 0.015, 6.0, 1, 13, 0)).isEmpty(), "13 foes");
        assertFalse(Budget.violations(new Budget.Spec("x.t", Budget.Kind.ORDINARY, 60, 0.015, 6.0, 1, 6, 401)).isEmpty(), "longer than 20 seconds");
        assertFalse(Budget.violations(new Budget.Spec("x.t", Budget.Kind.ULTIMATE, 700, 0.06, 0.0, 0, 6, 20)).isEmpty(), "an ultimate that does nothing");
        assertFalse(Budget.violations(new Budget.Spec("x.t", Budget.Kind.ORDINARY, 60, 0.015, 5.0, 0, 6, 0)).isEmpty(), "damage that never hits");
        assertFalse(Budget.violations(new Budget.Spec(" ", Budget.Kind.ORDINARY, 60, 0.015, 5.0, 1, 6, 0)).isEmpty(), "no id");
        assertFalse(Budget.violations((Budget.Spec) null).isEmpty(), "no row");
    }

    @Test
    void theLimitsAreExactlyTheOwnersNumbers() {
        assertEquals(12.0, Budget.Kind.ORDINARY.maxHit(), 1e-9);
        assertEquals(20.0, Budget.Kind.HEAVY.maxTotal(), 1e-9);
        assertEquals(32.0, Budget.Kind.ULTIMATE.maxTotal(), 1e-9);
        assertEquals(60, Budget.Kind.ORDINARY.minCooldown());
        assertEquals(100, Budget.Kind.UTILITY.minCooldown());
        assertEquals(600, Budget.Kind.ULTIMATE.minCooldown());
        assertEquals(12, Budget.MAX_TARGETS);
        assertEquals(400, Budget.MAX_DURATION_TICKS);
        // exactly on the edge is allowed
        assertTrue(Budget.violations(ok(Budget.Kind.ORDINARY, 60, 0.03, 12.0, 1)).isEmpty());
        assertTrue(Budget.violations(ok(Budget.Kind.HEAVY, 60, 0.01, 20.0, 1)).isEmpty());
        assertTrue(Budget.violations(ok(Budget.Kind.ULTIMATE, 600, 0.05, 32.0, 1)).isEmpty());
        assertTrue(Budget.violations(ok(Budget.Kind.ULTIMATE, 600, 0.08, 16.0, 2)).isEmpty());
    }

    @Test
    void aTableCatchesARepeatedId() {
        Budget.Spec a = ok(Budget.Kind.ORDINARY, 60, 0.015, 6.0, 1);
        assertTrue(Budget.violations(List.of(a)).isEmpty());
        assertFalse(Budget.violations(List.of(a, a)).isEmpty());
        assertEquals(12.0, ok(Budget.Kind.ORDINARY, 60, 0.015, 6.0, 2).total(), 1e-9);
    }
}
