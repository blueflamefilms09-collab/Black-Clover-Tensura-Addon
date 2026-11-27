package com.newuniverse.nusmp.lords;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: every number of the Lords of the Mortal Sins is pure ({@link LordRules}), so the mastery curve, the forms it opens, the burden (the named drawback), the Seifort relief and Greed's hunger numbers can be
 * checked without a game. Pure: no game types, no files.
 */
class LordRulesTest {
    @Test
    void theMasteryCurveIsMonotoneAndBounded() {
        assertEquals(0, LordRules.level(0));
        assertEquals(0, LordRules.progress(0));
        int prev = -1;
        for (int xp = 0; xp <= LordRules.MAX_XP + 500; xp += 7) {
            int l = LordRules.level(xp);
            assertTrue(l >= prev && l <= LordRules.MAX_LEVEL, "level " + l + " at " + xp);
            assertTrue(LordRules.progress(xp) >= 0 && LordRules.progress(xp) <= 100);
            prev = l;
        }
        assertEquals(LordRules.MAX_LEVEL, LordRules.level(LordRules.MAX_XP));
        assertEquals(100, LordRules.progress(LordRules.MAX_XP));
        assertEquals(50, LordRules.xpFor(1));
        assertEquals(5000, LordRules.xpFor(10));
        assertEquals(LordRules.MAX_XP, LordRules.clampXp(Long.MAX_VALUE));
        assertEquals(0, LordRules.clampXp(-5));
    }

    @Test
    void killExperienceIsBoundedSoABossIsNotALevel() {
        assertTrue(LordRules.xpForKill(1) >= 3);
        assertTrue(LordRules.xpForKill(1_000_000) <= 40);
        assertTrue(LordRules.xpForKill(Double.NaN) >= 3);
        assertTrue(LordRules.xpForKill(20) < LordRules.xpForKill(400));
    }

    @Test
    void theFormsAndTheDecreeOpenWithTheLevel() {
        assertTrue(LordRules.formOpen(0, 0));
        assertFalse(LordRules.formOpen(1, 2));
        assertTrue(LordRules.formOpen(1, 3));
        assertFalse(LordRules.formOpen(2, 5));
        assertTrue(LordRules.formOpen(2, 6));
        assertEquals(0, LordRules.topForm(2));
        assertEquals(1, LordRules.topForm(3));
        assertEquals(2, LordRules.topForm(10));
        assertFalse(LordRules.ultimateOpen(7));
        assertTrue(LordRules.ultimateOpen(8));
    }

    @Test
    void theBurdenIsClampedAndCurable() {
        assertEquals(0.0f, LordRules.clampBurden(Float.NaN));
        assertEquals(100.0f, LordRules.clampBurden(1e9f));
        float b = 0;
        for (int i = 0; i < 50; i++) b = LordRules.gain(b, LordRules.ART_BURDEN[2], 0);
        assertEquals(LordRules.BURDEN_MAX, b, "never above the max");
        assertTrue(LordRules.overloaded(b));
        assertTrue(LordRules.strained(LordRules.STRAIN_AT));
        assertFalse(LordRules.strained(LordRules.STRAIN_AT - 0.1f));
        for (int i = 0; i < 200; i++) b = LordRules.decay(b, true, 0);
        assertEquals(0.0f, b, "waiting cures it");
        assertTrue(LordRules.decay(80, false, 0) < LordRules.decay(80, true, 0), "stowed recovers faster");
        assertTrue(LordRules.relief(10) == 2.0f && LordRules.relief(3) == 0.0f);
        assertTrue(LordRules.OVERLOAD_RESET < LordRules.BURDEN_MAX);
    }

    @Test
    void masterySoftensTheDrawbackByAtMostHalf() {
        assertEquals(1.0f, LordRules.gainFactor(0));
        assertEquals(0.6f, LordRules.gainFactor(10), 1e-6f);
        assertEquals(1.5f, LordRules.decayFactor(10), 1e-6f);
        assertTrue(LordRules.gain(0, 22, 10) < LordRules.gain(0, 22, 0));
        assertEquals(LordRules.gainFactor(LordRules.MAX_LEVEL), LordRules.gainFactor(99), "levels past the top count as the top");
    }

    @Test
    void seifortPenanceIsEasedOnlyBySmitingTheUndead() {
        assertEquals(LordRules.ART_BURDEN[0] * 0.5f, LordRules.artBurden(LordRules.Sin.HOLY, 0, 1));
        assertEquals(LordRules.ART_BURDEN[0], LordRules.artBurden(LordRules.Sin.HOLY, 0, 0));
        for (LordRules.Sin s : LordRules.Sin.values()) if (s != LordRules.Sin.HOLY) assertEquals(LordRules.ART_BURDEN[1], LordRules.artBurden(s, 1, 5), s.id);
    }

