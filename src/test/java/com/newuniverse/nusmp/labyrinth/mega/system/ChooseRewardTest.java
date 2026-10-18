package com.newuniverse.nusmp.labyrinth.mega.system;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: in point mode "choose" a cleared floor quest pays into the free pool instead of a stat picked by dice. The dice that remain (a stat reward against a Shadow
 * Core, 70 / 30) must be the same ones as before, the roll must stay reproducible, a player whose stats are all full must not pile up points they can never spend,
 * and the old random roll must be exactly what it was (the old mode is kept). Also pinned: the saved free pool (it comes back from a save, an old save reads as 0,
 * a damaged value is clamped) and the order of the saved fields (the new one is appended last).
 */
class ChooseRewardTest {
    @Test void theChooseRollKeepsTheSeventyThirtySplit() {
        int free = 0, cores = 0, n = 30000;
        for (int i = 0; i < n; i++) {
            RewardRoller.Reward r = RewardRoller.rollChoose(QuestRoller.mix(i), 70, 30, 1, true, 100);
            if (r.kind() == RewardRoller.Kind.FREE) free++;
            else if (r.kind() == RewardRoller.Kind.CORE) cores++;
        }
        assertEquals(n, free + cores);
        assertTrue(free > n * 0.68 && free < n * 0.72, "free " + free);
    }

    @Test void theChooseRollIsDeterministicAndNeverNamesAStat() {
        for (int i = 0; i < 300; i++) {
            RewardRoller.Reward a = RewardRoller.rollChoose(i, 70, 30, 2, true, 50);
            assertEquals(a, RewardRoller.rollChoose(i, 70, 30, 2, true, 50));
            assertNull(a.stat());
        }
    }

    @Test void zeroWeightsAndForbiddenCoresBehaveAsInRandomMode() {
        for (int i = 0; i < 300; i++) {
            assertEquals(RewardRoller.Kind.FREE, RewardRoller.rollChoose(i, 70, 0, 1, true, 9).kind());
            assertEquals(RewardRoller.Kind.CORE, RewardRoller.rollChoose(i, 0, 30, 1, true, 9).kind());
            assertEquals(RewardRoller.Kind.FREE, RewardRoller.rollChoose(i, 0, 0, 1, true, 9).kind(), "no weights: points, never nothing");
            assertEquals(RewardRoller.Kind.FREE, RewardRoller.rollChoose(i, 1, 1000, 1, false, 9).kind(), "a template without a core always pays points");
        }
    }

    @Test void whenTheStatsHaveNoRoomTheCoreIsPaidOrNothing() {
        assertEquals(RewardRoller.Kind.CORE, RewardRoller.rollChoose(1, 100, 0, 1, true, 0).kind());
        assertEquals(RewardRoller.Kind.NONE, RewardRoller.rollChoose(1, 100, 0, 1, false, 0).kind());
        assertEquals(RewardRoller.Kind.NONE, RewardRoller.rollChoose(1, 100, 0, 1, false, -3).kind());
    }

    @Test void thePointsAreCutDownToTheRoomAndAreAtLeastOne() {
        assertEquals(2, RewardRoller.rollChoose(1, 100, 0, 2, false, 50).points());
        assertEquals(1, RewardRoller.rollChoose(1, 100, 0, 2, false, 1).points());
        assertEquals(1, RewardRoller.rollChoose(1, 100, 0, 0, false, 5).points(), "a template that pays 0 still pays 1");
    }

    @Test void theOldRandomRollIsUnchangedByTheNewKind() {
        for (int i = 0; i < 500; i++) {
            RewardRoller.Reward r = RewardRoller.roll(QuestRoller.mix(i), 70, 30, List.of(Stat.STR, Stat.AGI), 1, true, new int[Stat.COUNT], 30);
            assertTrue(r.kind() != RewardRoller.Kind.FREE, "the old roll never pays into the pool");
        }
        assertEquals(RewardRoller.Kind.STAT, RewardRoller.Kind.values()[0]);
        assertEquals(RewardRoller.Kind.CORE, RewardRoller.Kind.values()[1]);
        assertEquals(RewardRoller.Kind.NONE, RewardRoller.Kind.values()[2]);
        assertEquals(RewardRoller.Kind.FREE, RewardRoller.Kind.values()[3], "the new kind is appended at the end");
    }

    // ---------------------------------------------------------------- the saved pool
    @Test void theFreePoolSurvivesBeingSavedAndLoaded() {
        SystemState s = new SystemState();
        s.setFree(17);
        s.setPoints(Stat.INT, 4);
        SystemState back = SystemState.of(s.toData());
        assertEquals(17, back.free());
        assertEquals(4, back.points(Stat.INT));
        SystemState onlyFree = new SystemState();
        onlyFree.setFree(1);
        assertTrue(onlyFree.hasContent(), "a state with only a free point is worth saving");
        assertFalse(new SystemState().hasContent());
    }

    @Test void anOldSaveReadsAsAnEmptyPoolAndKeepsEveryOldField() {
        SystemState.Data old = new SystemState.Data(List.of(1, 2, 3, 4, 5), 2, 9, "SPEED", 3, 1, null, true);
        assertEquals(0, old.free());
        SystemState s = SystemState.of(old);
        assertEquals(0, s.free());
        assertEquals(15, s.totalPoints());
        assertEquals(1, s.points(Stat.STR));
        assertEquals(5, s.points(Stat.SENSE));
        assertEquals(2, s.cores());
        assertEquals(9, s.maxFloor());
        assertTrue(s.hintShown());
        SystemState older = SystemState.of(new SystemState.Data(List.of(1), 0, 0, "", 0, 0, null));
        assertEquals(0, older.free());
        assertFalse(older.hintShown());
    }

    @Test void aDamagedPoolValueIsClamped() {
        assertEquals(0, SystemState.of(new SystemState.Data(null, 0, 0, "", 0, 0, null, false, -40)).free());
        assertEquals(SystemState.FREE_MAX, SystemState.of(new SystemState.Data(null, 0, 0, "", 0, 0, null, false, Integer.MAX_VALUE)).free());
        SystemState s = new SystemState();
        s.setFree(Integer.MIN_VALUE);
        assertEquals(0, s.free());
        assertEquals(0, s.addFree(-5), "taking from an empty pool changes nothing");
        assertEquals(5, s.addFree(5));
        assertEquals(-2, s.addFree(-2));
        assertEquals(SystemState.FREE_MAX - 3, s.addFree(Integer.MAX_VALUE), "addFree stops at the ceiling");
        assertEquals(SystemState.FREE_MAX, s.free());
    }

    @Test void thePointsStayFirstAndInTheirOldOrder() {
        SystemState s = new SystemState();
        for (Stat st : Stat.values()) s.setPoints(st, st.ordinal() + 1);
        assertEquals(List.of(1, 2, 3, 4, 5), s.toData().points());
        assertEquals(Stat.STR, Stat.values()[0]);
        assertEquals(Stat.SENSE, Stat.values()[Stat.COUNT - 1]);
    }
}
