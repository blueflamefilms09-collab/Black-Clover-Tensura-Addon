package com.newuniverse.nusmp.heroweapon.weapon.harpoon;

import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a later edit to a number must not be able to break the balance budget of design.md section 5 unnoticed, so this pins every skill row of the Vassal Harpoon to it and checks the small rules that decide outcomes. */
class HarpoonNumbersTest {
    @Test
    void everySkillRowIsInsideTheBudget() {
        List<String> bad = Budget.violations(HarpoonNumbers.ALL);
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void thereAreEightSkillsAndOneUltimate() {
        assertEquals(8, HarpoonNumbers.ALL.size());
        assertEquals(1, HarpoonNumbers.ULTIMATES.size());
        for (Budget.Row r : HarpoonNumbers.ALL) assertTrue(r.id().startsWith("harpoon."), r.id());
    }

    @Test
    void theWhaleThrowIsWorthAQuarterMoreOnlyAgainstBigBodies() {
        assertEquals(14.0, HarpoonNumbers.whaleRaw(20.0), 1e-9);
        assertEquals(14.0, HarpoonNumbers.whaleRaw(39.9), 1e-9);
        assertEquals(17.5, HarpoonNumbers.whaleRaw(40.0), 1e-9);
        assertEquals(HarpoonNumbers.WHALE.raw(), HarpoonNumbers.whaleRaw(500.0), 1e-9, "the table's raw is the biggest throw");
    }

    @Test
    void theTwoLegsOfTheDragonAndThePulsesOfTheCrossAddUpToTheirRows() {
        assertEquals(HarpoonNumbers.DRAGON.total(), HarpoonNumbers.DRAGON.raw() + HarpoonNumbers.DRAGON_BACK_RAW, 1e-9);
        assertEquals(HarpoonNumbers.CRUCIFIXION.total(), HarpoonNumbers.CRUCIFY_PULSES * HarpoonNumbers.CRUCIFY_PULSE_RAW, 1e-9);
        assertEquals(HarpoonNumbers.CRUCIFY_PULSE_RAW, HarpoonNumbers.CRUCIFIXION.raw(), 1e-9);
    }

    @Test
    void secondHitsOnOneTargetComeAfterTheHurtResistanceWindow() {
        assertTrue(HarpoonNumbers.DRAGON_TURN > Budget.HURT_WINDOW);
        assertTrue(HarpoonNumbers.CRUCIFY_EVERY > Budget.HURT_WINDOW);
    }

    @Test
    void theShockAndItsArcsFitTheRow() {
        assertEquals(HarpoonNumbers.SHOCK.raw(), 7.0, 1e-9);
        assertTrue(HarpoonNumbers.SHOCK_ARC_RAW <= HarpoonNumbers.SHOCK.raw());
        assertTrue(1 + HarpoonNumbers.SHOCK_ARCS <= HarpoonNumbers.SHOCK.targets());
        assertTrue(HarpoonNumbers.ARC_RAW < HarpoonNumbers.THROW.raw());
    }

    @Test
    void theControlTechniquesStayShort() {
        assertTrue(HarpoonNumbers.CRUCIFY_TICKS <= 100, "the law caps it anyway: 3 s for a player, 5 s for a creature, 1 s for a boss");
        assertTrue(HarpoonNumbers.WHALE_STAGGER <= 20);
        assertTrue(HarpoonNumbers.FISH_STAGGER <= 20);
        assertTrue(HarpoonNumbers.REEL_TICKS <= 40);
        assertTrue((HarpoonNumbers.CRUCIFY_PULSES - 1) * HarpoonNumbers.CRUCIFY_EVERY <= 400);
    }

    @Test
    void theTetherLiveInsideTwentySeconds() {
        assertTrue(TetherBook.CEILING <= 400);
        assertTrue(TetherBook.LIFE <= TetherBook.CEILING);
    }
}
