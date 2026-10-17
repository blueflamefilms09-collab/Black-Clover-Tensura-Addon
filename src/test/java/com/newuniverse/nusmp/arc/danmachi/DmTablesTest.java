package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a boss whose table has no attack at some distance stands still, and one whose ultimate comes too soon or without a warning is unfair, so the four tables are
 * checked as data (well formed, a real mix, an attack at every distance, a telegraph of 16 ticks at the opening and still 12 at the Gates, 56 and 42 for an ultimate) and played out through the real scheduler.
 */
class DmTablesTest {
    private static Map<String, List<DmSpec>> tables() {
        Map<String, List<DmSpec>> m = new LinkedHashMap<>();
        m.put("minotaur", MinotaurTable.specs());
        m.put("black_goliath", BlackGoliathTable.specs());
        m.put("amphisbaena", AmphisbaenaTable.specs());
        m.put("udaeus", UdaeusTable.specs());
        return m;
    }

    private static boolean attacks(DmSpec s) { return !s.ultimate() && !s.leveled() && s.damage() > 0; }

    @Test
    void everyTableIsWellFormed() {
        for (Map.Entry<String, List<DmSpec>> e : tables().entrySet()) {
            String who = e.getKey();
            List<DmSpec> specs = e.getValue();
            assertTrue(specs.size() >= 8 && specs.size() <= 14, who + " has 8 to 14 moves: " + specs.size());
            Set<String> ids = new HashSet<>();
            for (DmSpec s : specs) {
                assertTrue(ids.add(s.id()), who + ": unique id " + s.id());
                assertTrue(s.id().matches("[a-z][a-z_]*"), who + ": snake case " + s.id());
                assertFalse(s.label().isBlank());
                assertTrue(s.minPower() == 20 || s.minPower() == 40 || s.minPower() == 60 || s.minPower() == 80 || s.minPower() == 100, who + ": a power step for " + s.id());
                assertTrue(s.windUpTicks() >= (s.ultimate() ? 56 : 16), who + ": " + s.id() + " has a real telegraph");
                assertTrue(Math.round(s.windUpTicks() * 0.75f) >= (s.ultimate() ? 42 : 12), who + ": " + s.id() + " keeps its telegraph at the Gates");
                assertTrue(s.cooldownTicks() >= 60, who + ": " + s.id() + " has a cooldown");
                assertTrue(s.weight() >= 1);
                assertTrue(s.minRange() >= 0 && s.maxRange() > s.minRange(), who + ": " + s.id() + " has a range band");
                assertTrue(s.recovery() >= 8, who + ": a beat to breathe after " + s.id());
                assertTrue(s.manaCost() > 0);
                assertTrue(s.damage() >= 0 && s.damage() <= 30, who + ": " + s.id() + " damage " + s.damage());
            }
            assertNull(tablesById(who, "nope"));
        }
    }

    private static DmSpec tablesById(String who, String id) {
        return switch (who) {
            case "minotaur" -> MinotaurTable.byId(id);
            case "black_goliath" -> BlackGoliathTable.byId(id);
            case "amphisbaena" -> AmphisbaenaTable.byId(id);
            default -> UdaeusTable.byId(id);
        };
    }

    @Test
    void oneUltimateThatIsRareAndLeavesTheBossExposed() {
        for (Map.Entry<String, List<DmSpec>> e : tables().entrySet()) {
            List<DmSpec> ults = e.getValue().stream().filter(DmSpec::ultimate).toList();
            assertEquals(1, ults.size(), e.getKey() + " has exactly one ultimate");
            DmSpec u = ults.get(0);
            assertTrue(u.minPower() >= 60, e.getKey() + ": only when its mana is unbound");
            assertTrue(u.recovery() >= 40, e.getKey() + ": exposed afterwards");
            assertTrue(u.cooldownTicks() >= PowerCurve.tuning(100).ultimateGapTicks(), e.getKey() + ": never faster than the framework's gap");
            assertTrue(u.crowd());
            assertEquals(60.0, u.manaCost(), 1e-9);
        }
    }