    @Test
    void everyWeaponHasThreeFormsAndGreedKeepsItsOneItem() {
        for (LordRules.Sin s : LordRules.Sin.values()) {
            assertEquals(s.items.length, s.formNames.length, s.id);
            assertEquals(s.items.length, s.arts.length, s.id);
            assertFalse(s.ultimate.isBlank() || s.drawback.isBlank() || s.mastery.isBlank(), s.id);
            if (s.isLord()) {
                assertEquals(3, s.forms(), s.id);
                for (String item : s.items) assertEquals(s, LordRules.Sin.ofItem(item), item);
            }
        }
        assertEquals(1, LordRules.Sin.GREED.forms());
        assertEquals(LordRules.LORDS, (int) java.util.Arrays.stream(LordRules.Sin.values()).filter(LordRules.Sin::isLord).count());
        assertEquals("SLOTH", LordRules.Sin.values()[0].name(), "enum entries are only ever appended (the ordinal is saved)");
        assertEquals("GREED", LordRules.Sin.values()[4].name());
    }

    @Test
    void artCooldownsAreBounded() {
        for (LordRules.Sin s : LordRules.Sin.values()) for (int f = 0; f < 3; f++) assertTrue(LordRules.artCooldown(s, f) >= 20 && LordRules.artCooldown(s, f) <= 400, s.id + f);
        assertTrue(LordRules.ready(100, 90, 60));
        assertFalse(LordRules.ready(100, 150, 60));
        assertTrue(LordRules.ready(100, 5000, 60), "a clock set back does not lock the art for good");
    }

    @Test
    void greedHungerNumbersMatchWhatThePlayerIsTold() {
        assertEquals(1.0, LordRules.bonusShare(50f));
        assertEquals(0.7, LordRules.bonusShare(19.9f));
        assertEquals(0.0, LordRules.bonusShare(0f));
        assertTrue(LordRules.ravenous(19.9f) && !LordRules.ravenous(20f));
        assertTrue(LordRules.dormant(0f) && !LordRules.dormant(0.1f));
        assertEquals(0.5f, LordRules.ravenousHeal(0), 1e-6f, "the lang text says half");
        assertEquals(0.9f, LordRules.ravenousHeal(10), 1e-6f, "the lang text says 4% per level");
        assertEquals(2.5, LordRules.halfHunger(0), 1e-9, "the lang text says 2.5 times");
        assertEquals(1.5, LordRules.halfHunger(10), 1e-9);
        assertEquals(1.5, LordRules.halfStats(0), 1e-9);
        assertEquals(1.6, LordRules.halfStats(10), 1e-9);
        assertEquals(1.5, LordRules.RAVENOUS_DRAIN_PERCENT, 1e-9, "the lang text says 1.5% of current magicule");
        assertEquals(20.0f, LordRules.RAVENOUS_BELOW, "the lang text says 20");
    }

    @Test
    void theNumbersInTheLangTextAreTheNumbersOfTheRules() {
        assertEquals(50.0f, LordRules.STRAIN_AT);
        assertEquals(100.0f, LordRules.BURDEN_MAX);
        assertEquals(120, LordRules.OVERLOAD_TICKS, "6 seconds");
        assertEquals(1.5f, LordRules.DECAY_PER_SECOND);
        assertEquals(3.0f, LordRules.STOWED_FACTOR);
        assertEquals(8.0f, LordRules.KILL_RELIEF);
        assertEquals(12.0f, LordRules.ART_BURDEN[0]);
        assertEquals(22.0f, LordRules.ART_BURDEN[2]);
        assertEquals(45.0f, LordRules.ULTIMATE_BURDEN);
        assertEquals(1.0f, LordRules.HIT_BURDEN);
        assertEquals(3, LordRules.FORM1_LEVEL);
        assertEquals(6, LordRules.FORM2_LEVEL);
        assertEquals(8, LordRules.ULTIMATE_LEVEL);
        assertEquals(14L * 24 * 3600 * 1000, LordRules.INACTIVITY_MS, "14 days");
    }

    @Test
    void halfStarvationUnlocksAtHalfMasteryAndAlwaysSwitchesOff() {
        assertFalse(LordRules.halfMayToggle(false, 49, 50));
        assertTrue(LordRules.halfMayToggle(false, 50, 50));
        assertTrue(LordRules.halfMayToggle(true, 0, 50), "switching off is never refused");
        assertTrue(LordRules.halfMayToggle(false, 0, 0), "0 = the old behaviour, no gate");
        assertEquals(50, LordRules.masteryPercent(500, 1000));
        assertEquals(0, LordRules.masteryPercent(5, 0));
        assertEquals(100, LordRules.masteryPercent(2000, 1000));
    }
}
