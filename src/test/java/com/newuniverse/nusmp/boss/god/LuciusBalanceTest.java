package com.newuniverse.nusmp.boss.god;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The 1.46.0 balance rules: the difficulty master, the cooldown floors, the pressure share, the hit grace, the stage grace. */
class LuciusBalanceTest {
    @Test
    void difficultyIsClampedAndGarbageBecomesTheDefault() {
        assertEquals(0.5, LuciusBalance.clampDifficulty(-4));
        assertEquals(1.5, LuciusBalance.clampDifficulty(99));
        assertEquals(1.0, LuciusBalance.clampDifficulty(1.0));
        assertEquals(LuciusBalance.DIFFICULTY_DEFAULT, LuciusBalance.clampDifficulty(Double.NaN));
        assertEquals(0.8, LuciusBalance.DIFFICULTY_DEFAULT);
    }

    @Test
    void damageFollowsTheDifficulty() {
        assertEquals(0.8, LuciusBalance.damageFactor(0.8), 1e-9);
        assertEquals(1.5, LuciusBalance.damageFactor(7.0), 1e-9);
    }

    @Test
    void aHarderLuciusActsMoreOftenButNeverUnderTheFloor() {
        // the Relic EMP: base 45 s in the chain table, floor 25 s
        assertEquals(900, LuciusBalance.cooldown("relic_emp", 900, 1.0));
        assertEquals(1125, LuciusBalance.cooldown("relic_emp", 900, 0.8), "default difficulty: slower");
        assertEquals(600, LuciusBalance.cooldown("relic_emp", 900, 1.5));
        assertEquals(500, LuciusBalance.cooldown("relic_emp", 500, 1.5), "floor 25 s");
        assertEquals(600, LuciusBalance.cooldown("severance_bind", 800, 1.5), "floor 30 s");
        assertEquals(600, LuciusBalance.cooldown("severance_bind", 600, 1.5));
        assertEquals(240, LuciusBalance.cooldown("starvation_pulse", 240, 1.5), "floor 12 s");
        assertEquals(300, LuciusBalance.cooldown("starvation_pulse", 240, 0.8));
    }

    @Test
    void theCountersCannotBeFasterThanTheirFloors() {
        for (String id : new String[]{"checkmate_counter", "ultimate_reversal", "spatial_swap"})
            for (double d : new double[]{0.5, 0.8, 1.0, 1.2, 1.5, 100.0})
                assertTrue(LuciusBalance.counterCooldown(id, d) >= LuciusBalance.floorOf(id), id + " at " + d);
        assertEquals(280, LuciusBalance.counterCooldown("checkmate_counter", 1.0), "14 s per target");
        assertEquals(400, LuciusBalance.counterCooldown("ultimate_reversal", 1.0), "20 s");
        assertEquals(400, LuciusBalance.counterCooldown("spatial_swap", 1.0), "20 s");
    }

    @Test
    void aBaseShorterThanTheFloorIsNotLengthened() {
        assertEquals(160, LuciusBalance.cooldown("checkmate_counter", 160, 1.0));
        assertEquals(160, LuciusBalance.cooldown("checkmate_counter", 160, 1.5), "a hard setting cannot go under the table's own number");
        assertEquals(0, LuciusBalance.cooldown("hit_counter", 0, 0.5));
        assertEquals(0, LuciusBalance.cooldown(null, -5, 1.0));
        assertEquals(300, LuciusBalance.cooldown("unknown_chain", 300, 1.0), "no floor, no change at 1.0");
    }

    @Test
    void thePressureShareIsEighteenInTheGodTierAndThirtyInTheClassicFight() {
        assertEquals(0.18, LuciusBalance.pressureShare(true));
        assertEquals(0.30, LuciusBalance.pressureShare(false));
    }

    @Test
    void theStageGraceCoversTheChangeAndTenSecondsAfter() {
        long until = LuciusBalance.stageGraceUntil(1000, 80);
        assertEquals(1000 + 80 + 200, until);
        assertTrue(LuciusBalance.inStageGrace(1000, until));
        assertTrue(LuciusBalance.inStageGrace(until - 1, until));
        assertFalse(LuciusBalance.inStageGrace(until, until));
        assertEquals(10, LuciusBalance.HIT_GRACE_TICKS, "half a second");
    }

    @Test
    void theHitGraceBlocksASecondTrueHitInsideHalfASecond() {
        CounterRules.Cooldowns<String> grace = new CounterRules.Cooldowns<>();
        assertTrue(grace.use("p", 100, LuciusBalance.HIT_GRACE_TICKS));
        assertFalse(grace.use("p", 105, LuciusBalance.HIT_GRACE_TICKS));
        assertFalse(grace.use("p", 109, LuciusBalance.HIT_GRACE_TICKS));
        assertTrue(grace.use("p", 110, LuciusBalance.HIT_GRACE_TICKS));
        assertTrue(grace.use("q", 105, LuciusBalance.HIT_GRACE_TICKS), "per player");
    }
}
