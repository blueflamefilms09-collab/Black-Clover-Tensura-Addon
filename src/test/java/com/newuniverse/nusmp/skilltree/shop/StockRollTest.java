package com.newuniverse.nusmp.skilltree.shop;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the shelf must be the same for every player and after every restart (so it is a pure function of the seed and the period), must stay inside each line's band, must keep
 * the flagship in the top third of its band, and must still MOVE from one restock to the next (a roll that ignored the period would be a fixed price list, not a market). These
 * tests pin each of those with many samples, and a stuck or biased mixer shows as a failed spread or mean.
 */
class StockRollTest {
    private static final long SEED = 123456789012345L;

    private static StockEntry book(long min, long max, double bandLow, int stockMin, int stockMax, int weight) {
        return new StockEntry("e", StockEntry.Kind.ROUTE_BOOK, "nusmp:x", false, "", min, max, bandLow, stockMin, stockMax, weight, false);
    }

    private static final StockEntry FULL = book(2_000_000, 3_000_000, 0.0, 1, 1, 100);
    private static final StockEntry FANG = book(2_000_000, 3_000_000, 0.6667, 1, 1, 100);

    @Test
    void theSameInputsGiveTheSameRollEveryTime() {
        for (long period = 0; period < 50; period++) {
            StockRoll.Roll a = StockRoll.roll(SEED, period, 0, "fang", FANG);
            StockRoll.Roll b = StockRoll.roll(SEED, period, 0, "fang", FANG);
            assertEquals(a, b);
        }
    }

    @Test
    void thePriceStaysInsideTheBandAndOnMarketNumbers() {
        for (long period = 0; period < 5_000; period++) {
            StockRoll.Roll r = StockRoll.roll(SEED, period, 0, "books@x", FULL);
            assertTrue(r.price() >= FULL.priceMin() && r.price() <= FULL.priceMax(), "price " + r.price() + " at period " + period);
            assertEquals(0, r.price() % 1_000, "a price in the millions is a multiple of 1,000: " + r.price());
        }
        StockEntry cheap = book(120_000, 180_000, 0.0, 3, 5, 100);
        for (long period = 0; period < 2_000; period++) {
            long p = StockRoll.roll(SEED, period, 0, "c", cheap).price();
            assertTrue(p >= 120_000 && p <= 180_000, "cheap price " + p);
            assertEquals(0, p % 1_000, "" + p);
        }
        StockEntry tiny = book(1_234, 1_234, 0.0, 1, 1, 100);
        assertEquals(1_234, StockRoll.roll(SEED, 3, 0, "t", tiny).price(), "a band of one price is exactly that price, rounding never leaves the band");
    }

    @Test
    void theFlagshipAlwaysLandsInTheTopThirdOfItsBand() {
        double bandStart = FANG.bandStart();
        assertTrue(bandStart >= 2_000_000 + 1_000_000 * (2.0 / 3.0), "the shipped band floor is not below two thirds");
        long lowest = Long.MAX_VALUE, highest = 0;
        for (long period = 0; period < 5_000; period++) {
            long p = StockRoll.roll(SEED, period, 0, "fang", FANG).price();
            assertTrue(p >= bandStart, "price " + p + " is below the top third (" + bandStart + ") at period " + period);
            assertTrue(p <= 3_000_000, "price " + p);
            lowest = Math.min(lowest, p);
            highest = Math.max(highest, p);
        }
        assertTrue(lowest < 2_700_000, "the roll reaches the bottom of the top third, lowest " + lowest);
        assertTrue(highest > 2_950_000, "the roll reaches the top of the band, highest " + highest);
    }

    @Test
    void aFullBandIsUsedEvenlyAndNotStuck() {
        double sum = 0;
        long min = Long.MAX_VALUE, max = 0;
        int n = 4_000;
        for (long period = 0; period < n; period++) {
            long p = StockRoll.roll(SEED, period, 0, "books@y", FULL).price();
            sum += p;
            min = Math.min(min, p);
            max = Math.max(max, p);
        }
        double mean = sum / n;
        assertTrue(Math.abs(mean - 2_500_000) < 40_000, "the mean of a uniform band is its middle, got " + mean);
        assertTrue(min < 2_030_000 && max > 2_970_000, "both ends of the band are reached: " + min + " .. " + max);
    }

