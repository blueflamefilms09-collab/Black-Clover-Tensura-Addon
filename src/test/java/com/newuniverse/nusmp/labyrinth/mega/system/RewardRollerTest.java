package com.newuniverse.nusmp.labyrinth.mega.system;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the reward roll is the System's payoff, weighted 70 / 30 by the owner and reproducible per player. These tests pin the distribution over thousands of
 * seeds, the determinism, the rules that stop it from paying nothing by accident (a capped stat falls back to another stat or to the core), and the
 * zero-weight and no-core edges.
 */
class RewardRollerTest {
    static final int[] NONE_YET = new int[Stat.COUNT];

    @Test void theSeedIsStableAndSeparatesPlayersFloorsQuestsAndCounts() {
        UUID p = new UUID(5, 6);
        assertEquals(RewardRoller.seed(p, 3, "q", 4), RewardRoller.seed(p, 3, "q", 4));
        assertNotEquals(RewardRoller.seed(p, 3, "q", 4), RewardRoller.seed(p, 4, "q", 4));
        assertNotEquals(RewardRoller.seed(p, 3, "q", 4), RewardRoller.seed(p, 3, "r", 4));
        assertNotEquals(RewardRoller.seed(p, 3, "q", 4), RewardRoller.seed(p, 3, "q", 5));
        assertNotEquals(RewardRoller.seed(p, 3, "q", 4), RewardRoller.seed(new UUID(5, 7), 3, "q", 4));
    }

    @Test void theRollIsDeterministic() {
        for (int i = 0; i < 200; i++) {
            RewardRoller.Reward a = RewardRoller.roll(i, 70, 30, List.of(Stat.STR, Stat.AGI), 1, true, NONE_YET, 30);
            RewardRoller.Reward b = RewardRoller.roll(i, 70, 30, List.of(Stat.STR, Stat.AGI), 1, true, NONE_YET, 30);
            assertEquals(a, b);
        }
    }

    @Test void seventyThirtyIsRoughlySeventyThirty() {
        int stats = 0, cores = 0, n = 30000;
        for (int i = 0; i < n; i++) {
            RewardRoller.Reward r = RewardRoller.roll(QuestRoller.mix(i), 70, 30, List.of(Stat.STR, Stat.AGI), 1, true, NONE_YET, 30);
            if (r.kind() == RewardRoller.Kind.STAT) stats++;
            else if (r.kind() == RewardRoller.Kind.CORE) cores++;
        }
        assertEquals(n, stats + cores);
        assertTrue(stats > n * 0.68 && stats < n * 0.72, "stats " + stats);
        assertTrue(cores > n * 0.28 && cores < n * 0.32, "cores " + cores);
    }

    @Test void theStatIsDrawnOnlyFromTheOnesTheQuestPays() {
        Map<Stat, Integer> seen = new EnumMap<>(Stat.class);
        for (int i = 0; i < 4000; i++) {
            RewardRoller.Reward r = RewardRoller.roll(QuestRoller.mix(i), 100, 0, List.of(Stat.STR, Stat.AGI), 1, true, NONE_YET, 30);
            assertEquals(RewardRoller.Kind.STAT, r.kind());
            assertTrue(r.stat() == Stat.STR || r.stat() == Stat.AGI);
            seen.merge(r.stat(), 1, Integer::sum);
        }
        assertTrue(seen.get(Stat.STR) > 1700 && seen.get(Stat.AGI) > 1700, seen.toString());
    }

    @Test void zeroWeightsAreHonoured() {
        for (int i = 0; i < 500; i++) {
            assertEquals(RewardRoller.Kind.STAT, RewardRoller.roll(i, 70, 0, List.of(Stat.INT), 1, true, NONE_YET, 30).kind(), "no core weight: never a core");
            assertEquals(RewardRoller.Kind.CORE, RewardRoller.roll(i, 0, 30, List.of(Stat.INT), 1, true, NONE_YET, 30).kind(), "no stat weight: always the core");
            assertEquals(RewardRoller.Kind.STAT, RewardRoller.roll(i, 0, 0, List.of(Stat.INT), 1, true, NONE_YET, 30).kind(), "no weights at all: a stat, never nothing");
        }
    }

    @Test void aTemplateThatForbidsTheCoreAlwaysPaysAStat() {
        for (int i = 0; i < 1000; i++) {
            RewardRoller.Reward r = RewardRoller.roll(QuestRoller.mix(i), 1, 1000, List.of(Stat.VIT), 2, false, NONE_YET, 30);
            assertEquals(RewardRoller.Kind.STAT, r.kind());
            assertEquals(Stat.VIT, r.stat());
            assertEquals(2, r.points());
        }
    }

    @Test void aCappedStatIsNeverPaidAndTheOtherOneIsUsed() {
        int[] have = new int[Stat.COUNT];
        have[Stat.STR.ordinal()] = 30;
        for (int i = 0; i < 1000; i++) {
            RewardRoller.Reward r = RewardRoller.roll(QuestRoller.mix(i), 100, 0, List.of(Stat.STR, Stat.AGI), 1, true, have, 30);
            assertEquals(Stat.AGI, r.stat());
        }
    }

    @Test void whenEveryPaidStatIsCappedTheCoreIsPaidOrNothing() {
        int[] have = new int[Stat.COUNT];
        have[Stat.INT.ordinal()] = 30;
        assertEquals(RewardRoller.Kind.CORE, RewardRoller.roll(1, 100, 0, List.of(Stat.INT), 1, true, have, 30).kind());
        assertEquals(RewardRoller.Kind.NONE, RewardRoller.roll(1, 100, 0, List.of(Stat.INT), 1, false, have, 30).kind());
        assertEquals(RewardRoller.Kind.CORE, RewardRoller.roll(1, 100, 0, List.of(Stat.INT), 1, true, NONE_YET, 0).kind(), "a cap of 0 allows no stat at all");
    }

    @Test void thePointsAreCutDownToWhatIsLeftUnderTheCap() {
        int[] have = new int[Stat.COUNT];
        have[Stat.VIT.ordinal()] = 29;
        RewardRoller.Reward r = RewardRoller.roll(3, 100, 0, List.of(Stat.VIT), 2, false, have, 30);
        assertEquals(Stat.VIT, r.stat());
        assertEquals(1, r.points());
    }

    @Test void aDuplicateInTheCandidateListDoesNotSkewTheDraw() {
        int str = 0, n = 6000;
        for (int i = 0; i < n; i++) {
            if (RewardRoller.roll(QuestRoller.mix(i), 100, 0, List.of(Stat.STR, Stat.STR, Stat.AGI), 1, true, NONE_YET, 30).stat() == Stat.STR) str++;
        }
        assertTrue(str > n * 0.45 && str < n * 0.55, "str " + str);
    }
}
