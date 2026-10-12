package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Lucius's move table, and a played-out fight through the scheduler: what he does at each power and distance. */
class LuciusTableTest {
    static final List<LuciusTable.Spec> SPECS = LuciusTable.specs();

    @Test
    void theTableIsWellFormed() {
        assertTrue(SPECS.size() >= 14, "at least fourteen moves");
        Set<String> ids = new HashSet<>();
        for (LuciusTable.Spec s : SPECS) {
            assertTrue(ids.add(s.id()), "unique id " + s.id());
            assertFalse(s.label().isBlank());
            assertTrue(s.minPower() >= 20 && s.minPower() <= 80, s.id() + " is available before the Gates");
            assertTrue(s.windUpTicks() > 0, s.id() + " has a telegraph");
            assertTrue(Math.round(s.windUpTicks() * 0.75f) >= 8, s.id() + " keeps 8 ticks of tell even at the Gates");
            assertTrue(s.cooldownTicks() >= 60 && s.weight() >= 1);
            assertTrue(s.minRange() >= 0 && s.maxRange() > s.minRange());
            assertTrue(s.recovery() >= 6, "a beat to breathe after every move");
            assertTrue(s.manaCost() > 0);
        }
        assertEquals(20, SPECS.size());
        assertSame(LuciusTable.byId(LuciusTable.TOWER_STORM), SPECS.get(SPECS.size() - 1));
        assertNull(LuciusTable.byId("nope"));
    }

    @Test
    void fourUltimatesArriveAsHisPowerGrows() {
        List<LuciusTable.Spec> ults = SPECS.stream().filter(LuciusTable.Spec::ultimate).toList();
        assertEquals(4, ults.size());
        for (LuciusTable.Spec u : ults) {
            assertTrue(u.windUpTicks() >= 56, u.id() + " is a ritual, not a flick");
            assertTrue(u.recovery() >= 40, u.id() + " leaves him exposed (Move.EXPOSED_FROM)");
            assertTrue(u.cooldownTicks() >= PowerCurve.tuning(100).ultimateGapTicks());
            assertTrue(u.minPower() >= 40, "no ultimate at the opening");
        }
        assertEquals(40, LuciusTable.byId(LuciusTable.SOUL_PURGE).minPower());
        assertEquals(60, LuciusTable.byId(LuciusTable.SIBLINGS).minPower());
        assertEquals(80, LuciusTable.byId(LuciusTable.TIME_STOP).minPower());
        assertEquals(80, LuciusTable.byId(LuciusTable.TOWER_STORM).minPower());
    }

    @Test
    void everyPartOfTheBriefHasAMove() {
        for (String id : new String[]{LuciusTable.SOUL_DRAIN, LuciusTable.SOUL_PURGE, LuciusTable.RESURRECTION, LuciusTable.PALADIN_CALL,
                LuciusTable.MEMORY_SCRAMBLE, LuciusTable.TIME_STOP, LuciusTable.TIME_HALT, LuciusTable.TIME_REVERSAL, LuciusTable.ACCELERATE,
                LuciusTable.LIGHT_STEP, LuciusTable.TOWER_SPELL, LuciusTable.TOWER_STORM, LuciusTable.GRAVITY_CRUSH, LuciusTable.BONE_ARSENAL,
                LuciusTable.BLOOD_TIDE, LuciusTable.SIBLINGS, LuciusTable.MIRROR_COURT, LuciusTable.SWAP_STRIKE})
            assertNotNull(LuciusTable.byId(id), id);
        assertTrue(SPECS.stream().anyMatch(s -> s.crowd()), "crowd moves");
        assertTrue(SPECS.stream().anyMatch(s -> s.maxRange() <= 22), "close-range answers");
        assertTrue(SPECS.stream().anyMatch(s -> s.minRange() >= 5 && s.maxRange() >= 34), "ranged");
    }

    @Test
    void thereIsNoDeadRangeAtTheOpening() {
        for (double d = 0; d <= 38; d += 0.5) {
            double dist = d;
            assertTrue(SPECS.stream().anyMatch(s -> !s.ultimate() && s.minPower() <= 20 && dist >= s.minRange() && dist <= s.maxRange()
                            && !s.id().equals(LuciusTable.MIRROR_COURT)),
                    "an attack to throw at " + d + " blocks at 20%");
        }
    }

    @Test
    void manaMatchesTheFramework() {
        for (LuciusTable.Spec s : SPECS) {
            if (s.ultimate()) assertEquals(60, s.manaCost());
            else assertEquals(Math.min(32, 6 + s.cooldownTicks() / 16.0), s.manaCost(), 1e-9);
        }
        double all = SPECS.stream().filter(s -> !s.ultimate() && s.minPower() <= 20).mapToDouble(LuciusTable.Spec::manaCost).sum();
        assertTrue(all > PowerCurve.tuning(20).manaCap() * 0.5 && all < PowerCurve.tuning(20).manaCap() * 3);
    }

