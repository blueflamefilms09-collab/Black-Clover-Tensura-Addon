package com.newuniverse.nusmp.arc.early;

import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The five move tables: the house rules, then fights played out through the framework's own scheduler. */
class EarlyTablesTest {
    private static final Map<String, List<MoveRow>> TABLES = Map.of(
            "revchi", ChainTable.rows(), "heath", HeathTable.rows(), "lotus", LotusTable.rows(), "mars", MarsTable.rows(), "vetto", VettoTable.rows());

    @Test
    void everyTableKeepsTheHouseRules() {
        for (Map.Entry<String, List<MoveRow>> e : TABLES.entrySet()) {
            List<String> bad = MoveRow.problems(e.getValue());
            assertTrue(bad.isEmpty(), e.getKey() + ": " + bad);
        }
    }

    @Test
    void theRulesCatchABadTable() {
        // negative controls: the checker must not wave anything through
        List<MoveRow> spam = List.of(MoveRow.of("a", "A", 20, 10, 40, 0, 5, 1));
        List<String> bad = MoveRow.problems(spam);
        assertTrue(bad.stream().anyMatch(s -> s.contains("8 to 14")), "too few moves: " + bad);
        assertTrue(bad.stream().anyMatch(s -> s.contains("wind-up")), "a 10-tick telegraph is too short: " + bad);
        assertTrue(bad.stream().anyMatch(s -> s.contains("cooldown")), "a 40-tick cooldown is too short: " + bad);
        assertTrue(bad.stream().anyMatch(s -> s.contains("ultimate")), "no ultimate: " + bad);
    }

    @Test
    void manaFollowsTheFrameworkFormula() {
        for (List<MoveRow> rows : TABLES.values()) {
            for (MoveRow r : rows) {
                if (r.ultimate()) assertEquals(60.0, r.manaCost(), 1e-9);
                else assertEquals(Math.min(32, 6 + r.cooldownTicks() / 16.0), r.manaCost(), 1e-9);
            }
            // the opening pool pays for a few moves, not for everything at once
            double all = rows.stream().filter(r -> !r.ultimate() && r.minPower() <= 20).mapToDouble(MoveRow::manaCost).sum();
            assertTrue(all > PowerCurve.tuning(20).manaCap() * 0.4 && all < PowerCurve.tuning(20).manaCap() * 3, "opening mana " + all);
        }
    }

    @Test
    void eachFightHasItsSignatureMoveAtTheRightPower() {
        assertEquals(60, ChainTable.byId(ChainTable.LOCKDOWN).minPower());
        assertTrue(ChainTable.byId(ChainTable.BINDING).minPower() <= 20, "the Magic Binding is there from the first second: it is the character");
        assertTrue(ChainTable.byId(ChainTable.FORMATION).crowd() && ChainTable.byId(ChainTable.FORMATION).minPower() >= 40);
        assertEquals(40, HeathTable.byId(HeathTable.CAGE).minPower());
        assertEquals(80, HeathTable.byId(HeathTable.FUNERAL).minPower(), "the Ice Funeral is his last act");
        assertTrue(HeathTable.byId(HeathTable.SHIELD).maxRange() >= 40, "the shield is a self-buff");
        assertTrue(LotusTable.byId(LotusTable.GARDEN).minPower() <= 20, "the smoke is there from the start");
        assertTrue(LotusTable.byId(LotusTable.BLOOM).ultimate());
        assertTrue(MarsTable.byId(MarsTable.ARMOR).minPower() <= 20);
        assertTrue(MarsTable.byId(MarsTable.TITAN).ultimate() && MarsTable.byId(MarsTable.TITAN).recovery() >= 40);
        assertTrue(VettoTable.byId(VettoTable.SHIFT).minPower() <= 20, "the first aura is put on early");
        assertEquals(70, VettoTable.byId(VettoTable.RELEASE).windUpTicks(), "the Mana Release is the longest ritual of the five");
        for (List<MoveRow> rows : TABLES.values()) {
            int longest = rows.stream().filter(MoveRow::ultimate).mapToInt(MoveRow::windUpTicks).max().orElse(0);
            assertTrue(longest <= 70);
        }
    }

