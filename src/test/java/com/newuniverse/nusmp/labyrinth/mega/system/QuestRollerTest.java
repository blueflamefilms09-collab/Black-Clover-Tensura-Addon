package com.newuniverse.nusmp.labyrinth.mega.system;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: which quest a floor gives is the part of the System a player feels most (a fair, varied, sensibly sized objective), and every rule of it is a pure
 * function: determinism per player and floor (no rerolling by re-entering), no repeated kind, scaling with the floor, clamping, weights, and never offering a
 * quest the floor cannot satisfy. Run over thousands of seeds so a rule that holds "usually" fails here.
 */
class QuestRollerTest {
    static QuestCatalog shipped() throws IOException { return new QuestCatalog(QuestParserTest.shipped().templates()); }

    static QuestTemplate t(String id, QuestKind kind, double base, double per, int min, int max, int weight) {
        return new QuestTemplate(id, kind, "", base, per, min, max, weight, false, List.of(), 1, true, "");
    }

    static final QuestRoller.FloorFacts ROOMY = new QuestRoller.FloorFacts(10, true);

    @Test void theSeedIsStableAndDependsOnPlayerAndFloor() {
        UUID a = new UUID(1, 2), b = new UUID(3, 4);
        assertEquals(QuestRoller.seed(a, 5), QuestRoller.seed(a, 5));
        assertNotEquals(QuestRoller.seed(a, 5), QuestRoller.seed(a, 6));
        assertNotEquals(QuestRoller.seed(a, 5), QuestRoller.seed(b, 5));
        assertTrue(QuestRoller.seed(a, 5) != QuestRoller.seed(a, -5), "a Holy floor and the Abyssal floor of the same depth are different floors");
    }

    @Test void thePickIsDeterministicForASeed() throws IOException {
        QuestCatalog cat = shipped();
        for (int floor = 1; floor <= 60; floor++) {
            long seed = QuestRoller.seed(new UUID(7, 9), floor);
            QuestTemplate x = QuestRoller.pick(cat, seed, floor, null, ROOMY);
            QuestTemplate y = QuestRoller.pick(cat, seed, floor, null, ROOMY);
            assertNotNull(x);
            assertSame(x, y);
        }
    }

    @Test void theTargetIsBasePlusPerFloorTimesThreatClamped() {
        QuestTemplate q = t("x", QuestKind.KILL_N, 6, 0.5, 4, 20, 10);
        assertEquals(7, QuestRoller.target(q, 1), "6.5 rounds to 7");
        assertEquals(8, QuestRoller.target(q, 4));
        assertEquals(20, QuestRoller.target(q, 500), "clamped to max");
        QuestTemplate speed = t("s", QuestKind.SPEED, 300, -2, 150, 300, 10);
        assertEquals(298, QuestRoller.target(speed, 1));
        assertEquals(250, QuestRoller.target(speed, 25));
        assertEquals(150, QuestRoller.target(speed, 400), "a negative perFloor shortens it down to min");
        QuestTemplate floor = t("f", QuestKind.KILL_N, 0, 0, 3, 9, 10);
        assertEquals(3, QuestRoller.target(floor, 1), "raised to min");
    }

    @Test void theTargetGrowsWithTheFloorForEveryGrowingShippedQuest() throws IOException {
        for (QuestTemplate q : shipped().all()) {
            if (q.perFloor() <= 0) continue;
            assertTrue(QuestRoller.target(q, 40) >= QuestRoller.target(q, 2), q.id());
            assertTrue(QuestRoller.target(q, 1) >= q.min() && QuestRoller.target(q, 1000) <= q.max(), q.id());
        }
    }

    @Test void aKindIsNeverRepeatedTwiceInARow() throws IOException {
        QuestCatalog cat = shipped();
        int checked = 0;
        for (QuestKind previous : QuestKind.values()) {
            for (int i = 0; i < 2000; i++) {
                QuestTemplate p = QuestRoller.pick(cat, QuestRoller.mix(i * 31L + previous.ordinal()), 10, previous, ROOMY);
                assertNotNull(p);
                assertTrue(previous != p.kind(), "seed " + i);
                checked++;
            }
        }
        assertEquals(5 * 2000, checked);
    }

    @Test void aLoneKindMayRepeatBecauseNothingElseIsLeft() {
        QuestCatalog only = new QuestCatalog(List.of(t("a", QuestKind.KILL_N, 5, 0, 1, 9, 10), t("b", QuestKind.KILL_N, 6, 0, 1, 9, 10)));
        QuestTemplate p = QuestRoller.pick(only, 1L, 10, QuestKind.KILL_N, ROOMY);
        assertNotNull(p);
        assertEquals(QuestKind.KILL_N, p.kind());
    }

