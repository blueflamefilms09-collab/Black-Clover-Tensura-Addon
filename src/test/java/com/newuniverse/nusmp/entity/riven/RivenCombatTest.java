package com.newuniverse.nusmp.entity.riven;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RivenCombatTest {
    @Test
    void healthThresholdsUseAllFourPhasesWithoutHealing() {
        assertEquals(1, RivenCombat.phase(0.751f));
        assertEquals(2, RivenCombat.phase(0.75f));
        assertEquals(3, RivenCombat.phase(0.50f));
        assertEquals(4, RivenCombat.phase(0.25f));
    }

    @Test
    void damageBandsMatchTheRaisedCeilingSheet() {
        assertEquals(12f, RivenCombat.damage(1, false, 0f));
        assertEquals(18f, RivenCombat.damage(1, false, 1f));
        assertEquals(22f, RivenCombat.damage(2, false, 0f));
        assertEquals(34f, RivenCombat.damage(2, false, 1f));
        assertEquals(34f, RivenCombat.damage(3, false, 0f));
        assertEquals(48f, RivenCombat.damage(3, false, 1f));
        assertEquals(48f, RivenCombat.damage(4, false, 0f));
        assertEquals(64f, RivenCombat.damage(4, false, 1f));
        assertEquals(22f, RivenCombat.damage(1, true, 0.5f));
        assertEquals(42f, RivenCombat.damage(2, true, 0.5f));
        assertEquals(62f, RivenCombat.damage(3, true, 0.5f));
        assertEquals(85f, RivenCombat.damage(4, true, 0.5f));
    }

    @Test
    void everyCombatTellIsBetweenSevenTenthsAndFourteenTenthsOfASecond() {
        for (int phase = 1; phase <= 4; phase++) {
            for (int cast = 0; cast <= 100; cast++) {
                int ticks = RivenCombat.castTicks(cast, phase);
                assertEquals(true, ticks >= 14 && ticks <= 28, "cast=" + cast + " phase=" + phase);
            }
        }
    }
}
