package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the room lockdown and the 7 x 7 clearance were built in parallel and never tried together. The clearance cuts every connected doorway 7 wide and 7 high through both wall
 * layers of the room; the lockdown read the wall one layer deep, from two blocks inside each corner, 9 rows up, and allowed 320 cells. These tests pin the sizing logic that closes the
 * seam, on made-up rooms whose doorways are cut exactly the way the clearance rule cuts them: the barrier covers the whole cut (both layers, rows 9..15, the outer columns one block
 * from the corner), takes nothing that is solid, the limits let twelve such doorways pass, the apron in front of a barrier is kept clear by the wave when the room has room, and a room
 * that was not cut is read exactly as before (one layer, inset 2, the old limits).
 */
class CutDoorwayTest {
    private static final int N = 27, FLOOR_ROW = 8, MAX_Y = 30;

    /** A 27 x 27 room, walls two layers thick, with the given cut boxes {x0, z0, x1, z1} free in rows 9..{9 + height - 1}. */
    private static OpeningScan.Free room(int height, int[]... cuts) {
        return (x, y, z) -> {
            if (x < 0 || z < 0 || x >= N || z >= N || y < 0 || y > MAX_Y) return false;
            boolean solid = x < 2 || z < 2 || x >= N - 2 || z >= N - 2 || y <= FLOOR_ROW || y > FLOOR_ROW + 12;
            if (!solid) return true;
            if (y < FLOOR_ROW + 1 || y > FLOOR_ROW + height) return false;
            for (int[] c : cuts) if (x >= c[0] && x <= c[2] && z >= c[1] && z <= c[3]) return true;
            return false;
        };
    }

    private static OpeningScan.Scan scan(boolean cut, OpeningScan.Free f) { return OpeningScan.scan(0, 0, 0, N - 1, MAX_Y, N - 1, 1, 9, FLOOR_ROW, cut, f); }

    @Test
    void theBuiltInCheckOfTheSelfTestHolds() {
        assertEquals(List.of(), OpeningScan.cutDoorwayCheck());
    }

    @Test
    void aCutDoorwayInTheFirstModuleIsClosedToItsOuterColumn() {
        int[] door = {1, 0, 7, 4};                                                         // x 1..7 through z 0..1 and the apron to z 4
        OpeningScan.Scan s = scan(true, room(7, door));
        assertEquals(49, s.plane().size(), "7 x 7 cells in the inner layer");
        assertEquals(98, s.fill().size(), "both wall layers");
        Set<OpeningScan.Cell> fill = new HashSet<>(s.fill());
        for (int x = 1; x <= 7; x++) {
            for (int y = 9; y <= 15; y++) {
                assertTrue(fill.contains(new OpeningScan.Cell(x, y, 0)), "outer layer " + x + "," + y);
                assertTrue(fill.contains(new OpeningScan.Cell(x, y, 1)), "inner layer " + x + "," + y);
            }
        }
        // read the old way, the column at x = 1 is missed: that is the one-block gap beside the barrier this fix closes
        OpeningScan.Scan old = scan(false, room(7, door));
        assertEquals(42, old.plane().size());
        assertFalse(new HashSet<>(old.fill()).contains(new OpeningScan.Cell(1, 12, 1)));
    }

    @Test
    void theLastModuleDoorwayIsClosedToTheOtherCorner() {
        OpeningScan.Scan s = scan(true, room(7, new int[] {19, 22, 25, 26}));
        assertEquals(49, s.plane().size());
        assertTrue(new HashSet<>(s.fill()).contains(new OpeningScan.Cell(25, 9, 25)));
        assertTrue(new HashSet<>(s.fill()).contains(new OpeningScan.Cell(25, 15, 26)));
    }

    @Test
    void theBarrierTakesNothingSolidAndNothingOutsideTheCut() {
        int[][] cuts = {{1, 0, 7, 4}, {0, 10, 4, 16}};
        OpeningScan.Free f = room(7, cuts);
        for (OpeningScan.Cell c : scan(true, f).fill()) {
            assertTrue(f.at(c.x(), c.y(), c.z()), "free " + c);
            boolean inside = false;
            for (int[] k : cuts) if (c.x() >= k[0] && c.x() <= k[2] && c.z() >= k[1] && c.z() <= k[3]) inside = true;
            assertTrue(inside, "inside a cut " + c);
        }
    }

