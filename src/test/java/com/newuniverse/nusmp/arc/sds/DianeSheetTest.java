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

/** Why: a typo in a Charter skill id, a floor or a prerequisite would silently break the character, so her record is checked without loading an entity class. */
class DianeSheetTest {
    @Test
    void dianeIsATierThreeBossOfTheSinsWithinTheHealthBand() {
        ArcCharacter c = DianeSheet.CHARACTER;
        assertEquals("diane", c.id());
        assertEquals("Diane, the Serpent's Sin of Envy", c.fullName());
        assertTrue(c.isBoss());
        assertEquals(ArcSaga.SEVEN_DEADLY_SINS, c.saga());
        assertEquals(3, c.tier());
        assertTrue(c.health() >= 2200 && c.health() <= 3200 && c.health() < 3600);
        assertEquals(DianeSheet.RGB, c.color());
        assertEquals(DianeSheet.ACCENT, c.accent());
        assertEquals("earth", c.magic());
        assertTrue(DianeSheet.SPEED < 0.3 && DianeSheet.ARMOR >= 10, "the slowest and hardest of the four");
    }

    @Test
    void sheStandsOnTheFirstFloorsOfEitherRoadBecauseSheHasTheMostRoom() {
        ArcCharacter c = DianeSheet.CHARACTER;
        assertTrue(c.standsOn(20, Polarity.HOLY) && c.standsOn(-60, Polarity.ABYSSAL));
        assertFalse(c.standsOn(10, Polarity.HOLY));
        assertFalse(c.standsOn(70, Polarity.ABYSSAL));
        assertTrue(c.themes().contains("mountain_monastery"));
    }

    @Test
    void sheFollowsTheEyeOfTheMidnightSun() {
        assertEquals(FourSinsStory.PREREQUISITES, DianeSheet.CHARACTER.prerequisites());
    }

    @Test
    void theGrimoireLendsOnlyRealLawfulSkills() throws IOException {
        ArcCharacter c = DianeSheet.CHARACTER;
        assertTrue(c.hasCharter());
        assertTrue(c.charterSkills().size() >= 6 && c.charterSkills().size() <= 9);
        assertEquals(c.charterSkills(), ArcCharterRules.sanitize(c.charterSkills()));
        Set<String> real = MerlinSheetTest.tensuraSkills();
        for (String s : c.charterSkills()) assertTrue(real.contains(s), s + " is a skill of Tensura 2.0.1.3");
    }

    @Test
    void herLinesAreShort() {
        ArcCharacter c = DianeSheet.CHARACTER;
        for (String line : List.of(c.opening(), c.gates(), c.death())) {
            assertFalse(line.isBlank());
            assertTrue(line.length() <= 140, "short: " + line);
        }
    }

    @Test
    void theLookIsTheHumanSizeAndItsCullingBoxIsTheGiants() {
        assertEquals(DianeSheet.HEIGHT, DianeSheet.LOOK.height(), 1e-6f);
        assertEquals("diane", DianeSheet.LOOK.model());
        assertTrue(DianeSheet.LOOK.glow());
        assertTrue(DianeSheet.LOOK.growXz() >= 2.0f && DianeSheet.LOOK.growUp() >= 1.5f, "a giant's hammer and arms reach far past the human hitbox");
        assertTrue(DianeSheet.LOOK.shadow() >= 0.9f, "a giant's shadow");
    }
}
