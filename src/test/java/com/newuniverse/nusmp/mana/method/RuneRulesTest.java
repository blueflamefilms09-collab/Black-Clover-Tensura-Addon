package com.newuniverse.nusmp.mana.method;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The numbers and the small maths of the Mana Method: Homing steers by a bounded angle, Growth is capped, True Magic cancels a bounded part of the resistance, the caps clamp. */
class RuneRulesTest {
    private static double[] unit(double x, double y, double z) {
        double l = Math.sqrt(x * x + y * y + z * z);
        return new double[]{x / l, y / l, z / l};
    }

    private static double angle(double[] a, double[] b) {
        return Math.acos(Math.max(-1, Math.min(1, a[0] * b[0] + a[1] * b[1] + a[2] * b[2])));
    }

    // ---------------------------------------------------------------- Homing
    @Test
    void steeringTurnsByAtMostTheLimitAndStaysAUnitVector() {
        Random r = new Random(11);
        double[] out = new double[3];
        for (int i = 0; i < 2000; i++) {
            double[] dir = unit(r.nextGaussian(), r.nextGaussian(), r.nextGaussian()), want = unit(r.nextGaussian(), r.nextGaussian(), r.nextGaussian());
            double before = angle(dir, want);
            double returned = RuneRules.steer(dir, want, RuneRules.HOMING_TURN, out);
            assertEquals(before, returned, 1e-9);
            assertEquals(1.0, Math.sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2]), 1e-9, "unit");
            double turned = angle(dir, out);
            assertTrue(turned <= RuneRules.HOMING_TURN + 1e-9, "turned " + turned);
            assertTrue(angle(out, want) <= before + 1e-9, "never turns away");
            if (before > RuneRules.HOMING_TURN) assertEquals(RuneRules.HOMING_TURN, turned, 1e-7, "a far target costs the whole turn");
            else assertEquals(0.0, angle(out, want), 1e-7, "a near target is met");
        }
    }

    @Test
    void steeringReachesAnyTargetInBoundedTicks() {
        double[] dir = {0, 0, 1}, want = {0, 0, -1}, out = new double[3];
        int ticks = 0;
        while (angle(dir, want) > 1e-6 && ticks < 100) {
            RuneRules.steer(dir, want, RuneRules.HOMING_TURN, out);
            assertFalse(Double.isNaN(out[0] + out[1] + out[2]), "no NaN when the target is straight behind");
            dir = out.clone();
            ticks++;
        }
        assertTrue(ticks <= Math.ceil(Math.PI / RuneRules.HOMING_TURN) + 1, "a 180 degree turn takes " + ticks + " ticks");
        assertTrue(ticks < RuneRules.HOMING_TICKS, "within the homing time");
    }

    @Test
    void steeringMayWriteIntoItsOwnInput() {
        double[] dir = unit(1, 0, 0), want = unit(0, 1, 0);
        RuneRules.steer(dir, want, 0.1, dir);
        assertEquals(1.0, Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]), 1e-9);
        assertEquals(0.1, Math.acos(dir[0]), 1e-9);
    }

    @Test
    void homingIsOnlyForTheSpellsThatAskAndOnlyForAWhile() {
        RuneSpec homing = new RuneSpec(RuneElement.WATER, RuneMods.HOMING, false), plain = new RuneSpec(RuneElement.WATER, RuneMods.NONE, false);
        assertTrue(RuneRules.homingActive(homing, 0));
        assertTrue(RuneRules.homingActive(homing, RuneRules.HOMING_TICKS - 1));
        assertFalse(RuneRules.homingActive(homing, RuneRules.HOMING_TICKS));
        assertFalse(RuneRules.homingActive(plain, 0));
        assertTrue(RuneRules.inHomingCone(1.0) && RuneRules.inHomingCone(RuneRules.HOMING_CONE_COS) && !RuneRules.inHomingCone(0.49) && !RuneRules.inHomingCone(-1.0));
        assertTrue(RuneRules.HOMING_TICKS * RuneRules.speed(homing) < RuneRules.MAX_RANGE * 2, "the homing time is of the order of the range");
    }

    // ---------------------------------------------------------------- Growth
    @Test
    void growthGrowsWithTheDistanceAndIsCapped() {
        RuneSpec grow = new RuneSpec(RuneElement.FIRE, RuneMods.GROWTH, false), plain = new RuneSpec(RuneElement.FIRE, RuneMods.NONE, false);
        double prevR = 0, prevD = 0;
        for (double d = 0; d <= 100; d += 0.5) {
            double r = RuneRules.hitRadius(grow, d), f = RuneRules.damageFactor(grow, d);
            assertTrue(r >= prevR && f >= prevD, "monotone at " + d);
            assertTrue(r <= RuneRules.BASE_HIT_RADIUS * RuneRules.GROWTH_RADIUS_MAX + 1e-12, "radius capped");
            assertTrue(f <= RuneRules.GROWTH_DAMAGE_MAX + 1e-12, "damage capped");
            assertEquals(RuneRules.BASE_HIT_RADIUS, RuneRules.hitRadius(plain, d), 1e-12, "no Growth, no change");
            assertEquals(1.0, RuneRules.damageFactor(plain, d), 1e-12);
            assertTrue(RuneRules.splashRadius(grow, d) <= RuneRules.SPLASH_RADIUS_MAX + 1e-12);
            prevR = r;
            prevD = f;
        }
        assertEquals(RuneRules.BASE_HIT_RADIUS, RuneRules.hitRadius(grow, 0), 1e-12);
        assertEquals(1.0, RuneRules.damageFactor(grow, 0), 1e-12);
        assertTrue(RuneRules.hitRadius(grow, 20) > 1.5 * RuneRules.BASE_HIT_RADIUS, "visibly bigger after 20 blocks");
        assertEquals(RuneRules.BASE_HIT_RADIUS * RuneRules.GROWTH_RADIUS_MAX, RuneRules.hitRadius(grow, 1e9), 1e-9);
        assertEquals(RuneRules.GROWTH_DAMAGE_MAX, RuneRules.damageFactor(grow, 1e9), 1e-9);
        assertEquals(1.0, RuneRules.growthScale(Double.NaN), 0.0);
        assertEquals(1.0, RuneRules.growthScale(-5), 0.0);
        assertTrue(RuneRules.GROWTH_DAMAGE_MAX * RuneRules.RAW_DAMAGE_TRUE < 60, "the grown True Magic hit stays inside a sane raw number");
    }

    @Test
    void bothRunesShareThePowerAndChargeLonger() {
        for (boolean t : new boolean[]{false, true}) {
            RuneSpec one = new RuneSpec(RuneElement.EARTH, RuneMods.GROWTH, t), both = new RuneSpec(RuneElement.EARTH, RuneMods.ALL, t);
            assertEquals(RuneRules.rawDamage(one) * RuneRules.BOTH_MODS_MULT, RuneRules.rawDamage(both), 1e-9);
            assertEquals(RuneRules.chargeTicks(one) + RuneRules.CHARGE_TICKS_BOTH, RuneRules.chargeTicks(both));
        }
        assertTrue(RuneRules.rawDamage(new RuneSpec(RuneElement.FIRE, 1, true)) > RuneRules.rawDamage(new RuneSpec(RuneElement.FIRE, 1, false)), "True Magic hits harder");
        assertTrue(RuneRules.chargeTicks(new RuneSpec(RuneElement.FIRE, 1, true)) > RuneRules.chargeTicks(new RuneSpec(RuneElement.FIRE, 1, false)), "and charges longer");
        assertTrue(RuneRules.chargeTicks(new RuneSpec(RuneElement.FIRE, RuneMods.ALL, true)) <= 60, "the packet carries the charge in 1..60");
    }

    // ---------------------------------------------------------------- True Magic
    @Test
    void trueMagicCancelsAtMostABoundedPartOfTheResistance() {
        assertEquals(1.0, RuneRules.resistanceBypass(0.0), 1e-12);
        double prev = 1.0;
        for (double r = 0; r <= 1.5; r += 0.05) {
            double f = RuneRules.resistanceBypass(r);
            assertTrue(f >= prev - 1e-12, "monotone");
            assertTrue(f <= RuneRules.BYPASS_MAX + 1e-12, "capped");
            prev = f;
        }
        assertEquals(2.0, RuneRules.resistanceBypass(0.5), 1e-9, "a 50% cut is undone");
        assertEquals(RuneRules.BYPASS_MAX, RuneRules.resistanceBypass(0.99), 1e-9, "a near-immune target is capped at the bypass limit");
        assertEquals(RuneRules.resistanceBypass(RuneRules.RESISTANCE_CAP), RuneRules.resistanceBypass(0.99), 1e-9, "the resistance itself is read only up to its cap");
        assertEquals(1.0, RuneRules.resistanceBypass(Double.NaN), 0.0);
        assertEquals(1.0, RuneRules.resistanceBypass(-3.0), 0.0);
        assertTrue(RuneRules.bypassFor(0.9, true) <= RuneRules.BOSS_BYPASS_MAX + 1e-12, "a boss loses less of its resistance");
        assertEquals(RuneRules.resistanceBypass(0.2), RuneRules.bypassFor(0.2, true), 1e-12, "a small resistance is cancelled in full even for a boss");
        assertEquals(RuneRules.resistanceBypass(0.9), RuneRules.bypassFor(0.9, false), 1e-12);
    }

    @Test
    void theAttributeIsReadAsAFractionOrAPercent() {
        assertEquals(0.3, RuneRules.resistanceOf(0.3), 1e-12);
        assertEquals(0.3, RuneRules.resistanceOf(30.0), 1e-12);
        assertEquals(1.0, RuneRules.resistanceOf(1.0), 1e-12);
        assertEquals(1.0, RuneRules.resistanceOf(500.0), 1e-12);
        assertEquals(0.0, RuneRules.resistanceOf(-0.4), 1e-12);
        assertEquals(0.0, RuneRules.resistanceOf(Double.NaN), 1e-12);
        assertEquals(0.0, RuneRules.resistanceOf(Double.POSITIVE_INFINITY), 1e-12);
    }

    // ---------------------------------------------------------------- the spec and the caps
    @Test
    void theSpecSurvivesPackingForEveryCombination() {
        for (RuneElement e : RuneElement.values())
            for (int mods = 0; mods <= RuneMods.ALL; mods++)
                for (boolean t : new boolean[]{false, true}) {
                    RuneSpec s = new RuneSpec(e, mods, t);
                    assertEquals(s, RuneSpec.unpack(s.pack()));
                    assertTrue(s.pack() >= 0 && s.pack() < 64, "fits the six bits of the packet");
                }
        assertEquals(new RuneSpec(RuneElement.FIRE, 0, false), RuneSpec.unpack(0));
        assertEquals(RuneElement.FIRE, RuneSpec.unpack(-1).element(), "a garbage int still unpacks to a real element (ordinal 7 is out of range: fire)");
        assertEquals(RuneMods.NONE, RuneMods.sanitize(8), "unknown bits are dropped");
        assertEquals(RuneElement.FIRE, RuneElement.byOrdinal(99), "a bad ordinal is fire, never an exception");
        assertEquals(RuneElement.FIRE, new RuneSpec(null, 0, false).element());
        assertEquals(2, RuneMods.count(RuneMods.ALL));
        assertEquals("pursue + grow", RuneMods.describe(RuneMods.ALL));
    }

    @Test
    void theElementsFollowTheNaturalAttributes() {
        assertEquals(RuneElement.FIRE, RuneElement.ofMagic("flame"));
        assertEquals(RuneElement.WATER, RuneElement.ofMagic("WATER"));
        assertEquals(RuneElement.WIND, RuneElement.ofMagic("Wind"));
        assertEquals(RuneElement.EARTH, RuneElement.ofMagic("EARTH"));
        assertEquals(RuneElement.LIGHTNING, RuneElement.ofMagic("LIGHTNING"));
        assertEquals(RuneElement.PLANT, RuneElement.ofMagic("PLANT"));
        assertNull(RuneElement.ofMagic("TIME"));
        assertNull(RuneElement.ofMagic(null));
        assertEquals(4, RuneElement.BASIC.length);
        assertEquals(RuneMods.HOMING, RuneElement.WATER.defaultMods());
        assertEquals(RuneMods.HOMING, RuneElement.WIND.defaultMods());
        assertEquals(RuneMods.GROWTH, RuneElement.FIRE.defaultMods());
        assertEquals(RuneMods.GROWTH, RuneElement.EARTH.defaultMods());
    }

    @Test
    void theCapsAreClampedWhateverTheConfigSays() {
        assertEquals(1, RuneRules.clampPerCaster(-5));
        assertEquals(1, RuneRules.clampPerCaster(0));
        assertEquals(RuneRules.MAX_PER_CASTER, RuneRules.clampPerCaster(500));
        assertEquals(RuneRules.MIN_GLOBAL, RuneRules.clampGlobal(0));
        assertEquals(RuneRules.MAX_GLOBAL, RuneRules.clampGlobal(100_000));
        assertEquals(RuneRules.DEFAULT_GLOBAL, RuneRules.clampGlobal(RuneRules.DEFAULT_GLOBAL));
        assertTrue(RuneRules.DEFAULT_PER_CASTER <= RuneRules.MAX_PER_CASTER && RuneRules.DEFAULT_GLOBAL <= RuneRules.MAX_GLOBAL);
    }

    @Test
    void theWorstCaseCostsAreBounded() {
        // the whole server: MAX_GLOBAL bolts, each one move, one ray, and an entity query only inside the shared per-tick budget
        assertTrue(RuneRules.MAX_GLOBAL <= 64);
        assertTrue(RuneRules.QUERIES_PER_TICK <= 16);
        assertTrue(RuneRules.MAX_FLIGHT_TICKS <= 40 && RuneRules.MAX_RANGE <= 36.0);
        int flightTrailPackets = RuneRules.MAX_FLIGHT_TICKS / RuneRules.TRAIL_EVERY;
        assertTrue(flightTrailPackets <= 14, "trail packets per cast " + flightTrailPackets);
        assertTrue(RuneRules.MAX_TARGETS <= 6);
        // a page costs real mana and has a real cooldown, True Magic more than an element rune
        assertTrue(RuneRules.COST_PERCENT_TRUE > RuneRules.COST_PERCENT && RuneRules.COOLDOWN_TRUE > RuneRules.COOLDOWN);
        assertTrue(RuneRules.COOLDOWN >= 160, "the mid-tier cooldown floor of the pacing table");
    }
}
