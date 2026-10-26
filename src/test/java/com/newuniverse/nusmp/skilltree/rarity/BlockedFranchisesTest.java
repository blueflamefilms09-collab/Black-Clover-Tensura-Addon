package com.newuniverse.nusmp.skilltree.rarity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the blacklist of whole franchises (Tensura, Black Clover, Magi by default) is typed by a server owner into a TOML string, so the parser has to take every kind of hand-typed input without throwing and
 * without letting junk through: empty, spaces, upper case, semicolons, duplicates, characters that are not part of an id, and an absurdly long list. The checker must treat a line with no franchise as never
 * blocked, and the facade (how the game reaches the setting) must never throw, never parse the same text twice, and block nothing when no source is installed (the client, every unit test).
 */
class BlockedFranchisesTest {
    private final Supplier<String> before = BlockedFranchises.source();

    @AfterEach
    void putBack() { BlockedFranchises.setSource(before); }

    @Test
    void theDefaultIsTheThreeFranchisesOfTheOwner() {
        assertEquals(Set.of("tensura", "black_clover", "magi"), BlockedFranchises.parse(BlockedFranchises.DEFAULT));
    }

    @Test
    void emptyAndBlankBlockNothing() {
        assertTrue(BlockedFranchises.parse(null).isEmpty());
        assertTrue(BlockedFranchises.parse("").isEmpty());
        assertTrue(BlockedFranchises.parse("   \t\n").isEmpty());
        assertTrue(BlockedFranchises.parse(",,;; ,").isEmpty());
    }

    @Test
    void separatorsCaseAndSpacesAreForgiven() {
        assertEquals(Set.of("tensura", "magi"), BlockedFranchises.parse(" Tensura ,MAGI "));
        assertEquals(Set.of("tensura", "magi", "danmachi"), BlockedFranchises.parse("tensura;magi danmachi"));
        assertEquals(Set.of("tensura"), BlockedFranchises.parse("tensura,tensura,TENSURA"));
        assertEquals(Set.of("a", "b"), BlockedFranchises.parse("a\nb"));
    }

    @Test
    void junkEntriesAreIgnoredNotThrown() {
        assertEquals(Set.of("tensura"), BlockedFranchises.parse("tensura, ../../etc, <script>, \"quoted\", ünï, a-b, minecraft:stone"));
        assertTrue(BlockedFranchises.parse("x".repeat(41)).isEmpty(), "an id longer than 40 characters is not a franchise key");
        assertEquals(Set.of("x".repeat(40)), BlockedFranchises.parse("x".repeat(40)));
    }

    @Test
    void anAbsurdListIsCapped() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) sb.append("f").append(i).append(',');
        Set<String> s = BlockedFranchises.parse(sb.toString());
        assertTrue(s.size() <= BlockedFranchises.MAX_ENTRIES, "capped: " + s.size());
        assertTrue(s.size() > 0);
        assertTrue(BlockedFranchises.parse("a".repeat(100_000)).isEmpty() || BlockedFranchises.parse("a".repeat(100_000)).size() <= 1);
    }

    @Test
    void theParsedSetCannotBeChanged() {
        assertThrows(UnsupportedOperationException.class, () -> BlockedFranchises.parse("tensura").add("x"));
    }

    @Test
    void theCheckerIsCaseInsensitiveAndNeverBlocksAnUnknownFranchise() {
        Set<String> set = BlockedFranchises.parse("tensura");
        assertTrue(BlockedFranchises.blocked(set, "tensura"));
        assertTrue(BlockedFranchises.blocked(set, "TENSURA"));
        assertFalse(BlockedFranchises.blocked(set, "danmachi"));
        assertFalse(BlockedFranchises.blocked(set, ""), "a classic route has no franchise");
        assertFalse(BlockedFranchises.blocked(set, null));
        assertFalse(BlockedFranchises.blocked(null, "tensura"));
        assertFalse(BlockedFranchises.blocked(Set.of(), "tensura"));
    }

    @Test
    void theFacadeBlocksNothingWithoutASource() {
        BlockedFranchises.setSource(null);
        assertEquals("", BlockedFranchises.setting());
        assertTrue(BlockedFranchises.current().isEmpty());
        assertFalse(BlockedFranchises.isBlocked("tensura"));
    }

    @Test
    void theFacadeReadsTheSourceAndParsesEachTextOnce() {
        String[] text = {"tensura,magi"};
        int[] reads = {0};
        BlockedFranchises.setSource(() -> {
            reads[0]++;
            return text[0];
        });
        Set<String> first = BlockedFranchises.current();
        assertTrue(BlockedFranchises.isBlocked("magi"));
        assertSame(first, BlockedFranchises.current(), "the same text is not parsed again");
        text[0] = "danmachi";
        assertTrue(BlockedFranchises.isBlocked("danmachi"));
        assertFalse(BlockedFranchises.isBlocked("magi"), "a changed setting is seen on the next question");
        text[0] = "";
        assertFalse(BlockedFranchises.isBlocked("danmachi"));
        assertTrue(reads[0] >= 4);
    }

    @Test
    void aBrokenSourceNeverThrowsAndBlocksNothing() {
        BlockedFranchises.setSource(() -> { throw new IllegalStateException("config not loaded"); });
        assertEquals("", BlockedFranchises.setting());
        assertFalse(BlockedFranchises.isBlocked("tensura"));
        BlockedFranchises.setSource(() -> null);
        assertEquals("", BlockedFranchises.setting());
    }

    @Test
    void theConfigSnapshotUsesTheListOnlyWhileItIsSwitchedOn() {
        RarityConfig on = new RarityConfig(true, 1.0, 0.001, true, "tensura");
        assertEquals("tensura", on.effectiveBlocked());
        assertEquals("", on.withBlocked(false, "tensura").effectiveBlocked());
        assertEquals("", new RarityConfig(true, 1.0, 0.001, true, null).effectiveBlocked());
        RarityConfig wild = new RarityConfig(true, Double.NaN, 7.0, true, "x");
        assertEquals(0.0, wild.guardianChance(), 0.0);
        assertEquals(1.0, wild.baseChance(), 0.0);
        assertFalse(RarityConfig.DEFAULT.withTiers(false).tiers());
        assertEquals(RarityConfig.DEFAULT.baseChance(), RarityConfig.DEFAULT.withTiers(false).baseChance(), 0.0);
    }
}
