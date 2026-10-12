package com.newuniverse.nusmp.dungeon.nexus;

import com.newuniverse.nusmp.dungeon.nexus.NexusOp.Kind;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The op collector: frames (mirror and swap), facings, ellipses and the phase marks. */
class NexusOpsTest {
    /** Every cell of the ops as "x,y,z". */
    static Set<String> cells(List<NexusOp> ops) {
        Set<String> out = new HashSet<>();
        for (NexusOp o : ops) for (int x = o.x1(); x <= o.x2(); x++) for (int y = o.y1(); y <= o.y2(); y++) for (int z = o.z1(); z <= o.z2(); z++) out.add(x + "," + y + "," + z);
        return out;
    }

    @Test
    void aBoxIsPutInWorldCoordinatesRoundTheOrigin() {
        NexusOps o = new NexusOps(100, 200);
        o.box("minecraft:obsidian", 4, 7, 6, -2, 5, -3);                    // corners in any order
        NexusOp op = o.list().get(0);
        assertEquals(Kind.BOX, op.kind());
        assertEquals(98, op.x1());
        assertEquals(5, op.y1());
        assertEquals(197, op.z1());
        assertEquals(104, op.x2());
        assertEquals(7, op.y2());
        assertEquals(206, op.z2());
        assertEquals("minecraft:obsidian", op.block());
        assertEquals("", op.props());
        assertEquals(7L * 3 * 10, op.volume());
    }

    @Test
    void aSingleCellIsABlockAndAnAirBoxPlacesAir() {
        NexusOps o = new NexusOps(0, 0);
        o.put("minecraft:stone", 1, 2, 3);
        o.air(0, 0, 0, 2, 2, 2);
        o.fillAir("minecraft:cobweb", "", 5, 5, 5, 5, 5, 5);
        assertEquals(Kind.BLOCK, o.list().get(0).kind());
        assertEquals(Kind.BOX, o.list().get(1).kind());
        assertEquals(NexusOp.AIR, o.list().get(1).block());
        assertEquals(Kind.FILL_AIR, o.list().get(2).kind(), "a one-cell fill-air stays a fill-air");
    }

    @Test
    void aMirroredAndShiftedFrameMovesTheCells() {
        NexusOps o = new NexusOps(1000, 2000);
        o.frame(10, 20, false, -1, 1);
        o.put("minecraft:stone", 3, 8, -2);
        NexusOp op = o.list().get(0);
        assertEquals(1000 + 10 - 3, op.x1());
        assertEquals(2000 + 20 - 2, op.z1());
        assertEquals(8, op.y1(), "y is absolute");
        o.resetFrame();
        o.put("minecraft:stone", 3, 8, -2);
        assertEquals(1003, o.list().get(1).x1());
        assertEquals(1998, o.list().get(1).z1());
    }

    @Test
    void aSwappedFrameExchangesXAndZ() {
        NexusOps o = new NexusOps(0, 0);
        o.frame(0, 0, true, 1, 1);
        o.put("minecraft:stone", 3, 0, 5);
        assertEquals(5, o.list().get(0).x1());
        assertEquals(3, o.list().get(0).z1());
    }

    @Test
    void facingsTurnWithTheFrame() {
        NexusOps o = new NexusOps(0, 0);
        assertArrayEquals(new int[]{0, 1, 2, 3}, new int[]{o.face(0), o.face(1), o.face(2), o.face(3)});
        o.frame(0, 0, false, -1, 1);
        assertArrayEquals(new int[]{0, 3, 2, 1}, new int[]{o.face(0), o.face(1), o.face(2), o.face(3)}, "mirrored in x: east and west change places");
        o.frame(0, 0, false, 1, -1);
        assertArrayEquals(new int[]{2, 1, 0, 3}, new int[]{o.face(0), o.face(1), o.face(2), o.face(3)}, "mirrored in z: north and south change places");
        o.frame(0, 0, true, 1, 1);
        assertArrayEquals(new int[]{3, 2, 1, 0}, new int[]{o.face(0), o.face(1), o.face(2), o.face(3)}, "swapped: north becomes west, east becomes south");
        o.frame(0, 0, false, -1, -1);
        assertArrayEquals(new int[]{2, 3, 0, 1}, new int[]{o.face(0), o.face(1), o.face(2), o.face(3)}, "a half turn");
    }

    @Test
    void aFacingInThePropertiesFollowsTheFrameAndOtherPropertiesStay() {
        NexusOps o = new NexusOps(0, 0);
        o.put("minecraft:stone_brick_stairs", "facing=north,half=top", 0, 0, 0);
        assertEquals("facing=north,half=top", o.list().get(0).props());
        o.frame(0, 0, true, 1, 1);
        o.put("minecraft:stone_brick_stairs", "facing=north,half=top", 1, 0, 1);
        assertEquals("facing=west,half=top", o.list().get(1).props());
        o.frame(0, 0, true, 1, 1);
        o.put("minecraft:chain", "axis=x", 2, 0, 2);
        assertEquals("axis=z", o.list().get(2).props(), "an axis swaps with x and z");
        o.frame(0, 0, false, -1, 1);
        o.put("minecraft:chain", "axis=y", 3, 0, 3);
        assertEquals("axis=y", o.list().get(3).props());
        o.put("minecraft:lantern", "facing=up", 4, 0, 4);
        assertEquals("facing=up", o.list().get(4).props(), "up and down are not turned");
    }

