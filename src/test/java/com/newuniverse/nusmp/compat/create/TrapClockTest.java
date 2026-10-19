package com.newuniverse.nusmp.compat.create;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the timing of the traps and the matching rule of the contraption guard, pinned without a world. */
class TrapClockTest {
    private static final Blueprint.Band BAND = new Blueprint.Band(-1, 1, 2, 3, 3, 10, 40, 12, 8, HazardKind.SLASH, 1.0, -1);

    @Test
    void aBandStartsAfterItsOffsetThenWarnsStrikesAndRests() {
        assertEquals(TrapClock.Phase.QUIET, TrapClock.phase(0, BAND));
        assertEquals(TrapClock.Phase.QUIET, TrapClock.phase(9, BAND));
        assertEquals(TrapClock.Phase.WARN, TrapClock.phase(10, BAND));
        assertEquals(TrapClock.Phase.WARN, TrapClock.phase(21, BAND));
        assertEquals(TrapClock.Phase.STRIKE, TrapClock.phase(22, BAND));
        assertEquals(TrapClock.Phase.STRIKE, TrapClock.phase(29, BAND));
        assertEquals(TrapClock.Phase.QUIET, TrapClock.phase(30, BAND));
        assertEquals(TrapClock.Phase.QUIET, TrapClock.phase(49, BAND));
        assertEquals(TrapClock.Phase.WARN, TrapClock.phase(50, BAND), "the next cycle");
        assertEquals(-1, TrapClock.cycle(5, BAND));
        assertEquals(0, TrapClock.cycle(22, BAND));
        assertEquals(1, TrapClock.cycle(52, BAND));
    }

    @Test
    void aBandStrikesForExactlyItsActiveTicksPerCycle() {
        int strikes = 0;
        for (long t = 10; t < 10 + 40 * 5; t++) if (TrapClock.phase(t, BAND) == TrapClock.Phase.STRIKE) strikes++;
        assertEquals(8 * 5, strikes);
    }

    @Test
    void theMachineRunsCoolsDownAndRearms() {
        TrapClock.Machine m = new TrapClock.Machine();
        assertEquals(TrapClock.State.IDLE, m.look(0, false, false, 100, 50));
        assertEquals(TrapClock.State.IDLE, m.look(10, false, false, 100, 50));
        assertEquals(TrapClock.State.RUNNING, m.look(20, true, false, 100, 50));
        assertEquals(30, m.runAge(50));
        assertEquals(TrapClock.State.RUNNING, m.look(119, true, false, 100, 50));
        assertEquals(TrapClock.State.COOLDOWN, m.look(120, true, false, 100, 50));
        assertEquals(TrapClock.State.COOLDOWN, m.look(169, true, false, 100, 50), "a trigger during the cool-down is ignored");
        assertEquals(TrapClock.State.IDLE, m.look(170, true, false, 100, 50));
        assertEquals(TrapClock.State.RUNNING, m.look(171, true, false, 100, 50));
    }

    @Test
    void aLeverStopsEverythingAtOnceAndLettingGoRearms() {
        TrapClock.Machine m = new TrapClock.Machine();
        m.look(0, true, false, 100, 50);
        assertEquals(TrapClock.State.RUNNING, m.state());
        assertEquals(TrapClock.State.DISARMED, m.look(5, true, true, 100, 50));
        assertEquals(0, m.runAge(6));
        assertEquals(TrapClock.State.DISARMED, m.look(500, true, true, 100, 50), "stays disarmed however often it is triggered");
        assertEquals(TrapClock.State.IDLE, m.look(501, false, false, 100, 50));
        assertFalse(m.state() == TrapClock.State.RUNNING);
    }

    @Test
    void onlyCreateContraptionsAreRefused() {
        assertTrue(CreateIds.isContraptionEntity("create:contraption"));
        assertTrue(CreateIds.isContraptionEntity("create:stationary_contraption"));
        assertTrue(CreateIds.isContraptionEntity("create:gantry_contraption"));
        assertTrue(CreateIds.isContraptionEntity("create:carriage_contraption"));
        assertFalse(CreateIds.isContraptionEntity("create:super_glue"));
        assertFalse(CreateIds.isContraptionEntity("minecraft:zombie"));
        assertFalse(CreateIds.isContraptionEntity("othermod:contraption"));
        assertFalse(CreateIds.isContraptionEntity("contraption"));
        assertFalse(CreateIds.isContraptionEntity(null));
    }

    @Test
    void theAxisFlagMatchesCreatesRule() {
        assertTrue(Dir6.alongFirst('x', 'y'));
        assertFalse(Dir6.alongFirst('x', 'z'));
        assertTrue(Dir6.alongFirst('z', 'x'));
        assertFalse(Dir6.alongFirst('z', 'y'));
        assertTrue(Dir6.alongFirst('y', 'x'));
        assertFalse(Dir6.alongFirst('y', 'z'));
        for (Dir6 d : Dir6.values()) {
            assertEquals(d, d.opposite().opposite());
            int[] a = d.step(), b = d.opposite().step();
            assertEquals(0, a[0] + b[0] + a[1] + b[1] + a[2] + b[2]);
        }
    }
}
