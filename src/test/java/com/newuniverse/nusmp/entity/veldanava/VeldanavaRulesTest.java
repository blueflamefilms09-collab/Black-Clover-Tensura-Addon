package com.newuniverse.nusmp.entity.veldanava;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Veldanava's pure rules: the three bodies, the move table, the choice of a move, the damage, the beam, the pull, the Genesis falloff, the loot and every cap of rule 11. */
class VeldanavaRulesTest {
    @Test
    void theThreeBodiesHaveTheirOwnModelAndSize() {
        assertEquals(3, VeldanavaRules.FORMS);
        assertEquals("true_dragon", VeldanavaRules.geo(VeldanavaRules.DRAGON));
        assertEquals("humanoid", VeldanavaRules.geo(VeldanavaRules.HUMANOID));
        assertEquals("dark_avatar", VeldanavaRules.geo(VeldanavaRules.AVATAR));
        assertTrue(VeldanavaRules.width(VeldanavaRules.DRAGON) > VeldanavaRules.width(VeldanavaRules.HUMANOID) * 5, "the dragon is the big body");
        assertTrue(VeldanavaRules.height(VeldanavaRules.DRAGON) >= 9f);
        assertEquals(1.6f, VeldanavaRules.modelScale(VeldanavaRules.DRAGON), 1e-6, "the dragon is drawn bigger than its model file (owner: he should be bigger)");
        assertEquals(1f, VeldanavaRules.modelScale(VeldanavaRules.HUMANOID), 1e-6);
        assertEquals(VeldanavaRules.DRAGON, VeldanavaRules.clampForm(-4));
        assertEquals(VeldanavaRules.AVATAR, VeldanavaRules.clampForm(99));
        Set<String> names = new HashSet<>();
        for (int f = 0; f < VeldanavaRules.FORMS; f++) names.add(VeldanavaRules.formName(f));
        assertEquals(3, names.size());
    }

    @Test
    void theLimiterIsReleasedOnlyInTheCreatorGodAtTheLastTenPercent() {
        assertTrue(VeldanavaRules.limiterDue(VeldanavaRules.HUMANOID, 400f, 4000f));
        assertTrue(VeldanavaRules.limiterDue(VeldanavaRules.HUMANOID, 1f, 4000f));
        assertFalse(VeldanavaRules.limiterDue(VeldanavaRules.HUMANOID, 401f, 4000f));
        assertFalse(VeldanavaRules.limiterDue(VeldanavaRules.HUMANOID, 0f, 4000f), "a dead body does not transform");
        assertFalse(VeldanavaRules.limiterDue(VeldanavaRules.DRAGON, 100f, 4000f));
        assertFalse(VeldanavaRules.limiterDue(VeldanavaRules.AVATAR, 100f, 4000f));
        assertFalse(VeldanavaRules.limiterDue(VeldanavaRules.HUMANOID, 100f, 0f));
    }

    @Test
    void theDragonNeverDiesItChanges() {
        assertTrue(VeldanavaRules.dragonLethal(VeldanavaRules.DRAGON, 10f, 10f));
        assertTrue(VeldanavaRules.dragonLethal(VeldanavaRules.DRAGON, 10f, 99f));
        assertFalse(VeldanavaRules.dragonLethal(VeldanavaRules.DRAGON, 10f, 9.9f));
        assertFalse(VeldanavaRules.dragonLethal(VeldanavaRules.HUMANOID, 10f, 99f));
    }

