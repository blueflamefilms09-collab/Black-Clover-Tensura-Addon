package com.newuniverse.nusmp.skilltree.rarity;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.skilltree.rarity.BossThemes.Cand;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Guardian's book is a PROMISE, not a roll: the same floor and the same Region Theme always give the same book (so a player can plan a run, and two servers agree), the book fits the theme of
 * the dungeon it dropped in, and it is never a blocked franchise or a mythic book. This pins that on a synthetic table (so it does not depend on the shipped books): every Region Theme of the game has
 * an entry that names real backgrounds, the pick is the same across 1000 floors and every theme however the list is ordered, a blocked or mythic line is never chosen, the step down when a rarity
 * has no line, the themed preference with its fall-back to the whole rarity, and the pity walk that hands an owned-everything-themed player the next book instead of the same one.
 */
class BossThemesTest {
    /** 15 backgrounds x 5 rarities x 4 books each, ids like {@code magma_epic_2} */
    private static List<Cand> table() {
        List<Cand> out = new ArrayList<>();
        for (String bg : BossThemes.BACKGROUNDS) {
            for (BookRarity r : BookRarity.values()) {
                for (int i = 0; i < 4; i++) out.add(new Cand(bg + "_" + r.id() + "_" + i, r, bg, false));
            }
        }
        return out;
    }

    @Test
    void everyRegionThemeOfTheGameHasAnEntryThatNamesRealBackgrounds() {
        Set<String> real = new HashSet<>(BossThemes.BACKGROUNDS);
        assertEquals(15, real.size());
        for (RegionTheme t : RegionTheme.values()) {
            List<String> bgs = BossThemes.backgroundsOf(t.id());
            assertFalse(bgs.isEmpty(), "no entry for the theme " + t.id());
            assertTrue(bgs.size() <= 3, t.id() + " names too many backgrounds");
            for (String b : bgs) assertTrue(real.contains(b), t.id() + " names the unknown background " + b);
            assertEquals(bgs.size(), new HashSet<>(bgs).size(), t.id() + " repeats a background");
        }
        assertEquals(RegionTheme.values().length, BossThemes.themeIds().size(), "no entry for a theme that does not exist");
        assertTrue(BossThemes.backgroundsOf("no_such_theme").isEmpty());
        assertTrue(BossThemes.backgroundsOf(null).isEmpty());
        assertEquals(BossThemes.backgroundsOf("flame"), BossThemes.backgroundsOf("FLAME"));
    }

    @Test
    void theSameSiteGivesTheSameBookWhateverTheOrderOfTheList() {
        List<Cand> a = table(), b = new ArrayList<>(a);
        java.util.Collections.reverse(b);
        java.util.Collections.shuffle(b, new java.util.Random(5));
        for (int seed = 0; seed < 1000; seed++) {
            int floor = (seed % 9 + 1) * 10;
            String theme = BossThemes.themeIds().get(seed % BossThemes.themeIds().size());
            BookRarity want = seed % 2 == 0 ? BookRarity.RARE : BookRarity.EPIC;
            String one = BossThemes.pick(a, want, floor, theme, null);
            assertNotNull(one);
            assertEquals(one, BossThemes.pick(a, want, floor, theme, null), "repeatable, seed " + seed);
            assertEquals(one, BossThemes.pick(b, want, floor, theme, null), "independent of the list order, seed " + seed);
            assertEquals(one, BossThemes.pick(a, want, -floor, theme, null), "the road (the sign of the floor) does not matter, seed " + seed);
        }
    }

    @Test
    void theBookIsOfTheAskedRarityAndFitsTheTheme() {
        List<Cand> t = table();
        for (String theme : BossThemes.themeIds()) {
            List<String> fits = BossThemes.backgroundsOf(theme);
            for (int floor = 10; floor <= 90; floor += 10) {
                for (BookRarity want : new BookRarity[] {BookRarity.COMMON, BookRarity.UNCOMMON, BookRarity.RARE, BookRarity.EPIC}) {
                    String id = BossThemes.pick(t, want, floor, theme, null);
                    assertNotNull(id);
                    Cand c = t.stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
                    assertEquals(want, c.rarity(), id);
                    assertTrue(fits.contains(c.theme()), id + " does not fit " + theme);
                }
            }
        }
    }

