package com.newuniverse.nusmp.arc.midnight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The small state machines shared by the four bosses: stagger with a lockout, the break watch, the sliding damage window, the reflect rules and the trio bond. */
class StateClocksTest {

    @Test
    void aStaggerLastsItsTicksAndThenLocksOut() {
        StaggerClock c = new StaggerClock();
        assertFalse(c.active(0));
        assertTrue(c.tryStart(100, 40, 120));
        assertTrue(c.active(100));
        assertTrue(c.active(139));
        assertFalse(c.active(140), "over after exactly 40 ticks");
        assertEquals(40, c.left(100));
        assertEquals(1, c.left(139));
        assertEquals(0, c.left(500));
        assertFalse(c.tryStart(150, 40, 120), "locked out after the stagger");
        assertFalse(c.tryStart(259, 40, 120), "still locked out");
        assertTrue(c.tryStart(260, 40, 120), "the lockout (120 ticks after the end) is over");
    }

    @Test
    void aStaggerNeedsTicksAndClearResets() {
        StaggerClock c = new StaggerClock();
        assertFalse(c.tryStart(0, 0, 10));
        assertFalse(c.tryStart(0, -5, 10));
        assertTrue(c.tryStart(0, 30, 1000));
        c.clear();
        assertFalse(c.active(5));
        assertTrue(c.tryStart(5, 30, 0), "clear forgets the lockout too");
    }

    @Test
    void theBreakWatchMeasuresRealHealthLost() {
        BreakWatch w = new BreakWatch();
        assertFalse(w.armed());
        assertEquals(0f, w.lost(500f), 0f, "not armed: nothing lost");
        assertFalse(w.broken(0f, 1000f, 0.05));
        w.arm(800f);
        assertTrue(w.armed());
        assertEquals(0f, w.lost(800f), 0f);
        assertEquals(30f, w.lost(770f), 1e-4f);
        assertFalse(w.broken(770f, 1000f, 0.05), "3% is not 5%");
        assertTrue(w.broken(750f, 1000f, 0.05), "5% exactly breaks it");
        assertTrue(w.broken(700f, 1000f, 0.05));
        assertEquals(0f, w.lost(900f), 0f, "health gained is never negative loss");
        assertEquals(0.5f, w.progress(775f, 1000f, 0.05), 1e-4f);
        assertEquals(1f, w.progress(100f, 1000f, 0.05), 1e-4f, "progress stops at 1");
        w.disarm();
        assertFalse(w.broken(0f, 1000f, 0.05));
        assertEquals(0f, w.progress(0f, 1000f, 0.05), 0f);
    }

    @Test
    void theDamageWindowSumsOnlyTheRecentHits() {
        DamageWindow d = new DamageWindow();
        d.add(10, 40f);
        d.add(30, 25f);
        d.add(95, 15f);
        assertEquals(80f, d.sum(100, 100), 1e-3f, "all three are inside the last 100 ticks");
        assertEquals(0f, d.sum(5, 100), 1e-3f, "a hit in the future is not counted");
    }

    @Test
    void theDamageWindowDropsOldHitsAndSkipsEmptyOnes() {
        DamageWindow d = new DamageWindow();
        d.add(10, 40f);
        d.add(30, 25f);
        d.add(95, 15f);
        assertEquals(15f, d.sum(100, 20), 1e-3f, "only the hit at tick 95 is within 20 ticks of 100");
        assertEquals(40f, d.sum(100, 75), 1e-3f, "ticks 30 and 95");
        d.add(100, 0f);
        d.add(100, -5f);
        assertEquals(3, d.size(), "zero and negative hits are not recorded");
        d.prune(200, 100);
        assertEquals(0, d.size(), "everything is older than 100 ticks");
        d.add(5, 1f);
        d.clear();
        assertEquals(0f, d.sum(5, 10), 0f);
    }

    @Test
    void theDamageWindowIsBounded() {
        DamageWindow d = new DamageWindow();
        for (int i = 0; i < 1000; i++) d.add(i, 1f);
        assertTrue(d.size() <= 256, "a long fight cannot grow the queue without limit");
    }

    @Test
    void reflectionPrefersTheStrongestAnswer() {
        assertEquals(ReflectRules.Kind.FULL_REFLECTION, ReflectRules.decide(true, true, true, true, true));
        assertEquals(ReflectRules.Kind.MIRROR, ReflectRules.decide(false, true, true, true, true));
        assertEquals(ReflectRules.Kind.ANTI_MAGIC_SWING, ReflectRules.decide(false, false, true, true, true));
        assertEquals(ReflectRules.Kind.NONE, ReflectRules.decide(false, false, true, false, true), "holding the blade is not swinging it");
        assertEquals(ReflectRules.Kind.NONE, ReflectRules.decide(false, false, true, true, false), "a beam cannot be met by a swing");
        assertEquals(ReflectRules.Kind.NONE, ReflectRules.decide(false, false, false, true, true));
        assertEquals(ReflectRules.Kind.MIRROR, ReflectRules.decide(false, true, false, false, false), "a mirror turns a beam");
    }

    @Test
    void reflectionPaysTheBossBackInProportion() {
        assertEquals(0f, ReflectRules.backfireShare(ReflectRules.Kind.NONE), 0f);
        assertTrue(ReflectRules.backfireShare(ReflectRules.Kind.FULL_REFLECTION) > ReflectRules.backfireShare(ReflectRules.Kind.MIRROR));
        assertTrue(ReflectRules.backfireShare(ReflectRules.Kind.MIRROR) > ReflectRules.backfireShare(ReflectRules.Kind.ANTI_MAGIC_SWING));
        assertTrue(ReflectRules.backfireShare(ReflectRules.Kind.FULL_REFLECTION) <= 0.1f, "never more than a tenth of the bar from one reflect");
        assertEquals(0, ReflectRules.openTicks(ReflectRules.Kind.NONE));
        assertTrue(ReflectRules.openTicks(ReflectRules.Kind.FULL_REFLECTION) > ReflectRules.openTicks(ReflectRules.Kind.ANTI_MAGIC_SWING));
        assertEquals(0.4, ReflectRules.antiMagicCut(true), 1e-12);
        assertEquals(1.0, ReflectRules.antiMagicCut(false), 1e-12);
    }

    @Test
    void theThirdEyeIsTheTrioAndTheyAnswerEachOther() {
        assertEquals(3, EyeBondRules.THIRD_EYE.size());
        assertTrue(EyeBondRules.isMember("fana"));
        assertTrue(EyeBondRules.isMember("RHYA"));
        assertTrue(EyeBondRules.isMember("vetto"));
        assertFalse(EyeBondRules.isMember("patolli"), "the leader is above the Third Eye");
        assertFalse(EyeBondRules.isMember("rades_spirito"));
        assertFalse(EyeBondRules.isMember(null));
        assertTrue(EyeBondRules.ragesOn("vetto", "fana"));
        assertTrue(EyeBondRules.ragesOn("fana", "rhya"));
        assertTrue(EyeBondRules.ragesOn("RHYA", "VETTO"));
        assertFalse(EyeBondRules.ragesOn("fana", "fana"), "nobody rages at their own fall");
        assertFalse(EyeBondRules.ragesOn("fana", "patolli"));
        assertFalse(EyeBondRules.ragesOn("patolli", "fana"));
        assertFalse(EyeBondRules.ragesOn(null, "fana"));
        assertEquals(2, EyeBondRules.remaining("fana").size());
        assertFalse(EyeBondRules.remaining("fana").contains("fana"));
        assertEquals(3, EyeBondRules.remaining("nobody").size());
    }
}
