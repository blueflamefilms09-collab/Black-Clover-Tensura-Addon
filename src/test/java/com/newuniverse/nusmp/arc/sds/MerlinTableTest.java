package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.sds.kit.MoveRow;
import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Merlin's move table, and a played-out fight through the framework's scheduler: what she does at each power and distance. */
class MerlinTableTest {
    static final List<MoveRow> ROWS = MerlinTable.rows();

    @Test
    void theTableIsWellFormed() {
        assertEquals(12, ROWS.size());
        Set<String> ids = new HashSet<>();
        for (MoveRow r : ROWS) {
            assertTrue(ids.add(r.id()), "unique id " + r.id());
            assertFalse(r.label().isBlank());
            assertTrue(r.minPower() >= 20 && r.minPower() <= 80, r.id() + " is available before the Gates");
            assertTrue(r.windUpTicks() > 0, r.id() + " has a telegraph");
            assertTrue(Math.round(r.windUpTicks() * 0.75f) >= 8, r.id() + " keeps 8 ticks of tell even at the Gates");
            assertTrue(r.cooldownTicks() >= 80 && r.weight() >= 1);
            assertTrue(r.minRange() >= 0 && r.maxRange() > r.minRange());
            assertTrue(r.recovery() >= 6, "a beat to breathe after every move");
        }
        assertEquals(MerlinTable.CHRONO_COFFIN, ROWS.get(11).id(), "the first twelve moves keep their places (the client reads names by index)");
        assertEquals(MerlinTable.ARCANE_VOLLEY, ROWS.get(0).id());
        assertNull(MerlinTable.byId("nope"));
        assertSame(ROWS.get(3), MerlinTable.byId(MerlinTable.ABSOLUTE_CANCEL));
    }

    @Test
    void infinityMeansNoMoveEverCostsMana() {
        for (MoveRow r : ROWS) assertEquals(0.0, r.manaCost(), 1e-9, r.id() + " is free: her magic never runs dry");
    }

    @Test
    void theMixIsBoltsCloseWorkMobilityCountersAndOneUltimate() {
        long ults = ROWS.stream().filter(MoveRow::ultimate).count();
        assertEquals(1, ults);
        MoveRow coffin = MerlinTable.byId(MerlinTable.CHRONO_COFFIN);
        assertTrue(coffin.ultimate() && coffin.minPower() >= 60);
        assertTrue(coffin.windUpTicks() >= 60, "the longest ritual she has: the time to break her focus");
        for (MoveRow r : ROWS) assertTrue(r == coffin || r.windUpTicks() < coffin.windUpTicks(), r.id() + " is quicker than the Coffin");
        assertTrue(coffin.recovery() >= 40, "exposed afterwards");
        assertTrue(coffin.cooldownTicks() >= PowerCurve.tuning(100).ultimateGapTicks());
        assertTrue(ROWS.stream().anyMatch(r -> r.maxRange() <= 8), "close work");
        assertTrue(ROWS.stream().anyMatch(r -> r.minRange() >= 5 && r.maxRange() >= 30), "ranged");
        assertTrue(ROWS.stream().anyMatch(MoveRow::crowd), "crowd moves");
        for (String id : new String[]{MerlinTable.ABSOLUTE_CANCEL, MerlinTable.PERFECT_CUBE, MerlinTable.INFINITY_FIELD, MerlinTable.TRANSFER, MerlinTable.MAGIC_SEAL})
            assertNotNull(MerlinTable.byId(id), id);
    }

    @Test
    void thereIsNoDeadRangeAtTheOpening() {
        for (double d = 0; d <= 34; d += 0.5) {
            double dist = d;
            assertTrue(ROWS.stream().anyMatch(r -> !r.ultimate() && r.minPower() <= 20 && dist >= r.minRange() && dist <= r.maxRange()
                            && (r.id().equals(MerlinTable.ARCANE_VOLLEY) || r.id().equals(MerlinTable.ALDAN_STRIKE))),
                    "an attack to throw at " + d + " blocks at 20%");
        }
    }

