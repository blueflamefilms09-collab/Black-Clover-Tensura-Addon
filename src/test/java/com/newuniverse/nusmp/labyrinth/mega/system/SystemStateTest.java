package com.newuniverse.nusmp.labyrinth.mega.system;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the attachment must come back exactly as it was saved, tolerate every missing or damaged field of an old or hand-edited save, and never let a stat go
 * negative or past its cap. Also pinned here: that every word the System shows has an English text (the lang fragment is written from the same map) and sits
 * under the contract's prefix, so no label ever reaches the screen as a raw key.
 */
class SystemStateTest {
    @Test void aStateSurvivesBeingSavedAndLoaded() {
        SystemState s = new SystemState();
        s.setPoints(Stat.STR, 4);
        s.setPoints(Stat.SENSE, 9);
        s.addCore();
        s.addCore();
        s.setMaxFloor(23);
        s.setLastKind(QuestKind.SPEED);
        s.countDone();
        s.countFailed();
        s.countFailed();
        QuestProgress q = QuestProgress.start(new QuestTemplate("clear", QuestKind.CLEAR_ROOMS, "", 3, 0, 1, 9, 10, true, List.of(), 1, true, ""), 23, 4);
        q.countRoom(8);
        s.setQuest(q);
        SystemState back = SystemState.of(s.toData());
        assertEquals(4, back.points(Stat.STR));
        assertEquals(9, back.points(Stat.SENSE));
        assertEquals(0, back.points(Stat.VIT));
        assertEquals(13, back.totalPoints());
        assertEquals(2, back.cores());
        assertEquals(23, back.maxFloor());
        assertEquals(QuestKind.SPEED, back.lastKindOrNull());
        assertEquals(1, back.done());
        assertEquals(2, back.failed());
        assertNotNull(back.quest());
        assertEquals(Set.of(8), back.quest().counted());
        assertTrue(back.quest().noMagic());
    }

    @Test void aNullOrEmptyRecordGivesAFreshValidState() {
        SystemState s = SystemState.of(null);
        assertEquals(0, s.totalPoints());
        assertNull(s.quest());
        assertEquals("", s.lastKind());
        SystemState t = SystemState.of(new SystemState.Data(null, -5, -9, null, -1, -1, null));
        assertEquals(0, t.cores());
        assertEquals(0, t.maxFloor());
        assertEquals("", t.lastKind());
        assertNull(t.lastKindOrNull());
    }

    @Test void aShortLongOrDamagedPointListStillLoads() {
        SystemState s = SystemState.of(new SystemState.Data(java.util.Arrays.asList(5, null, -3, 99999, 7, 8, 9), 0, 0, "", 0, 0, null));
        assertEquals(5, s.points(Stat.STR));
        assertEquals(0, s.points(Stat.AGI), "null reads as zero");
        assertEquals(0, s.points(Stat.VIT), "negative reads as zero");
        assertEquals(SystemState.HARD_CAP, s.points(Stat.INT), "past the hard cap reads as the cap");
        assertEquals(7, s.points(Stat.SENSE));
        SystemState shortList = SystemState.of(new SystemState.Data(List.of(2), 0, 0, "", 0, 0, null));
        assertEquals(2, shortList.points(Stat.STR));
        assertEquals(0, shortList.points(Stat.SENSE));
    }

    @Test void pointsRespectTheCapAndNeverGoBelowZero() {
        SystemState s = new SystemState();
        assertEquals(5, s.addPoints(Stat.STR, 5, 30));
        assertEquals(25, s.addPoints(Stat.STR, 100, 30), "cut to the cap");
        assertEquals(0, s.addPoints(Stat.STR, 1, 30), "already at the cap");
        assertEquals(30, s.points(Stat.STR));
        assertEquals(-7, s.addPoints(Stat.STR, -7, 30));
        assertEquals(-23, s.addPoints(Stat.STR, -999, 30), "stops at zero");
        assertEquals(0, s.points(Stat.STR));
    }

    @Test void aCapLoweredLaterNeverTakesEarnedPointsAway() {
        SystemState s = new SystemState();
        s.setPoints(Stat.VIT, 25);
        assertEquals(0, s.addPoints(Stat.VIT, 3, 10), "no new points above the lowered cap");
        assertEquals(25, s.points(Stat.VIT), "but the 25 earned stay");
        assertEquals(-5, s.addPoints(Stat.VIT, -5, 10), "an admin can still take points away");
    }

    @Test void aHugeAddCannotOverflow() {
        SystemState s = new SystemState();
        s.setPoints(Stat.AGI, 10);
        s.addPoints(Stat.AGI, Integer.MAX_VALUE, 30);
        assertEquals(30, s.points(Stat.AGI));
    }

    // ---------------------------------------------------------------- the words
    @Test void everyTextHasAnEnglishFallbackUnderTheContractsPrefix() {
        assertTrue(SystemText.all().size() >= 40, "found " + SystemText.all().size());
        for (var e : SystemText.all().entrySet()) {
            assertFalse(e.getValue().isBlank(), e.getKey());
            assertTrue(e.getKey().startsWith("message.nusmp.mega.system.") || e.getKey().equals(SystemText.KEY_TOGGLE) || e.getKey().equals(SystemText.ITEM_NAME), e.getKey());
            assertEquals(e.getValue(), SystemText.english(e.getKey()));
        }
        assertEquals("not.a.key", SystemText.english("not.a.key"), "an unknown key shows as itself, never null");
        assertEquals("null", SystemText.english(null));
    }

    @Test void theFormatsTakeTheArgumentsTheCodeGivesThem() {
        assertTrue(SystemText.english(SystemText.TOAST_REWARD_STAT).contains("%1$s") && SystemText.english(SystemText.TOAST_REWARD_STAT).contains("%2$s"));
        assertTrue(SystemText.english(SystemText.TOAST_FLOOR_REACHED).contains("%1$s"));
        assertTrue(SystemText.english(SystemText.TIP_CHARGES).contains("%2$s"));
        assertEquals("[ SYSTEM ]", SystemText.english(SystemText.HEADER));
        assertEquals("QUEST ARRIVED", SystemText.english(SystemText.TOAST_QUEST_ARRIVED));
        assertEquals("EXTRACTION READY", SystemText.english(SystemText.TOAST_EXTRACTION_READY));
    }

    @Test void theStatsAndKindsReadByTheirIds() {
        assertEquals(Stat.SENSE, Stat.fromId(" sense "));
        assertEquals("sense", Stat.SENSE.id());
        assertNull(Stat.fromId("luck"));
        assertNull(Stat.fromId(null));
        assertEquals(QuestKind.NO_DAMAGE_STRETCH, QuestKind.fromId("no_damage_stretch"));
        assertEquals("speed", QuestKind.SPEED.id());
        assertNull(QuestKind.fromId(""));
        assertEquals(5, Stat.COUNT);
        assertEquals(List.of(Stat.STR, Stat.AGI), QuestKind.KILL_N.defaultStats());
        assertTrue(QuestKind.SPEED.timed() && !QuestKind.KILL_N.timed());
        assertTrue(QuestKind.SPEED.completesOnArrival() && QuestKind.NO_DAMAGE_STRETCH.completesOnArrival());
        assertFalse(QuestKind.CLEAR_ROOMS.completesOnArrival() || QuestKind.KILL_N.completesOnArrival() || QuestKind.SURVIVE_AMBUSH.completesOnArrival());
    }
}
