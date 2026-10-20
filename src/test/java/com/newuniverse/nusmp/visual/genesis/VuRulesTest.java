package com.newuniverse.nusmp.visual.genesis;

import com.newuniverse.nusmp.perf.VfxQuality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The decisions of the upgraded Genesis weapon renderer (distance, quality, animation maths, bursts), without the game. */
class VuRulesTest {
    private static final String SPEC = """
            {"id":"t","mesh":"t","lod":{"mesh":"t_lod","near":12.0,"far":24.0},
             "groups":{"orbit":{"pivot":[0,50,0],"anim":[{"kind":"spin","axis":[0,1,0],"dps":90.0},{"kind":"bob","axis":[0,1,0],"amp":2.0,"period":40.0}],"lodSkip":true,
                       "draw":{"from_scale":0.0,"delay":4,"ticks":10}},
                       "arcs":{"pivot":[0,0,0],"anim":[{"kind":"flicker","min":0.0,"max":1.0,"rate":6.0},{"kind":"breathe","min":0.5,"max":1.0,"period":60.0}],"tint":"8A3CFF","alpha":0.8,"lodSkip":true}},
             "layers":[{"id":"a","kind":"orbit_shards","group":"orbit"},{"id":"m","kind":"motes","particle":"dust","rgb":"9A4AFF","rate":0.5,"where":"blade"}],
             "idle":[{"id":"i","kind":"group_spin","group":"orbit"}],
             "draw":{"ticks":16,"flash":"9A4AFF"},"swing":{"trail":"x","rgb":"9A4AFF","rgb2":"0C0A12","length":1.35,"width":0.3,"life":9},
             "acts":{"devour_soul":{"burst":"soul_drain","pattern":"inward","rgb":"9A4AFF","ring":3.0,"particles":26,"sound":"nusmp:item.x"}},
             "sound_loop":"nusmp:item.genesis.t.loop"}""";

    @Test
    void specParsesEveryField() {
        VuSpec s = VuSpec.parse(SPEC);
        assertEquals("t", s.id);
        assertEquals("t_lod", s.lodMesh);
        assertEquals(12.0f, s.lodNear, 1e-6f);
        assertEquals(2, s.groups.size());
        VuSpec.Group g = s.groups.get("orbit");
        assertEquals(50f, g.py, 1e-6f);
        assertEquals(2, g.anim.size());
        assertEquals("spin", g.anim.get(0).kind);
        assertEquals(1f, g.anim.get(0).ay, 1e-6f);
        assertEquals(4, g.draw.delay());
        assertEquals(0x8A3CFF, s.groups.get("arcs").tintRgb);
        assertEquals(2, s.layers.size());
        assertEquals(1, s.particleLayers().size());
        assertEquals(0x9A4AFF, s.particleLayers().get(0).rgb());
        assertEquals("inward", s.act("devour_soul").pattern());
        assertEquals("nusmp:item.genesis.t.loop", s.soundLoop);
        assertEquals(16, s.draw.ticks());
        assertTrue(s.swing.length() > 1f);
    }

    @Test
    void lodAndAdditiveByDistance() {
        assertFalse(VuRules.useLod(11.9 * 11.9, false, 12f));
        assertTrue(VuRules.useLod(12.1 * 12.1, false, 12f));
        assertFalse(VuRules.useLod(1e6, true, 12f), "first person always draws the full mesh");
        assertFalse(VuRules.additiveCulled(23.9 * 23.9, false, 24f));
        assertTrue(VuRules.additiveCulled(24.1 * 24.1, false, 24f));
        assertFalse(VuRules.additiveCulled(1e6, true, 24f));
    }

    @Test
    void qualityDropsTheExtras() {
        assertFalse(VuRules.haloAllowed(VfxQuality.LOW));
        assertTrue(VuRules.haloAllowed(VfxQuality.MEDIUM));
        assertFalse(VuRules.animationAllowed(VfxQuality.LOW));
        assertFalse(VuRules.flaresAllowed(VfxQuality.MEDIUM));
        assertTrue(VuRules.flaresAllowed(VfxQuality.HIGH));
        assertEquals(0, VuRules.particleBudget(VfxQuality.LOW));
        assertTrue(VuRules.particleBudget(VfxQuality.MEDIUM) < VuRules.particleBudget(VfxQuality.HIGH));
        assertTrue(VuRules.particleBudget(VfxQuality.HIGH) <= VuRules.MAX_PARTICLES_PER_HOLDER);
    }

    @Test
    void theFlagFallback() {
        assertFalse(VuRules.useUpgrade(false, true));
        assertFalse(VuRules.useUpgrade(true, false));
        assertTrue(VuRules.useUpgrade(true, true));
    }

