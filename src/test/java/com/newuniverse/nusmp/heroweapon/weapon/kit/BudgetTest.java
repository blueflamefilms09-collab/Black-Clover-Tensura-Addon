package com.newuniverse.nusmp.heroweapon.weapon.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Why: {@link Budget#violations} is the judge every weapon's {@code NumbersTest} asks, so the judge itself needs a negative control: a technique within the
 * budget passes, and each way of leaving it (too hard, too many hits, too fast, too dear, too many targets, a bad heal, a bad id) is reported. Without these a
 * judge that always said "fine" would let the whole balance table rot unnoticed.
 */
class BudgetTest {
    private static boolean mentions(List<String> bad, String word) {
        for (String s : bad) if (s.contains(word)) return true;
        return false;
    }

    @Test
    void techniquesWithinTheBudgetPass() {
        assertEquals(List.of(), Budget.violations(Budget.damage("staff.arcane_bolt", 80, 0.015, 7, 9.2, 1, 5)));
        assertEquals(List.of(), Budget.violations(Budget.heavy("gauntlet.turtle_crush", 200, 0.03, 14, 18, 1, 8)));
        assertEquals(List.of(), Budget.violations(Budget.ultimate("gauntlet.dragon_slayer", 900, 0.06, 20, 28, 1, 6)));
        assertEquals(List.of(), Budget.violations(Budget.utility("carriage.call", 100, 0.01, 1)));
        assertEquals(List.of(), Budget.violations(Budget.utilityHit("staff.float_mirror", 320, 0.025, 3, 4, 4)));
        assertEquals(List.of(), Budget.violations(Budget.utility("staff.healing_light", 420, 0.03, 8).withHeal(0.14)));
    }

    @Test
    void eachWayOfLeavingTheBudgetIsReported() {
        assertTrue(mentions(Budget.violations(Budget.damage("a.b", 80, 0.02, 7, 13, 1, 3)), "ordinary technique deals"), "13 raw is too hard for an ordinary one");
        assertTrue(mentions(Budget.violations(Budget.damage("a.b", 80, 0.02, 2, 8, 1, 3)), "ordinary technique deals"), "2 raw is below the ordinary floor");
        assertTrue(mentions(Budget.violations(Budget.damage("a.b", 80, 0.02, 4, 4, 4, 3)), "above the ordinary 12"), "four hits of 4 stack above 12");
        assertTrue(mentions(Budget.violations(Budget.damage("a.b", 59, 0.02, 5, 5, 1, 3)), "at least 60"), "59 ticks is too fast");
        assertTrue(mentions(Budget.violations(Budget.damage("a.b", 80, 0.05, 5, 5, 1, 3)), "cost"), "5 percent is too dear for an ordinary one");
        assertTrue(mentions(Budget.violations(Budget.damage("a.b", 80, 0.02, 5, 5, 1, 13)), "maxTargets"), "13 targets");
        assertTrue(mentions(Budget.violations(Budget.heavy("a.b", 100, 0.02, 9, 12, 1, 3)), "heavy technique deals"), "9 raw is below the heavy floor");
        assertTrue(mentions(Budget.violations(Budget.heavy("a.b", 100, 0.02, 12, 21, 1, 3)), "heavy technique deals"), "21 raw is above the heavy ceiling");
        assertTrue(mentions(Budget.violations(Budget.ultimate("a.b", 599, 0.06, 20, 20, 1, 3)), "at least 600"), "599 ticks is too fast for an ultimate");
        assertTrue(mentions(Budget.violations(Budget.ultimate("a.b", 900, 0.03, 20, 20, 1, 3)), "cost"), "an ultimate costs 5 to 8 percent");
        assertTrue(mentions(Budget.violations(Budget.ultimate("a.b", 900, 0.06, 20, 33, 1, 3)), "at most 32"), "33 raw is above the ultimate ceiling");
        assertTrue(mentions(Budget.violations(Budget.ultimate("a.b", 900, 0.06, 4, 4, 9, 3)), "at most 32"), "nine hits of 4 stack above 32");
        assertTrue(mentions(Budget.violations(Budget.utility("a.b", 99, 0.02, 3)), "at least 100"), "a utility waits 100");
        assertTrue(mentions(Budget.violations(Budget.utility("a.b", 120, 0.02, 3).withHeal(0.3)), "heal"), "a heal above a quarter");
        assertTrue(mentions(Budget.violations(Budget.utilityHit("a.b", 120, 0.02, 14, 1, 3)), "utility technique that hits"), "a utility that hits for 14");
        assertTrue(mentions(Budget.violations(Budget.utility("NoDot", 120, 0.02, 3)), "id must be"), "a bad id");
        assertFalse(Budget.violations(Budget.damage("a.b", 80, 0.02, 7, 13, 1, 3)).isEmpty());
    }

    @Test
    void hastedCooldownsKeepTheFloorOfTheirKind() {
        assertEquals(60, Budget.scaled(80, 0.55, Budget.floor(Budget.Kind.ORDINARY)), "80 hasted to 44 stops at the damage floor");
        assertEquals(88, Budget.scaled(160, 0.55, 60));
        assertEquals(100, Budget.scaled(120, 0.55, Budget.floor(Budget.Kind.UTILITY)));
        assertEquals(600, Budget.scaled(900, 0.1, Budget.floor(Budget.Kind.ULTIMATE)));
        assertEquals(60, Budget.floor(Budget.Kind.HEAVY));
    }
}
