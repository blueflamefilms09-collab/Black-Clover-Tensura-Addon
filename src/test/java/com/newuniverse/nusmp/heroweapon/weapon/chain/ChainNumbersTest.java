package com.newuniverse.nusmp.heroweapon.weapon.chain;

import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a later edit to a number must not be able to break the balance budget of design.md section 5 unnoticed, so this pins every skill row of the Vassal Chain to it and checks the small rules that decide outcomes. */
class ChainNumbersTest {
    @Test
    void everySkillRowIsInsideTheBudget() {
        List<String> bad = Budget.violations(ChainNumbers.ALL);
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void thereAreTenSkillsAndTwoUltimates() {
        assertEquals(10, ChainNumbers.ALL.size());
        assertEquals(2, ChainNumbers.ULTIMATES.size());
        assertTrue(ChainNumbers.ULTIMATES.contains("chain.orochi"));
        assertTrue(ChainNumbers.ULTIMATES.contains("chain.debt_comes_due"));
        for (Budget.Row r : ChainNumbers.ALL) assertTrue(r.id().startsWith("chain."), r.id());
    }

    @Test
    void theOrochiHitsTwiceAndTheTwoHitsFitTheRow() {
        assertEquals(ChainNumbers.OROCHI.total(), 2 * ChainNumbers.OROCHI.raw(), 1e-9);
        assertTrue(ChainNumbers.OROCHI_SQUEEZE > Budget.HURT_WINDOW, "the second hit comes after the hurt-resistance window");
    }

    @Test
    void theHydraAndTheDebtAreSingleHitsOnEachTarget() {
        assertEquals(ChainNumbers.HYDRA.raw(), ChainNumbers.HYDRA.total(), 1e-9);
        assertEquals(ChainNumbers.DEBT.raw(), ChainNumbers.DEBT.total(), 1e-9);
        assertEquals(ChainNumbers.SLOTH_SNAP.raw(), ChainNumbers.SLOTH_SNAP.total(), 1e-9);
    }

    @Test
    void aBossBarelyMovesAndEveryoneElseIsPulledFully() {
        assertEquals(0.25, ChainNumbers.pullWeight(true), 1e-9);
        assertEquals(1.0, ChainNumbers.pullWeight(false), 1e-9);
    }

    @Test
    void theDefenceShieldIsAFifthOfTheHealthAtMost() {
        assertEquals(0, ChainNumbers.absorptionAmp(20));
        assertEquals(1, ChainNumbers.absorptionAmp(40));
        assertTrue(4.0 * (ChainNumbers.absorptionAmp(20) + 1) <= 20 * 0.25 + 1e-9);
        assertTrue(4.0 * (ChainNumbers.absorptionAmp(40) + 1) <= 40 * 0.25 + 1e-9);
    }

    @Test
    void theHydraFanIsFiveLinesAndNothingRunsLongerThanTwentySeconds() {
        assertEquals(5, ChainNumbers.HYDRA_LINES);
        assertTrue(ChainNumbers.HYDRA_SPREAD < 180.0);
        assertTrue(ChainNumbers.DEBT_TICKS <= 400);
        assertTrue(ChainNumbers.SNAP_DELAY <= 400);
        assertTrue(ChainNumbers.OROCHI_TELL + ChainNumbers.OROCHI_SQUEEZE <= 400);
        assertTrue(ChainNumbers.DEFENCE_TICKS <= 400);
    }

    @Test
    void theSlothCostsAreSetAndTheBillRadiusReachesBeyondTheField() {
        assertTrue(ChainNumbers.DEBT_BILL_RADIUS > ChainNumbers.DEBT_RADIUS, "a foe at the edge of the field cannot step out of the bill");
        assertTrue(ChainNumbers.SNAP_KEEP > ChainNumbers.SNAP_RANGE, "a foe cannot dodge the snap by stepping back");
        assertTrue(ChainNumbers.SLOTH_WEIGHT_CORRUPTION > 0 && ChainNumbers.SNAP_CORRUPTION > 0 && ChainNumbers.DEBT_CORRUPTION > 0);
        assertTrue(ChainNumbers.DEBT_CORRUPTION > ChainNumbers.SNAP_CORRUPTION, "the ultimate costs the mind the most");
    }
}
