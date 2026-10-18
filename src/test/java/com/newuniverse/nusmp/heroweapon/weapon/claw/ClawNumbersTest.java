package com.newuniverse.nusmp.heroweapon.weapon.claw;

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
 * Why: the Claw's numbers are the owner's balance budget in code. Every row is run through the budget AT THE TOP MASTERY (the Claw's techniques grow 30 percent harder with use,
 * so a row that fits only at level 0 would break the budget later), the check is shown to be able to fail, and the facts the design promises are pinned: at least six techniques,
 * exactly one ultimate, a curse technique that is not on the budget's cheap side, the combo arithmetic of Second Slash, the cap of the Hemorrhage and the warning before the Dragon Rend.
 */
class ClawNumbersTest {
    @Test
    void everyRowIsInsideTheBudgetAtTheBestMastery() {
        assertEquals(List.of(), Budget.violations(ClawNumbers.SPECS, ClawMastery.MAX_DAMAGE_MULT));
    }

    @Test
    void theBudgetCanFailOnTheClaw() {
        SkillSpec tooHard = new SkillSpec("claw.air_strike_slash", Kind.DAMAGE, 9.5, 1, 60, 0.01, 3);
        assertFalse(Budget.violations(tooHard, ClawMastery.MAX_DAMAGE_MULT).isEmpty(), "9.5 x 1.3 = 12.35 is over 12");
        assertTrue(Budget.violations(tooHard, 1.0).isEmpty(), "but fine before mastery: the multiplier matters");
    }

    @Test
    void theWeaponHasEnoughTechniquesAndOneUltimate() {
        assertTrue(ClawNumbers.SPECS.size() >= 6);
        assertEquals(1, ClawNumbers.SPECS.stream().filter(SkillSpec::ultimate).count());
        for (SkillSpec s : ClawNumbers.SPECS) assertTrue(s.id().startsWith("claw."), s.id());
        assertEquals(ClawNumbers.SPECS.size(), Set.copyOf(ClawNumbers.SPECS.stream().map(SkillSpec::id).toList()).size());
    }

    @Test
    void theUltimateWarnsBeforeItRuns() {
        assertTrue(ClawNumbers.REND_WARN >= 4, "the line is shown for at least four ticks before the run, so a foe can leave it");
        assertTrue(ClawNumbers.REND_WARN <= 10, "and not so long that the lightest weapon loses its speed");
        assertTrue(ClawNumbers.REND_FIRST < ClawNumbers.REND_GAP, "the first pass follows the dash at once");
    }

    @Test
    void secondSlashRowMatchesItsComboArithmetic() {
        assertEquals(ClawNumbers.SECOND_BASE * ClawNumbers.COMBO_MULT, ClawNumbers.SECOND_SLASH.raw(), 1e-9, "the row is the combo hit");
        assertEquals(2, ClawNumbers.SECOND_SLASH.hits());
        assertTrue(ClawNumbers.COMBO_WINDOW > 40, "the dispatcher's global lock is 40 ticks; the combo window must outlast it");
    }

    @Test
    void theHemorrhageBurstIsCappedAtTheRow() {
        assertEquals(ClawNumbers.HEMORRHAGE.raw(), BleedMath.BURST_MAX, 1e-9);
        assertTrue(BleedMath.burst(BleedMath.MAX_STACKS_FANGS) <= ClawNumbers.HEMORRHAGE.raw());
        assertTrue(BleedMath.burst(1) < BleedMath.burst(5), "more wounds, a bigger burst");
    }

    @Test
    void aBleedNeverOutlivesTwentySeconds() {
        assertTrue(BleedMath.CHAIN_MAX <= 400);
        assertTrue(BleedMath.TICKS_FANGS + BleedMath.INTERVAL <= BleedMath.CHAIN_MAX);
    }

    @Test
    void thePassiveSetCoversTheCurseAndTheTree() {
        assertTrue(ClawNumbers.PASSIVES.containsAll(Set.of("rending_blows", "skill_mastery", "scent_of_blood", "rope_hook", "dragon_fangs", "madness")));
        assertTrue(ClawNumbers.WAHNSINN_CLAW.cost() >= 0.02, "the curse technique is not cheap");
        assertTrue(ClawNumbers.MADNESS_CORRUPTION > 0);
    }
}
