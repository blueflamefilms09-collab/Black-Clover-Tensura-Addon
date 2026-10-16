package com.newuniverse.nusmp.soul;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Who may be purified and with what chance: the immunity table, the reliable rule for weakened hostiles, the bounds, the channel break. */
class PaladinRulesTest {
    /** A plain hostile mob at the given health fraction, equal energy, no resistance, nothing special about it. */
    private static PaladinRules.Subject plain(double hp) {
        return new PaladinRules.Subject(false, false, false, false, false, false, false, false, false, true, hp, 1000, 1000, 0, true);
    }

    private static PaladinRules.Subject with(boolean player, boolean boss, boolean deny, boolean named, boolean family, boolean bossPal,
                                             boolean mine, boolean other, boolean pet, boolean alive, double tEp, double cEp) {
        return new PaladinRules.Subject(player, boss, deny, named, family, bossPal, mine, other, pet, alive, 0.4, tEp, cEp, 0, true);
    }

    @Test
    void anOrdinaryHostileMobIsAllowed() {
        assertEquals(PaladinRules.Verdict.OK, PaladinRules.verdict(plain(1.0)));
        assertTrue(PaladinRules.allowed(plain(0.3)));
    }

    @Test
    void everyImmunityRefuses() {
        assertEquals(PaladinRules.Verdict.PLAYER, PaladinRules.verdict(with(true, false, false, false, false, false, false, false, false, true, 1, 1)));
        assertEquals(PaladinRules.Verdict.BOSS, PaladinRules.verdict(with(false, true, false, false, false, false, false, false, false, true, 1, 1)));
        assertEquals(PaladinRules.Verdict.DENIED, PaladinRules.verdict(with(false, false, true, false, false, false, false, false, false, true, 1, 1)));
        assertEquals(PaladinRules.Verdict.NAMED, PaladinRules.verdict(with(false, false, false, true, false, false, false, false, false, true, 1, 1)));
        assertEquals(PaladinRules.Verdict.FAMILY, PaladinRules.verdict(with(false, false, false, false, true, false, false, false, false, true, 1, 1)));
        assertEquals(PaladinRules.Verdict.BOSS_PALADIN, PaladinRules.verdict(with(false, false, false, false, false, true, false, false, false, true, 1, 1)));
        assertEquals(PaladinRules.Verdict.ALREADY, PaladinRules.verdict(with(false, false, false, false, false, false, true, false, false, true, 1, 1)));
        assertEquals(PaladinRules.Verdict.OTHER_PALADIN, PaladinRules.verdict(with(false, false, false, false, false, false, false, true, false, true, 1, 1)));
        assertEquals(PaladinRules.Verdict.PET, PaladinRules.verdict(with(false, false, false, false, false, false, false, false, true, true, 1, 1)));
        assertEquals(PaladinRules.Verdict.DEAD, PaladinRules.verdict(with(false, false, false, false, false, false, false, false, false, false, 1, 1)));
    }

    @Test
    void aSoulFarStrongerThanTheCasterIsRefusedButANearOneIsNot() {
        assertEquals(PaladinRules.Verdict.TOO_STRONG, PaladinRules.verdict(with(false, false, false, false, false, false, false, false, false, true, 100_001, 10_000)));
        assertEquals(PaladinRules.Verdict.OK, PaladinRules.verdict(with(false, false, false, false, false, false, false, false, false, true, 99_999, 10_000)));
        assertEquals(PaladinRules.Verdict.OK, PaladinRules.verdict(with(false, false, false, false, false, false, false, false, false, true, 1e9, 0)), "an unknown caster energy never refuses");
    }

    @Test
    void everyRefusalHasAMessage() {
        for (PaladinRules.Verdict v : PaladinRules.Verdict.values()) {
            if (v == PaladinRules.Verdict.OK) assertEquals("", PaladinRules.refusal(v));
            else assertFalse(PaladinRules.refusal(v).isEmpty(), v.name());
        }
    }