    @Test
    void theLeveledMovesComeLastAndOnlyAtTheGates() {
        for (Map.Entry<String, List<DmSpec>> e : tables().entrySet()) {
            List<DmSpec> specs = e.getValue();
            boolean seen = false;
            int leveled = 0;
            for (DmSpec s : specs) {
                if (s.leveled()) {
                    seen = true;
                    leveled++;
                    assertEquals(100, s.minPower(), e.getKey() + ": " + s.id() + " belongs to the Gates");
                } else assertFalse(seen, e.getKey() + ": " + s.id() + " is after a leveled move (rows are only appended)");
            }
            assertEquals(1, leveled, e.getKey() + " has one leveled move");
        }
    }

    @Test
    void everyBossHasMeleeRangedCrowdAndCustomMoves() {
        for (Map.Entry<String, List<DmSpec>> e : tables().entrySet()) {
            List<DmSpec> specs = e.getValue();
            assertTrue(specs.stream().anyMatch(s -> attacks(s) && s.maxRange() <= 10), e.getKey() + ": melee");
            assertTrue(specs.stream().anyMatch(s -> attacks(s) && s.minRange() >= 4 && s.maxRange() >= 26), e.getKey() + ": ranged");
            assertTrue(specs.stream().anyMatch(s -> s.crowd() && !s.ultimate()), e.getKey() + ": crowd");
            assertTrue(specs.stream().anyMatch(s -> s.shape() == DmSpec.Shape.CUSTOM && !s.ultimate()), e.getKey() + ": its own moves");
            Set<DmSpec.Shape> shapes = new HashSet<>();
            for (DmSpec s : specs) shapes.add(s.shape());
            assertTrue(shapes.size() >= 4, e.getKey() + " draws at least four different shapes: " + shapes);
        }
    }

    @Test
    void thereIsAlwaysSomethingToThrowAtTheOpening() {
        for (Map.Entry<String, List<DmSpec>> e : tables().entrySet()) {
            for (double d = 0; d <= 28; d += 0.5) {
                double dist = d;
                assertTrue(e.getValue().stream().anyMatch(s -> attacks(s) && s.minPower() <= 20 && dist >= s.minRange() && dist <= s.maxRange()),
                        e.getKey() + ": an attack at " + d + " blocks at 20%");
            }
        }
    }

    @Test
    void shapeNumbersAreSane() {
        for (Map.Entry<String, List<DmSpec>> e : tables().entrySet()) {
            for (DmSpec s : e.getValue()) {
                String what = e.getKey() + "/" + s.id();
                switch (s.shape()) {
                    case CONE -> assertTrue(s.reach() >= 3 && s.reach() <= 12 && s.width() > 10 && s.width() <= 110 && s.damage() > 0 && s.maxRange() <= s.reach() + 3, what);
                    case SPIN -> assertTrue(s.reach() >= 3 && s.reach() <= 12 && s.damage() > 0 && s.maxRange() <= s.reach() + 3, what);
                    case WAVE -> assertTrue(s.reach() >= 6 && s.reach() <= 16 && s.damage() > 0 && s.count() >= 1 && s.count() <= 3 && (s.count() == 1 || s.extra() >= 8), what);
                    case LANE -> assertTrue(s.reach() >= 8 && s.reach() <= 36 && s.width() >= 1.5 && s.width() <= 4.5 && s.damage() > 0 && s.count() >= 1 && s.count() <= 3, what);
                    case MARKS -> assertTrue(s.reach() >= 2 && s.reach() <= 4 && s.damage() > 0 && s.count() >= 1 && s.count() <= 4 && s.extra() >= 0 && s.extra() <= 30, what);
                    case CHARGE -> assertTrue(s.reach() >= 10 && s.reach() <= 30 && s.width() >= 2.5 && s.width() <= 5 && s.damage() > 0 && s.extra() >= 0.8 && s.extra() <= 2.0, what);
                    case LEAP -> assertTrue(s.reach() >= 5 && s.reach() <= 10 && s.damage() > 0 && s.extra() >= 6 && s.extra() <= 14 && s.minRange() >= 6, what);
                    case VOLLEY -> assertTrue(s.count() >= 2 && s.count() <= 6 && s.width() >= 10 && s.width() <= 90 && s.damage() > 0 && s.extra() >= 0.8 && s.reach() >= 16, what);
                    case SPOKES -> assertTrue(s.count() >= 3 && s.count() <= 5 && s.reach() >= 10 && s.width() >= 1.5 && s.width() <= 3.5 && s.damage() > 0, what);
                    case BREATH -> assertTrue(s.reach() >= 8 && s.width() >= 15 && s.width() <= 60 && s.count() >= 4 && s.damage() > 0 && s.extra() >= 0, what);
                    case CUSTOM -> assertTrue(s.reach() >= 0, what);
                }
            }
        }
    }

