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

/** Why: Gowther's moves must read in time and never repeat, so the table is played out through the framework's scheduler at every power: what he does at each distance, when the Nightmare may come, and that every move gets used. */
class GowtherTableTest {
    static final List<MoveRow> ROWS = GowtherTable.rows();

    @Test
    void theTableIsWellFormed() {
        assertEquals(10, ROWS.size());
        Set<String> ids = new HashSet<>();
        for (MoveRow r : ROWS) {
            assertTrue(ids.add(r.id()), "unique id " + r.id());
            assertFalse(r.label().isBlank());
            assertTrue(r.minPower() >= 20 && r.minPower() <= 80, r.id() + " is available before the Gates");
            assertTrue(r.windUpTicks() > 0, r.id() + " has a telegraph");
            assertTrue(Math.round(r.windUpTicks() * 0.75f) >= 8, r.id() + " keeps 8 ticks of tell even at the Gates");
            assertTrue(r.cooldownTicks() >= 70 && r.weight() >= 1);
            assertTrue(r.minRange() >= 0 && r.maxRange() > r.minRange());
            assertTrue(r.recovery() >= 6, "a beat to breathe after every move");
            assertTrue(r.manaCost() > 0, "he is a doll, not an Infinity: his moves cost mana like the framework's");
        }
        assertEquals(GowtherTable.HERRITT_VOLLEY, ROWS.get(0).id(), "the first moves keep their places (the client reads names by index)");
        assertEquals(GowtherTable.NIGHTMARE_TELLER, ROWS.get(5).id());
        assertNull(GowtherTable.byId("nope"));
        assertSame(ROWS.get(2), GowtherTable.byId(GowtherTable.PHANTOM_GARDEN));
    }

    @Test
    void theMixIsBowsIllusionsMemoryReadingControlAndOneUltimate() {
        assertEquals(1, ROWS.stream().filter(MoveRow::ultimate).count());
        MoveRow ult = GowtherTable.byId(GowtherTable.NIGHTMARE_TELLER);
        assertTrue(ult.ultimate() && ult.minPower() >= 60);
        assertTrue(ult.windUpTicks() >= 56, "an ultimate is at least a second and a half, here a ritual");
        assertTrue(ult.recovery() >= 40, "exposed afterwards");
        assertTrue(ult.cooldownTicks() >= PowerCurve.tuning(100).ultimateGapTicks());
        assertTrue(ROWS.stream().anyMatch(r -> r.maxRange() <= 8), "close work");
        assertTrue(ROWS.stream().anyMatch(r -> r.minRange() >= 5 && r.maxRange() >= 30), "ranged");
        assertTrue(ROWS.stream().anyMatch(MoveRow::crowd), "crowd moves");
        for (String id : new String[]{GowtherTable.PHANTOM_GARDEN, GowtherTable.ECHO_OF_MEMORY, GowtherTable.PRYING_EYES, GowtherTable.MARIONETTE,
                GowtherTable.REWRITE_LIGHT, GowtherTable.HERRITT_CROSS}) assertNotNull(GowtherTable.byId(id), id);
        assertEquals(20, GowtherTable.byId(GowtherTable.PRYING_EYES).minPower(), "the reading is the first thing he does to a one-trick party");
    }

    @Test
    void thereIsNoDeadRangeAtTheOpening() {
        for (double d = 0; d <= 34; d += 0.5) {
            double dist = d;
            assertTrue(ROWS.stream().anyMatch(r -> !r.ultimate() && r.minPower() <= 20 && dist >= r.minRange() && dist <= r.maxRange()
                            && (r.id().equals(GowtherTable.HERRITT_VOLLEY) || r.id().equals(GowtherTable.PUPPET_RUSH) || r.id().equals(GowtherTable.PHANTOM_GARDEN))),
                    "an attack to throw at " + d + " blocks at 20%");
        }
    }

