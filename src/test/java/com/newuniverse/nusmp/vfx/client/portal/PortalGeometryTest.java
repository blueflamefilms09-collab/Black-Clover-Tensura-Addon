package com.newuniverse.nusmp.vfx.client.portal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the portal's counts, budgets, opening animation and shell parallax are plain numbers that are easy to break and cheap to pin. */
class PortalGeometryTest {
    @Test
    void tiersGetCoarserAndSmaller() {
        for (int t = 1; t < PortalSpec.TIERS.length; t++) {
            assertTrue(PortalSpec.tier(t).gateVertices() < PortalSpec.tier(t - 1).gateVertices());
            assertTrue(PortalSpec.tier(t).ringSegments() <= PortalSpec.tier(t - 1).ringSegments());
        }
    }

    @Test
    void shellCountsStayInThreeToFive() {
        for (PortalSpec s : PortalSpec.TIERS) assertTrue(s.shells() >= 3 && s.shells() <= 5);
        assertEquals(5, PortalSpec.tier(0).shells());
    }

    @Test
    void smallestTierFitsTheSharedBuffer() {
        assertTrue(PortalSpec.tier(2).maxVertices() <= 400);
        assertEquals(2, PortalSpec.fitting(0, 400).tier());
        assertEquals(0, PortalSpec.fitting(0, 100000).tier());
        assertEquals(1, PortalSpec.fitting(1, 100000).tier());
    }

    @Test
    void ringSegmentsAndSectorTablesMatch() {
        for (PortalSpec s : PortalSpec.TIERS) {
            PortalMesh m = PortalMesh.of(s);
            assertSame(m, PortalMesh.of(s));                      // cached, not rebuilt
            for (int n : new int[]{s.ringSegments(), s.shellSectors(), s.horizonSectors()}) {
                assertEquals(1f, m.cos(n, 0), 1e-6f);
                assertEquals(1f, m.cos(n, n), 1e-5f);             // the table closes the circle
                assertEquals(0f, m.sin(n, n), 1e-5f);
            }
        }
    }

    @Test
    void gateOpensThenHoldsThenCollapses() {
        float dur = 100;
        assertEquals(0f, PortalMath.openW(0, dur), 1e-6f);
        assertEquals(0f, PortalMath.openH(0, dur), 1e-6f);
        assertTrue(PortalMath.openH(4, dur) > PortalMath.openW(4, dur));           // the slit grows tall before it widens
        assertEquals(1f, PortalMath.openW(PortalMath.OPEN_TICKS, dur), 1e-3f);
        assertEquals(1f, PortalMath.openH(50, dur), 1e-6f);
        assertEquals(1f, PortalMath.openW(50, dur), 1e-6f);
        float w = PortalMath.openW(dur - 4, dur), h = PortalMath.openH(dur - 4, dur);
        assertTrue(w < h);                                                          // width collapses first
        assertEquals(0f, PortalMath.openW(dur, dur), 1e-5f);
        assertEquals(0f, PortalMath.openH(dur, dur), 1e-5f);
        assertEquals(0f, PortalMath.closing(dur - 8, dur), 1e-6f);
        assertEquals(1f, PortalMath.closing(dur, dur), 1e-6f);
    }

    @Test
    void shellsShrinkAndSinkDeeper() {
        for (int n = 3; n <= 5; n++) {
            float prev = 1f, prevDepth = 0f;
            for (int i = 0; i < n; i++) {
                float s = PortalMath.shellScale(i, n), d = PortalMath.shellDepth(i);
                assertTrue(s < prev && s > 0.2f);
                assertTrue(d > prevDepth);
                prev = s; prevDepth = d;
            }
        }
    }