    @Test
    void theNumberOfMarksAndSpokesGrowsWithTheVolleySizeButIsCapped() {
        DmSpec marks = UdaeusTable.byId(UdaeusTable.OSSUARY_PILLARS);
        assertEquals(3, marks.scaledCount(1));
        assertEquals(4, marks.scaledCount(2));
        assertEquals(6, marks.scaledCount(4));
        assertEquals(8, marks.scaledCount(40), "never more than eight");
        assertEquals(3, marks.scaledCount(0), "a volley size below 1 counts as 1");
        DmSpec volley = UdaeusTable.byId(UdaeusTable.RIB_VOLLEY);
        assertEquals(3, volley.scaledCount(1));
        assertEquals(9, volley.scaledCount(4));
        assertEquals(12, volley.scaledCount(40));
        DmSpec spokes = BlackGoliathTable.byId(BlackGoliathTable.FISSURE_SPOKES);
        assertEquals(3, spokes.scaledCount(1));
        assertEquals(6, spokes.scaledCount(4));
        assertEquals(9, spokes.scaledCount(40));
        DmSpec cone = MinotaurTable.byId(MinotaurTable.HORN_GORE);
        assertEquals(cone.count(), cone.scaledCount(4), "other shapes do not scale");
        // a full set of spokes at the Gates must still leave a gap to stand in 10 blocks out
        assertTrue(DmGeometry.spokeGap(spokes.scaledCount(4), 10, spokes.width()) >= 4.0, "gaps of " + DmGeometry.spokeGap(spokes.scaledCount(4), 10, spokes.width()));
    }

    @Test
    void theManaRuleMatchesTheFramework() {
        for (List<DmSpec> specs : tables().values()) {
            for (DmSpec s : specs) {
                if (s.ultimate()) assertEquals(60.0, s.manaCost(), 1e-9);
                else assertEquals(Math.min(32, 6 + s.cooldownTicks() / 16.0), s.manaCost(), 1e-9);
            }
        }
    }

