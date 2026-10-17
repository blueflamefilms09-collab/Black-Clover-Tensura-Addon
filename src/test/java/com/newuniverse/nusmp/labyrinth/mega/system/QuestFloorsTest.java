package com.newuniverse.nusmp.labyrinth.mega.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the quests are silent on a boss floor. With one floor per sector (1.58.0) the old "multiple of the sector's floors" test would call EVERY floor a boss floor
 * and no quest would ever appear, so the rule is pinned in floor numbers: every tenth floor of either road, never Floor 0, and the same answer for an old world whose
 * sectors still hold ten floors.
 */
class QuestFloorsTest {
    @Test void everyTenthFloorOfEitherRoadIsABossFloor() {
        for (int k = 1; k <= 12; k++) {
            assertTrue(QuestFloors.isBossFloor(10 * k), "+" + 10 * k);
            assertTrue(QuestFloors.isBossFloor(-10 * k), "-" + 10 * k);
        }
    }

    @Test void theFloorsBetweenHaveQuests() {
        for (int f = 1; f <= 120; f++) {
            if (f % 10 == 0) continue;
            assertFalse(QuestFloors.isBossFloor(f), "+" + f);
            assertFalse(QuestFloors.isBossFloor(-f), "-" + f);
        }
    }

    @Test void theHubIsNotABossFloor() {
        assertFalse(QuestFloors.isBossFloor(0));
        assertEquals(10, QuestFloors.BOSS_EVERY);
    }
}
