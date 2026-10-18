package com.newuniverse.nusmp.client.heroweapon.carriage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Why: the carriage's model is read from the weapon mesh text the art builder ships, and a reader that mis-counts one number of a line would put every vertex in
 * the wrong place without any error. These tests feed {@link MeshBake} a mesh small enough to check by hand: the layers go to the right lists (body, glow, and the
 * void layer is dropped), a quad with one normal and a quad with a normal per vertex are both read, the glow line is read, the box is mapped to the entity's frame
 * at the wanted length with the nose forward, the roof up and the floor on the ground, a broken or empty mesh is refused, and the fallback wagon fits the carriage's
 * own hitbox and seats.
 */
class MeshBakeTest {
    // a box 12 long (y 0..12), 6 tall (x -6..0, the roof at -6, the ground at 0), 8 wide (z -4..4): the two side walls, as quads with one normal each
    private static final String WALL_A = "q 0 -6 0 -4 0 0  -6 12 -4 1 0  0 12 -4 1 1  0 0 -4 0 1  0 0 -1";
    private static final String WALL_B = "q 0 -6 0 4 0 0  0 0 4 0 1  0 12 4 1 1  -6 12 4 1 0  0 0 1";

    private static List<String> mesh(String... extra) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>(List.of("# a comment line", "place 0 0 0 1 0", WALL_A, WALL_B));
        out.addAll(List.of(extra));
        return out;
    }

    @Test
    void theBoxIsMappedNoseForwardRoofUpAndFloorOnTheGround() {
        MeshBake.Baked b = MeshBake.bake(mesh(), 3.2);
        assertNotNull(b);
        assertEquals(8, b.body.size(), "two quads of four vertices");
        double s = 3.2 / 12.0;
        float[] v = b.body.get(1);                                  // wall A, the vertex (-6, 12, -4): the roof, the nose, the +x side
        assertEquals(4 * s, v[0], 1e-5, "left: -(z - centre z) x scale");
        assertEquals(6 * s, v[1], 1e-5, "up: (max x - x) x scale: the roof is 6 units high");
        assertEquals(6 * s, v[2], 1e-5, "forward: (y - centre y) x scale: the nose is 6 units ahead of the middle");
        float[] ground = b.body.get(3);                             // (0, 0, -4): the ground, the tail
        assertEquals(0.0, ground[1], 1e-5, "the floor rests on the ground");
        assertEquals(-6 * s, ground[2], 1e-5, "the tail is behind the middle");
        double lo = 1e9, hi = -1e9;
        for (float[] q : b.body) {
            lo = Math.min(lo, q[2]);
            hi = Math.max(hi, q[2]);
        }
        assertEquals(3.2, hi - lo, 1e-5, "the carriage is the wanted length");
    }

    @Test
    void normalsFollowTheSameRotationAndTheUvsAreKept() {
        MeshBake.Baked b = MeshBake.bake(mesh(), 3.2);
        float[] n = b.body.get(0);                                  // wall A: normal (0, 0, -1) in the mesh = +x (left) in the entity
        assertEquals(1.0, n[5], 1e-6);
        assertEquals(0.0, n[6], 1e-6);
        assertEquals(0.0, n[7], 1e-6);
        assertEquals(1.0f, b.body.get(1)[3], 0.0f, "u is kept as it was written");
        assertEquals(0.0f, b.body.get(1)[4], 0.0f, "v is kept as it was written");
    }

    @Test
    void layersGoWhereTheyBelongAndTheGlowLineIsRead() {
        MeshBake.Baked b = MeshBake.bake(mesh(
                "glow FFC247 0.10 0.50 1.00",
                "q 2 -1 5 -1 0 0  -1 6 -1 1 0  -2 6 1 1 1  -2 5 1 0 1  -1 0 0",
                "q 1 -3 3 -2 0 0  -3 4 -2 1 0  -3 4 2 1 1  -3 3 2 0 1  -1 0 0",
                "qn 2 -1 5 -1 0 0 -1 0 0  -1 6 -1 1 0 -1 0 0  -2 6 1 1 1 0 -1 0  -2 5 1 0 1 0 -1 0"), 3.2);
        assertNotNull(b);
        assertEquals(8, b.body.size(), "the void layer (1) is dropped and the body (0) keeps its two quads");
        assertEquals(8, b.glow.size(), "one quad and one per-vertex-normal quad on layer 2");
        assertEquals(0xFFC247, b.glowRgb);
        assertEquals(0.10f, b.glowSpeed, 1e-6f);
        assertEquals(0.50f, b.glowMin, 1e-6f);
        assertEquals(1.00f, b.glowMax, 1e-6f);
        float[] smooth = b.glow.get(6);                             // the third vertex of the qn quad has its own normal (0, -1, 0) in the mesh
        assertEquals(0.0, smooth[5], 1e-6);
        assertEquals(0.0, smooth[6], 1e-6);
        assertEquals(-1.0, smooth[7], 1e-6, "a mesh normal of -y (the tail) is -z (behind) in the entity");
        assertTrue(!b.fallback);
    }

    @Test
    void aBrokenOrEmptyMeshIsRefused() {
        assertNull(MeshBake.bake(List.of("# nothing", "place 0 0 0 1 0"), 3.2), "no quads: nothing to draw");
        assertNull(MeshBake.bake(List.of("q 1 -6 0 -4 0 0  -6 12 -4 1 0  0 12 -4 1 1  0 0 -4 0 1  0 0 -1"), 3.2), "only a void quad: no body");
        assertNull(MeshBake.bake(List.of("q 0 0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 1"), 3.2), "a body with no volume");
        boolean threw = false;
        try {
            MeshBake.bake(List.of("q 0 -6 0 -4 0 0  -6 12 -4 1 0"), 3.2);
        } catch (RuntimeException e) {
            threw = true;
        }
        assertTrue(threw, "a short quad line is an error the caller catches (it then draws the fallback)");
    }

    @Test
    void theFallbackWagonFitsTheHitboxAndTheSeats() {
        MeshBake.Baked f = MeshBake.fallback();
        assertTrue(f.fallback);
        assertEquals(0, f.body.size() % 4, "whole quads");
        double lo = 1e9, hi = -1e9, width = 0, floor = 1e9;
        for (float[] v : f.body) {
            lo = Math.min(lo, v[2]);
            hi = Math.max(hi, v[2]);
            width = Math.max(width, Math.abs(v[0]));
            floor = Math.min(floor, v[1]);
        }
        assertEquals(3.2, hi - lo, 0.2, "about as long as the real model");
        assertTrue(width <= 1.1 + 1e-6, "inside the 2.2 block hitbox, wheels included: " + width);
        assertEquals(0.0, floor, 1e-6, "its wheels stand on the ground");
    }
}