    @Test
    void theMoveTableIsConsistent() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < VeldanavaRules.MOVES; i++) {
            VeldanavaRules.Spec s = VeldanavaRules.spec(i);
            assertEquals(i, s.id(), "the table is indexed by the move id");
            assertTrue(ids.add(s.name()), "a move name is used twice: " + s.name());
            assertTrue(s.windup() > 0, s.name() + " has no telegraph");
            assertTrue(s.active() > 0 && s.recovery() > 0);
            assertEquals(s.windup() + s.active() + s.recovery(), s.length());
            assertTrue(s.minRange() <= s.maxRange());
            assertTrue(s.weight() > 0);
            assertTrue(s.share() >= 0f && s.share() <= 0.7f, s.name() + " takes too much of a player's health per hit");
            assertTrue(s.forms() != 0, s.name() + " belongs to no body");
            assertTrue(s.length() < 400, s.name() + " is too long a move");
        }
        assertEquals(5, VeldanavaRules.movesOf(VeldanavaRules.DRAGON).size());
        assertEquals(5, VeldanavaRules.movesOf(VeldanavaRules.HUMANOID).size());
        assertEquals(6, VeldanavaRules.movesOf(VeldanavaRules.AVATAR).size());
        List<VeldanavaRules.Spec> ultimates = VeldanavaRules.movesOf(VeldanavaRules.AVATAR).stream().filter(VeldanavaRules.Spec::ultimate).toList();
        assertEquals(1, ultimates.size(), "one ultimate");
        assertEquals(VeldanavaRules.GENESIS, ultimates.get(0).id());
        assertTrue(VeldanavaRules.spec(VeldanavaRules.GENESIS).has(VeldanavaRules.AVATAR) && !VeldanavaRules.spec(VeldanavaRules.GENESIS).has(VeldanavaRules.HUMANOID));
        assertEquals(VeldanavaRules.SWEEP, VeldanavaRules.spec(-5).id());
        assertEquals(VeldanavaRules.GENESIS, VeldanavaRules.spec(99).id());
    }

    @Test
    void aMoveIsChosenByWeightRangeAndCooldown() {
        long[] ready = new long[VeldanavaRules.MOVES];
        assertEquals(VeldanavaRules.SWEEP, VeldanavaRules.choose(VeldanavaRules.DRAGON, 5, ready, 0, t -> 0));
        assertEquals(VeldanavaRules.STARFALL, VeldanavaRules.choose(VeldanavaRules.DRAGON, 5, ready, 0, t -> 5), "Gravity Pull needs 6+ blocks");
        assertEquals(VeldanavaRules.ROAR, VeldanavaRules.choose(VeldanavaRules.DRAGON, 5, ready, 0, t -> 10));
        assertEquals(VeldanavaRules.BITE, VeldanavaRules.choose(VeldanavaRules.DRAGON, 5, ready, 0, t -> 12));
        assertEquals(VeldanavaRules.BITE, VeldanavaRules.choose(VeldanavaRules.DRAGON, 5, ready, 0, t -> t - 1), "the last point of the roll is still a move");
        assertEquals(-1, VeldanavaRules.choose(VeldanavaRules.DRAGON, 500, ready, 0, t -> 0), "nothing reaches that far");
        ready[VeldanavaRules.SWEEP] = 100;
        assertNotEquals(VeldanavaRules.SWEEP, VeldanavaRules.choose(VeldanavaRules.DRAGON, 5, ready, 99, t -> 0), "a move on cooldown is never chosen");
        assertEquals(VeldanavaRules.SWEEP, VeldanavaRules.choose(VeldanavaRules.DRAGON, 5, ready, 100, t -> 0));
        long[] all = new long[VeldanavaRules.MOVES];
        java.util.Arrays.fill(all, 1000);
        assertEquals(-1, VeldanavaRules.choose(VeldanavaRules.DRAGON, 5, all, 0, t -> 0));
        for (int form = 0; form < VeldanavaRules.FORMS; form++)
            for (int roll = 0; roll < 40; roll++) {
                final int r = roll;
                int m = VeldanavaRules.choose(form, 8, new long[VeldanavaRules.MOVES], 0, t -> Math.min(r, t - 1));
                assertTrue(m < 0 || VeldanavaRules.spec(m).has(form), "a body only uses its own moves");
            }
    }

    @Test
    void cooldownsAreNeverUnderASecondAndTheLaterBodiesAreFaster() {
        for (int i = 0; i < VeldanavaRules.MOVES; i++) {
            VeldanavaRules.Spec s = VeldanavaRules.spec(i);
            for (int f = 0; f < VeldanavaRules.FORMS; f++) assertTrue(VeldanavaRules.cooldown(s, f) >= 20);
        }
        VeldanavaRules.Spec s = VeldanavaRules.spec(VeldanavaRules.GOD_SPEED);
        assertTrue(VeldanavaRules.cooldown(s, VeldanavaRules.AVATAR) < VeldanavaRules.cooldown(s, VeldanavaRules.HUMANOID));
        assertTrue(VeldanavaRules.gap(VeldanavaRules.AVATAR) < VeldanavaRules.gap(VeldanavaRules.HUMANOID));
        assertTrue(VeldanavaRules.gap(VeldanavaRules.HUMANOID) < VeldanavaRules.gap(VeldanavaRules.DRAGON));
        assertTrue(VeldanavaRules.damageScale(VeldanavaRules.AVATAR) > VeldanavaRules.damageScale(VeldanavaRules.HUMANOID));
    }

    @Test
    void damageIsAShareOfTheTargetsHealthWithAFloorAndACap() {
        assertEquals(6f, VeldanavaRules.damage(20, 0.3f, 4f, 30f, 1f), 1e-4);
        assertEquals(4f, VeldanavaRules.damage(2, 0.3f, 4f, 30f, 1f), 1e-4, "the floor keeps a small target in danger");
        assertEquals(30f, VeldanavaRules.damage(100000, 0.3f, 4f, 30f, 1f), 1e-4, "the cap keeps a huge health bar from being one-shot");
        assertEquals(60f, VeldanavaRules.damage(100000, 0.3f, 4f, 30f, 2f), 1e-4);
        assertEquals(0f, VeldanavaRules.damage(20, 0.3f, 4f, 30f, 0f), 1e-4);
        assertEquals(0f, VeldanavaRules.damage(20, 0.3f, 4f, 30f, -3f), 1e-4, "a negative scale never heals");
    }

    @Test
    void theSoulSeizingTestIsAFatalBlow() {
        assertTrue(VeldanavaRules.isFatal(10, 0, 10));
        assertTrue(VeldanavaRules.isFatal(10, 0, 25));
        assertFalse(VeldanavaRules.isFatal(10, 0, 9.5));
        assertFalse(VeldanavaRules.isFatal(10, 4, 13), "absorption counts");
        assertTrue(VeldanavaRules.isFatal(10, 4, 14));
        assertFalse(VeldanavaRules.isFatal(10, 0, 0), "no blow, no death");
        assertFalse(VeldanavaRules.isFatal(10, -5, 9), "negative absorption is ignored");
    }

    @Test
    void theGenesisWaveFallsOffWithDistance() {
        assertEquals(1f, VeldanavaRules.falloff(0, 12, 56, 0.35f), 1e-6);
        assertEquals(1f, VeldanavaRules.falloff(12, 12, 56, 0.35f), 1e-6);
        assertEquals(0.35f, VeldanavaRules.falloff(56, 12, 56, 0.35f), 1e-6);
        assertEquals(0.35f, VeldanavaRules.falloff(500, 12, 56, 0.35f), 1e-6);
        assertEquals(0.675f, VeldanavaRules.falloff(34, 12, 56, 0.35f), 1e-4);
        float last = 1f;
        for (double d = 0; d <= 70; d += 0.5) {
            float v = VeldanavaRules.falloff(d, 12, 56, 0.35f);
            assertTrue(v <= last + 1e-6f && v >= 0.35f - 1e-6f, "the falloff only goes down, never under the minimum");
            last = v;
        }
    }

    @Test
    void theSweepingBeamTurnsOneCircleAndHitsOnlyWhatIsInIt() {
        assertEquals(0.0, VeldanavaRules.sweepYaw(0, 0, 50), 1e-9);
        assertEquals(180.0, VeldanavaRules.sweepYaw(0, 25, 50), 1e-9);
        assertEquals(360.0, VeldanavaRules.sweepYaw(0, 50, 50), 1e-9);
        assertEquals(90.0, VeldanavaRules.sweepYaw(90, 0, 0), 1e-9, "a zero length move does not divide by zero");
        assertTrue(VeldanavaRules.inBeam(0, 10, 0, 56, 3.5), "yaw 0 looks along +z");
        assertFalse(VeldanavaRules.inBeam(5, 10, 0, 56, 3.5), "too far to the side");
        assertFalse(VeldanavaRules.inBeam(0, -10, 0, 56, 3.5), "behind the boss");
        assertFalse(VeldanavaRules.inBeam(0, 60, 0, 56, 3.5), "past the end of the beam");
        assertTrue(VeldanavaRules.inBeam(-10, 0, 90, 56, 3.5), "yaw 90 looks along -x");
        assertTrue(VeldanavaRules.inBeam(0, -10, 180, 56, 3.5));
        // over one full circle every point of the ring is hit at least once
        for (int deg = 0; deg < 360; deg += 15) {
            double r = Math.toRadians(deg);
            double dx = -Math.sin(r) * 30, dz = Math.cos(r) * 30;
            boolean hit = false;
            for (int t = 0; t <= 50 && !hit; t++) hit = VeldanavaRules.inBeam(dx, dz, VeldanavaRules.sweepYaw(0, t, 50), 56, 3.5);
            assertTrue(hit, "a player standing at " + deg + " degrees is swept");
        }
    }

    @Test
    void theGravityPullIsBoundedAndStrongerNearby() {
        assertEquals(0.0, VeldanavaRules.pullStep(40, 38), 1e-9);
        assertEquals(0.0, VeldanavaRules.pullStep(2, 38), 1e-9, "inside the crush radius nobody is dragged further");
        double last = Double.MAX_VALUE;
        for (double d = 3; d <= 38; d += 1) {
            double v = VeldanavaRules.pullStep(d, 38);
            assertTrue(v > 0 && v <= 0.42, "a pull is a push of at most 0.42 blocks per tick");
            assertTrue(v <= last + 1e-9, "the closer the stronger");
            last = v;
        }
    }

    @Test
    void theStarsAreCappedAndScaleWithThePlayers() {
        assertEquals(0, VeldanavaRules.marks(0));
        assertEquals(0, VeldanavaRules.marks(-3));
        assertEquals(5, VeldanavaRules.marks(1));
        assertEquals(11, VeldanavaRules.marks(4));
        assertEquals(VeldanavaRules.MAX_MARKS, VeldanavaRules.marks(5));
        assertEquals(VeldanavaRules.MAX_MARKS, VeldanavaRules.marks(10000));
    }

    @Test
    void theOrbWaitsWithinTheServersBounds() {
        assertEquals(30 * 20, VeldanavaRules.orbTicks(0));
        assertEquals(300 * 20, VeldanavaRules.orbTicks(300));
        assertEquals(1800 * 20, VeldanavaRules.orbTicks(999999));
        assertEquals(30 * 20, VeldanavaRules.orbTicks(-5));
    }

    @Test
    void theLootIsBoundedPerFighterAndForAllOfThem() {
        int[] g = VeldanavaRules.guaranteed();
        assertArrayEquals(new int[]{1, 3, 1}, g, "Star King Dragon Core x1, Genesis Fragment x3, Creator's Essence x1");
        assertTrue(VeldanavaRules.hit(0, VeldanavaRules.CHANCE_FRAGMENT));
        assertTrue(VeldanavaRules.hit(149, VeldanavaRules.CHANCE_FRAGMENT));
        assertFalse(VeldanavaRules.hit(150, VeldanavaRules.CHANCE_FRAGMENT));
        assertFalse(VeldanavaRules.hit(-1, 1000));
        assertTrue(VeldanavaRules.CHANCE_DIVINE_CORE < VeldanavaRules.CHANCE_FRAGMENT, "the Divine Core is the rarest drop");
        assertEquals(0, VeldanavaRules.fighters(-4));
        assertEquals(VeldanavaRules.MAX_FIGHTERS, VeldanavaRules.fighters(1000));
        assertEquals(3, VeldanavaRules.fighters(3));
    }

    @Test
    void theWorstCaseCapsStaySmall() {
        assertTrue(VeldanavaRules.MAX_TARGETS <= 24, "targets hurt per tick");
        assertTrue(VeldanavaRules.MAX_MARKS <= 12, "stars per Starfall");
        assertTrue(VeldanavaRules.BLOCKS_PER_TICK <= 24, "blocks changed per tick");
        assertTrue(VeldanavaRules.FX_PER_TICK <= 12, "effect packets per tick");
        assertTrue(VeldanavaRules.MAX_FIGHTERS <= 8);
        assertTrue(VeldanavaRules.MAX_DESTROY_SPEED < 50f, "obsidian and harder stay");
        assertEquals(80, VeldanavaRules.TRANSFORM_TICKS);
    }
}
