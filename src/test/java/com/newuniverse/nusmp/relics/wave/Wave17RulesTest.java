package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 17: the pure rules of the notebooks, pouches and charms (docs/doc_wave17.md). */
class Wave17RulesTest {
    /** A roller that answers a fixed sequence (each value is the 0-based result of nextInt). */
    private static java.util.function.IntUnaryOperator seq(int... values) {
        int[] i = {0};
        return n -> Math.min(n - 1, values[Math.min(i[0]++, values.length - 1)]);
    }

    @Test
    void diceAreOneToSidesAndThreeD10SumsThree() {
        assertEquals(1, Wave17Rules.die(n -> 0, 8));
        assertEquals(8, Wave17Rules.die(n -> n - 1, 8));
        assertEquals(3, Wave17Rules.rollBeads(n -> 0), "3d10 is at least 3");
        assertEquals(30, Wave17Rules.rollBeads(n -> n - 1), "and at most 30");
        assertEquals(1 + 5 + 10, Wave17Rules.rollBeads(seq(0, 4, 9)));
    }

    @Test
    void theNotebookBonusIsOneDFourMinusOneAndLongerStudiesNeedMorePages() {
        assertEquals(0, Wave17Rules.studyBonus(n -> 0));
        assertEquals(3, Wave17Rules.studyBonus(n -> 3));
        assertEquals(2400, Wave17Rules.studyTicks(0));
        assertEquals(6000, Wave17Rules.studyTicks(3));
        assertEquals(6000, Wave17Rules.studyTicks(9), "clamped");
        assertTrue(Wave17Rules.STUDY_COOLDOWN > Wave17Rules.studyTicks(3), "a study never overlaps the next one");
        assertEquals(-1, Wave17Rules.botanistLuck(0), "a book with nothing to say gives no luck");
        assertEquals(2, Wave17Rules.botanistLuck(3));
        assertEquals(5, Wave17Rules.historianXp(0));
        assertEquals(20, Wave17Rules.historianXp(3));
        assertEquals(10f, Wave17Rules.physicianHeal(3));
        assertEquals(8f, Wave17Rules.theologianWard(3));
        assertSame(Wave17Rules.Notebook.PHYSICIAN, Wave17Rules.Notebook.byId("physicians_notebook"));
        assertNull(Wave17Rules.Notebook.byId("fury_of_the_fallen"));
        assertEquals(5, Wave17Rules.Notebook.values().length);
    }

    @Test
    void lifeStoresTenAtEachDawnUpToTheGradeTable() {
        assertEquals(50, Wave17Rules.lifeMax(4), "a very rare cuff holds 50");
        assertEquals(30, Wave17Rules.lifeMax(3));
        assertEquals(90, Wave17Rules.lifeMax(7));
        assertEquals(10, Wave17Rules.lifeMax(1));
        assertEquals(20, Wave17Rules.lifeRoll(10, 5, 6, 50));
        assertEquals(10, Wave17Rules.lifeRoll(10, 6, 6, 50), "the same day adds nothing");
        assertEquals(10, Wave17Rules.lifeRoll(10, 9, 6, 50), "a clock that runs backwards adds nothing");
        assertEquals(50, Wave17Rules.lifeRoll(40, 1, 5, 50), "never above the maximum");
        assertEquals(10 + 9 * 10, Wave17Rules.lifeRoll(10, 0, 1000, 500), "a month away pays only nine dawns");
        assertEquals(0, Wave17Rules.lifeRoll(-4, 3, 3, 50));
    }

    @Test
    void spendingTheStoreHealsInProportionAndNeverBeyondTheMaximum() {
        assertEquals(20f, Wave17Rules.lifeHeal(50, 20f), 1e-4, "50 stored is a full 20-point bar");
        assertEquals(4f, Wave17Rules.lifeHeal(10, 20f), 1e-4);
        assertEquals(1f, Wave17Rules.lifeHeal(1, 20f), 1e-4, "at least one point");
        assertEquals(20f, Wave17Rules.lifeHeal(90, 20f), 1e-4, "capped at the maximum health");
        assertEquals(0f, Wave17Rules.lifeHeal(0, 20f));
    }

    @Test
    void theBluffAddsThreeAndBigCreaturesAreHarderToFool() {
        assertTrue(Wave17Rules.bluff(7, 10), "7 + 3 meets 10");
        assertFalse(Wave17Rules.bluff(6, 10));
        assertEquals(10, Wave17Rules.bluffDc(20));
        assertEquals(14, Wave17Rules.bluffDc(100));
        assertEquals(18, Wave17Rules.bluffDc(5000), "capped");
        assertFalse(Wave17Rules.bluff(10, Wave17Rules.bluffDc(5000)));
    }

    @Test
    void furyLightsBelowAFifthAndDoublesDamage() {
        assertTrue(Wave17Rules.furyActive(3.9f, 20f));
        assertFalse(Wave17Rules.furyActive(4.0f, 20f), "exactly a fifth is not below it");
        assertFalse(Wave17Rules.furyActive(0f, 20f), "dead is not furious");
        assertFalse(Wave17Rules.furyActive(5f, 0f));
        assertEquals(12f, Wave17Rules.furyDamage(6f));
    }