    @Test
    void theNumbersAreSane() {
        assertTrue(ChainTable.HEAVY_HIT > ChainTable.LASH * 2 && ChainTable.LOCKDOWN_HIT > ChainTable.HEAVY_HIT, "the heavy chain beats a lash, the lockdown beats the heavy chain");
        assertTrue(HeathTable.FUNERAL_HIT > HeathTable.COFFIN_HIT && HeathTable.COFFIN_HIT > HeathTable.ICICLE * 2);
        assertTrue(LotusTable.SPHERE_HIT > LotusTable.PUFF_HIT);
        assertTrue(MarsTable.TITAN_HIT > MarsTable.SLASH_HIT && MarsTable.TITAN_AFTER < MarsTable.TITAN_HIT);
        assertTrue(VettoTable.RELEASE_HIT > VettoTable.CLAW_HIT && VettoTable.CLAW_HIT > VettoTable.FLURRY_HIT * 2);
        assertTrue(ChainTable.PREY > 1f && HeathTable.FLUSTER_BONUS > 1f && MarsTable.STAGGER_BONUS > 1f && VettoTable.SHIFT_BONUS > 1f);
        assertEquals(1, HeathTable.strikesPerWave(0) - 5, "six icicles a wave at the opening");
        assertTrue(HeathTable.strikesPerWave(4) > HeathTable.strikesPerWave(0));
        assertEquals(3, HeathTable.volley(1));
        assertEquals(6, HeathTable.volley(4));
        assertEquals(3, MarsTable.harpe(1));
        assertEquals(6, MarsTable.harpe(4));
        assertEquals(3, MarsTable.doubles(0));
        assertEquals(4, MarsTable.doubles(3));
        assertEquals(3, MarsTable.puppets(false));
        assertEquals(4, MarsTable.puppets(true));
        assertEquals(2, LotusTable.puppets(1));
        assertEquals(5, LotusTable.puppets(4));
        assertEquals(3, ChainTable.lashes(false));
        assertEquals(4, ChainTable.lashes(true));
        assertEquals(2, ChainTable.pillarMarks(1));
        assertEquals(5, ChainTable.pillarMarks(4));
        assertEquals(6, ChainTable.pillarMarks(9));
        assertTrue(ChainTable.lockdownTargets(true) > ChainTable.lockdownTargets(false));
        assertEquals(2, VettoTable.clawMarks(1));
        assertEquals(5, VettoTable.clawMarks(9));
        assertEquals(234.0, HeathTable.shieldPool(1300), 1e-9);
        assertEquals(112.0, MarsTable.stoneDamage(1600), 1e-9);
    }

    /** Plays a fight: a decision every 5 ticks, the target wandering between 0 and 38 blocks. Returns how often each move was used. */
    static Map<String, Integer> play(List<MoveRow> rows, long seed, int power, int ticks) {
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
            int idx = sch.pick(rows, s);
            if (idx < 0) continue;
            MoveRow m = rows.get(idx);
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
    void atTheOpeningOnlyTheOpeningMovesAreUsedAndNeverAnUltimate() {
        for (Map.Entry<String, List<MoveRow>> e : TABLES.entrySet()) {
            for (long seed = 1; seed <= 4; seed++) {
                Map<String, Integer> used = play(e.getValue(), seed, 20, 20000);
                for (String id : used.keySet()) assertTrue(MoveRow.find(e.getValue(), id).minPower() <= 20, e.getKey() + ": " + id + " is not an opening move");
                assertTrue(used.size() >= 4, e.getKey() + " has variety at 20%: " + used.keySet());
            }
        }
    }

    @Test
    void theUltimateWaitsForItsPowerAndComesAfterTheOpeningDelay() {
        for (Map.Entry<String, List<MoveRow>> e : TABLES.entrySet()) {
            MoveRow ult = e.getValue().stream().filter(MoveRow::ultimate).findFirst().orElseThrow();
            for (long seed = 1; seed <= 4; seed++) {
                assertNull(play(e.getValue(), seed, ult.minPower() - 1, 30000).get(ult.id()), e.getKey() + " keeps the ultimate for " + ult.minPower() + "%");
                assertNotNull(play(e.getValue(), seed, Math.max(ult.minPower(), 60), 30000).get(ult.id()), e.getKey() + " uses the ultimate once it may");
            }
            MoveScheduler sch = new MoveScheduler(3);
            sch.reset(0);
            MoveScheduler.Situation early = new MoveScheduler.Situation(10, 0.3f, 100, 1, MoveScheduler.FIRST_ULTIMATE_DELAY - 1, Double.POSITIVE_INFINITY, 900);
            assertFalse(sch.eligible(ult, early), e.getKey() + ": not in the first twenty seconds");
        }
    }

    @Test
    void atTheGatesEveryMoveGetsUsedAndUltimatesAreSpaced() {
        for (Map.Entry<String, List<MoveRow>> e : TABLES.entrySet()) {
            for (long seed = 1; seed <= 2; seed++) {
                Map<String, Integer> used = play(e.getValue(), seed, 100, 90000);
                for (MoveRow r : e.getValue()) assertNotNull(used.get(r.id()), e.getKey() + ": " + r.id() + " is used at the Gates (seed " + seed + ")");
                MoveRow ult = e.getValue().stream().filter(MoveRow::ultimate).findFirst().orElseThrow();
                int n = used.get(ult.id());
                int gap = PowerCurve.tuning(100).ultimateGapTicks();
                assertTrue(n >= 3, e.getKey() + ": several ultimates in a long fight");
                assertTrue((long) n * gap <= 90000L + gap, e.getKey() + ": never faster than the gap");
            }
        }
    }
}
