package com.newuniverse.nusmp.scalecast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Scale model is plain geometry shared by the game's renderer and the preview tool, so its shape is tested without the game: every part has quads, every quad is finite with a unit
 * normal and texture coordinates in 0..1, the levels of detail only get cheaper and stay inside the budget, the beam is symmetric, the pan insides face up, the chains run from the top ring to
 * the lugs, and the numbers the renderer and the generator share are the ones the geometry uses.
 */
class ScaleMeshTest {
    private static float[] raw(String p) { return ScaleMesh.raw(p); }

    @Test
    void everyPartIsWellFormed() {
        for (String part : ScaleMesh.PART_NAMES) {
            float[] q = raw(part);
            assertTrue(q.length > 0 && q.length % ScaleMesh.STRIDE == 0, part + " has " + q.length + " floats");
            for (int o = 0; o < q.length; o += ScaleMesh.STRIDE) {
                for (int k = 0; k < 20; k++) assertTrue(Float.isFinite(q[o + k]), part + " has a non finite number");
                float l = (float) Math.sqrt(q[o + 20] * q[o + 20] + q[o + 21] * q[o + 21] + q[o + 22] * q[o + 22]);
                assertEquals(1f, l, 1e-3f, part + " normal");
                for (int k = 0; k < 4; k++) {
                    float u = q[o + k * 5 + 3], v = q[o + k * 5 + 4];
                    assertTrue(u >= 0f && u <= 1f && v >= 0f && v <= 1f, part + " uv " + u + "," + v);
                }
            }
        }
    }

    @Test
    void levelsOfDetailGetCheaperAndStayInsideTheBudget() {
        int q0 = ScaleMesh.quads(0), q1 = ScaleMesh.quads(1), q2 = ScaleMesh.quads(2);
        assertTrue(q0 > q1 && q1 > q2 && q2 > 0, q0 + " " + q1 + " " + q2);
        assertTrue(q0 <= 1500, "the full Scale is at most 1500 quads: " + q0);
        assertTrue(q1 <= 450, "the medium Scale is at most 450: " + q1);
        assertTrue(q2 <= 100, "the low Scale is at most 100: " + q2);
        for (String base : new String[]{"beam", "column", "pan", "enamel", "chain", "gem", "orb"}) {
            int full = raw(base).length;
            String mid = base + "1";
            for (String n : ScaleMesh.PART_NAMES) if (n.equals(mid)) assertTrue(raw(n).length < full, mid + " is cheaper than " + base);
        }
        assertEquals(3, ScaleMesh.LODS);
        assertTrue(ScaleMesh.quads(-5) == q0 && ScaleMesh.quads(9) == q2, "out of range levels are clamped");
    }

    @Test
    void theBeamIsSymmetricAboutThePivotAndSpansItsHalfLength() {
        float[] q = raw("beam");
        double sumX = 0, minX = 9, maxX = -9;
        int n = 0;
        for (int o = 0; o < q.length; o += ScaleMesh.STRIDE)
            for (int k = 0; k < 4; k++) {
                float x = q[o + k * 5];
                sumX += x;
                n++;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
            }
        assertEquals(0.0, sumX / n, 0.01, "the beam is centred on the pivot");
        assertEquals(-minX, maxX, 1e-3);
        assertEquals(ScaleMesh.BEAM_HALF + 0.02, maxX, 0.01, "the knobs end at the beam's half length");
        // the hook rings hang at the hook point
        boolean hook = false;
        for (int o = 0; o < q.length; o += ScaleMesh.STRIDE) {
            float x = q[o], y = q[o + 1];
            if (Math.abs(x - ScaleMesh.HOOK_X) < 0.06f && y < ScaleMesh.HOOK_Y - 0.03f) hook = true;
        }
        assertTrue(hook, "a hook ring hangs under the right end");
    }

