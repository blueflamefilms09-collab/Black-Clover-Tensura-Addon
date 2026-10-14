package com.newuniverse.nusmp.client.relics.item;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The orientation convention (mesh -x is forward) and its detector, on synthetic axes, swords and a hammer. */
class WeaponOrientationTest {
    /** Adds an axis-aligned box as six quads (counter-clockwise from outside); normals are the face normals. */
    static void box(List<float[]> out, float x0, float x1, float y0, float y1, float z0, float z1) {
        float[][] c = {{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}};
        int[][] f = {{0, 3, 2, 1}, {4, 5, 6, 7}, {0, 4, 7, 3}, {1, 2, 6, 5}, {0, 1, 5, 4}, {3, 7, 6, 2}};
        float[][] n = {{0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}};
        for (int q = 0; q < 6; q++) {
            for (int k = 0; k < 4; k++) {
                float[] p = c[f[q][k]];
                out.add(new float[]{p[0], p[1], p[2], 0, 0, n[q][0], n[q][1], n[q][2]});
            }
        }
    }

    /** A haft 60 mesh units long (a few LONG quads: the profile must still be measured) with an axe head of the given x span at the top. */
    static List<float[]> axe(float headX0, float headX1) {
        List<float[]> v = new ArrayList<>();
        box(v, -1, 1, 0, 60, -1, 1);
        box(v, headX0, headX1, 40, 58, -0.5f, 0.5f);
        return v;
    }

    static final float SC = 0.02f;                       // blocks per mesh unit

    @Test
    void aHeadOnTheMinusXSideIsForward() {
        WeaponOrientation.Measures m = WeaponOrientation.measure(axe(-14, 0), SC);
        assertFalse(m.empty);
        assertTrue(m.imbalance < -0.05, "mass toward -x: " + m.imbalance);
        assertTrue(m.reachNeg > 0.25 && m.reachPos < 0.05, m.reachNeg + " / " + m.reachPos);
        assertTrue(WeaponOrientation.detect(m, "battleaxe", 45).isEmpty());
    }

    @Test
    void aHeadOnThePlusXSideFacesThePlayer() {
        WeaponOrientation.Measures m = WeaponOrientation.measure(axe(0, 14), SC);
        assertTrue(m.imbalance > 0.05, "mass toward +x: " + m.imbalance);
        Set<WeaponOrientation.Flag> f = WeaponOrientation.detect(m, "handaxe", 45);
        assertEquals(Set.of(WeaponOrientation.Flag.HEAD_TOWARD_PLAYER), f);
    }

    @Test
    void aDoubleBladedHeadHasNoSide() {
        WeaponOrientation.Measures m = WeaponOrientation.measure(axe(-12, 12), SC);
        assertEquals(0.0, m.imbalance, 0.012);
        assertTrue(WeaponOrientation.detect(m, "greataxe", 45).isEmpty(), "balanced about the haft");
    }

    @Test
    void mirroringPutsTheHeadForwardAndKeepsTheWinding() {
        List<float[]> v = axe(0, 14);
        float[] before3 = v.get(3).clone();
        float px = (float) WeaponOrientation.measure(v, SC).axisX;
        WeaponOrientation.mirrorX(v, px);
        WeaponOrientation.Measures m = WeaponOrientation.measure(v, SC);
        assertTrue(m.imbalance < -0.05);
        assertTrue(WeaponOrientation.detect(m, "handaxe", 45).isEmpty());
        // the second vertex of the first quad is now the former fourth one, mirrored: a reversed winding of a mirrored quad
        assertEquals(2 * px - before3[0], v.get(1)[0], 1e-5f);
        assertEquals(before3[1], v.get(1)[1], 1e-5f);
        assertEquals(before3[2], v.get(1)[2], 1e-5f);
        // normals: the +x face of the haft box is now a -x face
        boolean sawMinusX = false;
        for (float[] p : v) if (p[5] < -0.9f) sawMinusX = true;
        assertTrue(sawMinusX);
        // mirroring twice is the identity (up to float noise)
        List<float[]> w = axe(0, 14);
        List<float[]> orig = new ArrayList<>();
        for (float[] p : w) orig.add(p.clone());
        WeaponOrientation.mirrorX(w, 0.0);
        WeaponOrientation.mirrorX(w, 0.0);
        for (int i = 0; i < w.size(); i++) assertArrayEquals(orig.get(i), w.get(i), 1e-4f);
    }

