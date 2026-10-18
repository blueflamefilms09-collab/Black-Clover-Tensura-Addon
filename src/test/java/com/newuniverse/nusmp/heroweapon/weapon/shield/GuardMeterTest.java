package com.newuniverse.nusmp.heroweapon.weapon.shield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: the guard's Resolve and Counter are the rules the whole Shield plays by (what a raised shield soaks, when it breaks, what a parry is, how the stored answer
 * charges and fades). Each rule is checked here with plain numbers, including the awkward ones: a blow bigger than the Resolve, a broken guard, a world clock that
 * jumps back, and a capacity that shrinks.
 */
class GuardMeterTest {
    private static final double MAX = 20, MULT = 1.5;               // 20 health, so a Resolve of 30

    @Test
    void aBlowUnderTheResolveIsSoakedWholeAndCostsResolve() {
        GuardMeter g = new GuardMeter();
        GuardMeter.Result r = g.block(100, 8, MAX, MULT, false, 1.0);
        assertEquals(0.0, r.through(), 1e-9);
        assertEquals(8.0, r.absorbed(), 1e-9);
        assertFalse(r.broken());
        assertFalse(r.parried());
        assertEquals(22.0, g.resolve(MAX, MULT), 1e-9);
    }

    @Test
    void aBlowBiggerThanWhatIsLeftBreaksTheGuardAndTheRestGoesThrough() {
        GuardMeter g = new GuardMeter();
        g.block(100, 25, MAX, MULT, false, 1.0);                      // 5 left
        GuardMeter.Result r = g.block(110, 12, MAX, MULT, false, 1.0);
        assertTrue(r.broken());
        assertEquals(5.0, r.absorbed(), 1e-9);
        assertEquals(7.0, r.through(), 1e-9);
        assertEquals(0.0, g.resolve(MAX, MULT), 1e-9);
        assertTrue(g.broken(111));
        assertTrue(g.broken(110 + ShieldNumbers.BREAK_TICKS - 1));
        assertFalse(g.broken(110 + ShieldNumbers.BREAK_TICKS));
    }

    @Test
    void aBrokenGuardSoaksNothingUntilItMends() {
        GuardMeter g = new GuardMeter();
        g.block(100, 40, MAX, MULT, false, 1.0);                      // breaks at once: 30 soaked, 10 through
        GuardMeter.Result r = g.block(120, 6, MAX, MULT, false, 1.0);
        assertEquals(6.0, r.through(), 1e-9);
        assertEquals(0.0, r.absorbed(), 1e-9);
        assertEquals(0.0, r.counterGain(), 1e-9);
    }

    @Test
    void theGuardComesBackWithAQuarterOfItsResolveWhenTheBreakEnds() {
        GuardMeter g = new GuardMeter();
        g.block(100, 40, MAX, MULT, false, 1.0);
        g.tick(100 + ShieldNumbers.BREAK_TICKS - 1, MAX, MULT);
        assertEquals(0.0, g.resolve(MAX, MULT), 1e-9, "still broken");
        g.tick(100 + ShieldNumbers.BREAK_TICKS, MAX, MULT);
        double back = g.resolve(MAX, MULT);                            // the quarter, plus the one tick of regeneration that follows it
        assertTrue(back >= 30.0 * ShieldNumbers.AFTER_BREAK && back < 30.0 * ShieldNumbers.AFTER_BREAK + 1.0, "came back with " + back);
    }

    @Test
    void aParryCostsNoResolveAndGivesDoubleCounter() {
        GuardMeter g = new GuardMeter();
        GuardMeter.Result plain = new GuardMeter().block(100, 10, MAX, MULT, false, 1.0);
        GuardMeter.Result r = g.block(100, 10, MAX, MULT, true, 1.0);
        assertTrue(r.parried());
        assertEquals(0.0, r.through(), 1e-9);
        assertEquals(30.0, g.resolve(MAX, MULT), 1e-9, "a parry leaves the Resolve alone");
        assertEquals(plain.counterGain() * ShieldNumbers.PARRY_COUNTER_FACTOR, r.counterGain(), 1e-9);
        assertEquals(r.counterGain(), g.counter(), 1e-9);
    }

