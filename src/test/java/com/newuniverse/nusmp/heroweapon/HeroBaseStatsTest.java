package com.newuniverse.nusmp.heroweapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: the melee numbers of the 21 hero weapons are a table, so a weapon appended to the enum without a row, a typo that makes a Whip hit like a Hammer, or a
 * speed that lifts a weapon far above the vanilla netherite sword must fail a test, not be found by a player. The owner's list of damages and reaches is pinned
 * row by row.
 */
class HeroBaseStatsTest {
    @Test
    void everyWeaponHasARowOfItsOwn() {
        for (HeroKind k : HeroKind.values()) assertTrue(HeroBaseStats.has(k), k + " has no row in HeroBaseStats");
    }

    @Test
    void theOwnersDamagesAndReachesAreKept() {
        double[][] want = {   // damage, extra reach
                {2, 0}, {7, 0}, {7, 1.5}, {2, 0},                 // shield, sword, spear, bow
                {9, 0}, {5, 0}, {9, 0}, {4, 2}, {3, 0}, {6, 0}, {3, 0},   // hammer, claw, axe, whip, staff, gauntlet, projectile
                {2, 0}, {5, 0}, {8, 1}, {7, 0}, {2, 0}, {2, 0}, {2, 0}, {6, 1}, {2, 0}, {6, 2}};   // carriage, fan, scythe, katana, book, mirror, instrument, harpoon, sewing set, chain
        HeroKind[] kinds = HeroKind.values();
        assertEquals(want.length, kinds.length, "one row of the owner's list per weapon, in catalogue order");
        for (int i = 0; i < kinds.length; i++) {
            HeroBaseStats.Row r = HeroBaseStats.of(kinds[i]);
            assertEquals(want[i][0], r.damage(), 1e-9, kinds[i] + " damage");
            assertEquals(want[i][1], r.reach(), 1e-9, kinds[i] + " reach");
        }
    }

    @Test
    void slowAndFastWeaponsAreSlowAndFast() {
        assertTrue(HeroBaseStats.of(HeroKind.HAMMER).speed() < 1.0, "the Hammer is slow");
        assertTrue(HeroBaseStats.of(HeroKind.CLAW).speed() >= 2.2, "the Claw is fast");
        assertTrue(HeroBaseStats.of(HeroKind.GAUNTLET).speed() >= 2.2, "the Gauntlet is fast");
        assertTrue(HeroBaseStats.of(HeroKind.KATANA).speed() >= 2.0, "the Katana is fast");
        assertTrue(HeroBaseStats.of(HeroKind.HAMMER).speed() < HeroBaseStats.of(HeroKind.AXE).speed() + 0.2);
    }

    @Test
    void noWeaponOutDamagesANetheriteSwordByMoreThanAFifth() {
        double netherite = 8.0 * 1.6;
        for (HeroKind k : HeroKind.values()) {
            HeroBaseStats.Row r = HeroBaseStats.of(k);
            assertTrue(r.dps() <= netherite * 1.2, k + " does " + r.dps() + " per second");
            assertTrue(r.speed() > 0.0 && r.speed() <= 4.0, k + " speed " + r.speed() + " must be attackable and no faster than a fist");
        }
    }

    @Test
    void theModifiersAreTheTotalsMinusThePlayersOwn() {
        HeroBaseStats.Row sword = HeroBaseStats.of(HeroKind.SWORD);
        assertEquals(6.0, sword.damageModifier(), 1e-9);
        assertEquals(-2.2, sword.speedModifier(), 1e-9);
        assertEquals(1.0, HeroBaseStats.PLAYER_BASE_DAMAGE, 1e-9);
        assertEquals(4.0, HeroBaseStats.PLAYER_BASE_SPEED, 1e-9);
    }
}