    @Test
    void aHeadHeavyWeaponWithItsWeightAtTheButtIsTipDown() {
        List<float[]> v = new ArrayList<>();
        box(v, -1, 1, 0, 60, -1, 1);
        box(v, -20, 0, 2, 22, -2, 2);                         // a heavy head at the BUTT end
        WeaponOrientation.Measures m = WeaponOrientation.measure(v, SC);
        assertTrue(m.centroid < WeaponOrientation.HEAD_HEAVY_MAX, "centroid " + m.centroid);
        assertTrue(WeaponOrientation.detect(m, "battleaxe", 45).contains(WeaponOrientation.Flag.TIP_DOWN));
        WeaponOrientation.rotateZ(v, 180, (m.y0 + m.y1) / 2);
        WeaponOrientation.Measures r = WeaponOrientation.measure(v, SC);
        assertTrue(r.centroid > 0.6, "centroid after the turn " + r.centroid);
        assertFalse(WeaponOrientation.detect(r, "battleaxe", 45).contains(WeaponOrientation.Flag.TIP_DOWN));
    }

    @Test
    void aPlaceAnglePointingDownIsTipDownForWeaponsButNotForItems() {
        WeaponOrientation.Measures m = WeaponOrientation.measure(axe(-14, 0), SC);
        assertTrue(WeaponOrientation.detect(m, "longsword", -135).contains(WeaponOrientation.Flag.TIP_DOWN));
        assertFalse(WeaponOrientation.detect(m, "item", -32).contains(WeaponOrientation.Flag.TIP_DOWN));
        assertFalse(WeaponOrientation.detect(m, "longsword", 45).contains(WeaponOrientation.Flag.TIP_DOWN));
        assertTrue(WeaponOrientation.detect(m, null, 45).isEmpty(), "an unknown class: nothing is claimed");
    }

    @Test
    void aCurvedBladeBendsTheWayOfItsClass() {
        // a blade whose upper half is shifted by +x: tip toward +x
        List<float[]> v = new ArrayList<>();
        box(v, -1, 1, 0, 30, -0.5f, 0.5f);
        box(v, 4, 6, 30, 60, -0.5f, 0.5f);
        WeaponOrientation.Measures m = WeaponOrientation.measure(v, SC);
        assertTrue(m.curve > 0.05, "curve " + m.curve);
        assertTrue(WeaponOrientation.detect(m, "katana", 45).contains(WeaponOrientation.Flag.MIRRORED), "a katana's tip bends toward its edge, -x");
        assertFalse(WeaponOrientation.detect(m, "sabre", 45).contains(WeaponOrientation.Flag.MIRRORED), "a sabre's toward its spine, +x");
        WeaponOrientation.mirrorX(v, m.axisX);
        WeaponOrientation.Measures r = WeaponOrientation.measure(v, SC);
        assertTrue(r.curve < -0.05);
        assertFalse(WeaponOrientation.detect(r, "katana", 45).contains(WeaponOrientation.Flag.MIRRORED));
        assertTrue(WeaponOrientation.detect(r, "sabre", 45).contains(WeaponOrientation.Flag.MIRRORED));
    }

    @Test
    void anEmptyMeshHasNoMeasuresAndNoFlags() {
        WeaponOrientation.Measures m = WeaponOrientation.measure(new ArrayList<>(), SC);
        assertTrue(m.empty);
        assertTrue(WeaponOrientation.detect(m, "handaxe", 45).isEmpty());
    }

    @Test
    void theLengthIsTheYExtentTimesTheScale() {
        WeaponOrientation.Measures m = WeaponOrientation.measure(axe(-14, 0), SC);
        assertEquals(60 * SC, m.length, 1e-5);
        assertEquals(14 * SC, m.maxWidth, 0.02, "the widest slice is the head");
        assertTrue(m.widestAt > 0.6, "the head is high up: " + m.widestAt);
    }
}
