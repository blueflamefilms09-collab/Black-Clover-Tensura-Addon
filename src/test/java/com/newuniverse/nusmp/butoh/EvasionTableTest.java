package com.newuniverse.nusmp.butoh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The evasion odds: 35 to 65 percent by move, capped by the config, 20 points less against bosses, never against unavoidable damage. */
class EvasionTableTest {
    @Test
    void everyMoveStaysBetweenThirtyFiveAndSixtyFivePercent() {
        for (DanceMove m : DanceMove.values()) {
            assertTrue(m.evasion >= 0.35 - 1e-9 && m.evasion <= 0.65 + 1e-9, m + " " + m.evasion);
            assertEquals(m.evasion, EvasionTable.chance(m, false, EvasionTable.DEFAULT_CAP), 1e-9, "the default cap does not cut a move");
        }
    }

    @Test
    void theCapHoldsTheChanceDown() {
        for (DanceMove m : DanceMove.values()) {
            assertTrue(EvasionTable.chance(m, false, 0.5) <= 0.5 + 1e-9);
            assertEquals(0.0, EvasionTable.chance(m, false, 0.0), 1e-9);
        }
    }

    @Test
    void itIsNeverAGuarantee() {
        for (DanceMove m : DanceMove.values()) {
            assertTrue(EvasionTable.chance(m, false, 5.0) <= EvasionTable.HARD_CEILING + 1e-9, "a silly config value is still held below the ceiling");
            assertFalse(EvasionTable.dodges(EvasionTable.chance(m, false, 5.0), 0.99), "a roll of 0.99 always gets through");
        }
    }

    @Test
    void bossesTakeTwentyPointsOff() {
        for (DanceMove m : DanceMove.values()) {
            double normal = EvasionTable.chance(m, false, 0.65), boss = EvasionTable.chance(m, true, 0.65);
            assertEquals(Math.max(0, normal - 0.20), boss, 1e-9, m.toString());
            assertTrue(boss >= 0.0);
        }
        assertEquals(0.15, EvasionTable.chance(DanceMove.DRIFT, true, 0.65), 1e-9);
    }

    @Test
    void unavoidableDamageIsNeverDodged() {
        assertTrue(EvasionTable.eligible(false, false, true));
        assertFalse(EvasionTable.eligible(true, false, true), "/kill and anything bypassing invulnerability");
        assertFalse(EvasionTable.eligible(false, true, true), "out of the world");
        assertFalse(EvasionTable.eligible(false, false, false), "fall, fire, poison: no attacker, nothing to slip");
    }

    @Test
    void theRollIsStrictlyBelowTheChance() {
        assertTrue(EvasionTable.dodges(0.5, 0.0));
        assertTrue(EvasionTable.dodges(0.5, 0.4999));
        assertFalse(EvasionTable.dodges(0.5, 0.5));
        assertFalse(EvasionTable.dodges(0.0, 0.0));
    }

    @Test
    void theLongRunRateMatchesTheChance() {
        java.util.Random r = new java.util.Random(42);
        double chance = EvasionTable.chance(DanceMove.FOLD, false, 0.65);
        int hit = 0, n = 200_000;
        for (int i = 0; i < n; i++) if (EvasionTable.dodges(chance, r.nextDouble())) hit++;
        assertEquals(chance, hit / (double) n, 0.01);
    }
}
