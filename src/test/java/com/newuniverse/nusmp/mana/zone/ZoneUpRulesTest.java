package com.newuniverse.nusmp.mana.zone;

import com.newuniverse.nusmp.mana.zone.ZoneUpRules.OriginMode;
import com.newuniverse.nusmp.mana.zone.ZoneUpRules.Rules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The numbers of the Mana Zone upgrade: the shipped defaults are sane and unchanged by the sanitiser, the OFF set is the 1.99.0 zone, and no config can leave the safe ranges. */
class ZoneUpRulesTest {
    @Test
    void theShippedDefaultsAreInsideTheirRangesAndOn() {
        Rules d = ZoneUpRules.DEFAULT;
        assertEquals(d, ZoneUpRules.sanitize(d), "the sanitiser leaves the defaults alone");
        assertTrue(d.evasion() && d.buffs() && d.cast() && d.castRelocate(), "default on for new");
        assertEquals(OriginMode.BEHIND, d.castMode());
        assertTrue(d.aimGap() >= 1 && d.aimGap() <= 20, "the server's packet cap is at most 20 per second");
        assertTrue(d.aimTtl() >= 5, "the aim lives at least through a cast wind-up");
        assertTrue(d.evasionLooks() <= 8, "bounded projectiles examined per scan");
        assertTrue(d.evasionCooldown() >= 10, "a dodge is never a stream");
    }

    @Test
    void theOffSetIsTheOldZone() {
        Rules off = ZoneUpRules.sanitize(ZoneUpRules.OFF);
        assertFalse(off.evasion());
        assertFalse(off.buffs());
        assertFalse(off.cast());
        assertFalse(off.castRelocate());
        assertEquals(0.0, off.attackBonus(), 0.0);
        assertEquals(0.0, off.knockbackResist(), 0.0);
    }

    @Test
    void noConfigValueEscapesItsRange() {
        Rules wild = ZoneUpRules.sanitize(new Rules(true, 9, -4, 99, 500, 500, 99, 99, true, 9, 9, true, null, true, -3, -3, -3, 99, 99, 99));
        assertTrue(wild.evasionChance() <= 0.95 && wild.evasionCooldown() >= 10 && wild.evasionCostPct() <= 5 && wild.evasionHorizon() <= 12 && wild.evasionLooks() <= 8);
        assertTrue(wild.evasionHitRadius() <= 2.0 && wild.evasionImpulse() <= 1.0 && wild.attackBonus() <= 0.5 && wild.knockbackResist() <= 1.0);
        assertTrue(wild.castGap() >= 2 && wild.aimGap() >= 1 && wild.aimTtl() >= 5 && wild.castReach() <= 8 && wild.castMinRun() <= 6 && wild.tolerance() <= 4);
        assertEquals(OriginMode.BEHIND, wild.castMode(), "a missing mode is BEHIND");
        Rules nan = ZoneUpRules.sanitize(new Rules(true, Double.NaN, 30, Double.NaN, 6, 4, Double.NaN, Double.NaN, true, Double.NaN, Double.NaN, true, OriginMode.EDGE, true, 6, 2, 30, Double.NaN, Double.NaN, Double.NaN));
        assertTrue(nan.evasionChance() == 0.0 && nan.attackBonus() == 0.0 && nan.knockbackResist() == 0.0, "NaN becomes the smallest value, never an unbounded one");
        assertTrue(nan.evasionImpulse() >= 0.2 && nan.castReach() >= 1 && nan.castMinRun() >= 1);
        assertEquals(OriginMode.EDGE, nan.castMode());
    }
}
