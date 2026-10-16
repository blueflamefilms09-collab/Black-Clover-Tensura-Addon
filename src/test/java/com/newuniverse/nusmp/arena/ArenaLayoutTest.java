package com.newuniverse.nusmp.arena;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The courtyard: closed, solid, with the dais, the pillars, the seats and the four sigils where they should be. */
class ArenaLayoutTest {
    private static boolean has(List<ArenaLayout.Cell> col, ArenaLayout.Part part) {
        for (ArenaLayout.Cell c : col) if (c.part() == part) return true;
        return false;
    }

    private static ArenaLayout.Cell at(List<ArenaLayout.Cell> col, int dy) {
        for (ArenaLayout.Cell c : col) if (c.dy() == dy) return c;
        return null;
    }

    @Test
    void nothingStandsOutsideTheCourtyard() {
        assertTrue(ArenaLayout.column(43, 0).isEmpty());
        assertTrue(ArenaLayout.column(31, 31).isEmpty());
        assertTrue(ArenaLayout.column(-60, 5).isEmpty());
    }

    @Test
    void everyColumnInsideHasTwoLayersOfFoundationAndAFloor() {
        for (int dx = -45; dx <= 45; dx++)
            for (int dz = -45; dz <= 45; dz++) {
                List<ArenaLayout.Cell> col = ArenaLayout.column(dx, dz);
                assertEquals(ArenaLayout.inside(dx, dz), !col.isEmpty(), dx + "," + dz);
                if (col.isEmpty()) continue;
                assertEquals(ArenaLayout.Part.FOUNDATION, at(col, -2).part());
                assertEquals(ArenaLayout.Part.FOUNDATION, at(col, -1).part());
                assertNotNull(at(col, 0), "a floor block at " + dx + "," + dz);
                for (ArenaLayout.Cell c : col) assertTrue(c.dy() >= ArenaLayout.MIN_DY && c.dy() <= ArenaLayout.MAX_DY + 2, c + "");
            }
    }

    @Test
    void noColumnHasTwoBlocksAtTheSameHeight() {
        for (int dx = -42; dx <= 42; dx++)
            for (int dz = -42; dz <= 42; dz++) {
                java.util.Set<Integer> seen = new java.util.HashSet<>();
                for (ArenaLayout.Cell c : ArenaLayout.column(dx, dz)) assertTrue(seen.add(c.dy()), "duplicate dy at " + dx + "," + dz);
            }
    }

    @Test
    void theDaisIsOneBlockHigherThanTheFloorAndHasARuneInTheMiddle() {
        assertEquals(ArenaLayout.Part.DAIS_RUNE, at(ArenaLayout.column(0, 0), 1).part());
        assertEquals(1, ArenaLayout.surface(0, 0));
        assertEquals(1, ArenaLayout.surface(3, 0));
        assertEquals(ArenaLayout.Part.DAIS_EDGE, at(ArenaLayout.column(5, 0), 1).part());
        assertEquals(0, ArenaLayout.surface(8, 0), "the floor outside the dais");
    }

    @Test
    void theWallClosesTheEdgeWithNoGap() {
        for (int dx = -42; dx <= 42; dx++)
            for (int dz = -42; dz <= 42; dz++) {
                double r = ArenaLayout.radius(dx, dz);
                if (r > 41.0 && r <= 42.5) assertTrue(has(ArenaLayout.column(dx, dz), ArenaLayout.Part.WALL), "gap at " + dx + "," + dz);
                if (r < 40.4) assertFalse(has(ArenaLayout.column(dx, dz), ArenaLayout.Part.WALL), "wall inside at " + dx + "," + dz);
            }
    }

    @Test
    void theWallIsFiveHighWithMerlonsAndSomeLanterns() {
        int merlons = 0, lanterns = 0, columns = 0;
        for (int dx = -42; dx <= 42; dx++)
            for (int dz = -42; dz <= 42; dz++) {
                List<ArenaLayout.Cell> col = ArenaLayout.column(dx, dz);
                if (!has(col, ArenaLayout.Part.WALL)) continue;
                columns++;
                for (int dy = 1; dy <= ArenaLayout.WALL_HEIGHT; dy++) assertEquals(ArenaLayout.Part.WALL, at(col, dy).part());
                if (has(col, ArenaLayout.Part.MERLON)) merlons++;
                if (has(col, ArenaLayout.Part.LANTERN)) lanterns++;
            }
        assertTrue(merlons > columns / 4 && merlons < columns, "merlons alternate with gaps: " + merlons + "/" + columns);
        assertTrue(lanterns >= 4, "light on the wall: " + lanterns);
    }

    @Test
    void fourRunePillarsOnTheDiagonalsNineHighWithALightOnTop() {
        int lights = 0;
        for (int dx = -42; dx <= 42; dx++)
            for (int dz = -42; dz <= 42; dz++) {
                List<ArenaLayout.Cell> col = ArenaLayout.column(dx, dz);
                if (has(col, ArenaLayout.Part.PILLAR_LIGHT)) {
                    lights++;
                    assertEquals(20, Math.abs(dx));
                    assertEquals(20, Math.abs(dz));
                }
                if (ArenaLayout.pillarAt(dx, dz) >= 0) for (int dy = 1; dy <= ArenaLayout.PILLAR_HEIGHT; dy++) assertNotNull(at(col, dy));
            }
        assertEquals(4, lights);
        assertEquals(-1, ArenaLayout.pillarAt(0, 0), "the centre is the dais, not a pillar");
    }

    @Test
    void theSpectatorSeatIsARaisedRingBetweenTheDaisAndTheWall() {
        assertEquals(1, ArenaLayout.surface(34, 0), "inner half: one block up");
        assertEquals(2, ArenaLayout.surface(36, 0), "outer half: two blocks up");
        assertEquals(0, ArenaLayout.surface(30, 0));
        assertEquals(0, ArenaLayout.surface(38, 1), "past the seat");
    }

    @Test
    void fourReturnSigilsAtTheCompassPoints() {
        int n = 0;
        for (int dx = -42; dx <= 42; dx++)
            for (int dz = -42; dz <= 42; dz++) if (has(ArenaLayout.column(dx, dz), ArenaLayout.Part.SIGIL)) n++;
        assertEquals(4, n);
        for (int[] s : ArenaLayout.sigils()) assertTrue(has(ArenaLayout.column(s[0], s[1]), ArenaLayout.Part.SIGIL));
    }

    @Test
    void arrivalSpotsAreOnTheFloorNextToTheDaisAndInsideTheCourtyardWithRoomAbove() {
        for (int n : new int[]{1, 2, 4, 8, 16, 99}) {
            int[][] spots = ArenaLayout.arrivalSpots(n);
            assertEquals(Math.min(16, n), spots.length);
            for (int[] s : spots) {
                List<ArenaLayout.Cell> col = ArenaLayout.column(s[0], s[1]);
                assertEquals(0, ArenaLayout.surface(s[0], s[1]), "plain floor");
                assertNull(at(col, ArenaLayout.ARRIVAL_DY), "free space at the arrival height");
                assertNull(at(col, ArenaLayout.ARRIVAL_DY + 1));
                assertTrue(ArenaLayout.radius(s[0], s[1]) > ArenaLayout.DAIS_RADIUS + 0.5);
            }
        }
        assertEquals(3, ArenaLayout.ARRIVAL_DY, "two blocks above the dais top");
    }
}