    @Test
    void theCupcakeKeepsHungerAtSeventeenOnlyWhileItLasts() {
        assertEquals(17, Wave17Rules.cupcakeFood(4, 100, 5000));
        assertEquals(19, Wave17Rules.cupcakeFood(19, 100, 5000), "never lowers a full stomach");
        assertEquals(4, Wave17Rules.cupcakeFood(4, 5000, 5000), "over at the end");
        assertEquals(72000L, Wave17Rules.CUPCAKE_TICKS, "three Minecraft days");
        assertTrue(Wave17Rules.CUPCAKE_FLOOR < 20, "you can still eat");
    }

    @Test
    void theDestinationCycleVisitsBedGraveMarksAndTheNewMarkSlot() {
        int saved = 2;
        assertEquals(5, Wave17Rules.choiceCount(saved));
        int c = 0;
        int[] seen = new int[5];
        for (int i = 0; i < 5; i++) { seen[i] = c; c = Wave17Rules.nextChoice(c, saved); }
        assertArrayEquals(new int[]{0, 1, 2, 3, 4}, seen);
        assertEquals(0, c, "and round again");
        assertEquals(-1, Wave17Rules.savedIndex(0, saved));
        assertEquals(-1, Wave17Rules.savedIndex(1, saved));
        assertEquals(0, Wave17Rules.savedIndex(2, saved));
        assertEquals(1, Wave17Rules.savedIndex(3, saved));
        assertTrue(Wave17Rules.isNewMark(4, saved));
        assertFalse(Wave17Rules.isNewMark(3, saved));
        assertEquals(1, Wave17Rules.nextChoice(0, 0), "no marks: bed, grave, new");
        assertEquals(0, Wave17Rules.nextChoice(2, 0));
        assertEquals(2, Wave17Rules.nextChoice(-5, 0), "a damaged selection still lands in range (-5 is 1 modulo 3)");
    }

    @Test
    void aNewMarkReplacesANearbyOneElseIsAddedElseDropsTheOldest() {
        assertEquals(1, Wave17Rules.markSlot(3, 1, 4.0), "a mark within eight blocks is replaced");
        assertEquals(3, Wave17Rules.markSlot(3, 1, 50.0), "a far one is added");
        assertEquals(0, Wave17Rules.markSlot(0, -1, Double.MAX_VALUE));
        assertEquals(-1, Wave17Rules.markSlot(Wave17Rules.MAX_MARKS, 2, 50.0), "full: the first goes");
        assertEquals(2, Wave17Rules.markSlot(Wave17Rules.MAX_MARKS, 2, 1.0), "full but near: replaced");
    }

    @Test
    void theUrnEatsAFixedShareOfAPieceAndDestroysItAtTheEnd() {
        assertEquals(1.5f, Wave17Rules.urnDamage(n -> 0));
        assertEquals(12f, Wave17Rules.urnDamage(n -> n - 1));
        assertEquals(60, Wave17Rules.corrode(0, 400));
        assertEquals(400, Wave17Rules.corrode(380, 400), "eaten through");
        assertEquals(1, Wave17Rules.corrode(0, 3));
        int d = 0, hits = 0;
        while (d < 200) { d = Wave17Rules.corrode(d, 200); hits++; }
        assertEquals(7, hits, "cumulative: a fifth of a hundred blows is seven");
        assertEquals(5, Wave17Rules.corrode(5, 0), "an unbreakable piece is left alone");
    }

    @Test
    void theWildMagicTableHasTwentyEntriesAndTwiceNeverRepeats() {
        assertEquals(20, Wave17Rules.Surge.values().length);
        assertSame(Wave17Rules.Surge.GLOW, Wave17Rules.surge(1));
        assertSame(Wave17Rules.Surge.SURGE_TWICE, Wave17Rules.surge(20));
        assertSame(Wave17Rules.Surge.GLOW, Wave17Rules.surge(-3), "clamped");
        assertEquals(List.of(Wave17Rules.Surge.HEAL), Wave17Rules.sweet(seq(4)));
        assertEquals(List.of(Wave17Rules.Surge.GLOW, Wave17Rules.Surge.FEATHER), Wave17Rules.sweet(seq(19, 0, 1)));
        assertEquals(List.of(Wave17Rules.Surge.FORTUNE, Wave17Rules.Surge.FORTUNE), Wave17Rules.sweet(seq(19, 19, 19)), "a repeated twice is read as Fortune");
        assertEquals("Glow", Wave17Rules.Surge.GLOW.label());
        assertEquals("Surge twice", Wave17Rules.Surge.SURGE_TWICE.label());
    }

    @Test
    void pouchesAreTwoOrThreeRowsAndTheDragonCannotKillAPlayer() {
        assertEquals(18, Wave17Rules.slots(Wave17Rules.POUCH_ROWS));
        assertEquals(27, Wave17Rules.slots(Wave17Rules.BACKPACK_ROWS));
        assertEquals(54, Wave17Rules.slots(99), "clamped to the six rows a chest menu has");
        assertEquals(9, Wave17Rules.slots(0));
        assertEquals(8, Wave17Rules.DRAGON_TICKS / Wave17Rules.DRAGON_BREATH_EVERY, "eight breaths");
        assertTrue(Wave17Rules.DRAGON_PLAYER_BUDGET <= 0.30);
        assertTrue(Wave17Rules.DEVIL_TICKS <= 6000, "the core caps allies at five minutes");
        assertEquals(20, Wave17Rules.SWEETS_PER_BAG);
        assertTrue(Wave17Rules.DEVIL_TYPES.contains("tensura:lesser_daemon"));
    }
}
