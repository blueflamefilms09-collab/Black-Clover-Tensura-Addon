package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Zenon's move table, and played-out fights through the scheduler: what he does at each power and distance. */
class ZenonTableTest {
    static final List<ZenonTable.Spec> SPECS = ZenonTable.specs();

    @Test
    void theTableIsWellFormed() {
        assertTrue(SPECS.size() >= 10, "at least ten moves");
        Set<String> ids = new HashSet<>();
        for (ZenonTable.Spec s : SPECS) {
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
        assertSame(ZenonTable.byId(ZenonTable.DOMINATION), SPECS.get(10), "the first eleven moves keep their places (the client reads names by index)");
        assertEquals(ZenonTable.RIFT_CUTS, SPECS.get(11).id(), "1.35.0 moves are appended");
        assertEquals(ZenonTable.GATE_STORM, SPECS.get(12).id());
        assertTrue(ZenonTable.byId(ZenonTable.RIFT_CUTS).minPower() >= 60 && ZenonTable.byId(ZenonTable.GATE_STORM).minPower() >= 80, "late-phase moves");
        assertNull(ZenonTable.byId("nope"));
    }

    @Test
    void theMixIsBoneSpatialSummonUtilityAndOneUltimate() {
        long ultimates = SPECS.stream().filter(ZenonTable.Spec::ultimate).count();
        assertEquals(1, ultimates);
        ZenonTable.Spec ult = ZenonTable.byId(ZenonTable.DOMINATION);
        assertTrue(ult.ultimate() && ult.minPower() >= 60, "only usable at 60% and above");
        assertTrue(ult.windUpTicks() >= 40, "a ritual, not a flick");
        assertTrue(ult.recovery() >= 40, "he is exposed afterwards");
        assertTrue(ult.cooldownTicks() >= PowerCurve.tuning(100).ultimateGapTicks());
        assertTrue(SPECS.stream().anyMatch(s -> s.maxRange() <= 12), "melee");
        assertTrue(SPECS.stream().anyMatch(s -> s.minRange() >= 5 && s.maxRange() >= 26), "ranged");
        assertTrue(SPECS.stream().anyMatch(ZenonTable.Spec::crowd), "crowd moves");
        for (String id : new String[]{ZenonTable.BONE_SPEARS, ZenonTable.BONE_BLADES, ZenonTable.BONE_GARDEN, ZenonTable.BONE_LANCE, ZenonTable.BONE_ARMOR,
                ZenonTable.BACKSTAB, ZenonTable.PORTAL_LINK, ZenonTable.WARP_BARRAGE, ZenonTable.TENSURA_SPACE, ZenonTable.CALL_DISCIPLES})
            assertNotNull(ZenonTable.byId(id), id);
    }

    @Test
    void onlyTheUltimateLeavesHimExposed() {
        for (ZenonTable.Spec s : SPECS) if (!s.ultimate()) assertTrue(s.recovery() < 40, s.id() + " must not expose him (recovery under 40 ticks)");
    }

    @Test
    void thereIsNoDeadRangeAtTheOpening() {
        // an attack, not a utility move (the Disciples and the armour work at any distance and would hide a gap)
        for (double d = 0; d <= 42; d += 0.5) {
            double dist = d;
            assertTrue(SPECS.stream().anyMatch(s -> !s.ultimate() && s.minPower() <= 20 && dist >= s.minRange() && dist <= s.maxRange()
                            && !s.id().equals(ZenonTable.CALL_DISCIPLES) && !s.id().equals(ZenonTable.BONE_ARMOR)),
                    "an attack to throw at " + d + " blocks at 20%");
        }
    }

    @Test
    void manaMatchesTheFramework() {
        for (ZenonTable.Spec s : SPECS) {
            if (s.ultimate()) assertEquals(60, s.manaCost());
            else assertEquals(Math.min(32, 6 + s.cooldownTicks() / 16.0), s.manaCost(), 1e-9);
        }
        // the opening pool (100) pays for a few moves, not for everything at once
        double all = SPECS.stream().filter(s -> !s.ultimate() && s.minPower() <= 20).mapToDouble(ZenonTable.Spec::manaCost).sum();
        assertTrue(all > PowerCurve.tuning(20).manaCap() * 0.5 && all < PowerCurve.tuning(20).manaCap() * 3);
    }

    @Test
    void damageNumbersAreSmallAndPrecise() {
        assertTrue(ZenonTable.LANCE > ZenonTable.BACKSTAB_HIT && ZenonTable.BACKSTAB_HIT > ZenonTable.SPEAR, "the lance is the biggest single blow");
        float worstSpears = ZenonTable.SPEAR * BoneMath.MAX_HITS_PER_VICTIM;
        float worstBlades = ZenonTable.BLADE * BoneMath.bladeSlashes(4);
        float worstWarp = ZenonTable.WARP_SPEAR * PortalMath.warpHops(100);
        assertTrue(worstSpears < ZenonTable.LANCE, "a whole barrage on one victim is less than the lance: " + worstSpears);
        assertTrue(worstBlades < ZenonTable.LANCE * 2 && worstWarp < ZenonTable.LANCE * 2, "no combo is a one-shot: " + worstBlades + " / " + worstWarp);
        assertTrue(ZenonTable.SPEAR <= 8 && ZenonTable.PORTAL_SPEAR <= 8 && ZenonTable.WARP_SPEAR <= 8, "each projectile is small");
        assertTrue(ZenonTable.SHARDS < ZenonTable.GARDEN, "the armour's shards are a punishment, not an attack");
        assertTrue(ZenonTable.DAMAGE_SCALE > 1f && ZenonTable.DAMAGE_SCALE <= 1.25f, "precision, not brute force");
    }

    @Test
    void theBuildIsAFastMediumHealthAssassin() {
        assertTrue(ZenonTable.HEALTH >= 800 && ZenonTable.HEALTH < 1700, "medium health, under Dante's 1700");
        assertTrue(ZenonTable.SPEED >= 0.32, "fast");
        assertTrue(ZenonTable.ARMOR < 14, "lighter than Dante");
        assertTrue(ZenonTable.HYBRID_SCALE > 1f && ZenonTable.HYBRID_SCALE < 1.25f);
    }

    @Test
    void spatialEvasionGetsFasterWithPowerAndStaysAFairGap() {
        assertEquals(160, ZenonTable.evadeCooldown(20));
        assertEquals(140, ZenonTable.evadeCooldown(40));
        assertEquals(120, ZenonTable.evadeCooldown(60));
        assertEquals(90, ZenonTable.evadeCooldown(80));
        assertEquals(60, ZenonTable.evadeCooldown(100));
        int last = Integer.MAX_VALUE;
        for (int p = 0; p <= 120; p++) {
            int c = ZenonTable.evadeCooldown(p);
            assertTrue(c <= last, "never slower with more power at " + p);
            assertTrue(c >= 60, "at least three seconds between two slipped blows");
            last = c;
        }
        assertEquals(160, ZenonTable.evadeCooldown(0));
        assertEquals(60, ZenonTable.evadeCooldown(500));
        assertTrue(ZenonTable.EVADE_MIN >= 2 && ZenonTable.EVADE_MIN <= 6, "chip damage does not trigger it");
    }

    /** Plays a fight: a decision every 5 ticks, the target wandering between 2 and 38 blocks. Returns how often each move was used. */
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
            double dist = 2 + 18 * (1 + Math.sin(t / 97.0)) + 2 * Math.sin(t / 13.0);
            MoveScheduler.Situation s = new MoveScheduler.Situation(dist, 0.6f, power, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(SPECS, s);
            if (idx < 0) continue;
            ZenonTable.Spec m = SPECS.get(idx);
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
            for (String id : used.keySet()) assertTrue(ZenonTable.byId(id).minPower() <= 20, id + " is not an opening move");
            assertNull(used.get(ZenonTable.DOMINATION));
            assertNull(used.get(ZenonTable.PORTAL_LINK), "the portal link waits for 40%");
            assertNull(used.get(ZenonTable.WARP_BARRAGE), "the warp barrage waits for 40%");
            assertNull(used.get(ZenonTable.BONE_ARMOR), "the armour waits for 40%");
            assertTrue(used.size() >= 5, "variety at 20%: " + used.keySet());
        }
    }

