package com.newuniverse.nusmp.heroweapon.weapon.instrument;

import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a later edit to a number must not be able to break the balance budget of design.md section 5 unnoticed, so this pins every skill row of the Vassal Musical Instrument to it and checks the rules that turn harmony into numbers. */
class InstrumentNumbersTest {
    @Test
    void everySkillRowIsInsideTheBudget() {
        List<String> bad = Budget.violations(InstrumentNumbers.ALL);
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void thereAreTenSkillsAndOneUltimate() {
        assertEquals(10, InstrumentNumbers.ALL.size());
        assertEquals(1, InstrumentNumbers.ULTIMATES.size());
        for (Budget.Row r : InstrumentNumbers.ALL) assertTrue(r.id().startsWith("instrument."), r.id());
    }

    @Test
    void theTopStrikeAndTheMelodyMatchTheirRows() {
        assertEquals(InstrumentNumbers.MUSIC_STRIKE.raw(), InstrumentNumbers.strikeRaw(Tempo.MAX), 1e-9);
        assertEquals(6.0, InstrumentNumbers.strikeRaw(0), 1e-9);
        assertEquals(InstrumentNumbers.DEMONS_MELODY.total(), InstrumentNumbers.melodyDamage(99), 1e-9, "no more than three notes count on one target");
        assertEquals(3.0, InstrumentNumbers.melodyDamage(1), 1e-9);
        assertEquals(0.0, InstrumentNumbers.melodyDamage(-2), 1e-9);
    }

    @Test
    void theCrescendoStaysUnderTheUltimateCapAndItsHealUnderAQuarter() {
        double total = InstrumentNumbers.CRESCENDO_PULSES * InstrumentNumbers.CRESCENDO.raw();
        assertEquals(InstrumentNumbers.CRESCENDO.total(), total, 1e-9);
        assertTrue(total <= 32.0);
        assertTrue(InstrumentNumbers.CRESCENDO_PULSES * InstrumentNumbers.CRESCENDO_HEAL_SHARE <= InstrumentNumbers.CRESCENDO.heal() + 1e-9);
        assertTrue(InstrumentNumbers.CRESCENDO.heal() <= 0.25);
        assertTrue(InstrumentNumbers.CRESCENDO_EVERY > Budget.HURT_WINDOW, "a pulse after the target's hurt-resistance window");
        assertTrue(InstrumentNumbers.CRESCENDO_MIN_HARMONY <= Tempo.MAX);
    }

    @Test
    void theGluttonHealMatchesItsRow() {
        assertEquals(InstrumentNumbers.GLUTTON.heal(), InstrumentNumbers.GLUTTON_HEAL_SHARE, 1e-9);
    }

    @Test
    void notesStopStrayingFromHarmonyThreeAndPerfectPitchNeverStray() {
        assertEquals(6.0, InstrumentNumbers.spreadDeg(0, false), 1e-9);
        assertEquals(4.0, InstrumentNumbers.spreadDeg(1, false), 1e-9);
        assertEquals(0.0, InstrumentNumbers.spreadDeg(3, false), 1e-9);
        assertEquals(0.0, InstrumentNumbers.spreadDeg(9, false), 1e-9);
        assertEquals(0.0, InstrumentNumbers.spreadDeg(0, true), 1e-9);
    }

    @Test
    void theMelodyFanTightensWithHarmonyAndNeverBelowEight() {
        assertEquals(20.0, InstrumentNumbers.melodyAngle(0, false), 1e-9);
        assertEquals(11.0, InstrumentNumbers.melodyAngle(3, false), 1e-9);
        assertEquals(8.0, InstrumentNumbers.melodyAngle(4, false), 1e-9);
        assertEquals(12.0, InstrumentNumbers.melodyAngle(0, true), 1e-9, "perfect pitch caps the fan at twelve degrees");
        assertEquals(8.0, InstrumentNumbers.melodyAngle(4, true), 1e-9);
    }

    @Test
    void aSongLastsAtMostFortyPercentLongerAndNeverPastTwentySeconds() {
        assertEquals(200, InstrumentNumbers.songTicks(InstrumentNumbers.MARCH_TICKS, 0));
        assertEquals(280, InstrumentNumbers.songTicks(InstrumentNumbers.MARCH_TICKS, 4));
        assertEquals(280, InstrumentNumbers.songTicks(InstrumentNumbers.MARCH_TICKS, 99), "harmony is clamped to four");
        assertTrue(InstrumentNumbers.songTicks(InstrumentNumbers.MARCH_TICKS, 4) <= 400);
        assertTrue(InstrumentNumbers.songTicks(InstrumentNumbers.WAR_TICKS, 4) <= 400);
        assertTrue((InstrumentNumbers.CRESCENDO_PULSES - 1) * InstrumentNumbers.CRESCENDO_EVERY + InstrumentNumbers.CRESCENDO_TELL <= 400);
    }

    @Test
    void speedTwoComesFromHarmonyThree() {
        assertEquals(0, InstrumentNumbers.marchAmp(2));
        assertEquals(1, InstrumentNumbers.marchAmp(3));
        assertEquals(1, InstrumentNumbers.marchAmp(4));
    }
}
