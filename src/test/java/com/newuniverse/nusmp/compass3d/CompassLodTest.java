package com.newuniverse.nusmp.compass3d;

import com.newuniverse.nusmp.perf.PerfBudget;
import com.newuniverse.nusmp.perf.VfxQuality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the lag audit (docs/perf/lag_audit_1.md) put levels of detail, a per-frame quad budget and server-wide caps on the 3D compass. The decisions are pure, so the numbers are pinned here: the
 * levels only get coarser with distance, the budget is never passed (except by the compasses next to the camera, at the cheapest level), the meshes of the lower levels are real meshes (sane
 * quads, a fraction of the full model), and the worst-case frame of twenty casters in view is a fraction of what it was.
 */
class CompassLodTest {
    @Test
    void levelsOnlyGetCoarserWithDistanceAndEveryQualityEndsInNothing() {
        for (VfxQuality q : VfxQuality.values()) {
            int last = CompassLod.FULL;
            for (double d = 0; d <= 150; d += 0.5) {
                int l = CompassLod.level(d * d, q.detail, CompassLod.COMPASS);
                assertTrue(l >= last, q + " went back to a finer level at " + d);
                last = l;
            }
            assertEquals(CompassLod.CULLED, last, q + " draws a compass 150 blocks away");
        }
        assertEquals(CompassLod.FULL, CompassLod.level(17.9 * 17.9, 1.0f, CompassLod.COMPASS));
        assertEquals(CompassLod.MEDIUM, CompassLod.level(18.1 * 18.1, 1.0f, CompassLod.COMPASS));
        assertEquals(CompassLod.LOW, CompassLod.level(36.1 * 36.1, 1.0f, CompassLod.COMPASS));
        assertEquals(CompassLod.CULLED, CompassLod.level(56.1 * 56.1, 1.0f, CompassLod.COMPASS));
        // a weaker quality switches earlier
        assertTrue(CompassLod.level(30 * 30, 0.5f, CompassLod.COMPASS) > CompassLod.level(30 * 30, 1.0f, CompassLod.COMPASS));
        // the arrows and routes have their own bands
        assertEquals(CompassLod.FULL, CompassLod.level(23 * 23, 1.0f, CompassLod.ARROW));
        assertEquals(CompassLod.CULLED, CompassLod.level(65 * 65, 1.0f, CompassLod.ARROW));
        assertEquals(CompassLod.MEDIUM, CompassLod.level(30 * 30, 1.0f, CompassLod.ROUTE));
    }

    @Test
    void behindTheCameraIsCulledButNotTheNeighbourhood() {
        assertTrue(CompassLod.behind(-5.0));
        assertFalse(CompassLod.behind(-3.9));
        assertFalse(CompassLod.behind(10.0));
    }

    @Test
    void theLowerLevelsAreRealMeshesAndCostLess() {
        int full = CompassMesh.compassQuads(0, true, true), mid = CompassMesh.compassQuads(1, true, true), low = CompassMesh.compassQuads(2, true, true);
        assertTrue(full > 700, "the full model keeps its quads: " + full);
        assertTrue(mid < full * 0.45, "level 1 is well under half: " + mid + " of " + full);
        assertTrue(low < full * 0.05 && low > 0, "level 2 is the dial disc only: " + low + " of " + full);
        assertTrue(CompassMesh.arrowQuads(1) < CompassMesh.arrowQuads(0) / 2 && CompassMesh.arrowQuads(1) > 8);
        assertEquals(0, CompassMesh.arrowQuads(2));
        int[] cost = CompassLod.compassCost(true, true);
        assertTrue(cost[0] > cost[1] && cost[1] > cost[2] && cost[2] > 0);
        int[] arrow = CompassLod.arrowCost();
        assertTrue(arrow[0] > arrow[1] && arrow[1] > arrow[2] && arrow[2] > 0);
        // every part of every level draws something sane: the sink sees finite quads with unit normals and uv in 0..1
        for (int lod = 0; lod < CompassMesh.LODS; lod++) {
            final int[] count = {0};
            CompassMesh.Sink check = (layer, d, o) -> {
                count[0]++;
                for (int k = 0; k < CompassMesh.STRIDE; k++) assertTrue(Float.isFinite(d[o + k]));
                float l = (float) Math.sqrt(d[o + 20] * d[o + 20] + d[o + 21] * d[o + 21] + d[o + 22] * d[o + 22]);
                assertEquals(1f, l, 1e-3f);
                for (int k = 0; k < 4; k++) {
                    assertTrue(d[o + k * 5 + 3] >= -1e-4f && d[o + k * 5 + 3] <= 1.0001f);
                    assertTrue(d[o + k * 5 + 4] >= -1e-4f && d[o + k * 5 + 4] <= 1.0001f);
                }
            };
            for (int layer = CompassMesh.RIM; layer <= CompassMesh.STAR; layer++) CompassMesh.bodyLayer(check, lod, layer);
            CompassMesh.needle(check, lod);
            CompassMesh.glass(check, lod);
            CompassMesh.arrow(check, lod);
            assertTrue(count[0] > 0 || lod >= 2, "level " + lod + " drew nothing");
        }
        // level 0 through the new entry points is exactly the old model
        final int[] n0 = {0};
        CompassMesh.bodyLayer((l, d, o) -> n0[0]++, 0, CompassMesh.RIM);
        CompassMesh.bodyLayer((l, d, o) -> n0[0]++, 0, CompassMesh.FACE);
        CompassMesh.bodyLayer((l, d, o) -> n0[0]++, 0, CompassMesh.STAR);
        assertEquals(CompassMesh.bodyQuads(), n0[0]);
        final int[] n1 = {0};
        CompassMesh.body((l, d, o) -> n1[0]++);
        assertEquals(CompassMesh.bodyQuads(), n1[0]);
    }

