package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the fallback when a theme has no table decides what a whole road feels like, and it has several branches that only matter when a data pack removed a file,
 * which nothing in a normal run exercises; these tests pin every branch: the theme's own file wins, a road borrows its own table, the default is the last resort,
 * and a missing default means no table at all.
 */
class TableChoiceTest {
    private static final Set<String> ALL = Set.of("default", "gravity", "blood", "radiant", "abyss");

    @Test
    void theThemesOwnTableAlwaysWins() {
        assertEquals("gravity", TableChoice.pick(ALL, "gravity", Polarity.HOLY));
        assertEquals("gravity", TableChoice.pick(ALL, "gravity", Polarity.ABYSSAL));
        assertEquals("blood", TableChoice.pick(ALL, "blood", Polarity.HOLY), "a Holy sector that drew the blood theme meets blood monsters");
        assertEquals("radiant", TableChoice.pick(ALL, "radiant", Polarity.ABYSSAL), "a theme file is a theme file, whatever the road");
        assertEquals("abyss", TableChoice.pick(ALL, "abyss", Polarity.HOLY));
        assertEquals("gravity", TableChoice.pick(ALL, "gravity", null));
    }

    @Test
    void aMissingThemeTableBorrowsTheRoadsOwn() {
        assertEquals("radiant", TableChoice.pick(ALL, "storm", Polarity.HOLY));
        assertEquals("abyss", TableChoice.pick(ALL, "storm", Polarity.ABYSSAL));
        assertEquals("radiant", TableChoice.pick(ALL, null, Polarity.HOLY));
        assertEquals("abyss", TableChoice.pick(ALL, "", Polarity.ABYSSAL));
        assertEquals("abyss", TableChoice.pick(List.of("abyss", "default"), "frost", Polarity.ABYSSAL));
    }

    @Test
    void withoutAPolarityTheDefaultAnswers() {
        assertEquals("default", TableChoice.pick(ALL, "storm", null));
        assertEquals("default", TableChoice.pick(ALL, null, null));
    }

    @Test
    void aRoadWhoseOwnTableIsGoneFallsBackToTheDefault() {
        Set<String> noRoads = Set.of("default", "gravity");
        assertEquals("default", TableChoice.pick(noRoads, "storm", Polarity.HOLY));
        assertEquals("default", TableChoice.pick(noRoads, "storm", Polarity.ABYSSAL));
        Set<String> onlyHoly = Set.of("default", "radiant");
        assertEquals("default", TableChoice.pick(onlyHoly, "storm", Polarity.ABYSSAL), "the Abyssal road does not borrow the Holy table");
        assertEquals("radiant", TableChoice.pick(onlyHoly, "storm", Polarity.HOLY));
        Set<String> onlyAbyss = Set.of("default", "abyss");
        assertEquals("default", TableChoice.pick(onlyAbyss, "storm", Polarity.HOLY), "the Holy road does not borrow the Abyssal table");
    }

    @Test
    void withNoDefaultEitherThereIsNoTable() {
        assertNull(TableChoice.pick(Set.of("gravity"), "storm", Polarity.HOLY));
        assertNull(TableChoice.pick(Set.of("gravity"), "storm", null));
        assertNull(TableChoice.pick(Set.of(), "storm", Polarity.ABYSSAL));
        assertNull(TableChoice.pick(null, "storm", Polarity.HOLY));
        assertEquals("radiant", TableChoice.pick(Set.of("radiant"), "storm", Polarity.HOLY), "a road table alone is enough for its road");
    }

    @Test
    void theNamesAreTheFilesOfTheShippedData() {
        assertEquals("default", TableChoice.DEFAULT);
        assertEquals("radiant", TableChoice.HOLY);
        assertEquals("abyss", TableChoice.ABYSSAL);
    }
}
