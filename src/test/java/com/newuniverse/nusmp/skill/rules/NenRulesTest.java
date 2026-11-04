package com.newuniverse.nusmp.skill.rules;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Nen (3.46.0) is an Ultimate with ten modes; every number and decision of it is pure, so the bounds and the trade-offs are pinned without a world. */
class NenRulesTest {
    @Test
    void tenModesWithUniqueIdsAndKeys() {
        assertEquals(10, NenRules.MODES);
        assertEquals(NenRules.MODES, NenRules.IDS.length);
        Set<String> ids = new HashSet<>(), keys = new HashSet<>();
        for (int m = 0; m < NenRules.MODES; m++) {
            assertTrue(NenRules.valid(m));
            assertTrue(ids.add(NenRules.IDS[m]), "duplicate id " + NenRules.IDS[m]);
            assertTrue(keys.add(NenRules.untilKey(m)), "duplicate tag key for mode " + m);
        }
        assertFalse(NenRules.valid(-1));
        assertFalse(NenRules.valid(NenRules.MODES));
        assertEquals("ten", NenRules.IDS[NenRules.TEN]);
        assertEquals("vow", NenRules.IDS[NenRules.VOW]);
    }

    @Test
    void everyModeCostsAndWaitsInsideItsBand() {
        for (int m = 0; m < NenRules.MODES; m++) {
            assertTrue(NenRules.cooldown(m) > 0 && NenRules.cooldown(m) <= 6000, "cooldown of " + NenRules.IDS[m]);
            assertTrue(NenRules.cost(m) >= 0 && NenRules.cost(m) <= 5000, "cost of " + NenRules.IDS[m]);
            if (m != NenRules.VOW) assertTrue(NenRules.cost(m) > 0, NenRules.IDS[m] + " must cost energy");
        }
        assertEquals(0.0, NenRules.cost(NenRules.VOW), "a vow costs freedom, not energy");
    }

    @Test
    void windowsAreShortAndOnlyTheDefensiveOnesCancel() {
        for (int m = 0; m < NenRules.MODES; m++) {
            assertEquals(NenRules.duration(m) > 0, NenRules.timed(m));
            assertTrue(NenRules.duration(m) <= 1200, "window of " + NenRules.IDS[m] + " must stay short");
            if (NenRules.cancellable(m)) assertTrue(NenRules.timed(m), "only a window can be cancelled: " + NenRules.IDS[m]);
        }
        assertTrue(NenRules.cancellable(NenRules.TEN) && NenRules.cancellable(NenRules.ZETSU) && NenRules.cancellable(NenRules.REN));
        assertTrue(NenRules.cancellable(NenRules.KEN) && NenRules.cancellable(NenRules.VOW));
        assertFalse(NenRules.cancellable(NenRules.KO), "Ko waits for its strike");
        assertFalse(NenRules.timed(NenRules.EMIT) || NenRules.timed(NenRules.ENHANCE) || NenRules.timed(NenRules.GYO) || NenRules.timed(NenRules.EN), "the instant modes have no window");
    }

    @Test
    void aWindowIsOpenUntilItsEndTime() {
        long end = NenRules.endsAt(1000, NenRules.REN);
        assertEquals(1000 + NenRules.duration(NenRules.REN), end);
        assertTrue(NenRules.active(end, 1000));
        assertTrue(NenRules.active(end, end - 1));
        assertFalse(NenRules.active(end, end));
        assertFalse(NenRules.active(0, 5), "an unset window is closed");
    }

