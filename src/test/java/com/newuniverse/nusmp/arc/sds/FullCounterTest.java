package com.newuniverse.nusmp.arc.sds;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Meliodas's Full Counter: what it answers, what it lets through, and the limits that keep a reflection from being a kill. */
class FullCounterTest {
    private static FullCounter raised(long now, float breakAt, boolean omni) {
        FullCounter c = new FullCounter();
        assertTrue(c.raise(now, 60, 300, breakAt, omni, false));
        return c;
    }

    @Test
    void theStanceHasADurationAndACooldown() {
        FullCounter c = new FullCounter();
        assertTrue(c.ready(0));
        assertFalse(c.up(0));
        assertTrue(c.raise(10, 60, 300, 100f, false, false));
        assertTrue(c.up(10) && c.up(69));
        assertFalse(c.up(70), "over after 60 ticks");
        assertFalse(c.ready(70), "the cooldown runs from the end of the stance");
        assertFalse(c.ready(369));
        assertTrue(c.ready(370));
        assertFalse(c.raise(200, 60, 300, 100f, false, false), "no second stance inside the cooldown");
        assertTrue(c.raise(200, 60, 300, 100f, false, true), "the images and the Wrath force it");
        assertFalse(c.raise(210, 60, 300, 100f, false, true), "never a second one on top of the first");
        assertFalse(new FullCounter().raise(0, 0, 300, 100f, false, false), "a stance of no length is no stance");
    }

    @Test
    void onlyMagicIsAnswered() {
        FullCounter c = raised(0, 1000f, false);
        assertEquals(FullCounter.Verdict.PASS, c.onHit(5, false, true, 50f, 7, 100f).verdict(), "a blade, an arrow, an anti-magic blow");
        assertEquals(FullCounter.Verdict.REFLECT, c.onHit(5, true, true, 20f, 7, 100f).verdict());
        assertEquals(0f, c.onHit(5, false, true, 50f, 7, 100f).reflected(), 0f);
        assertEquals(FullCounter.Verdict.PASS, c.onHit(5, true, true, 0f, 7, 100f).verdict(), "a hit of nothing is nothing");
        assertEquals(FullCounter.Verdict.PASS, new FullCounter().onHit(5, true, true, 20f, 7, 100f).verdict(), "no stance, no answer");
    }

    @Test
    void itOnlyAnswersWhatHeFacesUnlessTheImagesGuardHim() {
        FullCounter c = raised(0, 1000f, false);
        assertEquals(FullCounter.Verdict.PASS, c.onHit(5, true, false, 20f, 7, 100f).verdict(), "magic from behind lands");
        FullCounter o = raised(0, 1000f, true);
        assertTrue(o.omni(5));
        assertEquals(FullCounter.Verdict.REFLECT, o.onHit(5, true, false, 20f, 7, 100f).verdict(), "the images watch every side");
        assertFalse(o.omni(60), "the omni stance ends with the stance");
    }

    @Test
    void aReflectionIsTheCastersOwnDamageButNeverALethalOne() {
        FullCounter c = raised(0, 10_000f, false);
        assertEquals(20f, c.onHit(0, true, true, 20f, 1, 100f).reflected(), 1e-4f, "their own damage comes back");
        FullCounter big = raised(0, 10_000f, false);
        assertEquals(30f, big.onHit(0, true, true, 500f, 1, 100f).reflected(), 1e-4f, "at most 30% of the caster's maximum health");
        FullCounter small = raised(0, 10_000f, false);
        assertEquals(3f, small.onHit(0, true, true, 0.5f, 1, 100f).reflected(), 1e-4f, "a feeble spell still stings: 3% of their maximum");
        assertTrue(FullCounter.VICTIM_CAP < FullCounter.VICTIM_BUDGET && FullCounter.VICTIM_BUDGET < 1.0);
    }