    @Test
    void twoDoorwaysThatShareACornerCellFillItOnce() {
        // a north doorway from x 1 and a west doorway from z 1 both pass through the cell (1, y, 1)
        OpeningScan.Scan s = scan(true, room(7, new int[] {1, 0, 7, 4}, new int[] {0, 1, 4, 7}));
        Set<OpeningScan.Cell> distinct = new HashSet<>(s.fill());
        assertEquals(distinct.size(), s.fill().size(), "no cell twice");
        assertEquals(91, s.plane().size(), "7 x 7 + 7 x 7 minus the seven cells they share");
        assertEquals(1, OpeningScan.openings(s.plane()).size(), "the two meet at the corner: one L-shaped opening");
        assertTrue(OpeningScan.judge(s.plane(), OpeningScan.limitsFor(OpeningScan.Limits.DEFAULT, true)).ok());
    }

    @Test
    void anOldDoorwayOfAnUncutRoomIsReadExactlyAsBefore() {
        int[] door = {3, 0, 5, 3};                                                         // 3 wide through the two layers, rows 9..12
        OpeningScan.Free f = (x, y, z) -> {
            if (x < 0 || z < 0 || x >= N || z >= N) return false;
            boolean solid = x < 2 || z < 2 || x >= N - 2 || z >= N - 2 || y <= FLOOR_ROW || y > FLOOR_ROW + 12;
            if (!solid) return true;
            return y >= 9 && y <= 12 && x >= door[0] && x <= door[2] && z >= door[1] && z <= door[3];
        };
        OpeningScan.Scan s = scan(false, f);
        assertEquals(12, s.plane().size(), "3 wide, 4 high");
        assertEquals(s.plane(), s.fill(), "one layer: the plane itself");
        for (OpeningScan.Cell c : s.fill()) assertEquals(1, c.z());
        assertEquals(2, OpeningScan.inset(false));
        assertEquals(9, OpeningScan.rows(9, false));
        assertSame(OpeningScan.Limits.DEFAULT, OpeningScan.limitsFor(OpeningScan.Limits.DEFAULT, false));
    }

    @Test
    void aCutRoomLooksAtLeastSevenRowsHighWhateverTheConfigSays() {
        assertEquals(7, OpeningScan.rows(4, true));
        assertEquals(9, OpeningScan.rows(9, true));
        assertEquals(4, OpeningScan.rows(4, false));
        assertEquals(1, OpeningScan.inset(true));
        // a config of 4 rows would leave the top three rows of a cut doorway open
        int[] door = {1, 0, 7, 4};
        OpeningScan.Scan s = OpeningScan.scan(0, 0, 0, N - 1, MAX_Y, N - 1, 1, 4, FLOOR_ROW, true, room(7, door));
        assertEquals(49, s.plane().size());
        OpeningScan.Scan oldRows = OpeningScan.scan(0, 0, 0, N - 1, MAX_Y, N - 1, 1, 4, FLOOR_ROW, false, room(7, door));
        assertEquals(24, oldRows.plane().size(), "the old reading with 4 rows and inset 2: 6 columns of 4");
    }

    @Test
    void theLimitsOfACutRoomHoldTwelveSevenBySevenDoorways() {
        OpeningScan.Limits base = OpeningScan.Limits.DEFAULT;
        OpeningScan.Limits cut = OpeningScan.limitsFor(base, true);
        assertTrue(cut.maxTotal() >= 12 * 49);
        assertTrue(cut.maxPerOpening() >= 7 * 9);
        assertEquals(base.maxOpenings(), cut.maxOpenings());
        List<OpeningScan.Cell> twelve = new ArrayList<>();
        for (int d = 0; d < 12; d++) for (int x = 0; x < 7; x++) for (int y = 0; y < 7; y++) twelve.add(new OpeningScan.Cell(d * 10 + x, y, 1));
        assertTrue(OpeningScan.judge(twelve, cut).ok());
        assertFalse(OpeningScan.judge(twelve, base).ok(), "the old limits refuse them");
        // a cavern is still refused: a wall open along 27 blocks and 9 high
        List<OpeningScan.Cell> cavern = new ArrayList<>();
        for (int x = 0; x < 40; x++) for (int y = 0; y < 9; y++) cavern.add(new OpeningScan.Cell(x, y, 1));
        assertFalse(OpeningScan.judge(cavern, cut).ok(), "an opening of 360 cells is not a doorway, cut or not");
        // a configured stricter limit is never loosened below itself
        OpeningScan.Limits big = new OpeningScan.Limits(2000, 300, 40);
        OpeningScan.Limits bigCut = OpeningScan.limitsFor(big, true);
        assertEquals(2000, bigCut.maxTotal());
        assertEquals(300, bigCut.maxPerOpening());
        assertEquals(40 * 49, 40 * OpeningScan.CUT_CELLS);
    }

