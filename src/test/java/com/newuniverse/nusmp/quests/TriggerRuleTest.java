package com.newuniverse.nusmp.quests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Why: this rule is what makes an advancement complete or not; every combination of key, argument and count is covered. */
class TriggerRuleTest {
    @Test
    void keysMustBeEqual() {
        assertTrue(TriggerRule.matches("a", null, 1, "a", null, 1));
        assertFalse(TriggerRule.matches("a", null, 1, "b", null, 1));
        assertFalse(TriggerRule.matches(null, null, 1, "b", null, 1));
    }

    @Test
    void criterionWithoutArgAcceptsAnyArgument() {
        assertTrue(TriggerRule.matches("spell.cast", null, 1, "spell.cast", "thread", 1));
        assertTrue(TriggerRule.matches("spell.cast", "", 1, "spell.cast", "thread", 1));
        assertTrue(TriggerRule.matches("spell.cast", null, 1, "spell.cast", null, 1));
    }

    @Test
    void criterionWithArgNeedsThatArgument() {
        assertTrue(TriggerRule.matches("spell.cast", "thread", 1, "spell.cast", "thread", 1));
        assertFalse(TriggerRule.matches("spell.cast", "thread", 1, "spell.cast", "fire", 1));
        assertFalse(TriggerRule.matches("spell.cast", "thread", 1, "spell.cast", null, 1));
    }

    @Test
    void countMustBeReached() {
        assertFalse(TriggerRule.matches("spell.cast", null, 10, "spell.cast", "fire", 9));
        assertTrue(TriggerRule.matches("spell.cast", null, 10, "spell.cast", "fire", 10));
        assertTrue(TriggerRule.matches("spell.cast", null, 10, "spell.cast", "fire", 11));
    }

    @Test
    void countBelowOneCountsAsOne() {
        assertTrue(TriggerRule.matches("a", null, 0, "a", null, 1));
        assertTrue(TriggerRule.matches("a", null, -5, "a", null, 1));
        assertFalse(TriggerRule.matches("a", null, 0, "a", null, 0));
    }

    @Test
    void endToEndWithCounters() {
        Map<String, Integer> c = new HashMap<>();
        int fired = 0;
        for (int i = 0; i < 3; i++) {
            EventCounters.increment(c, "spell.cast", i < 2 ? "thread" : "fire");
            boolean any3 = TriggerRule.matches("spell.cast", null, 3, "spell.cast", i < 2 ? "thread" : "fire",
                    EventCounters.read(c, "spell.cast", null));
            boolean thread3 = TriggerRule.matches("spell.cast", "thread", 3, "spell.cast", i < 2 ? "thread" : "fire",
                    EventCounters.read(c, "spell.cast", "thread"));
            if (any3) fired++;
            assertFalse(thread3);
        }
        assertTrue(fired == 1);
    }
}
