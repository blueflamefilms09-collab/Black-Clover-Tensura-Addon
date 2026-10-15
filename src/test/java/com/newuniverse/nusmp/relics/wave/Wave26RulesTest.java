package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Relics of Many Worlds, wave 26: the pure rules of the head relics (Frank, the helms, the masks, the lens). */
class Wave26RulesTest {
    @Test
    void daysAndDiceStayInRange() {
        assertEquals(0, Wave26Rules.dayOf(23999));
        assertEquals(1, Wave26Rules.dayOf(24000));
        assertEquals(-1, Wave26Rules.dayOf(-1));
        for (int i = -200; i < 200; i++) {
            final int seed = i;
            int d = Wave26Rules.die(n -> seed, 8);
            assertTrue(d >= 1 && d <= 8, "d8 " + d);
        }
        assertEquals(1, Wave26Rules.die(n -> 0, 0), "a zero-sided die is a one");
    }

    @Test
    void savingThrowsHaveNaturalTwentyAndOne() {
        assertTrue(Wave26Rules.saves(20, -50, 30));
        assertFalse(Wave26Rules.saves(1, 50, 5));
        assertTrue(Wave26Rules.saves(13, 3, 16));
        assertFalse(Wave26Rules.saves(12, 3, 16));
        assertTrue(Wave26Rules.epSaves(1000, 100));
        assertFalse(Wave26Rules.epSaves(300, 100));
        assertFalse(Wave26Rules.epSaves(0, 0));
    }

    @Test
    void frankWeakensTheUnmaskedAndStrengthensForesight() {
        assertEquals(1.0f, Wave26Rules.outFactor(false, false), 1e-6);
        assertEquals(0.7f, Wave26Rules.outFactor(false, true), 1e-6);
        assertEquals(1.2f, Wave26Rules.outFactor(true, false), 1e-6);
        assertEquals(0.84f, Wave26Rules.outFactor(true, true), 1e-5);
        assertTrue(Wave26Rules.FORESIGHT_IN < 1.0f);
    }

    @Test
    void frankCommandsCycleAndFailuresCountToTheVisit() {
        assertEquals(Wave26Rules.Command.STRIKE_PEACEFUL, Wave26Rules.commandFor(0));
        assertEquals(Wave26Rules.Command.THROW_AWAY, Wave26Rules.commandFor(1));
        assertEquals(Wave26Rules.Command.HOLD_STILL, Wave26Rules.commandFor(2));
        assertEquals(Wave26Rules.Command.STRIKE_PEACEFUL, Wave26Rules.commandFor(3));
        assertEquals(Wave26Rules.Command.HOLD_STILL, Wave26Rules.commandFor(-1), "a negative round still maps");
        int fails = 0;
        for (int i = 0; i < 4; i++) { fails = Wave26Rules.afterFail(fails); assertFalse(Wave26Rules.visitEarned(fails)); }
        fails = Wave26Rules.afterFail(fails);
        assertTrue(Wave26Rules.visitEarned(fails));
        assertEquals(1, Wave26Rules.afterFail(-5), "a damaged counter restarts");
    }

    @Test
    void theCountdownRunsTwentyEightDays() {
        assertEquals(28, Wave26Rules.daysLeft(10, 10));
        assertEquals(1, Wave26Rules.daysLeft(10, 37));
        assertEquals(0, Wave26Rules.daysLeft(10, 38));
        assertEquals(0, Wave26Rules.daysLeft(10, 400));
        assertEquals(28, Wave26Rules.daysLeft(10, 3), "a clock gone backwards does not add days");
    }

    @Test
    void theBeardIsOneInFive() {
        int yes = 0;
        for (int r = 1; r <= 100; r++) if (Wave26Rules.growsBeard(r)) yes++;
        assertEquals(20, yes);
        assertFalse(Wave26Rules.growsBeard(0));
        assertFalse(Wave26Rules.growsBeard(101));
    }

    @Test
    void voicesStrikeUnlessTheSaveHolds() {
        int fails = 0;
        for (int d = 1; d <= 20; d++) if (Wave26Rules.madnessStrikes(d)) fails++;
        assertEquals(12, fails, "d20 + 3 against 16: 1 to 12 fail");
        assertFalse(Wave26Rules.madnessStrikes(20));
    }

    @Test
    void headbuttDiceAndCritical() {
        for (int d = -3; d < 14; d++) {
            float f = Wave26Rules.headbuttDamage(d);
            assertTrue(f >= 1f && f <= 8f);
        }
        assertTrue(Wave26Rules.headbuttCrit(true, 1));
        assertTrue(Wave26Rules.headbuttCrit(false, 20));
        assertFalse(Wave26Rules.headbuttCrit(false, 19));
    }

