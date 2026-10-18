package com.newuniverse.nusmp.elite.hub;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the quest system is a state machine plus a data format, and both break quietly: a kill that does not count, a daily that never resets, a reward paid twice, a quest file with one bad row that
 * takes the rest down. Everything pure is pinned here: the parser on good and bad rows, EVERY shipped quest file (at least 30 templates, unique ids, requirements that exist and form no cycle, a chain
 * that can be started), the kill/floor matching, the day-seeded board, and the full life of a quest (accept, progress, ready, hand in, once) including the save round trip.
 */
class QuestsTest {
    private static final Path QUEST_DIR = Path.of("src/main/resources/data/nusmp/elite/hub/quests");

    private static QuestDef kill(String id, int target, int tier, String road, String faction, String mob) {
        return new QuestDef(id, QuestType.DAILY, ObjectiveKind.KILL, "", "", target, tier, road, faction, mob, "", 0, 100, 0, List.of(), 10);
    }

    private static QuestDef of(String id, QuestType type, ObjectiveKind kind, int target, List<String> requires, int floor, int minFloor) {
        return new QuestDef(id, type, kind, "", "", target, 0, "", "", "any", kind == ObjectiveKind.DELIVER ? "minecraft:iron_ingot" : "", floor, 100, minFloor, requires, 10);
    }

