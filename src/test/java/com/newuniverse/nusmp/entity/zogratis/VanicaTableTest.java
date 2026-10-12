package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Vanica's move table, and a played-out fight through the scheduler: what she does at each power and distance. */
class VanicaTableTest {
    static final List<VanicaTable.Spec> SPECS = VanicaTable.specs();

    @Test
    void theTableIsWellFormed() {
        assertTrue(SPECS.size() >= 10, "at least ten moves");
        Set<String> ids = new HashSet<>();
        for (VanicaTable.Spec s : SPECS) {
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
        assertSame(VanicaTable.byId(VanicaTable.CRIMSON_CROWN), SPECS.get(SPECS.size() - 1));
        assertNull(VanicaTable.byId("nope"));
    }

    @Test
    void theMixIsLancesCurseSummonDashUtilityAndOneUltimate() {
        assertEquals(1, SPECS.stream().filter(VanicaTable.Spec::ultimate).count());
        VanicaTable.Spec ult = VanicaTable.byId(VanicaTable.CRIMSON_CROWN);
        assertTrue(ult.ultimate() && ult.minPower() >= 60, "only usable at 60% and above");
        assertTrue(ult.windUpTicks() >= 40, "a ritual, not a flick");
        assertTrue(ult.recovery() >= 40, "she is exposed afterwards (Move.EXPOSED_FROM)");
        assertTrue(ult.cooldownTicks() >= PowerCurve.tuning(100).ultimateGapTicks());
        assertTrue(SPECS.stream().anyMatch(s -> s.maxRange() <= 10), "melee");
        assertTrue(SPECS.stream().anyMatch(s -> s.minRange() >= 5 && s.maxRange() >= 26), "ranged");
        assertTrue(SPECS.stream().anyMatch(VanicaTable.Spec::crowd), "crowd moves");
        for (String id : new String[]{VanicaTable.BLOOD_LANCES, VanicaTable.BLOOD_SCYTHE, VanicaTable.BLOOD_WHIP, VanicaTable.CRIMSON_DASH,
                VanicaTable.ROT_FIELD, VanicaTable.DECAY_CURSE, VanicaTable.BLOOD_BEASTS, VanicaTable.CALL_DISCIPLES, VanicaTable.BLOOD_FRENZY})
            assertNotNull(VanicaTable.byId(id), id);
        assertTrue(VanicaTable.byId(VanicaTable.CRIMSON_DASH).minRange() >= 5, "the dash closes a gap, it is not a melee move");
    }

    @Test
    void thereIsNoDeadRangeAtTheOpening() {
        // an attack, not a utility move (the beasts, the disciples and the frenzy work at any distance and would hide a gap)
        for (double d = 0; d <= 34; d += 0.5) {
            double dist = d;
            assertTrue(SPECS.stream().anyMatch(s -> !s.ultimate() && s.minPower() <= 20 && dist >= s.minRange() && dist <= s.maxRange()
                            && !s.id().equals(VanicaTable.CALL_DISCIPLES) && !s.id().equals(VanicaTable.BLOOD_FRENZY) && !s.id().equals(VanicaTable.BLOOD_BEASTS)),
                    "an attack to throw at " + d + " blocks at 20%");
        }
    }

    @Test
    void manaMatchesTheFramework() {
        for (VanicaTable.Spec s : SPECS) {
            if (s.ultimate()) assertEquals(60, s.manaCost());
            else assertEquals(Math.min(32, 6 + s.cooldownTicks() / 16.0), s.manaCost(), 1e-9);
        }
        double all = SPECS.stream().filter(s -> !s.ultimate() && s.minPower() <= 20).mapToDouble(VanicaTable.Spec::manaCost).sum();
        assertTrue(all > PowerCurve.tuning(20).manaCap() * 0.5 && all < PowerCurve.tuning(20).manaCap() * 3, "the opening pool pays for a few moves, not all");
    }

    @Test
    void damageNumbersAreSane() {
        assertTrue(VanicaTable.SCYTHE > VanicaTable.WHIP && VanicaTable.DASH > VanicaTable.WHIP, "the close blows are the heavy ones");
        assertTrue(VanicaTable.LANCE * VanicaMath.lanceCount(4, true) < VanicaTable.THORN * 3, "a full volley is no thorn ring");
        assertTrue(VanicaTable.THORN > VanicaTable.SCYTHE, "the ultimate hits hardest");
        assertTrue(VanicaTable.CURSE_DOT < VanicaTable.LANCE, "the curse is a slow burn, not a hit");
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
            VanicaTable.Spec m = SPECS.get(idx);
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
    void atTheOpeningSheUsesOnlyTheOpeningMovesAndNeverTheUltimate() {
        for (long seed = 1; seed <= 5; seed++) {
            Map<String, Integer> used = play(seed, 20, 20000);
            for (String id : used.keySet()) assertTrue(VanicaTable.byId(id).minPower() <= 20, id + " is not an opening move");
            assertNull(used.get(VanicaTable.CRIMSON_CROWN));
            assertNull(used.get(VanicaTable.CRIMSON_RAIN), "the rain waits for 40%");
            assertNull(used.get(VanicaTable.BLOOD_FRENZY), "the frenzy waits for 40%");
            assertTrue(used.size() >= 6, "variety at 20%: " + used.keySet());
        }
    }

    @Test
    void theCrownNeedsSixtyPercentAndComesAfterTheOpeningDelay() {
        for (long seed = 1; seed <= 5; seed++) {
            assertNull(play(seed, 59, 20000).get(VanicaTable.CRIMSON_CROWN));
            assertNotNull(play(seed, 60, 20000).get(VanicaTable.CRIMSON_CROWN), "she uses it at 60%");
        }
        MoveScheduler sch = new MoveScheduler(3);
        sch.reset(0);
        MoveScheduler.Situation early = new MoveScheduler.Situation(10, 0.3f, 80, 1, MoveScheduler.FIRST_ULTIMATE_DELAY - 1, Double.POSITIVE_INFINITY, 1200);
        assertFalse(sch.eligible(VanicaTable.byId(VanicaTable.CRIMSON_CROWN), early));
    }

    @Test
    void atTheGatesEveryMoveGetsUsedAndUltimatesAreSpaced() {
        for (long seed = 1; seed <= 3; seed++) {
            Map<String, Integer> used = play(seed, 100, 60000);
            for (VanicaTable.Spec s : SPECS) assertNotNull(used.get(s.id()), s.id() + " is used at the Gates (seed " + seed + ")");
            int ults = used.get(VanicaTable.CRIMSON_CROWN);
            assertTrue(ults >= 3, "several crowns in a long fight");
            assertTrue(ults * PowerCurve.tuning(100).ultimateGapTicks() <= 60000 + PowerCurve.tuning(100).ultimateGapTicks(), "never faster than the gap");
        }
    }

    @Test
    void upCloseTheScytheIsTheBreadAndButter() {
        Map<String, Integer> used = new HashMap<>();
        MoveScheduler sch = new MoveScheduler(11);
        sch.reset(0);
        PowerCurve.Tuning tu = PowerCurve.tuning(40);
        for (long t = 20; t < 4000; t += 5) {
            MoveScheduler.Situation s = new MoveScheduler.Situation(3.0, 0.8f, 40, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(SPECS, s);
            if (idx < 0) continue;
            VanicaTable.Spec m = SPECS.get(idx);
            sch.commit(m, s, m.cooldownTicks());
            used.merge(m.id(), 1, Integer::sum);
        }
        assertTrue(used.getOrDefault(VanicaTable.BLOOD_SCYTHE, 0) >= used.getOrDefault(VanicaTable.BLOOD_WHIP, 0), "close in she cuts: " + used);
        assertNull(used.get(VanicaTable.BLOOD_LANCES), "no lances at 3 blocks");
        assertNull(used.get(VanicaTable.CRIMSON_DASH), "no dash at 3 blocks");
    }

    @Test
    void theMoveBookKeepsEveryMove() {
        MoveBook<VanicaTable.Spec> book = MoveBook.of(SPECS);
        assertEquals(SPECS.size(), book.moves().size());
        assertTrue(book.dropped().isEmpty());
        assertEquals(SPECS.size() - 1, book.indexOf(VanicaTable.CRIMSON_CROWN));
    }
}