    @Test
    void aSecondTimedBlowInsideThePauseIsOnlyAnOrdinaryBlock() {
        GuardMeter g = new GuardMeter();
        assertTrue(g.block(100, 4, MAX, MULT, true, 1.0).parried());
        GuardMeter.Result again = g.block(100 + ShieldNumbers.PARRY_PAUSE - 1, 4, MAX, MULT, true, 1.0);
        assertFalse(again.parried(), "lowering and raising the guard again and again cannot make every blow a parry");
        assertEquals(4.0, again.absorbed(), 1e-9);
        assertEquals(26.0, g.resolve(MAX, MULT), 1e-9, "the second one cost Resolve");
        assertTrue(g.block(100 + ShieldNumbers.PARRY_PAUSE - 1 + ShieldNumbers.PARRY_PAUSE, 4, MAX, MULT, true, 1.0).parried(), "after the pause a timed block parries again");
    }

    @Test
    void anUntimedBlowIsNeverAParryWhateverTheClock() {
        GuardMeter g = new GuardMeter();
        assertFalse(g.block(100, 4, MAX, MULT, false, 1.0).parried());
        assertFalse(g.block(1000, 4, MAX, MULT, false, 1.0).parried());
    }

    @Test
    void aParryWindowIsTheFirstTicksAfterTheGuardBecomesActive() {
        assertTrue(GuardMeter.isParry(ShieldNumbers.BLOCK_DELAY, ShieldNumbers.PARRY_WINDOW));
        assertTrue(GuardMeter.isParry(ShieldNumbers.BLOCK_DELAY + ShieldNumbers.PARRY_WINDOW, ShieldNumbers.PARRY_WINDOW));
        assertFalse(GuardMeter.isParry(ShieldNumbers.BLOCK_DELAY + ShieldNumbers.PARRY_WINDOW + 1, ShieldNumbers.PARRY_WINDOW));
        assertTrue(GuardMeter.isParry(ShieldNumbers.BLOCK_DELAY + ShieldNumbers.PARRY_WINDOW + 1, ShieldNumbers.PARRY_WINDOW_BULWARK), "the Light Metal form's window is wider");
        assertFalse(GuardMeter.isParry(-1, 8));
    }

    @Test
    void blockedDamageChargesTheCounterAsAShareOfMaxHealthAndItIsCapped() {
        GuardMeter g = new GuardMeter();
        g.block(100, 4, MAX, MULT, false, 1.0);                       // 4 of 20 health = 20 percent
        assertEquals(20.0, g.counter(), 1e-9);
        g.block(101, 6, MAX, MULT, false, 1.0);                       // 30 more
        assertEquals(50.0, g.counter(), 1e-9);
        g.block(102, 20, MAX, MULT, false, 1.0);                      // would be 100 more
        assertEquals(ShieldNumbers.COUNTER_MAX, g.counter(), 1e-9);
    }

    @Test
    void spendingTheCounterTakesOnlyWhatIsThere() {
        GuardMeter g = new GuardMeter();
        g.addCounter(100, 30);
        assertEquals(10.0, g.spendCounter(10), 1e-9);
        assertEquals(20.0, g.counter(), 1e-9);
        assertEquals(20.0, g.spendCounter(50), 1e-9);
        assertEquals(0.0, g.counter(), 1e-9);
        assertEquals(0.0, g.spendCounter(5), 1e-9);
        assertEquals(0.0, g.spendCounter(-5), 1e-9, "a negative spend takes nothing");
    }

    @Test
    void resolveComesBackOnlyAfterAQuietSpell() {
        GuardMeter g = new GuardMeter();
        g.block(100, 20, MAX, MULT, false, 1.0);                      // 10 left
        g.tick(110, MAX, MULT);
        assertEquals(10.0, g.resolve(MAX, MULT), 1e-9, "no regeneration right after a block");
        g.tick(100 + ShieldNumbers.REGEN_DELAY, MAX, MULT);
        double after = g.resolve(MAX, MULT);
        assertTrue(after > 10.0 && after < 30.0, "regenerating, now " + after);
        for (long t = 100 + ShieldNumbers.REGEN_DELAY + 10; t < 100 + ShieldNumbers.REGEN_DELAY + 2000; t += 10) g.tick(t, MAX, MULT);
        assertEquals(30.0, g.resolve(MAX, MULT), 1e-9, "full again, never over the capacity");
    }

