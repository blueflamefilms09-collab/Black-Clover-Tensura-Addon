package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.sds.kit.MoveRow;
import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: King's fields are only fair if a garden is within a walk of every foe and his moves must read in time, so the table is checked and a whole fight is played through the framework's scheduler at every power. */
class KingTableTest {
    static final List<MoveRow> ROWS = KingTable.rows();

    @Test
    void theTableIsWellFormed() {
        assertEquals(10, ROWS.size());
        Set<String> ids = new HashSet<>();
        for (MoveRow r : ROWS) {
            assertTrue(ids.add(r.id()), "unique id " + r.id());
            assertFalse(r.label().isBlank());
            assertTrue(r.minPower() >= 20 && r.minPower() <= 60, r.id() + " is available before the Gates");
            assertTrue(r.windUpTicks() >= 16, r.id() + " has a telegraph of at least 16 ticks");
            assertTrue(Math.round(r.windUpTicks() * 0.75f) >= 12, r.id() + " keeps 12 ticks of tell even at the Gates");
            assertTrue(r.cooldownTicks() >= 110 || r.id().equals(KingTable.GUARDIAN_SLAM) || r.id().equals(KingTable.WING_GUST), r.id() + " has a cool-down");
            assertTrue(r.weight() >= 1);
            assertTrue(r.minRange() >= 0 && r.maxRange() > r.minRange());
            assertTrue(r.recovery() >= 8, "a beat to breathe after every move");
            assertTrue(r.manaCost() > 0, "his moves cost mana like the framework's");
        }
        assertEquals(KingTable.CHASTIEFOL_THROW, ROWS.get(0).id(), "the first moves keep their places (the client reads names by index)");
        assertEquals(KingTable.DISASTER, ROWS.get(9).id());
        assertNull(KingTable.byId("nope"));
        assertSame(ROWS.get(4), KingTable.byId(KingTable.POLLEN_GARDEN));
    }

    @Test
    void theMixIsThrowsRainsAirMovesFieldsAShellAndOneUltimate() {
        assertEquals(1, ROWS.stream().filter(MoveRow::ultimate).count());
        MoveRow ult = KingTable.byId(KingTable.DISASTER);
        assertTrue(ult.ultimate() && ult.minPower() >= 60);
        assertTrue(ult.windUpTicks() >= 56, "an ultimate is at least a second and a half, here a ritual");
        assertTrue(ult.recovery() >= 40, "exposed afterwards");
        assertTrue(ult.cooldownTicks() >= PowerCurve.tuning(100).ultimateGapTicks());
        assertTrue(KingTable.byId(KingTable.DIVE_STRIKE).recovery() - KingFlight.DIVE_MAX >= 40, "the dive leaves him exposed on the ground for 40 ticks even after the longest dive (the framework's exposed from 40 ticks of recovery, counted from the start of the dive)");
        assertTrue(ROWS.stream().anyMatch(r -> r.maxRange() <= 8), "close work");
        assertTrue(ROWS.stream().anyMatch(r -> r.minRange() >= 6 && r.maxRange() >= 36), "ranged");
        assertTrue(ROWS.stream().anyMatch(MoveRow::crowd), "crowd moves");
        assertEquals(20, KingTable.byId(KingTable.CHASTIEFOL_THROW).minPower());
        assertEquals(40, KingTable.byId(KingTable.GUARDIAN).minPower(), "the Guardian comes at 40%");
    }

    @Test
    void thereIsNoDeadRangeAtTheOpening() {
        for (double d = 0; d <= 38; d += 0.5) {
            double dist = d;
            assertTrue(ROWS.stream().anyMatch(r -> !r.ultimate() && r.minPower() <= 20 && dist >= r.minRange() && dist <= r.maxRange()), "an attack to throw at " + d + " blocks at 20%");
        }
    }

    @Test
    void theFieldsAreFairToReadAndToReach() {
        for (DisasterBeats.Spec s : new DisasterBeats.Spec[]{KingTable.POLLEN, KingTable.DISASTER_SPEC, KingTable.DISASTER_SPEC_GATES}) {
            assertTrue(s.valid(), "a beat gives at least 12 ticks of warning and ends before the next shows");
            // a player WALKING (0.216 blocks a tick, not even sprinting) who takes half a second (12 ticks) to react still makes the garden's edge within the warning
            double walk = 0.216 * (s.warn() - 12);
            double edge = KingTable.MAX_WALK - KingTable.GARDEN_RADIUS;
            assertTrue(walk >= edge, "the edge of a garden is " + edge + " blocks away at the most, a walking player makes " + walk);
        }
        assertEquals(3, KingTable.POLLEN.beats());
        assertTrue(KingTable.DISASTER_SPEC.total() <= 200 && KingTable.DISASTER_SPEC_GATES.total() <= 200, "no field lasts more than ten seconds");
        assertTrue(KingTable.DISASTER_SPEC_GATES.beats() > KingTable.DISASTER_SPEC.beats());
        assertTrue(KingTable.DISASTER_RADIUS > KingTable.POLLEN_RADIUS);
        assertTrue(KingTable.GARDEN_GAP > KingTable.GARDEN_RADIUS, "gardens do not overlap");
        assertEquals(3, KingTable.pollenGardens(1), "a Pollen Garden always has three gardens");
        assertEquals(6, KingTable.pollenGardens(4));
        assertEquals(2, KingTable.disasterGardens(1), "a Disaster always has two gardens");
        assertEquals(5, KingTable.disasterGardens(4));
        assertEquals(SafeGardens.MAX, KingTable.pollenGardens(40));
    }