    @Test
    void theDomainNeedsSixtyPercentAndComesAfterTheOpeningDelay() {
        for (long seed = 1; seed <= 5; seed++) {
            assertNull(play(seed, 59, 20000).get(ZenonTable.DOMINATION));
            assertNotNull(play(seed, 60, 20000).get(ZenonTable.DOMINATION), "he uses it at 60%");
        }
        MoveScheduler sch = new MoveScheduler(3);
        sch.reset(0);
        MoveScheduler.Situation early = new MoveScheduler.Situation(10, 0.3f, 80, 1, MoveScheduler.FIRST_ULTIMATE_DELAY - 1, Double.POSITIVE_INFINITY, 1200);
        assertFalse(sch.eligible(ZenonTable.byId(ZenonTable.DOMINATION), early));
    }

    @Test
    void atTheGatesEveryMoveGetsUsedAndUltimatesAreSpaced() {
        for (long seed = 1; seed <= 3; seed++) {
            Map<String, Integer> used = play(seed, 100, 60000);
            for (ZenonTable.Spec s : SPECS) assertNotNull(used.get(s.id()), s.id() + " is used at the Gates (seed " + seed + ")");
            int ults = used.get(ZenonTable.DOMINATION);
            assertTrue(ults >= 3, "several domains in a long fight");
            assertTrue(ults * PowerCurve.tuning(100).ultimateGapTicks() <= 60000 + PowerCurve.tuning(100).ultimateGapTicks(), "never faster than the gap");
        }
    }

