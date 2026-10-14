package com.newuniverse.nusmp.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The nullifications as cuts: bounded, never total, and the boss stays killable. */
class LuciusMitigationTest {
    @Test
    void damageTypesAreClassified() {
        assertEquals(LuciusMitigation.DamageClass.MAGIC, LuciusMitigation.classify("indirect_magic", true));
        assertEquals(LuciusMitigation.DamageClass.MAGIC, LuciusMitigation.classify("flame_elemental", true));
        assertEquals(LuciusMitigation.DamageClass.MAGIC, LuciusMitigation.classify("holy_damage", false));
        assertEquals(LuciusMitigation.DamageClass.SPATIAL, LuciusMitigation.classify("space_elemental", true), "space beats elemental");
        assertEquals(LuciusMitigation.DamageClass.SPATIAL, LuciusMitigation.classify("dimension_cutter", true));
        assertEquals(LuciusMitigation.DamageClass.PHYSICAL, LuciusMitigation.classify("player_attack", true));
        assertEquals(LuciusMitigation.DamageClass.PHYSICAL, LuciusMitigation.classify("arrow", true));
        assertEquals(LuciusMitigation.DamageClass.NONE, LuciusMitigation.classify("on_fire", false));
        assertEquals(LuciusMitigation.DamageClass.NONE, LuciusMitigation.classify("fall", false));
        assertEquals(LuciusMitigation.DamageClass.NONE, LuciusMitigation.classify(null, false));
    }

    @Test
    void theCutIsBoundedAtEveryStrength() {
        for (LuciusMitigation.DamageClass c : LuciusMitigation.DamageClass.values())
            for (double s : new double[]{-1, 0, 0.5, 1, 2, 3, 100, Double.NaN}) {
                double cut = LuciusMitigation.cut(c, s);
                assertTrue(cut >= 0 && cut <= LuciusNumbers.REDUCE_CEILING, c + " at " + s);
                for (float hit : new float[]{0.5f, 1f, 10f, 400f}) {
                    float after = LuciusMitigation.apply(c, hit, s);
                    assertTrue(after >= hit * 0.25f - 1e-4f, "never under a quarter of the hit: " + c + " " + hit + " -> " + after);
                    assertTrue(after <= hit + 1e-4f);
                }
            }
    }

    @Test
    void theDefaultCutsAreTheDocumentedOnes() {
        assertEquals(0.45, LuciusMitigation.cut(LuciusMitigation.DamageClass.MAGIC, 1.0), 1e-9);
        assertEquals(0.30, LuciusMitigation.cut(LuciusMitigation.DamageClass.PHYSICAL, 1.0), 1e-9);
        assertEquals(0.60, LuciusMitigation.cut(LuciusMitigation.DamageClass.SPATIAL, 1.0), 1e-9);
        assertEquals(0.0, LuciusMitigation.cut(LuciusMitigation.DamageClass.NONE, 1.0), 1e-9);
        assertEquals(100f, LuciusMitigation.apply(LuciusMitigation.DamageClass.NONE, 100f, 1.0), 1e-4f);
        assertEquals(55f, LuciusMitigation.apply(LuciusMitigation.DamageClass.MAGIC, 100f, 1.0), 1e-3f);
        assertEquals(0f, LuciusMitigation.apply(LuciusMitigation.DamageClass.MAGIC, 0f, 1.0));
        assertEquals(-3f, LuciusMitigation.apply(LuciusMitigation.DamageClass.MAGIC, -3f, 1.0), "non-positive hits pass through");
        assertEquals(100f, LuciusMitigation.apply(LuciusMitigation.DamageClass.MAGIC, 100f, 0.0), 1e-4f, "multiplier 0 = no cut");
    }

    @Test
    void theBossIsStillKillableInAFairNumberOfHits() {
        // 3600 health, hits of 60 (an ordinary strong hit): the worst cut class still needs well under a thousand hits
        float health = 3600, hit = 60;
        int blows = 0;
        while (health > 0 && blows < 10_000) {
            health -= LuciusMitigation.apply(LuciusMitigation.DamageClass.SPATIAL, hit, 1.0);
            blows++;
        }
        assertTrue(health <= 0 && blows <= 200, "killed in " + blows + " blows");
    }

    @Test
    void theDodgeCapKeepsTheFightFair() {
        assertEquals(LuciusNumbers.DODGE_MAX_BONUS, LuciusNumbers.dodgeBonus(0), 1e-9);
        assertEquals(LuciusNumbers.DODGE_MAX_BONUS, LuciusNumbers.dodgeBonus(6), 1e-9);
        assertEquals(4.0, LuciusNumbers.dodgeBonus(16), 1e-9);
        assertEquals(0.0, LuciusNumbers.dodgeBonus(20), 1e-9);
        assertEquals(0.0, LuciusNumbers.dodgeBonus(500), 1e-9, "a high configured base is not raised further");
        for (double base = -5; base < 100; base += 0.5)
            assertTrue(Math.max(0, base) + LuciusNumbers.dodgeBonus(base) <= Math.max(LuciusNumbers.DODGE_TOTAL_CAP, base) + 1e-9);
    }
}
