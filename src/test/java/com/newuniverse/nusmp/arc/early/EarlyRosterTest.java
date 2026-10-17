package com.newuniverse.nusmp.arc.early;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcCharterRules;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The five records: the story chain, the tier bands, the floors, the Charter skills, the themes, the voice. */
class EarlyRosterTest {
    /** The themes the 1.59.0 builders add, by road (docs/labyrinth/mega_v2.md section 5). */
    private static final Set<String> HOLY_ONLY = Set.of("holy_church", "celestial_court", "sky_citadel", "mountain_monastery", "feywild_grove",
            "dwarven_forgehold", "crystal_geode", "sunken_temple");
    private static final Set<String> ABYSSAL_ONLY = Set.of("cursed_archive", "necropolis", "vampire_castle", "haunted_manor", "fungal_underdark",
            "infernal_foundry", "drowned_crypt", "spider_warrens");

    private static Set<String> tensuraSkills() throws Exception {
        Set<String> out = new HashSet<>();
        try (InputStream in = EarlyRosterTest.class.getResourceAsStream("/com/newuniverse/nusmp/charter/tensura_skill_ids.txt")) {
            assertNotNull(in, "the Tensura skill id reference is on the test classpath");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line = r.readLine(); line != null; line = r.readLine()) if (!line.isBlank() && !line.startsWith("#")) out.add("tensura:" + line.trim());
        }
        return out;
    }

    @Test
    void fiveBossesOfTheFirstSagaAreRegisteredWithoutAClash() {
        ArcRoster.clearForTests();
        for (ArcCharacter c : EarlyRoster.all()) assertSame(c, ArcRoster.register(c));
        assertEquals(5, ArcRoster.bosses().size());
        for (ArcCharacter c : EarlyRoster.all()) {
            assertTrue(c.isBoss());
            assertEquals(ArcSaga.ELF_REINCARNATION, c.saga());
            assertEquals("Black Clover", c.source());
            assertSame(c, ArcRoster.byId(c.id()));
        }
        ArcRoster.clearForTests();
    }

    @Test
    void theStoryChainIsInTheOrderTheAnimeMetThem() {
        List<ArcCharacter> all = EarlyRoster.all();
        assertEquals(List.of("revchi_salik", "heath_grice", "lotus_whomalt", "mars", "vetto"), all.stream().map(ArcCharacter::id).toList());
        assertTrue(all.get(0).prerequisites().isEmpty(), "the first chapter's thief opens the story");
        for (int i = 1; i < all.size(); i++) assertEquals(List.of(all.get(i - 1).id()), all.get(i).prerequisites(), all.get(i).id() + " follows the one before");
    }

    @Test
    void healthSitsInTheBandOfItsTier() {
        for (ArcCharacter c : EarlyRoster.all()) {
            if (c.tier() == 1) assertTrue(c.health() >= 700 && c.health() <= 1100, c.id() + " tier 1 health " + c.health());
            else if (c.tier() == 2) assertTrue(c.health() >= 1300 && c.health() <= 2000, c.id() + " tier 2 health " + c.health());
            else assertTrue(false, c.id() + " is tier " + c.tier());
            assertTrue(c.health() < 3600, "Lucius stays the top");
            assertTrue(c.attack() > 0 && c.speed() > 0.2 && c.speed() < 0.45 && c.armor() >= 0, c.id() + " has sane stats");
        }
    }

    @Test
    void everyFloorRangeReachesABossFloor() {
        for (ArcCharacter c : EarlyRoster.all()) {
            assertTrue(c.floorMin() >= 10 && c.floorMax() <= 60 && c.floorMin() <= c.floorMax(), c.id());
            boolean boss = false;
            for (int f = c.floorMin(); f <= c.floorMax(); f++) boss |= f % 10 == 0;
            assertTrue(boss, c.id() + " may stand on a boss floor (every tenth)");
            assertTrue(c.floorMin() % 10 == 0 && c.floorMax() % 10 == 0, c.id() + " names boss floors");
        }
        assertEquals(Polarity.ABYSSAL, EarlyRoster.HEATH_GRICE.road());
        assertEquals(Polarity.HOLY, EarlyRoster.MARS.road());
        assertEquals(Polarity.ABYSSAL, EarlyRoster.VETTO.road());
        assertEquals(60, EarlyRoster.VETTO.floorMax(), "Vetto goes down to floor 60 on the Abyssal road");
        assertTrue(EarlyRoster.REVCHI_SALIK.standsOn(10, Polarity.HOLY) && EarlyRoster.REVCHI_SALIK.standsOn(-10, Polarity.ABYSSAL), "the thief is on both roads");
        assertFalse(EarlyRoster.VETTO.standsOn(30, Polarity.HOLY));
    }

    @Test
    void thereIsAnEarlyBossOnEveryBossFloorOfBothRoadsUpToSixty() {
        for (int f = 10; f <= 60; f += 10) {
            for (Polarity road : Polarity.values()) {
                int floor = f * road.sign();
                boolean any = EarlyRoster.all().stream().anyMatch(c -> c.standsOn(floor, road));
                if (f <= 40 || road == Polarity.ABYSSAL) assertTrue(any, "floor " + floor);
            }
        }
    }

    @Test
    void everyCharterSkillIsARealTensuraIdAndTheCharterLawsKeepAllOfThem() throws Exception {
        Set<String> real = tensuraSkills();
        assertTrue(real.size() > 300);
        for (ArcCharacter c : EarlyRoster.all()) {
            assertTrue(c.hasCharter());
            assertTrue(c.charterSkills().size() >= 6 && c.charterSkills().size() <= 9, c.id() + " lends " + c.charterSkills().size() + " skills");
            for (String s : c.charterSkills()) assertTrue(real.contains(s), c.id() + " lends " + s + ", which is not a Tensura skill id");
            assertEquals(c.charterSkills(), ArcCharterRules.sanitize(c.charterSkills()), c.id() + ": the Charter laws drop nothing");
            assertEquals(c.charterSkills().size(), new HashSet<>(c.charterSkills()).size(), c.id() + " lists a skill twice");
            assertTrue(c.charterChance() > 0 && c.charterChance() <= 0.5);
        }
    }

    @Test
    void themesExistAndNoneBelongsOnlyToTheOtherRoad() {
        Set<String> known = new HashSet<>();
        for (RegionTheme t : RegionTheme.values()) known.add(t.id());
        known.addAll(HOLY_ONLY);
        known.addAll(ABYSSAL_ONLY);
        for (ArcCharacter c : EarlyRoster.all()) {
            assertTrue(c.themes().size() >= 3, c.id() + " favours a few themes");
            for (String t : c.themes()) {
                assertTrue(known.contains(t), c.id() + " names the unknown theme " + t);
                if (c.road() == Polarity.HOLY) assertFalse(ABYSSAL_ONLY.contains(t), c.id() + " is Holy but names " + t);
                if (c.road() == Polarity.ABYSSAL) assertFalse(HOLY_ONLY.contains(t), c.id() + " is Abyssal but names " + t);
            }
        }
    }

    @Test
    void theVoiceIsPlainAndTheRecordIsFinished() {
        for (ArcCharacter c : EarlyRoster.all()) {
            for (String line : new String[]{c.opening(), c.gates(), c.death()}) {
                assertFalse(line.isBlank(), c.id() + " has all three lines");
                assertTrue(line.length() < 140, c.id() + ": a line is short");
                String low = line.toLowerCase();
                for (String banned : new String[]{"claude", "anthropic", "openai", "gpt", "assistant", "language model"}) assertFalse(low.contains(banned), line);
                for (char ch : line.toCharArray()) assertTrue(ch < 128, c.id() + ": plain ASCII in the lines");
            }
            assertNotEquals(c.color(), c.accent());
            assertTrue((c.accent() >> 16 & 255) + (c.accent() >> 8 & 255) + (c.accent() & 255) > (c.color() >> 16 & 255) + (c.color() >> 8 & 255) + (c.color() & 255), c.id() + ": the accent is the lighter colour");
            assertFalse(c.magic().isEmpty());
            assertFalse(c.title().isEmpty());
            assertTrue(c.name().length() >= 4);
        }
    }
}
