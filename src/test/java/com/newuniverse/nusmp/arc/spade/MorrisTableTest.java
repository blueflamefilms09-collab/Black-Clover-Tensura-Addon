package com.newuniverse.nusmp.arc.spade;

import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Morris's move table, and a played-out fight through the framework's scheduler: what the scholar does at each power and distance. */
class MorrisTableTest {
    static final List<MorrisTable.Spec> SPECS = MorrisTable.specs();
    static final int EXPOSED_FROM = 40;

    @Test
    void theTableIsWellFormed() {
        assertEquals(11, SPECS.size());
        Set<String> ids = new HashSet<>();
        for (MorrisTable.Spec s : SPECS) {
            assertTrue(ids.add(s.id()), "unique id " + s.id());
            assertFalse(s.label().isBlank());
            assertTrue(s.minPower() == 20 || s.minPower() == 40 || s.minPower() == 60 || s.minPower() == 80 || s.minPower() == 100, s.id() + " gates on a real power step");
            assertTrue(s.windUpTicks() > 0, s.id() + " has a telegraph");
            assertTrue(Math.round(s.windUpTicks() * 0.75f) >= 8, s.id() + " keeps 8 ticks of tell even at the Gates");
            assertTrue(s.cooldownTicks() >= 60 && s.weight() >= 1);
            assertTrue(s.minRange() >= 0 && s.maxRange() > s.minRange());
            assertTrue(s.recovery() >= 8);
            assertTrue(s.manaCost() > 0);
        }
    }

    @Test
    void theOrderIsPinnedBecauseTheClientReadsNamesByIndex() {
        String[] order = {MorrisTable.NEEDLES, MorrisTable.STRUCTURE_SHOCK, MorrisTable.TENDRILS, MorrisTable.SPECIMEN, MorrisTable.SPELL_REWRITE,
                MorrisTable.TEST_SUBJECTS, MorrisTable.RESTRUCTURE, MorrisTable.PIN_DOWN, MorrisTable.BORROWED_WEIGHT, MorrisTable.DISSECTION_THEATRE,
                MorrisTable.SECOND_GATE};
        for (int i = 0; i < order.length; i++) assertEquals(order[i], SPECS.get(i).id(), "move " + i);
        assertNull(MorrisTable.byId("nope"));
    }

    @Test
    void theMixIsPokeShockGrabThreadModificationFollowersBorrowedGravityAndTwoUltimates() {
        assertEquals(2, SPECS.stream().filter(MorrisTable.Spec::ultimate).count());
        MorrisTable.Spec theatre = MorrisTable.byId(MorrisTable.DISSECTION_THEATRE), gate = MorrisTable.byId(MorrisTable.SECOND_GATE);
        assertTrue(theatre.ultimate() && theatre.minPower() == 60);
        assertTrue(gate.ultimate() && gate.minPower() == 100, "the second gate opens with the Gates");
        for (MorrisTable.Spec u : new MorrisTable.Spec[]{theatre, gate}) {
            assertTrue(u.windUpTicks() >= 60 && Math.round(u.windUpTicks() * 0.75f) >= 30, u.id() + " is a ritual");
            assertTrue(u.recovery() >= EXPOSED_FROM, "exposed after " + u.id());
            assertTrue(u.cooldownTicks() >= PowerCurve.tuning(100).ultimateGapTicks());
        }
        assertTrue(MorrisTable.byId(MorrisTable.PIN_DOWN).minPower() >= 60 && MorrisTable.byId(MorrisTable.BORROWED_WEIGHT).minPower() >= 60,
                "the gravity is Lucifero's, and comes in late");
        assertTrue(MorrisTable.byId(MorrisTable.RESTRUCTURE).minPower() >= 40, "the tuning is not there at the opening");
        assertTrue(SPECS.stream().anyMatch(s -> s.maxRange() <= 6 && !s.ultimate()), "point blank");
        assertTrue(SPECS.stream().anyMatch(s -> s.minRange() >= 5 && s.maxRange() >= 30), "range");
        assertTrue(SPECS.stream().anyMatch(MorrisTable.Spec::crowd));
    }

    @Test
    void thereIsNoDeadRangeAtTheOpening() {
        Set<String> utility = Set.of(MorrisTable.SPELL_REWRITE, MorrisTable.TEST_SUBJECTS, MorrisTable.RESTRUCTURE);
        for (double d = 0; d <= 30; d += 0.5) {
            double dist = d;
            assertTrue(SPECS.stream().anyMatch(s -> !s.ultimate() && s.minPower() <= 20 && dist >= s.minRange() && dist <= s.maxRange() && !utility.contains(s.id())),
                    "an attack to throw at " + d + " blocks at 20%");
        }
    }