    @Test
    void theBladesAreTheBreadAndButterUpClose() {
        Map<String, Integer> used = new HashMap<>();
        MoveScheduler sch = new MoveScheduler(11);
        sch.reset(0);
        PowerCurve.Tuning tu = PowerCurve.tuning(40);
        for (long t = 20; t < 4000; t += 5) {
            MoveScheduler.Situation s = new MoveScheduler.Situation(2.0, 0.8f, 40, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(SPECS, s);
            if (idx < 0) continue;
            ZenonTable.Spec m = SPECS.get(idx);
            sch.commit(m, s, m.cooldownTicks());
            used.merge(m.id(), 1, Integer::sum);
        }
        assertTrue(used.getOrDefault(ZenonTable.BONE_BLADES, 0) > 0, "close in he cuts: " + used);
        assertNull(used.get(ZenonTable.BONE_SPEARS), "no spears at 2 blocks");
        assertNull(used.get(ZenonTable.BONE_LANCE), "no lance at 2 blocks");
        assertNull(used.get(ZenonTable.BACKSTAB), "no backstab at 2 blocks: he is already there");
    }

    @Test
    void farAwayHeStillHasSomethingToThrow() {
        MoveScheduler sch = new MoveScheduler(5);
        sch.reset(0);
        Set<String> seen = new HashSet<>();
        PowerCurve.Tuning tu = PowerCurve.tuning(40);
        for (long t = 20; t < 6000; t += 5) {
            MoveScheduler.Situation s = new MoveScheduler.Situation(38, 0.8f, 40, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(SPECS, s);
            if (idx < 0) continue;
            ZenonTable.Spec m = SPECS.get(idx);
            sch.commit(m, s, m.cooldownTicks());
            seen.add(m.id());
        }
        assertTrue(seen.contains(ZenonTable.PORTAL_LINK) || seen.contains(ZenonTable.BONE_LANCE), "at 38 blocks: " + seen);
        assertFalse(seen.contains(ZenonTable.BONE_BLADES));
    }
}
