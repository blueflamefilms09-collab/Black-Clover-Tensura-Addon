package com.newuniverse.nusmp.client.zogratis;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The clip clock of the Zogratis bodies: priorities, pacing, the ultimate's release and settling, walking without sliding. */
class ZogratisPoseTest {
    /** The lengths of a Triad body: one-shot cast_big, looping cast. */
    private static final ZogratisPose.Clips TRIAD = clips(Map.of("idle", 3.2f, "walk", 1.1f, "run", 0.66f, "attack", 0.9f, "cast", 1.6f,
            "cast_big", 2.4f, "hurt", 0.45f, "transform", 3.0f, "death", 2.8f), false);
    /** Lucius's cast_big loops. */
    private static final ZogratisPose.Clips LOOPING = clips(Map.of("idle", 4f, "walk", 1.1f, "run", 0.62f, "attack", 0.9f, "cast", 1.2f,
            "cast_big", 2.0f, "hurt", 0.45f, "transform", 3.2f, "death", 2.8f), true);

    private static ZogratisPose.Clips clips(Map<String, Float> lengths, boolean bigLoops) {
        return new ZogratisPose.Clips() {
            @Override public float length(String clip) { return lengths.getOrDefault(clip, 0f); }
            @Override public boolean loops(String clip) { return clip.equals("cast_big") ? bigLoops : clip.equals("idle") || clip.equals("walk") || clip.equals("run") || clip.equals("cast"); }
        };
    }

    private static ZogratisPose.Frame frame(int tick) {
        ZogratisPose.Frame f = new ZogratisPose.Frame();
        f.tick = tick;
        f.age = tick;
        return f;
    }

    @Test
    void standingStillIsIdleOnTheAgeClock() {
        ZogratisPose p = new ZogratisPose();
        ZogratisPose.Frame f = frame(40);
        p.update(f, TRIAD);
        assertEquals("idle", p.clip);
        assertEquals(2.0f, p.seconds, 1e-4);
    }

    @Test
    void walkingIsPacedByGroundCoveredNotByTime() {
        ZogratisPose p = new ZogratisPose();
        for (int t = 0; t < 12; t++) {
            ZogratisPose.Frame f = frame(t);
            f.rawSpeed = 0.1f;
            p.update(f, TRIAD);
        }
        assertEquals("walk", p.clip);
        assertEquals(1.1f, p.travel(), 1e-4, "eleven finished ticks of 0.1 block");
        float expected = (1.1f / 1.6f - (float) Math.floor(1.1f / 1.6f)) * 1.1f;
        assertEquals(expected, p.seconds, 1e-4);
        ZogratisPose.Frame f = frame(11);                 // a later frame of the same tick: halfway through it, half a step more
        f.age = 11.5f;
        f.rawSpeed = 0.1f;
        p.update(f, TRIAD);
        assertEquals(1.15f, p.travel(), 1e-4, "the partial tick adds a share of that tick's step");
        for (int t = 12; t < 24; t++) {                   // it stops: the last step ends, the legs settle into idle, no more ground is covered
            f = frame(t);
            f.rawSpeed = 0f;
            p.update(f, TRIAD);
        }
        assertEquals("idle", p.clip);
        assertEquals(1.2f, p.travel(), 1e-4);
    }

    @Test
    void aSlowerWalkCoversLessGroundPerStepOfTheClip() {
        ZogratisPose slow = new ZogratisPose(), quick = new ZogratisPose();
        for (int t = 0; t < 30; t++) {
            ZogratisPose.Frame a = frame(t), b = frame(t);
            a.rawSpeed = 0.05f;
            b.rawSpeed = 0.10f;
            slow.update(a, TRIAD);
            quick.update(b, TRIAD);
        }
        assertEquals("walk", slow.clip);
        assertEquals(2f * slow.travel(), quick.travel(), 1e-3);
    }