    @Test
    void manaMatchesTheFramework() {
        for (MorrisTable.Spec s : SPECS) {
            if (s.ultimate()) assertEquals(60, s.manaCost());
            else assertEquals(Math.min(32, 6 + s.cooldownTicks() / 16.0), s.manaCost(), 1e-9);
        }
    }

    @Test
    void damageNumbersAreSane() {
        assertTrue(MorrisTable.DISSECT > MorrisTable.SHOCK * 1.5, "the dissection is the heaviest single blow");
        assertTrue(MorrisTable.NEEDLE * 7 < MorrisTable.DISSECT * 2, "a full fan of needles is no dissection");
        assertTrue(MorrisTable.THEATRE_SQUEEZE >= MorrisTable.DISSECT, "the cage's squeeze is the ultimate's price");
        assertTrue(MorrisTable.SPECIMEN_DRAIN < MorrisTable.NEEDLE, "the thread drains, it does not burst");
    }

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
            double dist = 2 + 15 * (1 + Math.sin(t / 91.0)) + 2 * Math.sin(t / 11.0);
            MoveScheduler.Situation s = new MoveScheduler.Situation(dist, 0.6f, power, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(SPECS, s);
            if (idx < 0) continue;
            MorrisTable.Spec m = SPECS.get(idx);
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
    void theFightGrowsStepByStep() {
        for (long seed = 1; seed <= 5; seed++) {
            Map<String, Integer> open = play(seed, 20, 30000);
            for (String id : open.keySet()) assertTrue(MorrisTable.byId(id).minPower() <= 20, id + " is not an opening move");
            assertTrue(open.size() >= 5, "variety at 20%: " + open.keySet());
            assertNull(play(seed, 39, 30000).get(MorrisTable.RESTRUCTURE));
            assertNotNull(play(seed, 40, 30000).get(MorrisTable.RESTRUCTURE), "the tuning from 40% (seed " + seed + ")");
            assertNull(play(seed, 59, 30000).get(MorrisTable.PIN_DOWN));
            assertNull(play(seed, 59, 30000).get(MorrisTable.DISSECTION_THEATRE));
            assertNotNull(play(seed, 60, 30000).get(MorrisTable.DISSECTION_THEATRE), "the theatre from 60% (seed " + seed + ")");
            assertNull(play(seed, 79, 30000).get(MorrisTable.BORROWED_WEIGHT));
            assertNull(play(seed, 99, 30000).get(MorrisTable.SECOND_GATE));
        }
    }

    @Test
    void atTheGatesEveryMoveGetsUsedAndTheUltimatesAreSpaced() {
        for (long seed = 1; seed <= 3; seed++) {
            Map<String, Integer> used = play(seed, 100, 80000);
            for (MorrisTable.Spec s : SPECS) assertNotNull(used.get(s.id()), s.id() + " is used at the Gates (seed " + seed + ")");
            int ults = used.get(MorrisTable.DISSECTION_THEATRE) + used.get(MorrisTable.SECOND_GATE);
            assertTrue(ults >= 3, "several ultimates in a long fight: " + ults);
            assertTrue((long) ults * PowerCurve.tuning(100).ultimateGapTicks() <= 80000 + PowerCurve.tuning(100).ultimateGapTicks(), "never faster than the gap");
        }
    }

    @Test
    void closeInHePushesYouOffAndAtRangeHeThrowsNeedles() {
        Map<String, Integer> close = new HashMap<>(), far = new HashMap<>();
        for (int pass = 0; pass < 2; pass++) {
            MoveScheduler sch = new MoveScheduler(5 + pass);
            sch.reset(0);
            PowerCurve.Tuning tu = PowerCurve.tuning(40);
            for (long t = 20; t < 6000; t += 5) {
                MoveScheduler.Situation s = new MoveScheduler.Situation(pass == 0 ? 3.0 : 18.0, 0.8f, 40, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
                int idx = sch.pick(SPECS, s);
                if (idx < 0) continue;
                MorrisTable.Spec m = SPECS.get(idx);
                sch.commit(m, s, m.cooldownTicks());
                (pass == 0 ? close : far).merge(m.id(), 1, Integer::sum);
            }
        }
        assertNotNull(close.get(MorrisTable.STRUCTURE_SHOCK), "point blank he shocks: " + close);
        assertNull(close.get(MorrisTable.NEEDLES), "no needles at 3 blocks");
        assertNotNull(far.get(MorrisTable.NEEDLES), "at range, needles: " + far);
        assertNull(far.get(MorrisTable.STRUCTURE_SHOCK));
    }
}
