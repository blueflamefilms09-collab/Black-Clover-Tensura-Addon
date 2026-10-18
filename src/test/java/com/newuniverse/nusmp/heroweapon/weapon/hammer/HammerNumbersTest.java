package com.newuniverse.nusmp.heroweapon.weapon.hammer;

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
 * Why: the Hammer's numbers are the owner's balance budget in code. This test runs every row through the budget (raw damage, cooldown, cost, targets), proves the check can fail,
 * and pins the facts the design promises: the weapon has at least six techniques, exactly one ultimate, every technique that hits for more than 8 raw has a tell (a delay before
 * the blow), no two ids repeat, every id belongs to the hammer, and one poise blow of the heaviest techniques is enough to break an ordinary foe.
 */
class HammerNumbersTest {
    @Test
    void everyRowIsInsideTheBudget() {
        assertEquals(List.of(), Budget.violations(HammerNumbers.SPECS, 1.0));
    }

    @Test
    void theBudgetCanFailOnTheHammer() {
        SkillSpec tooHard = new SkillSpec("hammer.overhead_smash", Kind.HEAVY, 21, 1, 80, 0.015, 12);
        assertFalse(Budget.violations(tooHard, 1.0).isEmpty());
        SkillSpec tooQuick = new SkillSpec("hammer.shockwave", Kind.DAMAGE, 7, 1, 30, 0.02, 12);
        assertFalse(Budget.violations(tooQuick, 1.0).isEmpty());
    }

    @Test
    void theWeaponHasEnoughTechniquesAndOneUltimate() {
        assertTrue(HammerNumbers.SPECS.size() >= 6);
        assertEquals(1, HammerNumbers.SPECS.stream().filter(SkillSpec::ultimate).count());
        for (SkillSpec s : HammerNumbers.SPECS) assertTrue(s.id().startsWith("hammer."), s.id());
        assertEquals(HammerNumbers.SPECS.size(), Set.copyOf(HammerNumbers.SPECS.stream().map(SkillSpec::id).toList()).size());
    }

    @Test
    void everyHeavyBlowHasATellOfAtLeastFourTicks() {
        int[] delays = {HammerNumbers.SMASH_DELAY, HammerNumbers.SHOCK_DELAY, HammerNumbers.CRACK_DELAY, HammerNumbers.QUAKE_DELAY, HammerNumbers.SKYWARD_DELAY};
        for (int d : delays) assertTrue(d >= 4, "a tell of " + d);
        assertTrue(HammerNumbers.TITAN_MIN_AIR >= 4, "the leap itself is the ultimate's tell");
    }

    @Test
    void theHeavyTechniquesBreakAnOrdinaryFoeInOneBlowButNeverABossInOne() {
        float ordinary = PoiseMath.capacity(40f, false);
        float weakestBoss = PoiseMath.capacity(300f, true);
        assertTrue(HammerNumbers.SMASH_POISE >= ordinary, "an overhead smash breaks a 40-health mob: " + ordinary);
        assertTrue(HammerNumbers.TITAN_POISE >= PoiseMath.capacity(120f, false), "the ultimate breaks a golem-sized foe");
        float heaviestOrdinaryTechnique = Math.max(Math.max(HammerNumbers.SMASH_POISE, HammerNumbers.CRACK_POISE), Math.max(HammerNumbers.QUAKE_POISE, HammerNumbers.SHOCK_POISE));
        assertTrue(heaviestOrdinaryTechnique < weakestBoss, "no ordinary technique breaks even the weakest boss in one blow: " + weakestBoss);
        assertTrue(PoiseMath.BREAK_TICKS_BOSS <= 20 && PoiseMath.LOCKOUT_BOSS >= 400, "a boss break is brief and rare");
    }

    @Test
    void thePassiveSetMatchesTheSynthesisTable() {
        for (String form : new String[]{"base", "stone_hammer", "anvil_hammer", "synthesis_hammer"}) {
            assertTrue(HammerNumbers.PASSIVES.contains(HammerSynthesis.signature(form)), form);
        }
        assertTrue(HammerNumbers.PASSIVES.contains(HammerNumbers.SYNTHESIS));
    }

    @Test
    void theStanceStillLetsMostOfABlowThrough() {
        assertTrue(HammerNumbers.ANVIL_STANCE_FACTOR >= 0.75 && HammerNumbers.ANVIL_STANCE_FACTOR < 1.0);
    }
}