    @Test
    void theNumbersAreSane() {
        assertTrue(MerlinTable.ALDAN > MerlinTable.BOLT * 2, "the orb is the heavy close blow");
        assertTrue(MerlinTable.COFFIN_COLLAPSE > MerlinTable.ALDAN, "the ultimate hits hardest");
        assertEquals(3, MerlinTable.boltCount(1, false));
        assertEquals(6, MerlinTable.boltCount(4, false));
        assertEquals(8, MerlinTable.boltCount(4, true));
        assertEquals(4, MerlinTable.starCount(1));
        assertEquals(7, MerlinTable.starCount(4));
        assertEquals(2.5f, MerlinTable.fieldTick(0), 1e-6f);
        assertEquals(MerlinTable.FIELD_TICK_MAX, MerlinTable.fieldTick(100), 1e-6f, "a field's tick has a ceiling");
        assertTrue(MerlinTable.fieldTick(3) > MerlinTable.fieldTick(1), "staying in a field hurts more");
        assertEquals(5.0, MerlinTable.coffinHalf(0), 1e-9);
        assertEquals(7.0, MerlinTable.coffinHalf(4), 1e-9);
        assertEquals(5.0, MerlinTable.coffinHalf(-3), 1e-9);
        assertTrue(MerlinTable.CUBE_CHARGES_GATES > MerlinTable.CUBE_CHARGES && MerlinTable.CANCEL_CHARGES_GATES > MerlinTable.CANCEL_CHARGES);
        assertTrue(MerlinTable.FIELD_CAP_GATES > MerlinTable.FIELD_CAP);
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
            MoveScheduler.Situation s = new MoveScheduler.Situation(dist, 0.6f, power, 1, t, 0.0, tu.ultimateGapTicks());
            int idx = sch.pick(ROWS, s);
            if (idx < 0) continue;
            MoveRow m = ROWS.get(idx);
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
    void withNoManaAtAllTheScheduleStillRunsBecauseNothingCostsAnything() {
        for (long seed = 1; seed <= 4; seed++) {
            Map<String, Integer> used = play(seed, 20, 20000);
            assertTrue(used.size() >= 5, "variety at 20% with an empty mana pool: " + used.keySet());
        }
    }

    @Test
    void atTheOpeningOnlyOpeningMovesAndNeverTheCoffin() {
        for (long seed = 1; seed <= 5; seed++) {
            Map<String, Integer> used = play(seed, 20, 20000);
            for (String id : used.keySet()) assertTrue(MerlinTable.byId(id).minPower() <= 20, id + " is not an opening move");
            assertNull(used.get(MerlinTable.CHRONO_COFFIN));
            assertNull(used.get(MerlinTable.PERFECT_CUBE), "the cube waits for 40%");
        }
    }

    @Test
    void theCoffinNeedsSixtyPercentAndComesAfterTheOpeningDelay() {
        for (long seed = 1; seed <= 5; seed++) {
            assertNull(play(seed, 59, 20000).get(MerlinTable.CHRONO_COFFIN));
            assertNotNull(play(seed, 60, 20000).get(MerlinTable.CHRONO_COFFIN), "she uses it at 60%");
        }
        MoveScheduler sch = new MoveScheduler(3);
        sch.reset(0);
        MoveScheduler.Situation early = new MoveScheduler.Situation(10, 0.3f, 80, 1, MoveScheduler.FIRST_ULTIMATE_DELAY - 1, 0.0, 1200);
        assertFalse(sch.eligible(MerlinTable.byId(MerlinTable.CHRONO_COFFIN), early));
    }

    @Test
    void atTheGatesEveryMoveGetsUsedAndTheCoffinIsSpaced() {
        for (long seed = 1; seed <= 3; seed++) {
            Map<String, Integer> used = play(seed, 100, 60000);
            for (MoveRow r : ROWS) assertNotNull(used.get(r.id()), r.id() + " is used at the Gates (seed " + seed + ")");
            int ults = used.get(MerlinTable.CHRONO_COFFIN);
            assertTrue(ults >= 3, "several coffins in a long fight");
            assertTrue(ults * PowerCurve.tuning(100).ultimateGapTicks() <= 60000 + PowerCurve.tuning(100).ultimateGapTicks(), "never faster than the gap");
        }
    }

    @Test
    void closeUpSheUsesTheOrbAndTransferAndNeverTheVolley() {
        MoveScheduler sch = new MoveScheduler(11);
        sch.reset(0);
        Map<String, Integer> used = new HashMap<>();
        PowerCurve.Tuning tu = PowerCurve.tuning(20);
        for (long t = 20; t < 8000; t += 5) {
            MoveScheduler.Situation s = new MoveScheduler.Situation(3.0, 0.9f, 20, 1, t, 0.0, tu.ultimateGapTicks());
            int idx = sch.pick(ROWS, s);
            if (idx < 0) continue;
            sch.commit(ROWS.get(idx), s, ROWS.get(idx).cooldownTicks());
            used.merge(ROWS.get(idx).id(), 1, Integer::sum);
        }
        assertNotNull(used.get(MerlinTable.ALDAN_STRIKE));
        assertNotNull(used.get(MerlinTable.TRANSFER), "when the orb is on cool-down she steps away instead");
        assertNull(used.get(MerlinTable.ARCANE_VOLLEY), "the volley needs six blocks");
    }
}
