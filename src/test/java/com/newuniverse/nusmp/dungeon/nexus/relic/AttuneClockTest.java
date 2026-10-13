package com.newuniverse.nusmp.dungeon.nexus.relic;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The ten second attunement and the cap of three (docs/living_dungeon.md section 7). */
class AttuneClockTest {
    private static AttuneClock.Step run(AttuneClock c, int ticks, boolean sneak, int slot, int relic) {
        AttuneClock.Step last = null;
        for (int i = 0; i < ticks; i++) last = c.tick(sneak, slot, relic, false);
        return last;
    }

    @Test
    void tenSecondsOfSneakingAttunes() {
        AttuneClock c = new AttuneClock();
        assertEquals(AttuneClock.Step.RUNNING, run(c, AttuneClock.TICKS - 1, true, 2, 1));
        assertEquals(AttuneClock.TICKS - 1, c.progress());
        assertEquals(1, c.secondsLeft());
        assertEquals(AttuneClock.Step.DONE, c.tick(true, 2, 1, false));
        assertEquals(0, c.progress(), "done clears the clock");
        assertEquals(AttuneClock.Step.IDLE, c.tick(false, 2, 1, false));
    }

    @Test
    void tenSecondsIsExactlyTwoHundredTicks() { assertEquals(200, AttuneClock.TICKS); }

    @Test
    void stoppingSneakingResets() {
        AttuneClock c = new AttuneClock();
        run(c, 150, true, 0, 3);
        assertEquals(AttuneClock.Step.RESET, c.tick(false, 0, 3, false));
        assertEquals(0, c.progress());
        assertEquals(AttuneClock.Step.RUNNING, c.tick(true, 0, 3, false), "sneaking again starts a new ten seconds");
        assertEquals(1, c.progress());
    }

    @Test
    void takingDamageResets() {
        AttuneClock c = new AttuneClock();
        run(c, 100, true, 4, 0);
        assertEquals(AttuneClock.Step.RESET, c.tick(true, 4, 0, true));
        assertEquals(0, c.progress());
    }

    @Test
    void swappingItemsOrSlotsStartsOver() {
        AttuneClock c = new AttuneClock();
        run(c, 120, true, 1, 2);
        assertEquals(AttuneClock.Step.RUNNING, c.tick(true, 5, 2, false), "another slot");
        assertEquals(1, c.progress());
        run(c, 50, true, 5, 2);
        assertEquals(AttuneClock.Step.RUNNING, c.tick(true, 5, 4, false), "another relic");
        assertEquals(1, c.progress());
    }

    @Test
    void nothingToAttuneNeverRuns() {
        AttuneClock c = new AttuneClock();
        assertEquals(AttuneClock.Step.IDLE, run(c, 500, true, 0, -1));
        assertEquals(0, c.progress());
    }

    @Test
    void theBarFills() {
        assertEquals("░░░░", AttuneClock.bar(0, 4));
        assertEquals("██░░", AttuneClock.bar(0.5, 4));
        assertEquals("████", AttuneClock.bar(7, 4), "clamped");
        AttuneClock c = new AttuneClock();
        run(c, 100, true, 0, 0);
        assertEquals(0.5, c.fraction(), 1e-9);
    }

    @Test
    void aFourthRelicNeedsOneReleased() {
        List<String> bonds = List.of();
        for (String id : new String[]{"a", "b", "c"}) {
            assertEquals(Bonds.Verdict.OK, Bonds.check(bonds, id));
            bonds = Bonds.add(bonds, id);
        }
        assertEquals(3, bonds.size());
        assertEquals(Bonds.Verdict.FULL, Bonds.check(bonds, "d"));
        assertEquals(bonds, Bonds.add(bonds, "d"), "adding past the cap changes nothing");
        assertEquals(Bonds.Verdict.ALREADY, Bonds.check(bonds, "b"));
        bonds = Bonds.remove(bonds, "b");
        assertEquals(1, Bonds.free(bonds));
        assertEquals(Bonds.Verdict.OK, Bonds.check(bonds, "d"));
        assertEquals(List.of("a", "c", "d"), Bonds.add(bonds, "d"));
        assertEquals("a", Bonds.oldest(bonds));
        assertNull(Bonds.oldest(List.of()));
    }

    @Test
    void aLostRelicFreesItsSlotWhenPruned() {
        List<String> bonds = List.of("a", "b", "c");
        List<String> kept = Bonds.prune(bonds, Set.of("a", "c"));
        assertEquals(List.of("a", "c"), kept);
        assertEquals(Bonds.Verdict.OK, Bonds.check(kept, "d"));
        assertEquals(List.of(), Bonds.prune(bonds, Set.of()));
    }

    // ---------------------------------------------------------------- the Sentient Dungeon: movement resets the counter
    private static AttuneClock.Step runAt(AttuneClock c, int ticks, double x, double z) {
        AttuneClock.Step last = null;
        for (int i = 0; i < ticks; i++) last = c.tick(true, 2, 1, false, x, z);
        return last;
    }

    @Test
    void standingStillForTenSecondsAttunesWhereverYouStand() {
        AttuneClock c = new AttuneClock();
        assertEquals(AttuneClock.Step.RUNNING, runAt(c, AttuneClock.TICKS - 1, 123.7, -4001.2));
        assertEquals(AttuneClock.Step.DONE, c.tick(true, 2, 1, false, 123.7, -4001.2));
        assertEquals(AttuneClock.Why.NONE, c.why());
    }

