package com.newuniverse.nusmp.skill.rules;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Ki (3.47.0) is an Ultimate with ten modes; every number and decision of it is pure, so the bounds and the trade-offs are pinned without a world. */
class KiRulesTest {
    @Test
    void tenModesWithUniqueIdsAndKeys() {
        assertEquals(10, KiRules.MODES);
        assertEquals(KiRules.MODES, KiRules.IDS.length);
        Set<String> ids = new HashSet<>(), keys = new HashSet<>();
        for (int m = 0; m < KiRules.MODES; m++) {
            assertTrue(KiRules.valid(m));
            assertTrue(ids.add(KiRules.IDS[m]), "duplicate id " + KiRules.IDS[m]);
            assertTrue(keys.add(KiRules.untilKey(m)), "duplicate tag key for mode " + m);
        }
        assertFalse(KiRules.valid(-1));
        assertFalse(KiRules.valid(KiRules.MODES));
        assertFalse(keys.contains(KiRules.UI_DODGE_KEY), "the dodge key must not collide with a window key");
        assertEquals("blast", KiRules.IDS[KiRules.BLAST]);
        assertEquals("sense", KiRules.IDS[KiRules.SENSE]);
    }

    @Test
    void everyModeCostsAndWaitsInsideItsBand() {
        for (int m = 0; m < KiRules.MODES; m++) {
            assertTrue(KiRules.cooldown(m) > 0 && KiRules.cooldown(m) <= 6000, "cooldown of " + KiRules.IDS[m]);
            assertTrue(KiRules.cost(m) > 0 && KiRules.cost(m) <= 10000, "cost of " + KiRules.IDS[m]);
        }
        assertTrue(KiRules.cost(KiRules.BLAST) < KiRules.cost(KiRules.KAME), "the quick shot is cheaper than the beam");
        assertTrue(KiRules.cooldown(KiRules.BLAST) < KiRules.cooldown(KiRules.KAME));
        for (int m = 0; m < KiRules.MODES; m++) if (m != KiRules.SPIRIT) assertTrue(KiRules.cost(m) < KiRules.cost(KiRules.SPIRIT), "Spirit Bomb is the dearest, not " + KiRules.IDS[m]);
    }

    @Test
    void windowsAreShortAndAfterimageWaitsForItsBlow() {
        for (int m = 0; m < KiRules.MODES; m++) {
            assertEquals(KiRules.duration(m) > 0, KiRules.timed(m));
            assertTrue(KiRules.duration(m) <= 1200, "window of " + KiRules.IDS[m] + " must stay short");
            if (KiRules.cancellable(m)) assertTrue(KiRules.timed(m), "only a window can be cancelled: " + KiRules.IDS[m]);
        }
        assertTrue(KiRules.cancellable(KiRules.SSJ) && KiRules.cancellable(KiRules.KAIOKEN) && KiRules.cancellable(KiRules.UI));
        assertFalse(KiRules.cancellable(KiRules.AFTERIMAGE), "Afterimage ends when the blow it turns away lands");
        assertFalse(KiRules.timed(KiRules.BLAST) || KiRules.timed(KiRules.KAME) || KiRules.timed(KiRules.TELEPORT) || KiRules.timed(KiRules.SPIRIT) || KiRules.timed(KiRules.SENSE));
    }

    @Test
    void aWindowIsOpenUntilItsEndTime() {
        long end = KiRules.endsAt(1000, KiRules.SSJ);
        assertEquals(1000 + KiRules.duration(KiRules.SSJ), end);
        assertTrue(KiRules.active(end, 1000));
        assertTrue(KiRules.active(end, end - 1));
        assertFalse(KiRules.active(end, end));
        assertFalse(KiRules.active(0, 5), "an unset window is closed");
    }

