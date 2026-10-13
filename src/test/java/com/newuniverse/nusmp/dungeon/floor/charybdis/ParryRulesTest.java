package com.newuniverse.nusmp.dungeon.floor.charybdis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The deflect rule: which scale is turned, by what, and what it costs the boss. */
class ParryRulesTest {
    @Test
    void aRaisedShieldTurnsAScaleFromTheFrontOnly() {
        assertEquals(ParryRules.Deflect.SHIELD, ParryRules.onImpact(true, false, 1.0));
        assertEquals(ParryRules.Deflect.SHIELD, ParryRules.onImpact(true, false, 0.3));
        assertEquals(ParryRules.Deflect.NONE, ParryRules.onImpact(true, false, 0.0), "side-on is not covered by a shield");
        assertEquals(ParryRules.Deflect.NONE, ParryRules.onImpact(true, false, -0.8), "from behind it hits");
        assertEquals(ParryRules.Deflect.NONE, ParryRules.onImpact(false, false, 1.0), "no shield up, no deflect");
    }

    @Test
    void theParryWindowCoversMoreThanAShieldAndWinsWhenBothApply() {
        assertEquals(ParryRules.Deflect.PARRY, ParryRules.onImpact(false, true, 1.0));
        assertEquals(ParryRules.Deflect.PARRY, ParryRules.onImpact(false, true, -0.2), "the parry turns missiles beside you too");
        assertEquals(ParryRules.Deflect.NONE, ParryRules.onImpact(false, true, -0.6), "but not from straight behind");
        assertEquals(ParryRules.Deflect.PARRY, ParryRules.onImpact(true, true, 1.0), "both: the parry's turquoise wins");
        assertEquals(ParryRules.Deflect.PARRY, ParryRules.onImpact(true, true, -0.2), "the shield alone would not cover it, the parry does");
        assertTrue(ParryRules.PARRY_FRONT < ParryRules.SHIELD_FRONT);
    }

    @Test
    void aBlowAtTheScaleTurnsItOncePerPlayersHand() {
        assertTrue(ParryRules.strikeDeflects(true, false));
        assertFalse(ParryRules.strikeDeflects(false, false), "only a player's blow");
        assertFalse(ParryRules.strikeDeflects(true, true), "a scale that is already on its way back is not turned again");
    }

    @Test
    void aReturnedScaleIsHalfAgainAsFastWithinBounds() {
        assertEquals(0.75, ParryRules.returnSpeed(0.5), 1e-9);
        assertEquals(ParryRules.MIN_RETURN_SPEED, ParryRules.returnSpeed(0.1), 1e-9, "a crawling scale still comes back");
        assertEquals(ParryRules.MAX_RETURN_SPEED, ParryRules.returnSpeed(5.0), 1e-9, "and a fast one does not tunnel through a wall");
        assertEquals(1.5, ParryRules.SPEED_FACTOR, 1e-9);
        for (double v = 0; v < 3; v += 0.05) {
            double r = ParryRules.returnSpeed(v);
            assertTrue(r >= ParryRules.MIN_RETURN_SPEED && r <= ParryRules.MAX_RETURN_SPEED);
        }
    }

    @Test
    void aReturnedScaleWoundsAFortiethAndAVolleyAddsAtMostAEighth() {
        assertEquals(44.0, ParryRules.bonus(1100), 1e-9, "4% of 1100");
        assertEquals(0.0, ParryRules.bonus(-5), 1e-9);
        ParryRules.Pool pool = new ParryRules.Pool(1100);
        assertEquals(132.0, pool.cap(), 1e-9, "12% of 1100");
        double bonus = ParryRules.bonus(1100), total = 0;
        for (int i = 0; i < 9; i++) total += pool.take(bonus);
        assertEquals(132.0, total, 1e-9, "nine returned scales are worth three");
        assertTrue(pool.empty());
        assertEquals(0.0, pool.take(44), 1e-9);
        ParryRules.Pool half = new ParryRules.Pool(1100);
        assertEquals(44, half.take(44), 1e-9);
        assertEquals(88, half.take(100), 1e-9, "the last bit of the pool is handed out whole");
        assertEquals(132, half.paid(), 1e-9);
        assertEquals(0, new ParryRules.Pool(0).take(10), 1e-9);
    }

    @Test
    void theFacingIsADotProduct() {
        assertEquals(1.0, ParryRules.facing(0, 0, 1, 0, 0, 5), 1e-9);
        assertEquals(-1.0, ParryRules.facing(0, 0, 1, 0, 0, -3), 1e-9);
        assertEquals(0.0, ParryRules.facing(0, 0, 1, 4, 0, 0), 1e-9);
        assertEquals(0.0, ParryRules.facing(0, 0, 0, 1, 1, 1), 1e-9, "no look vector: no cover");
        assertEquals(Math.sqrt(0.5), ParryRules.facing(1, 0, 1, 0, 0, 2), 1e-9);
    }
}
