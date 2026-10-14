package com.newuniverse.nusmp.boss.refill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The second health bar: once per mode, eased to the maximum, shielded while it runs, saved and restored, glow quantised. */
class PhaseRefillTest {
    @Test
    void refillsToMaxOverTheTicksAndNeverDips() {
        PhaseRefill r = new PhaseRefill();
        assertTrue(r.begin("gates", 200f, 1000f, 40));
        assertTrue(r.active());
        float last = 200f;
        int ticks = 0;
        while (r.active()) {
            float hp = r.tick();
            assertTrue(hp >= last, "health fell at tick " + ticks);
            assertTrue(hp <= 1000f);
            last = hp;
            ticks++;
        }
        assertEquals(40, ticks);
        assertEquals(1000f, last, 0f, "the last tick is exactly the maximum");
        assertEquals(-1f, r.tick(), "idle");
    }

    @Test
    void aModeRefillsOnlyOncePerFight() {
        PhaseRefill r = new PhaseRefill();
        assertTrue(r.begin("gates", 10f, 100f, 5));
        assertFalse(r.begin("gates", 10f, 100f, 5), "running");
        while (r.active()) r.tick();
        assertFalse(r.begin("gates", 10f, 100f, 5), "already done");
        assertTrue(r.done("gates"));
        assertTrue(r.begin("last_stand", 10f, 100f, 5), "another mode is a new refill");
        r.reset();
        assertTrue(r.begin("gates", 10f, 100f, 5), "a new fight may refill again");
    }

    @Test
    void oneRefillRunsAtATime() {
        PhaseRefill r = new PhaseRefill();
        assertTrue(r.begin("a", 1f, 10f, 10));
        assertFalse(r.canBegin("b"));
        assertFalse(r.begin("b", 1f, 10f, 10));
        assertFalse(r.done("b"), "a refused mode is not recorded");
    }

    @Test
    void rejectsNonsense() {
        PhaseRefill r = new PhaseRefill();
        assertFalse(r.begin(null, 1f, 10f, 10));
        assertFalse(r.begin(" ", 1f, 10f, 10));
        assertFalse(r.begin("x", 1f, 0f, 10));
        assertFalse(r.begin("x", Float.NaN, 10f, 10));
        assertFalse(r.active());
    }

    @Test
    void aFullBossConsumesTheModeAndEndsAtOnce() {
        PhaseRefill r = new PhaseRefill();
        assertTrue(r.begin("m", 500f, 500f, 0));
        assertEquals(500f, r.tick(), 0f);
        assertFalse(r.active());
        assertTrue(r.done("m"));
    }

    @Test
    void healthAbovePlanIsNeverLowered() {
        PhaseRefill r = new PhaseRefill();
        r.begin("m", 900f, 1000f, 10);
        for (int i = 0; i < 10; i++) assertTrue(r.tick() >= 900f);
    }

    @Test
    void retargetFollowsAnewMaximum() {
        PhaseRefill r = new PhaseRefill();
        r.begin("m", 0f, 100f, 4);
        r.tick();
        r.retarget(200f);
        float hp = 0;
        while (r.active()) hp = r.tick();
        assertEquals(200f, hp, 0f);
    }

    @Test
    void finishJumpsToTheMaximum() {
        PhaseRefill r = new PhaseRefill();
        r.begin("m", 5f, 50f, 40);
        r.tick();
        assertEquals(50f, r.finish(), 0f);
        assertFalse(r.active());
        assertEquals(-1f, r.finish(), 0f);
    }

    @Test
    void saveAndLoadKeepWhatWasDone() {
        PhaseRefill a = new PhaseRefill();
        a.begin("gates", 100f, 1000f, 40);
        for (int i = 0; i < 15; i++) a.tick();
        PhaseRefill b = new PhaseRefill();
        b.load(a.save());
        assertTrue(b.active());
        assertEquals("gates", b.mode());
        assertEquals(25, b.ticksLeft());
        assertFalse(b.begin("gates", 1f, 10f, 5), "a reload grants no second refill");
        float x = 0, y = 0;
        while (a.active()) x = a.tick();
        while (b.active()) y = b.tick();
        assertEquals(x, y, 0f);
        PhaseRefill c = new PhaseRefill();
        c.load(b.save());
        assertFalse(c.active());
        assertTrue(c.done("gates"));
        assertFalse(c.begin("gates", 1f, 10f, 5));
    }

    @Test
    void loadToleratesGarbage() {
        PhaseRefill r = new PhaseRefill();
        r.load(null);
        r.load(new PhaseRefill.Saved(null, null, -4, -1, Float.NaN, -3f));
        assertFalse(r.active());
        assertEquals(0, r.doneCount());
        r.load(new PhaseRefill.Saved("a,,b, c", "b", 10, 99, 1f, 5f));
        assertFalse(r.active(), "left > total is corrupt");
        assertEquals(3, r.doneCount());
    }

    @Test
    void glowFlashesFirstThenSettles() {
        PhaseRefill r = new PhaseRefill();
        assertEquals(0f, r.glow(), 0f);
        r.begin("m", 1f, 100f, 40);
        assertEquals(1f, r.glow(), 1e-6f, "bright at the start");
        float mid = 0;
        for (int i = 0; i < 20; i++) { r.tick(); mid = r.glow(); }
        assertTrue(mid > 0f && mid < 0.5f, "a shimmer while it fills: " + mid);
        while (r.active()) { r.tick(); assertTrue(r.glow() <= 1f); }
        assertEquals(0f, r.glow(), 0f);
    }

    @Test
    void glowStepsAreFew() {
        java.util.Set<Float> seen = new java.util.TreeSet<>();
        for (float g = -0.5f; g <= 1.5f; g += 0.01f) seen.add(PhaseRefill.glowStep(g));
        assertEquals(PhaseRefill.GLOW_STEPS + 1, seen.size());
    }

    @Test
    void liftBlendsTowardWhite() {
        assertEquals(0x102030, PhaseRefill.lift(0x102030, 0f));
        int full = PhaseRefill.lift(0x000000, 1f);
        assertEquals(0xCC, full & 255, "80% toward white");
        assertEquals(0xCCCCCC, full);
        assertEquals(PhaseRefill.lift(0xFF0000, 5f), PhaseRefill.lift(0xFF0000, 1f), "clamped");
    }
}
