package com.newuniverse.nusmp.heroweapon.weapon.strike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.weapon.strike.SkillSpec.Kind;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Why: every weapon's numbers table is judged by {@link Budget}, so the judge itself must be right at its edges and must be able to say no. Each rule of the owner's budget gets
 * a spec just inside it (passes) and a spec just outside it (fails).
 */
class BudgetTest {
    private static SkillSpec spec(Kind k, double raw, int cooldown, double cost) { return new SkillSpec("test.skill", k, raw, 1, cooldown, cost, 12); }

    @Test
    void aSpecInsideTheBudgetPasses() {
        assertTrue(Budget.violations(spec(Kind.DAMAGE, 12, 60, 0.03), 1.0).isEmpty());
        assertTrue(Budget.violations(spec(Kind.DAMAGE, 3, 60, 0.01), 1.0).isEmpty());
        assertTrue(Budget.violations(spec(Kind.HEAVY, 20, 60, 0.02), 1.0).isEmpty());
        assertTrue(Budget.violations(spec(Kind.HEAVY, 10, 80, 0.02), 1.0).isEmpty());
        assertTrue(Budget.violations(spec(Kind.UTILITY, 0, 100, 0.02), 1.0).isEmpty());
        assertTrue(Budget.violations(spec(Kind.UTILITY, 3, 100, 0.02), 1.0).isEmpty());
        assertTrue(Budget.violations(spec(Kind.ULTIMATE, 32, 600, 0.08), 1.0).isEmpty());
        assertTrue(Budget.violations(spec(Kind.ULTIMATE, 24, 900, 0.05), 1.0).isEmpty());
    }

    @Test
    void eachRuleCanSayNo() {
        assertFalse(Budget.violations(spec(Kind.DAMAGE, 12.5, 60, 0.02), 1.0).isEmpty(), "raw above 12");
        assertFalse(Budget.violations(spec(Kind.DAMAGE, 2.9, 60, 0.02), 1.0).isEmpty(), "raw below 3");
        assertFalse(Budget.violations(spec(Kind.DAMAGE, 8, 59, 0.02), 1.0).isEmpty(), "cooldown below 60");
        assertFalse(Budget.violations(spec(Kind.DAMAGE, 8, 60, 0.031), 1.0).isEmpty(), "cost above 3 percent");
        assertFalse(Budget.violations(spec(Kind.DAMAGE, 8, 60, 0.009), 1.0).isEmpty(), "cost below 1 percent");
        assertFalse(Budget.violations(spec(Kind.HEAVY, 20.5, 60, 0.02), 1.0).isEmpty(), "heavy above 20");
        assertFalse(Budget.violations(spec(Kind.UTILITY, 2, 100, 0.02), 1.0).isEmpty(), "a utility hit under 3");
        assertFalse(Budget.violations(spec(Kind.UTILITY, 0, 99, 0.02), 1.0).isEmpty(), "utility cooldown below 100");
        assertFalse(Budget.violations(spec(Kind.ULTIMATE, 32.5, 600, 0.06), 1.0).isEmpty(), "ultimate above 32");
        assertFalse(Budget.violations(spec(Kind.ULTIMATE, 24, 599, 0.06), 1.0).isEmpty(), "ultimate cooldown below 600");
        assertFalse(Budget.violations(spec(Kind.ULTIMATE, 24, 600, 0.04), 1.0).isEmpty(), "ultimate cost below 5 percent");
        assertFalse(Budget.violations(spec(Kind.ULTIMATE, 24, 600, 0.09), 1.0).isEmpty(), "ultimate cost above 8 percent");
        assertFalse(Budget.violations(new SkillSpec("test.skill", Kind.DAMAGE, 8, 1, 60, 0.02, 13), 1.0).isEmpty(), "thirteen targets");
        assertFalse(Budget.violations(new SkillSpec("test.skill", Kind.DAMAGE, 8, 0, 60, 0.02, 12), 1.0).isEmpty(), "zero hits with damage");
    }

    @Test
    void theMultiplierIsCountedAtTheWeaponsBest() {
        assertTrue(Budget.violations(spec(Kind.DAMAGE, 9, 60, 0.02), 1.3).isEmpty(), "9 x 1.3 = 11.7");
        assertFalse(Budget.violations(spec(Kind.DAMAGE, 9.5, 60, 0.02), 1.3).isEmpty(), "9.5 x 1.3 = 12.35");
        assertTrue(Budget.violations(spec(Kind.HEAVY, 15, 60, 0.02), 1.3).isEmpty(), "15 x 1.3 = 19.5");
        assertFalse(Budget.violations(spec(Kind.HEAVY, 15.5, 60, 0.02), 1.3).isEmpty(), "15.5 x 1.3 = 20.15");
    }

    @Test
    void theCapOfEachKindIsTheTopOfItsRange() {
        assertEquals(12.0, Budget.cap(Kind.DAMAGE), 1e-9);
        assertEquals(12.0, Budget.cap(Kind.UTILITY), 1e-9);
        assertEquals(20.0, Budget.cap(Kind.HEAVY), 1e-9);
        assertEquals(32.0, Budget.cap(Kind.ULTIMATE), 1e-9);
        for (Kind k : Kind.values()) {
            double top = Budget.cap(k);
            assertTrue(Budget.violations(new SkillSpec("test.skill", k, top, 1, Budget.cooldownFloor(k), k == Kind.ULTIMATE ? 0.05 : 0.01, 1), 1.0).isEmpty(), k + " at its cap");
            assertFalse(Budget.violations(new SkillSpec("test.skill", k, top + 0.01, 1, Budget.cooldownFloor(k), k == Kind.ULTIMATE ? 0.05 : 0.01, 1), 1.0).isEmpty(), k + " just over its cap");
        }
    }

    @Test
    void aTableCatchesARepeatedId() {
        SkillSpec a = spec(Kind.DAMAGE, 8, 60, 0.02);
        assertTrue(Budget.violations(List.of(a), 1.0).isEmpty());
        assertEquals(1, Budget.violations(List.of(a, a), 1.0).size());
    }

    @Test
    void theShortNameDropsThePrefix() {
        assertEquals("quake_stomp", new SkillSpec("hammer.quake_stomp", Kind.DAMAGE, 9, 1, 140, 0.02, 12).shortName());
        assertEquals("plain", new SkillSpec("plain", Kind.DAMAGE, 9, 1, 140, 0.02, 12).shortName());
    }
}