    @Test
    void runningStartsAboveTheThresholdAndKeepsUntilWellBelow() {
        ZogratisPose p = new ZogratisPose();
        for (int t = 0; t < 30; t++) { ZogratisPose.Frame f = frame(t); f.rawSpeed = 0.35f; p.update(f, TRIAD); }
        assertEquals("run", p.clip);
        for (int t = 30; t < 60; t++) { ZogratisPose.Frame f = frame(t); f.rawSpeed = 0.19f; p.update(f, TRIAD); }
        assertEquals("run", p.clip, "between the thresholds it keeps running");
        for (int t = 60; t < 100; t++) { ZogratisPose.Frame f = frame(t); f.rawSpeed = 0.08f; p.update(f, TRIAD); }
        assertEquals("walk", p.clip);
        for (int t = 100; t < 140; t++) { ZogratisPose.Frame f = frame(t); f.rawSpeed = 0f; p.update(f, TRIAD); }
        assertEquals("idle", p.clip);
    }

    @Test
    void aWarpCoversNoGroundAndDoesNotStartARun() {
        ZogratisPose p = new ZogratisPose();
        for (int t = 0; t < 40; t++) {
            ZogratisPose.Frame f = frame(t);
            f.rawSpeed = t == 20 ? 6f : t == 30 ? Float.NaN : t == 31 ? -2f : 0f;    // a portal, then garbage
            p.update(f, TRIAD);
            assertEquals("idle", p.clip, "tick " + t);
            assertFalse(p.running(), "tick " + t);
        }
        assertEquals(0f, p.travel(), 1e-6);
        assertEquals(0f, p.speed(), 1e-6);
        for (int t = 40; t < 60; t++) { ZogratisPose.Frame f = frame(t); f.rawSpeed = ZogratisPose.TELEPORT; p.update(f, TRIAD); }
        assertTrue(p.running() || p.clip.equals("walk") || p.clip.equals("run"), "the fastest honest stride is still a stride");
    }

    @Test
    void aNewBodyWithTheIdOfAForgottenOneStartsWithACleanClock() {
        ZogratisPose p = new ZogratisPose();
        for (int t = 5000; t < 5030; t++) {
            ZogratisPose.Frame f = frame(t);
            f.rawSpeed = 0.4f;
            f.swinging = t == 5029;
            p.update(f, TRIAD);
        }
        p.smoothScale(1.5f, 5029f);
        p.smoothYaw(90f, 5029f);
        assertEquals("attack", p.clip);
        assertTrue(p.speed() > 0.2f);
        ZogratisPose.Frame fresh = frame(3);            // the new entity's own tick count restarts at zero
        p.update(fresh, TRIAD);
        assertEquals("idle", p.clip);
        assertEquals(0f, p.speed(), 1e-6);
        assertEquals(0f, p.travel(), 1e-6);
        assertFalse(p.running());
        assertEquals(1f, p.smoothScale(1f, 3f), 1e-6, "the size starts over");
        assertEquals(-45f, p.smoothYaw(-45f, 3f), 1e-6, "the facing starts over");
        assertEquals(3f, p.lastAge(), 1e-6);
    }

    @Test
    void aSwingPlaysTheAttackClipFromItsLeadAndThenEnds() {
        ZogratisPose p = new ZogratisPose();
        p.update(frame(0), TRIAD);
        ZogratisPose.Frame f = frame(10);
        f.swinging = true;
        p.update(f, TRIAD);
        assertEquals("attack", p.clip);
        assertEquals(ZogratisPose.ATTACK_LEAD, p.seconds, 1e-4);
        f = frame(20);                              // the swing flag itself is over after six ticks
        p.update(f, TRIAD);
        assertEquals("attack", p.clip, "the clip outlives the swing flag");
        assertEquals(ZogratisPose.ATTACK_LEAD + 0.5f, p.seconds, 1e-4);
        f = frame(10 + 15);                         // 0.75 s later is past the clip's 0.9 s
        p.update(f, TRIAD);
        assertEquals("idle", p.clip);
    }

    @Test
    void aSwingFlagHeldForSixTicksStartsTheClipOnlyOnce() {
        ZogratisPose p = new ZogratisPose();
        for (int t = 5; t <= 10; t++) {
            ZogratisPose.Frame f = frame(t);
            f.swinging = true;
            p.update(f, TRIAD);
        }
        assertEquals("attack", p.clip);
        assertEquals(ZogratisPose.ATTACK_LEAD + 5 / 20f, p.seconds, 1e-4, "five ticks into the clip, not restarted every frame");
    }

