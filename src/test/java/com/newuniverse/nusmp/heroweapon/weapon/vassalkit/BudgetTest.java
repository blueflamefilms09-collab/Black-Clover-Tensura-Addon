package com.newuniverse.nusmp.heroweapon.weapon.vassalkit;

import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget.Kind;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget.Row;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the validator guards the balance of five weapons, so it is proved itself, with a negative control for every limit of the budget of design.md section 5. */
class BudgetTest {
    private static boolean flags(Row r, String word) {
        for (String s : Budget.violations(r)) if (s.contains(word)) return true;
        return false;
    }

    @Test
    void goodRowsOfEveryKindPass() {
        assertTrue(Budget.violations(Row.damage("mirror.mirror_bash", Kind.ORDINARY, 6, 6, 6, 60, 0.01)).isEmpty());
        assertTrue(Budget.violations(Row.damage("harpoon.whale_hunting_harpoon", Kind.HEAVY, 17.5, 17.5, 1, 220, 0.025)).isEmpty());
        assertTrue(Budget.violations(Row.utility("mirror.release_rebound", 200, 0.02)).isEmpty());
        assertTrue(Budget.violations(Row.damage("mirror.kaleidoscope", Kind.ULTIMATE, 5, 30, 8, 700, 0.06)).isEmpty());
        assertTrue(Budget.violations(Row.utility("instrument.glutton_god_tango", 400, 0.02).withHeal(0.1).reaching(6)).isEmpty());
    }

    @Test
    void anOrdinaryHitAboveTwelveIsRefused() {
        assertTrue(flags(Row.damage("a.b", Kind.ORDINARY, 12.5, 12.5, 1, 60, 0.02), "raw"));
        assertFalse(flags(Row.damage("a.b", Kind.ORDINARY, 12, 12, 1, 60, 0.02), "raw"));
    }

    @Test
    void severalHitsMayNotAddUpPastTheCapOfTheirKind() {
        assertTrue(flags(Row.damage("a.b", Kind.ORDINARY, 7, 14, 1, 60, 0.02), "in all"));
        assertFalse(flags(Row.damage("a.b", Kind.HEAVY, 14, 14, 1, 60, 0.02), "in all"));
        assertTrue(flags(Row.damage("a.b", Kind.ULTIMATE, 8, 33, 8, 700, 0.06), "in all"));
    }

    @Test
    void aHeavyHitIsBetweenTenAndTwenty() {
        assertTrue(flags(Row.damage("a.b", Kind.HEAVY, 9, 9, 1, 100, 0.02), "heavy"));
        assertTrue(flags(Row.damage("a.b", Kind.HEAVY, 21, 21, 1, 100, 0.02), "raw"));
        assertTrue(Budget.violations(Row.damage("a.b", Kind.HEAVY, 10, 10, 1, 100, 0.02)).isEmpty());
        assertTrue(Budget.violations(Row.damage("a.b", Kind.HEAVY, 20, 20, 1, 100, 0.02)).isEmpty());
    }

    @Test
    void anUltimateIsCappedAtThirtyTwoAndNeedsASixHundredTickCooldownAndFiveToEightPercent() {
        assertTrue(flags(Row.damage("a.b", Kind.ULTIMATE, 33, 33, 4, 700, 0.06), "raw"));
        assertTrue(flags(Row.damage("a.b", Kind.ULTIMATE, 10, 10, 4, 599, 0.06), "cooldown"));
        assertTrue(flags(Row.damage("a.b", Kind.ULTIMATE, 10, 10, 4, 600, 0.04), "cost"));
        assertTrue(flags(Row.damage("a.b", Kind.ULTIMATE, 10, 10, 4, 600, 0.09), "cost"));
        assertTrue(Budget.violations(Row.damage("a.b", Kind.ULTIMATE, 32, 32, 12, 600, 0.05)).isEmpty());
        assertTrue(Budget.violations(Row.damage("a.b", Kind.ULTIMATE, 32, 32, 12, 600, 0.08)).isEmpty());
    }

    @Test
    void cooldownFloorsAreSixtyForDamageAndOneHundredForUtility() {
        assertTrue(flags(Row.damage("a.b", Kind.ORDINARY, 5, 5, 1, 59, 0.02), "cooldown"));
        assertFalse(flags(Row.damage("a.b", Kind.ORDINARY, 5, 5, 1, 60, 0.02), "cooldown"));
        assertTrue(flags(Row.utility("a.b", 99, 0.02), "cooldown"));
        assertFalse(flags(Row.utility("a.b", 100, 0.02), "cooldown"));
    }

    @Test
    void costOfAnOrdinaryTechniqueIsOneToThreePercent() {
        assertTrue(flags(Row.utility("a.b", 100, 0.009), "cost"));
        assertTrue(flags(Row.utility("a.b", 100, 0.031), "cost"));
        assertFalse(flags(Row.utility("a.b", 100, 0.01), "cost"));
        assertFalse(flags(Row.utility("a.b", 100, 0.03), "cost"));
    }

    @Test
    void atMostTwelveTargetsAndAtMostAQuarterHealed() {
        assertTrue(flags(Row.damage("a.b", Kind.ORDINARY, 5, 5, 13, 60, 0.02), "targets"));
        assertFalse(flags(Row.damage("a.b", Kind.ORDINARY, 5, 5, 12, 60, 0.02), "targets"));
        assertTrue(flags(Row.utility("a.b", 100, 0.02).withHeal(0.26), "heals"));
        assertFalse(flags(Row.utility("a.b", 100, 0.02).withHeal(0.25), "heals"));
    }

    @Test
    void aHitBelowThreeOrWithoutATargetIsRefused() {
        assertTrue(flags(Row.damage("a.b", Kind.ORDINARY, 2, 2, 1, 60, 0.02), "floor"));
        assertTrue(flags(Row.damage("a.b", Kind.ORDINARY, 5, 5, 0, 60, 0.02), "target"));
    }

    @Test
    void idsMustBeShapedLikeWeaponDotName() {
        assertTrue(flags(Row.utility("nodot", 100, 0.02), "id"));
        assertTrue(flags(Row.utility("Mirror.Bash", 100, 0.02), "id"));
        assertFalse(flags(Row.utility("sewing_set.needle_shot", 100, 0.02), "id"));
    }

    @Test
    void aTableRefusesARepeatedId() {
        List<Row> rows = List.of(Row.utility("a.b", 100, 0.02), Row.utility("a.b", 200, 0.02));
        List<String> bad = Budget.violations(rows);
        assertEquals(1, bad.size());
        assertTrue(bad.get(0).contains("twice"));
    }
}
