package com.newuniverse.nusmp.arc.world;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: which boss stands at the end of a boss floor must be the same for everybody, every time (no reroll by leaving and coming back), must fall back to the
 * plain Guardian whenever there is any doubt, must favour a boss that belongs to the floor's theme and must never offer a boss the party's story has not opened.
 * These tests pin each rule with fake characters, and the determinism with a sweep over many floors.
 */
class FloorBossChoiceTest {
    private static ArcCharacter boss(String id, int lo, int hi, Polarity road, String... themes) {
        return ArcCharacter.boss(id, id).floors(road, lo, hi).themes(themes).build();
    }

    private static FloorBossChoice.Request req(long seed, int sector, int floor, Polarity road, String theme, double chance, boolean on, List<ArcCharacter> standing,
                                                java.util.function.Predicate<ArcCharacter> allowed) {
        return new FloorBossChoice.Request(seed, sector, floor, road, theme, chance, on, standing, allowed);
    }

    private static final List<ArcCharacter> TWO = List.of(boss("aa", 10, 40, null), boss("bb", 10, 40, null));

    @Test
    void nothingStandingOrTheLayerOffOrZeroChanceIsTheGuardian() {
        assertNull(FloorBossChoice.choose(null));
        assertNull(FloorBossChoice.choose(req(1, 3, 10, Polarity.HOLY, "radiant", 1.0, true, List.of(), c -> true)));
        assertNull(FloorBossChoice.choose(req(1, 3, 10, Polarity.HOLY, "radiant", 1.0, true, null, c -> true)));
        assertNull(FloorBossChoice.choose(req(1, 3, 10, Polarity.HOLY, "radiant", 1.0, false, TWO, c -> true)));
        assertNull(FloorBossChoice.choose(req(1, 3, 10, Polarity.HOLY, "radiant", 0.0, true, TWO, c -> true)));
        assertNull(FloorBossChoice.choose(req(1, 3, 10, Polarity.HOLY, "radiant", -3.0, true, TWO, c -> true)));
        assertNull(FloorBossChoice.choose(req(1, 3, 10, Polarity.HOLY, "radiant", Double.NaN, true, TWO, c -> true)));
    }

    @Test
    void chanceOneAlwaysHostsAndTheCoinIsTheSameEveryTime() {
        for (int floor = 10; floor <= 40; floor += 10) {
            ArcCharacter c = FloorBossChoice.choose(req(7, floor / 10, floor, Polarity.HOLY, "radiant", 1.0, true, TWO, x -> true));
            assertNotNull(c, "floor " + floor);
            for (int again = 0; again < 5; again++) assertSame(c, FloorBossChoice.choose(req(7, floor / 10, floor, Polarity.HOLY, "radiant", 1.0, true, TWO, x -> true)));
        }
        int yes = 0;
        for (int sector = 0; sector < 2000; sector++) {
            boolean a = FloorBossChoice.hosts(99, sector, 10 * (sector % 9 + 1), 0.5);
            assertEquals(a, FloorBossChoice.hosts(99, sector, 10 * (sector % 9 + 1), 0.5), "the coin of sector " + sector + " changed");
            if (a) yes++;
        }
        assertTrue(yes > 850 && yes < 1150, "a chance of one half hosts about half the floors: " + yes);
        assertFalse(FloorBossChoice.hosts(1, 1, 10, 0.0));
        assertTrue(FloorBossChoice.hosts(1, 1, 10, 1.0));
        assertTrue(FloorBossChoice.hosts(1, 1, 10, 7.5));
    }

    @Test
    void theListOrderDoesNotChangeTheChoice() {
        List<ArcCharacter> many = new ArrayList<>();
        for (String id : List.of("ee", "bb", "dd", "aa", "cc")) many.add(boss(id, 10, 90, null));
        for (int sector = 0; sector < 60; sector++) {
            ArcCharacter a = FloorBossChoice.choose(req(5, sector, 10, Polarity.ABYSSAL, "abyss", 1.0, true, many, c -> true));
            List<ArcCharacter> shuffled = new ArrayList<>(many);
            Collections.shuffle(shuffled, new java.util.Random(sector));
            ArcCharacter b = FloorBossChoice.choose(req(5, sector, 10, Polarity.ABYSSAL, "abyss", 1.0, true, shuffled, c -> true));
            assertSame(a, b);
        }
    }

