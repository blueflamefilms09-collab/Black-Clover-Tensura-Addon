package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcCharterRules;
import com.newuniverse.nusmp.arc.ArcRole;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.charter.CharterTable;
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

/** The records of Meliodas, Escanor and Ban: real Charter skills under the Charter's laws, stats under Lucius, floors and roads that make sense. */
class SdsCharactersTest {
    private static final List<ArcCharacter> ALL = List.of(SdsCharacters.MELIODAS, SdsCharacters.ESCANOR, SdsCharacters.BAN);

    private static Set<String> tensuraSkills() throws IOException {
        Set<String> out = new HashSet<>();
        try (InputStream in = SdsCharactersTest.class.getResourceAsStream("/com/newuniverse/nusmp/charter/tensura_skill_ids.txt")) {
            assertNotNull(in, "the id list is a test resource of the Charter");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = r.readLine()) != null; ) if (!line.isBlank() && !line.startsWith("#")) out.add("tensura:" + line.trim());
        }
        return out;
    }

    @Test
    void threeBossesOfTheSevenDeadlySinsAtTierThree() {
        assertEquals(List.of("meliodas", "escanor", "ban"), ALL.stream().map(ArcCharacter::id).toList());
        for (ArcCharacter c : ALL) {
            assertEquals(ArcRole.BOSS, c.role(), c.id());
            assertEquals(ArcSaga.SEVEN_DEADLY_SINS, c.saga(), c.id());
            assertEquals(3, c.tier(), c.id());
            assertEquals("The Seven Deadly Sins", c.source());
            assertFalse(c.title().isEmpty());
            assertTrue(c.title().contains("Sin of"), c.title());
            assertTrue(c.fullName().startsWith(c.name()));
        }
    }

    @Test
    void theStatsStayInTheTierThreeBandAndUnderLucius() {
        for (ArcCharacter c : ALL) {
            assertTrue(c.health() >= 2200 && c.health() <= 3200, c.id() + " health " + c.health());
            assertTrue(c.health() < 3600, "Lucius stays the top");
            assertTrue(c.attack() >= 10 && c.attack() <= 30, c.id());
            assertTrue(c.speed() >= 0.25 && c.speed() <= 0.45, c.id());
            assertTrue(c.armor() >= 4 && c.armor() <= 20, c.id());
        }
        assertTrue(SdsCharacters.MELIODAS.health() >= SdsCharacters.ESCANOR.health() && SdsCharacters.ESCANOR.health() > SdsCharacters.BAN.health(),
                "Ban has the least, because he has the Fountain");
        assertTrue(SdsCharacters.BAN.speed() > SdsCharacters.ESCANOR.speed(), "the fox is quicker than the lion");
    }

    @Test
    void everyCharterSkillIsARealTensuraSkillAndTheGrimoireObeysTheLaws() throws IOException {
        Set<String> real = tensuraSkills();
        assertTrue(real.size() > 300, "the reference list was read");
        for (ArcCharacter c : ALL) {
            List<String> skills = c.charterSkills();
            assertTrue(c.hasCharter(), c.id());
            assertTrue(skills.size() >= 6 && skills.size() <= 9, c.id() + " lends " + skills.size());
            assertTrue(skills.size() <= CharterTable.MAX_SKILLS);
            assertEquals(skills.size(), new HashSet<>(skills).size(), c.id() + " lists a skill twice");
            for (String s : skills) {
                assertTrue(real.contains(s), c.id() + ": " + s + " is not in the Tensura id reference");
                assertFalse(CharterTable.NEVER_LENT.contains(s), c.id() + " lends a thief's skill");
            }
            assertEquals(skills, ArcCharterRules.sanitize(skills), "the laws drop nothing");
            assertTrue(ArcCharterRules.dropped(skills).isEmpty());
            CharterTable.Boss row = ArcCharterRules.toBoss(c);
            assertNotNull(row, c.id());
            assertEquals("LEGEND", row.grade());
            assertEquals(c.id(), row.entityPath());
            assertTrue(c.charterChance() > 0.0 && c.charterChance() <= 0.5);
        }
    }

    @Test
    void eachGrimoireSpeaksForItsSin() {
        assertTrue(SdsCharacters.MELIODAS.charterSkills().containsAll(List.of("tensura:reflector", "tensura:wrath", "tensura:black_flame")), "the Full Counter, the rage, the Hellblaze");
        assertTrue(SdsCharacters.ESCANOR.charterSkills().containsAll(List.of("tensura:pride", "tensura:heat_wave", "tensura:solar_flare", "tensura:heat_resistance")), "the sun, and what stands in it");
        assertTrue(SdsCharacters.BAN.charterSkills().containsAll(List.of("tensura:greed", "tensura:envy", "tensura:ultraspeed_regeneration")), "the taking and the Fountain");
    }

    @Test
    void roadsAndFloorsFollowTheBrief() {
        assertNull(SdsCharacters.MELIODAS.road(), "Meliodas is deep on either road");
        assertEquals(Polarity.HOLY, SdsCharacters.ESCANOR.road());
        assertEquals(Polarity.ABYSSAL, SdsCharacters.BAN.road());
        for (ArcCharacter c : ALL) {
            assertTrue(c.floorMin() >= 20 && c.floorMax() <= 90 && c.floorMin() < c.floorMax(), c.id());
            assertFalse(c.standsOn(c.floorMin() - 1, Polarity.HOLY) && c.standsOn(c.floorMin() - 1, Polarity.ABYSSAL));
            assertTrue(c.standsOn(c.floorMin(), c.road() == null ? Polarity.HOLY : c.road()));
            assertTrue(c.standsOn(-c.floorMax(), c.road() == null ? Polarity.ABYSSAL : c.road()), "floors are counted by their absolute number");
            assertFalse(c.standsOn(c.floorMax() + 1, c.road() == null ? Polarity.ABYSSAL : c.road()));
        }
        assertTrue(SdsCharacters.MELIODAS.floorMin() >= 60, "deep");
        assertFalse(SdsCharacters.ESCANOR.standsOn(40, Polarity.ABYSSAL));
        assertFalse(SdsCharacters.BAN.standsOn(40, Polarity.HOLY));
        assertTrue(SdsCharacters.MELIODAS.standsOn(75, Polarity.HOLY) && SdsCharacters.MELIODAS.standsOn(-75, Polarity.ABYSSAL));
    }

    @Test
    void theGroupOpensAfterTheEyeOfTheMidnightSunInAnyOrder() {
        for (ArcCharacter c : ALL) {
            assertFalse(c.prerequisites().isEmpty(), c.id());
            assertEquals(List.of("fana", "patolli", "rades_spirito", "rhya"), c.prerequisites());
            for (String other : ALL.stream().map(ArcCharacter::id).toList()) assertFalse(c.prerequisites().contains(other), c.id() + " does not wait for " + other);
        }
    }

    @Test
    void themesAndLinesAreFilledInAndShort() {
        for (ArcCharacter c : ALL) {
            assertTrue(c.themes().size() >= 3, c.id());
            assertFalse(c.magic().isEmpty());
            for (String line : new String[]{c.opening(), c.gates(), c.death()}) {
                assertFalse(line.isBlank(), c.id());
                assertTrue(line.length() <= 140, c.id() + " keeps it short: " + line);
            }
            assertTrue(c.color() != c.accent());
        }
        assertTrue(SdsCharacters.ESCANOR.themes().contains("radiant"));
        assertTrue(SdsCharacters.MELIODAS.themes().contains("flame"));
        assertTrue(SdsCharacters.BAN.themes().contains("verdant"));
        assertEquals("demon_fire", SdsCharacters.MELIODAS.magic());
        assertEquals("flame", SdsCharacters.ESCANOR.magic());
        assertEquals("world_tree", SdsCharacters.BAN.magic());
    }
}