    @Test
    void theDrawAnimationEasesAndRestarts() {
        assertEquals(0f, VuRules.drawProgress(-3f, 10), 0f);
        assertEquals(1f, VuRules.drawProgress(10f, 10), 0f);
        assertEquals(0.5f, VuRules.drawProgress(5f, 10), 1e-6f);
        assertEquals(0f, VuRules.easeOutBack(0f), 0f);
        assertEquals(1f, VuRules.easeOutBack(1f), 0f);
        float peak = 0f;
        for (int i = 0; i <= 100; i++) peak = Math.max(peak, VuRules.easeOutBack(i / 100f));
        assertTrue(peak > 1.0f && peak < 1.2f, "a small overshoot: " + peak);
        assertTrue(VuRules.newDraw(-1, 100, 6), "the first sighting is a draw");
        assertFalse(VuRules.newDraw(100, 103, 6), "a short gap is the same draw");
        assertTrue(VuRules.newDraw(100, 120, 6), "a long gap starts a new draw");
        assertTrue(VuRules.newDraw(100, 50, 6), "the clock going back starts a new draw");
    }

    @Test
    void theVeinWaveAndTheFlickerStayInRange() {
        for (int i = 0; i < 400; i++) {
            float w = VuRules.veinWave(i * 0.7f, i * 0.31f, 0.045f, 0.012f);
            assertTrue(w >= 0.3f - 1e-5f && w <= 1.0f + 1e-5f, "wave " + w);
            float f = VuRules.flicker(i * 0.5f, 6f, 0.1f, 0.9f, 42);
            assertTrue(f >= 0.1f - 1e-5f && f <= 0.9f + 1e-5f, "flicker " + f);
        }
        assertEquals(VuRules.flicker(37.5f, 6f, 0f, 1f, 3), VuRules.flicker(37.5f, 6f, 0f, 1f, 3), 0f, "deterministic");
    }

    @Test
    void aSpinningGroupTurnsAboutItsAxisAndBobs() {
        VuSpec s = VuSpec.parse(SPEC);
        VuSpec.Group g = s.groups.get("orbit");
        VuPose p = new VuPose();
        p.eval(g, 0f, 1e9f);
        assertTrue(p.identity() || Math.abs(p.r[0] - 1f) < 1e-5f);
        p.eval(g, 20f, 1e9f);                                                            // one second at 90 degrees a second: a quarter turn about y
        assertEquals(0f, p.r[0], 1e-4f);
        assertEquals(1f, p.r[2], 1e-4f);
        assertEquals(-1f, p.r[6], 1e-4f);
        assertEquals(0.0f, p.oy, 1e-4f, "the bob is back at the middle after half its period of 40 ticks");
        p.eval(g, 10f, 1e9f);
        assertEquals(2.0f, p.oy, 1e-4f, "the bob is at the top after a quarter of its period");
    }

    @Test
    void theDrawAnimationGrowsAGroup() {
        VuSpec.Group g = VuSpec.parse(SPEC).groups.get("orbit");
        VuPose p = new VuPose();
        p.eval(g, 0f, 0f);
        assertEquals(0f, p.scale, 1e-6f);
        assertEquals(0f, p.alpha, 1e-6f);
        p.eval(g, 0f, 4f + 10f);
        assertEquals(1f, p.scale, 1e-6f);
        assertEquals(1f, p.alpha, 1e-6f);
    }

    @Test
    void everyBurstPatternGivesFinitePointsInsideTheRadius() {
        float[] out = new float[6];
        for (String pat : VuBurst.PATTERNS) {
            for (int n : new int[]{1, 7, 40}) {
                for (int i = 0; i < n; i++) {
                    VuBurst.point(pat, i, n, 4.0f, i / (float) n, out);
                    for (float v : out) assertTrue(Float.isFinite(v), pat);
                    assertTrue(Math.abs(out[0]) <= 4.01f && Math.abs(out[2]) <= 4.0f * 1.5f && Math.abs(out[1]) <= 4.01f, pat + " " + out[0] + "," + out[1] + "," + out[2]);
                }
            }
        }
        assertTrue(VuBurst.known("inward"));
        assertFalse(VuBurst.known("squiggle"));
    }

    @Test
    void theSwingTipArcsAcrossTheFront() {
        float[] o = new float[3];
        VuRules.swingTip(0f, 2f, o);
        float firstSide = o[0];
        VuRules.swingTip(1f, 2f, o);
        assertTrue(firstSide > 0f && o[0] < 0f, "from one side to the other");
        VuRules.swingTip(0.5f, 2f, o);
        assertTrue(o[2] > 1.5f, "straight ahead in the middle");
    }
}