    @Test
    void parallaxIsCentredHeadOnSymmetricAndClamped() {
        float[] o = new float[2];
        PortalMath.parallax(0, 0, 4, 1, 1, 1, o);
        assertEquals(0f, o[0], 1e-6f);
        assertEquals(0f, o[1], 1e-6f);
        PortalMath.parallax(1, 0.5f, 4, 1, 10, 10, o);
        float x = o[0], y = o[1];
        assertTrue(x > 0 && y > 0);
        PortalMath.parallax(1, 0.5f, -4, 1, 10, 10, o);                            // from behind the gate: same slide
        assertEquals(x, o[0], 1e-6f);
        assertEquals(y, o[1], 1e-6f);
        PortalMath.parallax(-1, -0.5f, 4, 1, 10, 10, o);
        assertEquals(-x, o[0], 1e-6f);
        PortalMath.parallax(50, 50, 0.5f, 3, 0.3f, 0.2f, o);                       // far to the side: stays inside the ellipse
        float e = (o[0] / 0.3f) * (o[0] / 0.3f) + (o[1] / 0.2f) * (o[1] / 0.2f);
        assertTrue(e <= 1.0001f);
        float[] a = new float[2], b = new float[2];
        PortalMath.parallax(2, 0, 3, 0.4f, 9, 9, a);
        PortalMath.parallax(2, 0, 3, 1.6f, 9, 9, b);
        assertTrue(b[0] > a[0]);                                                    // deeper shells slide more
    }

    @Test
    void swirlSpinsTheCoreFasterThanTheRim() {
        assertTrue(PortalMath.swirlTurns(0f, 10) > PortalMath.swirlTurns(1f, 10));
        assertEquals(0f, PortalMath.swirlTurns(0.5f, 0), 1e-9f);
    }

    @Test
    void jitterIsDeterministicAndBounded() {
        for (int k = 0; k < 50; k++) {
            float j = PortalMath.boltJitter(99L, 2, k, 13f);
            assertTrue(j >= -1f && j <= 1f);
            assertEquals(j, PortalMath.boltJitter(99L, 2, k, 13f));
            int g = PortalMath.glyph(5L, k);
            assertTrue(g >= 0 && g < 4);
        }
    }

    /** Counts what the composer hands to a sink and checks every tier stays inside its own vertex count. */
    @Test
    void composerEmitsWithinTheSpecBudgetAndNothingWhenClosed() {
        PortalComposer c = new PortalComposer();
        for (PortalSpec s : PortalSpec.TIERS) {
            int[] n = {0};
            PortalSink count = (pass, p, u0, u1, vi, vo, ci, co) -> n[0]++;
            PortalFrame f = new PortalFrame();
            f.nx = 0; f.nz = 1; f.rx = 1; f.uy = 1;
            f.hw = 0.9f; f.hh = 1.35f; f.cz = 5; f.age = 50; f.duration = 100; f.fade = 1; f.seed = 3;
            f.viewerFromCamera();
            c.compose(PortalMesh.of(s), f, count);
            assertTrue(n[0] > 0 && n[0] <= s.gateQuads(), "tier " + s.tier() + " emitted " + n[0]);
            n[0] = 0;
            f.hw = 0f;
            c.compose(PortalMesh.of(s), f, count);
            assertEquals(0, n[0]);
        }
    }

    @Test
    void shaderModeSkipsTheBackdrop() {
        PortalComposer c = new PortalComposer();
        boolean[] backdrop = {false};
        PortalSink sink = (pass, p, u0, u1, vi, vo, ci, co) -> { if (pass == PortalPass.BACKDROP) backdrop[0] = true; };
        PortalFrame f = new PortalFrame();
        f.nz = 1; f.rx = 1; f.uy = 1; f.hw = 1; f.hh = 1; f.cz = 4; f.fade = 1; f.age = 30; f.duration = 100;
        f.shader = true;
        c.compose(PortalMesh.of(PortalSpec.tier(0)), f, sink);
        assertFalse(backdrop[0]);
        f.shader = false;
        c.compose(PortalMesh.of(PortalSpec.tier(0)), f, sink);
        assertTrue(backdrop[0]);
    }
}
