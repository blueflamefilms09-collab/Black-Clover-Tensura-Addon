package com.newuniverse.nusmp.entity.veldanava.pocket;

import com.newuniverse.nusmp.entity.veldanava.skills.IsrafilRules;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (docs/veldanava_skills.md, Dimensional Labyrinth): the fairness of the ever-shifting maze is a rule of numbers, so it is pinned here without a game: a generated maze is perfect (one piece,
 * one way between two cells), a shift keeps it perfect however many times it runs, a wall that may not close is never closed, the blocks agree with the walls, and the numbers of IsrafilRules are in range.
 */
class PocketMazeTest {
    @Test
    void aGeneratedMazeIsPerfectForEverySizeAndSeed() {
        for (int n : new int[]{2, 3, 6, 8, 12}) {
            for (long seed = 0; seed < 40; seed++) {
                PocketMaze m = PocketMaze.generate(n, seed);
                assertTrue(m.connected(), "n=" + n + " seed=" + seed);
                assertEquals(n * n - 1, m.openCount(), "a perfect maze has cells - 1 open walls");
                assertTrue(m.perfect());
            }
        }
    }

    @Test
    void theSameSeedMakesTheSameMazeAndAnotherSeedUsuallyAnotherOne() {
        PocketMaze a = PocketMaze.generate(8, 7), b = PocketMaze.generate(8, 7);
        for (int e = 0; e < a.edgeCount(); e++) assertEquals(a.isOpen(e), b.isOpen(e));
        int differ = 0;
        for (long seed = 100; seed < 120; seed++) {
            PocketMaze c = PocketMaze.generate(8, seed);
            for (int e = 0; e < a.edgeCount(); e++) if (a.isOpen(e) != c.isOpen(e)) { differ++; break; }
        }
        assertTrue(differ >= 18, "different seeds make different mazes");
    }

    @Test
    void aShiftOpensOneWallClosesOneWallAndKeepsTheMazePerfect() {
        for (int n : new int[]{9, 11}) {
            PocketMaze m = PocketMaze.generate(n, 11);
            Random rnd = new Random(5);
            Set<Integer> everOpened = new HashSet<>();
            for (int i = 0; i < 2000; i++) {
                PocketMaze.Change c = m.plan(rnd, e -> true);
                assertNotNull(c, "with nothing forbidden a shift is always found");
                assertFalse(m.isOpen(c.opened()));
                assertTrue(m.isOpen(c.closed()));
                assertNotEquals(c.opened(), c.closed());
                m.apply(c);
                everOpened.add(c.opened());
                assertTrue(m.perfect(), "shift " + i);
            }
            assertTrue(everOpened.size() > n * n / 2, "the walls really move all over the maze");
        }
    }

    @Test
    void aWallThatMayNotCloseIsNeverClosed() {
        PocketMaze m = PocketMaze.generate(6, 3);
        Random rnd = new Random(9);
        Set<Integer> held = new HashSet<>();
        for (int e = 0; e < m.edgeCount(); e++) if (m.isOpen(e) && e % 3 == 0) held.add(e);
        for (int i = 0; i < 500; i++) {
            PocketMaze.Change c = m.plan(rnd, e -> !held.contains(e));
            if (c == null) continue;
            assertFalse(held.contains(c.closed()));
            m.apply(c);
            held.removeIf(e -> !m.isOpen(e));
            assertTrue(m.perfect());
        }
    }

    @Test
    void whenNothingMayCloseThereIsNoShiftAndTheMazeIsUntouched() {
        PocketMaze m = PocketMaze.generate(6, 3);
        boolean[] before = new boolean[m.edgeCount()];
        for (int e = 0; e < before.length; e++) before[e] = m.isOpen(e);
        assertNull(m.plan(new Random(1), e -> false));
        for (int e = 0; e < before.length; e++) assertEquals(before[e], m.isOpen(e));
    }

    @Test
    void aStaleChangeIsRefused() {
        PocketMaze m = PocketMaze.generate(6, 3);
        PocketMaze.Change c = m.plan(new Random(2), e -> true);
        m.apply(c);
        assertThrows(IllegalStateException.class, () -> m.apply(c));
    }

    @Test
    void distancesAndTheFarthestCell() {
        PocketMaze m = PocketMaze.generate(8, 21);
        int far = m.farthest(0);
        int[] d = m.distances(0);
        assertEquals(0, d[0]);
        for (int v : d) assertTrue(v >= 0 && v <= d[far]);
        assertTrue(d[far] >= 8, "the exit is a real walk away");
    }

    @Test
    void theBlocksAgreeWithTheWalls() {
        PocketMaze m = PocketMaze.generate(6, 4);
        int size = m.size();
        assertEquals(6 * PocketMaze.PITCH + 1, size);
        for (int i = 0; i < size; i++) {
            assertTrue(m.solid(0, i) && m.solid(size - 1, i) && m.solid(i, 0) && m.solid(i, size - 1), "the outer wall is shut");
        }
        for (int c = 0; c <= 6; c++) for (int r = 0; r <= 6; r++) assertTrue(m.solid(c * 4, r * 4), "pillar " + c + "," + r);
        for (int c = 0; c < 6; c++) for (int r = 0; r < 6; r++) {
            for (int x = 1; x <= 3; x++) for (int z = 1; z <= 3; z++) assertFalse(m.solid(c * 4 + x, r * 4 + z), "cell floor is open");
        }
        for (int e = 0; e < m.edgeCount(); e++) {
            if (!m.valid(e)) continue;
            int[] g = m.gap(e);
            assertEquals(3, (g[2] - g[0] + 1) * (g[3] - g[1] + 1), "a wall is three columns");
            for (int x = g[0]; x <= g[2]; x++) for (int z = g[1]; z <= g[3]; z++) assertEquals(!m.isOpen(e), m.solid(x, z), "edge " + e + " at " + x + "," + z);
        }
    }

    @Test
    void cellsAreFoundFromBlockCoordinates() {
        PocketMaze m = PocketMaze.generate(6, 4);
        for (int cell = 0; cell < m.cellCount(); cell++) {
            double[] c = m.centre(cell);
            assertEquals(cell, m.cellAt(c[0], c[1]));
        }
        assertEquals(-1, m.cellAt(-0.5, 3));
        assertEquals(-1, m.cellAt(3, 6 * 4 + 2));
    }

    @Test
    void theNumbersOfTheSkillAreInRange() {
        assertTrue(IsrafilRules.pocketCells(true) > IsrafilRules.pocketCells(false));
        assertTrue(IsrafilRules.pocketCells(true) * PocketMaze.PITCH + 1 <= 45, "the biggest maze fits nine chunks and a room");
        assertTrue(IsrafilRules.pocketShiftTicks(true) < IsrafilRules.pocketShiftTicks(false));
        assertTrue(IsrafilRules.POCKET_WARN_TICKS < IsrafilRules.pocketShiftTicks(true), "a shift is applied before the next one is planned");
        assertTrue(IsrafilRules.pocketTicks(true, false) < IsrafilRules.pocketTicks(false, false));
        assertTrue(IsrafilRules.pocketTicks(true, true) > IsrafilRules.pocketTicks(true, false));
        assertEquals(1, IsrafilRules.pocketMembers(false));
        assertEquals(3, IsrafilRules.pocketMembers(true));
        assertTrue(IsrafilRules.POCKET_SLOTS * 128 + 16 + 45 < 30_000_000, "slots stay far inside the world border");
    }
}
