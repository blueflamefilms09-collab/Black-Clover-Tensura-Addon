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
}
