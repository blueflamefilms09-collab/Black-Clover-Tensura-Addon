package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the numbers behind wave 101 are pure ({@link Wave101Rules}) so they can be tested without the game: what the Millennium set lifts at each count of worn pieces, the power factor of its acts, the fatal-blow test of the
 * Puzzle's second soul, the Necklace's foresight cut, the marks a foe can carry, who the Scale weighs, and what the Ring draws.
 */
class Wave101RulesTest {
    @Test
    void theSetLiftsMoreAndMoreAsPiecesAreAdded() {
        assertEquals(7, Wave101Rules.SET.length);
        for (String id : Wave101Rules.SET) assertTrue(Wave101Rules.inSet(id), id);
        assertFalse(Wave101Rules.inSet("blackrazor"));
        assertFalse(Wave101Rules.inSet(null));
        assertEquals(-1, Wave101Rules.resistAmp(2));
        assertEquals(0, Wave101Rules.resistAmp(3));
        assertEquals(1, Wave101Rules.resistAmp(7));
        assertEquals(-1, Wave101Rules.regenAmp(3));
        assertEquals(0, Wave101Rules.regenAmp(4));
        assertEquals(1, Wave101Rules.regenAmp(7));
        assertEquals(-1, Wave101Rules.strengthAmp(4));
        assertEquals(0, Wave101Rules.strengthAmp(5));
        assertEquals(1, Wave101Rules.strengthAmp(7));
        assertEquals(-1, Wave101Rules.speedAmp(5));
        assertEquals(0, Wave101Rules.speedAmp(6));
        assertEquals(0, Wave101Rules.speedAmp(7));
        int last = -4;
        for (int n = 0; n <= 7; n++) {
            int total = Wave101Rules.resistAmp(n) + Wave101Rules.regenAmp(n) + Wave101Rules.strengthAmp(n) + Wave101Rules.speedAmp(n);
            assertTrue(total >= last, "the set never gives less with another piece: " + n);
            last = total;
        }
    }

    @Test
    void thePowerFactorGrowsFivePercentAPieceToThirty() {
        assertEquals(1.0, Wave101Rules.power(0), 1e-9);
        assertEquals(1.0, Wave101Rules.power(1), 1e-9);
        assertEquals(1.05, Wave101Rules.power(2), 1e-9);
        assertEquals(1.30, Wave101Rules.power(7), 1e-9);
        assertEquals(1.30, Wave101Rules.power(12), 1e-9, "more than seven is still seven");
    }

    @Test
    void theSecondSoulTakesAFatalBlowOnly() {
        assertTrue(Wave101Rules.fatal(30f, 20f, 0f));
        assertTrue(Wave101Rules.fatal(20f, 20f, 0f));
        assertFalse(Wave101Rules.fatal(19.9f, 20f, 0f));
        assertFalse(Wave101Rules.fatal(30f, 20f, 15f), "absorption counts as life");
        assertFalse(Wave101Rules.fatal(0f, 1f, 0f));
        assertEquals(40f, Wave101Rules.soulHealth(100f, 3f), 1e-4);
        assertEquals(60f, Wave101Rules.soulHealth(100f, 60f), 1e-4, "never lower than the wearer has");
        assertEquals(6000, Wave101Rules.SOUL_COOLDOWN);
    }

    @Test
    void foresightHalvesAHeavyBlowOnly() {
        assertEquals(10f, Wave101Rules.foreseen(20f), 1e-4);
        assertEquals(2f, Wave101Rules.foreseen(2f), 1e-4, "a nettle is not foreseen");
        assertEquals(1.5f, Wave101Rules.foreseen(3f), 1e-4);
        assertEquals(160, Wave101Rules.FORESIGHT_COOLDOWN);
    }

    @Test
    void marksAddUpButNeverPastTheCap() {
        assertEquals(0.0, Wave101Rules.markBonus(false, false, false), 1e-9);
        assertEquals(0.25, Wave101Rules.markBonus(true, false, false), 1e-9);
        assertEquals(0.20, Wave101Rules.markBonus(false, true, false), 1e-9);
        assertEquals(0.30, Wave101Rules.markBonus(false, false, true), 1e-9);
        assertEquals(0.55, Wave101Rules.markBonus(true, false, true), 1e-9);
        assertEquals(Wave101Rules.MARK_CAP, Wave101Rules.markBonus(true, true, true), 1e-9);
        assertTrue(Wave101Rules.MARK_CAP <= 0.6);
    }

    @Test
    void theScaleWeighsMonstersAndNeverPlayersOrAnimals() {
        assertTrue(Wave101Rules.sinful(false, true, false, false, false));
        assertTrue(Wave101Rules.sinful(false, false, true, false, false));
        assertTrue(Wave101Rules.sinful(false, false, false, true, false));
        assertTrue(Wave101Rules.sinful(false, false, false, false, true));
        assertFalse(Wave101Rules.sinful(true, true, true, true, true), "never a player");
        assertFalse(Wave101Rules.sinful(false, false, false, false, false), "an animal or a villager is spared");
        assertEquals(60f, Wave101Rules.verdictHeal(5), 1e-4);
        assertEquals(60f, Wave101Rules.verdictHeal(9), 1e-4, "up to five hearts");
        assertEquals(0f, Wave101Rules.verdictHeal(-1), 1e-4);
    }

    @Test
    void theRingDrawsAFewPercentAFoeUpToACap() {
        assertEquals(0.0, Wave101Rules.draw(0), 1e-9);
        assertEquals(0.04, Wave101Rules.draw(1), 1e-9);
        assertEquals(0.24, Wave101Rules.draw(6), 1e-9);
        assertEquals(0.24, Wave101Rules.draw(12), 1e-9);
    }
}
