package com.newuniverse.nusmp.labyrinth.mega.gen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the assembler plans in pure Java and the game then places the plan, so if "turned by one quarter" meant something different in the two, every
 * sector would be built rotated or mirrored and no test of the assembler alone could notice. These tests pin the maths with numbers worked out by hand
 * from the game's own rule ({@code StructureTemplate.transform} about the origin: CLOCKWISE_90 sends (x, z) to (-z, x), CLOCKWISE_180 to (-x, -z),
 * COUNTERCLOCKWISE_90 to (z, -x)), including the bounding box a placed piece covers, and cross-check {@link Rot} against a second, independent
 * transcription of that rule written the way the game writes it (with the pivot terms spelled out). The lead's plan compositor relies on exactly the
 * property tested last: a piece's world cell for a template cell c is the piece's origin plus the vanilla rotation of c.
 */
class RotationTest {

    /** The game's transform with a pivot at (0, 0), written out term by term as in StructureTemplate.transform (second, independent transcription). */
    private static int[] vanilla(int rotation, int i, int j, int k) {
        int l = 0, i1 = 0;                                   // pivot x, z
        switch (rotation) {
            case 3: return new int[] {l - i1 + k, j, l + i1 - i};      // COUNTERCLOCKWISE_90
            case 1: return new int[] {l + i1 - k, j, i1 - l + i};      // CLOCKWISE_90
            case 2: return new int[] {l + l - i, j, i1 + i1 - k};      // CLOCKWISE_180
            default: return new int[] {i, j, k};
        }
    }

    @Test
    void cellsMatchHandComputedCases() {
        Vec c = new Vec(3, 5, 7);
        assertEquals(new Vec(3, 5, 7), Rot.cell(0, c));
        assertEquals(new Vec(-7, 5, 3), Rot.cell(1, c), "CLOCKWISE_90 sends (x,z) to (-z,x)");
        assertEquals(new Vec(-3, 5, -7), Rot.cell(2, c), "CLOCKWISE_180 sends (x,z) to (-x,-z)");
        assertEquals(new Vec(7, 5, -3), Rot.cell(3, c), "COUNTERCLOCKWISE_90 sends (x,z) to (z,-x)");
        assertEquals(new Vec(3, 5, 7), Rot.cell(4, c), "turns are taken modulo 4");
        assertEquals(new Vec(7, 5, -3), Rot.cell(-1, c), "a negative turn count goes the other way");
    }

    @Test
    void cellsAgreeWithTheIndependentTranscriptionOfTheGamesRule() {
        for (int r = 0; r < 4; r++) {
            for (int x = -12; x <= 12; x++) {
                for (int z = -12; z <= 12; z++) {
                    int[] v = vanilla(r, x, 9, z);
                    Vec got = Rot.cell(r, new Vec(x, 9, z));
                    assertEquals(v[0], got.x(), "x for rotation " + r + " of " + x + "," + z);
                    assertEquals(v[1], got.y());
                    assertEquals(v[2], got.z(), "z for rotation " + r + " of " + x + "," + z);
                }
            }
        }
    }

    @Test
    void fourQuarterTurnsAreTheIdentityAndTurnsAddUp() {
        Vec c = new Vec(11, 2, -4);
        assertEquals(c, Rot.cell(1, Rot.cell(1, Rot.cell(1, Rot.cell(1, c)))));
        for (int a = 0; a < 4; a++) {
            for (int b = 0; b < 4; b++) assertEquals(Rot.cell(a + b, c), Rot.cell(a, Rot.cell(b, c)), a + " then " + b);
        }
    }

    @Test
    void facingsFollowTheClockwiseOrderOfTheGame() {
        assertEquals(Facing.EAST, Rot.facing(1, Facing.NORTH), "CLOCKWISE_90 turns north into east");
        assertEquals(Facing.SOUTH, Rot.facing(1, Facing.EAST));
        assertEquals(Facing.WEST, Rot.facing(1, Facing.SOUTH));
        assertEquals(Facing.NORTH, Rot.facing(1, Facing.WEST));
        assertEquals(Facing.SOUTH, Rot.facing(2, Facing.NORTH));
        assertEquals(Facing.WEST, Rot.facing(3, Facing.NORTH), "COUNTERCLOCKWISE_90 turns north into west");
        // a facing turns the same way as the unit step it stands for
        for (Facing f : Facing.values()) {
            for (int r = 0; r < 4; r++) {
                Vec step = Rot.cell(r, new Vec(f.dx, 0, f.dz));
                Facing t = Rot.facing(r, f);
                assertEquals(t.dx, step.x(), f + " turned " + r);
                assertEquals(t.dz, step.z(), f + " turned " + r);
            }
        }
        assertEquals(1, Rot.turnsFor(Facing.NORTH, Facing.EAST));
        assertEquals(3, Rot.turnsFor(Facing.NORTH, Facing.WEST));
        assertEquals(2, Rot.turnsFor(Facing.SOUTH, Facing.NORTH));
        assertEquals(0, Rot.turnsFor(Facing.WEST, Facing.WEST));
    }

