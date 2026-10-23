package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.avatar.cast.PageTier;
import com.newuniverse.nusmp.core.discovery.DiscoveryRules;
import com.newuniverse.nusmp.multiverse.PageSlots;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: a grimoire stored which pages are open as one int (1 << mode), and Java masks a shift by 32, so mode 32 would have aliased mode 0. The "15 spells for every grimoire" project needs more than 32 family
 * pages in the Flame book (24 + 9), so the mask is 64 bits (modes 0-31 in the old int tag "Unlocked", 32-63 in "Unlocked2"). This pins the arithmetic: bits above 31 never alias, the old int is exactly what it was,
 * the avatar's page tier, the discovery reader and the slot rule follow. Pure (no Minecraft class).
 */
class UnlockMaskTest {
    @Test
    void everyModeFrom0To63IsItsOwnBitAndNeverAliasesAnother() {
        for (int a = 0; a < 64; a++) {
            long m = UnlockMask.with(0L, a);
            for (int b = 0; b < 64; b++) assertEquals(a == b, UnlockMask.has(m, b), "set " + a + ", asked " + b);
            assertEquals(0L, UnlockMask.without(m, a), "cleared " + a);
        }
    }

    @Test
    void mode32IsNotMode0AndMode33IsNotMode1() {
        long m = UnlockMask.with(UnlockMask.with(0L, 0), 1);
        assertFalse(UnlockMask.has(m, 32));
        assertFalse(UnlockMask.has(m, 33));
        long hi = UnlockMask.with(0L, 33);
        assertFalse(UnlockMask.has(hi, 1));
        assertEquals(0, UnlockMask.lo(hi), "a high mode leaves the old int untouched");
        assertEquals(2, UnlockMask.hi(hi));
    }

    @Test
    void anOldItemWithOnlyTheLowIntLoadsUnchanged() {
        int old = (1 << 1) | (1 << 5) | (1 << 31);      // bit 31 is the sign bit of the old int and was a valid mode
        long m = UnlockMask.join(old, 0);
        for (int k = 0; k < 32; k++) assertEquals((old & (1 << k)) != 0, UnlockMask.has(m, k), "mode " + k);
        for (int k = 32; k < 64; k++) assertFalse(UnlockMask.has(m, k), "mode " + k);
        assertEquals(old, UnlockMask.lo(m));
        assertEquals(0, UnlockMask.hi(m));
        assertTrue(UnlockMask.has(m, 31));
    }

    @Test
    void joinAndSplitRoundTripIncludingNegativeInts() {
        for (int lo : new int[]{0, 1, -1, Integer.MIN_VALUE, Integer.MAX_VALUE, 0x12345678})
            for (int hi : new int[]{0, 1, -1, Integer.MIN_VALUE, 0x7ABCDEF0}) {
                long m = UnlockMask.join(lo, hi);
                assertEquals(lo, UnlockMask.lo(m));
                assertEquals(hi, UnlockMask.hi(m));
            }
    }

    @Test
    void outOfRangeModesAreNeverOpenAndNeverChangeAnything() {
        long all = -1L;
        assertFalse(UnlockMask.has(all, -1));
        assertFalse(UnlockMask.has(all, 64));
        assertFalse(UnlockMask.in(64));
        assertEquals(5L, UnlockMask.with(5L, 64));
        assertEquals(5L, UnlockMask.with(5L, -3));
        assertEquals(5L, UnlockMask.without(5L, 100));
    }

    @Test
    void firstNAndCountCoverTheFamily() {
        assertEquals(0L, UnlockMask.firstN(0));
        assertEquals(1L, UnlockMask.firstN(1));
        assertEquals(0xFFFFFFFFL, UnlockMask.firstN(32));
        assertEquals((1L << 33) - 1, UnlockMask.firstN(33));
        assertEquals(-1L, UnlockMask.firstN(64));
        assertEquals(-1L, UnlockMask.firstN(500));
        assertEquals(33, UnlockMask.count(UnlockMask.firstN(33), 33));
        assertEquals(30, UnlockMask.count(UnlockMask.firstN(33), 30), "only the first family modes count");
        assertEquals(0, UnlockMask.count(-1L, 0));
    }

    @Test
    void theAvatarPageTierMaskFollowsTheNewWidthAndKeepsTheOldOne() {
        for (int pages = 0; pages <= 30; pages++) assertEquals(PageTier.mask(pages) & 0xFFFFFFFFL, PageTier.mask64(pages) & 0xFFFFFFFFL, "pages " + pages);
        assertEquals(0L, PageTier.mask64(0));
        assertEquals((1L << 33) - 1, PageTier.mask64(33));
        assertEquals(-1L, PageTier.mask64(64));
        assertEquals(-1L, PageTier.mask64(99));
        assertEquals(0x7FFFFFFF, PageTier.mask(40), "the old int mask stays as it was");
    }

    @Test
    void theDiscoveryReaderSeesModesAbove31() {
        long m = UnlockMask.with(UnlockMask.with(0L, 2), 40);
        assertEquals(List.of(0, 2, 40), DiscoveryRules.unlockedModes(m, 41));
        assertEquals(List.of(0, 2), DiscoveryRules.unlockedModes(m, 40), "past the family count is not a page");
        assertEquals(64, DiscoveryRules.unlockedModes(-1L, 99).size());
        assertEquals(List.of(0), DiscoveryRules.unlockedModes(0L, 6));
        assertEquals(List.of(0, 1, 3), DiscoveryRules.unlockedModes((1 << 1) | (1 << 3), 6), "the int overload is the old one");
    }

    @Test
    void aFifteenPageGrimoireHasEveryPageOpenAtFullMasteryEvenWithAnOldConfig() {
        // new defaults 6 / 15 / +4 / cap 19
        assertEquals(6, PageSlots.slots(6, 15, 4, 19, 15, 0.0, false));
        assertEquals(15, PageSlots.slots(6, 15, 4, 19, 15, 1.0, false));
        assertEquals(19, PageSlots.slots(6, 15, 4, 19, 15, 1.0, true));
        // an existing config file keeps 12 / 16: the visible pages win
        assertEquals(15, PageSlots.slots(6, 12, 4, 16, 15, 1.0, false), "the ceiling is never below the pages the grimoire shows");
        assertEquals(16, PageSlots.slots(6, 12, 4, 16, 15, 1.0, true), "the old cap holds with a contract");
        assertEquals(20, PageSlots.slots(6, 12, 4, 16, 20, 1.0, false), "a 20-page grimoire can open all 20 even with the old cap");
        // a small book behaves as before: ceiling 12 gave 9 open slots at half mastery
        assertEquals(9, PageSlots.slots(6, 12, 4, 16, 8, 0.5, false));
        assertEquals(10, PageSlots.slots(6, 15, 4, 19, 8, 0.5, false), "with the new default ceiling 15 half mastery gives 10");
        // garbage mastery is clamped
        assertEquals(6, PageSlots.slots(6, 15, 4, 19, 15, -3.0, false));
        assertEquals(15, PageSlots.slots(6, 15, 4, 19, 15, 9.0, false));
    }
}