    @Test
    void theCounterFadesAfterTheHoldTime() {
        GuardMeter g = new GuardMeter();
        g.addCounter(100, 50);
        g.tick(100 + ShieldNumbers.COUNTER_HOLD - 1, MAX, MULT);
        assertEquals(50.0, g.counter(), 1e-9, "held");
        for (long t = 100 + ShieldNumbers.COUNTER_HOLD; t < 100 + ShieldNumbers.COUNTER_HOLD + 2000; t += 10) g.tick(t, MAX, MULT);
        assertEquals(0.0, g.counter(), 1e-9, "faded to nothing, never below");
    }

    @Test
    void aShrinkingCapacityClampsTheResolve() {
        GuardMeter g = new GuardMeter();
        assertEquals(30.0, g.resolve(MAX, MULT), 1e-9);
        assertEquals(15.0, g.resolve(10, MULT), 1e-9, "less health, less capacity");
        g.tick(500, 10, MULT);
        assertTrue(g.resolve(10, MULT) <= 15.0 + 1e-9);
    }

    @Test
    void aWorldClockThatJumpsBackNeverSticksTheMeter() {
        GuardMeter g = new GuardMeter();
        g.block(100000, 20, MAX, MULT, false, 1.0);
        g.tick(50, MAX, MULT);                                         // the clock went back
        g.tick(51, MAX, MULT);
        double r = g.resolve(MAX, MULT);
        assertTrue(r > 10.0 && r <= 30.0, "the meter regenerated instead of waiting for a time that will not come: " + r);
        GuardMeter.Result next = g.block(60, 5, MAX, MULT, false, 1.0);
        assertTrue(next.absorbed() >= 0.0 && next.through() >= 0.0);
    }

    @Test
    void nothingBlockedGivesNothing() {
        GuardMeter g = new GuardMeter();
        GuardMeter.Result r = g.block(100, 0, MAX, MULT, false, 1.0);
        assertEquals(0.0, r.absorbed(), 1e-9);
        assertEquals(0.0, r.through(), 1e-9);
        assertEquals(0.0, g.counter(), 1e-9);
        GuardMeter.Result n = g.block(100, -5, MAX, MULT, false, 1.0);
        assertEquals(0.0, n.through(), 1e-9);
        GuardMeter.Result nan = g.block(100, Double.NaN, MAX, MULT, false, 1.0);
        assertEquals(0.0, nan.through(), 1e-9);
    }

    @Test
    void theResolveShareGoesFromOneToZero() {
        GuardMeter g = new GuardMeter();
        assertEquals(1.0, g.share(MAX, MULT), 1e-9);
        g.block(100, 15, MAX, MULT, false, 1.0);
        assertEquals(0.5, g.share(MAX, MULT), 1e-9);
        g.block(101, 100, MAX, MULT, false, 1.0);
        assertEquals(0.0, g.share(MAX, MULT), 1e-9);
    }

    @Test
    void resetStartsOver() {
        GuardMeter g = new GuardMeter();
        g.block(100, 40, MAX, MULT, false, 1.0);
        g.addCounter(100, 60);
        g.reset();
        assertEquals(30.0, g.resolve(MAX, MULT), 1e-9);
        assertEquals(0.0, g.counter(), 1e-9);
        assertFalse(g.broken(101));
    }

    @Test
    void capacityIsHealthTimesMultipleAndNeverBelowOne() {
        assertEquals(30.0, GuardMeter.capacity(20, 1.5), 1e-9);
        assertEquals(1.0, GuardMeter.capacity(0, 1.5), 1e-9);
        assertEquals(1.0, GuardMeter.capacity(20, -3), 1e-9);
    }
}
