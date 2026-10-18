package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the pity rule is "never drop a book of a line the player already owns twice in a row". It is small and it is the kind of rule that is easy to get subtly wrong (the memory kept for the
 * wrong player, never cleared, cleared too early, or an infinite loop for a player who owns everything), so each edge is pinned: the memory is per player, an owned drop turns it on and an
 * unowned one off, only the draw AFTER an owned drop avoids owned lines, a player who owns every line still gets an answer, a drop with no player reads and writes nothing, forgetting a player
 * and clearing work; and a long simulation over 283 lines with 60 owned (drawn with the real weights) never produces two owned lines in a row, while the owned lines are still drawn at about their
 * natural rate (the rule only forbids a repeat, it does not remove the owned lines).
 */
class BookPityTest {
    private static Dice fixed(double v) { return () -> v; }

    @Test
    void anOwnedDropTurnsTheMemoryOnAndAnUnownedOneOff() {
        BookPity p = new BookPity();
        UUID a = UUID.randomUUID();
        assertFalse(p.avoidOwned(a));
        p.record(a, true);
        assertTrue(p.avoidOwned(a));
        p.record(a, false);
        assertFalse(p.avoidOwned(a));
        assertEquals(0, p.size());
    }

    @Test
    void theMemoryIsPerPlayer() {
        BookPity p = new BookPity();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        p.record(a, true);
        assertTrue(p.avoidOwned(a));
        assertFalse(p.avoidOwned(b));
        p.forget(a);
        assertFalse(p.avoidOwned(a));
        p.record(b, true);
        p.clear();
        assertFalse(p.avoidOwned(b));
        assertFalse(p.avoidOwned(null));
        p.record(null, true);
        p.forget(null);
        assertEquals(0, p.size());
    }

    @Test
    void onlyTheDrawAfterAnOwnedDropAvoidsOwnedLines() {
        BookPity p = new BookPity();
        UUID a = UUID.randomUUID();
        WeightedTable t = WeightedTable.of(new int[] {1, 1, 1, 1});
        Set<Integer> owned = Set.of(0, 1);
        // a first draw that lands on an owned line (dice 0.1 -> index 0) is allowed: no memory yet
        assertEquals(0, p.pick(t, fixed(0.1), a, owned::contains, 12));
        assertTrue(p.avoidOwned(a), "the owned drop is remembered");
        // the next draw, whatever the dice says, must not be owned
        for (double d : new double[] {0.0, 0.1, 0.3, 0.49, 0.5, 0.74, 0.99}) {
            int got = p.pick(t, fixed(d), a, owned::contains, 12);
            assertFalse(owned.contains(got), "dice " + d + " handed an owned line right after an owned one: " + got);
            assertFalse(p.avoidOwned(a), "an unowned drop clears the memory");
            p.record(a, true);                                              // pretend the player got an owned one again, to test the next dice value
        }
    }

    @Test
    void aPlayerWhoOwnsEveryLineStillGetsAnAnswer() {
        BookPity p = new BookPity();
        UUID a = UUID.randomUUID();
        WeightedTable t = WeightedTable.of(new int[] {1, 2, 3});
        p.record(a, true);
        for (int i = 0; i < 50; i++) assertTrue(p.pick(t, new Random(i)::nextDouble, a, k -> true, 12) >= 0);
        assertEquals(-1, p.pick(WeightedTable.EMPTY, fixed(0.5), a, k -> true, 12), "nothing to draw: -1, and no memory written for it");
    }

    @Test
    void aDropWithNoPlayerTouchesNoMemory() {
        BookPity p = new BookPity();
        WeightedTable t = WeightedTable.of(new int[] {1, 1});
        assertEquals(0, p.pick(t, fixed(0.1), null, k -> true, 12));
        assertEquals(0, p.size());
        UUID a = UUID.randomUUID();
        assertEquals(1, p.pick(t, fixed(0.9), a, null, 12), "no ownership test: the plain pick");
        assertEquals(0, p.size());
    }

    @Test
    void neverTwoOwnedInARowOver283LinesAndTheOwnedOnesStillDrop() {
        int[] w = new int[283];
        for (int i = 0; i < w.length; i++) w[i] = i < 8 ? new int[] {3, 2, 2, 2, 2, 2, 1, 2}[i] : 1;
        WeightedTable t = WeightedTable.of(w);
        Set<Integer> owned = new HashSet<>();
        for (int i = 0; i < 60; i++) owned.add(i * 4);                          // 60 owned lines, including the heavy ones
        BookPity p = new BookPity();
        UUID a = UUID.randomUUID();
        Random r = new Random(2026);
        boolean previous = false;
        int ownedDraws = 0, n = 200_000;
        for (int i = 0; i < n; i++) {
            int got = p.pick(t, r::nextDouble, a, owned::contains, 12);
            boolean o = owned.contains(got);
            assertFalse(o && previous, "two owned lines in a row at draw " + i);
            if (o) ownedDraws++;
            previous = o;
        }
        // without the rule about 60/283 weighted by the heavy owned lines are owned (about 21%); with it an owned draw can only follow an unowned one, so roughly 17% or a little less, never 0
        assertTrue(ownedDraws > n * 0.08 && ownedDraws < n * 0.22, "owned lines drawn " + ownedDraws + " of " + n);
    }
}
