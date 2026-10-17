package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcCharterRules;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.sds.kit.FourSinsStory;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a typo in a Charter skill id, a floor or a prerequisite would silently break the character, so her record is checked without loading an entity class. */
class MerlinSheetTest {
    static Set<String> tensuraSkills() throws IOException {
        Set<String> out = new HashSet<>();
        try (InputStream in = MerlinSheetTest.class.getResourceAsStream("/com/newuniverse/nusmp/charter/tensura_skill_ids.txt")) {
            assertNotNull(in, "the id list is a test resource of the Charter tests");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = r.readLine()) != null; ) if (!line.isBlank() && !line.startsWith("#")) out.add("tensura:" + line.trim());
        }
        return out;
    }

    @Test
    void merlinIsATierThreeBossOfTheSinsWithinTheHealthBand() {
        ArcCharacter c = MerlinSheet.CHARACTER;
        assertEquals("merlin", c.id());
        assertEquals("Merlin, the Boar's Sin of Gluttony", c.fullName());
        assertTrue(c.isBoss());
        assertEquals(ArcSaga.SEVEN_DEADLY_SINS, c.saga());
        assertEquals(3, c.tier());
        assertTrue(c.health() >= 2200 && c.health() <= 3200, "tier 3 band");
        assertTrue(c.health() < 3600, "Lucius stays the top");
        assertEquals(MerlinSheet.RGB, c.color());
        assertEquals(MerlinSheet.ACCENT, c.accent());
    }

    @Test
    void sheStandsOnTheDeepFloorsOfEitherRoad() {
        ArcCharacter c = MerlinSheet.CHARACTER;
        assertTrue(c.standsOn(50, Polarity.HOLY) && c.standsOn(-90, Polarity.ABYSSAL));
        assertFalse(c.standsOn(40, Polarity.HOLY));
        assertFalse(c.standsOn(100, Polarity.HOLY));
        assertTrue(c.themes().contains("cursed_archive") && c.themes().contains("astral"));
    }

    @Test
    void sheFollowsTheEyeOfTheMidnightSun() {
        assertEquals(FourSinsStory.PREREQUISITES, MerlinSheet.CHARACTER.prerequisites());
        assertFalse(MerlinSheet.CHARACTER.prerequisites().contains("merlin"));
    }

    @Test
    void theGrimoireLendsOnlyRealLawfulSkills() throws IOException {
        ArcCharacter c = MerlinSheet.CHARACTER;
        assertTrue(c.hasCharter());
        assertTrue(c.charterSkills().size() >= 6 && c.charterSkills().size() <= 9, "six to nine skills");
        assertEquals(c.charterSkills(), ArcCharterRules.sanitize(c.charterSkills()), "the Charter's laws drop nothing");
        Set<String> real = tensuraSkills();
        for (String s : c.charterSkills()) assertTrue(real.contains(s), s + " is a skill of Tensura 2.0.1.3");
    }

    @Test
    void sheHasTheFourLinesOfAVoiceAndTheyAreShort() {
        ArcCharacter c = MerlinSheet.CHARACTER;
        for (String line : List.of(c.opening(), c.gates(), c.death())) {
            assertFalse(line.isBlank());
            assertTrue(line.length() <= 140, "short: " + line);
        }
    }

    @Test
    void herPlainBlowIsLightBecauseSheIsACaster() {
        assertTrue(MerlinSheet.ATTACK < 20);
        assertTrue(PowerCurve.tuning(20).damageMultiplier() < 1.0);
    }
}