    @Test
    void theNumbersAreSane() {
        assertTrue(KingTable.DISASTER_PULSE > KingTable.POLLEN_PULSE, "the ultimate's field hits harder");
        assertTrue(KingTable.disasterPulse(true) > KingTable.disasterPulse(false));
        assertTrue(KingTable.SLAM > KingTable.THROW && KingTable.DIVE > KingTable.THROW, "the heavy moves hit harder than the light one");
        assertEquals(4, KingTable.rainCount(1));
        assertEquals(7, KingTable.rainCount(4));
        assertEquals(8, KingTable.rainCount(40), "the rain is capped");
        assertEquals(6, KingTable.petalCount(false));
        assertEquals(8, KingTable.petalCount(true));
        assertEquals(1, KingTable.sunflowerVolleys(false));
        assertEquals(2, KingTable.sunflowerVolleys(true));
        assertEquals(KingTable.byId(KingTable.CHASTIEFOL_THROW).maxRange(), KingTable.THROW_REACH, 1e-9, "he throws the spear only as far as it reaches");
        assertTrue(KingTable.GUARDIAN_TICKS >= 100 && KingTable.GUARDIAN_TICKS <= 240);
        assertTrue(KingTable.ROOT_TICKS <= 60, "Fossilization is a short hold before the balance law's cap");
        assertTrue(KingTable.BREAK_FRACTION > 0.04f && KingTable.BREAK_FRACTION < 0.15f);
    }

    @Test
    void theSunflowerHasRoomToStandAtEveryPetalCount() {
        for (boolean gates : new boolean[]{false, true}) {
            int n = KingTable.petalCount(gates);
            double r = SunflowerMath.minSafeRadius(n, KingTable.PETAL_HALF, 0.8, 0.6);
            assertTrue(r < 5.0, n + " petals leave room to stand from " + r + " blocks out");
            assertTrue(r < KingTable.PETAL_LENGTH, "and that is well inside the petals' length");
        }
    }

    /** Plays a fight through the scheduler. {@code slam}: the Guardian stands, so the Four Arms may be used. */
    static Map<String, Integer> play(long seed, int power, int ticks, boolean slam) {
        List<MoveRow> rows = new ArrayList<>();
        for (MoveRow r : ROWS) if (slam || !r.id().equals(KingTable.GUARDIAN_SLAM)) rows.add(r);
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
    void atTheOpeningOnlyOpeningMovesAndNeverTheDisaster() {
        for (long seed = 1; seed <= 5; seed++) {
            Map<String, Integer> used = play(seed, 20, 20000, true);
            for (String id : used.keySet()) assertTrue(KingTable.byId(id).minPower() <= 20, id + " is not an opening move");
            assertNull(used.get(KingTable.DISASTER));
            assertNull(used.get(KingTable.GUARDIAN));
            assertTrue(used.size() >= 3, "variety at 20%: " + used.keySet());
        }
    }

    @Test
    void theDisasterNeedsSixtyPercentAndComesAfterTheOpeningDelay() {
        for (long seed = 1; seed <= 5; seed++) {
            assertNull(play(seed, 59, 20000, true).get(KingTable.DISASTER));
            assertNotNull(play(seed, 60, 20000, true).get(KingTable.DISASTER), "he uses it at 60%");
        }
        MoveScheduler sch = new MoveScheduler(3);
        sch.reset(0);
        MoveScheduler.Situation early = new MoveScheduler.Situation(10, 0.3f, 80, 1, MoveScheduler.FIRST_ULTIMATE_DELAY - 1, Double.POSITIVE_INFINITY, 1200);
        assertFalse(sch.eligible(KingTable.byId(KingTable.DISASTER), early));
    }

    @Test
    void atTheGatesEveryMoveGetsUsedAndTheDisasterIsSpaced() {
        for (long seed = 1; seed <= 3; seed++) {
            Map<String, Integer> used = play(seed, 100, 60000, true);
            for (MoveRow r : ROWS) assertNotNull(used.get(r.id()), r.id() + " is used at the Gates (seed " + seed + ")");
            int ults = used.get(KingTable.DISASTER);
            assertTrue(ults >= 3);
            assertTrue(ults * PowerCurve.tuning(100).ultimateGapTicks() <= 60000 + PowerCurve.tuning(100).ultimateGapTicks(), "never faster than the gap");
        }
    }

    @Test
    void withoutTheShellTheFourArmsAreNeverUsed() {
        for (long seed = 1; seed <= 3; seed++) assertNull(play(seed, 100, 30000, false).get(KingTable.GUARDIAN_SLAM));
    }
}
