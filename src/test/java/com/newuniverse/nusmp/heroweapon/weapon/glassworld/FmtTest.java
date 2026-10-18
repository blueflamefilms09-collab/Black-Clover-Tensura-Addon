package com.newuniverse.nusmp.heroweapon.weapon.glassworld;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Why: the descriptions print their numbers through this class, so a wrong rounding would put a wrong number in front of the player; the cases are the ones the
 * descriptions use (whole numbers, halves, quarters, seconds from ticks, percents).
 */
class FmtTest {
    @Test
    void numbersPrintWithTheFewestDecimals() {
        assertEquals("5", Fmt.n(5.0));
        assertEquals("5.5", Fmt.n(5.5));
        assertEquals("0.25", Fmt.n(0.25));
        assertEquals("0", Fmt.n(1.0E-4));
        assertEquals("12", Fmt.n(11.999));
        assertEquals("1.6", Fmt.n(1.6));
        assertEquals("-2", Fmt.n(-2.0));
        assertEquals("0.35", Fmt.n(0.35));
    }

    @Test
    void ticksAndSharesPrintAsSecondsAndPercents() {
        assertEquals("4", Fmt.sec(80));
        assertEquals("4.5", Fmt.sec(90));
        assertEquals("0.7", Fmt.sec(14));
        assertEquals("60", Fmt.pct(0.6));
        assertEquals("2.5", Fmt.pct(0.025));
        assertEquals("25", Fmt.pct(0.25));
    }
}
