package com.newuniverse.nusmp.arc.sds;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Echo of Memory strikes where you stood and the Marionette Strings move a player, so the memory of the places and the arithmetic of the pull are tested where a wrong number would hurt. */
class EchoTrailTest {
    private static EchoTrail walker() {
        EchoTrail t = new EchoTrail(140);
        for (int tick = 0; tick <= 100; tick += 4) t.record(7, tick, tick * 0.1, 64, 0);        // walks along +x at half a block a 5 ticks
        return t;
    }

    @Test
    void anEchoIsWhereTheFoeStoodSoManyTicksAgo() {
        EchoTrail t = walker();
        double[] a = t.at(7, 100, 20);
        assertNotNull(a);
        assertEquals(8.0, a[0], 1e-9, "tick 80 was at x = 8");
        assertEquals(64.0, a[1], 1e-9);
        double[] b = t.at(7, 100, 0);
        assertEquals(10.0, b[0], 1e-9, "now");
    }

    @Test
    void theNearestSampleAtOrBeforeIsUsed() {
        EchoTrail t = walker();
        double[] a = t.at(7, 100, 21);                       // tick 79: the last sample at or before is tick 76
        assertEquals(7.6, a[0], 1e-9);
    }

    @Test
    void anAgeTheTrailDoesNotReachIsSkipped() {
        EchoTrail t = new EchoTrail(140);
        t.record(1, 90, 5, 64, 5);
        t.record(1, 100, 6, 64, 5);
        assertNull(t.at(1, 100, 50), "the oldest sample is from tick 90");
        List<double[]> e = t.echoes(1, 100, new int[]{5, 20, 40});
        assertEquals(1, e.size(), "only the age that exists");
        assertNull(t.at(99, 100, 5), "an unknown entity has no trail");
    }

    @Test
    void aStandingFoeLeavesTheSameEchoEveryTime() {
        EchoTrail t = new EchoTrail(140);
        for (int tick = 0; tick <= 100; tick += 4) t.record(3, tick, 12, 64, -4);
        List<double[]> e = t.echoes(3, 100, GowtherTable.echoAges(false));
        assertEquals(3, e.size());
        for (double[] p : e) assertEquals(12.0, p[0], 1e-9);
    }

    @Test
    void theTrailForgetsWhatIsOlderThanItsLengthAndWhoWasNotSeen() {
        EchoTrail t = new EchoTrail(40);
        for (int tick = 0; tick <= 100; tick += 4) t.record(1, tick, tick, 64, 0);
        assertNull(t.at(1, 100, 80), "40 ticks is as far as it remembers");
        t.record(2, 0, 0, 64, 0);
        t.prune(100);
        assertEquals(1, t.tracked(), "the entity not seen for a while is dropped");
        t.forget(1);
        assertEquals(0, t.tracked());
        t.record(5, 1, 1, 1, 1);
        t.clear();
        assertEquals(0, t.tracked());
    }

    @Test
    void aRepeatInTheSameTickReplacesTheSample() {
        EchoTrail t = new EchoTrail(140);
        t.record(1, 10, 1, 64, 1);
        t.record(1, 10, 2, 64, 2);
        assertEquals(2.0, t.at(1, 10, 0)[0], 1e-9);
    }

    @Test
    void theStringPullsTowardsHimAndStopsInFront() {
        assertTrue(StringPull.reaches(12, 30));
        assertFalse(StringPull.reaches(31, 30));
        assertFalse(StringPull.reaches(0, 30));
        assertEquals(0.0, StringPull.speed(2.6, 2.6), 1e-9, "it has arrived");
        assertEquals(0.0, StringPull.speed(1.0, 2.6), 1e-9);
        assertEquals(StringPull.MAX_SPEED, StringPull.speed(30, 2.6), 1e-9, "a long way is the capped speed");
        assertTrue(StringPull.speed(5, 2.6) < StringPull.speed(9, 2.6));
        assertTrue(StringPull.ticksNeeded(20, 2.6, 40) <= 30, "a pull from twenty blocks is over within a second and a half");
        assertEquals(GowtherTable.STRINGS_TICKS, StringPull.ticksNeeded(30, 2.6, GowtherTable.STRINGS_TICKS), "a pull from the far end is cut off by the move's own limit");
        assertTrue(GowtherTable.STRINGS_TICKS * StringPull.MAX_SPEED <= 12.5, "a string never drags a foe more than about twelve blocks");
        assertEquals(0, StringPull.ticksNeeded(2.0, 2.6, 40));
        double[] d = StringPull.destination(0, 0, 10, 0, 2.6);
        assertEquals(2.6, d[0], 1e-9);
        assertEquals(0.0, d[1], 1e-9);
        double[] near = StringPull.destination(0, 0, 1, 1, 2.6);
        assertEquals(1.0, near[0], 1e-9, "a foe already close is left where it is");
    }
}
