package com.newuniverse.nusmp.visual.grip;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1.111.0 (docs/weapon_grip.md): the grip finder on small hand-built meshes: a sword (pommel, handle, guard, blade), a spear, a held item, and the order of its sources.
 * Pure: the vertices are boxes made here, no Minecraft types.
 */
class GripFinderTest {
    /** Adds a box (x0..x1, y0..y1, z0..z1) as six quads (four vertices each, xyz). */
    static void box(List<float[]> out, float x0, float y0, float z0, float x1, float y1, float z1) {
        float[][] c = {{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}};
        int[][] f = {{0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 5, 4}, {3, 2, 6, 7}, {0, 3, 7, 4}, {1, 2, 6, 5}};
        for (int[] q : f) for (int i : q) out.add(c[i]);
    }

    static float[] flat(List<float[]> v) {
        float[] a = new float[v.size() * 3];
        for (int i = 0; i < v.size(); i++) System.arraycopy(v.get(i), 0, a, i * 3, 3);
        return a;
    }

    /** A sword 100 units long: pommel y 0-4 (6 wide), handle y 4-24 (4 wide), guard y 24-28 (30 wide), blade y 28-100 (10 wide). */
    static List<float[]> sword() {
        List<float[]> v = new ArrayList<>();
        box(v, -3, 0, -3, 3, 4, 3);
        box(v, -2, 4, -2, 2, 24, 2);
        box(v, -15, 24, -2, 15, 28, 2);
        box(v, -5, 28, -1, 5, 100, 1);
        return v;
    }

    @Test
    void theHandleOfASwordIsFoundBetweenThePommelAndTheGuard() {
        List<float[]> v = sword();
        GripFinder.Grip g = GripFinder.find(flat(v), v.size(), "longsword", 0.2f, Float.NaN, Float.NaN, null);
        assertNotNull(g);
        assertEquals(GripFinder.Source.HANDLE, g.source());
        assertTrue(g.y() > 4f && g.y() < 24f, "the grip is on the handle, not on the pommel, the guard or the blade: y = " + g.y());
        assertEquals(14f, g.y(), 3.5f, "about the middle of the handle");
        assertEquals(g.y() / 100f, g.fraction(), 1e-4f);
        assertEquals(0f, g.x(), 1e-4f);
    }

    @Test
    void theOldPlacementHeldTheGuardOrTheBladeNotTheHandle() {
        // the hand of a katana-class json is 0.26 of the way up the mesh, a greatsword's 0.245 (GripMath.oldHandFraction): here that is the guard, not the handle
        List<float[]> v = sword();
        float oldHand = 0.26f * 100f;
        assertTrue(oldHand >= 24f && oldHand <= 28f, "the old hand point lies on the guard of this sword");
        GripFinder.Grip g = GripFinder.find(flat(v), v.size(), "longsword", 0.2f, Float.NaN, Float.NaN, null);
        assertTrue(Math.abs(g.y() - oldHand) > 8f, "the new grip is clear of where the old hand was");
    }

    @Test
    void aSpearIsHeldOnItsShaftAtTheClassFraction() {
        List<float[]> v = new ArrayList<>();
        box(v, -1, 0, -1, 1, 300, 1);
        box(v, -4, 300, -1, 4, 340, 1);
        GripFinder.Grip g = GripFinder.find(flat(v), v.size(), "spear", 0.33f, Float.NaN, Float.NaN, null);
        assertEquals(GripFinder.Source.CLASS, g.source());
        assertEquals(0.33f, g.fraction(), 1e-4f);
        assertEquals(0.33f * 340f, g.y(), 0.5f);
    }

    @Test
    void theSourcesComeInTheStatedOrder() {
        List<float[]> v = sword();
        float[] xyz = flat(v);
        assertEquals(GripFinder.Source.GRIPXYZ, GripFinder.find(xyz, v.size(), "longsword", 0.2f, 0.3f, 0.2f, new float[]{1, 50, 2}).source());
        GripFinder.Grip st = GripFinder.find(xyz, v.size(), "longsword", 0.2f, 0.3f, 0.2f, new float[]{1, 50, 2});
        assertEquals(50f, st.y(), 1e-4f);
        assertEquals(1f, st.x(), 1e-4f);
        assertEquals(GripFinder.Source.OVERRIDE, GripFinder.find(xyz, v.size(), "longsword", 0.2f, 0.3f, 0.2f, null).source(), "a per-weapon fraction beats the mesh's grip line");
        assertEquals(GripFinder.Source.DIRECTIVE, GripFinder.find(xyz, v.size(), "longsword", 0.2f, Float.NaN, 0.2f, null).source(), "the mesh's grip line beats the handle finder");
        assertEquals(GripFinder.Source.HANDLE, GripFinder.find(xyz, v.size(), "longsword", 0.2f, Float.NaN, Float.NaN, null).source());
        assertEquals(GripFinder.Source.CLASS, GripFinder.find(xyz, v.size(), "staff", 0.4f, Float.NaN, Float.NaN, null).source(), "a class that is not handle-mode never reads a handle");
    }

    @Test
    void aSwordLikeGripStaysInTheLowerHalfAndAwayFromTheVeryEnd() {
        List<float[]> v = sword();
        float[] xyz = flat(v);
        assertEquals(GripFinder.FRAC_MAX, GripFinder.find(xyz, v.size(), "longsword", 0.2f, 0.9f, Float.NaN, null).fraction(), 1e-6f, "never above the middle");
        assertEquals(GripFinder.FRAC_MIN, GripFinder.find(xyz, v.size(), "longsword", 0.2f, 0.0f, Float.NaN, null).fraction(), 1e-6f, "never on the very end");
    }

    @Test
    void aHeldItemIsHeldAtTheCentreOfItsBoxAndABowAtItsMiddle() {
        List<float[]> v = new ArrayList<>();
        box(v, 10, 0, -4, 20, 30, 4);
        GripFinder.Grip item = GripFinder.find(flat(v), v.size(), "handitem", 0.5f, Float.NaN, 0.1f, null);
        assertEquals(GripFinder.Source.CLASS, item.source(), "a held item ignores a grip line (it has none)");
        assertEquals(15f, item.y(), 1e-4f);
        assertEquals(15f, item.x(), 1e-4f, "centred sideways over the whole box");
        GripFinder.Grip bow = GripFinder.find(flat(v), v.size(), "bow", 0.5f, Float.NaN, Float.NaN, null);
        assertEquals(0.5f, bow.fraction(), 1e-6f);
    }

    @Test
    void anEmptyMeshHasNoGrip() {
        assertNull(GripFinder.find(new float[0], 0, "longsword", 0.2f, Float.NaN, Float.NaN, null));
        List<float[]> flat = new ArrayList<>();
        box(flat, 0, 5, 0, 1, 5, 1);                                                  // no length
        assertNull(GripFinder.find(flat(flat), flat.size(), "longsword", 0.2f, Float.NaN, Float.NaN, null));
    }

    @Test
    void aTaperingBladeIsNotMistakenForAGuard() {
        List<float[]> v = new ArrayList<>();
        box(v, -2, 0, -2, 2, 20, 2);                                                  // handle
        box(v, -4, 20, -1, 4, 24, 1);                                                 // small guard, twice the handle
        box(v, -3, 24, -1, 3, 60, 1);
        GripFinder.Grip g = GripFinder.find(flat(v), v.size(), "dagger", 0.2f, Float.NaN, Float.NaN, null);
        assertEquals(GripFinder.Source.HANDLE, g.source());
        assertTrue(g.y() < 20f, "on the handle: " + g.y());
    }
}
