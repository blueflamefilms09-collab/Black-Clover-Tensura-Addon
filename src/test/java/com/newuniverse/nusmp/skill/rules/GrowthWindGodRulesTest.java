package com.newuniverse.nusmp.skill.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Liaris Freese must not be farmable, Airiel's Tempest must help the dash, and the gods' mastered tier must be stronger. */
class GrowthWindGodRulesTest {
    @Test
    void ownPetsSelfKillsAndArmorStandsNeverCount() {
        assertTrue(GrowthRules.countsAsKill(false, false, false));
        assertFalse(GrowthRules.countsAsKill(true, false, false));
        assertFalse(GrowthRules.countsAsKill(false, true, false));
        assertFalse(GrowthRules.countsAsKill(false, false, true));
    }

    @Test
    void aPayoutNeedsTheChanceAndAQuietSecond() {
        assertTrue(GrowthRules.pays(0.5, 0.2, 1000, 0));
        assertFalse(GrowthRules.pays(0.5, 0.7, 1000, 0));
        assertFalse(GrowthRules.pays(1.0, 0.0, 1005, 1000), "second payout inside the guard");
        assertTrue(GrowthRules.pays(1.0, 0.0, 1020, 1000));
        assertTrue(GrowthRules.pays(1.0, 0.0, 5, 1000), "a clock that went backwards does not lock the skill forever");
        assertFalse(GrowthRules.pays(0.0, 0.0, 1000, 0));
        assertTrue(GrowthRules.pays(5.0, 0.99, 1000, 0), "chance above 1 is clamped, not an error");
    }

    @Test
    void bossesPayTriple() {
        assertEquals(1, GrowthRules.amount(1, false));
        assertEquals(3, GrowthRules.amount(1, true));
        assertEquals(0, GrowthRules.amount(0, true));
        assertEquals(0, GrowthRules.amount(-2, false));
    }

    @Test
    void tempestSpeedsUpTheDashAndShieldsFromArrows() {
        assertTrue(WindRules.tempestTicks(Tier.MASTERED) > WindRules.tempestTicks(Tier.BASE));
        assertTrue(WindRules.dashPower(Tier.MASTERED) > WindRules.dashPower(Tier.BASE));
        assertTrue(WindRules.dashCooldown(true) < WindRules.dashCooldown(false));
        assertEquals(100.0, WindRules.dashCost(300.0), 0.0001);
        assertTrue(WindRules.projectileShield(Tier.BASE) > 0 && WindRules.projectileShield(Tier.MASTERED) < 1);
        assertTrue(WindRules.tempestActive(10, 20));
        assertFalse(WindRules.tempestActive(20, 20));
    }

    @Test
    void everyGodGetsStrongerWhenMastered() {
        assertTrue(GodRules.Hestia.radius(Tier.MASTERED) > GodRules.Hestia.radius(Tier.BASE));
        assertTrue(GodRules.Hestia.healShare(Tier.MASTERED) > GodRules.Hestia.healShare(Tier.BASE));
        assertFalse(GodRules.Hestia.cleanses(Tier.BASE));
        assertTrue(GodRules.Hestia.cleanses(Tier.MASTERED));
        assertTrue(GodRules.Freya.dazeTicks(Tier.MASTERED) > GodRules.Freya.dazeTicks(Tier.BASE));
        assertTrue(GodRules.Freya.charmTicks(Tier.MASTERED) > GodRules.Freya.charmTicks(Tier.BASE));
        assertTrue(GodRules.Loki.blinkRange(Tier.MASTERED) > GodRules.Loki.blinkRange(Tier.BASE));
        assertTrue(GodRules.Hephaestus.armor(Tier.MASTERED));
        assertFalse(GodRules.Hephaestus.armor(Tier.BASE));
    }

    @Test
    void repairNeverRestoresMoreThanWasLost() {
        assertEquals(0, GodRules.Hephaestus.restored(0, 100, Tier.BASE));
        assertEquals(40, GodRules.Hephaestus.restored(40, 100, Tier.BASE));
        assertEquals(0, GodRules.Hephaestus.restored(40, 0, Tier.BASE));
        assertEquals(0, GodRules.Hephaestus.restored(-5, 100, Tier.MASTERED));
    }
}
