package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The numbers and the tabletop-to-game translations of the wave 6 armours and robes (docs/doc_wave6.md). */
class Wave6TuneTest {
    @Test
    void skycrystalDawnGainIsOneD8PlusTwo() {
        assertEquals(3, Wave6Tune.skyDawnGain(n -> 0));
        assertEquals(10, Wave6Tune.skyDawnGain(n -> n - 1));
    }

    @Test
    void theLastChargeD10OnlyPaysOnATen() {
        for (int face = 0; face < 9; face++) {
            int f = face;
            assertEquals(0, Wave6Tune.skyLastChargeGain(n -> n == 10 ? f : 0), "a " + (f + 1) + " on the d10");
        }
        assertEquals(2, Wave6Tune.skyLastChargeGain(n -> n == 10 ? 9 : 0), "a 10, then 1d4+1 with a 1");
        assertEquals(5, Wave6Tune.skyLastChargeGain(n -> n == 10 ? 9 : 3), "a 10, then 1d4+1 with a 4");
    }

    @Test
    void anAbsorbedSpellGivesOneToFourCharges() {
        assertEquals(1, Wave6Tune.absorbGain(0.5f));
        assertEquals(1, Wave6Tune.absorbGain(5f));
        assertEquals(2, Wave6Tune.absorbGain(5.1f));
        assertEquals(4, Wave6Tune.absorbGain(400f));
    }

    @Test
    void theFlashSaveIsDistanceAndTheUndeadAreAlwaysCaught() {
        assertTrue(Wave6Tune.failsSave(false, 2.0));
        assertFalse(Wave6Tune.failsSave(false, 4.0));
        assertTrue(Wave6Tune.failsSave(true, 5.9), "undead have disadvantage on the save");
        assertEquals(6f, Wave6Tune.flashDamage(false, 1.0), 1e-6);
        assertEquals(3f, Wave6Tune.flashDamage(false, 5.0), 1e-6, "half on a success");
        assertEquals(9f, Wave6Tune.flashDamage(true, 5.0), 1e-6);
        assertEquals(Wave6Tune.DAWN_BLIND_TICKS, Wave6Tune.flashBlind(false, 1.0));
        assertEquals(0, Wave6Tune.flashBlind(false, 5.0), "no blindness on a success");
        assertEquals(Wave6Tune.DAWN_BLIND_TICKS, Wave6Tune.flashBlind(true, 5.0));
        assertEquals(1, Wave6Tune.dawnGain(n -> 0));
        assertEquals(3, Wave6Tune.dawnGain(n -> n - 1));
    }

    @Test
    void theFlashStaysUnderThePvpCap() {
        float worst = Wave6Tune.flashDamage(false, 0.0);
        assertTrue(worst <= 20f * 0.30f, "the flash must not take more than 30% of a 20 HP player: " + worst);
    }

    @Test
    void shadowCloudAndFlashRespectThePvpCap() {
        assertTrue(Wave6Tune.CLOUD_PULSES * Wave6Tune.CLOUD_PULSE_DAMAGE <= 20f * 0.30f, "the whole cloud, all three pulses, is at most 6 HP");
        assertTrue(Wave6Tune.CLOUD_RADIUS < Wave6Tune.GLOBE_RADIUS);
    }

    @Test
    void dimMeansLightSevenOrLess() {
        assertTrue(Wave6Tune.isDim(0));
        assertTrue(Wave6Tune.isDim(7));
        assertFalse(Wave6Tune.isDim(8));
        assertFalse(Wave6Tune.isDim(15));
    }

    @Test
    void shadowStepPicksTheFarthestAcceptedDistance() {
        assertEquals(12, Wave6Tune.pickDistance(12, 2, d -> true));
        assertEquals(7, Wave6Tune.pickDistance(12, 2, d -> d <= 7));
        assertEquals(2, Wave6Tune.pickDistance(12, 2, d -> d == 2));
        assertEquals(-1, Wave6Tune.pickDistance(12, 2, d -> false));
        assertEquals(-1, Wave6Tune.pickDistance(1, 2, d -> true), "a wall in your face leaves no distance");
    }