    @Test
    void aBossOnlyStandsWhereItsFloorsAndRoadAllow() {
        List<ArcCharacter> only = List.of(boss("holy_only", 20, 30, Polarity.HOLY));
        assertNull(FloorBossChoice.choose(req(1, 1, 10, Polarity.HOLY, "radiant", 1.0, true, only, c -> true)), "too shallow");
        assertNull(FloorBossChoice.choose(req(1, 1, 40, Polarity.HOLY, "radiant", 1.0, true, only, c -> true)), "too deep");
        assertNull(FloorBossChoice.choose(req(1, 1, -20, Polarity.ABYSSAL, "abyss", 1.0, true, only, c -> true)), "the wrong road");
        assertNotNull(FloorBossChoice.choose(req(1, 1, 20, Polarity.HOLY, "radiant", 1.0, true, only, c -> true)));
        assertNotNull(FloorBossChoice.choose(req(1, 1, 30, Polarity.HOLY, "radiant", 1.0, true, only, c -> true)));
        List<ArcCharacter> npc = List.of(ArcCharacter.npc("friend", "Friend").build());
        assertNull(FloorBossChoice.choose(req(1, 1, 20, Polarity.HOLY, "radiant", 1.0, true, npc, c -> true)), "a friendly character is never a floor boss");
    }

    @Test
    void theStoryGateSkipsABossThePartyHasNotOpened() {
        for (int sector = 0; sector < 40; sector++) {
            ArcCharacter c = FloorBossChoice.choose(req(3, sector, 10, Polarity.HOLY, "radiant", 1.0, true, TWO, x -> x.id().equals("bb")));
            assertNotNull(c);
            assertEquals("bb", c.id(), "only the open one may stand");
        }
        assertNull(FloorBossChoice.choose(req(3, 1, 10, Polarity.HOLY, "radiant", 1.0, true, TWO, x -> false)), "nothing is open: the Guardian");
        assertNotNull(FloorBossChoice.choose(req(3, 1, 10, Polarity.HOLY, "radiant", 1.0, true, TWO, null)), "a null filter is the gate switched off");
    }

    @Test
    void aBossThatNamesOtherThemesNeverStandsOnThisOneAndOneThatNamesItIsFavoured() {
        ArcCharacter blood = boss("blood_boss", 10, 40, null, "blood", "shadow");
        ArcCharacter any = boss("any_boss", 10, 40, null);
        ArcCharacter radiantBoss = boss("radiant_boss", 10, 40, null, "radiant");
        assertTrue(FloorBossChoice.themeFits(any, "radiant") && FloorBossChoice.themeFits(any, ""));
        assertFalse(FloorBossChoice.themeFits(blood, "radiant"));
        assertTrue(FloorBossChoice.themeFits(blood, "BLOOD"));
        assertEquals(FloorBossChoice.FAVOURED_WEIGHT, FloorBossChoice.weight(radiantBoss, "radiant"));
        assertEquals(FloorBossChoice.NEUTRAL_WEIGHT, FloorBossChoice.weight(any, "radiant"));
        List<ArcCharacter> all = List.of(blood, any, radiantBoss);
        Map<String, Integer> tally = new HashMap<>();
        for (int sector = 0; sector < 4000; sector++) {
            ArcCharacter c = FloorBossChoice.choose(req(11, sector, 20, Polarity.HOLY, "radiant", 1.0, true, all, x -> true));
            assertNotNull(c);
            tally.merge(c.id(), 1, Integer::sum);
        }
        assertNull(tally.get("blood_boss"), "a blood boss never stands in a radiant hall: " + tally);
        int r = tally.getOrDefault("radiant_boss", 0), a = tally.getOrDefault("any_boss", 0);
        assertTrue(r > 2 * a && r < 4 * a, "the favoured boss is about three times as likely: " + tally);
        assertNull(FloorBossChoice.choose(req(1, 1, 20, Polarity.HOLY, "radiant", 1.0, true, List.of(blood), c -> true)), "when every candidate belongs elsewhere the Guardian stands");
    }

    @Test
    void everyBossGetsFloorsOverManySectors() {
        Map<String, Integer> tally = new HashMap<>();
        for (int sector = 0; sector < 3000; sector++) {
            ArcCharacter c = FloorBossChoice.choose(req(42, sector, 10 * (sector % 9 + 1), Polarity.HOLY, "gravity", 1.0, true,
                    List.of(boss("x1", 10, 90, null), boss("x2", 10, 90, null), boss("x3", 10, 90, null)), x -> true));
            tally.merge(c.id(), 1, Integer::sum);
        }
        for (String id : List.of("x1", "x2", "x3")) assertTrue(tally.getOrDefault(id, 0) > 800, id + " is starved: " + tally);
    }

    @Test
    void differentDungeonsDecideDifferently() {
        int differ = 0;
        for (int sector = 0; sector < 200; sector++) {
            ArcCharacter a = FloorBossChoice.choose(req(1, sector, 30, Polarity.HOLY, "gravity", 1.0, true, TWO, x -> true));
            ArcCharacter b = FloorBossChoice.choose(req(2, sector, 30, Polarity.HOLY, "gravity", 1.0, true, TWO, x -> true));
            if (a != b) differ++;
        }
        assertTrue(differ > 40, "two seeds agree on nearly every floor: " + differ);
    }
}
