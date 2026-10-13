package com.newuniverse.nusmp.client.floor.goliath;

import com.newuniverse.nusmp.client.zogratis.ZogratisPose;
import com.newuniverse.nusmp.dungeon.floor.goliath.GoliathTable;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Which clip the Goliath plays when: the move clips paced to the wind-up with a settling after the blow, the rise, the death, and the fallbacks. */
class GoliathPoseTest {
    private static final Map<String, Float> LEN = Map.ofEntries(Map.entry("idle", 3.2f), Map.entry("walk", 2.0f), Map.entry("cast", 1.6f), Map.entry("cast_big", 2.4f),
            Map.entry("slam", 2.0f), Map.entry("stomp", 3.0f), Map.entry("rise", 2.0f), Map.entry("death", 1.0f), Map.entry("transform", 3.0f), Map.entry("attack", 1.0f),
            Map.entry("hurt", 0.5f));
    private static final ZogratisPose.Clips CLIPS = new ZogratisPose.Clips() {
        @Override public float length(String c) { return LEN.getOrDefault(c, 0f); }
        @Override public boolean loops(String c) { return c.equals("idle") || c.equals("walk") || c.equals("cast"); }
    };

    private static ZogratisPose.Frame frame(float age) {
        ZogratisPose.Frame f = new ZogratisPose.Frame();
        f.age = age;
        f.tick = (int) age;
        return f;
    }

    private static ZogratisPose.Frame casting(float age, float progress) {
        ZogratisPose.Frame f = frame(age);
        f.cast = 1;
        f.castProgress = progress;
        return f;
    }

    @Test
    void everyMoveHasAClipAndTheQuakeUsesTheBigCast() {
        for (GoliathTable.Spec s : GoliathTable.specs()) assertNotNull(GoliathPose.clipOf(s.id()), s.id());
        assertEquals("slam", GoliathPose.clipOf(GoliathTable.GROUND_SLAM));
        assertEquals("cast_big", GoliathPose.clipOf(GoliathTable.BEDROCK_QUAKE));
        assertNull(GoliathPose.clipOf("nonsense"));
        assertNull(GoliathPose.clipOf(null));
    }

    @Test
    void aMoveClipIsPacedToTheWindUpAndTheBlowLandsAtEightyPercent() {
        GoliathPose p = new GoliathPose();
        p.update(casting(100f, 0f), "slam", 1f, CLIPS);
        assertEquals("slam", p.clip);
        assertEquals(0f, p.seconds, 1e-5);
        p.update(casting(110f, 0.5f), "slam", 1f, CLIPS);
        assertEquals(0.5f * 0.8f * 2.0f, p.seconds, 1e-5);
        p.update(casting(120f, 1f), "slam", 1f, CLIPS);
        assertEquals(0.8f * 2.0f, p.seconds, 1e-5);
    }

    @Test
    void afterTheReleaseTheClipSettlesThenGivesWayToIdle() {
        GoliathPose p = new GoliathPose();
        p.update(casting(100f, 0.95f), "slam", 1f, CLIPS);
        p.update(frame(101f), null, 1f, CLIPS);
        assertEquals("slam", p.clip, "settling");
        assertEquals(1.6f, p.seconds, 1e-5);
        p.update(frame(105f), null, 1f, CLIPS);
        assertEquals("slam", p.clip);
        assertEquals(1.6f + 4f / 20f, p.seconds, 1e-4);
        p.update(frame(120f), null, 1f, CLIPS);
        assertEquals("idle", p.clip, "settled");
    }

    @Test
    void aCancelledWindUpDoesNotSettle() {
        GoliathPose p = new GoliathPose();
        p.update(casting(100f, 0.3f), "slam", 1f, CLIPS);
        p.update(frame(101f), null, 1f, CLIPS);
        assertEquals("idle", p.clip);
    }

    @Test
    void aMoveWithoutAClipFallsBackToTheGenericCast() {
        GoliathPose p = new GoliathPose();
        p.update(casting(100f, 0.4f), "roar", 1f, CLIPS);
        assertEquals("cast", p.clip, "no roar clip in this file");
        p.update(casting(101f, 0.4f), null, 1f, CLIPS);
        assertEquals("cast", p.clip);
    }

    @Test
    void theRiseClipTimeIsTheRise() {
        GoliathPose p = new GoliathPose();
        p.update(frame(10f), null, 0.25f, CLIPS);
        assertEquals("rise", p.clip);
        assertEquals(0.5f, p.seconds, 1e-5);
        p.update(frame(11f), null, 0f, CLIPS);
        assertEquals(0f, p.seconds, 1e-5);
        p.update(frame(12f), null, 1f, CLIPS);
        assertEquals("idle", p.clip, "standing");
    }

    @Test
    void deathBeatsEverythingAndTheTransformationBeatsAMove() {
        GoliathPose p = new GoliathPose();
        ZogratisPose.Frame d = casting(10f, 0.5f);
        d.dead = true;
        d.deathTicks = 10f;
        d.deathSpan = 20f;
        p.update(d, "slam", 0.5f, CLIPS);
        assertEquals("death", p.clip);
        assertEquals(0.5f, p.seconds, 1e-5);
        GoliathPose q = new GoliathPose();
        ZogratisPose.Frame t = frame(10f);
        t.transforming = true;
        t.transformProgress = 0.5f;
        q.update(t, "slam", 1f, CLIPS);
        assertEquals("transform", q.clip);
        assertEquals(1.5f, q.seconds, 1e-5);
    }

    @Test
    void aNewBodyWithAnOldIdStartsClean() {
        GoliathPose p = new GoliathPose();
        p.update(casting(500f, 0.95f), "slam", 1f, CLIPS);
        p.update(frame(5f), null, 1f, CLIPS);
        assertEquals("idle", p.clip, "time ran backwards: nothing settles");
    }
}