    @Test
    void theNumbersAreSane() {
        assertTrue(GowtherTable.CROSS_ARROW > GowtherTable.ARROW, "a locked arrow hurts more than a loose one");
        assertTrue(GowtherTable.GARDEN_BLAST > GowtherTable.RUSH, "a real ring is the heavier hit");
        assertTrue(GowtherTable.STRING_STRIKE >= GowtherTable.RUSH);
        assertEquals(2, GowtherTable.arrowCount(1));
        assertEquals(8, GowtherTable.arrowCount(4));
        assertEquals(8, GowtherTable.arrowCount(40), "the volley is capped");
        assertEquals(2, GowtherTable.realPerFoe(1));
        assertEquals(5, GowtherTable.realPerFoe(4));
        assertEquals(2, GowtherTable.falsePerReal());
        assertTrue(GowtherTable.echoAges(true).length > GowtherTable.echoAges(false).length);
        assertTrue(GowtherTable.echoAges(false)[0] >= 20 && GowtherTable.echoAges(false)[2] <= GowtherTable.TRAIL_TICKS, "the trail remembers what the echoes ask for");
        assertTrue(GowtherTable.echoAges(true)[4] <= GowtherTable.TRAIL_TICKS);
        assertTrue(GowtherTable.readTicks(true) > GowtherTable.readTicks(false));
        assertTrue(GowtherTable.readCut(true) > GowtherTable.readCut(false));
        assertTrue(GowtherTable.readCut(true) < 0.9f, "even the Gates never make a kind harmless");
        assertTrue(GowtherTable.nightmareWaves() >= 4 && GowtherTable.nightmareWaves() * GowtherTable.NIGHTMARE_WAVE <= GowtherTable.NIGHTMARE_TICKS);
        assertTrue(GowtherTable.NIGHTMARE_FALL >= 12 && GowtherTable.NIGHTMARE_FALL < GowtherTable.NIGHTMARE_WAVE, "each wave's marks show long enough to read and fall before the next wave");
        assertTrue(GowtherTable.READ_SPAN >= GowtherTable.READ_TICKS - 100);
    }

    static Map<String, Integer> play(long seed, int power, int ticks, double mana) {
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
            MoveScheduler.Situation s = new MoveScheduler.Situation(dist, 0.6f, power, 1, t, mana, tu.ultimateGapTicks());
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
    void atTheOpeningOnlyOpeningMovesAndNeverTheNightmare() {
        for (long seed = 1; seed <= 5; seed++) {
            Map<String, Integer> used = play(seed, 20, 20000, Double.POSITIVE_INFINITY);
            for (String id : used.keySet()) assertTrue(GowtherTable.byId(id).minPower() <= 20, id + " is not an opening move");
            assertNull(used.get(GowtherTable.NIGHTMARE_TELLER));
            assertNull(used.get(GowtherTable.ECHO_OF_MEMORY), "the echoes wait for 40%");
            assertTrue(used.size() >= 5, "variety at 20%: " + used.keySet());
        }
    }

    @Test
    void theNightmareNeedsSixtyPercentAndComesAfterTheOpeningDelay() {
        for (long seed = 1; seed <= 5; seed++) {
            assertNull(play(seed, 59, 20000, Double.POSITIVE_INFINITY).get(GowtherTable.NIGHTMARE_TELLER));
            assertNotNull(play(seed, 60, 20000, Double.POSITIVE_INFINITY).get(GowtherTable.NIGHTMARE_TELLER), "he uses it at 60%");
        }
        MoveScheduler sch = new MoveScheduler(3);
        sch.reset(0);
        MoveScheduler.Situation early = new MoveScheduler.Situation(10, 0.3f, 80, 1, MoveScheduler.FIRST_ULTIMATE_DELAY - 1, Double.POSITIVE_INFINITY, 1200);
        assertFalse(sch.eligible(GowtherTable.byId(GowtherTable.NIGHTMARE_TELLER), early));
    }

    @Test
    void atTheGatesEveryMoveGetsUsedAndTheNightmareIsSpaced() {
        for (long seed = 1; seed <= 3; seed++) {
            Map<String, Integer> used = play(seed, 100, 60000, Double.POSITIVE_INFINITY);
            for (MoveRow r : ROWS) assertNotNull(used.get(r.id()), r.id() + " is used at the Gates (seed " + seed + ")");
            int ults = used.get(GowtherTable.NIGHTMARE_TELLER);
            assertTrue(ults >= 3);
            assertTrue(ults * PowerCurve.tuning(100).ultimateGapTicks() <= 60000 + PowerCurve.tuning(100).ultimateGapTicks(), "never faster than the gap");
        }
    }

    @Test
    void anEmptyPoolMeansNoMovesBecauseEveryMoveCostsMana() {
        Map<String, Integer> used = play(2, 20, 20000, 0.0);
        assertTrue(used.isEmpty(), "with an empty pool and no regeneration in this model nothing is affordable: unlike Merlin, he is not an Infinity");
    }

    @Test
    void theCostsAreWithinWhatTheFrameworkPoolHolds() {
        for (MoveRow r : ROWS) assertTrue(r.manaCost() <= 60, r.id() + " costs at most an ultimate's 60");
    }
}
