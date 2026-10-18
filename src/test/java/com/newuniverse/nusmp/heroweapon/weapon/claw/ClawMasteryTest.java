package com.newuniverse.nusmp.heroweapon.weapon.claw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: Skill Mastery is the Claw's canon power-up, and it must be a smooth, bounded climb: five levels at fixed numbers of uses, each worth a fixed step, never more than 30
 * percent damage or 20 percent cooldown, and a level-up message exactly when a use crosses a threshold.
 */
class ClawMasteryTest {
    @Test
    void theLevelsComeAtTheirThresholds() {
        assertEquals(0, ClawMastery.level(0));
        assertEquals(0, ClawMastery.level(5));
        assertEquals(1, ClawMastery.level(6));
        assertEquals(2, ClawMastery.level(16));
        assertEquals(3, ClawMastery.level(32));
        assertEquals(4, ClawMastery.level(56));
        assertEquals(5, ClawMastery.level(90));
        assertEquals(5, ClawMastery.level(9000), "there is no level 6");
        assertEquals(0, ClawMastery.level(-4));
    }

    @Test
    void theClimbIsMonotonicAndBounded() {
        int last = 0;
        for (int uses = 0; uses < 200; uses++) {
            int level = ClawMastery.level(uses);
            assertTrue(level >= last, "never goes down");
            last = level;
        }
        assertEquals(1.0, ClawMastery.damageMult(0), 1e-9);
        assertEquals(1.3, ClawMastery.damageMult(5), 1e-9);
        assertEquals(1.3, ClawMastery.damageMult(50), 1e-9, "clamped");
        assertEquals(1.0, ClawMastery.damageMult(-3), 1e-9);
        assertEquals(1.0, ClawMastery.cooldownMult(0), 1e-9);
        assertEquals(0.8, ClawMastery.cooldownMult(5), 1e-9);
        assertEquals(0.8, ClawMastery.cooldownMult(50), 1e-9);
        assertEquals(ClawMastery.damageMult(5), ClawMastery.MAX_DAMAGE_MULT, 1e-9);
    }

    @Test
    void aLevelUpIsExactlyAThresholdCrossing() {
        assertFalse(ClawMastery.leveledUp(0));
        assertFalse(ClawMastery.leveledUp(4));
        assertTrue(ClawMastery.leveledUp(5), "the sixth use reaches level 1");
        assertFalse(ClawMastery.leveledUp(6));
        assertTrue(ClawMastery.leveledUp(15));
        assertTrue(ClawMastery.leveledUp(89));
        assertFalse(ClawMastery.leveledUp(90), "nothing above level 5");
        assertFalse(ClawMastery.leveledUp(-1));
    }
}