    @Test
    void aNewSwingRestartsTheAttack() {
        ZogratisPose p = new ZogratisPose();
        ZogratisPose.Frame f = frame(5);
        f.swinging = true;
        p.update(f, TRIAD);
        f = frame(8);
        f.swinging = true;
        p.update(f, TRIAD);
        f = frame(15);                              // the flag dropped, then a second swing began
        p.update(f, TRIAD);
        f = frame(30);
        f.swinging = true;
        p.update(f, TRIAD);
        assertEquals("attack", p.clip);
        assertEquals(ZogratisPose.ATTACK_LEAD, p.seconds, 1e-4);
    }

    @Test
    void aPlainCastLoopsFromItsStart() {
        ZogratisPose p = new ZogratisPose();
        ZogratisPose.Frame f = frame(100);
        f.cast = 1;
        f.castProgress = 0.1f;
        p.update(f, TRIAD);
        assertEquals("cast", p.clip);
        assertEquals(0f, p.seconds, 1e-4);
        f = frame(120);
        f.cast = 1;
        f.castProgress = 0.6f;
        p.update(f, TRIAD);
        assertEquals(1.0f, p.seconds, 1e-4);
    }

    @Test
    void anUltimateIsPacedToItsWindUpAndSettlesAfterTheRelease() {
        ZogratisPose p = new ZogratisPose();
        int wind = 60;
        for (int t = 0; t < wind; t++) {
            ZogratisPose.Frame f = frame(200 + t);
            f.cast = 2;
            f.castProgress = t / (float) wind;
            p.update(f, TRIAD);
            assertEquals("cast_big", p.clip);
            assertEquals(f.castProgress * ZogratisPose.CAST_BIG_RELEASE * 2.4f, p.seconds, 1e-4);
        }
        ZogratisPose.Frame f = frame(200 + wind);   // released: the slam point, then the settling
        p.update(f, TRIAD);
        assertEquals("cast_big", p.clip);
        assertEquals(ZogratisPose.CAST_BIG_RELEASE * 2.4f, p.seconds, 1e-4);
        f = frame(200 + wind + 4);
        p.update(f, TRIAD);
        assertEquals(ZogratisPose.CAST_BIG_RELEASE * 2.4f + 0.2f, p.seconds, 1e-4);
        f = frame(200 + wind + 10);                 // 0.5 s: the 0.48 s of settling are over
        p.update(f, TRIAD);
        assertEquals("idle", p.clip);
    }

    @Test
    void aCancelledUltimateDoesNotSettle() {
        ZogratisPose p = new ZogratisPose();
        for (int t = 0; t < 20; t++) {
            ZogratisPose.Frame f = frame(t);
            f.cast = 2;
            f.castProgress = t / 60f;
            p.update(f, TRIAD);
        }
        p.update(frame(20), TRIAD);
        assertEquals("idle", p.clip);
    }

    @Test
    void aLoopingBigCastRunsOnTheClockAndHasNoSettling() {
        ZogratisPose p = new ZogratisPose();
        ZogratisPose.Frame f = frame(50);
        f.cast = 2;
        f.castProgress = 0.05f;
        p.update(f, LOOPING);
        f = frame(80);
        f.cast = 2;
        f.castProgress = 0.99f;
        p.update(f, LOOPING);
        assertEquals("cast_big", p.clip);
        assertEquals(1.5f, p.seconds, 1e-4);
        p.update(frame(81), LOOPING);
        assertEquals("idle", p.clip);
    }

    @Test
    void aBodyWithoutAnUltimateClipCastsNormally() {
        ZogratisPose p = new ZogratisPose();
        ZogratisPose.Frame f = frame(1);
        f.cast = 2;
        f.castProgress = 0.5f;
        p.update(f, clips(Map.of("idle", 3f, "cast", 1.6f), false));
        assertEquals("cast", p.clip);
    }

