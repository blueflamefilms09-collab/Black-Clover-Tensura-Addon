package com.newuniverse.nusmp.labyrinth.mega.admin;

import com.newuniverse.nusmp.labyrinth.mega.cyclic.Roles;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: {@code /dungeon debug wfc} is only useful if its colours MEAN what the owner asked (green connected on a loop, yellow single link, red dead end) and if it cannot flood a
 * client. Both are pure, so both are pinned: the classification on small graphs whose answer can be worked out by hand (a ring of four rooms with a dead-end spur, a chain between
 * two terminals, a floor in two pieces), the geometry of a doorway and of a room outline, and the throttle (a burst never exceeds its budget, only points in range are sent, the
 * cursor walks through everything over successive bursts, an empty model is safe).
 */
public class WfcDebugModelTest {

    /** A 9-block room at module column {@code col}, row {@code row}: rooms side by side touch, as placed rooms do. */
    private static WfcDebugModel.RoomIn room(int id, int roles, int col, int row) {
        int x = col * 9, z = row * 9;
        return new WfcDebugModel.RoomIn(id, roles, null, x, 64, z, x + 8, 68, z + 8);
    }

    private static List<int[]> doors(int... ab) {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < ab.length; i += 2) out.add(new int[] {ab[i], ab[i + 1]});
        return out;
    }

    // ---------------------------------------------------------------- the colour code
    @Test
    public void aRingIsGreenAndItsSpurIsARedDeadEnd() {
        // 0-1-2-3 form a ring (a square of rooms), 4 hangs off room 0 and leads nowhere
        List<WfcDebugModel.RoomIn> rooms = List.of(room(0, 0, 0, 0), room(1, 0, 1, 0), room(2, 0, 1, 1), room(3, 0, 0, 1), room(4, 0, -1, 0));
        WfcDebugModel.Model m = WfcDebugModel.build(rooms, doors(0, 1, 1, 2, 2, 3, 3, 0, 0, 4), 64);
        assertEquals(WfcDebugModel.Hue.GREEN, m.roomHue[0]);
        assertEquals(WfcDebugModel.Hue.GREEN, m.roomHue[1]);
        assertEquals(WfcDebugModel.Hue.GREEN, m.roomHue[2]);
        assertEquals(WfcDebugModel.Hue.GREEN, m.roomHue[3]);
        assertEquals(WfcDebugModel.Hue.RED, m.roomHue[4], "the spur is a dead end");
        int green = 0, red = 0, yellow = 0;
        for (WfcDebugModel.Hue h : m.edgeHue) {
            if (h == WfcDebugModel.Hue.GREEN) green++;
            else if (h == WfcDebugModel.Hue.RED) red++;
            else yellow++;
        }
        assertEquals(4, green, "the four doorways of the ring are on a loop");
        assertEquals(1, red, "the doorway into the dead end is red");
        assertEquals(0, yellow);
        WfcDebugModel.Summary s = m.summary;
        assertEquals(5, s.rooms());
        assertEquals(5, s.edges());
        assertEquals(1, s.deadEnds());
        assertEquals(1, s.loops(), "one independent loop");
        assertEquals(4, s.onLoop());
        assertEquals(0, s.singleLink());
        assertEquals(1, s.bridges());
        assertEquals(1, s.components());
        assertFalse(s.clean(), "a dead end is not clean");
    }

    @Test
    public void aChainBetweenTwoTerminalsIsYellowSingleLinkAndNoDeadEnd() {
        // arrival stairs - hall - hall - forward stairs: the stairs are terminals, so no room is a dead end, but nothing lies on a loop
        List<WfcDebugModel.RoomIn> rooms = List.of(room(0, Roles.STAIRS, 0, 0), room(1, 0, 1, 0), room(2, 0, 2, 0), room(3, Roles.STAIRS, 3, 0));
        WfcDebugModel.Model m = WfcDebugModel.build(rooms, doors(0, 1, 1, 2, 2, 3), 64);
        for (WfcDebugModel.Hue h : m.roomHue) assertEquals(WfcDebugModel.Hue.YELLOW, h);
        for (WfcDebugModel.Hue h : m.edgeHue) assertEquals(WfcDebugModel.Hue.YELLOW, h);
        assertEquals(3, m.edgeHue.size());
        WfcDebugModel.Summary s = m.summary;
        assertEquals(0, s.deadEnds(), "terminals are destinations, not dead ends");
        assertEquals(0, s.loops());
        assertEquals(0, s.onLoop());
        assertEquals(4, s.singleLink());
        assertEquals(3, s.bridges());
        assertTrue(s.clean(), "no dead end and one piece is the owner's rule, even without a loop");
    }

    @Test
    public void everyKindOfTerminalIsExemptFromTheDeadEndRule() {
        int[] terminals = {Roles.ENTRANCE, Roles.STAIRS, Roles.BOSS, Roles.SAFE};
        for (int role : terminals) {
            List<WfcDebugModel.RoomIn> rooms = List.of(room(0, 0, 0, 0), room(1, 0, 1, 0), room(2, role, 2, 0));
            WfcDebugModel.Model m = WfcDebugModel.build(rooms, doors(0, 1, 1, 2), 64);
            assertEquals(WfcDebugModel.Hue.YELLOW, m.roomHue[2], "a leaf with role " + role);
            // room 0 is a plain leaf: a dead end
            assertEquals(WfcDebugModel.Hue.RED, m.roomHue[0], "the plain leaf, role " + role);
        }
    }

    @Test
    public void aDoorwayIntoADeadEndIsRedEvenWhenItIsAlsoABridge() {
        List<WfcDebugModel.RoomIn> rooms = List.of(room(0, Roles.STAIRS, 0, 0), room(1, 0, 1, 0), room(2, 0, 2, 0));
        WfcDebugModel.Model m = WfcDebugModel.build(rooms, doors(0, 1, 1, 2), 64);
        assertEquals(WfcDebugModel.Hue.RED, m.roomHue[2]);
        assertEquals(WfcDebugModel.Hue.YELLOW, m.roomHue[1], "the room before the dead end only has bridges: single link");
        assertEquals(WfcDebugModel.Hue.YELLOW, m.edgeHue.get(0), "stairs - hall is a bridge");
        assertEquals(WfcDebugModel.Hue.RED, m.edgeHue.get(1), "hall - dead end is red");
    }

    @Test
    public void aFloorInTwoPiecesIsReportedAsNotClean() {
        // two separate rings of three rooms: no dead end, two parts
        List<WfcDebugModel.RoomIn> rooms = new ArrayList<>();
        for (int i = 0; i < 6; i++) rooms.add(room(i, 0, i % 3, i / 3 * 5));
        WfcDebugModel.Model m = WfcDebugModel.build(rooms, doors(0, 1, 1, 2, 2, 0, 3, 4, 4, 5, 5, 3), 64);
        assertEquals(0, m.summary.deadEnds());
        assertEquals(2, m.summary.components());
        assertEquals(2, m.summary.loops());
        assertFalse(m.summary.clean(), "two parts means a part the player cannot reach");
    }

    @Test
    public void anIsolatedRoomIsADeadEndAndAnUnknownDoorwayIsCountedNotDrawn() {
        List<WfcDebugModel.RoomIn> rooms = List.of(room(10, 0, 0, 0), room(11, 0, 1, 0), room(12, 0, 2, 0));
        List<int[]> d = doors(10, 11, 11, 99, 77, 10, 12, 12);
        d.add(new int[] {10});
        WfcDebugModel.Model m = WfcDebugModel.build(rooms, d, 64);
        assertEquals(4, m.summary.skippedEdges(), "a missing room, a missing room, a loop onto itself and a pair with one id");
        assertEquals(1, m.summary.edges());
        assertEquals(WfcDebugModel.Hue.RED, m.roomHue[2], "room 12 has no doorway at all");
        assertFalse(m.summary.clean());
    }

    @Test
    public void aDuplicateDoorwayIsNotCountedTwice() {
        List<WfcDebugModel.RoomIn> rooms = List.of(room(0, Roles.STAIRS, 0, 0), room(1, Roles.STAIRS, 1, 0));
        WfcDebugModel.Model m = WfcDebugModel.build(rooms, doors(0, 1, 1, 0, 0, 1), 64);
        assertEquals(1, m.summary.edges());
        assertEquals(0, m.summary.skippedEdges());
        assertEquals(1, m.edgeHue.size());
    }

    @Test
    public void anEmptyOrSingleRoomFloorDoesNotCrash() {
        WfcDebugModel.Model none = WfcDebugModel.build(List.of(), List.of(), 64);
        assertEquals(0, none.points());
        assertFalse(none.summary.clean(), "no rooms is not a floor");
        WfcDebugModel.Model one = WfcDebugModel.build(List.of(room(0, 0, 0, 0)), List.of(), 64);
        assertEquals(0, one.summary.deadEnds(), "one room has nothing to be stuck in");
        assertTrue(one.summary.clean());
        assertTrue(one.boxes.count() > 0);
        assertEquals(0, one.nodes.count());
        WfcDebugModel.Burst b = WfcDebugModel.next(none, new WfcDebugModel.Cursor(), 0, 64, 0, 72, 160);
        assertEquals(0, b.count());
    }

    // ---------------------------------------------------------------- the geometry
    @Test
    public void aDoorwayIsWhereTwoTouchingRoomsMeet() {
        WfcDebugModel.RoomIn a = room(0, 0, 0, 0), b = room(1, 0, 1, 0);                       // x 0..8 and 9..17, z 0..8: they meet on the line x = 9
        double[] door = WfcDebugModel.doorway(a, b);
        assertEquals(9.0, door[0], 1e-9);
        assertEquals(4.5, door[1], 1e-9, "the middle of the shared stretch of z");
        double[] south = WfcDebugModel.doorway(a, room(2, 0, 0, 1));
        assertEquals(4.5, south[0], 1e-9);
        assertEquals(9.0, south[1], 1e-9);
        double[] apart = WfcDebugModel.doorway(a, room(3, 0, 3, 0));                            // a gap of two rooms: the middle between the centres
        assertEquals((4.5 + 31.5) / 2, apart[0], 1e-9);
        assertEquals(4.5, apart[1], 1e-9);
    }

    @Test
    public void everyRoomIsOutlinedAndEveryDoorwayHasANodeAndTwoLines() {
        List<WfcDebugModel.RoomIn> rooms = List.of(room(0, Roles.STAIRS, 0, 0), room(1, Roles.STAIRS, 1, 0));
        WfcDebugModel.Model m = WfcDebugModel.build(rooms, doors(0, 1), 64);
        assertEquals(1, m.nodes.count(), "one socket node");
        assertEquals(9.0, m.nodes.x(0), 1e-9);
        assertEquals(65.5, m.nodes.y(0), 1e-9, "a block and a half above the floor row");
        assertTrue(m.edges.count() >= 2 * 3, "a line on each side of the doorway");
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < m.boxes.count(); i++) {
            minX = Math.min(minX, m.boxes.x(i));
            maxX = Math.max(maxX, m.boxes.x(i));
            minY = Math.min(minY, m.boxes.y(i));
            maxY = Math.max(maxY, m.boxes.y(i));
        }
        assertEquals(0.0, minX, 1e-9, "the outline encloses the blocks: it starts at the first block's edge");
        assertEquals(18.0, maxX, 1e-9, "and ends at the far edge of the last block (x 17 + 1)");
        assertEquals(64.0, minY, 1e-9);
        assertEquals(69.0, maxY, 1e-9);
        for (int i = 0; i < m.boxes.count(); i++) assertEquals(WfcDebugModel.Hue.YELLOW, m.boxes.hue(i));
    }

    // ---------------------------------------------------------------- the throttle
    private static WfcDebugModel.Model bigFloor() {
        List<WfcDebugModel.RoomIn> rooms = new ArrayList<>();
        List<int[]> d = new ArrayList<>();
        int n = 6;
        for (int r = 0; r < n; r++) for (int c = 0; c < n; c++) rooms.add(room(r * n + c, 0, c, r));
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                if (c + 1 < n) d.add(new int[] {r * n + c, r * n + c + 1});
                if (r + 1 < n) d.add(new int[] {r * n + c, (r + 1) * n + c});
            }
        }
        return WfcDebugModel.build(rooms, d, 64);
    }

    @Test
    public void aBurstNeverExceedsItsBudgetOrTheHardCap() {
        WfcDebugModel.Model m = bigFloor();
        assertTrue(m.points() > 10 * WfcDebugModel.MAX_PER_BURST, "the floor is much bigger than one burst: " + m.points());
        WfcDebugModel.Cursor c = new WfcDebugModel.Cursor();
        for (int budget : new int[] {0, 1, 5, 40, 160}) {
            WfcDebugModel.Burst b = WfcDebugModel.next(m, c, 27, 66, 27, 500.0, budget);
            assertTrue(b.count() <= budget, "budget " + budget + " gave " + b.count());
            if (budget >= 5) assertTrue(b.count() > 0, "budget " + budget);
            assertEquals(b.count() * 3, b.xyz().length);
            assertEquals(b.count(), b.kind().length);
        }
        assertEquals(0, WfcDebugModel.next(m, c, 27, 66, 27, 500.0, -5).count(), "a negative budget sends nothing");
        assertTrue(WfcDebugModel.MAX_PER_BURST <= WfcDebugModel.MAX_PER_TICK, "one burst fits in a tick's share");
    }

    @Test
    public void aBurstSharesItsBudgetBetweenTheThreeLayers() {
        WfcDebugModel.Model m = bigFloor();
        WfcDebugModel.Burst b = WfcDebugModel.next(m, new WfcDebugModel.Cursor(), 27, 66, 27, 500.0, 100);
        int[] per = new int[3];
        for (int i = 0; i < b.count(); i++) per[b.kind()[i]]++;
        assertEquals(40, per[WfcDebugModel.Kind.NODE.ordinal()], "40 % to the socket nodes");
        assertEquals(35, per[WfcDebugModel.Kind.EDGE.ordinal()], "35 % to the graph lines");
        assertEquals(25, per[WfcDebugModel.Kind.BOX.ordinal()], "the rest to the room outlines");
    }

    @Test
    public void onlyPointsInRangeAreSent() {
        WfcDebugModel.Model m = bigFloor();
        double px = 4.5, py = 66, pz = 4.5, range = 20.0;
        WfcDebugModel.Cursor c = new WfcDebugModel.Cursor();
        for (int round = 0; round < 30; round++) {
            WfcDebugModel.Burst b = WfcDebugModel.next(m, c, px, py, pz, range, 160);
            for (int i = 0; i < b.count(); i++) {
                double dx = b.xyz()[3 * i] - px, dy = b.xyz()[3 * i + 1] - py, dz = b.xyz()[3 * i + 2] - pz;
                assertTrue(dx * dx + dy * dy + dz * dz <= range * range + 1e-9, "a point " + Math.sqrt(dx * dx + dy * dy + dz * dz) + " blocks away");
            }
        }
        WfcDebugModel.Burst nothing = WfcDebugModel.next(m, new WfcDebugModel.Cursor(), 5000, 66, 5000, 72, 160);
        assertEquals(0, nothing.count(), "a floor 5000 blocks away is not drawn");
    }

    @Test
    public void successiveBurstsWalkThroughEverythingInRange() {
        WfcDebugModel.Model m = bigFloor();
        WfcDebugModel.Cursor c = new WfcDebugModel.Cursor();
        Set<String> expected = new HashSet<>();
        addAll(expected, m.nodes, WfcDebugModel.Kind.NODE);
        addAll(expected, m.edges, WfcDebugModel.Kind.EDGE);
        addAll(expected, m.boxes, WfcDebugModel.Kind.BOX);
        Set<String> seen = new HashSet<>();
        int rounds = 0;
        while (rounds < 400 && seen.size() < expected.size()) {
            rounds++;
            WfcDebugModel.Burst b = WfcDebugModel.next(m, c, 27, 66, 27, 500.0, WfcDebugModel.MAX_PER_BURST);
            for (int i = 0; i < b.count(); i++) seen.add(b.kind()[i] + ":" + b.xyz()[3 * i] + "," + b.xyz()[3 * i + 1] + "," + b.xyz()[3 * i + 2]);
        }
        assertTrue(rounds < 400, "everything was drawn within " + rounds + " bursts (" + seen.size() + " of " + expected.size() + " distinct points)");
        assertEquals(expected, seen, "every point of every layer was sent");
    }

    private static void addAll(Set<String> out, WfcDebugModel.Layer l, WfcDebugModel.Kind kind) {
        for (int i = 0; i < l.count(); i++) out.add((byte) kind.ordinal() + ":" + l.x(i) + "," + l.y(i) + "," + l.z(i));
    }

    @Test
    public void theCursorWrapsAndStaysInside() {
        WfcDebugModel.Model m = bigFloor();
        WfcDebugModel.Cursor c = new WfcDebugModel.Cursor();
        c.nodes = 1_000_000;
        c.edges = -7;
        c.boxes = Integer.MIN_VALUE;
        WfcDebugModel.Burst b = WfcDebugModel.next(m, c, 27, 66, 27, 500.0, 160);
        assertTrue(b.count() > 0);
        assertTrue(c.nodes >= 0 && c.nodes < m.nodes.count());
        assertTrue(c.edges >= 0 && c.edges < m.edges.count());
        assertTrue(c.boxes >= 0 && c.boxes < m.boxes.count());
    }

    @Test
    public void theBurstsAreStaggeredEveryTenTicksAndTheViewEndsAfterFiveMinutes() {
        assertEquals(10, WfcDebugModel.BURST_EVERY);
        int hits = 0;
        for (long t = 0; t < 100; t++) if (WfcDebugModel.burstNow(t, 3)) hits++;
        assertEquals(10, hits, "one burst every ten ticks");
        assertTrue(WfcDebugModel.burstNow(7, 3));
        assertTrue(WfcDebugModel.burstNow(17, 3));
        assertFalse(WfcDebugModel.burstNow(8, 3));
        for (int offset : new int[] {Integer.MIN_VALUE, -1, 0, 5, Integer.MAX_VALUE}) {
            int n = 0;
            for (long t = 0; t < 1000; t++) if (WfcDebugModel.burstNow(t, offset)) n++;
            assertEquals(100, n, "offset " + offset);
        }
        assertEquals(6000L, WfcDebugModel.TTL_TICKS);
        assertFalse(WfcDebugModel.expired(100, 100));
        assertFalse(WfcDebugModel.expired(100, 6099));
        assertTrue(WfcDebugModel.expired(100, 6100));
        assertTrue(WfcDebugModel.expired(100, 100000));
    }
}
