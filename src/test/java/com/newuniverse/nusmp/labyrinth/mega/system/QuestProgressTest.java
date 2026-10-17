package com.newuniverse.nusmp.labyrinth.mega.system;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: "the quest succeeds BEFORE the next staircase" is the whole rule of the System, so every transition of the state machine is pinned: a counting quest
 * completes exactly at its target and pays once, fails if the next floor is entered first, ignores everything after it ends; a room counts once; magic damage
 * fails only a "no magic" quest and any damage fails only the "no damage" one; a clock runs out; the two "reach the next floor" kinds succeed on arrival; a
 * saved quest comes back as it was and a damaged save cannot break the machine. The tests never touch Minecraft.
 */
class QuestProgressTest {
    static QuestTemplate tpl(QuestKind kind, boolean noMagic) {
        return new QuestTemplate("q_" + kind.name().toLowerCase(), kind, "", 5, 0, 1, 99, 10, noMagic, List.of(), 1, true, "");
    }

    static QuestProgress q(QuestKind kind, int floor, int target) { return QuestProgress.start(tpl(kind, false), floor, target); }

    static QuestProgress qm(QuestKind kind, int floor, int target) { return QuestProgress.start(tpl(kind, true), floor, target); }

    @Test void aNewQuestIsActiveAtZero() {
        QuestProgress p = q(QuestKind.KILL_N, 4, 5);
        assertTrue(p.isActive());
        assertEquals(0, p.progress());
        assertEquals(5, p.target());
        assertEquals(4, p.floor());
        assertEquals(-1, p.timeLeft(), "only SPEED has a clock");
    }

    @Test void killsCountUpAndCompleteExactlyAtTheTarget() {
        QuestProgress p = q(QuestKind.KILL_N, 4, 3);
        assertEquals(QuestProgress.Change.PROGRESSED, p.onKill());
        assertEquals(QuestProgress.Change.PROGRESSED, p.onKill());
        assertEquals(2, p.progress());
        assertEquals(QuestProgress.Change.COMPLETED, p.onKill());
        assertEquals(QuestProgress.State.DONE, p.state());
        assertEquals(3, p.progress());
    }

    @Test void aFinishedQuestIgnoresEverythingSoItPaysOnce() {
        QuestProgress p = q(QuestKind.KILL_N, 4, 1);
        assertEquals(QuestProgress.Change.COMPLETED, p.onKill());
        for (int i = 0; i < 5; i++) {
            assertEquals(QuestProgress.Change.NONE, p.onKill());
            assertEquals(QuestProgress.Change.NONE, p.onNextFloor());
            assertEquals(QuestProgress.Change.NONE, p.onDamage());
            assertEquals(QuestProgress.Change.NONE, p.onMagicDamage());
            assertEquals(QuestProgress.Change.NONE, p.forceFail());
            assertEquals(QuestProgress.Change.NONE, p.forceComplete());
        }
        assertEquals(QuestProgress.State.DONE, p.state());
        QuestProgress f = q(QuestKind.KILL_N, 4, 3);
        assertEquals(QuestProgress.Change.FAILED, f.forceFail());
        assertEquals(QuestProgress.Change.NONE, f.onKill());
        assertEquals(QuestProgress.Change.NONE, f.forceComplete());
        assertEquals(QuestProgress.State.FAILED, f.state());
    }

    @Test void aKindOnlyCountsItsOwnEvents() {
        QuestProgress kills = q(QuestKind.KILL_N, 4, 3);
        assertEquals(QuestProgress.Change.NONE, kills.onAmbushKill());
        assertEquals(QuestProgress.Change.NONE, kills.countRoom(1));
        QuestProgress ambush = q(QuestKind.SURVIVE_AMBUSH, 4, 3);
        assertEquals(QuestProgress.Change.NONE, ambush.onKill());
        assertEquals(QuestProgress.Change.PROGRESSED, ambush.onAmbushKill());
        QuestProgress rooms = q(QuestKind.CLEAR_ROOMS, 4, 3);
        assertEquals(QuestProgress.Change.NONE, rooms.onKill());
        assertEquals(QuestProgress.Change.NONE, rooms.onAmbushKill());
    }