    @Test
    void aQuarterOfABlockIsAllowedAndMoreIsNot() {
        AttuneClock c = new AttuneClock();
        runAt(c, 50, 10.0, 10.0);
        assertEquals(AttuneClock.Step.RUNNING, c.tick(true, 2, 1, false, 10.2, 10.0), "0.2 blocks off: still fine");
        assertEquals(AttuneClock.Step.RUNNING, c.tick(true, 2, 1, false, 10.0, 10.25), "exactly the drift: still fine");
        assertEquals(AttuneClock.Step.RUNNING, c.tick(true, 2, 1, false, 10.17, 10.17), "diagonal 0.24: still fine");
        assertEquals(52 + 1, c.progress());
        assertEquals(AttuneClock.Step.RESET, c.tick(true, 2, 1, false, 10.0, 10.26), "0.26 blocks off: starts over");
        assertEquals(AttuneClock.Why.MOVED, c.why());
        assertEquals(0, c.progress());
    }

    @Test
    void theDriftIsMeasuredFromWhereTheTenSecondsBeganNotFromTheLastTick() {
        AttuneClock c = new AttuneClock();
        c.tick(true, 2, 1, false, 0.0, 0.0);
        AttuneClock.Step hit = null;
        int at = 0;
        for (int i = 1; i <= 8 && hit == null; i++) {                                               // a slow creep: 0.1 a tick
            AttuneClock.Step s = c.tick(true, 2, 1, false, i * 0.1, 0.0);
            if (s == AttuneClock.Step.RESET) {
                hit = s;
                at = i;
            }
        }
        assertEquals(AttuneClock.Step.RESET, hit, "the creep adds up to more than the drift");
        assertEquals(3, at, "0.1 and 0.2 are within a quarter block, 0.3 is not");
        assertEquals(AttuneClock.Why.MOVED, c.why());
    }

    @Test
    void movingBeforeTheFirstTickOfARunDoesNotCount() {
        AttuneClock c = new AttuneClock();
        assertEquals(AttuneClock.Step.IDLE, c.tick(false, 2, 1, false, 0.0, 0.0), "walking about, not sneaking");
        assertEquals(AttuneClock.Step.IDLE, c.tick(false, 2, 1, false, 5.0, 5.0));
        assertEquals(AttuneClock.Step.RUNNING, c.tick(true, 2, 1, false, 9.0, 9.0), "the place they stop at is the place they keep");
        assertEquals(AttuneClock.Step.RUNNING, c.tick(true, 2, 1, false, 9.0, 9.0));
        assertEquals(2, c.progress());
    }

    @Test
    void afterAResetByMovementTheNextRunKeepsTheNewPlace() {
        AttuneClock c = new AttuneClock();
        runAt(c, 30, 0.0, 0.0);
        assertEquals(AttuneClock.Step.RESET, c.tick(true, 2, 1, false, 3.0, 0.0));
        assertEquals(0, c.progress());
        assertEquals(AttuneClock.Step.RUNNING, c.tick(true, 2, 1, false, 3.0, 0.0), "begins again where they stand now");
        assertEquals(AttuneClock.Why.NONE, c.why());
        assertEquals(AttuneClock.Step.RUNNING, runAt(c, 100, 3.1, 0.1));
        assertEquals(AttuneClock.Step.DONE, runAt(c, AttuneClock.TICKS - 101, 3.1, 0.1));
    }

    @Test
    void aKnockbackOrACurrentOrATeleportResetsIt() {
        for (double[] push : new double[][]{{1.5, 0.0}, {0.0, -1.0}, {400.0, 400.0}}) {
            AttuneClock c = new AttuneClock();
            runAt(c, 120, 0.0, 0.0);
            assertEquals(AttuneClock.Step.RESET, c.tick(true, 2, 1, false, push[0], push[1]));
            assertEquals(AttuneClock.Why.MOVED, c.why());
        }
    }

    @Test
    void theReasonOfAResetIsReported() {
        AttuneClock c = new AttuneClock();
        runAt(c, 40, 0.0, 0.0);
        assertEquals(AttuneClock.Step.RESET, c.tick(true, 2, 1, true, 0.0, 0.0));
        assertEquals(AttuneClock.Why.HURT, c.why());
        runAt(c, 40, 0.0, 0.0);
        assertEquals(AttuneClock.Step.RESET, c.tick(false, 2, 1, false, 0.0, 0.0));
        assertEquals(AttuneClock.Why.STOPPED, c.why());
        assertEquals(AttuneClock.Step.IDLE, c.tick(false, 2, 1, false, 0.0, 0.0));
        assertEquals(AttuneClock.Why.NONE, c.why(), "an idle tick has no reason");
    }

    @Test
    void theOldFourArgumentTickNeverCountsAsMoving() {
        AttuneClock c = new AttuneClock();
        assertEquals(AttuneClock.Step.DONE, run(c, AttuneClock.TICKS, true, 1, 3));
    }

    @Test
    void theDriftIsAQuarterOfABlock() { assertEquals(0.25, AttuneClock.DRIFT, 1e-12); }
}