    @Test
    void defenceTradesAreBoundedAndOrdered() {
        assertEquals(1.0, NenRules.takenMultiplier(false, false, false, false));
        assertEquals(0.8, NenRules.takenMultiplier(true, false, false, false), 1e-9);
        assertEquals(0.4, NenRules.takenMultiplier(false, true, false, false), 1e-9);
        assertTrue(NenRules.takenMultiplier(false, false, true, false) > 1.0, "Ko leaves you open");
        assertTrue(NenRules.takenMultiplier(false, false, false, true) > 1.0, "a vow leaves you open");
        for (int mask = 0; mask < 16; mask++) {
            double m = NenRules.takenMultiplier((mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0, (mask & 8) != 0);
            assertTrue(m >= 0.25 && m <= 1.5, "multiplier out of band for mask " + mask + ": " + m);
        }
        assertEquals(0.32, NenRules.takenMultiplier(true, true, false, false), 1e-9, "Ten and Ken together");
    }

    @Test
    void theVowForbidsTheDefencesAndPaysInHatsu() {
        assertTrue(NenRules.vowForbids(NenRules.TEN) && NenRules.vowForbids(NenRules.ZETSU) && NenRules.vowForbids(NenRules.KEN));
        assertFalse(NenRules.vowForbids(NenRules.REN) || NenRules.vowForbids(NenRules.ENHANCE) || NenRules.vowForbids(NenRules.EMIT) || NenRules.vowForbids(NenRules.KO));
        assertEquals(1.0, NenRules.hatsuMultiplier(false));
        assertTrue(NenRules.hatsuMultiplier(true) > 1.0 && NenRules.hatsuMultiplier(true) <= 2.0, "the reward is real but bounded");
    }

    @Test
    void attackAndSenseNumbersStayBounded() {
        assertTrue(NenRules.enhanceDamage() > 0 && NenRules.enhanceDamage() <= 40);
        assertTrue(NenRules.emitDamage() > 0 && NenRules.emitDamage() <= 40);
        assertTrue(NenRules.koDamage() > NenRules.enhanceDamage(), "Ko is the heavy single strike");
        assertTrue(NenRules.koDamage() * NenRules.hatsuMultiplier(true) <= 80);
        assertTrue(NenRules.emitRange() <= 32 && NenRules.emitWidth() <= 3 && NenRules.emitPierce() <= 4, "the bolt looks at a handful of creatures at most");
        assertTrue(NenRules.enhanceReach() <= 6);
        assertTrue(NenRules.enRadius() <= 24 && NenRules.gyoRadius() <= 32 && NenRules.zetsuRadius() <= 24);
        assertTrue(NenRules.gyoMaxRevealed() <= 16);
        assertTrue(NenRules.enGlowTicks() <= 400 && NenRules.gyoRevealTicks() <= 400 && NenRules.gyoNightTicks() <= 2400);
        assertTrue(NenRules.renStrengthAmp() <= 2 && NenRules.renSpeedAmp() <= 1 && NenRules.kenSlowAmp() <= 2);
    }

    @Test
    void seventeenModesAndTheWheel() {
        assertEquals(17, NenRules.MODES);
        assertEquals(NenRules.MODES, NenRules.IDS.length);
        assertEquals(1.0, NenRules.efficiency(NenRules.T_ENHANCE, NenRules.T_ENHANCE));
        assertEquals(0.8, NenRules.efficiency(NenRules.T_ENHANCE, NenRules.T_TRANS));
        assertEquals(0.8, NenRules.efficiency(NenRules.T_ENHANCE, NenRules.T_EMIT));
        assertEquals(0.4, NenRules.efficiency(NenRules.T_ENHANCE, NenRules.T_SPEC));
        for (int m = 0; m < NenRules.MODES; m++) assertTrue(NenRules.hatsu(m) == (NenRules.typeOfMode(m) >= 0));
        for (long a = 0; a < 50; a++) { int t = NenRules.divine(a * 7919L, a * 104729L); assertTrue(t >= 0 && t < 6); }
        assertEquals(NenRules.divine(5L, 9L), NenRules.divine(5L, 9L));
    }

    @Test
    void zetsuIsDefencelessAndRyuGuards() {
        assertTrue(NenRules.takenMultiplier(false, false, false, false, true, false) > 1.0);
        assertTrue(NenRules.takenMultiplier(false, false, false, false, false, true) < 1.0);
        double hi = NenRules.takenMultiplier(true, true, true, true, true, true), lo = NenRules.takenMultiplier(true, true, false, false, false, true);
        assertTrue(hi <= 1.5 && lo >= 0.25);
    }

    @Test
    void newHatsuAreBounded() {
        assertTrue(NenRules.transMax() <= 6 && NenRules.conjDamage() > 0 && NenRules.specRadius() <= 16 && NenRules.manipRange() <= 24);
        assertTrue(NenRules.duration(NenRules.SHU) > 0 && NenRules.duration(NenRules.IN) > 0 && NenRules.duration(NenRules.RYU) > 0);
    }
}