    @Test
    void boxesMatchTheBoundingBoxTheGameReturns() {
        // a 27 x 48 x 18 template placed at (100, 56, 200): the cases are worked out by hand from the corners (0,0,0) and (26,47,17)
        Vec o = new Vec(100, 56, 200);
        assertEquals(new Box(100, 56, 200, 126, 103, 217), Rot.box(o, 0, 27, 48, 18));
        assertEquals(new Box(83, 56, 200, 100, 103, 226), Rot.box(o, 1, 27, 48, 18), "CLOCKWISE_90: far corner (26,17) goes to (-17,26)");
        assertEquals(new Box(74, 56, 183, 100, 103, 200), Rot.box(o, 2, 27, 48, 18), "CLOCKWISE_180: far corner goes to (-26,-17)");
        assertEquals(new Box(100, 56, 174, 117, 103, 200), Rot.box(o, 3, 27, 48, 18), "COUNTERCLOCKWISE_90: far corner goes to (17,-26)");
        // an odd number of quarter turns swaps the footprint
        assertEquals(27, Rot.box(o, 0, 27, 48, 18).sizeX());
        assertEquals(18, Rot.box(o, 1, 27, 48, 18).sizeX());
        assertEquals(27, Rot.box(o, 1, 27, 48, 18).sizeZ());
        assertArrayEqualsInts(new int[] {18, 27}, Rot.size(1, 27, 18));
        assertArrayEqualsInts(new int[] {27, 18}, Rot.size(2, 27, 18));
    }

    private static void assertArrayEqualsInts(int[] expected, int[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], actual[i]);
    }

    @Test
    void aPieceCoversExactlyTheImagesOfItsTemplateCells() {
        int sx = 5, sy = 3, sz = 8;
        for (int r = 0; r < 4; r++) {
            Vec o = new Vec(-40 + r * 13, 56, 77 - r * 5);
            Box box = Rot.box(o, r, sx, sy, sz);
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
            for (int x = 0; x < sx; x++) {
                for (int y = 0; y < sy; y++) {
                    for (int z = 0; z < sz; z++) {
                        Vec w = Rot.world(o, r, new Vec(x, y, z));
                        assertTrue(box.contains(w), "cell " + x + "," + y + "," + z + " rotation " + r + " lands at " + w + " outside " + box);
                        minX = Math.min(minX, w.x());
                        maxX = Math.max(maxX, w.x());
                        minZ = Math.min(minZ, w.z());
                        maxZ = Math.max(maxZ, w.z());
                    }
                }
            }
            assertEquals(box.minX(), minX, "the box is tight in x for rotation " + r);
            assertEquals(box.maxX(), maxX);
            assertEquals(box.minZ(), minZ);
            assertEquals(box.maxZ(), maxZ);
            assertEquals(sx * sz, box.sizeX() * box.sizeZ(), "the footprint keeps its area");
        }
    }

    @Test
    void aPiecesWorldCellIsItsOriginPlusTheVanillaRotationOfTheTemplateCell() {
        // the property the lead's plan compositor relies on, checked against the independent transcription for every rotation
        Vec origin = new Vec(-137, 56, 4021);
        for (int r = 0; r < 4; r++) {
            for (Vec c : new Vec[] {new Vec(0, 0, 0), new Vec(4, 9, 0), new Vec(0, 9, 13), new Vec(26, 47, 17), new Vec(13, 8, 13)}) {
                int[] v = vanilla(r, c.x(), c.y(), c.z());
                Vec expected = new Vec(origin.x() + v[0], origin.y() + v[1], origin.z() + v[2]);
                assertEquals(expected, Rot.world(origin, r, c), "rotation " + r + " cell " + c);
            }
        }
    }
}
