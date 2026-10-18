package com.newuniverse.nusmp.skilltree.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Why: a price is read at a glance in the screen and in chat, and a screenshot in a bug report must read the same on every machine, so the exact strings are pinned (the
 * grouping, the truncation that never makes a price look cheaper than it is, the in-game restock clock).
 */
class GoldFormatTest {
    @Test
    void fullGroupsByThousands() {
        assertEquals("0", GoldFormat.full(0));
        assertEquals("999", GoldFormat.full(999));
        assertEquals("1,000", GoldFormat.full(1_000));
        assertEquals("2,347,000", GoldFormat.full(2_347_000));
        assertEquals("1,234,567,890", GoldFormat.full(1_234_567_890L));
        assertEquals("-1,200", GoldFormat.full(-1_200));
        assertEquals("-9,223,372,036,854,775,808", GoldFormat.full(Long.MIN_VALUE));
    }

    @Test
    void compactKeepsThreeSignificantDigitsAndTruncates() {
        assertEquals("0", GoldFormat.compact(0));
        assertEquals("950", GoldFormat.compact(950));
        assertEquals("1K", GoldFormat.compact(1_000));
        assertEquals("1.5K", GoldFormat.compact(1_500));
        assertEquals("12.3K", GoldFormat.compact(12_399));
        assertEquals("152K", GoldFormat.compact(152_999));
        assertEquals("1.05M", GoldFormat.compact(1_050_000));
        assertEquals("2.34M", GoldFormat.compact(2_349_999), "truncated, not rounded up");
        assertEquals("1.5M", GoldFormat.compact(1_500_000));
        assertEquals("2.5M", GoldFormat.compact(2_500_000));
        assertEquals("120K", GoldFormat.compact(120_000));
        assertEquals("999K", GoldFormat.compact(999_999));
        assertEquals("1.2B", GoldFormat.compact(1_200_000_000L));
        assertEquals("4T", GoldFormat.compact(4_000_000_000_000L));
        assertEquals("-1.5K", GoldFormat.compact(-1_500));
    }

    @Test
    void clockSpeaksInGameTime() {
        assertEquals("<1h", GoldFormat.clock(0));
        assertEquals("<1h", GoldFormat.clock(999));
        assertEquals("1h", GoldFormat.clock(1_000));
        assertEquals("23h", GoldFormat.clock(23_999));
        assertEquals("1d 0h", GoldFormat.clock(24_000));
        assertEquals("2d 4h", GoldFormat.clock(2 * 24_000 + 4_500));
        assertEquals("<1h", GoldFormat.clock(-50), "never negative");
    }
}