    @Test
    void thePanInsideFacesUpAndTheChainsRunFromTheTopRingToTheLugs() {
        float[] in = raw("enamel");
        for (int o = 0; o < in.length; o += ScaleMesh.STRIDE) assertTrue(in[o + 21] > 0f, "an enamel quad faces down");
        float[] chain = raw("chain");
        float top = -9, bottom = 9, maxR = 0;
        for (int o = 0; o < chain.length; o += ScaleMesh.STRIDE)
            for (int k = 0; k < 4; k++) {
                float x = chain[o + k * 5], y = chain[o + k * 5 + 1], z = chain[o + k * 5 + 2];
                top = Math.max(top, y);
                bottom = Math.min(bottom, y);
                maxR = Math.max(maxR, (float) Math.hypot(x, z));
            }
        assertEquals(ScaleMesh.CHAIN_LEN, top, 0.05f, "the chains start at the top ring");
        assertTrue(bottom > -0.01f && bottom < 0.08f, "and end at the lugs on the rim: " + bottom);
        assertEquals(ScaleMesh.PAN_R, maxR, 0.04f, "at the radius of the rim");
        float[] pan = raw("pan");
        float low = 9, high = -9, rim = 0;
        for (int o = 0; o < pan.length; o += ScaleMesh.STRIDE)
            for (int k = 0; k < 4; k++) {
                low = Math.min(low, pan[o + k * 5 + 1]);
                high = Math.max(high, pan[o + k * 5 + 1]);
                if (pan[o + k * 5 + 1] < 0.2f) rim = Math.max(rim, (float) Math.hypot(pan[o + k * 5], pan[o + k * 5 + 2]));
            }
        assertEquals(-ScaleMesh.PAN_DEPTH, low, 0.01f, "the bowl is as deep as the layout says");
        assertEquals(ScaleMesh.CHAIN_LEN + 0.046f, high, 0.03f, "the top ring crowns the pan");
        assertEquals(0.45f, rim, 0.03f, "the rim lip is the widest part");
    }

    @Test
    void theStandReachesItsFootAndTheOrbIsTheUnitSphere() {
        float[] col = raw("column");
        float low = 9;
        for (int o = 0; o < col.length; o += ScaleMesh.STRIDE) for (int k = 0; k < 4; k++) low = Math.min(low, col[o + k * 5 + 1]);
        assertEquals(ScaleMesh.COLUMN_BOTTOM, low, 0.01f);
        float[] orb = raw("orb");
        for (int o = 0; o < orb.length; o += ScaleMesh.STRIDE)
            for (int k = 0; k < 4; k++) {
                float x = orb[o + k * 5], y = orb[o + k * 5 + 1], z = orb[o + k * 5 + 2];
                assertEquals(1f, (float) Math.sqrt(x * x + y * y + z * z), 1e-3f, "orb vertex on the unit sphere");
            }
        // the pointer is a thin plate in front of the stand, pointing down by its length
        float[] ptr = raw("pointer");
        float tip = 9;
        for (int o = 0; o < ptr.length; o += ScaleMesh.STRIDE) for (int k = 0; k < 4; k++) tip = Math.min(tip, ptr[o + k * 5 + 1]);
        assertEquals(-ScaleMesh.POINTER_LEN, tip, 1e-3f);
        assertFalse(ScaleMesh.GAUGE_R < ScaleMesh.POINTER_LEN, "the ticks lie just outside the tip of the needle");
    }

    @Test
    void theSinkSeesEveryQuadOfAPartOnItsLayer() {
        int[] count = new int[ScaleMesh.LAYERS];
        ScaleMesh.Sink s = (layer, d, o) -> count[layer]++;
        ScaleMesh.beam(s, 0);
        ScaleMesh.beamGlow(s, 0);
        ScaleMesh.column(s, 0);
        ScaleMesh.pointer(s, 0);
        ScaleMesh.gem(s, 0);
        ScaleMesh.pan(s, 0);
        ScaleMesh.chains(s, 0);
        ScaleMesh.orb(s, 0);
        assertEquals(raw("beam").length / ScaleMesh.STRIDE, count[ScaleMesh.RUNES], "the rune pass is the beam again");
        assertTrue(count[ScaleMesh.GOLD] > 0 && count[ScaleMesh.ENAMEL] > 0 && count[ScaleMesh.CHAIN] > 0 && count[ScaleMesh.GEM] > 0);
        int[] low = new int[ScaleMesh.LAYERS];
        ScaleMesh.Sink l = (layer, d, o) -> low[layer]++;
        ScaleMesh.beamGlow(l, 2);
        ScaleMesh.pointer(l, 2);
        ScaleMesh.gem(l, 2);
        ScaleMesh.orb(l, 2);
        for (int c : low) assertEquals(0, c, "the lowest level draws no glow pass, no needle, no jewel, no orbs");
    }
}