    @Test
    void neverBlockedNeverMythicAndAMythicAskIsEpic() {
        List<Cand> t = new ArrayList<>(table());
        t.replaceAll(c -> c.id().startsWith("magma_") || c.id().startsWith("iron_") ? new Cand(c.id(), c.rarity(), c.theme(), true) : c);
        for (String theme : BossThemes.themeIds()) {
            for (int floor = 10; floor <= 90; floor += 10) {
                for (BookRarity want : BookRarity.values()) {
                    String id = BossThemes.pick(t, want, floor, theme, null);
                    assertNotNull(id);
                    Cand c = t.stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
                    assertFalse(c.blocked(), id + " is blocked");
                    assertNotEquals(BookRarity.MYTHIC, c.rarity(), id + " is mythic");
                }
            }
        }
        assertEquals(BossThemes.pick(t, BookRarity.EPIC, 50, "flame", null), BossThemes.pick(t, BookRarity.MYTHIC, 50, "flame", null));
    }

    @Test
    void aRarityWithNoLineStepsDownAndAnEmptyTableGivesNull() {
        List<Cand> onlyCommon = new ArrayList<>();
        for (int i = 0; i < 6; i++) onlyCommon.add(new Cand("c" + i, BookRarity.COMMON, "magma", false));
        String id = BossThemes.pick(onlyCommon, BookRarity.EPIC, 50, "flame", null);
        assertNotNull(id, "no epic line: the pick steps down to the next rarity that has one");
        assertTrue(id.startsWith("c"));
        assertNull(BossThemes.pick(List.of(), BookRarity.RARE, 10, "flame", null));
        assertNull(BossThemes.pick(null, BookRarity.RARE, 10, "flame", null));
        assertNull(BossThemes.pick(onlyCommon, null, 10, "flame", null));
        List<Cand> allBlocked = new ArrayList<>();
        for (Cand c : onlyCommon) allBlocked.add(new Cand(c.id(), c.rarity(), c.theme(), true));
        assertNull(BossThemes.pick(allBlocked, BookRarity.RARE, 10, "flame", null));
    }

    @Test
    void anUnknownThemeOrNoThemeFallsBackToTheWholeRarity() {
        List<Cand> t = table();
        assertNotNull(BossThemes.pick(t, BookRarity.RARE, 10, "no_such_theme", null));
        assertNotNull(BossThemes.pick(t, BookRarity.RARE, 10, "", null));
        assertNotNull(BossThemes.pick(t, BookRarity.RARE, 10, null, null));
        List<Cand> noTheme = new ArrayList<>();
        for (int i = 0; i < 5; i++) noTheme.add(new Cand("x" + i, BookRarity.RARE, "", false));
        assertNotNull(BossThemes.pick(noTheme, BookRarity.RARE, 10, "flame", null), "books with no theme are still in the rarity list");
    }

    @Test
    void thePityWalkHandsTheNextUnownedBookAndNeverSomethingOwnedWhenAnotherExists() {
        List<Cand> t = table();
        String first = BossThemes.pick(t, BookRarity.RARE, 20, "flame", null);
        Set<String> owned = new HashSet<>();
        for (int k = 0; k < 40; k++) {
            String id = BossThemes.pick(t, BookRarity.RARE, 20, "flame", owned::contains);
            assertNotNull(id);
            assertFalse(owned.contains(id), "step " + k + ": handed " + id + " again");
            if (k == 0) assertEquals(first, id, "an empty collection gets the seeded book");
            owned.add(id);
        }
        // flame fits magma and iron (8 rare books): from the 9th pick on the walk has left the themed list and goes on through the whole rarity (60 rare books in all)
        assertEquals(40, owned.size());
    }

    @Test
    void aPlayerWhoOwnsTheWholeRarityGetsTheSeededBookAgain() {
        List<Cand> t = table();
        String seeded = BossThemes.pick(t, BookRarity.EPIC, 50, "frost", null);
        assertEquals(seeded, BossThemes.pick(t, BookRarity.EPIC, 50, "frost", id -> true));
    }

    @Test
    void differentFloorsAndThemesSpreadOverTheBooks() {
        List<Cand> t = table();
        Set<String> seen = new HashSet<>();
        for (String theme : BossThemes.themeIds()) for (int floor = 10; floor <= 90; floor += 10) seen.add(BossThemes.pick(t, BookRarity.RARE, floor, theme, null));
        assertTrue(seen.size() >= 20, "the Guardian books are not all the same few: " + seen.size());
        assertNotEquals(BossThemes.seed(10, "flame", BookRarity.RARE), BossThemes.seed(20, "flame", BookRarity.RARE));
        assertNotEquals(BossThemes.seed(10, "flame", BookRarity.RARE), BossThemes.seed(10, "frost", BookRarity.RARE));
        assertNotEquals(BossThemes.seed(10, "flame", BookRarity.RARE), BossThemes.seed(10, "flame", BookRarity.EPIC));
        assertEquals(BossThemes.seed(10, "flame", BookRarity.RARE), BossThemes.seed(-10, "FLAME", BookRarity.RARE));
    }
}