    @Test
    void theFrameBudgetHoldsForACrowdOfCasters() {
        int[] cost = CompassLod.compassCost(true, true);
        for (VfxQuality q : VfxQuality.values()) {
            PerfBudget b = new PerfBudget("test", CompassLod.frameQuads(q));
            long spent = 0;
            for (int i = 0; i < 20; i++) {                                           // twenty casters between 8 and 46 blocks away
                double dist = 8 + 2 * i;
                int want = CompassLod.level(dist * dist, q.detail, CompassLod.COMPASS);
                int got = CompassLod.admit(b, 7L, want, dist * dist, cost);
                assertTrue(got >= want, "never finer than asked");
                if (got < CompassLod.CULLED) spent += cost[got];
            }
            assertTrue(spent <= CompassLod.frameQuads(q), q + " spent " + spent + " of " + CompassLod.frameQuads(q));
            assertEquals(spent, b.used());
        }
        // the old worst case: twenty full compasses = 20 * cost[0] quads; the new one is the cap
        assertTrue(20 * cost[0] > 3 * CompassLod.frameQuads(VfxQuality.HIGH) / 2 + 1000, "the cap really is below the old worst case");
    }

    @Test
    void aCompassNextToTheCameraIsNeverDropped() {
        int[] cost = CompassLod.compassCost(true, true);
        PerfBudget b = new PerfBudget("test", 10);                                   // a budget that fits nothing
        assertEquals(CompassLod.LOW, CompassLod.admit(b, 1L, CompassLod.FULL, 25.0, cost), "within 6 blocks: the cheapest level");
        assertEquals(CompassLod.CULLED, CompassLod.admit(b, 1L, CompassLod.FULL, 49.0, cost), "beyond 6 blocks it yields");
        assertEquals(CompassLod.CULLED, CompassLod.admit(b, 1L, CompassLod.CULLED, 1.0, cost), "a culled one stays culled");
    }

    @Test
    void theBudgetRollsOnANewFrame() {
        int[] cost = CompassLod.compassCost(true, true);
        PerfBudget b = new PerfBudget("test", cost[0]);
        assertEquals(CompassLod.FULL, CompassLod.admit(b, 1L, CompassLod.FULL, 100.0, cost));
        assertTrue(CompassLod.admit(b, 1L, CompassLod.FULL, 100.0, cost) > CompassLod.FULL, "the second one in the same frame is degraded");
        assertEquals(CompassLod.FULL, CompassLod.admit(b, 2L, CompassLod.FULL, 100.0, cost), "the next frame starts fresh");
    }

    @Test
    void routeDashesShrinkWithTheLevel() {
        assertEquals(10, CompassLod.routeDashes(10, CompassLod.FULL));
        assertEquals(5, CompassLod.routeDashes(10, CompassLod.MEDIUM));
        assertEquals(2, CompassLod.routeDashes(3, CompassLod.MEDIUM));
        assertEquals(0, CompassLod.routeDashes(10, CompassLod.LOW));
        int[] c = CompassLod.routeCost(10);
        assertTrue(c[0] > c[1] && c[1] > c[2]);
    }

    @Test
    void theServerWideCapsAreAboveThePerPlayerOnesAndTheWordThrottleHolds() {
        assertTrue(CompassRules.globalCap(CompassRules.V_NEEDLE) >= CompassRules.CAP_NEEDLES * 2);
        assertTrue(CompassRules.globalCap(CompassRules.V_SEAL) >= CompassRules.CAP_SEALS);
        assertTrue(CompassRules.globalCap(CompassRules.V_TURN) >= CompassRules.CAP_TURNS);
        assertTrue(CompassRules.globalCap(CompassRules.V_ROUTE) >= CompassRules.CAP_ROUTES);
        assertEquals(0, CompassRules.globalCap(99));
        int a = CompassRules.packMain(10f, 5f, CompassRules.MODE_IDLE, 0), b = CompassRules.packMain(40f, 5f, CompassRules.MODE_IDLE, 0);
        int mode = CompassRules.packMain(40f, 5f, CompassRules.MODE_GUARD, 0), fade = CompassRules.packMain(40f, 5f, CompassRules.MODE_GUARD, 9);
        assertFalse(CompassRules.shouldSetWord(a, a, 50, 0), "nothing changed");
        assertFalse(CompassRules.shouldSetWord(a, b, 51, 50), "aim only, one tick after the last send");
        assertFalse(CompassRules.shouldSetWord(a, b, 52, 50));
        assertTrue(CompassRules.shouldSetWord(a, b, 53, 50), "aim only, three ticks later");
        assertTrue(CompassRules.shouldSetWord(b, mode, 51, 50), "a new mode is sent at once");
        assertTrue(CompassRules.shouldSetWord(mode, fade, 51, 50), "the fade is sent at once");
        // the aim mask covers exactly the yaw and pitch bits
        int onlyAim = CompassRules.packMain(100f, -30f, CompassRules.MODE_ATTACK, 7);
        int otherAim = CompassRules.packMain(200f, 60f, CompassRules.MODE_ATTACK, 7);
        assertEquals(0, (onlyAim ^ otherAim) & ~CompassRules.AIM_MASK, "mode and fade bits are outside the aim mask");
    }

    @Test
    void theAllocationFreeDashesAreTheOldDashes() {
        for (int n = 2; n <= 14; n++) {
            for (float t = 0; t < 300; t += 3.7f) {
                for (int k = 0; k < n; k++) {
                    float[] d = CompassRules.dash(k, n, t);
                    assertEquals(d[0], CompassRules.dashStart(k, n, t), 0f);
                    assertEquals(d[1], CompassRules.dashEnd(k, n, t), 0f);
                }
            }
        }
    }
}