    @Test
    void aWeakenedHostileMobConvertsReliably() {
        for (double hp : new double[]{0.5, 0.3, 0.05}) {
            for (double mastery : new double[]{0.0, 0.5, 1.0}) {
                assertTrue(PaladinRules.chance(hp, 1000, 1000, 0.0, mastery, true) >= PaladinRules.RELIABLE_CHANCE, "hp " + hp + " mastery " + mastery);
            }
        }
    }

    @Test
    void aHealthyOrNonHostileMobIsHarder() {
        double healthy = PaladinRules.chance(1.0, 1000, 1000, 0.0, 0.0, true);
        double hurt = PaladinRules.chance(0.4, 1000, 1000, 0.0, 0.0, true);
        assertTrue(healthy < hurt);
        assertTrue(healthy < PaladinRules.RELIABLE_CHANCE);
        assertTrue(PaladinRules.chance(1.0, 1000, 1000, 0.0, 0.0, false) < healthy, "a hostile creature is easier than a passive one");
    }

    @Test
    void masteryAndEnergyMoveTheChanceTheRightWay() {
        assertTrue(PaladinRules.chance(0.9, 1000, 1000, 0, 1.0, true) > PaladinRules.chance(0.9, 1000, 1000, 0, 0.0, true));
        assertTrue(PaladinRules.chance(0.9, 100, 1000, 0, 0.5, true) > PaladinRules.chance(0.9, 1000, 1000, 0, 0.5, true), "a weaker target is easier");
        assertTrue(PaladinRules.chance(0.9, 8000, 1000, 0, 0.5, true) < PaladinRules.chance(0.9, 1000, 1000, 0, 0.5, true), "a stronger target is harder");
        assertTrue(PaladinRules.chance(0.9, 1000, 1000, 0.8, 0.5, true) < PaladinRules.chance(0.9, 1000, 1000, 0.0, 0.5, true), "resistance lowers it");
    }

    @Test
    void theChanceStaysInsideItsBoundsForAnyInput() {
        double[] vals = {-5, 0, 0.5, 1, 7, Double.NaN};
        for (double hp : vals) for (double r : vals) for (double m : vals) for (boolean h : new boolean[]{true, false}) {
            double c = PaladinRules.chance(hp, 5, 5e6, r, m, h);
            assertTrue(c >= PaladinRules.MIN_CHANCE && c <= PaladinRules.MAX_CHANCE, "chance " + c);
            double d = PaladinRules.chance(hp, 5e9, 5, r, m, h);
            assertTrue(d >= PaladinRules.MIN_CHANCE && d <= PaladinRules.MAX_CHANCE, "chance " + d);
        }
    }

    @Test
    void aFullyResistantWeakenedHostileIsNotGuaranteed() {
        assertTrue(PaladinRules.chance(0.2, 1000, 1000, 1.0, 0.0, true) < PaladinRules.RELIABLE_CHANCE);
    }

    @Test
    void theChannelBreaksOnDistanceDamageAndDeath() {
        assertFalse(PaladinRules.interrupted(5, false, true, true, true));
        assertTrue(PaladinRules.interrupted(PaladinRules.LEASH + 0.1, false, true, true, true));
        assertTrue(PaladinRules.interrupted(5, true, true, true, true));
        assertTrue(PaladinRules.interrupted(5, false, false, true, true));
        assertTrue(PaladinRules.interrupted(5, false, true, false, true));
        assertTrue(PaladinRules.interrupted(5, false, true, true, false));
        assertEquals(60, PaladinRules.CHANNEL_TICKS);
    }

    @Test
    void failureWeaknessIsShortAndGrowsWithMastery() {
        assertEquals(80, PaladinRules.failWeaknessTicks(0.1));
        assertEquals(120, PaladinRules.failWeaknessTicks(0.9));
    }
}