    @Test
    void priorityIsDeathThenTransformThenCastThenSwingThenHurtThenMoving() {
        ZogratisPose p = new ZogratisPose();
        ZogratisPose.Frame f = frame(10);
        f.dead = true;
        f.deathTicks = 10;
        f.transforming = true;
        f.cast = 2;
        f.swinging = true;
        f.hurtTicks = 1;
        f.rawSpeed = 0.4f;
        p.update(f, TRIAD);
        assertEquals("death", p.clip);
        assertEquals(0.5f * 2.8f, p.seconds, 1e-4, "ten of twenty ticks of the death: half the clip");
        f.dead = false;
        f.transformProgress = 0.5f;
        p.update(f, TRIAD);
        assertEquals("transform", p.clip);
        assertEquals(1.5f, p.seconds, 1e-4);
        f.transforming = false;
        p.update(f, TRIAD);
        assertEquals("cast_big", p.clip);
        f.cast = 0;
        f.age = 12;
        p.update(f, TRIAD);
        assertEquals("attack", p.clip);
        f.swinging = false;
        f.age = 40;
        f.tick = 40;
        p.update(f, TRIAD);
        assertEquals("hurt", p.clip, "the attack clip has run out; a flinch comes before the stride");
        f.hurtTicks = -1;
        p.update(f, TRIAD);
        assertTrue(p.clip.equals("walk") || p.clip.equals("run"));
    }

    @Test
    void aFlinchEndsWithItsClipAndTheDeathHoldsItsLastFrame() {
        ZogratisPose p = new ZogratisPose();
        ZogratisPose.Frame f = frame(3);
        f.hurtTicks = 5f;
        p.update(f, TRIAD);
        assertEquals("hurt", p.clip);
        assertEquals(0.25f, p.seconds, 1e-4);
        f.hurtTicks = 9.5f;
        p.update(f, TRIAD);
        assertEquals("idle", p.clip, "0.475 s is past the 0.45 s clip");
        f.dead = true;
        f.deathTicks = 400f;
        p.update(f, TRIAD);
        assertEquals(2.8f, p.seconds, 1e-4, "clamped to the end of the clip");
        f.deathTicks = -3f;
        p.update(f, TRIAD);
        assertEquals(0f, p.seconds);
    }

    @Test
    void missingClipsNeverThrowAndFallBackToIdle() {
        ZogratisPose p = new ZogratisPose();
        ZogratisPose.Clips none = clips(Map.of(), false);
        ZogratisPose.Frame f = frame(5);
        f.swinging = true;
        f.cast = 1;
        f.rawSpeed = 0.4f;
        f.hurtTicks = 1;
        p.update(f, none);
        assertEquals("cast", p.clip, "the name is still chosen: a clip the model lacks draws the rest pose");
        f.cast = 0;
        for (int t = 6; t < 30; t++) { f.tick = t; f.age = t; p.update(f, none); }
        assertEquals("walk", p.clip, "locomotion is chosen without clips; with no run clip it walks");
    }

    @Test
    void sizeEasesTowardItsTargetWithoutOvershooting() {
        ZogratisPose p = new ZogratisPose();
        assertEquals(1f, p.smoothScale(1f, 0f), 1e-6, "the first call jumps");
        float last = 1f;
        for (int t = 1; t <= 100; t++) {
            float s = p.smoothScale(1.18f, t);
            assertTrue(s >= last && s <= 1.18f, "monotonic and bounded at " + t);
            last = s;
        }
        assertEquals(1.18f, last, 1e-3);
        assertEquals(last, p.smoothScale(1.18f, 100f), 1e-6, "no time, no change");
    }

    @Test
    void facingTurnsTheShortWayRound() {
        ZogratisPose p = new ZogratisPose();
        assertEquals(-170f, p.smoothYaw(-170f, 0f), 1e-6, "the first call jumps");
        float y = p.smoothYaw(170f, 2f);
        assertTrue(y < -170f && y > -200f, "from -170 to 170 is a 20 degree turn through 180, not 340 the other way: " + y);
        float last = y;
        for (int t = 3; t < 80; t++) {
            float n = p.smoothYaw(170f, t);
            assertTrue(n <= last + 1e-4f, "keeps turning the same way");
            last = n;
        }
        assertEquals(-190f, last, 0.5f, "arrives (-190 is 170)");
        assertEquals(last, p.smoothYaw(170f, 79f), 1e-6, "no time, no turn");
    }

    @Test
    void wrapKeepsAnglesInRange() {
        assertEquals(-170f, ZogratisPose.wrapDegrees(190f), 1e-4);
        assertEquals(170f, ZogratisPose.wrapDegrees(-190f), 1e-4);
        assertEquals(0f, ZogratisPose.wrapDegrees(720f), 1e-4);
        assertEquals(-180f, ZogratisPose.wrapDegrees(180f), 1e-4);
        assertEquals(10f, ZogratisPose.wrapDegrees(370f), 1e-4);
    }
}
