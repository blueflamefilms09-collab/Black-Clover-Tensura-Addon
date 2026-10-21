package com.newuniverse.nusmp.labyrinth.trial;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a trial hall's doorways hold three Ward Runes each in the inner wall layer and the Sealed Archway fills a column of at most four barrier cells over every rune. In a sector cut
 * to 7 x 7 the doorway is seven wide and seven high through both wall layers, so the columns must be widened to seven, doubled into the outer layer and allowed to rise seven cells;
 * in a hall that was not cut nothing may change. These tests pin that on the layout the trial templates write (three runes at lateral offsets -1, 0, 1 of the doorway centre, depth 1).
 */
class ArchwayDoorwaysTest {
    private static List<int[]> doorway(boolean alongX, int centre, int fixed) {
        List<int[]> out = new ArrayList<>();
        for (int lat = centre - 1; lat <= centre + 1; lat++) out.add(alongX ? new int[] {lat, 8, fixed} : new int[] {fixed, 8, lat});
        return out;
    }

    private static Set<ArchwayDoorways.Col> set(List<ArchwayDoorways.Col> l) { return new HashSet<>(l); }

    @Test
    void anOldHallKeepsExactlyItsRunesInOrder() {
        List<int[]> runes = doorway(true, 13, 1);
        List<ArchwayDoorways.Col> cols = ArchwayDoorways.columns(runes, 13, 13, false);
        assertEquals(3, cols.size());
        for (int i = 0; i < 3; i++) assertEquals(new ArchwayDoorways.Col(runes.get(i)[0], 8, 1), cols.get(i));
        assertEquals(4, ArchwayDoorways.columnCap(false));
    }

    @Test
    void aCutDoorwayInTheNorthWallIsSevenWideInBothLayers() {
        List<ArchwayDoorways.Col> cols = ArchwayDoorways.columns(doorway(true, 13, 1), 13, 13, true);
        assertEquals(14, cols.size());
        for (int x = 10; x <= 16; x++) {
            assertTrue(cols.contains(new ArchwayDoorways.Col(x, 8, 1)), "inner " + x);
            assertTrue(cols.contains(new ArchwayDoorways.Col(x, 8, 0)), "outer " + x);
        }
        assertEquals(7, ArchwayDoorways.columnCap(true));
    }

    @Test
    void theOuterLayerLiesAwayFromTheArchwayOnEverySide() {
        // south wall: inner layer at z 25, outer at z 26
        List<ArchwayDoorways.Col> south = ArchwayDoorways.columns(doorway(true, 13, 25), 13, 13, true);
        assertTrue(south.contains(new ArchwayDoorways.Col(13, 8, 26)));
        assertFalse(south.contains(new ArchwayDoorways.Col(13, 8, 24)));
        // west wall: runes along z at x 1, outer at x 0
        List<ArchwayDoorways.Col> west = ArchwayDoorways.columns(doorway(false, 13, 1), 13, 13, true);
        assertEquals(14, west.size());
        assertTrue(west.contains(new ArchwayDoorways.Col(0, 8, 10)));
        assertTrue(west.contains(new ArchwayDoorways.Col(1, 8, 16)));
        // east wall at x 25, outer at x 26
        List<ArchwayDoorways.Col> east = ArchwayDoorways.columns(doorway(false, 13, 25), 13, 13, true);
        assertTrue(east.contains(new ArchwayDoorways.Col(26, 8, 13)));
        assertFalse(east.contains(new ArchwayDoorways.Col(24, 8, 13)));
    }

    @Test
    void severalDoorwaysAreWidenedEachOnItsOwn() {
        List<int[]> runes = new ArrayList<>();
        runes.addAll(doorway(true, 4, 1));
        runes.addAll(doorway(true, 22, 1));
        runes.addAll(doorway(false, 13, 25));
        List<ArchwayDoorways.Col> cols = ArchwayDoorways.columns(runes, 13, 13, true);
        assertEquals(42, cols.size());
        assertEquals(cols.size(), set(cols).size(), "no column twice");
        assertTrue(cols.contains(new ArchwayDoorways.Col(1, 8, 1)), "the first doorway reaches x 1");
        assertTrue(cols.contains(new ArchwayDoorways.Col(25, 8, 1)), "the last doorway reaches x 25");
    }

    @Test
    void aLoneRuneOrABrokenLineIsLeftAsTheTemplateDrewIt() {
        assertEquals(1, ArchwayDoorways.columns(List.of(new int[] {5, 8, 5}), 13, 13, true).size());
        List<int[]> bent = List.of(new int[] {5, 8, 5}, new int[] {6, 8, 5}, new int[] {6, 8, 6});
        assertEquals(3, ArchwayDoorways.columns(bent, 13, 13, true).size(), "an L of runes has no single wall direction");
        List<int[]> wide = new ArrayList<>();
        for (int x = 5; x <= 11; x++) wide.add(new int[] {x, 8, 1});
        assertEquals(7, ArchwayDoorways.columns(wide, 13, 13, true).size(), "a line already seven wide is not widened");
    }

    @Test
    void runesOnDifferentFloorRowsAreDifferentLines() {
        List<int[]> runes = new ArrayList<>(doorway(true, 13, 1));
        runes.add(new int[] {14, 12, 1});                                                  // a lone rune on a gallery above
        List<ArchwayDoorways.Col> cols = ArchwayDoorways.columns(runes, 13, 13, true);
        assertTrue(cols.contains(new ArchwayDoorways.Col(14, 12, 1)));
        assertFalse(cols.contains(new ArchwayDoorways.Col(15, 12, 1)), "the gallery rune does not widen");
    }

    @Test
    void theColumnCapReachesTheTrialMathHeight() {
        assertEquals(7, TrialMath.columnHeight(9, ArchwayDoorways.columnCap(true)));
        assertEquals(4, TrialMath.columnHeight(9, ArchwayDoorways.columnCap(false)));
        assertEquals(4, TrialMath.columnHeight(9), "the one-argument form is unchanged");
        assertEquals(0, TrialMath.columnHeight(-2, 7));
    }

    @Test
    void theBuiltInCheckOfTheSelfTestHolds() {
        assertEquals(List.of(), ArchwayDoorways.selfCheck());
    }
}
