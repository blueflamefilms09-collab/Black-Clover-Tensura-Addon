package com.newuniverse.nusmp.relics.order;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The God of Order's numbers and words: telegraphs are never short, ids are unique, the fight is bounded, and the voice never runs dry. */
class OrderTableTest {
    @Test
    void theBodyIsTheDesignedOne() {
        assertEquals(1200, OrderTable.HEALTH);
        assertTrue(OrderTable.XP > 0);
    }

    @Test
    void everyMoveIsTelegraphedAndHasAUniqueId() {
        Set<String> ids = new HashSet<>();
        for (OrderTable.Spec s : OrderTable.specs()) {
            assertTrue(ids.add(s.id()), "unique id " + s.id());
            assertTrue(s.windUp() >= 8, s.id() + " wind-up is at least the framework minimum of 8 ticks");
            if (s.ultimate()) assertTrue(s.windUp() >= 30, s.id() + " an ultimate winds up for at least 1.5 s");
            assertTrue(s.cooldown() >= 10 && s.maxRange() > s.minRange() && s.weight() >= 1);
            assertFalse(s.label().isEmpty());
        }
        assertEquals(4, ids.size(), "Decree, Writ, Judgement, Erasure");
        assertTrue(ids.containsAll(Set.of(OrderTable.DECREE, OrderTable.WRIT, OrderTable.JUDGEMENT, OrderTable.ERASURE)));
    }

    @Test
    void onlyErasureIsAnUltimate() {
        for (OrderTable.Spec s : OrderTable.specs()) assertEquals(OrderTable.ERASURE.equals(s.id()), s.ultimate(), s.id());
    }

    @Test
    void baseDamagesAreModestBecauseTheFrameworkScalesThem() {
        assertTrue(OrderTable.DECREE_DAMAGE < OrderTable.WRIT_DAMAGE && OrderTable.WRIT_DAMAGE < OrderTable.JUDGEMENT_DAMAGE);
        assertTrue(OrderTable.JUDGEMENT_DAMAGE <= 24f, "the heaviest base hit stays under a quarter of a 100-point player");
        assertTrue(OrderTable.DECREE_SLOW_TICKS <= 100 && OrderTable.DECREE_SLOW_AMP <= 3);
    }

    @Test
    void theFightEndsByErasureAfterAMinuteAndAHalfAtTheLatest() {
        assertFalse(OrderTable.forcedErasureDue(0, 1f));
        assertFalse(OrderTable.forcedErasureDue(OrderTable.ERASURE_AFTER_TICKS - 1, 0.9f));
        assertTrue(OrderTable.forcedErasureDue(OrderTable.ERASURE_AFTER_TICKS, 0.9f));
        assertTrue(OrderTable.forcedErasureDue(10, 0.3f), "hurt badly: it ends the matter");
        assertTrue(OrderTable.ERASURE_AFTER_TICKS < OrderPlan.COMBAT_LIMIT_TICKS, "the Erasure comes before the time limit");
    }

    @Test
    void theFirstSpawnRadiusIsTheEightBlocksOfTheDesign() {
        assertEquals(8.0, OrderTable.SPAWN_RADII[0]);
        for (double r : OrderTable.SPAWN_RADII) assertTrue(r >= 3 && r <= 10);
    }

    @Test
    void theVoiceHasLinesForEveryOccasionAndNoneIsEmpty() {
        for (int i = -5; i < 12; i++) {
            assertFalse(OrderText.reading(i).isEmpty());
            assertFalse(OrderText.beam(i).isEmpty());
            assertFalse(OrderText.farewell(i).isEmpty());
            assertFalse(OrderText.provoked(i).isEmpty());
            for (OrderTable.Spec s : OrderTable.specs()) assertFalse(OrderText.quip(s.id(), i).isEmpty());
        }
        assertFalse(OrderText.erasure().isEmpty());
        assertFalse(OrderText.withdraw().isEmpty());
        assertFalse(OrderText.power(20, 40).isEmpty());
        assertFalse(OrderText.gates().isEmpty());
        assertFalse(OrderText.death().isEmpty());
    }

    @Test
    void theAnnouncementReadsAsTheOwnerWroteIt() {
        assertEquals("[Order] Two of Bloodbound Katana cannot be.", OrderText.PREFIX + OrderText.TWO_OF + OrderText.titleCase("bloodbound_katana") + OrderText.CANNOT_BE);
    }

    @Test
    void titleCaseHandlesIdsOfEveryShape() {
        assertEquals("Quicklings Ivy", OrderText.titleCase("quicklings_ivy"));
        assertEquals("Astralis The Eye Of The Universe", OrderText.titleCase("astralis_the_eye_of_the_universe"));
        assertEquals("", OrderText.titleCase(""));
        assertEquals("", OrderText.titleCase(null));
        assertEquals("A B", OrderText.titleCase("a__b".replace("__", "_")));
    }

    @Test
    void censusMessagesNameWhatTheyMustAndNeverPromiseZeroMinutes() {
        assertTrue(OrderText.censusStart(10).contains("10 minutes"));
        assertTrue(OrderText.censusWarning("Doom Hammer", 0).contains("In 1 minutes"), "at least one minute");
        assertTrue(OrderText.censusWarning("Doom Hammer", 7).contains("Doom Hammer") && OrderText.censusWarning("Doom Hammer", 7).contains("7 minutes"));
        assertTrue(OrderText.censusEnd(16, 2).contains("16") && OrderText.censusEnd(16, 2).contains("2"));
    }
}
