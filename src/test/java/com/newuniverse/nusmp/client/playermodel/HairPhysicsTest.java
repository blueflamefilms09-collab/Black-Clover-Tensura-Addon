package com.newuniverse.nusmp.client.playermodel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The pure maths of the hair: the spring chain, the pull it is given, the palette and the poses. */
class HairPhysicsTest {
    private static void run(HairSpring s, int ticks, float tp, float tr) {
        for (int i = 0; i < ticks; i++) s.step(1f, tp, tr);
    }

    @Test
    void aSpringSettlesOnItsTargetAndNeverExceedsTheLimit() {
        HairSpring s = new HairSpring(3, HairSpring.Params.DEFAULT);
        run(s, 400, 0.4f, -0.3f);
        float tp = 0, tr = 0;
        for (int i = 0; i < 3; i++) {
            tp += s.pitch(i);
            tr += s.roll(i);
        }
        assertEquals(0.4f, tp, 0.02f, "the chain's total pitch settles on the pull");
        assertEquals(-0.3f, tr, 0.02f);
        HairSpring w = new HairSpring(4, HairSpring.Params.HEAVY);
        run(w, 200, 50f, -50f);
        for (int i = 0; i < 4; i++) {
            assertTrue(Math.abs(w.pitch(i)) <= HairSpring.Params.HEAVY.maxAngle() + 1e-4f);
            assertTrue(Math.abs(w.roll(i)) <= HairSpring.Params.HEAVY.maxAngle() + 1e-4f);
        }
    }

    @Test
    void theTipLagsBehindTheRootAndTheSharesSumToOne() {
        HairSpring s = new HairSpring(4, HairSpring.Params.DEFAULT);
        s.step(1f, 0.6f, 0f);
        assertTrue(s.pitch(0) > s.pitch(3) * 0.2f);
        float sum = 0f;
        for (int i = 0; i < 4; i++) sum += HairSpring.share(i, 4);
        assertEquals(1f, sum, 1e-5f);
        assertTrue(HairSpring.share(3, 4) > HairSpring.share(0, 4), "the tip takes the biggest share of the swing");
    }

    @Test
    void anyFrameTimeIsStable() {
        for (float dt : new float[]{0.01f, 0.5f, 1f, 3.9f, 40f, -2f, Float.NaN}) {
            HairSpring s = new HairSpring(3, HairSpring.Params.DEFAULT);
            for (int i = 0; i < 50; i++) s.step(Float.isNaN(dt) ? 0f : dt, 0.5f, 0.2f);
            for (int i = 0; i < 3; i++) {
                assertFalse(Float.isNaN(s.pitch(i)) || Float.isInfinite(s.pitch(i)), "dt " + dt);
                assertTrue(Math.abs(s.pitch(i)) <= 0.7f + 1e-4f);
            }
        }
    }

    @Test
    void aKickWhipsTheLockAndItSettlesAgain() {
        HairSpring s = new HairSpring(3, HairSpring.Params.DEFAULT);
        s.kick(0.2f, 0.4f);
        s.step(1f, 0f, 0f);
        assertTrue(Math.abs(s.pitch(0)) + Math.abs(s.pitch(1)) > 0.01f);
        run(s, 300, 0f, 0f);
        for (int i = 0; i < 3; i++) assertEquals(0f, s.pitch(i), 0.01f);
    }

    @Test
    void windInertiaTurnAndGravity() {
        float[] o = new float[2];
        HairPhysics.Tuning t = new HairPhysics.Tuning(1.5f, 9f, 0.9f, 0.85f, false, 0f, 0f);
        HairPhysics.target(HairPhysics.Input.STILL, t, o);
        assertEquals(0f, o[0], 1e-6f);
        assertEquals(0f, o[1], 1e-6f);
        HairPhysics.target(new HairPhysics.Input(0, 0, 0, 0.28f, 0, 0, 0, 0), t, o);
        assertTrue(o[0] > 0.3f && o[0] <= HairPhysics.MAX_WIND, "running streams the lock to the back");
        HairPhysics.target(new HairPhysics.Input(0, 0, 0, 0, 0.05f, 0, 0, 0), t, o);
        assertTrue(o[0] > 0f, "speeding up lags the lock to the back");
        HairPhysics.target(new HairPhysics.Input(0, 0, 0, 0, -0.05f, 0, 0, 0), t, o);
        assertTrue(o[0] < 0f, "slowing down throws it forward");
        HairPhysics.target(new HairPhysics.Input(0, 0, 0, 0, 0, 0, 0.4f, 0), t, o);
        assertTrue(o[1] > 0f, "a turn to the left swings the tip to the right");
        HairPhysics.target(new HairPhysics.Input(0.6f, 0.2f, 0, 0, 0, 0, 0, 0), t, o);
        assertTrue(o[0] < 0f && o[1] < 0f, "a nod and a tilt of the head are met by the lock staying hung");
        HairPhysics.Tuning body = t.asBody();
        HairPhysics.target(new HairPhysics.Input(0.6f, 0.2f, 0.3f, 0, 0, 0, 0, 0), body, o);
        assertEquals(-0.3f * Math.max(0.85f, 0.6f), o[0], 1e-5f, "a lock on the body follows the body's lean, not the head");
        HairPhysics.target(new HairPhysics.Input(0, 0, 0, 5f, 5f, 5f, 5f, 0), t, o);
        assertTrue(o[0] <= HairPhysics.MAX_WIND + HairPhysics.MAX_INERTIA + 1e-4f);
    }

