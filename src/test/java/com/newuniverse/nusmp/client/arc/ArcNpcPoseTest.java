package com.newuniverse.nusmp.client.arc;

import com.newuniverse.nusmp.client.zogratis.ZogratisPose;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The clips of a friendly character: it idles, it walks by the ground it covers, it gestures (the cast clip) when someone stands close, and it flinches and dies. */
class ArcNpcPoseTest {
    /** The clips the art kit writes: loops for idle, walk, run and cast (talk), one-shots for the rest. */
    private static final ZogratisPose.Clips KIT = new ZogratisPose.Clips() {
        private final Map<String, Float> len = Map.of("idle", 3.2f, "walk", 1.1f, "run", 0.62f, "attack", 0.9f, "cast", 1.2f, "cast_big", 2.4f, "hurt", 0.45f,
                "transform", 3.0f, "death", 2.8f);
        @Override public float length(String clip) { return len.getOrDefault(clip, 0f); }
        @Override public boolean loops(String clip) { return clip.equals("idle") || clip.equals("walk") || clip.equals("run") || clip.equals("cast"); }
    };

    private static ZogratisPose.Frame frame(int tick) {
        ZogratisPose.Frame f = new ZogratisPose.Frame();
        f.tick = tick;
        f.age = tick;
        return f;
    }

    @Test
    void aPlayerCloseToAStandingNpcMakesItTalk() {
        assertTrue(ArcNpcPose.talking(4.0, true, false), "2 blocks away");
        assertTrue(ArcNpcPose.talking(ArcNpcPose.TALK_RANGE * ArcNpcPose.TALK_RANGE, true, false), "exactly at the range");
        assertFalse(ArcNpcPose.talking(100.0, true, false), "10 blocks away");
        assertFalse(ArcNpcPose.talking(4.0, false, false), "a walking NPC does not stop to gesture");
        assertFalse(ArcNpcPose.talking(4.0, true, true), "the dead do not talk");
        assertFalse(ArcNpcPose.talking(-1.0, true, false), "no player at all (a negative sentinel)");
    }

    @Test
    void standingAloneIsIdle() {
        ZogratisPose p = new ZogratisPose();
        ZogratisPose.Frame f = frame(40);
        ArcNpcPose.fill(f, false, 1.3f, 2.2f);
        p.update(f, KIT);
        assertEquals("idle", ArcNpcPose.clipOf(p));
        assertEquals(2.0f, p.seconds, 1e-4);
    }

    @Test
    void talkingIsTheCastClip() {
        ZogratisPose p = new ZogratisPose();
        for (int t = 0; t < 10; t++) {
            ZogratisPose.Frame f = frame(t);
            ArcNpcPose.fill(f, true, 1.3f, 2.2f);
            p.update(f, KIT);
        }
        assertEquals("cast", ArcNpcPose.clipOf(p));
        assertEquals(9f / 20f, p.seconds, 1e-4, "it runs from the tick the talk began");
        ZogratisPose.Frame f = frame(10);
        ArcNpcPose.fill(f, false, 1.3f, 2.2f);
        p.update(f, KIT);
        assertEquals("idle", ArcNpcPose.clipOf(p), "when the player leaves, it settles back to idle");
    }

    @Test
    void walkingIsPacedByTheStrideOfTheModel() {
        ZogratisPose slow = new ZogratisPose(), longLegs = new ZogratisPose();
        for (int t = 0; t < 20; t++) {
            ZogratisPose.Frame a = frame(t), b = frame(t);
            a.rawSpeed = 0.1f;
            b.rawSpeed = 0.1f;
            ArcNpcPose.fill(a, false, 1.0f, 2.0f);
            ArcNpcPose.fill(b, false, 2.0f, 3.0f);
            slow.update(a, KIT);
            longLegs.update(b, KIT);
        }
        assertEquals("walk", ArcNpcPose.clipOf(slow));
        assertEquals("walk", ArcNpcPose.clipOf(longLegs));
        float phaseShort = slow.seconds / 1.1f, phaseLong = longLegs.seconds / 1.1f;       // how far through the 1.1 s walk clip each one is
        assertEquals(0.9f, phaseShort, 1e-3, "1.9 blocks walked on a 1.0 block stride: nine tenths into the second cycle");
        assertEquals(0.95f, phaseLong, 1e-3, "the same ground on a 2.0 block stride: 95 percent into the first cycle");
    }

    @Test
    void aHitNpcFlinchesAndADyingOneDies() {
        ZogratisPose hurt = new ZogratisPose();
        ZogratisPose.Frame f = frame(5);
        f.hurtTicks = 2f;
        ArcNpcPose.fill(f, false, 1.3f, 2.2f);
        hurt.update(f, KIT);
        assertEquals("hurt", ArcNpcPose.clipOf(hurt));
        ZogratisPose dead = new ZogratisPose();
        ZogratisPose.Frame g = frame(5);
        g.dead = true;
        g.deathTicks = 10f;
        ArcNpcPose.fill(g, true, 1.3f, 2.2f);
        dead.update(g, KIT);
        assertEquals("death", ArcNpcPose.clipOf(dead), "dying beats talking");
    }

    @Test
    void fillSetsTheCastFlagsAndTheStrides() {
        ZogratisPose.Frame f = new ZogratisPose.Frame();
        ArcNpcPose.fill(f, true, 1.5f, 2.5f);
        assertEquals(1, f.cast);
        assertEquals(1f, f.castProgress, 0f);
        assertEquals(1.5f, f.walkCycle, 0f);
        assertEquals(2.5f, f.runCycle, 0f);
        ArcNpcPose.fill(f, false, 1.5f, 2.5f);
        assertEquals(0, f.cast);
        assertEquals(0f, f.castProgress, 0f);
    }
}
