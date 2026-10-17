package com.newuniverse.nusmp.labyrinth.mega.system;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: {@code quests.json} is hand-written data, and a typo in data fails SILENTLY in the game (a quest with an unknown kind is just never rolled). This test
 * reads the real shipped file the way the game does and fails the build on any complaint, and feeds the parser deliberately broken rows to prove each check
 * fires and that one bad row never costs the good ones.
 */
class QuestParserTest {
    static final Path QUESTS = Path.of("src/main/resources/data/nusmp/mega/system/quests.json");

    static Map<String, Object> shippedTree() throws IOException {
        return MiniJson.object(Files.readString(QUESTS, StandardCharsets.UTF_8));
    }

    static QuestParser.Result shipped() throws IOException { return QuestParser.parse(shippedTree()); }

    static Map<String, Object> tree(String json) { return MiniJson.object(json); }

    @Test void theShippedFileParsesWithNoComplaint() throws IOException {
        QuestParser.Result r = shipped();
        assertTrue(r.problems().isEmpty(), "problems: " + r.problems());
        assertEquals(7, r.templates().size());
    }

    @Test void everyKindOfTheBriefIsShipped() throws IOException {
        Set<QuestKind> kinds = new HashSet<>();
        for (QuestTemplate t : shipped().templates()) kinds.add(t.kind());
        for (QuestKind k : QuestKind.values()) assertTrue(kinds.contains(k), "no shipped quest of kind " + k);
    }

    @Test void theBriefsOwnExampleIsShipped() throws IOException {
        QuestTemplate t = new QuestCatalog(shipped().templates()).get("clear_rooms_no_magic");
        assertNotNull(t, "clear_rooms_no_magic");
        assertEquals(QuestKind.CLEAR_ROOMS, t.kind());
        assertTrue(t.noMagic());
        assertEquals("Clear 5 rooms without taking magic damage", t.englishTitle(5));
    }

    @Test void everyShippedRowSaysWhyAndHasATitleWithItsNumber() throws IOException {
        Map<String, Object> root = shippedTree();
        assertTrue(root.get("why") instanceof String w && w.length() >= 120, "the file needs its WHY");
        for (QuestTemplate t : shipped().templates()) {
            assertTrue(t.why().length() >= 40, t.id() + " needs a why");
            assertFalse(t.title().isEmpty(), t.id() + " needs an English title");
            if (t.kind() != QuestKind.NO_DAMAGE_STRETCH) assertTrue(t.title().contains("{n}") || t.title().contains("{t}"), t.id() + " title shows its number");
            assertTrue(t.weight() > 0, t.id());
        }
    }

    @Test void everyShippedRowPaysAKnownStatAndRespectsItsKind() throws IOException {
        for (QuestTemplate t : shipped().templates()) {
            assertFalse(t.stats().isEmpty(), t.id());
            assertTrue(t.points() >= 1 && t.points() <= 2, t.id());
            if (t.kind() == QuestKind.SURVIVE_AMBUSH) assertEquals(List.of(Stat.SENSE), t.stats(), t.id());
            if (t.kind() == QuestKind.CLEAR_ROOMS) assertTrue(t.stats().contains(Stat.INT), t.id());
            if (t.kind() == QuestKind.NO_DAMAGE_STRETCH) assertEquals(List.of(Stat.VIT), t.stats(), t.id());
            if (t.kind() == QuestKind.SPEED) assertEquals(List.of(Stat.AGI), t.stats(), t.id());
        }
    }

    @Test void idsAreUniqueAndLangSafe() throws IOException {
        Set<String> ids = new HashSet<>();
        for (QuestTemplate t : shipped().templates()) {
            assertTrue(ids.add(t.id()), "duplicate " + t.id());
            assertTrue(t.id().matches("[a-z0-9_]{1,48}"), t.id());
            assertEquals("message.nusmp.mega.system.quest." + t.id(), t.langKey());
        }
    }

    @Test void aMissingOrNonObjectRootIsReported() {
        assertFalse(QuestParser.parse(null).problems().isEmpty());
        assertFalse(QuestParser.parse(tree("{}")).problems().isEmpty());
        assertFalse(QuestParser.parse(tree("{\"quests\": 5}")).problems().isEmpty());
    }

    @Test void anUnknownKindIsReportedAndOnlyThatRowIsDropped() {
        QuestParser.Result r = QuestParser.parse(tree("{\"quests\":[{\"id\":\"a\",\"kind\":\"DANCE\",\"base\":1},{\"id\":\"b\",\"kind\":\"KILL_N\",\"base\":4}]}"));
        assertEquals(1, r.templates().size());
        assertEquals("b", r.templates().get(0).id());
        assertEquals(1, r.problems().size());
        assertTrue(r.problems().get(0).contains("DANCE"), r.problems().get(0));
    }