    @Test
    void differentDaysDifferAndSoDoDifferentSlotsSeedsAndRestocks() {
        Set<Long> prices = new HashSet<>();
        for (long period = 0; period < 100; period++) prices.add(StockRoll.roll(SEED, period, 0, "books@x", FULL).price());
        assertTrue(prices.size() > 60, "100 restocks should give many different prices, got " + prices.size());

        int slotDiffers = 0, seedDiffers = 0, epochDiffers = 0;
        for (long period = 0; period < 50; period++) {
            long base = StockRoll.roll(SEED, period, 0, "books@x", FULL).price();
            if (StockRoll.roll(SEED, period, 0, "books@y", FULL).price() != base) slotDiffers++;
            if (StockRoll.roll(SEED + 1, period, 0, "books@x", FULL).price() != base) seedDiffers++;
            if (StockRoll.roll(SEED, period, 1, "books@x", FULL).price() != base) epochDiffers++;
        }
        assertTrue(slotDiffers > 40, "two slots of one line roll on their own: " + slotDiffers);
        assertTrue(seedDiffers > 40, "two worlds have two markets: " + seedDiffers);
        assertTrue(epochDiffers > 40, "an operator's restock command makes a new shelf: " + epochDiffers);
        assertNotEquals(StockRoll.roll(SEED, 7, 0, "a", FULL), StockRoll.roll(SEED, 8, 0, "a", FULL));
    }

    @Test
    void theWeightIsTheChanceOfBeingOnTheShelf() {
        StockEntry never = book(1, 2, 0.0, 1, 1, 0);
        StockEntry always = book(1, 2, 0.0, 1, 1, 100);
        StockEntry half = book(1, 2, 0.0, 1, 1, 50);
        int present = 0, n = 4_000;
        for (long period = 0; period < n; period++) {
            assertFalse(StockRoll.roll(SEED, period, 0, "n", never).present(), "weight 0 never appears");
            assertTrue(StockRoll.roll(SEED, period, 0, "a", always).present(), "weight 100 always appears");
            if (StockRoll.roll(SEED, period, 0, "h", half).present()) present++;
        }
        double share = present / (double) n;
        assertTrue(share > 0.46 && share < 0.54, "weight 50 should appear about half the time, got " + share);
    }

    @Test
    void theStockCountCoversItsWholeRangeAndNeverLeavesIt() {
        StockEntry sp = book(1, 2, 0.0, 3, 5, 100);
        Set<Integer> seen = new HashSet<>();
        for (long period = 0; period < 600; period++) {
            int c = StockRoll.roll(SEED, period, 0, "sp", sp).count();
            assertTrue(c >= 3 && c <= 5, "" + c);
            seen.add(c);
        }
        assertEquals(Set.of(3, 4, 5), seen);
        assertEquals(1, StockRoll.roll(SEED, 9, 0, "one", book(1, 2, 0.0, 1, 1, 100)).count());
    }

    @Test
    void marketNumbersRoundNearestAndCeilingNeverGoesDown() {
        assertEquals(2_347_000, StockRoll.nice(2_347_312));
        assertEquals(2_348_000, StockRoll.nice(2_347_500));
        assertEquals(135_000, StockRoll.nice(134_600));
        assertEquals(12_300, StockRoll.nice(12_304));
        assertEquals(1_230, StockRoll.nice(1_231));
        assertEquals(999, StockRoll.nice(999));
        assertEquals(0, StockRoll.nice(0));
        assertEquals(0, StockRoll.nice(-5));
        assertEquals(2_348_000, StockRoll.ceilNice(2_347_001));
        assertEquals(2_347_000, StockRoll.ceilNice(2_347_000));
        for (long v = 1; v < 3_000_000; v += 7_919) {
            assertEquals(StockRoll.nice(v), StockRoll.nice(StockRoll.nice(v)), "nice is idempotent at " + v);
            assertTrue(StockRoll.ceilNice(v) >= v, "ceilNice never rounds down at " + v);
        }
    }
}
