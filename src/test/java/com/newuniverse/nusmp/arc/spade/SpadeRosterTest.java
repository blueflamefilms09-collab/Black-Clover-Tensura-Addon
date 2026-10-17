package com.newuniverse.nusmp.arc.spade;

import com.newuniverse.nusmp.arc.ArcBossProfile;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcCharterRules;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.charter.CharterTable;
import com.newuniverse.nusmp.entity.zogratis.DevilStyle;
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

/** The two records of the Spade Kingdom Raid: story chain, health band, floors, themes, a lawful Charter Grimoire of real skills, lines in a voice. */
class SpadeRosterTest {
    private static final ArcCharacter MORRIS = SpadeRoster.MORRIS, LUCIFERO = SpadeRoster.LUCIFERO;

    private static Set<String> tensuraSkills() throws IOException {
        Set<String> out = new HashSet<>();
        try (InputStream in = SpadeRosterTest.class.getResourceAsStream("/com/newuniverse/nusmp/charter/tensura_skill_ids.txt")) {
            assertNotNull(in, "the id list is a test resource");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = r.readLine()) != null; ) if (!line.isBlank() && !line.startsWith("#")) out.add("tensura:" + line.trim());
        }
        return out;
    }

    @Test
    void bothAreTierFourBossesOfTheSpadeRaid() {
        for (ArcCharacter c : List.of(MORRIS, LUCIFERO)) {
            assertTrue(c.isBoss() && !c.isNpc(), c.id());
            assertEquals(ArcSaga.SPADE_RAID, c.saga());
            assertEquals(4, c.tier());
            assertEquals("Black Clover", c.source());
            assertTrue(c.health() >= 3000 && c.health() <= 3800, c.id() + " is inside the tier-4 band: " + c.health());
            assertTrue(c.health() < 3600, c.id() + " stays under Lucius, the top of the ladder");
            assertTrue(c.armor() > 0 && c.speed() > 0.2 && c.attack() > 0);
            assertEquals(Devil.LUCIFERO, c.devil(), "Morris is Lucifero's host, Lucifero is the devil");
            assertFalse(c.opening().isBlank() || c.gates().isBlank() || c.death().isBlank(), c.id() + " has its three lines");
            assertTrue(c.id().matches("[a-z][a-z0-9_]*"));
        }
        assertEquals("morris_libardirt", MORRIS.id());
        assertEquals("lucifero", LUCIFERO.id());
        assertEquals("Morris Libardirt", MORRIS.name());
        assertEquals("Lucifero, King of Devils", LUCIFERO.fullName());
        assertTrue(LUCIFERO.health() > MORRIS.health(), "the King is the bigger fight");
        assertEquals("modification", MORRIS.magic());
        assertEquals("gravity", LUCIFERO.magic());
    }

    @Test
    void theStoryChainRunsTheTriadThenMorrisThenTheKing() {
        assertEquals(List.of("dante", "vanica", "zenon"), MORRIS.prerequisites());
        assertEquals(List.of("dante", "vanica", "zenon", "morris_libardirt"), LUCIFERO.prerequisites());
        assertFalse(MORRIS.prerequisites().contains(LUCIFERO.id()), "no loop");
    }

    @Test
    void theFloorsAreTheDeepBossFloorsOfEitherRoad() {
        for (Polarity road : Polarity.values()) {
            assertTrue(MORRIS.standsOn(70, road) && MORRIS.standsOn(80, road) && MORRIS.standsOn(90, road), "Morris on 70 to 90, " + road);
            assertFalse(MORRIS.standsOn(60, road) || MORRIS.standsOn(100, road));
            assertTrue(LUCIFERO.standsOn(90, road) && LUCIFERO.standsOn(100, road), "the King on 90 and 100, " + road);
            assertFalse(LUCIFERO.standsOn(80, road) || LUCIFERO.standsOn(101, road));
        }
        assertTrue(MORRIS.standsOn(-80, Polarity.ABYSSAL), "floors are absolute values: the Abyssal road counts down");
        assertTrue(LUCIFERO.standsOn(-100, Polarity.ABYSSAL), "the likely guardian of the last floor of the Abyssal road");
        assertTrue(LUCIFERO.themes().contains("infernal_foundry") && LUCIFERO.themes().contains("abyss"), "he favours the infernal, abyssal themes");
        assertFalse(MORRIS.themes().isEmpty());
    }

    @Test
    void theColoursAreTheOnesTheModAlreadyUsesForTheirMagic() {
        assertEquals(DevilStyle.color(Devil.LUCIFERO), LUCIFERO.color(), "the gravity violet of the Zogratis family");
        assertEquals(DevilStyle.accent(Devil.LUCIFERO), LUCIFERO.accent());
        assertEquals(0xFF9B58, MORRIS.color(), "Modification Magic's orange from the mod's own books");
        assertNotEquals(MORRIS.color(), LUCIFERO.color());
    }

    @Test
    void theCharterGrimoiresAreLawfulAndMadeOfRealSkills() throws IOException {
        Set<String> real = tensuraSkills();
        assertTrue(real.size() > 300, "the reference list was read");
        for (ArcCharacter c : List.of(MORRIS, LUCIFERO)) {
            assertTrue(c.hasCharter(), c.id());
            List<String> skills = c.charterSkills();
            assertTrue(skills.size() >= 6 && skills.size() <= 9, c.id() + " has " + skills.size() + " (the brief asks 6 to 9)");
            assertEquals(skills.size(), new HashSet<>(skills).size(), c.id() + " lists a skill twice");
            for (String s : skills) {
                assertTrue(real.contains(s), c.id() + ": " + s + " is not in the Tensura id reference");
                assertFalse(CharterTable.NEVER_LENT.contains(s), c.id() + " lends a thief's skill");
            }
            assertTrue(ArcCharterRules.dropped(skills).isEmpty(), "the Charter's laws drop nothing: " + ArcCharterRules.dropped(skills));
            CharterTable.Boss row = ArcCharterRules.toBoss(c);
            assertNotNull(row, c.id());
            assertEquals("LEGEND", row.grade());
            assertEquals(skills, row.skills());
            assertTrue(c.charterChance() > 0 && c.charterChance() <= 0.5);
            assertNull(CharterTable.byId(c.id()), "the id is free of the seven old grimoires");
        }
        assertTrue(LUCIFERO.charterSkills().contains("tensura:gravity_domination") && LUCIFERO.charterSkills().contains("tensura:majesty"));
        assertTrue(MORRIS.charterSkills().contains("tensura:molecular_manipulation"), "the structure-rewriting skill is the scholar's");
    }

    @Test
    void theBuffLadderGivesTierFourItsBarriers() {
        assertEquals(4, ArcBossProfile.forTier(MORRIS.tier()).tier());
        assertEquals(ArcBossProfile.profileFor(MORRIS.id(), 4).bossId(), MORRIS.id());
    }

    @Test
    void theLinesAreInTheirOwnVoices() {
        assertTrue(LUCIFERO.opening().toLowerCase().contains("kneel"), "the King opens with the order");
        assertTrue(MORRIS.opening().toLowerCase().contains("specimen"), "the scholar opens with what he sees");
        for (ArcCharacter c : List.of(MORRIS, LUCIFERO)) {
            for (String line : new String[]{c.opening(), c.gates(), c.death()}) {
                assertTrue(line.length() >= 20 && line.length() <= 200, c.id() + " line length " + line.length());
                assertFalse(line.contains("\n"));
            }
        }
    }
}
