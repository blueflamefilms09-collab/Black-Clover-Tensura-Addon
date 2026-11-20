package com.newuniverse.nusmp.client.zogratis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Leon study fix #2 on the client clock: a swing with a timeline row draws its arm raise onto the hit tick; without one nothing changed. */
class ZogratisPoseTimelineTest {
    private static final ZogratisPose.Clips CLIPS = new ZogratisPose.Clips() {
        @Override public float length(String clip) { return clip.equals("attack") ? 0.9f : 3f; }
        @Override public boolean loops(String clip) { return clip.equals("idle"); }
    };

    private static ZogratisPose.Frame frame(int tick, boolean swinging, int hit) {
        ZogratisPose.Frame f = new ZogratisPose.Frame();
        f.tick = tick;
        f.age = tick;
        f.swinging = swinging;
        f.attackHit = hit;
        return f;
    }

    @Test
    void theBlowOfTheClipIsOnTheHitTick() {
        ZogratisPose p = new ZogratisPose();
        p.update(frame(0, false, 0), CLIPS);
        p.update(frame(10, true, 13), CLIPS);
        assertEquals("attack", p.clip);
        assertEquals(0f, p.seconds, 1e-4, "the swing starts at the start of the raise, not 0.2 s in");
        p.update(frame(10 + 13, false, 13), CLIPS);
        assertEquals(0.2f, p.seconds, 1e-4, "thirteen ticks later the arm is where the old lead put it");
        p.update(frame(10 + 13 + 5, false, 13), CLIPS);
        assertEquals(0.2f + 0.25f, p.seconds, 1e-4);
    }

    @Test
    void theRowOfTheSwingThatBeganIsKeptWhileTheNextOneIsSynced() {
        ZogratisPose p = new ZogratisPose();
        p.update(frame(10, true, 13), CLIPS);
        p.update(frame(16, false, 17), CLIPS);                      // the synced index moved on already: this swing keeps its own hit tick
        assertEquals(6 / 13f * 0.2f, p.seconds, 1e-4);
    }

    @Test
    void withoutARowTheOldLeadStays() {
        ZogratisPose p = new ZogratisPose();
        p.update(frame(10, true, 0), CLIPS);
        assertEquals(ZogratisPose.ATTACK_LEAD, p.seconds, 1e-4);
    }
}
