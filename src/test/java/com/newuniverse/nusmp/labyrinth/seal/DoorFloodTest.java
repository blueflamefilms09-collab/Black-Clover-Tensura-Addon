package com.newuniverse.nusmp.labyrinth.seal;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The flood that opens a whole Gatekeeper doorway from one clicked block. */
class DoorFloodTest {
    private static Set<Long> box(int x0, int y0, int z0, int x1, int y1, int z1) {
        Set<Long> s = new HashSet<>();
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) s.add(DoorFlood.pack(x, y, z));
        return s;
    }

    @Test
    void packRoundTripsNegativeAndLargeCoordinates() {
        int[][] samples = {{0, 0, 0}, {-1, -64, -1}, {123456, 319, -654321}, {-1048576, 2047, 1048575}, {30000000 % 1048576, -2048, 7}};
        for (int[] p : samples) {
            long k = DoorFlood.pack(p[0], p[1], p[2]);
            assertEquals(p[0], DoorFlood.x(k), "x of " + p[0]);
            assertEquals(p[1], DoorFlood.y(k), "y of " + p[1]);
            assertEquals(p[2], DoorFlood.z(k), "z of " + p[2]);
        }
        assertNotEquals(DoorFlood.pack(1, 0, 0), DoorFlood.pack(0, 1, 0));
        assertNotEquals(DoorFlood.pack(0, 0, 1), DoorFlood.pack(0, 1, 0));
    }

    @Test
    void aTwoByFourDoorwayOpensWholeFromAnyBlock() {
        Set<Long> door = box(10, 64, 10, 11, 67, 10);                                   // 2 wide, 4 tall, 1 deep
        for (long start : door) {
            List<Long> got = DoorFlood.flood(start, door::contains, DoorFlood.LIMIT);
            assertEquals(8, got.size(), "every block of the doorway from " + start);
            assertEquals(door, new HashSet<>(got));
            assertEquals(start, (long) got.get(0), "the clicked block comes first");
        }
    }

    @Test
    void facesOnlyNeverDiagonals() {
        long a = DoorFlood.pack(0, 0, 0), diag = DoorFlood.pack(1, 1, 0), far = DoorFlood.pack(2, 0, 0);
        Set<Long> members = Set.of(a, diag, far);
        List<Long> got = DoorFlood.flood(a, members::contains, DoorFlood.LIMIT);
        assertEquals(List.of(a), got, "a diagonal neighbour is a different door; a gap of one block too");
    }

    @Test
    void stopsAtTheWallBetweenTwoDoors() {
        Set<Long> left = box(0, 0, 0, 1, 1, 0), right = box(3, 0, 0, 4, 1, 0);           // x=2 is masonry
        Set<Long> all = new HashSet<>(left);
        all.addAll(right);
        List<Long> got = DoorFlood.flood(DoorFlood.pack(0, 0, 0), all::contains, DoorFlood.LIMIT);
        assertEquals(left, new HashSet<>(got));
    }

    @Test
    void theLimitCapsAWallOfDoorBlocks() {
        Set<Long> wall = box(0, 0, 0, 30, 10, 0);                                         // 341 door blocks in one wall
        List<Long> got = DoorFlood.flood(DoorFlood.pack(15, 5, 0), wall::contains, DoorFlood.LIMIT);
        assertEquals(DoorFlood.LIMIT, got.size());
        assertEquals(DoorFlood.LIMIT, new HashSet<>(got).size(), "no position twice");
        assertTrue(wall.containsAll(got));
    }

    @Test
    void aNonMemberStartAndAZeroLimitGiveNothing() {
        Set<Long> door = box(0, 0, 0, 1, 1, 0);
        assertTrue(DoorFlood.flood(DoorFlood.pack(5, 5, 5), door::contains, DoorFlood.LIMIT).isEmpty());
        assertTrue(DoorFlood.flood(DoorFlood.pack(0, 0, 0), door::contains, 0).isEmpty());
        assertEquals(1, DoorFlood.flood(DoorFlood.pack(0, 0, 0), door::contains, 1).size());
    }

    @Test
    void thePredicateIsAskedAtMostOncePerPosition() {
        Set<Long> door = box(0, 0, 0, 1, 3, 0);
        Set<Long> asked = new HashSet<>();
        List<Long> got = DoorFlood.flood(DoorFlood.pack(0, 0, 0), k -> { assertTrue(asked.add(k), "asked twice: " + k); return door.contains(k); }, DoorFlood.LIMIT);
        assertEquals(8, got.size());
    }
}
