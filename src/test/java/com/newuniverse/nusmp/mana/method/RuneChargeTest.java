package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.RuneCharge.Charge;
import com.newuniverse.nusmp.mana.method.RuneCharge.Stage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Hold-to-charge: the curve from ticks held to a stage, a power, a size and a tier; the settings' ranges; the 3.4.0 spell kept as LEGACY. */
class RuneChargeTest {
    private static final int MIN = RuneCharge.DEFAULT_MIN_HOLD, MAX = RuneCharge.DEFAULT_MAX_HOLD;

    @Test
    void aTapIsBasicAndAFullHoldIsFull() {
        assertEquals(Stage.BASIC, RuneCharge.stage(0, MIN, MAX));
        assertEquals(Stage.BASIC, RuneCharge.stage(MIN - 1, MIN, MAX));
        assertEquals(Stage.CHARGED, RuneCharge.stage(MIN, MIN, MAX), "the minimum hold itself is already charged");
        assertEquals(Stage.CHARGED, RuneCharge.stage(MAX - 1, MIN, MAX));
        assertEquals(Stage.FULL, RuneCharge.stage(MAX, MIN, MAX));
        assertEquals(Stage.FULL, RuneCharge.stage(500, MIN, MAX));
    }

    @Test
    void thePowerRisesWithTheHoldAndNeverFallsBack() {
        double last = -1.0;
        for (int h = 0; h <= RuneCharge.HOLD_CAP; h++) {
            Charge c = RuneCharge.of(h, MIN, MAX);
            assertTrue(c.power() >= last - 1e-9, "power must not fall at " + h + ": " + c.power() + " < " + last);
            last = c.power();
            assertTrue(c.size() > 0 && c.growth() > 0);
            assertFalse(c.legacy());
        }
        assertEquals(RuneCharge.BASIC_POWER, RuneCharge.of(0, MIN, MAX).power(), 1e-12);
        assertEquals(RuneCharge.FULL_POWER, RuneCharge.of(MAX, MIN, MAX).power(), 1e-12);
        assertTrue(RuneCharge.of(0, MIN, MAX).power() < 1.0, "a tap is weaker than the 3.4.0 spell");
        assertTrue(RuneCharge.of(MAX, MIN, MAX).power() > 1.0, "a full hold is stronger than the 3.4.0 spell");
    }

    @Test
    void theChargedRangeIsSmoothAtBothEdges() {
        double justBelow = RuneCharge.of(MIN - 1, MIN, MAX).power(), atMin = RuneCharge.of(MIN, MIN, MAX).power();
        assertEquals(RuneCharge.CHARGED_POWER_MIN, atMin, 1e-12);
        assertTrue(atMin > justBelow, "the first charged tick is above the tap");
        assertEquals(RuneCharge.CHARGED_POWER_MAX, RuneCharge.of(MAX, MIN, MAX).power(), 1e-12, "the charged range ends at the full power");
        double mid = RuneCharge.of((MIN + MAX) / 2, MIN, MAX).power();
        assertTrue(mid > RuneCharge.CHARGED_POWER_MIN && mid < RuneCharge.CHARGED_POWER_MAX);
    }

    @Test
    void theTiersAreOneTwoThreeAndOnlyAFullHoldWritesTheSecondRune() {
        assertEquals(1, RuneCharge.of(0, MIN, MAX).tier());
        assertEquals(2, RuneCharge.of(MIN + 3, MIN, MAX).tier());
        assertEquals(3, RuneCharge.of(MAX, MIN, MAX).tier());
        assertFalse(RuneCharge.of(1, MIN, MAX).allowsSecondRune(), "a tap never writes both runes");
        assertTrue(RuneCharge.of(MIN, MIN, MAX).allowsSecondRune());
        assertTrue(RuneCharge.of(MAX, MIN, MAX).allowsSecondRune());
        assertTrue(Charge.LEGACY.allowsSecondRune() && Charge.LEGACY.tier() == 3);
    }

    @Test
    void sneakingWritesBothRunesOnlyWhereTheStageAllowsIt() {
        for (RuneElement e : RuneElement.values()) {
            assertEquals(e.defaultMods(), RuneCharge.modsFor(RuneCharge.of(0, MIN, MAX), e, true), "a tap with sneak is still the page's own rune: " + e);
            assertEquals(RuneMods.ALL, RuneCharge.modsFor(RuneCharge.of(MAX, MIN, MAX), e, true));
            assertEquals(e.defaultMods(), RuneCharge.modsFor(RuneCharge.of(MAX, MIN, MAX), e, false));
            assertEquals(RuneMods.ALL, RuneCharge.modsFor(Charge.LEGACY, e, true), "the 3.4.0 cast: sneak writes both");
            assertEquals(e.defaultMods(), RuneCharge.modsFor(Charge.LEGACY, e, false));
        }
    }

    @Test
    void theLegacySpellIsTheOldNumbers() {
        assertTrue(Charge.LEGACY.legacy());
        assertEquals(1.0, Charge.LEGACY.power());
        assertEquals(1.0, Charge.LEGACY.size());
        assertEquals(1.0, Charge.LEGACY.growth());
        assertEquals(18, Charge.LEGACY.castTicks(18), "the old fixed charge");
        assertEquals(26, Charge.LEGACY.castTicks(26));
        assertEquals(RuneCharge.FINISH_BASIC, RuneCharge.of(0, MIN, MAX).castTicks(18));
        assertEquals(RuneCharge.FINISH_FULL, RuneCharge.of(MAX, MIN, MAX).castTicks(18));
        assertTrue(RuneCharge.FINISH_BASIC < RuneCharge.FINISH_CHARGED && RuneCharge.FINISH_CHARGED < RuneCharge.FINISH_FULL);
    }

    @Test
    void theSettingsAreKeptInRangeAndTheTwoHoldsNeverCross() {
        assertEquals(RuneCharge.MIN_HOLD_LOW, RuneCharge.minHold(-5));
        assertEquals(RuneCharge.MIN_HOLD_HIGH, RuneCharge.minHold(9999));
        for (int cfgMin = -10; cfgMin <= 200; cfgMin += 7)
            for (int cfgMax = -10; cfgMax <= 400; cfgMax += 13) {
                int min = RuneCharge.minHold(cfgMin), max = RuneCharge.maxHold(cfgMin, cfgMax);
                assertTrue(max >= min + RuneCharge.MIN_SPAN, "the full hold stays above the minimum hold: " + cfgMin + "/" + cfgMax);
                assertTrue(max <= Math.max(RuneCharge.MAX_HOLD_HIGH, min + RuneCharge.MIN_SPAN));
            }
    }

    @Test
    void bogusHoldsNeverBreakTheCurve() {
        assertEquals(0, RuneCharge.of(-50, MIN, MAX).held());
        assertEquals(RuneCharge.HOLD_CAP, RuneCharge.of(Integer.MAX_VALUE, MIN, MAX).held());
        assertEquals(1.0, RuneCharge.fraction(25, 20, 20), "a collapsed range cannot divide by zero");
        assertEquals(0.0, RuneCharge.fraction(10, 20, 20), 1e-12);
        for (Stage s : Stage.values()) {
            assertTrue(RuneCharge.power(s, 0.5) > 0 && RuneCharge.size(s, 0.5) > 0 && RuneCharge.growth(s, 0.5) > 0);
            assertTrue(RuneCharge.finishTicks(s) > 0 && RuneCharge.tier(s) >= 1);
        }
    }
}