    @Test
    void jellyfishWrathHalvesOnASave() {
        assertEquals(6f, Wave26Rules.wrathDamage(6, false), 1e-6);
        assertEquals(3f, Wave26Rules.wrathDamage(6, true), 1e-6);
        assertEquals(1f, Wave26Rules.wrathDamage(-2, false), 1e-6);
        assertEquals(6f, Wave26Rules.wrathDamage(40, false), 1e-6);
    }

    @Test
    void theLensRemembersABoundedRecentPast() {
        Wave26Rules.Ledger l = new Wave26Rules.Ledger();
        l.add(0, 64, 0, 100, 0);
        l.add(0.1, 64, 0.1, 110, 0);                                              // standing still: not noted again
        assertEquals(1, l.size());
        l.add(5, 64, 5, 120, 1);
        assertEquals(2, l.size());
        List<Wave26Rules.Mark> recent = l.recent(200, 100, 0, 0, 30, 10);
        assertEquals(2, recent.size());
        assertEquals(1, recent.get(0).kind(), "newest first");
        assertEquals(0, l.recent(100000, Wave26Rules.TRACK_AGE, 0, 0, 30, 10).size(), "old tracks fade");
        assertEquals(1, l.recent(200, 100, 5, 5, 1, 10).size(), "only those inside the radius");
        assertEquals(1, l.recent(200, 100, 0, 0, 30, 1).size(), "the limit holds");
        for (int i = 0; i < 1000; i++) l.add(i * 3, 64, 0, 300 + i, 2);
        assertEquals(Wave26Rules.LEDGER_CAP, l.size());
        l.clear();
        assertEquals(0, l.size());
    }

    @Test
    void mindMeltingNeedsAnUnconsciousMindAndMayTakeALevel() {
        assertTrue(Wave26Rules.unconscious(true, false, 20, 20));
        assertTrue(Wave26Rules.unconscious(false, true, 20, 20));
        assertTrue(Wave26Rules.unconscious(false, false, 3, 20));
        assertFalse(Wave26Rules.unconscious(false, false, 4, 20));
        assertEquals(10, Wave26Rules.memoryXp(1));
        assertEquals(40, Wave26Rules.memoryXp(20));
        assertEquals(120, Wave26Rules.memoryXp(5000));
        int steals = 0;
        for (int r = 1; r <= 100; r++) if (Wave26Rules.stealsMemory(r)) steals++;
        assertEquals(30, steals);
        assertEquals(0, Wave26Rules.levelsLost(0, 3), "nothing to lose");
        assertEquals(2, Wave26Rules.levelsLost(2, 3), "never more than the wearer has");
        assertEquals(3, Wave26Rules.levelsLost(30, 9));
        assertEquals(1, Wave26Rules.levelsLost(30, 0));
    }

    @Test
    void theRavenQueenSparesTheDying() {
        assertTrue(Wave26Rules.dying(5f, 20f));
        assertTrue(Wave26Rules.dying(6f, 20f));
        assertFalse(Wave26Rules.dying(7f, 20f));
        assertTrue(Wave26Rules.dying(30f, 100f), "30% of a big pool counts");
        assertEquals(6f, Wave26Rules.sparedTo(1f, 20f), 1e-6);
        assertEquals(10f, Wave26Rules.sparedTo(10f, 20f), 1e-6, "never lowers");
        assertEquals(4f, Wave26Rules.sparedTo(1f, 4f), 1e-6, "never past the maximum");
    }

    @Test
    void theVoodooTableCoversTheD4() {
        assertEquals(Wave26Rules.Voodoo.WITCH_BOLT, Wave26Rules.voodoo(1));
        assertEquals(Wave26Rules.Voodoo.HEX, Wave26Rules.voodoo(2));
        assertEquals(Wave26Rules.Voodoo.FROG, Wave26Rules.voodoo(3));
        assertEquals(Wave26Rules.Voodoo.HYPNOTIC_PATTERN, Wave26Rules.voodoo(4));
        assertEquals(Wave26Rules.Voodoo.WITCH_BOLT, Wave26Rules.voodoo(-9));
        assertEquals(Wave26Rules.Voodoo.HYPNOTIC_PATTERN, Wave26Rules.voodoo(99));
    }

    @Test
    void charmAndBoredomAreBounded() {
        assertEquals(0, Wave26Rules.charmTicks(3600, true));
        assertEquals(3600, Wave26Rules.charmTicks(3600, false));
        assertEquals(0, Wave26Rules.charmTicks(-5, false));
        assertFalse(Wave26Rules.bored(Wave26Rules.BORED_AFTER - 1));
        assertTrue(Wave26Rules.bored(Wave26Rules.BORED_AFTER));
    }
}
