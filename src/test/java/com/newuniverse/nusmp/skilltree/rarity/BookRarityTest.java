package com.newuniverse.nusmp.skilltree.rarity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the five rarities are saved by id on the stack, written into the book table and turned into a model property, a colour and a lang key, so one wrong mapping is silent (a mythic book that reads as
 * common, a model that shows the wrong cover). This pins the enum: the order (common to mythic, append-only), the ids, the colours the owner named, the glint on epic and mythic only, the model value
 * {@code (rank + 1) / 6} and its way back (0 stays free for "unknown"), and the forgiving readers (an unknown or damaged id is common, never an exception).
 */
class BookRarityTest {
    @Test
    void theFiveRaritiesInOrderWithTheirIds() {
        assertEquals(5, BookRarity.count());
        String[] ids = {"common", "uncommon", "rare", "epic", "mythic"};
        for (int i = 0; i < 5; i++) {
            assertEquals(ids[i], BookRarity.values()[i].id());
            assertEquals(i, BookRarity.values()[i].rank());
            assertEquals("rarity.nusmp." + ids[i], BookRarity.values()[i].langKey());
            assertEquals(Character.toUpperCase(ids[i].charAt(0)) + ids[i].substring(1), BookRarity.values()[i].label());
        }
    }

    @Test
    void theColoursAndTheGlint() {
        assertEquals(0xFFFFFF, BookRarity.COMMON.color());
        assertEquals(0x55FF55, BookRarity.UNCOMMON.color());
        assertEquals(0x55AAFF, BookRarity.RARE.color());
        assertEquals(0xC855FF, BookRarity.EPIC.color());
        assertEquals(0xFFAA2B, BookRarity.MYTHIC.color());
        for (BookRarity r : BookRarity.values()) {
            assertEquals(r.rank() >= 3, r.glint(), r.id());
            assertEquals(r.color(), r.particleColor());
        }
    }

    @Test
    void theModelValueIsRankPlusOneOverSixAndComesBack() {
        for (BookRarity r : BookRarity.values()) {
            assertEquals((r.rank() + 1) / 6f, r.modelValue(), 1e-6);
            assertSame(r, BookRarity.fromModelValue(r.modelValue()));
        }
        assertNull(BookRarity.fromModelValue(0f), "0 means unknown");
        assertNull(BookRarity.fromModelValue(-1f));
        assertNull(BookRarity.fromModelValue(2f));
        assertNull(BookRarity.fromModelValue(Float.NaN));
    }

    @Test
    void readersAreForgiving() {
        assertSame(BookRarity.EPIC, BookRarity.fromId("EPIC"));
        assertSame(BookRarity.EPIC, BookRarity.fromId("  epic "));
        assertSame(BookRarity.COMMON, BookRarity.fromId(null));
        assertSame(BookRarity.COMMON, BookRarity.fromId(""));
        assertSame(BookRarity.COMMON, BookRarity.fromId("legendary"));
        assertNull(BookRarity.byId("legendary"));
        assertNull(BookRarity.byId(null));
        assertSame(BookRarity.COMMON, BookRarity.fromRank(-5));
        assertSame(BookRarity.MYTHIC, BookRarity.fromRank(99));
        assertSame(BookRarity.RARE, BookRarity.fromRank(2));
    }
}