    @Test
    void damageNumbersAreSane() {
        assertTrue(LuciusTable.HALT_BLOW > LuciusTable.STEP && LuciusTable.STEP > LuciusTable.MOTE * 2);
        assertTrue(LuciusTable.STOP_BLOW < LuciusTable.HALT_BLOW, "the stop's blows are many, so each is lighter");
        assertTrue(LuciusTable.MOTE * 6 < LuciusTable.HALT_BLOW * 2, "a full volley of motes is no stasis");
        assertTrue(LuciusTable.DRAIN_TICK < LuciusTable.PURGE);
        for (float f : new float[]{LuciusTable.MOTE, LuciusTable.DRAIN_TICK, LuciusTable.STEP, LuciusTable.HALT_BLOW, LuciusTable.STOP_BLOW, LuciusTable.PURGE,
                LuciusTable.SWAP, LuciusTable.BONE_SPEAR, LuciusTable.BONE_BLADE, LuciusTable.BLOOD_LANCE, LuciusTable.SIBLING_SPEAR,
                LuciusTable.SIBLING_LANCE, LuciusTable.LASH, LuciusTable.MEMORY_TOUCH, LuciusTable.STORM_BOLT})
            assertTrue(f > 0 && f <= 30, "a sane base damage " + f);
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
            LuciusTable.Spec m = SPECS.get(idx);
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
    void atTheOpeningHeUsesOnlyTheOpeningMovesAndNoUltimate() {
        for (long seed = 1; seed <= 5; seed++) {
            Map<String, Integer> used = play(seed, 20, 20000);
            for (String id : used.keySet()) assertTrue(LuciusTable.byId(id).minPower() <= 20, id + " is not an opening move");
            for (LuciusTable.Spec s : SPECS) if (s.ultimate()) assertNull(used.get(s.id()));
            assertNull(used.get(LuciusTable.TIME_HALT), "the stasis waits for 40%");
            assertNull(used.get(LuciusTable.TIME_REVERSAL), "the rewind waits for 60%");
            assertTrue(used.size() >= 4, "variety at 20%: " + used.keySet());
        }
    }

    @Test
    void theUltimatesUnlockInOrder() {
        for (long seed = 1; seed <= 5; seed++) {
            assertNull(play(seed, 39, 30000).get(LuciusTable.SOUL_PURGE));
            assertNotNull(play(seed, 40, 30000).get(LuciusTable.SOUL_PURGE), "the purge at 40%");
            assertNull(play(seed, 59, 30000).get(LuciusTable.SIBLINGS));
            assertNotNull(play(seed, 60, 30000).get(LuciusTable.SIBLINGS), "the siblings at 60%");
            assertNull(play(seed, 79, 30000).get(LuciusTable.TIME_STOP));
            assertNull(play(seed, 79, 30000).get(LuciusTable.TOWER_STORM));
        }
        Map<String, Integer> top = new HashMap<>();
        for (long seed = 1; seed <= 6; seed++) play(seed, 80, 60000).forEach((k, v) -> top.merge(k, v, Integer::sum));
        assertNotNull(top.get(LuciusTable.TIME_STOP), "the time stop at 80%");
        assertNotNull(top.get(LuciusTable.TOWER_STORM), "the tower storm at 80%");
    }

    @Test
    void firstUltimateNeverComesBeforeTheOpeningDelay() {
        MoveScheduler sch = new MoveScheduler(3);
        sch.reset(0);
        MoveScheduler.Situation early = new MoveScheduler.Situation(10, 0.3f, 100, 1, MoveScheduler.FIRST_ULTIMATE_DELAY - 1, Double.POSITIVE_INFINITY, 900);
        for (LuciusTable.Spec s : SPECS) if (s.ultimate()) assertFalse(sch.eligible(s, early), s.id());
    }

    @Test
    void atTheGatesEveryMoveGetsUsedAndUltimatesAreSpaced() {
        Map<String, Integer> total = new HashMap<>();
        for (long seed = 1; seed <= 4; seed++) {
            Map<String, Integer> used = play(seed, 100, 90000);
            used.forEach((k, v) -> total.merge(k, v, Integer::sum));
            int ults = 0;
            for (LuciusTable.Spec s : SPECS) if (s.ultimate()) ults += used.getOrDefault(s.id(), 0);
            assertTrue(ults >= 5, "several ultimates in a long fight (seed " + seed + "): " + ults);
            assertTrue((long) ults * PowerCurve.tuning(100).ultimateGapTicks() <= 90000 + PowerCurve.tuning(100).ultimateGapTicks(), "never faster than the gap");
        }
        for (LuciusTable.Spec s : SPECS) assertNotNull(total.get(s.id()), s.id() + " is used at the Gates");
    }

    @Test
    void theTowersAreHisBreadAndButter() {
        Map<String, Integer> used = new HashMap<>();
        MoveScheduler sch = new MoveScheduler(11);
        sch.reset(0);
        PowerCurve.Tuning tu = PowerCurve.tuning(40);
        for (long t = 20; t < 8000; t += 5) {
            MoveScheduler.Situation s = new MoveScheduler.Situation(14.0, 0.8f, 40, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(SPECS, s);
            if (idx < 0) continue;
            LuciusTable.Spec m = SPECS.get(idx);
            sch.commit(m, s, m.cooldownTicks());
            used.merge(m.id(), 1, Integer::sum);
        }
        int towers = used.getOrDefault(LuciusTable.TOWER_SPELL, 0);
        for (var e : used.entrySet())
            if (!e.getKey().equals(LuciusTable.TOWER_SPELL) && !LuciusTable.byId(e.getKey()).ultimate())
                assertTrue(towers >= e.getValue() * 0.6, "the towers are used about as much as anything else: " + used);
    }
}