    @Test
    void extentReadsWidthAndHeightOfAnOpening() {
        List<OpeningScan.Cell> d = new ArrayList<>();
        for (int x = 0; x < 7; x++) for (int y = 9; y < 16; y++) d.add(new OpeningScan.Cell(x, y, 1));
        int[] e = OpeningScan.extent(d);
        assertEquals(7, e[0]);
        assertEquals(7, e[1]);
        int[] none = OpeningScan.extent(List.of());
        assertEquals(0, none[0]);
        List<OpeningScan.Cell> along = new ArrayList<>();
        for (int z = 3; z < 10; z++) along.add(new OpeningScan.Cell(1, 9, z));
        assertEquals(7, OpeningScan.extent(along)[0], "a doorway in a west wall runs along z");
    }

    // ------------------------------------------------------------------------------------------------------------------------------ the wave

    private static List<double[]> players(double x, double z) {
        List<double[]> out = new ArrayList<>();
        out.add(new double[] {x, 65.0, z});
        return out;
    }

    @Test
    void withoutDoorwaysTheWaveIsExactlyThePlainPick() {
        List<WaveCells.Cell> plain = WaveCells.pick(0, 0, 30, 30, 65, List.of(), players(15.5, 15.5), 4.0, new Random(7));
        List<WaveCells.Cell> same = WaveCells.pick(0, 0, 30, 30, 65, List.of(), players(15.5, 15.5), 4.0, new Random(7), List.of());
        assertEquals(plain, same);
    }

    @Test
    void theWaveKeepsOutOfTheApronInFrontOfTheBarriersWhenItCan() {
        List<int[]> doorways = new ArrayList<>();
        for (int x = 10; x <= 16; x++) doorways.add(new int[] {x, 1});                     // the plane of a north doorway of a 27 x 27 room
        List<WaveCells.Cell> cells = WaveCells.pick(0, 0, 26, 26, 65, List.of(), players(13.5, 13.5), 4.0, new Random(3), doorways);
        assertFalse(cells.isEmpty());
        boolean seenNear = false;
        for (WaveCells.Cell c : cells) {
            boolean near = WaveCells.nearDoorway(doorways, c.x(), c.z());
            if (near) seenNear = true;
            else assertFalse(seenNear, "a cell far from the door follows one near it: " + c);
        }
        assertTrue(seenNear, "the near cells are kept as the reserve, not dropped");
        List<WaveCells.Cell> plain = WaveCells.pick(0, 0, 26, 26, 65, List.of(), players(13.5, 13.5), 4.0, new Random(3));
        assertEquals(new HashSet<>(plain), new HashSet<>(cells), "the same cells, only the order differs");
        // the first eight, the group the encounter spreads over, are all out of the apron
        for (int i = 0; i < Math.min(8, cells.size()); i++) assertFalse(WaveCells.nearDoorway(doorways, cells.get(i).x(), cells.get(i).z()), "group cell " + i);
    }

    @Test
    void aRoomTooSmallToAvoidTheApronStillGetsItsWave() {
        List<int[]> doorways = new ArrayList<>();
        for (int x = 1; x <= 7; x++) doorways.add(new int[] {x, 1});
        List<WaveCells.Cell> cells = WaveCells.pick(0, 0, 8, 8, 65, List.of(), players(2.5, 2.5), 1.0, new Random(1), doorways);
        List<WaveCells.Cell> plain = WaveCells.pick(0, 0, 8, 8, 65, List.of(), players(2.5, 2.5), 1.0, new Random(1));
        assertEquals(new HashSet<>(plain), new HashSet<>(cells));
    }
}
