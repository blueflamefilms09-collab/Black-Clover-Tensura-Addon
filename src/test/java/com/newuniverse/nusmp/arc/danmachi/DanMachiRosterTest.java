package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcCharterRules;
import com.newuniverse.nusmp.arc.ArcRole;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.charter.CharterTable;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the four records of {@link DanMachiRoster} are numbers that other systems read blind (the floor-boss picker, the story chain, the Charter table, the boss
 * bar colours). A health outside the band of its tier, a floor gap on the Dungeon road, a story chain with a hole, or a grimoire skill that is not a real Tensura
 * skill would each be a bug that only shows in the game, so each is checked here against the real data (the shipped list of Tensura skill ids, the real theme enum).
 */
class DanMachiRosterTest {
    private static final double[][] BANDS = {{700, 1100}, {1300, 2000}, {2200, 3200}, {3000, 3800}};

    private static Set<String> realSkills() throws IOException {
        Set<String> out = new HashSet<>();
        try (InputStream in = DanMachiRosterTest.class.getResourceAsStream("/com/newuniverse/nusmp/charter/tensura_skill_ids.txt")) {
            assertNotNull(in, "the list of real Tensura skill ids is on the test classpath");
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                String t = line.trim();
                if (!t.isEmpty() && !t.startsWith("#")) out.add(t);
            }
        }
        return out;
    }

    @Test
    void fourBossesOfTheDanMachiSagaInStoryOrder() {
        List<ArcCharacter> b = DanMachiRoster.bosses();
        assertEquals(List.of("minotaur", "black_goliath", "amphisbaena", "udaeus"), b.stream().map(ArcCharacter::id).toList());
        for (ArcCharacter c : b) {
            assertEquals(ArcRole.BOSS, c.role(), c.id());
            assertEquals(ArcSaga.DANMACHI, c.saga(), c.id());
            assertEquals("DanMachi", c.source(), c.id());
            assertFalse(c.title().isEmpty(), c.id());
            assertTrue(c.id().matches("[a-z][a-z0-9_]*"), c.id());
        }
    }

    @Test
    void theHealthIsInTheBandOfTheTier() {
        for (ArcCharacter c : DanMachiRoster.bosses()) {
            double[] band = BANDS[c.tier() - 1];
            assertTrue(c.health() >= band[0] && c.health() <= band[1], c.id() + " tier " + c.tier() + " health " + c.health());
            assertTrue(c.attack() > 0 && c.speed() > 0.1 && c.speed() < 0.5 && c.armor() >= 0, c.id());
        }
        List<Integer> tiers = DanMachiRoster.bosses().stream().map(ArcCharacter::tier).toList();
        assertEquals(List.of(1, 2, 2, 3), tiers, "the tiers climb with the floors");
    }

    @Test
    void theFloorsCoverTheDungeonFromTheFifthToTheFiftiethOnBothRoads() {
        for (Polarity road : Polarity.values()) {
            for (int floor = 5; floor <= 50; floor++) {
                final int f = floor;
                assertTrue(DanMachiRoster.bosses().stream().anyMatch(c -> c.standsOn(f, road)), "floor " + f + " on " + road + " has a DanMachi monster to stand on it");
            }
            assertFalse(DanMachiRoster.bosses().stream().anyMatch(c -> c.standsOn(4, road)), "none stands above the fifth floor");
            assertFalse(DanMachiRoster.bosses().stream().anyMatch(c -> c.standsOn(51, road)), "none stands below the fiftieth");
        }
        for (ArcCharacter c : DanMachiRoster.bosses()) assertEquals(null, c.road(), c.id() + " stands on either road");
    }

    @Test
    void theFloorsGrowWithTheStory() {
        List<ArcCharacter> b = DanMachiRoster.bosses();
        for (int i = 1; i < b.size(); i++) {
            assertTrue(b.get(i).floorMin() > b.get(i - 1).floorMin(), b.get(i).id() + " starts deeper than " + b.get(i - 1).id());
            assertTrue(b.get(i).floorMax() > b.get(i - 1).floorMax(), b.get(i).id() + " reaches deeper than " + b.get(i - 1).id());
            assertTrue(b.get(i).floorMin() <= b.get(i - 1).floorMax() + 1, "no hole between " + b.get(i - 1).id() + " and " + b.get(i).id());
        }
    }

    @Test
    void theStoryIsAChainInsideTheSaga() {
        List<ArcCharacter> b = DanMachiRoster.bosses();
        assertTrue(b.get(0).prerequisites().isEmpty(), "the first monster has nothing before it in this saga (the lead may chain the saga behind another)");
        assertEquals(List.of("minotaur", "goliath"), b.get(1).prerequisites(), "the irregular giant follows the Minotaur and the stone Goliath of the floors");
        for (int i = 2; i < b.size(); i++) assertEquals(List.of(b.get(i - 1).id()), b.get(i).prerequisites(), b.get(i).id());
        for (ArcCharacter c : b) assertEquals("", c.arena(), c.id() + " is fought in its saga's arena, the Pit");
    }

    @Test
    void everyGrimoireIsLawfulAndMadeOfRealTensuraSkills() throws IOException {
        Set<String> real = realSkills();
        for (ArcCharacter c : DanMachiRoster.bosses()) {
            List<String> skills = c.charterSkills();
            assertTrue(skills.size() >= 6 && skills.size() <= 9, c.id() + " lends " + skills.size() + " skills (six to nine)");
            assertEquals(skills.size(), new HashSet<>(skills).size(), c.id() + " has no duplicate");
            for (String id : skills) {
                assertTrue(id.startsWith("tensura:"), id);
                assertTrue(real.contains(id.substring("tensura:".length())), c.id() + ": " + id + " is not a Tensura skill id");
                assertFalse(CharterTable.NEVER_LENT.contains(id), c.id() + ": " + id + " is never lent");
            }
            assertTrue(ArcCharterRules.dropped(skills).isEmpty(), c.id() + " loses nothing to the Charter laws");
            assertNotNull(ArcCharterRules.toBoss(c), c.id());
            assertTrue(c.charterChance() > 0.0 && c.charterChance() <= 0.5, c.id());
        }
    }

    @Test
    void themesAreRealThemesAndTheLinesAreShort() {
        for (ArcCharacter c : DanMachiRoster.bosses()) {
            assertFalse(c.themes().isEmpty(), c.id());
            for (String t : c.themes()) assertEquals(t, RegionTheme.fromId(t).id(), c.id() + ": " + t + " is not a theme of the Dungeon");
            for (String line : new String[]{c.opening(), c.gates(), c.death()}) {
                assertFalse(line.isBlank(), c.id());
                assertTrue(line.length() <= 90, c.id() + ": a line should be short: " + line);
            }
        }
    }

    @Test
    void theColoursAreDistinctSoTheBarsAreTold() {
        Set<Integer> mains = new HashSet<>();
        for (ArcCharacter c : DanMachiRoster.bosses()) assertTrue(mains.add(c.color()), c.id() + " shares its main colour");
        for (ArcCharacter c : DanMachiNpcs.all()) assertTrue(mains.add(c.color()), c.id() + " shares its main colour with another character of the saga");
    }

    @Test
    void theRegistryTakesAllTenAndTheFriendsAskForTheRightMonsters() {
        ArcRoster.clearForTests();
        DanMachiRoster.registerRecords();
        DanMachiRoster.registerRecords();
        assertEquals(4, ArcRoster.bosses().size());
        assertEquals(6, ArcRoster.npcs().size());
        assertEquals(10, ArcRoster.ofSaga(ArcSaga.DANMACHI).size());
        for (QuestBook.Request r : QuestBook.requests()) {
            ArcCharacter npc = ArcRoster.byId(r.npcId()), boss = ArcRoster.byId(r.bossId());
            assertNotNull(npc, r.npcId());
            assertNotNull(boss, r.bossId());
            assertTrue(npc.isNpc());
            assertTrue(boss.isBoss());
        }
        ArcRoster.clearForTests();
    }
}
