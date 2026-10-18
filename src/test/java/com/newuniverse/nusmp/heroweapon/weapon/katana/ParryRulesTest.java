package com.newuniverse.nusmp.heroweapon.weapon.katana;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: the parry must not make the wielder immune, so what it stops and what it returns are pinned: a small blow is stopped whole, a huge one only up to 60
 * percent of the wielder's maximum health (the rest gets through), nothing is stopped for no damage, and the answering cut is 4 plus half the blow, between 4
 * and 12.
 */
class ParryRulesTest {
    @Test
    void aSmallBlowIsStoppedWhole() {
        assertEquals(5.0f, ParryRules.stopped(5.0f, 20.0f), 1e-6f);
        assertEquals(0.0f, ParryRules.passThrough(5.0f, 20.0f), 1e-6f);
        assertEquals(12.0f, ParryRules.stopped(12.0f, 20.0f), 1e-6f, "exactly 60 percent of 20");
    }

    @Test
    void aHugeBlowGetsPartlyThrough() {
        assertEquals(12.0f, ParryRules.stopped(40.0f, 20.0f), 1e-6f);
        assertEquals(28.0f, ParryRules.passThrough(40.0f, 20.0f), 1e-6f);
        assertEquals(60.0f, ParryRules.stopped(500.0f, 100.0f), 1e-6f);
        for (float hp = 1f; hp < 400f; hp += 13f) {
            assertTrue(ParryRules.stopped(1.0e6f, hp) <= hp * 0.6f + 1e-3f, "never more than 60 percent at " + hp);
            assertTrue(ParryRules.passThrough(1.0e6f, hp) > 0f);
        }
    }

    @Test
    void noDamageAndNoHealthStopNothing() {
        assertEquals(0.0f, ParryRules.stopped(0.0f, 20.0f), 1e-6f);
        assertEquals(0.0f, ParryRules.stopped(-3.0f, 20.0f), 1e-6f);
        assertEquals(0.0f, ParryRules.stopped(5.0f, 0.0f), 1e-6f);
        assertEquals(5.0f, ParryRules.passThrough(5.0f, 0.0f), 1e-6f);
    }

    @Test
    void theAnsweringCutGrowsWithTheBlowBetweenFourAndTwelve() {
        assertEquals(4.0, ParryRules.counterRaw(0.0f), 1e-9);
        assertEquals(4.0, ParryRules.counterRaw(-5.0f), 1e-9);
        assertEquals(6.0, ParryRules.counterRaw(4.0f), 1e-9);
        assertEquals(10.0, ParryRules.counterRaw(12.0f), 1e-9);
        assertEquals(12.0, ParryRules.counterRaw(16.0f), 1e-9);
        assertEquals(12.0, ParryRules.counterRaw(400.0f), 1e-9, "never more than 12");
    }
}
