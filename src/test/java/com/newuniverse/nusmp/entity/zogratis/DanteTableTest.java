package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Dante's move table, and a played-out fight through the scheduler: what he does at each power and distance. */
class DanteTableTest {
    static final List<DanteTable.Spec> SPECS = DanteTable.specs();

    @Test
    void theTableIsWellFormed() {
        assertTrue(SPECS.size() >= 10, "at least ten moves");
        Set<String> ids = new HashSet<>();
        for (DanteTable.Spec s : SPECS) {
            assertTrue(ids.add(s.id()), "unique id " + s.id());
            assertFalse(s.label().isBlank());
            assertTrue(s.minPower() >= 20 && s.minPower() <= 80, s.id() + " is available before the Gates");
            assertTrue(s.windUpTicks() > 0, s.id() + " has a telegraph");
            assertTrue(Math.round(s.windUpTicks() * 0.75f) >= 8, s.id() + " keeps 8 ticks of tell even at the Gates");
            assertTrue(s.cooldownTicks() >= 60 && s.weight() >= 1);
            assertTrue(s.minRange() >= 0 && s.maxRange() > s.minRange());
            assertTrue(s.recovery() >= 8, "a beat to breathe after every move");
            assertTrue(s.manaCost() > 0);
        }
        assertSame(DanteTable.byId(DanteTable.SINGULARITY), SPECS.get(11), "the first twelve moves keep their places (the client reads names by index)");
        assertEquals(DanteTable.TWIN_WELLS, SPECS.get(12).id(), "1.35.0 moves are appended");
        assertEquals(DanteTable.TITAN_QUAKE, SPECS.get(13).id());
        assertTrue(DanteTable.byId(DanteTable.TWIN_WELLS).minPower() >= 60 && DanteTable.byId(DanteTable.TITAN_QUAKE).minPower() >= 80, "late-phase moves");
        assertNull(DanteTable.byId("nope"));
    }

    @Test
    void theMixIsMeleeGravityProjectileSummonUtilityAndOneUltimate() {
        long ultimates = SPECS.stream().filter(DanteTable.Spec::ultimate).count();
        assertEquals(1, ultimates);
        DanteTable.Spec ult = DanteTable.byId(DanteTable.SINGULARITY);
        assertTrue(ult.ultimate() && ult.minPower() >= 60, "only usable at 60% and above");
        assertTrue(ult.windUpTicks() >= 40, "a ritual, not a flick");
        assertTrue(ult.recovery() >= 40, "he is exposed afterwards");
        assertTrue(ult.cooldownTicks() >= PowerCurve.tuning(100).ultimateGapTicks());
        assertTrue(SPECS.stream().anyMatch(s -> s.maxRange() <= 10), "melee");
        assertTrue(SPECS.stream().anyMatch(s -> s.minRange() >= 5 && s.maxRange() >= 26), "ranged");
        assertTrue(SPECS.stream().anyMatch(DanteTable.Spec::crowd), "crowd moves");
        assertNotNull(DanteTable.byId(DanteTable.CALL_DISCIPLES), "summon");
        assertNotNull(DanteTable.byId(DanteTable.BODY_SURGE), "utility");
        assertNotNull(DanteTable.byId(DanteTable.GRAVITY_DOMAIN), "gravity AoE");
        assertNotNull(DanteTable.byId(DanteTable.ROCK_SWORDS), "rock swords");
    }

    @Test
    void thereIsNoDeadRangeAtTheOpening() {
        // an attack, not a utility move (the Disciples and the surge work at any distance and would hide a gap)
        for (double d = 0; d <= 34; d += 0.5) {
            double dist = d;
            assertTrue(SPECS.stream().anyMatch(s -> !s.ultimate() && s.minPower() <= 20 && dist >= s.minRange() && dist <= s.maxRange()
                            && !s.id().equals(DanteTable.CALL_DISCIPLES) && !s.id().equals(DanteTable.BODY_SURGE)),
                    "an attack to throw at " + d + " blocks at 20%");
        }
    }

    @Test
    void manaMatchesTheFramework() {
        for (DanteTable.Spec s : SPECS) {
            if (s.ultimate()) assertEquals(60, s.manaCost());
            else assertEquals(Math.min(32, 6 + s.cooldownTicks() / 16.0), s.manaCost(), 1e-9);
        }
        // the opening pool (100) pays for a few moves, not for everything at once
        double all = SPECS.stream().filter(s -> !s.ultimate() && s.minPower() <= 20).mapToDouble(DanteTable.Spec::manaCost).sum();
        assertTrue(all > PowerCurve.tuning(20).manaCap() * 0.5 && all < PowerCurve.tuning(20).manaCap() * 3);
    }