    @Test void aFloorWithTooFewRoomsIsNeverAskedToClearMoreThanItHas() throws IOException {
        QuestCatalog cat = shipped();
        QuestRoller.FloorFacts small = new QuestRoller.FloorFacts(2, true);
        for (int threat = 1; threat <= 60; threat++) {
            for (int i = 0; i < 300; i++) {
                QuestTemplate p = QuestRoller.pick(cat, QuestRoller.mix(i * 977L + threat), threat, null, small);
                assertNotNull(p);
                if (p.kind() == QuestKind.CLEAR_ROOMS) assertTrue(QuestRoller.target(p, threat) <= 2, p.id() + " at threat " + threat);
            }
        }
        QuestRoller.FloorFacts none = new QuestRoller.FloorFacts(0, true);
        for (int i = 0; i < 500; i++) assertNotEquals(QuestKind.CLEAR_ROOMS, QuestRoller.pick(cat, i, 3, null, none).kind());
    }

    @Test void theAmbushQuestIsNotOfferedWhenItsSwitchIsOff() throws IOException {
        QuestCatalog cat = shipped();
        QuestRoller.FloorFacts off = new QuestRoller.FloorFacts(10, false);
        for (int i = 0; i < 2000; i++) assertNotEquals(QuestKind.SURVIVE_AMBUSH, QuestRoller.pick(cat, QuestRoller.mix(i), 8, null, off).kind());
        boolean seen = false;
        for (int i = 0; i < 2000; i++) if (QuestRoller.pick(cat, QuestRoller.mix(i), 8, null, ROOMY).kind() == QuestKind.SURVIVE_AMBUSH) seen = true;
        assertTrue(seen, "with the switch on it does come up");
    }

    @Test void weightZeroSwitchesAQuestOff() {
        QuestCatalog cat = new QuestCatalog(List.of(t("on", QuestKind.KILL_N, 5, 0, 1, 9, 10), t("off", QuestKind.SPEED, 200, 0, 1, 999, 0)));
        for (int i = 0; i < 500; i++) assertEquals("on", QuestRoller.pick(cat, i, 3, null, ROOMY).id());
        assertEquals(1, QuestRoller.eligible(cat, 3, ROOMY).size());
    }

    @Test void weightsDecideHowOftenAQuestComes() {
        QuestCatalog cat = new QuestCatalog(List.of(t("heavy", QuestKind.KILL_N, 5, 0, 1, 9, 90), t("light", QuestKind.SPEED, 200, 0, 1, 999, 10)));
        int heavy = 0, n = 20000;
        for (int i = 0; i < n; i++) if (QuestRoller.pick(cat, QuestRoller.mix(i), 3, null, ROOMY).id().equals("heavy")) heavy++;
        assertTrue(heavy > n * 0.86 && heavy < n * 0.94, "heavy came " + heavy + " of " + n);
    }

    @Test void everyShippedKindComesUpAndNoneDominates() throws IOException {
        QuestCatalog cat = shipped();
        Map<QuestKind, Integer> count = new EnumMap<>(QuestKind.class);
        int n = 20000;
        for (int i = 0; i < n; i++) count.merge(QuestRoller.pick(cat, QuestRoller.mix(i), 12, null, ROOMY).kind(), 1, Integer::sum);
        for (QuestKind k : QuestKind.values()) {
            int c = count.getOrDefault(k, 0);
            assertTrue(c > n * 0.05, k + " came only " + c);
            assertTrue(c < n * 0.45, k + " came " + c);
        }
    }

    @Test void anEmptyOrFullyExcludedCatalogGivesNull() {
        assertNull(QuestRoller.pick(QuestCatalog.EMPTY, 1, 3, null, ROOMY));
        QuestCatalog rooms = new QuestCatalog(List.of(t("r", QuestKind.CLEAR_ROOMS, 9, 0, 9, 9, 10)));
        assertNull(QuestRoller.pick(rooms, 1, 3, null, new QuestRoller.FloorFacts(3, true)));
    }

    @Test void theQuestChanceBehavesAtItsEdgesAndRoughlyInBetween() {
        for (int i = 0; i < 200; i++) {
            assertTrue(QuestRoller.hasQuest(i, 1.0));
            assertTrue(QuestRoller.hasQuest(i, 2.0));
            assertFalse(QuestRoller.hasQuest(i, 0.0));
            assertFalse(QuestRoller.hasQuest(i, -1.0));
        }
        int yes = 0, n = 10000;
        for (int i = 0; i < n; i++) if (QuestRoller.hasQuest(QuestRoller.mix(i), 0.3)) yes++;
        assertTrue(yes > n * 0.26 && yes < n * 0.34, "got " + yes);
        assertEquals(QuestRoller.hasQuest(77L, 0.5), QuestRoller.hasQuest(77L, 0.5));
    }

    @Test void differentPlayersGetDifferentFirstQuestsAndTheSamePlayerKeepsHis() throws IOException {
        QuestCatalog cat = shipped();
        Set<String> firsts = new HashSet<>();
        for (int p = 0; p < 200; p++) firsts.add(QuestRoller.pick(cat, QuestRoller.seed(new UUID(p, p * 31L), 1), 1, null, ROOMY).id());
        assertTrue(firsts.size() >= 5, "only " + firsts);
        List<String> again = new ArrayList<>();
        for (int k = 0; k < 3; k++) again.add(QuestRoller.pick(cat, QuestRoller.seed(new UUID(9, 9), 4), 4, null, ROOMY).id());
        assertEquals(again.get(0), again.get(1));
        assertEquals(again.get(1), again.get(2));
    }
}
