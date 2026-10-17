package com.newuniverse.nusmp.labyrinth.mega.gen;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: every placement decision of the assembler is a box question, so the box semantics are tested on their own, with the cases that matter spelled
 * out: two pieces that stand face to face TOUCH and must not count as overlapping (or no two rooms could ever be glued together), one shared cell DOES
 * overlap, a door cap may protrude past the region by the cap margin but never vertically. The spatial index that answers the same question quickly is
 * checked against the slow obvious answer on thousands of random boxes, including negative coordinates (sectors grow in every direction), and its undo
 * (used by the backtracking boss-chain search) is checked to restore the previous answers exactly.
 */
class GeometryTest {

    @Test
    void boxesThatOnlyTouchDoNotOverlapButOneSharedCellDoes() {
        Box a = new Box(0, 0, 0, 8, 47, 8);
        Box touching = new Box(9, 0, 0, 17, 47, 8);
        Box oneCell = new Box(8, 0, 0, 16, 47, 8);
        assertFalse(a.overlaps(touching), "face to face is not an overlap");
        assertFalse(touching.overlaps(a));
        assertTrue(a.overlaps(oneCell), "one shared column is an overlap");
        assertTrue(oneCell.overlaps(a));
        assertFalse(a.overlaps(new Box(0, 48, 0, 8, 60, 8)), "stacked pieces touch vertically too");
        assertTrue(a.overlaps(a));
    }

    @Test
    void insideAllowsTheCapMarginSidewaysButNeverVertically() {
        Box region = new Box(0, 56, -85, 170, 103, 85);
        assertTrue(new Box(0, 56, -85, 170, 103, 85).inside(region, 0));
        assertFalse(new Box(-3, 56, 0, 5, 61, 5).inside(region, 0));
        assertTrue(new Box(-3, 64, 0, 5, 69, 5).inside(region, 8), "a cap may stand 8 outside");
        assertFalse(new Box(-9, 64, 0, 5, 69, 5).inside(region, 8), "but not 9");
        assertFalse(new Box(0, 50, 0, 5, 61, 5).inside(region, 8), "the margin never applies to y");
        assertFalse(new Box(0, 100, 0, 5, 104, 5).inside(region, 8));
    }

    @Test
    void farAndNearAreTheExtremesAlongADirection() {
        Box b = new Box(10, 0, -30, 40, 5, 20);
        assertEquals(40, b.far(Facing.EAST));
        assertEquals(10, b.near(Facing.EAST));
        assertEquals(-10, b.far(Facing.WEST), "walking west the far edge is minX, negated");
        assertEquals(-40, b.near(Facing.WEST));
        assertEquals(20, b.far(Facing.SOUTH));
        assertEquals(-30, b.near(Facing.SOUTH));
        assertEquals(30, b.far(Facing.NORTH));
        assertEquals(-20, b.near(Facing.NORTH));
        assertEquals(new Box(2, 0, -38, 48, 5, 28), b.inflateXZ(8));
        assertEquals(new Box(10, 0, -30, 60, 9, 20), b.union(new Box(50, 9, 0, 60, 9, 0)));
    }

    @Test
    void emptyBoxesAreRejected() {
        boolean threw = false;
        try { new Box(5, 0, 0, 4, 0, 0); } catch (IllegalArgumentException e) { threw = true; }
        assertTrue(threw);
    }

    private static Box random(Rng rng, int spread) {
        int x = rng.nextInt(2 * spread) - spread, z = rng.nextInt(2 * spread) - spread;
        return new Box(x, 56, z, x + rng.nextInt(60), 103, z + rng.nextInt(60));
    }

    @Test
    void theIndexAgreesWithBruteForceOnRandomBoxes() {
        Rng rng = new Rng(99);
        for (int round = 0; round < 40; round++) {
            BoxIndex index = new BoxIndex();
            List<Box> all = new ArrayList<>();
            for (int i = 0; i < 80; i++) {
                Box b = random(rng, 400);
                index.add(b);
                all.add(b);
            }
            for (int q = 0; q < 200; q++) {
                Box probe = random(rng, 450);
                int expected = -1;
                boolean any = false;
                for (int i = 0; i < all.size(); i++) {
                    if (all.get(i).overlaps(probe)) { any = true; if (expected < 0) expected = i; }
                }
                assertEquals(any, index.overlapsAny(probe), "overlap answer for " + probe);
                int got = index.firstOverlap(probe);
                assertEquals(any, got >= 0);
                if (got >= 0) assertTrue(all.get(got).overlaps(probe), "the reported box really overlaps");
            }
        }
    }

    @Test
    void removingTheLastBoxRestoresEveryEarlierAnswer() {
        Rng rng = new Rng(7);
        BoxIndex index = new BoxIndex();
        List<Box> all = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            Box b = random(rng, 200);
            index.add(b);
            all.add(b);
        }
        List<Box> probes = new ArrayList<>();
        for (int i = 0; i < 100; i++) probes.add(random(rng, 220));
        boolean[] before = new boolean[probes.size()];
        for (int i = 0; i < probes.size(); i++) before[i] = index.overlapsAny(probes.get(i));
        Box extra1 = random(rng, 200), extra2 = random(rng, 200);
        index.add(extra1);
        index.add(extra2);
        assertEquals(32, index.size());
        index.removeLast();
        index.removeLast();
        assertEquals(30, index.size());
        for (int i = 0; i < probes.size(); i++) assertEquals(before[i], index.overlapsAny(probes.get(i)), "probe " + i);
        // ids keep counting from the new end
        assertEquals(30, index.add(new Box(1000, 56, 1000, 1001, 57, 1001)));
    }

    @Test
    void vecArithmeticFollowsTheDirectionSteps() {
        Vec v = new Vec(4, 9, 0);
        assertEquals(new Vec(4, 9, -1), v.step(Facing.NORTH));
        assertEquals(new Vec(5, 9, 0), v.step(Facing.EAST));
        assertEquals(new Vec(4, 9, 9), v.step(Facing.SOUTH, 9));
        assertEquals(new Vec(-5, 9, 0), v.step(Facing.WEST, 9));
        assertEquals(9, v.along(Facing.SOUTH) + 9, "south is +z");
        assertEquals(-4, v.along(Facing.WEST));
        assertEquals(Facing.NORTH, Facing.byName(" North "));
        assertNull(Facing.byName("up"));
        assertEquals(Facing.WEST, Facing.NORTH.ccw());
        assertEquals(Facing.EAST, Facing.NORTH.cw());
        assertTrue(Facing.EAST.sameAxis(Facing.WEST));
        assertFalse(Facing.EAST.sameAxis(Facing.NORTH));
    }
}