    @Test
    void mistwalkerDawnGainAndMisfire() {
        assertEquals(2, Wave6Tune.mistGain(n -> 0));
        assertEquals(5, Wave6Tune.mistGain(n -> n - 1));
        assertTrue(Wave6Tune.mistMisfire(n -> 0), "a 1 on the d20");
        assertFalse(Wave6Tune.mistMisfire(n -> 1));
        assertFalse(Wave6Tune.mistMisfire(n -> n - 1));
    }

    @Test
    void plotDodgeRisesAsTheStoryGetsDesperate() {
        double full = Wave6Tune.plotDodge(20f, 20f), half = Wave6Tune.plotDodge(10f, 20f), low = Wave6Tune.plotDodge(1f, 20f), gone = Wave6Tune.plotDodge(0f, 20f);
        assertEquals(0.06, full, 1e-9);
        assertTrue(full < half && half < low && low < gone);
        assertEquals(0.33, gone, 1e-9);
        assertEquals(0.33, Wave6Tune.plotDodge(-5f, 20f), 1e-9, "below zero health is zero");
        assertEquals(0.06, Wave6Tune.plotDodge(40f, 20f), 1e-9, "above the bar is the bar");
        assertEquals(0.33, Wave6Tune.plotDodge(5f, 0f), 1e-9, "no max health: treated as desperate, never a crash");
        assertTrue(gone < 0.5, "never a guarantee");
    }

    @Test
    void plotSaveLeavesAThirdOfTheBarAndAtLeastSix() {
        assertEquals(20f / 3f, Wave6Tune.plotSaveHealth(20f), 1e-5);
        assertEquals(6f, Wave6Tune.plotSaveHealth(10f), 1e-5);
        assertEquals(20f, Wave6Tune.plotSaveHealth(60f), 1e-5);
    }

    @Test
    void timelessnessHalvesAndLiftsTheLastSeconds() {
        assertEquals(300, Wave6Tune.timeless(600));
        assertEquals(20, Wave6Tune.timeless(41 * 1));
        assertEquals(0, Wave6Tune.timeless(40));
        assertEquals(0, Wave6Tune.timeless(1));
        int t = 1200, passes = 0;
        while (t > 0) { t = Wave6Tune.timeless(t); passes++; }
        assertTrue(passes <= 6, "a minute of poison is gone in six passes (30 s)");
    }

    @Test
    void theSlimeNumbers() {
        assertTrue(Wave6Tune.SLASH_FACTOR < 1.0 && Wave6Tune.BLUNT_FACTOR > 1.0, "resistant to slashing, vulnerable to bludgeoning");
        assertEquals(0.45, 1.0 + Wave6Tune.SLIME_SCALE, 1e-9, "the form is 45% size");
        assertEquals(3, Wave6Tune.SLIME_USES);
        assertEquals(60, Wave6Tune.SLIME_TICKS / 20);
    }

    @Test
    void flagsAndIdsAreDistinct() {
        String[] flags = {Wave6Tune.F_SKY, Wave6Tune.F_SLIME, Wave6Tune.F_DAWN, Wave6Tune.F_ASTRAL, Wave6Tune.F_SHADOW, Wave6Tune.F_NEKET, Wave6Tune.F_PLOT, Wave6Tune.F_ZAR};
        assertEquals(flags.length, java.util.Set.of(flags).size());
        for (String f : flags) assertTrue(f.startsWith("w6_"), f);
        String[] ids = {Wave6Tune.ID_SKY, Wave6Tune.ID_SLIME, Wave6Tune.ID_DAWN, Wave6Tune.ID_ASTRAL, Wave6Tune.ID_SHADOW, Wave6Tune.ID_MIST, Wave6Tune.ID_NEKET, Wave6Tune.ID_PLOT, Wave6Tune.ID_ZAR, Wave6Tune.ID_COAT};
        assertEquals(10, java.util.Set.of(ids).size());
        for (String i : ids) assertTrue(i.matches("[a-z0-9_]+"), i);
    }
}
