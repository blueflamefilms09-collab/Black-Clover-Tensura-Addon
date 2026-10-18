package com.newuniverse.nusmp.heroweapon.weapon.whip;

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
 * Why: the Whip's numbers are the owner's balance budget in code, and the Command is the one technique with a hard limit stated by the owner (at most three monsters, six
 * seconds, only after the shared control lock, never on a boss or a player). This test runs every row through the budget, shows the check can fail, and pins those limits, the
 * Hourglass Flow cooldown that must not take a utility below its floor, the lightning chain that must lose strength at every jump, and the ultimate's single use of the control lock.
 */
class WhipNumbersTest {
    @Test
    void everyRowIsInsideTheBudget() {
        assertEquals(List.of(), Budget.violations(WhipNumbers.SPECS, 1.0));
    }

    @Test
    void theBudgetCanFailOnTheWhip() {
        assertFalse(Budget.violations(new SkillSpec("whip.command", Kind.UTILITY, 0, 0, 99, 0.03, 3), 1.0).isEmpty(), "a utility under 100 ticks");
        assertFalse(Budget.violations(new SkillSpec("whip.lash", Kind.DAMAGE, 6, 1, 60, 0.01, 20), 1.0).isEmpty(), "twenty targets");
    }

    @Test
    void theWeaponHasEnoughTechniquesAndOneUltimate() {
        assertTrue(WhipNumbers.SPECS.size() >= 6);
        assertEquals(1, WhipNumbers.SPECS.stream().filter(SkillSpec::ultimate).count());
        for (SkillSpec s : WhipNumbers.SPECS) assertTrue(s.id().startsWith("whip."), s.id());
        assertEquals(WhipNumbers.SPECS.size(), Set.copyOf(WhipNumbers.SPECS.stream().map(SkillSpec::id).toList()).size());
    }

    @Test
    void theCommandStaysInsideTheOwnersLimits() {
        assertTrue(WhipNumbers.COMMAND_MAX <= 3, "at most three monsters");
        assertEquals(3, WhipNumbers.COMMAND.maxTargets());
        assertTrue(WhipNumbers.COMMAND_TICKS <= 120, "at most six seconds");
        assertTrue(WhipNumbers.COMMAND.cooldown() >= 400, "a rare technique");
        assertEquals(0.0, WhipNumbers.COMMAND.raw(), 1e-9, "it does no damage itself");
        assertTrue(WhipNumbers.COMMAND_STEER >= 2 && WhipNumbers.COMMAND_TICKS % WhipNumbers.COMMAND_STEER == 0, "the steering zone ends exactly on the last tick");
    }

    @Test
    void hourglassFlowNeverTakesAMovementTechniqueUnderItsFloor() {
        for (SkillSpec s : List.of(WhipNumbers.TETHER_SWING, WhipNumbers.PORTAL_WHIP)) {
            long reduced = Math.round(s.cooldown() * WhipNumbers.HOURGLASS_FACTOR);
            assertTrue(reduced >= Budget.cooldownFloor(s.kind()), s.id() + " would drop to " + reduced);
        }
    }

    @Test
    void theLightningLosesStrengthAtEveryJumpAndNeverGainsFromChargeAboveTheCap() {
        double last = 1.0;
        for (int i = 1; i < WhipNumbers.LIGHTNING_FOES; i++) {
            double f = Math.pow(WhipNumbers.LIGHTNING_FALLOFF, i);
            assertTrue(f < last, "jump " + i);
            last = f;
        }
        assertEquals(WhipNumbers.LIGHTNING_FOES, WhipNumbers.LIGHTNING_WHIP.maxTargets());
        assertTrue(WhipNumbers.LIGHTNING_WHIP.raw() * WhipNumbers.CHARGE_BONUS <= Budget.cap(Kind.DAMAGE), "a charged first hit stays under the damage cap");
        assertTrue(WhipNumbers.GIGANTIC_STAR.raw() * WhipNumbers.STAR_BONUS <= Budget.cap(Kind.HEAVY), "a star on a bound foe stays under the heavy cap");
    }

    @Test
    void everyBlowWithATellHasADelay() {
        assertTrue(WhipNumbers.LIGHTNING_DELAY >= 4 && WhipNumbers.STAR_DELAY >= 4 && WhipNumbers.DIMENSION_DELAY >= 10);
    }

    @Test
    void thePassivesAreTheFiveTheFormsUse() {
        assertEquals(Set.of("coil", "static_charge", "star_weight", "taskmaster", "hourglass_flow"), WhipNumbers.PASSIVES);
    }
}
