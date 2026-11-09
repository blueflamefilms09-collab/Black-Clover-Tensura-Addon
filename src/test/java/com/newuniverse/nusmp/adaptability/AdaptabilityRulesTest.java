package com.newuniverse.nusmp.adaptability;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Why: pins the maths of Adaptability (3.70.0): the damage floor, the bounded record, the Severance rule, the learning multipliers and the mastery windows. */
class AdaptabilityRulesTest {
    private static final double PER = 0.007142857, FLOOR = 0.75;

    @Test
    void theReductionGrowsPerStackAndStopsAtTheFloor() {
        assertEquals(0.0, AdaptabilityRules.reduction(0, PER, FLOOR), 1e-12);
        assertEquals(PER, AdaptabilityRules.reduction(1, PER, FLOOR), 1e-12);
        assertEquals(0.25, AdaptabilityRules.reduction(35, PER, FLOOR), 0.001);        // 35 stacks reach the 25% cap
        assertEquals(0.25, AdaptabilityRules.reduction(100000, PER, FLOOR), 1e-12);
        assertEquals(0.75, AdaptabilityRules.multiplier(Integer.MAX_VALUE, PER, FLOOR), 1e-12);
        assertEquals(1.0, AdaptabilityRules.multiplier(-5, PER, FLOOR), 1e-12);        // a negative count never raises damage
        assertEquals(1.0, AdaptabilityRules.multiplier(50, -1.0, FLOOR), 1e-12);       // nor does a negative setting
        assertEquals(1.0, AdaptabilityRules.multiplier(50, PER, 2.0), 1e-12);          // a floor above 1 means no reduction
        assertEquals(0.0, AdaptabilityRules.multiplier(1000000, 1.0, -1.0), 1e-12);    // a floor below 0 is read as 0
    }

    @Test
    void theRecordHoldsSixteenKindsAndForgetsTheWeakest() {
        Map<String, Integer> m = new HashMap<>();
        for (int i = 0; i < AdaptabilityRules.MAX_TYPES; i++) for (int n = 0; n <= i; n++) AdaptabilityRules.bump(m, "k" + i, 100);
        assertEquals(AdaptabilityRules.MAX_TYPES, m.size());
        assertEquals(1, m.get("k0").intValue());
        assertEquals("k0", AdaptabilityRules.bump(m, "new", 100));                      // k0 has the fewest stacks, so it goes
        assertFalse(m.containsKey("k0"));
        assertEquals(1, m.get("new").intValue());
        assertEquals(AdaptabilityRules.MAX_TYPES, m.size());
        assertNull(AdaptabilityRules.bump(m, "new", 100));                              // a known kind never evicts
        assertEquals(2, m.get("new").intValue());
    }

    @Test
    void aCorruptedOversizedRecordIsTrimmed() {
        Map<String, Integer> m = new HashMap<>();
        for (int i = 0; i < 40; i++) m.put("x" + i, i + 1);
        AdaptabilityRules.bump(m, "fresh", 100);
        assertTrue(m.size() <= AdaptabilityRules.MAX_TYPES);
        assertTrue(m.containsKey("fresh"));
    }

    @Test
    void stacksStopAtTheMaximum() {
        Map<String, Integer> m = new HashMap<>();
        for (int i = 0; i < 20; i++) AdaptabilityRules.bump(m, "fire", 5);
        assertEquals(5, m.get("fire").intValue());
        assertTrue(AdaptabilityRules.learning(4, 5));
        assertFalse(AdaptabilityRules.learning(5, 5));
        assertEquals(1, AdaptabilityRules.maxStacks(false, 1, 7));
        assertEquals(7, AdaptabilityRules.maxStacks(true, 1, 7));
        assertEquals(0, AdaptabilityRules.maxStacks(true, 1, -3));
    }

    @Test
    void keysAreSafe() {
        assertEquals("unknown", AdaptabilityRules.key(null));
        assertEquals("unknown", AdaptabilityRules.key("  "));
        assertEquals("minecraft:magic", AdaptabilityRules.key("minecraft:magic"));
        assertEquals(AdaptabilityRules.MAX_KEY, AdaptabilityRules.key("x".repeat(500)).length());
    }

    @Test
    void severanceImmunityNeedsTheSetNumberOfHitsAndAtLeastOne() {
        assertFalse(AdaptabilityRules.severanceImmune(24, 25));
        assertTrue(AdaptabilityRules.severanceImmune(25, 25));
        assertFalse(AdaptabilityRules.severanceImmune(0, 0));                           // zero needed is read as one
        assertTrue(AdaptabilityRules.severanceImmune(1, 0));
    }

    @Test
    void masteryAndLearningNumbers() {
        assertEquals(1.0, AdaptabilityRules.selfMastery(false, 1.0, 2.0, 6.0), 1e-12);
        assertEquals(3.0, AdaptabilityRules.selfMastery(true, 1.0, 2.0, 6.0), 1e-12);
        assertEquals(6.0, AdaptabilityRules.selfMastery(true, 10.0, 2.0, 6.0), 1e-12);   // capped
        assertEquals(0.0, AdaptabilityRules.selfMastery(true, -3.0, -3.0, 6.0), 1e-12);
        assertEquals(3.45, AdaptabilityRules.learningPoints(1.0, false, 3.45, 5.85), 1e-12);
        assertEquals(5.85, AdaptabilityRules.learningPoints(1.0, true, 3.45, 5.85), 1e-12);
        assertEquals(0.0, AdaptabilityRules.learningPoints(0.0, true, 3.45, 5.85), 1e-12);   // nothing learned stays nothing
        assertEquals(-2.0, AdaptabilityRules.learningPoints(-2.0, true, 3.45, 5.85), 1e-12);
        assertEquals(1.0, AdaptabilityRules.learningPoints(1.0, false, 0.1, 0.1), 1e-12);    // never slows learning
        assertEquals(2.0, AdaptabilityRules.globalBonus(true, 0.5, 2.0), 1e-12);
        assertEquals(0.5, AdaptabilityRules.globalBonus(false, 0.5, 2.0), 1e-12);
        assertEquals(0.0, AdaptabilityRules.globalBonus(false, -1.0, 2.0), 1e-12);
    }

    @Test
    void theGlobalPassIsDueEveryIntervalAndPaysAFixedWindow() {
        assertTrue(AdaptabilityRules.globalDue(0, 100));
        assertTrue(AdaptabilityRules.globalDue(200, 100));
        assertFalse(AdaptabilityRules.globalDue(150, 100));
        assertTrue(AdaptabilityRules.globalDue(40, 1));                                  // an interval below 20 is read as 20
        assertFalse(AdaptabilityRules.globalDue(30, 1));
        assertTrue(java.util.Arrays.equals(new int[] {0, 0}, AdaptabilityRules.window(0, 3, 24)));
        assertTrue(java.util.Arrays.equals(new int[] {0, 5}, AdaptabilityRules.window(5, 9, 24)));         // a short list is paid whole
        int[] seen = new int[100];
        for (int pass = 0; pass < 5; pass++) {
            int[] w = AdaptabilityRules.window(100, pass, 24);
            assertEquals(24, w[1]);
            for (int n = 0; n < w[1]; n++) seen[(w[0] + n) % 100]++;
        }
        for (int v : seen) assertTrue(v <= 2, "no skill is paid twice as often as another over a few passes");
    }
}
