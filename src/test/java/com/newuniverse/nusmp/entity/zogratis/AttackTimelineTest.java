package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.entity.zogratis.AttackTimeline.Runner;
import com.newuniverse.nusmp.entity.zogratis.AttackTimeline.Swing;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Leon study fix #2: the table is sane and damage neutral, the hit lands on the stated tick (once, never stale), and the client clip maps the arm raise onto it. */
class AttackTimelineTest {
    private static final List<String> BOSSES = List.of("meliodas", "ban", "escanor", "heath_grice", "vetto", "mars", "udaeus", "minotaur", "black_goliath");

    @Test
    void everyListedBossHasRowsAndEveryRowIsInsideItsClip() {
        for (String id : BOSSES) {
            assertTrue(AttackTimeline.has(id), id);
            for (Swing s : AttackTimeline.forBoss(id)) {
                assertTrue(s.hitTick() >= 1 && s.hitTick() < s.clipTicks(), id + " " + s.name());
                if (s.hits() == 2) assertTrue(s.secondHitTick() > s.hitTick() && s.secondHitTick() < s.clipTicks(), id + " second hit");
                assertTrue(s.clipTicks() <= AttackTimeline.MAX_CLIP_TICKS);
            }
        }
        assertFalse(AttackTimeline.has("revchi_salik"));
        assertFalse(AttackTimeline.has(null));
        assertTrue(AttackTimeline.forBoss("nobody").isEmpty());
    }

    @Test
    void leonsClipsAreInTheTable() {
        Swing cross = AttackTimeline.forBoss("meliodas").get(2);
        assertEquals(38, cross.clipTicks());
        assertEquals(12, cross.hitTick());
        assertEquals(22, cross.secondHitTick());
        assertEquals(2, cross.hits());
        Swing heavy = AttackTimeline.forBoss("escanor").get(2);
        assertEquals(38, heavy.clipTicks());
        assertEquals(17, heavy.hitTick());
    }

    @Test
    void noSwingDealsMoreDamagePerTickThanTheOldFixedCooldown() {
        for (String id : BOSSES)
            for (Swing s : AttackTimeline.forBoss(id)) {
                assertTrue(AttackTimeline.neutral(s), id + " " + s.name() + " = " + s.totalMult() * 20f / s.clipTicks() + " of the old damage per tick");
                assertTrue(s.damageMult() <= 2.2f && s.totalMult() <= 2.2f, "no single move is a one-shot: " + s.totalMult());
            }
    }

    @Test
    void theRowConstructorClampsBadNumbers() {
        Swing s = new Swing("x", 3, 99, 2, 100f);
        assertEquals(8, s.clipTicks());
        assertEquals(7, s.hitTick());
        assertEquals(1, s.hits(), "a second hit before the first is dropped");
        assertEquals(3f, s.damageMult());
        assertEquals(1, new Swing("y", 30, 0, 0, 0f).hitTick());
        assertTrue(new Swing("y", 30, 10, 0, -1f).damageMult() > 0f);
    }

    @Test
    void theRotationWrapsAndTheSyncedIndexAgrees() {
        List<Swing> l = AttackTimeline.forBoss("ban");
        for (int i = 0; i < 10; i++) {
            assertSame(l.get(i % l.size()), AttackTimeline.pick("ban", i));
            assertSame(AttackTimeline.pick("ban", i), AttackTimeline.at("ban", AttackTimeline.indexOf("ban", i)), "what the client reads is what the server picked");
        }
        assertSame(l.get(l.size() - 1), AttackTimeline.pick("ban", -1), "a negative count still lands on a row");
        assertNull(AttackTimeline.pick("revchi_salik", 0));
        assertEquals(-1, AttackTimeline.indexOf("revchi_salik", 0));
        assertNull(AttackTimeline.at("ban", -1), "-1 on the wire = the old lead");
        assertNull(AttackTimeline.at("ban", 99));
    }

    @Test
    void theHitIsDueOnItsTickOnceAndTheSecondOnItsOwn() {
        Runner r = new Runner();
        Swing cross = new Swing("c", 38, 12, 22, 0.9f);
        r.start(1000, cross);
        for (long t = 1000; t < 1012; t++) assertEquals(0, r.due(t), "tick " + (t - 1000));
        assertEquals(1, r.due(1012), "the first hit on tick 12");
        assertEquals(0, r.due(1012), "once");
        for (long t = 1013; t < 1022; t++) assertEquals(0, r.due(t));
        assertEquals(2, r.due(1022));
        assertEquals(0, r.due(1023));
        assertSame(cross, r.current());
    }

    @Test
    void aSingleHitMoveNeverReportsASecond() {
        Runner r = new Runner();
        r.start(0, new Swing("a", 30, 13, 0, 1.5f));
        int total = 0;
        for (long t = 0; t < 40; t++) total += r.due(t) > 0 ? 1 : 0;
        assertEquals(1, total);
    }

    @Test
    void aStalledSwingNeverLandsAStaleHit() {
        Runner r = new Runner();
        r.start(0, new Swing("a", 30, 13, 0, 1.5f));
        assertEquals(0, r.due(500), "the boss was busy for 25 seconds: the swing is forgotten, not landed");
        assertNull(r.current());
        assertFalse(r.busy(500));
        r.start(600, new Swing("a", 30, 13, 0, 1.5f));
        assertTrue(r.busy(610));
        assertFalse(r.busy(631));
        r.start(700, new Swing("a", 30, 13, 0, 1.5f));
        r.cancel();
        assertEquals(0, r.due(713));
    }

    @Test
    void theClientClipRaisesTheArmOntoTheHitTickThenPlaysOnAtItsOwnSpeed() {
        assertEquals(0.2f + 5 / 20f, AttackTimeline.clipSeconds(0, 5, 0.2f), 1e-6, "no timeline: the old lead, as before");
        assertEquals(0f, AttackTimeline.clipSeconds(13, 0, 0.2f), 1e-6);
        assertEquals(AttackTimeline.CLIP_PEAK_SECONDS / 2, AttackTimeline.clipSeconds(12, 6, 0.2f), 1e-6, "halfway to the hit = half the raise");
        assertEquals(AttackTimeline.CLIP_PEAK_SECONDS, AttackTimeline.clipSeconds(13, 13, 0.2f), 1e-6, "the blow is on the hit tick");
        assertEquals(AttackTimeline.CLIP_PEAK_SECONDS + 0.25f, AttackTimeline.clipSeconds(13, 18, 0.2f), 1e-6);
        float last = -1f;
        for (int t = 0; t < 40; t++) {
            float s = AttackTimeline.clipSeconds(17, t, 0.2f);
            assertTrue(s >= last, "the clip never runs backwards");
            last = s;
        }
        assertEquals(0f, AttackTimeline.clipSeconds(13, -3, 0.2f), "a clock that is a little early does not run negative");
    }
}
