package com.newuniverse.nusmp.heroweapon.weapon.axe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.weapon.strike.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.strike.SkillSpec;
import com.newuniverse.nusmp.heroweapon.weapon.strike.SkillSpec.Kind;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the Axe's numbers are the owner's balance budget in code. Every row is run through the budget, the check is shown to be able to fail, and the facts the design promises
 * are pinned: at least six techniques, exactly one ultimate, the ultimate's best damage (at eight Brawn) is exactly the row and inside the cap, and a hit stacked with Brawn
 * and with an execution is clamped to the cap of its kind in the arithmetic the skills use, and the Curse Axe's self-repair stays small once the healing law has had its say.
 */
class AxeNumbersTest {
    @Test
    void everyRowIsInsideTheBudget() {
        assertEquals(List.of(), Budget.violations(AxeNumbers.SPECS, 1.0));
    }

    @Test
    void theBudgetCanFailOnTheAxe() {
        assertFalse(Budget.violations(new SkillSpec("axe.guillotine", Kind.HEAVY, 21, 1, 110, 0.02, 1), 1.0).isEmpty());
        assertFalse(Budget.violations(new SkillSpec("axe.crescent_break", Kind.DAMAGE, 8, 1, 70, 0.015, 13), 1.0).isEmpty());
    }

    @Test
    void theWeaponHasEnoughTechniquesAndOneUltimate() {
        assertTrue(AxeNumbers.SPECS.size() >= 6);
        assertEquals(1, AxeNumbers.SPECS.stream().filter(SkillSpec::ultimate).count());
        for (SkillSpec s : AxeNumbers.SPECS) assertTrue(s.id().startsWith("axe."), s.id());
        assertEquals(AxeNumbers.SPECS.size(), Set.copyOf(AxeNumbers.SPECS.stream().map(SkillSpec::id).toList()).size());
    }

    @Test
    void theUltimateRowIsExactlyTheBestBurst() {
        assertEquals(BrawnMath.shineRaw(BrawnMath.MAX), AxeNumbers.SHINE_AXE_BURST.raw(), 1e-4);
        assertTrue(BrawnMath.shineRaw(BrawnMath.MAX) <= Budget.cap(Kind.ULTIMATE));
        assertTrue(BrawnMath.shineRaw(BrawnMath.SHINE_MIN) >= 24.9, "five Brawn already make a real blow");
    }

    @Test
    void aStackedHitNeverPassesTheCapOfItsKind() {
        double brawn = BrawnMath.skillMult(BrawnMath.MAX);
        for (SkillSpec s : AxeNumbers.SPECS) {
            float raw = ExecuteMath.raw((float) s.raw(), brawn, true, false, (float) Budget.cap(s.kind()));
            assertTrue(raw <= Budget.cap(s.kind()) + 1e-6, s.id() + " executed at full Brawn: " + raw);
        }
        float plain = ExecuteMath.raw(AxeNumbers.GUILLOTINE_BASE, 1.0, true, false, (float) Budget.cap(Kind.HEAVY));
        assertEquals(AxeNumbers.GUILLOTINE.raw(), plain, 1e-4, "the row is the executing hit");
        float guillotine = ExecuteMath.raw(AxeNumbers.GUILLOTINE_BASE, brawn, true, false, (float) Budget.cap(Kind.HEAVY));
        assertEquals(20f, guillotine, 1e-4, "9 x 1.16 x 2.2 is 23, clamped to the heavy cap of 20");
    }

    @Test
    void everyBlowWithATellHasADelay() {
        assertTrue(AxeNumbers.CRESCENT_DELAY >= 4 && AxeNumbers.POWER_DELAY >= 4 && AxeNumbers.MOUNTAIN_DELAY >= 4);
        assertTrue(AxeNumbers.GUILLOTINE_DELAY >= 4 && AxeNumbers.SHINE_DELAY >= 10 && AxeNumbers.PENETRATION_DELAY >= 4);
    }

    @Test
    void theTornadoRunsFourPulsesAndEndsQuickly() {
        int pulses = 0;
        for (int age = 0; age < AxeNumbers.TORNADO_TICKS; age++) if (age % AxeNumbers.TORNADO_GAP == 0) pulses++;
        assertEquals(AxeNumbers.TORNADO_AXE.hits(), pulses);
        assertTrue(AxeNumbers.TORNADO_TICKS <= 40, "no repeating task near the 20-second limit");
    }

    @Test
    void theCurseFormPaysCorruptionAndTheWoundComesAfterTheBlow() {
        assertTrue(AxeNumbers.PENETRATION_CORRUPTION > 0);
        assertTrue(AxeNumbers.WOUND_DELAY > AxeNumbers.PENETRATION_DELAY);
        assertTrue(AxeNumbers.CURSED_PENETRATION.cost() >= 0.02);
        assertTrue(AxeNumbers.WOUND_RAW <= AxeNumbers.CURSED_PENETRATION.raw());
    }

    @Test
    void theSelfRepairIsSmallButNotNothing() {
        assertTrue(AxeNumbers.REPAIR_EVERY >= 20, "a mend at least a second apart");
        assertTrue(AxeNumbers.REPAIR_SHARE >= 0.01 && AxeNumbers.REPAIR_SHARE <= 0.06, "a few percent of the health, not a fraction of a heart and not a second life");
        double perSecondAfterTheLaw = AxeNumbers.REPAIR_SHARE * 0.4 * 20.0 / AxeNumbers.REPAIR_EVERY;
        assertTrue(perSecondAfterTheLaw < 0.015, "under 1.5 percent of the maximum health a second once the healing law has cut the repeated heals to 40 percent");
    }
}