    @Test
    void formsTradeProtectionAndStrainWithinABand() {
        assertEquals(1.0, KiRules.takenMultiplier(false, false, false));
        assertEquals(0.85, KiRules.takenMultiplier(true, false, false), 1e-9);
        assertEquals(1.25, KiRules.takenMultiplier(false, true, false), 1e-9);
        assertEquals(0.5, KiRules.takenMultiplier(false, false, true), 1e-9);
        for (int mask = 0; mask < 8; mask++) {
            double m = KiRules.takenMultiplier((mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0);
            assertTrue(m >= 0.1 && m <= 1.5, "multiplier out of band for mask " + mask + ": " + m);
        }
        assertTrue(KiRules.takenMultiplier(true, true, false) > KiRules.takenMultiplier(true, false, false), "Kaioken's strain shows on top of Super Saiyan");
        assertEquals(0.2, KiRules.afterimageMultiplier(), 1e-9);
    }

    @Test
    void ultraInstinctDodgesOneBlowASecond() {
        assertTrue(KiRules.uiDodges(0, 100), "the first blow is dodged");
        assertFalse(KiRules.uiDodges(100, 105), "a blow right after a dodge is not");
        assertFalse(KiRules.uiDodges(100, 119));
        assertTrue(KiRules.uiDodges(100, 120), "a second later it dodges again");
    }

    @Test
    void kaiokenNeverKillsItsUser() {
        assertTrue(KiRules.kaiokenAllowed(20f) && KiRules.kaiokenAllowed(4.5f));
        assertFalse(KiRules.kaiokenAllowed(4f));
        assertFalse(KiRules.kaiokenAllowed(1f));
        assertEquals(3f, KiRules.kaiokenHealthCost(20f, 20f), 1e-6, "15% of a 20 point maximum");
        assertEquals(1f, KiRules.kaiokenHealthCost(3f, 20f), 1e-6, "never below two points left");
        assertEquals(0f, KiRules.kaiokenHealthCost(1.5f, 20f), 1e-6, "no negative cost when almost dead");
        for (float h = 0f; h <= 40f; h += 0.5f) {
            float left = h - KiRules.kaiokenHealthCost(h, 40f);
            assertTrue(left >= Math.min(h, 2f) - 1e-6, "left " + left + " of " + h);
        }
    }

    @Test
    void spiritBombGrowsWithLendersUpToACap() {
        assertEquals(KiRules.spiritBase(), KiRules.spiritDamage(0), 1e-9);
        assertEquals(KiRules.spiritBase(), KiRules.spiritDamage(-5), 1e-9, "a negative count is nobody");
        assertTrue(KiRules.spiritDamage(3) > KiRules.spiritDamage(2));
        assertEquals(KiRules.spiritDamage(KiRules.spiritLendMax()), KiRules.spiritDamage(500), 1e-9, "more lenders than the cap add nothing");
        assertTrue(KiRules.spiritDamage(KiRules.spiritLendMax()) <= 80);
        assertTrue(KiRules.spiritLendMax() <= 8 && KiRules.spiritRadius() <= 10 && KiRules.spiritLendRadius() <= 32);
    }

    @Test
    void attackAndSenseNumbersStayBounded() {
        assertTrue(KiRules.blastDamage() > 0 && KiRules.blastDamage() < KiRules.kameDamage());
        assertTrue(KiRules.kameDamage() <= 40 && KiRules.kamePierce() <= 8 && KiRules.kameRange() <= 32 && KiRules.kameWidth() <= 3);
        assertTrue(KiRules.blastRange() <= 40 && KiRules.blastWidth() <= 2);
        assertTrue(KiRules.teleportRange() <= 32 && KiRules.teleportOk(1.5) && !KiRules.teleportOk(1.4));
        assertTrue(KiRules.flareRange() <= 16 && KiRules.flareCone() > 0 && KiRules.flareCone() < 1);
        assertTrue(KiRules.flareBlindTicks() <= 200 && KiRules.flareSlowTicks() <= 200);
        assertTrue(KiRules.senseRadius() <= 48 && KiRules.senseGlowTicks() <= 400);
        assertTrue(KiRules.ssjStrengthAmp() <= 2 && KiRules.ssjSpeedAmp() <= 1 && KiRules.kaiokenStrengthAmp() <= 2 && KiRules.kaiokenSpeedAmp() <= 1);
        assertTrue(KiRules.kaiokenStrengthAmp() > KiRules.ssjStrengthAmp(), "Kaioken is the greater burst");
    }
}