    @Test
    void landingsAndJumpsKick() {
        assertEquals(0f, HairPhysics.landingKick(-0.05f));
        assertTrue(HairPhysics.landingKick(-0.5f) > HairPhysics.landingKick(-0.25f));
        assertTrue(HairPhysics.landingKick(-9f) <= 0.3f);
        assertEquals(0f, HairPhysics.jumpKick(0.1f));
        assertTrue(HairPhysics.jumpKick(0.42f) > 0f);
    }

    @Test
    void theIdleSwayIsAliveButSmall() {
        float[] o = new float[2];
        HairPhysics.Tuning t = HairPhysics.Tuning.HEAD_LOCK;
        float lo = 9f, hi = -9f;
        for (int a = 0; a < 400; a++) {
            HairPhysics.target(new HairPhysics.Input(0, 0, 0, 0, 0, 0, 0, a), t, o);
            lo = Math.min(lo, o[0]);
            hi = Math.max(hi, o[0]);
        }
        assertTrue(hi - lo > 0.04f && hi - lo < 0.2f, "sway " + (hi - lo));
    }

    @Test
    void thePaletteGivesALiftedHighlightAndADarkerShade() {
        for (int rgb : new int[]{0x000000, 0x1C1A22, 0x5A3C28, 0xE0C070, 0xFFFFFF, 0x3C6ED0}) {
            HairPalette.Tint t = HairPalette.of(rgb);
            assertEquals(rgb, t.base());
            assertTrue(lum(t.highlight()) >= lum(t.base()) - 120, Integer.toHexString(rgb));
            assertTrue(lum(t.shade()) <= lum(t.base()) + 80, Integer.toHexString(rgb));
        }
        assertTrue(lum(HairPalette.of(0x0A0A0A).highlight()) > lum(0x0A0A0A) + 8, "black hair still shines");
        assertEquals(0xB02A2A, HairPalette.parse("red", 0));
        assertEquals(0x123456, HairPalette.parse("#123456", 0));
        assertEquals(7, HairPalette.parse("nonsense", 7));
        assertEquals(0x808080, HairPalette.mix(0x000000, 0xFFFFFF, 0.5f));
    }

    private static int lum(int c) { return ((c >> 16) & 255) * 3 + ((c >> 8) & 255) * 6 + (c & 255); }

    @Test
    void posesFadeInSmoothlyAndOnlyPullTheirParts() {
        PoseState p = new PoseState();
        PosePatterns.cast(p, 0f, false, 0f);
        assertEquals(0f, p.blend[PoseState.ARM_R], 1e-6f, "no pose at progress 0");
        p.clear();
        PosePatterns.cast(p, 1f, true, 3f);
        assertTrue(p.blend[PoseState.ARM_R] == 1f && p.target[PoseState.ARM_R][0] < -2f, "a big cast raises the arms overhead");
        assertEquals(0f, p.blend[PoseState.LEG_R], 1e-6f);
        assertEquals(-2.9f, p.apply(PoseState.ARM_R, 0, 0.7f), 0.3f);
        p.clear();
        assertFalse(p.any());
        PosePatterns.slump(p, 1f, 0f);
        assertTrue(p.target[PoseState.HEAD][0] > 0.3f, "a slumped head hangs forward");
        p.clear();
        p.pull(PoseState.ARM_R, 0.3f, 1f, 0f, 0f);
        p.pull(PoseState.ARM_R, 0.8f, 2f, 0f, 0f);
        p.pull(PoseState.ARM_R, 0.5f, 9f, 0f, 0f);
        assertEquals(2f, p.target[PoseState.ARM_R][0], "the strongest pull wins");
    }

    @Test
    void theExtraClipFallsBackDownTheChain() {
        java.util.Set<String> some = java.util.Set.of("idle", "walk", "cast");
        assertEquals("death", ExtraClip.pick(java.util.Set.of("death", "idle"), true, false, 0, false, 0f));
        assertEquals("idle", ExtraClip.pick(some, true, false, 0, false, 0f), "no death clip: idle");
        assertEquals("cast", ExtraClip.pick(some, false, false, 2, false, 0f), "cast_big falls back to cast");
        assertEquals("walk", ExtraClip.pick(some, false, false, 0, false, 0.3f), "run falls back to walk");
        assertEquals("idle", ExtraClip.pick(java.util.Set.of(), false, false, 0, true, 0.3f));
        assertEquals(0.4f, ExtraClip.time(2.4f, 1f, true), 1e-5f);
        assertEquals(1f, ExtraClip.time(2.4f, 1f, false));
    }
}