    @Test
    void ringsLeaveTheInsideEmpty() {
        NexusOps o = new NexusOps(0, 0);
        o.ring("minecraft:stone", "", -3, -2, 3, 2, 0, 1);
        Set<String> got = cells(o.list());
        int expected = 0;
        for (int x = -3; x <= 3; x++) for (int z = -2; z <= 2; z++) for (int y = 0; y <= 1; y++) {
            boolean wall = x == -3 || x == 3 || z == -2 || z == 2;
            assertEquals(wall, got.contains(x + "," + y + "," + z), x + "," + y + "," + z);
            if (wall) expected++;
        }
        assertEquals(expected, got.size());
        long volume = 0;
        for (NexusOp op : o.list()) volume += op.volume();
        assertEquals(expected, volume, "no cell is written twice");
    }

    @Test
    void aDiscIsTheOvalAndAHollowDiscIsTheOvalMinusTheSmallerOne() {
        for (int[] p : new int[][]{{10, 10, 1}, {17, 9, 2}, {9, 17, 3}, {30, 22, 4}, {6, 6, 6}, {5, 3, 1}}) {
            int a = p[0], b = p[1], wall = p[2];
            NexusOps full = new NexusOps(50, -50);
            full.disc("minecraft:stone", "", a, b, 3, 4);
            Set<String> disc = cells(full.list());
            NexusOps hollow = new NexusOps(50, -50);
            hollow.discHollow("minecraft:stone", "", a, b, wall, 3, 4);
            Set<String> ring = cells(hollow.list());
            int n = 0, rn = 0;
            for (int x = -a - 1; x <= a + 1; x++) {
                for (int z = -b - 1; z <= b + 1; z++) {
                    boolean in = Footprint.inside(x, z, a, b);
                    boolean inner = a - wall >= 0 && b - wall >= 0 && Footprint.inside(x, z, a - wall, b - wall);
                    for (int y = 3; y <= 4; y++) {
                        String key = (50 + x) + "," + y + "," + (-50 + z);
                        assertEquals(in, disc.contains(key), "disc " + key);
                        assertEquals(in && !inner, ring.contains(key), "ring a=" + a + " b=" + b + " wall=" + wall + " " + key);
                        if (in) n++;
                        if (in && !inner) rn++;
                    }
                }
            }
            assertEquals(n, disc.size());
            assertEquals(rn, ring.size());
            long volume = 0;
            for (NexusOp op : hollow.list()) volume += op.volume();
            assertEquals(rn, volume, "no cell of the ring is written twice");
        }
    }

    @Test
    void carveIgnoresTheFrameAndClearsADiscToo() {
        NexusOps o = new NexusOps(10, 20);
        o.frame(5, 5, true, -1, 1);
        o.carve(4, -50, -3, -4, -40, 3);
        NexusOp op = o.list().get(0);
        assertEquals(Kind.CARVE, op.kind());
        assertEquals(6, op.x1());
        assertEquals(14, op.x2());
        assertEquals(17, op.z1());
        assertEquals(23, op.z2());
        assertEquals(-50, op.y1());
        assertEquals(-40, op.y2());
        NexusOps d = new NexusOps(0, 0);
        d.carveDisc(8, 5, 0, 2);
        Set<String> got = cells(d.list());
        for (int x = -9; x <= 9; x++) for (int z = -6; z <= 6; z++) assertEquals(Footprint.inside(x, z, 8, 5), got.contains(x + ",1," + z));
    }

    @Test
    void phasesAreMarkedWhereTheyStart() {
        NexusOps o = new NexusOps(0, 0);
        o.put("a:b", 0, 0, 0);
        o.put("a:b", 1, 0, 0);
        o.phase();
        o.put("a:b", 2, 0, 0);
        o.phase();
        o.phase();
        o.put("a:b", 3, 0, 0);
        assertEquals(List.of(2, 3, 3), o.phaseStarts());
        assertEquals(4, o.size());
    }

    @Test
    void propertiesAreParsedLeniently() {
        assertEquals(0, BlockProps.parse("").size());
        assertEquals(0, BlockProps.parse(null).size());
        List<String[]> kv = BlockProps.parse("facing=north, half=top ,junk,=x,y=");
        assertEquals(2, kv.size());
        assertEquals("half", kv.get(1)[0]);
        assertEquals("top", kv.get(1)[1]);
        assertEquals(1, BlockProps.facingIndex("east"));
        assertEquals(-1, BlockProps.facingIndex("up"));
    }

    @Test
    void anOpNeedsItsPartsInOrder() {
        assertThrows(IllegalArgumentException.class, () -> new NexusOp(Kind.BOX, "a:b", "", 1, 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new NexusOp(Kind.BOX, null, "", 0, 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new NexusOp(null, "a:b", "", 0, 0, 0, 0, 0, 0));
    }
}
