package com.newuniverse.nusmp.labyrinth.mega.rift;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the rift rules are where the owner's numbers live (3 to 5 floors, never past a boss, a slot is a cell, a puzzle has one order); they are checked without a game. */
class RiftRulesTest {
    @Test
    void aJumpIsThreeToFiveAndStableForASeed() {
        Set<Integer> seen = new HashSet<>();
        for (long s = 0; s < 400; s++) {
            int j = RiftRules.jumpFor(s, 3, 5);
            assertTrue(j >= 3 && j <= 5, "jump " + j);
            assertEquals(j, RiftRules.jumpFor(s, 3, 5));
            seen.add(j);
        }
        assertEquals(Set.of(3, 4, 5), seen, "all three jumps occur");
        assertEquals(4, RiftRules.jumpFor(9, 4, 4), "a narrowed range is honoured");
    }

    @Test
    void aShortcutNeverGoesPastABossFloor() {
        java.util.function.IntPredicate boss = c -> (c + 1) % 10 == 0;       // chain index 9 is floor 10: a boss floor
        assertEquals(8, RiftRules.shortcutTarget(3, 5, boss));
        assertEquals(9, RiftRules.shortcutTarget(6, 5, boss), "held at the boss floor");
        assertEquals(9, RiftRules.shortcutTarget(9, 5, boss), "on a boss floor there is nowhere to jump");
        assertEquals(11, RiftRules.shortcutTarget(10, 1, c -> false));
        assertTrue(RiftRules.shortcutTarget(4, 3, boss) > 4);
    }

    @Test
    void rollsFollowTheChance() {
        assertFalse(RiftRules.rolls(0, 0));
        assertTrue(RiftRules.rolls(1, 0.99));
        assertTrue(RiftRules.rolls(0.25, 0.1));
        assertFalse(RiftRules.rolls(0.25, 0.5));
    }

    @Test
    void aRiftGrowsHoldsAndGutters() {
        assertEquals(0f, RiftRules.openness(0, 1000), 1e-6);
        assertEquals(1f, RiftRules.openness(RiftRules.GROW_TICKS, 1000), 1e-6);
        assertTrue(RiftRules.openness(500, 20) < 0.3f, "guttering near its end");
        assertEquals(0f, RiftRules.openness(500, 0), 1e-6);
    }

    @Test
    void pocketCellsDoNotOverlapAndAreFoundByPosition() {
        for (int s = 0; s < 5; s++) {
            assertEquals(s, RiftRules.slotAt(RiftRules.pocketX(s) + 10, RiftRules.POCKET_Z + 5));
            assertEquals(s, RiftRules.slotAt(RiftRules.pocketX(s) - 90, RiftRules.POCKET_Z));
        }
        assertEquals(-1, RiftRules.slotAt(0, 0));
        assertEquals(-1, RiftRules.slotAt(RiftRules.pocketX(0), RiftRules.POCKET_Z + 500));
        assertEquals(-1, RiftRules.slotAt(RiftRules.pocketX(RiftRules.MAX_SLOTS) + 10, RiftRules.POCKET_Z));
    }

    @Test
    void variantsFollowTheShareAndAPuzzleHasOneOrder() {
        int boss = 0;
        for (long s = 0; s < 2000; s++) if (RiftRules.variantFor(s, 0.5) == RiftRules.Variant.BOSS) boss++;
        assertTrue(boss > 800 && boss < 1200, "about half are boss pockets: " + boss);
        for (long s = 0; s < 100; s++) {
            int[] o = RiftRules.puzzleOrder(s);
            int[] sorted = o.clone();
            Arrays.sort(sorted);
            assertTrue(Arrays.equals(new int[]{0, 1, 2, 3}, sorted), "a permutation");
            assertTrue(Arrays.equals(o, RiftRules.puzzleOrder(s)), "the same order for the same seed");
            RiftRules.Puzzle p = new RiftRules.Puzzle(o);
            assertEquals(RiftRules.Press.PROGRESS, p.press(o[0]));
            assertEquals(RiftRules.Press.WRONG, p.press(o[1] == 0 ? 1 : 0));
            assertEquals(0, p.done(), "a wrong plate resets");
            for (int i = 0; i < 3; i++) assertEquals(RiftRules.Press.PROGRESS, p.press(o[i]));
            assertEquals(RiftRules.Press.SOLVED, p.press(o[3]));
            assertTrue(p.solved());
        }
    }

    @Test
    void collapseWarningsComeOnceEach() {
        assertEquals(60, RiftRules.warnSeconds(60 * 20, 60 * 20 + 1));
        assertEquals(0, RiftRules.warnSeconds(59 * 20, 60 * 20));
        assertEquals(10, RiftRules.warnSeconds(10 * 20 - 3, 10 * 20 + 2));
        assertEquals(0, RiftRules.warnSeconds(0, 0));
    }
}