    @Test void aBadIdAndAMissingBaseAreReported() {
        assertTrue(QuestParser.parse(tree("{\"quests\":[{\"id\":\"Bad Id\",\"kind\":\"KILL_N\",\"base\":4}]}")).templates().isEmpty());
        assertTrue(QuestParser.parse(tree("{\"quests\":[{\"id\":\"x\",\"kind\":\"KILL_N\"}]}")).templates().isEmpty());
        assertTrue(QuestParser.parse(tree("{\"quests\":[{\"id\":\"x\",\"kind\":\"KILL_N\",\"base\":-1}]}")).templates().isEmpty());
        assertTrue(QuestParser.parse(tree("{\"quests\":[\"nope\"]}")).templates().isEmpty());
    }

    @Test void defaultsApplyWhenOptionalFieldsAreAbsent() {
        QuestTemplate t = QuestParser.parse(tree("{\"quests\":[{\"id\":\"x\",\"kind\":\"KILL_N\",\"base\":4}]}")).templates().get(0);
        assertEquals(10, t.weight());
        assertEquals(1, t.min());
        assertEquals(99, t.max());
        assertEquals(0.0, t.perFloor());
        assertFalse(t.noMagic());
        assertEquals(1, t.points());
        assertTrue(t.shadowCore());
        assertEquals(QuestKind.KILL_N.defaultStats(), t.stats());
        assertEquals("Defeat 4 monsters on this floor", t.englishTitle(4));
    }

    @Test void rewardsAreValidated() {
        QuestParser.Result r = QuestParser.parse(tree("{\"quests\":[{\"id\":\"x\",\"kind\":\"KILL_N\",\"base\":4,\"rewards\":{\"stats\":[\"STR\",\"LUCK\",\"str\"],\"points\":99,\"shadowCore\":false}}]}"));
        QuestTemplate t = r.templates().get(0);
        assertEquals(List.of(Stat.STR), t.stats(), "the unknown stat is skipped and the duplicate is folded");
        assertEquals(10, t.points());
        assertFalse(t.shadowCore());
        assertEquals(2, r.problems().size(), r.problems().toString());
    }

    @Test void maxBelowMinIsRaisedAndReported() {
        QuestParser.Result r = QuestParser.parse(tree("{\"quests\":[{\"id\":\"x\",\"kind\":\"KILL_N\",\"base\":4,\"min\":5,\"max\":3}]}"));
        assertEquals(5, r.templates().get(0).max());
        assertEquals(1, r.problems().size());
    }

    @Test void noMagicOnARoomlessKindIsIgnoredWithAComplaint() {
        QuestParser.Result r = QuestParser.parse(tree("{\"quests\":[{\"id\":\"x\",\"kind\":\"SPEED\",\"base\":200,\"noMagic\":true}]}"));
        assertFalse(r.templates().get(0).noMagic());
        assertEquals(1, r.problems().size());
    }

    @Test void aDuplicateIdInOneFileIsReportedAndTheLaterRowWins() {
        QuestParser.Result r = QuestParser.parse(tree("{\"quests\":[{\"id\":\"x\",\"kind\":\"KILL_N\",\"base\":4},{\"id\":\"x\",\"kind\":\"SPEED\",\"base\":99}]}"));
        assertEquals(1, r.templates().size());
        assertEquals(QuestKind.SPEED, r.templates().get(0).kind());
        assertEquals(1, r.problems().size());
    }

    @Test void theCatalogLetsALaterFileOverrideAnEarlierOne() {
        QuestTemplate a = QuestParser.parse(tree("{\"quests\":[{\"id\":\"x\",\"kind\":\"KILL_N\",\"base\":4}]}")).templates().get(0);
        QuestTemplate b = QuestParser.parse(tree("{\"quests\":[{\"id\":\"x\",\"kind\":\"KILL_N\",\"base\":9},{\"id\":\"y\",\"kind\":\"SPEED\",\"base\":9}]}")).templates().get(0);
        QuestTemplate c = QuestParser.parse(tree("{\"quests\":[{\"id\":\"y\",\"kind\":\"SPEED\",\"base\":9}]}")).templates().get(0);
        QuestCatalog cat = new QuestCatalog(List.of(a, c, b));
        assertEquals(2, cat.size());
        assertEquals(9.0, cat.get("x").base());
        assertEquals(9.0, cat.get("nusmp:x").base(), "the namespace is ignored");
        assertNull(cat.get("nope"));
        assertTrue(cat.hasKind(QuestKind.SPEED));
        assertFalse(cat.hasKind(QuestKind.CLEAR_ROOMS));
    }
}