    @Test
    void aSpellThatHitsEveryTickIsAnsweredOncePerGap() {
        FullCounter c = raised(0, 10_000f, false);
        assertEquals(10f, c.onHit(0, true, true, 10f, 1, 100f).reflected(), 1e-4f);
        for (long t = 1; t < FullCounter.REPEAT_GAP; t++) {
            FullCounter.Result r = c.onHit(t, true, true, 10f, 1, 100f);
            assertEquals(FullCounter.Verdict.REFLECT, r.verdict(), "still absorbed");
            assertEquals(0f, r.reflected(), 0f, "but not answered again at tick " + t);
        }
        assertEquals(10f, c.onHit(FullCounter.REPEAT_GAP, true, true, 10f, 1, 100f).reflected(), 1e-4f);
        assertEquals(10f, c.onHit(1, true, true, 10f, 2, 100f).reflected(), 1e-4f, "another caster has a gap of their own");
    }

    @Test
    void oneStanceTakesAtMostItsBudgetFromOneCaster() {
        FullCounter c = new FullCounter();
        assertTrue(c.raise(0, 300, 300, 10_000f, false, false));
        float total = 0;
        for (int i = 0; i < 10; i++) total += c.onHit(i * 10L, true, true, 100f, 1, 100f).reflected();
        assertEquals(55f, total, 1e-3f, "30 + 25, then nothing");
        assertEquals(0f, c.onHit(95, true, true, 100f, 1, 100f).reflected(), 0f);
        assertEquals(30f, c.onHit(96, true, true, 100f, 2, 100f).reflected(), 1e-4f, "the budget is per caster");
    }

    @Test
    void tooMuchMagicShattersTheStance() {
        FullCounter c = raised(0, 100f, false);
        assertEquals(FullCounter.Verdict.REFLECT, c.onHit(1, true, true, 60f, 1, 1000f).verdict());
        assertEquals(60f, c.absorbed(), 1e-4f);
        assertEquals(0.6f, c.load(), 1e-4f);
        FullCounter.Result r = c.onHit(2, true, true, 60f, 2, 1000f);
        assertEquals(FullCounter.Verdict.SHATTER, r.verdict());
        assertEquals(0f, r.reflected(), 0f, "the hit that breaks it lands, nothing is sent back");
        assertFalse(c.up(3), "it is down");
        assertFalse(c.ready(3), "and it has to cool down");
        assertEquals(FullCounter.Verdict.PASS, c.onHit(3, true, true, 60f, 1, 1000f).verdict());
    }

    @Test
    void aShatteredStanceStartsItsCooldownFromTheShatter() {
        FullCounter c = raised(0, 10f, false);
        c.onHit(20, true, true, 50f, 1, 100f);
        assertFalse(c.ready(319));
        assertTrue(c.ready(320));
    }

    @Test
    void aCasterWeDoNotKnowIsAbsorbedAndNothingIsSentBack() {
        FullCounter c = raised(0, 1000f, false);
        FullCounter.Result r = c.onHit(1, true, true, 20f, -1, 0f);
        assertEquals(FullCounter.Verdict.REFLECT, r.verdict());
        assertEquals(0f, r.reflected(), 0f);
        assertEquals(20f, c.absorbed(), 1e-4f);
    }

    @Test
    void dropEndsTheStanceAtOnce() {
        FullCounter c = raised(0, 1000f, true);
        c.drop(10);
        assertFalse(c.up(11));
        assertFalse(c.omni(11));
        assertFalse(c.ready(11), "a stagger or a stolen stance still costs the cooldown");
        c.reset();
        assertTrue(c.ready(11));
    }

    @Test
    void theSpellPressureRaisesTheStanceOnlyWhenItIsReady() {
        FullCounter c = new FullCounter();
        c.notePressure(100);
        c.notePressure(110);
        assertFalse(c.pressed(120), "two spells are not yet a barrage");
        c.notePressure(120);
        assertTrue(c.pressed(121));
        assertFalse(c.pressed(100 + FullCounter.PRESSURE_WINDOW + 30), "old spells are forgotten");
        c.raise(121, 60, 300, 100f, false, false);
        assertFalse(c.pressed(130), "not while it is up or cooling down");
        FullCounter many = new FullCounter();
        for (int i = 0; i < 40; i++) many.notePressure(i);
        assertTrue(many.pressure(40) <= 8, "only the last eight are kept");
    }
}