    @Test void aRoomCountsOnlyOnce() {
        QuestProgress p = q(QuestKind.CLEAR_ROOMS, 2, 3);
        assertEquals(QuestProgress.Change.PROGRESSED, p.countRoom(17));
        assertEquals(QuestProgress.Change.NONE, p.countRoom(17));
        assertEquals(QuestProgress.Change.NONE, p.countRoom(17));
        assertEquals(1, p.progress());
        assertEquals(QuestProgress.Change.PROGRESSED, p.countRoom(18));
        assertEquals(QuestProgress.Change.COMPLETED, p.countRoom(19));
        assertTrue(p.counted().contains(17) && p.counted().contains(19));
    }

    @Test void enteringTheNextFloorFailsAnUnfinishedCountingQuest() {
        for (QuestKind k : new QuestKind[] {QuestKind.CLEAR_ROOMS, QuestKind.KILL_N, QuestKind.SURVIVE_AMBUSH}) {
            QuestProgress p = q(k, 4, 3);
            assertEquals(QuestProgress.Change.FAILED, p.onNextFloor(), k.name());
            assertEquals(QuestProgress.State.FAILED, p.state());
            assertEquals(QuestProgress.Change.NONE, p.onNextFloor(), "idempotent");
        }
    }

    @Test void enteringTheNextFloorLeavesACompletedQuestDone() {
        QuestProgress p = q(QuestKind.KILL_N, 4, 1);
        p.onKill();
        assertEquals(QuestProgress.Change.NONE, p.onNextFloor());
        assertEquals(QuestProgress.State.DONE, p.state());
    }

    @Test void theTwoReachKindsSucceedOnArrivalWhileStillActive() {
        QuestProgress hold = q(QuestKind.NO_DAMAGE_STRETCH, 4, 1);
        assertEquals(QuestProgress.Change.COMPLETED, hold.onNextFloor());
        assertEquals(QuestProgress.State.DONE, hold.state());
        assertEquals(1, hold.progress());
        QuestProgress race = q(QuestKind.SPEED, 4, 120);
        race.tickTime(200);
        assertEquals(QuestProgress.Change.COMPLETED, race.onNextFloor());
    }

    @Test void theReachKindsCannotBeCompletedByCounting() {
        QuestProgress p = q(QuestKind.NO_DAMAGE_STRETCH, 4, 1);
        assertEquals(QuestProgress.Change.NONE, p.addProgress(5));
        assertEquals(QuestProgress.Change.NONE, p.onKill());
        assertTrue(p.isActive());
    }

    @Test void anyDamageFailsTheNoDamageQuestAndNothingElse() {
        QuestProgress hold = q(QuestKind.NO_DAMAGE_STRETCH, 4, 1);
        assertEquals(QuestProgress.Change.FAILED, hold.onDamage());
        assertEquals(QuestProgress.Change.NONE, hold.onNextFloor(), "a failed quest is not rescued by the staircase");
        assertEquals(QuestProgress.State.FAILED, hold.state());
        for (QuestKind k : new QuestKind[] {QuestKind.CLEAR_ROOMS, QuestKind.KILL_N, QuestKind.SURVIVE_AMBUSH, QuestKind.SPEED}) {
            QuestProgress p = q(k, 4, 3);
            assertEquals(QuestProgress.Change.NONE, p.onDamage(), k.name());
            assertTrue(p.isActive());
        }
    }

    @Test void magicDamageFailsOnlyAQuestThatForbidsIt() {
        QuestProgress strict = qm(QuestKind.CLEAR_ROOMS, 4, 5);
        assertEquals(QuestProgress.Change.FAILED, strict.onMagicDamage());
        assertEquals(QuestProgress.Change.NONE, strict.onMagicDamage());
        QuestProgress loose = q(QuestKind.CLEAR_ROOMS, 4, 5);
        assertEquals(QuestProgress.Change.NONE, loose.onMagicDamage());
        assertTrue(loose.isActive());
        QuestProgress done = qm(QuestKind.KILL_N, 4, 1);
        done.onKill();
        assertEquals(QuestProgress.Change.NONE, done.onMagicDamage(), "too late: it was already completed");
        assertEquals(QuestProgress.State.DONE, done.state());
    }

