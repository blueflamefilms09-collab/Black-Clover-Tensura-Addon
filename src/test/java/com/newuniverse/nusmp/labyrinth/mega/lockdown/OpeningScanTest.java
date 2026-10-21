package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a room can only be locked where it has a wall. These tests build the wall of a room as the dungeon makes it (a plane of cells with doorways cut in it) and pin that
 * a doorway of the old size (3 wide, 4 high) and of the clearance rule (7 by 7) are both one opening each, that four doors are four openings, that a cavern's endless gap
 * is refused as an open-plan room instead of sealing a hillside, that a wall with no doorway has nothing to close, and that the grouping does not depend on the order the
 * cells were found in.
 */
class OpeningScanTest {
    /** The cells of a doorway of w by h in the plane z = {@code z}, from (x0, y0). */
    private static List<OpeningScan.Cell> door(int x0, int y0, int z, int w, int h) {
        List<OpeningScan.Cell> out = new ArrayList<>();
        for (int x = x0; x < x0 + w; x++) for (int y = y0; y < y0 + h; y++) out.add(new OpeningScan.Cell(x, y, z));
        return out;
    }

    @Test
    void oneDoorwayIsOneOpeningWhateverItsSize() {
        for (int[] size : new int[][] {{3, 4}, {7, 7}, {5, 5}}) {
            List<OpeningScan.Cell> d = door(10, 65, 1, size[0], size[1]);
            OpeningScan.Result r = OpeningScan.judge(d, OpeningScan.Limits.DEFAULT);
            assertTrue(r.ok(), size[0] + "x" + size[1] + ": " + r.why());
            assertEquals(1, r.openings());
            assertEquals(size[0] * size[1], r.cells());
        }
    }

    @Test
    void fourDoorsOfTheClearanceSizeStayWithinTheLimits() {
        List<OpeningScan.Cell> all = new ArrayList<>();
        all.addAll(door(10, 65, 1, 7, 7));
        all.addAll(door(10, 65, 40, 7, 7));
        for (OpeningScan.Cell c : door(10, 65, 1, 7, 7)) all.add(new OpeningScan.Cell(1, c.y(), c.x() + 30));
        for (OpeningScan.Cell c : door(10, 65, 1, 7, 7)) all.add(new OpeningScan.Cell(40, c.y(), c.x() + 30));
        OpeningScan.Result r = OpeningScan.judge(all, OpeningScan.Limits.DEFAULT);
        assertTrue(r.ok(), r.why());
        assertEquals(4, r.openings());
        assertEquals(4 * 49, r.cells());
    }

    @Test
    void twoDoorsAdjacentOnTheSameWallAreSeparateWhenAPillarSeparatesThem() {
        List<OpeningScan.Cell> all = new ArrayList<>(door(10, 65, 1, 3, 4));
        all.addAll(door(14, 65, 1, 3, 4));                                              // x 13 is solid between them
        assertEquals(2, OpeningScan.openings(all).size());
        all.addAll(door(13, 65, 1, 1, 4));                                              // the pillar is gone: it is one wide opening
        assertEquals(1, OpeningScan.openings(all).size());
    }

    @Test
    void anArchWithAGapAboveItsKeystoneIsStillOneOpening() {
        List<OpeningScan.Cell> d = door(10, 65, 1, 3, 4);
        d.add(new OpeningScan.Cell(11, 69, 1));                                         // the cell above the middle
        assertEquals(1, OpeningScan.openings(d).size());
    }

    @Test
    void aWallWithNoDoorwayHasNothingToClose() {
        OpeningScan.Result r = OpeningScan.judge(List.of(), OpeningScan.Limits.DEFAULT);
        assertFalse(r.ok());
        assertEquals(0, r.openings());
        assertTrue(r.why().contains("nothing to close"), r.why());
    }

    @Test
    void anOpenPlanRoomIsRefusedNotSealed() {
        // a whole wall missing: 40 wide, 9 high
        OpeningScan.Result wall = OpeningScan.judge(door(0, 65, 1, 40, 9), OpeningScan.Limits.DEFAULT);
        assertFalse(wall.ok());
        assertTrue(wall.why().contains("not a doorway"), wall.why());
        // many small gaps: a ruined hall
        List<OpeningScan.Cell> gaps = new ArrayList<>();
        for (int i = 0; i < 20; i++) gaps.addAll(door(i * 5, 65, 1, 2, 2));
        OpeningScan.Result ruin = OpeningScan.judge(gaps, OpeningScan.Limits.DEFAULT);
        assertFalse(ruin.ok());
        assertTrue(ruin.why().contains("open-plan"), ruin.why());
        // a few big ones adding up to too much
        List<OpeningScan.Cell> sum = new ArrayList<>();
        for (int i = 0; i < 5; i++) sum.addAll(door(i * 20, 65, 1, 9, 9));
        OpeningScan.Result total = OpeningScan.judge(sum, OpeningScan.Limits.DEFAULT);
        assertFalse(total.ok(), "405 cells is more than the limit");
        assertEquals(5, total.openings());
    }

    @Test
    void theLimitsAreTheCallersToSet() {
        List<OpeningScan.Cell> d = door(0, 65, 1, 7, 7);
        assertTrue(OpeningScan.judge(d, new OpeningScan.Limits(100, 49, 4)).ok());
        assertFalse(OpeningScan.judge(d, new OpeningScan.Limits(100, 48, 4)).ok());
        assertFalse(OpeningScan.judge(d, new OpeningScan.Limits(48, 49, 4)).ok());
    }

    @Test
    void theGroupingIgnoresTheOrderOfTheCells() {
        List<OpeningScan.Cell> a = new ArrayList<>(door(10, 65, 1, 3, 4));
        a.addAll(door(20, 65, 1, 3, 4));
        List<OpeningScan.Cell> b = new ArrayList<>(a);
        java.util.Collections.reverse(b);
        java.util.Collections.shuffle(b, new java.util.Random(4));
        List<List<OpeningScan.Cell>> ga = OpeningScan.openings(a), gb = OpeningScan.openings(b);
        assertEquals(ga.size(), gb.size());
        for (int i = 0; i < ga.size(); i++) assertEquals(ga.get(i).size(), gb.get(i).size());
    }
}
