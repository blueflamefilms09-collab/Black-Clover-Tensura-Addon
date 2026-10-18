package com.newuniverse.nusmp.heroweapon.weapon.sewingset;

import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a later edit to a number must not be able to break the balance budget of design.md section 5 unnoticed, so this pins every skill row of the Vassal Sewing Set to it and checks the small rules that decide outcomes. */
class SewingNumbersTest {
    @Test
    void everySkillRowIsInsideTheBudget() {
        List<String> bad = Budget.violations(SewingNumbers.ALL);
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void thereAreEightSkillsAndOneUltimate() {
        assertEquals(8, SewingNumbers.ALL.size());
        assertEquals(1, SewingNumbers.ULTIMATES.size());
        for (Budget.Row r : SewingNumbers.ALL) assertTrue(r.id().startsWith("sewing_set."), r.id());
    }

    @Test
    void aVolleyOfThreeIsNineAndNoMore() {
        assertEquals(0.0, SewingNumbers.volleyDamage(0), 1e-9);
        assertEquals(3.0, SewingNumbers.volleyDamage(1), 1e-9);
        assertEquals(9.0, SewingNumbers.volleyDamage(3), 1e-9);
        assertEquals(9.0, SewingNumbers.volleyDamage(30), 1e-9);
        assertEquals(SewingNumbers.NEEDLE_SHOT.total(), SewingNumbers.volleyDamage(SewingNumbers.NEEDLES), 1e-9);
    }

    @Test
    void theCutIsThirtyPercentMoreOnAMarkedFoeAndThatIsTheRowsRaw() {
        assertEquals(10.0, SewingNumbers.cutDamage(false), 1e-9);
        assertEquals(13.0, SewingNumbers.cutDamage(true), 1e-9);
        assertEquals(SewingNumbers.SCISSOR_CUT.raw(), SewingNumbers.cutDamage(true), 1e-9);
    }

    @Test
    void fineStitchLengthensTheMarkByEightyTicks() {
        assertEquals(160, SewingNumbers.foeMarkTicks(false));
        assertEquals(240, SewingNumbers.foeMarkTicks(true));
    }

    @Test
    void theMendHealsAtMostWhatItsRowSaysAndAUltimateHasNoDamage() {
        assertEquals(SewingNumbers.STITCH_MEND.heal(), SewingNumbers.MEND_SHARE, 1e-9);
        assertEquals(0.0, SewingNumbers.ARIADNE.raw(), 1e-9);
        assertTrue(SewingNumbers.ARIADNE_PICKS <= SewingNumbers.ARIADNE.targets());
    }

    @Test
    void nothingRunsLongerThanTwentySeconds() {
        assertTrue(SewingNumbers.BARRIER_TICKS <= 400);
        assertTrue(SewingNumbers.ARIADNE_TELL <= 400);
        assertTrue(SewingNumbers.ALLY_MARK_TICKS <= 400);
        assertTrue(SewingNumbers.PLACE_TICKS <= 400);
        assertTrue(SewingNumbers.NET_TICKS <= 100 && SewingNumbers.BIND_TICKS <= 100, "the law caps control anyway");
    }

    @Test
    void theBarrierCatchesAtMostSixShotsAndPricksOncePerFoe() {
        assertEquals(6, SewingNumbers.BARRIER_CATCHES);
        assertEquals(SewingNumbers.STITCHED_BARRIER.raw(), SewingNumbers.BARRIER_PRICK, 1e-9);
    }
}
