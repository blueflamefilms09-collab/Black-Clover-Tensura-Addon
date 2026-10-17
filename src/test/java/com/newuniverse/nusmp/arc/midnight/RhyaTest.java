package com.newuniverse.nusmp.arc.midnight;

import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import com.newuniverse.nusmp.entity.zogratis.TowerMath;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Rhya's whole fight is the memory of what he copied (the newest wins, a repeat refreshes, time forgets, what he knows hurts him less), and it is easy to get subtly wrong, so the memory, his table and the fairness of his marks are tested. */
class RhyaTest {
    static final List<MidnightSpec> SPECS = RhyaTable.specs();

    @Test
    void theTableObeysTheLawsOfTheFramework() {
        List<String> problems = MidnightSpec.problems(SPECS);
        assertTrue(problems.isEmpty(), "problems: " + problems);
        assertTrue(SPECS.size() >= 8 && SPECS.size() <= 14, "eight to fourteen moves");
    }

    @Test
    void theMixAndTheOrderAreRight() {
        assertEquals(RhyaTable.COPY_TOUCH, SPECS.get(0).id());
        assertEquals(RhyaTable.SPELLBOOK, SPECS.get(10).id());
        assertEquals(1, SPECS.stream().filter(MidnightSpec::ultimate).count());
        MidnightSpec book = RhyaTable.byId(RhyaTable.SPELLBOOK);
        assertTrue(book.ultimate() && book.minPower() == 60 && book.recovery() >= 60);
        assertTrue(SPECS.stream().anyMatch(s -> s.minRange() == 0), "something works at point-blank");
        assertTrue(SPECS.stream().anyMatch(s -> s.minRange() >= 5 && s.maxRange() >= 36), "ranged");
        assertTrue(SPECS.stream().anyMatch(MidnightSpec::crowd), "crowd");
        assertNotNull(RhyaTable.byId(RhyaTable.SCOUT), "mobility");
        assertNotNull(RhyaTable.byId(RhyaTable.SEAL), "control");
        assertNotNull(RhyaTable.byId(RhyaTable.COPY_TOUCH), "the signature");
        assertNull(RhyaTable.byId("nope"));
    }

    @Test
    void thereIsNoDeadRangeAtTheOpening() {
        for (double d = 0; d <= 40; d += 0.5) assertTrue(MidnightSpec.reaches(SPECS, 20, d), "an attack to throw at " + d + " blocks at 20%");
    }

    @Test
    void theCountsRiseWithThePower() {
        int[] powers = {20, 40, 60, 80, 100};
        for (int i = 1; i < powers.length; i++) {
            assertTrue(RhyaMath.boltCount(powers[i]) >= RhyaMath.boltCount(powers[i - 1]));
            assertTrue(RhyaMath.lightCopyBlades(powers[i]) >= RhyaMath.lightCopyBlades(powers[i - 1]));
            assertTrue(RhyaMath.novaCount(powers[i]) >= RhyaMath.novaCount(powers[i - 1]));
            assertTrue(RhyaMath.sealCount(powers[i]) >= RhyaMath.sealCount(powers[i - 1]));
        }
        assertEquals(1, RhyaMath.sealCount(20));
        assertEquals(2, RhyaMath.sealCount(100), "two rings of chains at the end");
        assertEquals(3, RhyaMath.boltCount(20));
        assertEquals(6, RhyaMath.boltCount(100));
        assertEquals(1, RhyaMath.memoryCapacity(false));
        assertEquals(2, RhyaMath.memoryCapacity(true), "the open Third Eye holds two");
    }

    @Test
    void theShapesAgreeWithTheTowerTables() {
        // the copied nova must be as big as the Tower nova, and its warning (the wind-up) long enough to walk out of it
        assertEquals(TowerMath.radius(TowerMath.Shape.NOVA), RhyaMath.NOVA_RADIUS, 1e-12);
        MidnightSpec nova = RhyaTable.byId(RhyaTable.NOVA);
        assertTrue(MidnightMath.escapeReach(nova.windUpTicks()) >= RhyaMath.NOVA_RADIUS + 0.3);
        assertTrue(nova.windUpTicks() >= TowerMath.markTicks(TowerMath.Shape.NOVA));
        assertTrue(RhyaMath.novaCount(100) <= TowerMath.count(TowerMath.Shape.NOVA, 4));
        assertTrue(RhyaMath.boltCount(100) <= TowerMath.count(TowerMath.Shape.BOLT, 4));
    }

    @Test
    void everyMarkCanBeLeftInTheTimeItIsShown() {
        MidnightSpec seal = RhyaTable.byId(RhyaTable.SEAL);
        assertTrue(MidnightMath.escapeReach(seal.windUpTicks()) >= RhyaMath.SEAL_RADIUS + 0.3);
        assertTrue(MidnightMath.escapeReach(RhyaMath.BOOK_WARN) >= RhyaMath.BOOK_RADIUS + 0.3, "the Stolen Spellbook's marks");
        MidnightSpec scout = RhyaTable.byId(RhyaTable.SCOUT);
        assertTrue(MidnightMath.escapeReach(scout.windUpTicks()) >= RhyaMath.SCOUT_RADIUS + 0.3, "the arrival ring of the scout");
        MidnightSpec avidya = RhyaTable.byId(RhyaTable.AVIDYA);
        assertTrue(MidnightMath.escapeReach(avidya.windUpTicks()) >= RhyaMath.AVIDYA_HALF_WIDTH + 0.3, "even the wide slash can be left");
        MidnightSpec lance = RhyaTable.byId(RhyaTable.LANCE);
        assertTrue(lance.windUpTicks() - RhyaMath.LANCE_LOCK >= 12);
        assertTrue(MidnightMath.escapeReach(RhyaMath.LANCE_LOCK) >= RhyaMath.LANCE_HALF_WIDTH + 0.3 + 0.5);
        MidnightSpec bolts = RhyaTable.byId(RhyaTable.BOLTS);
        assertTrue(bolts.windUpTicks() > RhyaMath.BOLT_LOCK, "the volley locks before it leaves");
        assertTrue(MidnightMath.escapeReach(RhyaMath.BOLT_LOCK) >= RhyaMath.BOLT_RADIUS + 0.3 + 0.5, "a frozen line of copied bolts can be stepped out of");
        MidnightSpec light = RhyaTable.byId(RhyaTable.LIGHT_COPY);
        assertTrue(light.windUpTicks() - RhyaMath.LIGHT_COPY_LOCK >= 12);
        assertTrue(RhyaMath.BOOK_GAP * RhyaMath.BOOK_SALVOS < 120, "the Stolen Spellbook is over in six seconds");
    }

