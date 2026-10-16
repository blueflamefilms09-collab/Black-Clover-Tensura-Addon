package com.newuniverse.nusmp.butoh;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The dance sequence: no move twice in a row, the same seed gives the same dance on every side, every step lasts 12 to 30 ticks. */
class DanceChoreographerTest {
    @Test
    void noMoveEverRepeatsRightAfterItself() {
        for (long seed = -50; seed < 400; seed++) {
            DanceChoreographer.Cursor c = new DanceChoreographer.Cursor(DanceChoreographer.seedFor(seed, ~seed, seed * 7));
            DanceMove prev = null;
            int t = 0;
            for (int i = 0; i < 600; i++) {
                DanceChoreographer.Step s = c.at(t);
                assertEquals(i, s.index());
                if (prev != null) assertTrue(prev != s.move(), "seed " + seed + " step " + i);
                prev = s.move();
                t = s.endTick();
            }
        }
    }

    @Test
    void sameSeedSameDanceOnServerAndClient() {
        long seed = DanceChoreographer.seedFor(123456789L, 987654321L, 40_000L);
        DanceChoreographer.Cursor server = new DanceChoreographer.Cursor(seed);
        DanceChoreographer.Cursor client = new DanceChoreographer.Cursor(seed);
        for (int tick = 0; tick < 5000; tick++) assertEquals(server.at(tick), client.at(tick), "tick " + tick);
        // a client that joins late and asks for a far tick first, then the near ones, still sees the server's dance
        DanceChoreographer.Cursor late = new DanceChoreographer.Cursor(seed);
        assertEquals(server.at(4321), late.at(4321));
        assertEquals(server.at(10), late.at(10), "asking backwards starts over and stays correct");
    }

    @Test
    void differentSeedsGiveDifferentDances() {
        DanceChoreographer.Cursor a = new DanceChoreographer.Cursor(DanceChoreographer.seedFor(1, 2, 100));
        DanceChoreographer.Cursor b = new DanceChoreographer.Cursor(DanceChoreographer.seedFor(1, 2, 101));
        int same = 0;
        for (int t = 0; t < 2000; t++) if (a.at(t).equals(b.at(t))) same++;
        assertTrue(same < 400, "two dances that start a tick apart must not be the same dance (" + same + " equal ticks of 2000)");
    }

    @Test
    void everyStepLastsTwelveToThirtyTicksAndStepsAreContiguous() {
        DanceChoreographer.Cursor c = new DanceChoreographer.Cursor(DanceChoreographer.seedFor(9, 9, 9));
        DanceChoreographer.Step prev = c.at(0);
        assertEquals(0, prev.startTick());
        int min = 99, max = 0;
        for (int i = 0; i < 3000; i++) {
            assertTrue(prev.length() >= DanceChoreographer.MIN_TICKS && prev.length() <= DanceChoreographer.MAX_TICKS, "length " + prev.length());
            min = Math.min(min, prev.length());
            max = Math.max(max, prev.length());
            DanceChoreographer.Step next = c.at(prev.endTick());
            assertEquals(prev.endTick(), next.startTick());
            assertEquals(prev.index() + 1, next.index());
            prev = next;
        }
        assertEquals(DanceChoreographer.MIN_TICKS, min, "the shortest move is reached");
        assertEquals(DanceChoreographer.MAX_TICKS, max, "the longest move is reached");
    }

    @Test
    void everyTickInsideAStepReturnsThatStep() {
        DanceChoreographer.Cursor c = new DanceChoreographer.Cursor(77L);
        DanceChoreographer.Step s = c.at(100);
        for (int t = s.startTick(); t < s.endTick(); t++) assertEquals(s, c.at(t));
        assertEquals(c.at(0), c.at(-5), "a negative tick is the first step");
    }

    @Test
    void theWholePoolIsUsed() {
        Set<DanceMove> seen = EnumSet.noneOf(DanceMove.class);
        DanceChoreographer.Cursor c = new DanceChoreographer.Cursor(DanceChoreographer.seedFor(5, 6, 7));
        for (int t = 0; t < 4000; t++) seen.add(c.at(t).move());
        assertEquals(DanceMove.count(), seen.size());
        assertTrue(DanceMove.count() >= 6 && DanceMove.count() <= 8, "six to eight moves");
    }

    @Test
    void everyMoveHasItsOwnDanceClipName() {
        Set<String> names = new java.util.HashSet<>();
        for (DanceMove m : DanceMove.values()) {
            assertTrue(m.clip.startsWith("dance_"), m.clip);
            assertTrue(names.add(m.clip), "duplicate clip " + m.clip);
        }
    }
}