    // ================================================================ a played fight
    /** Plays a fight: a decision every 5 ticks, the target wandering between 2 and 36 blocks. Returns how often each move was used. */
    static Map<String, Integer> play(List<DmSpec> specs, long seed, int power, int ticks) {
        MoveScheduler sch = new MoveScheduler(seed);
        sch.reset(0);
        Map<String, Integer> used = new HashMap<>();
        PowerCurve.Tuning tu = PowerCurve.tuning(power);
        List<DmSpec> pool = specs.stream().filter(s -> !s.leveled()).toList();
        long busyUntil = 0;
        String prev = "";
        int run = 0;
        for (long t = 20; t < ticks; t += 5) {
            if (t < busyUntil) continue;
            double dist = 2 + 17 * (1 + Math.sin(t / 97.0)) + 2 * Math.sin(t / 13.0);
            MoveScheduler.Situation s = new MoveScheduler.Situation(dist, 0.6f, power, 2, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(pool, s);
            if (idx < 0) continue;
            DmSpec m = pool.get(idx);
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
    void atTheOpeningOnlyTheOpeningMovesAreUsedAndNeverTheUltimate() {
        for (Map.Entry<String, List<DmSpec>> e : tables().entrySet()) {
            for (long seed = 1; seed <= 4; seed++) {
                Map<String, Integer> used = play(e.getValue(), seed, 20, 20000);
                for (String id : used.keySet()) {
                    DmSpec s = e.getValue().stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
                    assertTrue(s.minPower() <= 20, e.getKey() + ": " + id + " is not an opening move");
                    assertFalse(s.ultimate());
                }
                assertTrue(used.size() >= 4, e.getKey() + ": variety at 20%: " + used.keySet());
            }
        }
    }

    @Test
    void theUltimateNeedsSixtyPercentAndComesAfterTheOpeningDelay() {
        for (Map.Entry<String, List<DmSpec>> e : tables().entrySet()) {
            DmSpec ult = e.getValue().stream().filter(DmSpec::ultimate).findFirst().orElseThrow();
            for (long seed = 1; seed <= 4; seed++) {
                assertNull(play(e.getValue(), seed, 59, 20000).get(ult.id()), e.getKey() + ": not under 60%");
                assertNotNull(play(e.getValue(), seed, 60, 20000).get(ult.id()), e.getKey() + ": used at 60%");
            }
            MoveScheduler sch = new MoveScheduler(3);
            sch.reset(0);
            MoveScheduler.Situation early = new MoveScheduler.Situation(10, 0.3f, 80, 1, MoveScheduler.FIRST_ULTIMATE_DELAY - 1, Double.POSITIVE_INFINITY, 1200);
            assertFalse(sch.eligible(ult, early), e.getKey() + ": not in the first twenty seconds");
        }
    }

    @Test
    void atTheGatesEveryOrdinaryMoveGetsUsedAndUltimatesAreSpaced() {
        for (Map.Entry<String, List<DmSpec>> e : tables().entrySet()) {
            for (long seed = 1; seed <= 3; seed++) {
                Map<String, Integer> used = play(e.getValue(), seed, 100, 60000);
                for (DmSpec s : e.getValue()) if (!s.leveled()) assertNotNull(used.get(s.id()), e.getKey() + ": " + s.id() + " is used at the Gates (seed " + seed + ")");
                DmSpec ult = e.getValue().stream().filter(DmSpec::ultimate).findFirst().orElseThrow();
                int ults = used.get(ult.id());
                assertTrue(ults >= 3, e.getKey() + ": several ultimates in a long fight");
                assertTrue((long) ults * PowerCurve.tuning(100).ultimateGapTicks() <= 60000 + PowerCurve.tuning(100).ultimateGapTicks(), e.getKey() + ": never faster than the gap");
            }
        }
    }

    @Test
    void theSignatureMovesAreTheBreadAndButterWhereTheyBelong() {
        // up close the Minotaur gores and hammers; far away it charges
        Map<String, Integer> close = new HashMap<>(), far = new HashMap<>();
        for (int round = 0; round < 2; round++) {
            Map<String, Integer> target = round == 0 ? close : far;
            double dist = round == 0 ? 3.0 : 20.0;
            MoveScheduler sch = new MoveScheduler(11);
            sch.reset(0);
            PowerCurve.Tuning tu = PowerCurve.tuning(40);
            for (long t = 20; t < 5000; t += 5) {
                MoveScheduler.Situation s = new MoveScheduler.Situation(dist, 0.8f, 40, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
                int idx = sch.pick(MinotaurTable.specs(), s);
                if (idx < 0) continue;
                DmSpec m = MinotaurTable.specs().get(idx);
                sch.commit(m, s, m.cooldownTicks());
                target.merge(m.id(), 1, Integer::sum);
            }
        }
        assertTrue(close.getOrDefault(MinotaurTable.HORN_GORE, 0) > 0, "gore up close: " + close);
        assertNull(close.get(MinotaurTable.BULL_CHARGE), "no charge at 3 blocks");
        assertTrue(far.getOrDefault(MinotaurTable.BULL_CHARGE, 0) > 0, "charge from afar: " + far);
        assertNull(far.get(MinotaurTable.HORN_GORE), "no gore at 20 blocks");
    }

    @Test
    void theRedHeadMovesAreTheOnesThatNeedTheRedHead() {
        assertTrue(AmphisbaenaTable.needsRedHead(AmphisbaenaTable.RED_MIST_BREATH));
        assertFalse(AmphisbaenaTable.needsRedHead(AmphisbaenaTable.TWIN_CALAMITY), "the ultimate always exists: without the red head it is the flame alone");
        assertFalse(AmphisbaenaTable.needsRedHead(AmphisbaenaTable.BLUE_FLAME_BREATH));
        assertFalse(AmphisbaenaTable.needsRedHead(null));
        for (DmSpec s : AmphisbaenaTable.specs()) if (AmphisbaenaTable.needsRedHead(s.id())) assertNotNull(AmphisbaenaTable.byId(s.id()));
        // without them the Amphisbaena still has an attack at every distance up to 28 blocks at the opening
        for (double d = 0; d <= 28; d += 0.5) {
            double dist = d;
            assertTrue(AmphisbaenaTable.specs().stream().anyMatch(s -> attacks(s) && !AmphisbaenaTable.needsRedHead(s.id()) && s.minPower() <= 20
                    && s.damage() > 0 && dist >= s.minRange() && dist <= s.maxRange()), "an attack with no red head at " + d);
        }
    }
}
