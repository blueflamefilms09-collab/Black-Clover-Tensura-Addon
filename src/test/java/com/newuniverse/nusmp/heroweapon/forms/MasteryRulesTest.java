package com.newuniverse.nusmp.heroweapon.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: a form must finish exactly once, at its total, and never before; these cases pin the edge (the step that reaches the cap is the only one that says so).
 */
class MasteryRulesTest {
    @Test
    void pointsAddUpToTheCapAndNoFurther() {
        MasteryRules.Step s = MasteryRules.add(0, 1, 40);
        assertEquals(1, s.value());
        assertFalse(s.finished());
        assertEquals(40, MasteryRules.add(39, 5, 40).value());
        assertEquals(40, MasteryRules.add(0, 10_000, 40).value());
        assertEquals(40, MasteryRules.add(Integer.MAX_VALUE - 1, Integer.MAX_VALUE, 40).value(), "no overflow");
    }

    @Test
    void theStepThatReachesTheCapIsTheOnlyOneThatFinishes() {
        assertFalse(MasteryRules.add(37, 2, 40).finished());
        assertTrue(MasteryRules.add(38, 2, 40).finished());
        assertTrue(MasteryRules.add(30, 99, 40).finished(), "overshooting still finishes, once");
        assertFalse(MasteryRules.add(40, 1, 40).finished(), "already full");
        assertFalse(MasteryRules.add(41, 1, 40).finished(), "a stored value above the cap (the data changed) does not finish again");
        assertEquals(40, MasteryRules.add(41, 1, 40).value());
    }

    @Test
    void aFormWithoutMasteryAndOddInputsAreSafe() {
        assertEquals(0, MasteryRules.add(5, 5, 0).value());
        assertFalse(MasteryRules.add(5, 5, 0).finished());
        assertEquals(0, MasteryRules.add(5, 5, -3).value());
        assertEquals(10, MasteryRules.add(10, -4, 40).value(), "negative points add nothing");
        assertEquals(3, MasteryRules.add(-8, 3, 40).value(), "a negative stored value counts as 0");
        assertFalse(MasteryRules.add(10, -4, 40).finished());
    }

    @Test
    void shareAndReward() {
        assertEquals(0.25, MasteryRules.share(10, 40), 1e-9);
        assertEquals(1.0, MasteryRules.share(50, 40), 1e-9);
        assertEquals(0.0, MasteryRules.share(-5, 40), 1e-9);
        assertEquals(1.0, MasteryRules.share(0, 0), 1e-9, "no mastery needed reads as complete");
        assertEquals(40, MasteryRules.reward(40, 0));
        assertEquals(40, MasteryRules.reward(40, 1));
        assertEquals(160, MasteryRules.reward(40, 4));
        assertEquals(0, MasteryRules.reward(-5, 3));
    }
}
