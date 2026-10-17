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

/** Why: a typo in a Charter skill id, a floor or a prerequisite would silently break the character, so his record is checked without loading an entity class. */
class KingSheetTest {
    @Test
    void kingIsATierThreeBossOfTheSinsWithinTheHealthBand() {
        ArcCharacter c = KingSheet.CHARACTER;
        assertEquals("king", c.id());
        assertEquals("King, the Grizzly's Sin of Sloth", c.fullName());
        assertTrue(c.isBoss());
        assertEquals(ArcSaga.SEVEN_DEADLY_SINS, c.saga());
        assertEquals(3, c.tier());
        assertTrue(c.health() >= 2200 && c.health() <= 3200 && c.health() < 3600);
        assertEquals(KingSheet.RGB, c.color());
        assertEquals(KingSheet.ACCENT, c.accent());
        assertEquals("plant", c.magic());
    }

    @Test
    void heStandsOnTheMiddleFloorsOfEitherRoad() {
        ArcCharacter c = KingSheet.CHARACTER;
        assertTrue(c.standsOn(30, Polarity.HOLY) && c.standsOn(-70, Polarity.ABYSSAL));
        assertFalse(c.standsOn(20, Polarity.HOLY));
        assertFalse(c.standsOn(80, Polarity.ABYSSAL));
        assertTrue(c.themes().contains("feywild_grove"));
    }

    @Test
    void heFollowsTheEyeOfTheMidnightSun() {
        assertEquals(FourSinsStory.PREREQUISITES, KingSheet.CHARACTER.prerequisites());
    }

    @Test
    void theGrimoireLendsOnlyRealLawfulSkills() throws IOException {
        ArcCharacter c = KingSheet.CHARACTER;
        assertTrue(c.hasCharter());
        assertTrue(c.charterSkills().size() >= 6 && c.charterSkills().size() <= 9);
        assertEquals(c.charterSkills(), ArcCharterRules.sanitize(c.charterSkills()));
        Set<String> real = MerlinSheetTest.tensuraSkills();
        for (String s : c.charterSkills()) assertTrue(real.contains(s), s + " is a skill of Tensura 2.0.1.3");
    }

    @Test
    void hisLinesAreShort() {
        ArcCharacter c = KingSheet.CHARACTER;
        for (String line : List.of(c.opening(), c.gates(), c.death())) {
            assertFalse(line.isBlank());
            assertTrue(line.length() <= 140, "short: " + line);
        }
    }

    @Test
    void theLookMatchesTheHitboxSoTheModelIsDrawnAtItsOwnSize() {
        assertEquals(KingSheet.HEIGHT, KingSheet.LOOK.height(), 1e-6f);
        assertEquals("king", KingSheet.LOOK.model());
        assertTrue(KingSheet.LOOK.glow());
        assertTrue(KingSheet.LOOK.growXz() >= 1.0f && KingSheet.LOOK.growUp() >= 0.5f, "wings and a spear reach past the hitbox");
    }
}
