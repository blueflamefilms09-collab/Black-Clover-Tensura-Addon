package com.newuniverse.nusmp.corpse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The numbers of a Soul Corpse zombie: growth with the caster's power, life, the cap, and the daylight rule. */
class SoulZombieRulesTest {
    @Test
    void aNoviceGetsTheBaseAndTheMightiestTheCapOfTheGrowth() {
        assertEquals(1.0, SoulZombieRules.scale(1.0), 1e-9);
        assertEquals(1.0, SoulZombieRules.scale(0.0), 1e-9, "a bad input never shrinks a zombie");
        assertEquals(1.4, SoulZombieRules.scale(12.0), 1e-9, "the same 1.0 to 1.4 range as the old puppets (SongRebuildKit.size)");
        assertEquals(1.2, SoulZombieRules.scale(5.0), 1e-9);
        double last = 0;
        for (double ep = 1; ep <= 12; ep += 0.5) {
            double s = SoulZombieRules.scale(ep);
            assertTrue(s >= last && s <= SoulZombieRules.MAX_SCALE);
            last = s;
        }
    }

    @Test
    void healthAndBlowGrowWithTheScaleAndStayInRange() {
        assertEquals(SoulZombieRules.BASE_HEALTH, SoulZombieRules.health(1.0), 1e-9);
        assertEquals(SoulZombieRules.BASE_HEALTH * 1.4, SoulZombieRules.health(1.4), 1e-9);
        assertEquals(SoulZombieRules.BASE_HEALTH * 1.4, SoulZombieRules.health(9.0), 1e-9, "a scale beyond the cap is clamped");
        assertEquals(SoulZombieRules.BASE_HEALTH, SoulZombieRules.health(0.2), 1e-9);
        assertTrue(SoulZombieRules.attack(1.4) > SoulZombieRules.attack(1.0));
        // a zombie of the mightiest caster is still far from a boss (BalanceLaw.isBoss is 300 health) and hits like a heavy mob, not a boss
        assertTrue(SoulZombieRules.health(1.4) < 100 && SoulZombieRules.attack(1.4) < 12);
        // more than a vanilla zombie (20 health, 3 damage), less than the named undead
        assertTrue(SoulZombieRules.BASE_HEALTH > 20 && SoulZombieRules.BASE_ATTACK > 3);
        for (NamedCorpses n : NamedCorpses.values()) assertTrue(n.health > SoulZombieRules.health(1.4), n + " must outlast a zombie");
    }

    @Test
    void lifeGrowsUpToHalfAndIsClamped() {
        assertEquals(500, SoulZombieRules.life(500, 1.0));
        assertEquals(720, SoulZombieRules.life(500, 12.0), "+4 percent per EP step, never more than +50 percent");
        assertTrue(SoulZombieRules.life(500, 100.0) <= 750);
        assertEquals(SoulZombieRules.MIN_LIFE, SoulZombieRules.life(1, 1.0));
        assertEquals(SoulZombieRules.MAX_LIFE, SoulZombieRules.life(1_000_000, 1.0));
        assertTrue(SoulZombieRules.life(400, 6.0) > SoulZombieRules.life(400, 2.0));
    }

    @Test
    void theCapDismissesTheOldestAndNeverMakesMoreThanTheCap() {
        assertEquals("[2, 0]", java.util.Arrays.toString(SoulZombieRules.fit(0, 2, 6)));
        assertEquals("[4, 0]", java.util.Arrays.toString(SoulZombieRules.fit(2, 4, 6)));
        assertEquals("[4, 2]", java.util.Arrays.toString(SoulZombieRules.fit(4, 4, 6)), "two of the four standing go so that six remain");
        assertEquals("[6, 6]", java.util.Arrays.toString(SoulZombieRules.fit(6, 20, 6)), "a mass summon is cut to the cap and replaces all");
        assertEquals("[1, 0]", java.util.Arrays.toString(SoulZombieRules.fit(0, 5, 0)), "a cap below one counts as one");
        assertEquals("[0, 0]", java.util.Arrays.toString(SoulZombieRules.fit(3, -4, 6)));
        for (int live = 0; live <= 16; live++)
            for (int want = 0; want <= 20; want++)
                for (int cap = 1; cap <= 16; cap++) {
                    int[] f = SoulZombieRules.fit(live, want, cap);
                    assertTrue(f[0] >= 0 && f[0] <= cap && f[0] <= want, "made: live " + live + " want " + want + " cap " + cap);
                    assertTrue(f[1] >= 0 && f[1] <= live, "dismissed: live " + live + " want " + want + " cap " + cap);
                    assertTrue(live - f[1] + f[0] <= cap, "the count after is within the cap: live " + live + " want " + want + " cap " + cap);
                }
    }

    @Test
    void daylightNeverBurnsASummonedZombie() {
        assertFalse(SoulZombieRules.SUN_BURNS);
    }

    @Test
    void theDefaultCapCoversTheBiggestSingleSummonOfOnePage() {
        assertTrue(SoulZombieRules.CAP_DEFAULT >= 4, "the Legion page raises four at once");
        assertTrue(SoulZombieRules.CAP_DEFAULT <= 16);
    }
}
