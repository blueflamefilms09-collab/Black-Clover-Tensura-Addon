package com.newuniverse.nusmp.heroweapon.weapon.mirror;

import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a later edit to a number must not be able to break the balance budget of design.md section 5 unnoticed, so this pins every skill row of the Vassal Mirror to it and checks the small rules that decide outcomes. */
class MirrorNumbersTest {
    @Test
    void everySkillRowIsInsideTheBudget() {
        List<String> bad = Budget.violations(MirrorNumbers.ALL);
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void thereAreTenSkillsAndOneUltimate() {
        assertEquals(10, MirrorNumbers.ALL.size());
        assertEquals(1, MirrorNumbers.ULTIMATES.size());
        assertTrue(MirrorNumbers.ULTIMATES.contains("mirror.kaleidoscope"));
        for (Budget.Row r : MirrorNumbers.ALL) assertTrue(r.id().startsWith("mirror."), r.id());
    }

    @Test
    void theBiggestRayAndItsEchoStayUnderTheHeavyCap() {
        double top = MirrorNumbers.rayDamage(100);
        assertEquals(MirrorNumbers.PRISM_RAY.raw(), top, 1e-9, "the table's raw is the full-gleam ray");
        assertTrue(top + top * MirrorNumbers.AFTERIMAGE_SCALE <= MirrorNumbers.PRISM_RAY.total() + 1e-9, "ray plus the image's echo fits the total");
        assertTrue(MirrorNumbers.PRISM_RAY.total() <= 20.0);
    }

    @Test
    void aRayIsWorthMoreAsTheGleamFillsAndTheGleamIsClamped() {
        assertEquals(7.0, MirrorNumbers.rayDamage(0), 1e-9);
        assertEquals(10.0, MirrorNumbers.rayDamage(50), 1e-9);
        assertEquals(13.0, MirrorNumbers.rayDamage(100), 1e-9);
        assertEquals(13.0, MirrorNumbers.rayDamage(500), 1e-9, "more than a full glass is worth no more");
        assertEquals(7.0, MirrorNumbers.rayDamage(-20), 1e-9);
    }

    @Test
    void theKaleidoscopeStaysUnderTheUltimateCapOverAllItsPulses() {
        assertTrue(MirrorNumbers.KALEIDO_PULSES * MirrorNumbers.KALEIDOSCOPE.raw() <= 32.0);
        assertEquals(MirrorNumbers.KALEIDO_PULSES * MirrorNumbers.KALEIDOSCOPE.raw(), MirrorNumbers.KALEIDOSCOPE.total(), 1e-9);
    }

    @Test
    void bashPlusItsEchoFitsTheOrdinaryTotal() {
        assertEquals(MirrorNumbers.BASH.raw() * (1 + MirrorNumbers.AFTERIMAGE_SCALE), MirrorNumbers.BASH.total(), 1e-9);
        assertTrue(MirrorNumbers.BASH.total() <= 12.0);
    }

    @Test
    void theAbsorptionShieldIsAboutAFifthOfTheHealthAtMost() {
        assertEquals(0, MirrorNumbers.absorptionAmp(20));
        assertEquals(0, MirrorNumbers.absorptionAmp(39.9));
        assertEquals(1, MirrorNumbers.absorptionAmp(40));
        double hp = 20.0;
        assertTrue(4.0 * (MirrorNumbers.absorptionAmp(hp) + 1) <= hp * 0.25 + 1e-9, "a shield never passes a quarter of the health it protects (the heal limit)");
        double big = 40.0;
        assertTrue(4.0 * (MirrorNumbers.absorptionAmp(big) + 1) <= big * 0.25 + 1e-9);
    }

    @Test
    void theSpellWardCutsFifteenPercentAndTheImageSoaksThirtyFivePercentOfTheHealthAtMost() {
        assertEquals(8.5f, MirrorNumbers.wardCut(10f), 1e-6f);
        assertEquals(0f, MirrorNumbers.wardCut(0f), 1e-6f);
        assertEquals(7f, MirrorNumbers.imageSoak(30f, 20f), 1e-6f, "35 percent of 20 health");
        assertEquals(4f, MirrorNumbers.imageSoak(4f, 20f), 1e-6f, "a small blow is soaked whole");
        assertEquals(0f, MirrorNumbers.imageSoak(-3f, 20f), 1e-6f);
    }

    @Test
    void aReboundTurnsBackHalfCappedPerBlowAndByTheBudget() {
        assertEquals(5f, MirrorNumbers.reboundBack(10f, 20.0), 1e-6f);
        assertEquals(12f, MirrorNumbers.reboundBack(100f, 20.0), 1e-6f, "the per-blow cap");
        assertEquals(3f, MirrorNumbers.reboundBack(100f, 3.0), 1e-6f, "what is left of the budget");
        assertEquals(0f, MirrorNumbers.reboundBack(100f, 0.0), 1e-6f);
        assertEquals(0f, MirrorNumbers.reboundBack(-4f, 20.0), 1e-6f);
        assertTrue(MirrorNumbers.reboundBack(10f, 20.0) <= 10f, "never more than the damage taken");
    }

    @Test
    void onlyPlainDamagingEffectsAreSentBackAndSlownessOnlyUpToLevelTwo() {
        for (String ok : new String[]{"poison", "wither", "weakness", "hunger", "mining_fatigue", "unluck"}) assertTrue(MirrorNumbers.returnable(ok, 0), ok);
        assertTrue(MirrorNumbers.returnable("slowness", 1));
        assertFalse(MirrorNumbers.returnable("slowness", 2));
        for (String no : new String[]{"paralysis", "petrification", "levitation", "blindness", "darkness", "nausea", "stun", ""}) assertFalse(MirrorNumbers.returnable(no, 0), no);
    }

    @Test
    void theEnvySkillsAreBoundedInTheirCopies() {
        assertTrue(MirrorNumbers.COVET_MAX <= 3);
        assertTrue(MirrorNumbers.COVET_TICKS <= 200);
        assertTrue(MirrorNumbers.COVET_AMP_CAP <= 2);
        assertTrue(MirrorNumbers.RESSENT_TICKS <= 100);
        assertTrue(MirrorNumbers.RESSENT_MAX <= 6);
    }

    @Test
    void anEchoAndEveryPulseClearTheHurtResistanceWindow() {
        assertTrue(MirrorNumbers.AFTERIMAGE_DELAY > Budget.HURT_WINDOW);
        assertTrue(MirrorNumbers.KALEIDO_EVERY > Budget.HURT_WINDOW);
    }

    @Test
    void nothingRunsLongerThanTwentySeconds() {
        assertTrue(MirrorNumbers.PANE_LIFE <= 400);
        assertTrue(MirrorNumbers.STARDUST_TICKS <= 400);
        assertTrue(MirrorNumbers.IMAGE_LIFE <= 400);
        assertTrue((MirrorNumbers.KALEIDO_PULSES - 1) * MirrorNumbers.KALEIDO_EVERY + MirrorNumbers.KALEIDO_TELL <= 400);
    }
}