    private static List<QuestDef> shipped() throws IOException {
        List<QuestDef> all = new ArrayList<>();
        try (Stream<Path> files = Files.list(QUEST_DIR)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".json")).sorted().toList()) {
                QuestParser.Result r = QuestParser.parse(Files.readString(p));
                assertTrue(r.problems().isEmpty(), p + ": " + r.problems());
                all.addAll(r.quests());
            }
        }
        return all;
    }

    private static Function<String, QuestDef> resolver(List<QuestDef> defs) {
        return id -> defs.stream().filter(d -> d.id().equals(id)).findFirst().orElse(null);
    }

    // ------------------------------------------------------------------ the data
    @Test
    void theShippedQuestFilesAreCleanAndNumerous() throws IOException {
        List<QuestDef> all = shipped();
        assertTrue(all.size() >= 30, "at least 30 templates, got " + all.size());
        Set<String> ids = new HashSet<>();
        for (QuestDef q : all) assertTrue(ids.add(q.id()), "duplicate id across files: " + q.id());
        long dailies = all.stream().filter(QuestDef::daily).count();
        assertTrue(dailies >= 12 && all.size() - dailies >= 12, "both kinds are well stocked: " + dailies + " daily");
        for (QuestDef q : all) {
            for (String r : q.requires()) assertTrue(ids.contains(r), q.id() + " requires " + r + " which does not exist");
            assertTrue(q.reward() > 0, q.id() + " pays something");
            assertFalse(QuestText.title(q).isEmpty());
            assertFalse(QuestText.label(q).isEmpty());
        }
        for (ObjectiveKind k : ObjectiveKind.values()) assertTrue(all.stream().anyMatch(q -> q.kind() == k), "a quest of kind " + k);
        for (int t = 1; t <= CoinLoot.TIERS; t++) { int tier = t; assertTrue(all.stream().anyMatch(q -> q.tier() == tier), "a quest of tier " + tier); }
        assertTrue(all.stream().anyMatch(q -> q.road().equals("holy")) && all.stream().anyMatch(q -> q.road().equals("abyssal")), "both roads");
        assertTrue(all.stream().anyMatch(q -> q.mob().equals("foundry")), "the Corrupted Foundations hunt of the brief");
        assertTrue(all.stream().anyMatch(q -> !q.faction().isEmpty()), "faction hunts");
    }

    @Test
    void theProgressionChainHasNoCycleAndSomethingToStartWith() throws IOException {
        List<QuestDef> all = shipped();
        Function<String, QuestDef> defs = resolver(all);
        assertTrue(all.stream().anyMatch(q -> !q.daily() && q.requires().isEmpty() && q.minFloor() == 0), "a fresh player can start the chain");
        for (QuestDef q : all) {
            Set<String> seen = new HashSet<>();
            List<String> stack = new ArrayList<>(q.requires());
            while (!stack.isEmpty()) {
                String id = stack.remove(stack.size() - 1);
                assertFalse(id.equals(q.id()), "cycle through " + q.id());
                if (seen.add(id)) stack.addAll(defs.apply(id).requires());
            }
        }
    }

    @Test
    void theParserSkipsBadRowsWithASentenceAndKeepsTheGoodOnes() {
        String json = "{\"quests\":[" +
                "{\"id\":\"ok\",\"type\":\"daily\",\"kind\":\"kill\",\"target\":5,\"reward\":10}," +
                "{\"id\":\"Bad Id\",\"type\":\"daily\",\"kind\":\"kill\",\"target\":5,\"reward\":10}," +
                "{\"id\":\"no_type\",\"kind\":\"kill\",\"target\":5,\"reward\":10}," +
                "{\"id\":\"zero_target\",\"type\":\"daily\",\"kind\":\"kill\",\"target\":0,\"reward\":10}," +
                "{\"id\":\"neg_reward\",\"type\":\"daily\",\"kind\":\"kill\",\"target\":3,\"reward\":-1}," +
                "{\"id\":\"deliver_no_item\",\"type\":\"daily\",\"kind\":\"deliver\",\"target\":3,\"reward\":1}," +
                "{\"id\":\"reach_no_floor\",\"type\":\"progression\",\"kind\":\"reach_floor\",\"reward\":1}," +
                "{\"id\":\"bad_tier\",\"type\":\"daily\",\"kind\":\"kill\",\"target\":3,\"reward\":1,\"tier\":9}," +
                "{\"id\":\"ok\",\"type\":\"daily\",\"kind\":\"kill\",\"target\":7,\"reward\":10}," +
                "{\"id\":\"reach\",\"type\":\"progression\",\"kind\":\"reach_floor\",\"floor\":-12,\"reward\":1}," +
                "42]}";
        QuestParser.Result r = QuestParser.parse(json);
        assertEquals(2, r.quests().size(), "only 'ok' and 'reach' survive: " + r.problems());
        assertEquals(5, r.quests().get(0).target());
        assertEquals("abyssal", r.quests().get(1).effectiveRoad());
        assertEquals(9, r.problems().size(), "one sentence per bad row: " + r.problems());
        assertTrue(QuestParser.parse("{{{ not json").quests().isEmpty());
        assertTrue(QuestParser.parse("[]").quests().isEmpty());
        assertEquals(1, QuestParser.parse("{\"id\":\"solo\",\"type\":\"daily\",\"kind\":\"kill\",\"target\":2,\"reward\":3}").quests().size(), "a lone quest object is a file too");
    }

    // ------------------------------------------------------------------ the rules
    @Test
    void aKillCountsOnlyWhenEveryFilterMatches() {
        QuestDef t1abyss = kill("a", 5, 1, "abyssal", "", "any");
        assertTrue(QuestRules.matches(t1abyss, KillFact.onFloor(-30, "undead", false, false)));
        assertFalse(QuestRules.matches(t1abyss, KillFact.onFloor(30, "undead", false, false)), "wrong road");
        assertFalse(QuestRules.matches(t1abyss, KillFact.onFloor(-250, "undead", false, false)), "wrong tier");
        assertTrue(QuestRules.matches(kill("f", 5, 0, "", "goblin", "any"), KillFact.onFloor(7, "goblin", false, false)));
        assertFalse(QuestRules.matches(kill("f", 5, 0, "", "goblin", "any"), KillFact.onFloor(7, "ogre", false, false)));
        QuestDef foundry = kill("m", 5, 0, "", "", "foundry");
        assertTrue(QuestRules.matches(foundry, KillFact.onFloor(7, "undead", true, false)));
        assertFalse(QuestRules.matches(foundry, KillFact.onFloor(7, "undead", false, false)), "an ordinary mob is not a foundry mob");
        QuestDef boss = kill("b", 1, 0, "", "", "boss");
        assertTrue(QuestRules.matches(boss, KillFact.onFloor(10, "", false, true)));
        assertFalse(QuestRules.matches(boss, KillFact.onFloor(10, "", false, false)));
        assertFalse(QuestRules.matches(of("d", QuestType.DAILY, ObjectiveKind.DELIVER, 3, List.of(), 0, 0), KillFact.onFloor(1, "", false, false)), "a delivery is not fed by kills");
    }

    @Test
    void floorsAndReachesFollowTheRoadAndTheTier() {
        QuestDef clear = new QuestDef("c", QuestType.DAILY, ObjectiveKind.CLEAR_FLOORS, "", "", 3, 1, "holy", "", "any", "", 0, 1, 0, List.of(), 10);
        assertTrue(QuestRules.matchesFloor(clear, 5));
        assertFalse(QuestRules.matchesFloor(clear, -5));
        assertFalse(QuestRules.matchesFloor(clear, 205));
        assertFalse(QuestRules.matchesFloor(clear, 0));
        QuestDef reach = of("r", QuestType.PROGRESSION, ObjectiveKind.REACH_FLOOR, 1, List.of(), -10, 0);
        assertTrue(QuestRules.reaches(reach, -10));
        assertTrue(QuestRules.reaches(reach, -11));
        assertFalse(QuestRules.reaches(reach, -9));
        assertFalse(QuestRules.reaches(reach, 12), "the other road does not count");
        assertEquals(3L, QuestRules.dayOf(3 * 24_000L + 5));
        assertEquals(0L, QuestRules.dayOf(-5));
    }

    @Test
    void theDailyBoardIsStablePerDayChangesAcrossDaysAndRespectsUnlocks() throws IOException {
        List<QuestDef> all = shipped();
        List<QuestDef> d5 = QuestRules.dailyBoard(5, all, 4, 0);
        assertEquals(4, d5.size());
        assertEquals(d5, QuestRules.dailyBoard(5, all, 4, 0), "the same day gives the same board");
        Set<String> union = new HashSet<>();
        for (long day = 0; day < 30; day++) for (QuestDef q : QuestRules.dailyBoard(day, all, 4, 0)) union.add(q.id());
        assertTrue(union.size() > 8, "over a month the board rotates through many quests, saw " + union.size());
        for (QuestDef q : d5) assertTrue(q.daily() && q.minFloor() == 0, "a fresh player is only shown quests of floors they can reach: " + q.id());
        boolean deepSeen = false;
        for (long day = 0; day < 30 && !deepSeen; day++) for (QuestDef q : QuestRules.dailyBoard(day, all, 4, 900)) deepSeen |= q.minFloor() > 0;
        assertTrue(deepSeen, "a deep player sees deep quests");
        assertEquals(2, QuestRules.dailyBoard(1, all, 2, 0).size());
        assertTrue(QuestRules.dailyBoard(1, all, 0, 0).isEmpty());
    }

    @Test
    void theChainOffersOnlyTheNextSteps() throws IOException {
        List<QuestDef> all = shipped();
        List<QuestDef> first = QuestRules.progressionOffers(all, Set.of(), Set.of(), 0, 50);
        assertFalse(first.isEmpty());
        for (QuestDef q : first) assertTrue(q.requires().isEmpty() && !q.daily() && q.minFloor() == 0, q.id() + " should not be offered to a newcomer");
        Set<String> done = new HashSet<>(List.of("road_holy_reach_5"));
        List<QuestDef> next = QuestRules.progressionOffers(all, done, Set.of(), 0, 50);
        assertTrue(next.stream().anyMatch(q -> q.id().equals("road_holy_reach_10")), "finishing a step unlocks the next");
        assertFalse(next.stream().anyMatch(q -> q.id().equals("road_holy_reach_5")), "a done quest is not offered again");
        assertEquals(3, QuestRules.progressionOffers(all, Set.of(), Set.of(), 0, 3).size());
    }

    // ------------------------------------------------------------------ the state machine
    @Test
    void aKillQuestRunsAcceptProgressReadyHandInOnce() {
        QuestDef q = kill("k", 3, 1, "abyssal", "", "any");
        Function<String, QuestDef> defs = id -> id.equals("k") ? q : null;
        PlayerQuestData p = new PlayerQuestData();
        assertEquals(PlayerQuestData.AcceptResult.OK, p.accept(q, 0, 5, true));
        assertEquals(PlayerQuestData.AcceptResult.ALREADY_ACTIVE, p.accept(q, 0, 5, true));
        assertTrue(p.onKill(KillFact.onFloor(30, "", false, false), defs).isEmpty(), "a Holy kill does not count");
        assertEquals(PlayerQuestData.Change.PROGRESSED, p.onKill(KillFact.onFloor(-30, "", false, false), defs).get(0).change());
        p.onKill(KillFact.onFloor(-31, "", false, false), defs);
        assertFalse(p.canTurnIn(q, 0), "not ready at 2/3");
        assertFalse(p.complete(q, 0));
        assertEquals(PlayerQuestData.Change.READY, p.onKill(KillFact.onFloor(-32, "", false, false), defs).get(0).change());
        assertTrue(p.onKill(KillFact.onFloor(-33, "", false, false), defs).isEmpty(), "a ready quest takes no more");
        assertEquals(3, p.find("k").progress());
        assertTrue(p.complete(q, 0));
        assertFalse(p.complete(q, 0), "a quest is paid exactly once");
        assertEquals(PlayerQuestData.AcceptResult.ALREADY_DONE, p.accept(q, 0, 5, true), "a daily is done for the day");
        assertEquals(1, p.totalDone());
    }

    @Test
    void aDailyResetsOnANewDayAndAnOldOneIsDropped() {
        QuestDef q = kill("k", 3, 0, "", "", "any");
        Function<String, QuestDef> defs = id -> id.equals("k") ? q : null;
        PlayerQuestData p = new PlayerQuestData();
        p.rollover(10, defs);
        p.accept(q, 10, 5, true);
        assertTrue(p.rollover(10, defs).isEmpty(), "the same day changes nothing");
        assertEquals(List.of("k"), p.rollover(11, defs), "an unfinished daily of yesterday is dropped");
        assertEquals(0, p.active().size());
        p.accept(q, 11, 5, true);
        p.onKill(KillFact.onFloor(1, "", false, false), defs);
        p.onKill(KillFact.onFloor(1, "", false, false), defs);
        p.onKill(KillFact.onFloor(1, "", false, false), defs);
        assertTrue(p.complete(q, 0));
        assertEquals(PlayerQuestData.AcceptResult.ALREADY_DONE, p.accept(q, 11, 5, true));
        p.rollover(12, defs);
        assertEquals(PlayerQuestData.AcceptResult.OK, p.accept(q, 12, 5, true), "the next day it is on offer again");
        assertEquals(PlayerQuestData.AcceptResult.NOT_OFFERED, new PlayerQuestData().accept(q, 12, 5, false), "a daily that is not on the board cannot be taken");
    }

    @Test
    void aProgressionQuestIsDoneForGoodAndLockedByItsChainAndFloor() {
        QuestDef a = of("a", QuestType.PROGRESSION, ObjectiveKind.KILL, 1, List.of(), 0, 0);
        QuestDef b = of("b", QuestType.PROGRESSION, ObjectiveKind.KILL, 1, List.of("a"), 0, 0);
        QuestDef deep = of("deep", QuestType.PROGRESSION, ObjectiveKind.KILL, 1, List.of(), 0, 50);
        List<QuestDef> defs = List.of(a, b, deep);
        PlayerQuestData p = new PlayerQuestData();
        assertEquals(PlayerQuestData.AcceptResult.LOCKED, p.accept(b, 0, 5, false), "needs a first");
        assertEquals(PlayerQuestData.AcceptResult.LOCKED, p.accept(deep, 0, 5, false), "needs floor 50");
        p.accept(a, 0, 5, false);
        p.onKill(KillFact.onFloor(1, "", false, false), resolver(defs));
        assertTrue(p.complete(a, 0));
        p.rollover(99, resolver(defs));
        assertEquals(PlayerQuestData.AcceptResult.ALREADY_DONE, p.accept(a, 99, 5, false), "never again, whatever the day");
        assertEquals(PlayerQuestData.AcceptResult.OK, p.accept(b, 99, 5, false));
        p.onFloorEntered(51, resolver(defs));
        assertEquals(PlayerQuestData.AcceptResult.OK, p.accept(deep, 99, 5, false), "floor 51 unlocked it");
    }

    @Test
    void floorQuestsFeedFromFloorEventsAndAReachedFloorCountsAtOnce() {
        QuestDef reach = of("r", QuestType.PROGRESSION, ObjectiveKind.REACH_FLOOR, 1, List.of(), 8, 0);
        QuestDef clear = of("c", QuestType.PROGRESSION, ObjectiveKind.CLEAR_FLOORS, 2, List.of(), 0, 0);
        List<QuestDef> defs = List.of(reach, clear);
        PlayerQuestData p = new PlayerQuestData();
        p.accept(reach, 0, 5, false);
        p.accept(clear, 0, 5, false);
        assertTrue(p.onFloorEntered(-3, resolver(defs)).isEmpty(), "the other road");
        assertEquals(PlayerQuestData.Change.READY, p.onFloorEntered(8, resolver(defs)).get(0).change());
        assertEquals(PlayerQuestData.Change.PROGRESSED, p.onFloorCleared(7, resolver(defs)).get(0).change());
        assertEquals(PlayerQuestData.Change.READY, p.onFloorCleared(-7, resolver(defs)).get(0).change());
        QuestDef reach2 = of("r2", QuestType.PROGRESSION, ObjectiveKind.REACH_FLOOR, 1, List.of(), 6, 0);
        p.accept(reach2, 0, 5, false);
        assertTrue(p.find("r2").ready(), "the player already stood deeper than 6");
        assertEquals(8, p.maxFloor(true));
        assertEquals(3, p.maxFloor(false), "the Abyssal depth is tracked on its own road");
    }

    @Test
    void aDeliveryIsReadyWhenTheBagHoldsEnough() {
        QuestDef d = of("d", QuestType.PROGRESSION, ObjectiveKind.DELIVER, 16, List.of(), 0, 0);
        PlayerQuestData p = new PlayerQuestData();
        p.accept(d, 0, 5, false);
        assertFalse(p.canTurnIn(d, 15));
        assertTrue(p.canTurnIn(d, 16));
        assertFalse(p.complete(d, 15));
        assertTrue(p.complete(d, 99));
        assertFalse(p.canTurnIn(d, 99), "gone once handed in");
    }

    @Test
    void theActiveLimitHolds() {
        PlayerQuestData p = new PlayerQuestData();
        for (int i = 0; i < 3; i++) assertEquals(PlayerQuestData.AcceptResult.OK, p.accept(kill("q" + i, 3, 0, "", "", "any"), 0, 3, true));
        assertEquals(PlayerQuestData.AcceptResult.FULL, p.accept(kill("q9", 3, 0, "", "", "any"), 0, 3, true));
        assertTrue(p.abandon("q1"));
        assertFalse(p.abandon("q1"));
        assertEquals(PlayerQuestData.AcceptResult.OK, p.accept(kill("q9", 3, 0, "", "", "any"), 0, 3, true));
    }

    @Test
    void theSaveRoundTripKeepsEverythingAndSurvivesDamage() {
        QuestDef q = kill("k", 4, 0, "", "", "any");
        Function<String, QuestDef> defs = id -> id.equals("k") ? q : null;
        PlayerQuestData p = new PlayerQuestData();
        p.rollover(7, defs);
        p.accept(q, 7, 5, true);
        p.onKill(KillFact.onFloor(1, "", false, false), defs);
        p.onFloorEntered(-14, defs);
        p.recordPurchase("bread", 7, 2);
        PlayerQuestData back = PlayerQuestData.of(p.toData());
        assertEquals(p.toData(), back.toData());
        assertEquals(1, back.find("k").progress());
        assertEquals(14, back.maxFloor(false));
        assertEquals(2, back.boughtToday("bread", 7));
        assertEquals(0, back.boughtToday("bread", 8), "the counters belong to one day");
        PlayerQuestData bad = PlayerQuestData.of(new PlayerQuestData.Data(
                List.of(new PlayerQuestData.ActiveData("", 5, 5, "ACTIVE", 0), new PlayerQuestData.ActiveData("x", 99, 3, "weird", 0), new PlayerQuestData.ActiveData("x", 1, 3, "ACTIVE", 0)),
                java.util.Arrays.asList("a", null, ""), 5, List.of(), -9, -3, 1, java.util.Collections.singletonMap("z", -4), -7));
        assertEquals(1, bad.active().size(), "the empty id and the duplicate are skipped");
        assertEquals(3, bad.find("x").progress(), "progress is clamped to the target");
        assertTrue(bad.find("x").ready());
        assertEquals(0, bad.maxFloor());
        assertEquals(0, bad.totalDone());
        assertEquals(Set.of("a"), bad.completed());
        assertNotNull(PlayerQuestData.of(null));
        assertFalse(new PlayerQuestData().hasContent());
    }

    // ------------------------------------------------------------------ the words
    @Test
    void theOverlayReadsLikeTheBriefsExample() {
        QuestDef q = kill("k", 50, 1, "abyssal", "", "any");
        assertEquals("Tier 1 Abyssal Kills", QuestText.label(q));
        assertEquals("0/50 Tier 1 Abyssal Kills", QuestText.progressLine(0, 50, QuestText.label(q)));
        assertEquals("Defeat 50 Tier 1 Abyssal monsters", QuestText.title(q));
        assertEquals("Foundry Kills", QuestText.label(kill("m", 50, 0, "", "", "foundry")));
        assertEquals("Defeat 50 Foundry monsters", QuestText.title(kill("m", 50, 0, "", "", "foundry")));
        assertEquals("Reach Floor -12", QuestText.label(of("r", QuestType.PROGRESSION, ObjectiveKind.REACH_FLOOR, 1, List.of(), -12, 0)));
        assertEquals("Iron Ingot Delivered", QuestText.label(of("d", QuestType.DAILY, ObjectiveKind.DELIVER, 3, List.of(), 0, 0)));
        QuestDef titled = new QuestDef("t", QuestType.DAILY, ObjectiveKind.KILL, "Defeat {n} Corrupted Foundations mobs (Tier {tier})", "", 50, 1, "", "", "foundry", "", 0, 1, 0, List.of(), 10);
        assertEquals("Defeat 50 Corrupted Foundations mobs (Tier 1)", QuestText.title(titled));
        assertEquals("+12", QuestText.floorLabel(12));
    }
}