    @Test void theClockRunsOutAndFailsOnlyATimedQuest() {
        QuestProgress race = q(QuestKind.SPEED, 4, 10);
        assertEquals(200, race.timeLeft(), "a timed quest's target is its length in seconds");
        assertEquals(QuestProgress.Change.NONE, race.tickTime(150));
        assertEquals(50, race.timeLeft());
        assertEquals(QuestProgress.Change.FAILED, race.tickTime(50));
        assertEquals(0, race.timeLeft());
        assertEquals(QuestProgress.Change.NONE, race.tickTime(5));
        assertEquals(QuestProgress.State.FAILED, race.state());
        QuestProgress kills = q(QuestKind.KILL_N, 4, 3);
        assertEquals(QuestProgress.Change.NONE, kills.tickTime(100000));
        assertTrue(kills.isActive());
        assertEquals(QuestProgress.Change.NONE, q(QuestKind.SPEED, 4, 10).tickTime(0));
        assertEquals(QuestProgress.Change.NONE, q(QuestKind.SPEED, 4, 10).tickTime(-5));
    }

    @Test void theNextFloorIsDeeperOnTheSameRoadOnly() {
        assertTrue(QuestProgress.isNextFloor(4, 5));
        assertTrue(QuestProgress.isNextFloor(4, 9), "skipping floors is still moving on");
        assertFalse(QuestProgress.isNextFloor(4, 4));
        assertFalse(QuestProgress.isNextFloor(4, 3), "going back down is not the next floor");
        assertTrue(QuestProgress.isNextFloor(-4, -5));
        assertFalse(QuestProgress.isNextFloor(-4, -3));
        assertFalse(QuestProgress.isNextFloor(4, -5), "the other road is a different matter");
        assertFalse(QuestProgress.isNextFloor(-4, 5));
        assertFalse(QuestProgress.isNextFloor(0, 5));
        assertFalse(QuestProgress.isNextFloor(4, 0));
    }

    @Test void countingNeverOvershootsAndNegativeCountsDoNothing() {
        QuestProgress p = q(QuestKind.KILL_N, 4, 4);
        assertEquals(QuestProgress.Change.NONE, p.addProgress(0));
        assertEquals(QuestProgress.Change.NONE, p.addProgress(-3));
        assertEquals(QuestProgress.Change.COMPLETED, p.addProgress(Integer.MAX_VALUE));
        assertEquals(4, p.progress());
    }

    @Test void aQuestSurvivesBeingSavedAndLoaded() {
        QuestProgress p = qm(QuestKind.CLEAR_ROOMS, -7, 5);
        p.countRoom(3);
        p.countRoom(8);
        QuestProgress back = QuestProgress.of(p.toData());
        assertNotNull(back);
        assertEquals(p.id(), back.id());
        assertEquals(p.kind(), back.kind());
        assertEquals(-7, back.floor());
        assertEquals(5, back.target());
        assertEquals(2, back.progress());
        assertTrue(back.noMagic());
        assertEquals(p.counted(), back.counted());
        assertEquals(QuestProgress.Change.NONE, back.countRoom(3), "the room counted before saving is still counted");
        QuestProgress race = q(QuestKind.SPEED, 2, 100);
        race.tickTime(777);
        assertEquals(2000 - 777, QuestProgress.of(race.toData()).timeLeft());
        QuestProgress failed = q(QuestKind.KILL_N, 2, 3);
        failed.forceFail();
        assertEquals(QuestProgress.State.FAILED, QuestProgress.of(failed.toData()).state());
    }

    @Test void aDamagedSaveCannotBreakTheMachine() {
        assertNull(QuestProgress.of(null));
        assertNull(QuestProgress.of(new QuestProgress.Data("", "KILL_N", 3, 5, 0, "ACTIVE", false, -1, List.of())), "no id");
        assertNull(QuestProgress.of(new QuestProgress.Data("x", "DANCE", 3, 5, 0, "ACTIVE", false, -1, List.of())), "a kind that is gone");
        QuestProgress p = QuestProgress.of(new QuestProgress.Data("x", "kill_n", 3, -9, 99, "WHAT", false, 5, null));
        assertNotNull(p);
        assertEquals(1, p.target(), "a non-positive target is raised to 1");
        assertEquals(1, p.progress(), "progress is clamped to the target");
        assertEquals(QuestProgress.State.FAILED, p.state(), "an unknown state is a finished quest, not a free one");
        assertEquals(-1, p.timeLeft());
    }
}