    @Test
    void theMemoryKeepsTheNewestAndForgetsWithTime() {
        ImitationMemory m = new ImitationMemory(1);
        assertNull(m.current(0));
        m.learn("FLAME", 100);
        assertEquals("FLAME", m.current(100));
        m.learn("WATER", 200);
        assertEquals("WATER", m.current(200), "the newest wins");
        assertFalse(m.knows("FLAME", 200), "one slot: the old one is gone");
        assertEquals("WATER", m.current(200 + ImitationMemory.WINDOW - 1));
        assertNull(m.current(200 + ImitationMemory.WINDOW), "forgotten after twenty-four seconds");
        m.learn("", 300);
        m.learn(null, 300);
        assertEquals(0, m.known(300 + ImitationMemory.WINDOW).size());
    }

    @Test
    void withTheThirdEyeOpenHeHoldsTwoAndMeetingOneAgainRefreshesIt() {
        ImitationMemory m = new ImitationMemory(1);
        m.learn("FLAME", 10);
        m.setCapacity(2);
        m.learn("WATER", 20);
        assertEquals(List.of("WATER", "FLAME"), m.known(30), "newest first");
        m.learn("FLAME", 40);
        assertEquals(List.of("FLAME", "WATER"), m.known(50), "meeting it again moves it to the front, it does not fill a second slot");
        m.learn("LIGHT", 60);
        assertEquals(List.of("LIGHT", "FLAME"), m.known(70), "the oldest is dropped");
        assertEquals(2, m.known(70).size());
        m.setCapacity(1);
        assertEquals(List.of("LIGHT"), m.known(70), "shrinking forgets the oldest");
        m.setCapacity(0);
        assertEquals(1, m.capacity(), "never fewer than one");
        m.clear();
        assertNull(m.current(70));
    }

    @Test
    void whatHeKnowsHurtsHimLess() {
        ImitationMemory m = new ImitationMemory(1);
        assertEquals(1.0, m.factorAgainst("FLAME", 0), 1e-12, "knows nothing yet");
        m.learn("FLAME", 100);
        assertEquals(0.70, m.factorAgainst("FLAME", 150), 1e-12);
        assertEquals(1.0, m.factorAgainst("WATER", 150), 1e-12, "a magic he does not know hits whole");
        assertEquals(1.0, m.factorAgainst(null, 150), 1e-12, "a sword has no magic to know");
        assertEquals(1.0, m.factorAgainst("FLAME", 100 + ImitationMemory.WINDOW), 1e-12, "forgotten, so whole again");
        assertTrue(ImitationMemory.RESONANCE > 0.5, "a resistance, not an immunity");
    }

    @Test
    void theDefaultsCycleThroughTheStoryMagics() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 20; i++) seen.add(RhyaMath.defaultFor(i));
        assertEquals(RhyaMath.DEFAULTS.length, seen.size(), "every default comes up");
        assertEquals(RhyaMath.defaultFor(0), RhyaMath.defaultFor(RhyaMath.DEFAULTS.length));
        assertNotNull(RhyaMath.defaultFor(-7), "a negative salt still answers");
        assertEquals("FLAME", RhyaMath.DEFAULTS[0]);
    }

    @Test
    void touchAtHandAndCopiesAtRange() {
        Map<String, Integer> near = play(2.0), far = play(30.0);
        assertTrue(near.getOrDefault(RhyaTable.COPY_TOUCH, 0) > 0, "up close he touches: " + near);
        assertNull(near.get(RhyaTable.BOLTS), "no bolts at two blocks");
        assertTrue(far.getOrDefault(RhyaTable.BOLTS, 0) + far.getOrDefault(RhyaTable.JAVELIN, 0) > 5, "far away he throws what he borrowed: " + far);
        assertNull(far.get(RhyaTable.SPELLBOOK), "no Stolen Spellbook at 40%");
        assertNull(far.get(RhyaTable.SALAMANDER_COPY), "no copied breath at 40%");
    }

    static Map<String, Integer> play(double distance) {
        Map<String, Integer> used = new HashMap<>();
        MoveScheduler sch = new MoveScheduler(33);
        sch.reset(0);
        PowerCurve.Tuning tu = PowerCurve.tuning(40);
        for (long t = 20; t < 6000; t += 5) {
            MoveScheduler.Situation s = new MoveScheduler.Situation(distance, 0.8f, 40, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(SPECS, s);
            if (idx < 0) continue;
            MidnightSpec m = SPECS.get(idx);
            sch.commit(m, s, (int) Math.round(m.cooldownTicks() * tu.cooldownMultiplier()));
            used.merge(m.id(), 1, Integer::sum);
        }
        return used;
    }
}