    @Test
    void damageNumbersAreSane() {
        assertTrue(DanteTable.FIST > DanteTable.STOMP && DanteTable.FIST > DanteTable.SWORD * 2, "the fist is the heaviest ordinary blow");
        assertTrue(DanteTable.SWORD * GravityMath.swordCount(4) < GravityMath.collapseDamage(1.0) * 2, "a full volley is no singularity");
        assertTrue(GravityMath.collapseDamage(1.0) > DanteTable.FIST * 1.5, "the ultimate hits hardest");
    }

    /** Plays a fight: a decision every 5 ticks, the target wandering between 2 and 36 blocks. Returns how often each move was used. */
    static Map<String, Integer> play(long seed, int power, int ticks) {
        MoveScheduler sch = new MoveScheduler(seed);
        sch.reset(0);
        Map<String, Integer> used = new HashMap<>();
        PowerCurve.Tuning tu = PowerCurve.tuning(power);
        long busyUntil = 0;
        String prev = "";
        int run = 0;
        for (long t = 20; t < ticks; t += 5) {
            if (t < busyUntil) continue;
            double dist = 2 + 17 * (1 + Math.sin(t / 97.0)) + 2 * Math.sin(t / 13.0);
            MoveScheduler.Situation s = new MoveScheduler.Situation(dist, 0.6f, power, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(SPECS, s);
            if (idx < 0) continue;
            DanteTable.Spec m = SPECS.get(idx);
            sch.commit(m, s, (int) Math.max(10, Math.round(m.cooldownTicks() * tu.cooldownMultiplier())));
            used.merge(m.id(), 1, Integer::sum);
            busyUntil = t + Math.max(8, Math.round(m.windUpTicks() * tu.windUpMultiplier())) + m.recovery();
            if (m.id().equals(prev)) run++;
            else { prev = m.id(); run = 1; }
            assertTrue(m.ultimate() || run <= MoveScheduler.REPEAT_LIMIT, "no move spammed: " + m.id());
        }
        return used;
    }

    @Test
    void atTheOpeningHeUsesOnlyTheOpeningMovesAndNeverTheUltimate() {
        for (long seed = 1; seed <= 5; seed++) {
            Map<String, Integer> used = play(seed, 20, 20000);
            for (String id : used.keySet()) assertTrue(DanteTable.byId(id).minPower() <= 20, id + " is not an opening move");
            assertNull(used.get(DanteTable.SINGULARITY));
            assertNull(used.get(DanteTable.GRAVITY_PULL), "the pull waits for 40%");
            assertTrue(used.size() >= 6, "variety at 20%: " + used.keySet());
        }
    }

    @Test
    void singularityNeedsSixtyPercentAndComesAfterTheOpeningDelay() {
        for (long seed = 1; seed <= 5; seed++) {
            assertNull(play(seed, 59, 20000).get(DanteTable.SINGULARITY));
            assertNotNull(play(seed, 60, 20000).get(DanteTable.SINGULARITY), "he uses it at 60%");
        }
        MoveScheduler sch = new MoveScheduler(3);
        sch.reset(0);
        MoveScheduler.Situation early = new MoveScheduler.Situation(10, 0.3f, 80, 1, MoveScheduler.FIRST_ULTIMATE_DELAY - 1, Double.POSITIVE_INFINITY, 1200);
        assertFalse(sch.eligible(DanteTable.byId(DanteTable.SINGULARITY), early));
    }

    @Test
    void atTheGatesEveryMoveGetsUsedAndUltimatesAreSpaced() {
        for (long seed = 1; seed <= 3; seed++) {
            Map<String, Integer> used = play(seed, 100, 60000);
            for (DanteTable.Spec s : SPECS) assertNotNull(used.get(s.id()), s.id() + " is used at the Gates (seed " + seed + ")");
            int ults = used.get(DanteTable.SINGULARITY);
            assertTrue(ults >= 3, "several singularities in a long fight");
            assertTrue(ults * PowerCurve.tuning(100).ultimateGapTicks() <= 60000 + PowerCurve.tuning(100).ultimateGapTicks(), "never faster than the gap");
        }
    }

    @Test
    void theFistIsTheBreadAndButterUpClose() {
        Map<String, Integer> used = new HashMap<>();
        MoveScheduler sch = new MoveScheduler(11);
        sch.reset(0);
        PowerCurve.Tuning tu = PowerCurve.tuning(40);
        for (long t = 20; t < 4000; t += 5) {
            MoveScheduler.Situation s = new MoveScheduler.Situation(3.0, 0.8f, 40, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(SPECS, s);
            if (idx < 0) continue;
            DanteTable.Spec m = SPECS.get(idx);
            sch.commit(m, s, m.cooldownTicks());
            used.merge(m.id(), 1, Integer::sum);
        }
        assertTrue(used.getOrDefault(DanteTable.TITAN_FIST, 0) >= used.getOrDefault(DanteTable.STONE_SPIKES, 0), "close in he brawls: " + used);
        assertNull(used.get(DanteTable.ROCK_SWORDS), "no swords at 3 blocks");
    }
}
