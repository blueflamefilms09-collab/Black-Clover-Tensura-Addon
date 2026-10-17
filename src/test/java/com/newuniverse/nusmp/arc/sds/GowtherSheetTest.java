package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcCharterRules;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.sds.kit.FourSinsStory;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a typo in a Charter skill id, a floor or a prerequisite would silently break the character, and Gowther must lend nothing that dominates a mind, so his record is checked without loading an entity class. */
class GowtherSheetTest {
    @Test
    void gowtherIsATierThreeBossOfTheSinsWithinTheHealthBand() {
        ArcCharacter c = GowtherSheet.CHARACTER;
        assertEquals("gowther", c.id());
        assertEquals("Gowther, the Goat's Sin of Lust", c.fullName());
        assertTrue(c.isBoss());
        assertEquals(ArcSaga.SEVEN_DEADLY_SINS, c.saga());
        assertEquals(3, c.tier());
        assertTrue(c.health() >= 2200 && c.health() <= 3200 && c.health() < 3600);
        assertEquals(GowtherSheet.RGB, c.color());
        assertEquals(GowtherSheet.ACCENT, c.accent());
        assertEquals("memory", c.magic());
    }

    @Test
    void heStandsOnTheMiddleFloorsOfEitherRoad() {
        ArcCharacter c = GowtherSheet.CHARACTER;
        assertTrue(c.standsOn(40, Polarity.HOLY) && c.standsOn(-80, Polarity.ABYSSAL));
        assertFalse(c.standsOn(30, Polarity.HOLY));
        assertFalse(c.standsOn(90, Polarity.ABYSSAL));
        assertTrue(c.themes().contains("haunted_manor"));
    }

    @Test
    void heFollowsTheEyeOfTheMidnightSun() {
        assertEquals(FourSinsStory.PREREQUISITES, GowtherSheet.CHARACTER.prerequisites());
    }

    @Test
    void theGrimoireLendsOnlyRealLawfulSkillsAndNoDomination() throws IOException {
        ArcCharacter c = GowtherSheet.CHARACTER;
        assertTrue(c.hasCharter());
        assertTrue(c.charterSkills().size() >= 6 && c.charterSkills().size() <= 9);
        assertEquals(c.charterSkills(), ArcCharterRules.sanitize(c.charterSkills()));
        Set<String> real = MerlinSheetTest.tensuraSkills();
        for (String s : c.charterSkills()) assertTrue(real.contains(s), s + " is a skill of Tensura 2.0.1.3");
        for (String banned : List.of("tensura:dominate", "tensura:hypnos", "tensura:possession", "tensura:demon_marionette", "tensura:demon_dominate", "tensura:lust"))
            assertFalse(c.charterSkills().contains(banned), banned + " is not lent (no mind control, no Ultimate)");
    }

    @Test
    void hisLinesAreShort() {
        ArcCharacter c = GowtherSheet.CHARACTER;
        for (String line : List.of(c.opening(), c.gates(), c.death())) {
            assertFalse(line.isBlank());
            assertTrue(line.length() <= 140, "short: " + line);
        }
    }

    @Test
    void theLookMatchesTheHitboxSoTheModelIsDrawnAtItsOwnSize() {
        assertEquals(GowtherSheet.HEIGHT, GowtherSheet.LOOK.height(), 1e-6f);
        assertEquals("gowther", GowtherSheet.LOOK.model());
        assertTrue(GowtherSheet.LOOK.glow());
        assertTrue(GowtherSheet.LOOK.growXz() >= 1.0f && GowtherSheet.LOOK.growUp() >= 0.5f);
    }
}
