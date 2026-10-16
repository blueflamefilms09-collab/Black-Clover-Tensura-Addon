package com.newuniverse.nusmp.thread;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Rouge on the head: the body and the head cat are never both drawn and never both missing; names stay clean. */
class RougePerchTest {
    @Test
    void bodyAndHeadCatAreAlwaysExclusive() {
        for (boolean resting : new boolean[] {false, true})
            for (int age = 0; age < 40; age++)
                assertTrue(RougePerch.headCatDrawn(resting, age) != RougePerch.bodyDrawn(resting, age), resting + " " + age);
    }

    @Test
    void theHeadCatTakesOverOnlyWhenSettled() {
        assertFalse(RougePerch.headCatDrawn(true, RougePerch.SETTLE_TICKS - 1));
        assertTrue(RougePerch.headCatDrawn(true, RougePerch.SETTLE_TICKS));
        assertFalse(RougePerch.headCatDrawn(false, 100));                  // scurrying or tapping: always the body
    }

    @Test
    void headIsLowerWhenSneaking() {
        assertEquals(1.875, RougePerch.headY(false), 1e-9);
        assertTrue(RougePerch.headY(true) < RougePerch.headY(false));
    }

    @Test
    void namesAreTrimmedShortenedAndNeverBlank() {
        assertEquals("Rouge Jr", RougePerch.cleanName("  Rouge Jr \n"));
        assertNull(RougePerch.cleanName("   "));
        assertNull(RougePerch.cleanName(null));
        assertEquals(RougePerch.MAX_NAME, RougePerch.cleanName("x".repeat(100)).length());
        assertEquals("a b", RougePerch.cleanName("a\nb"));
    }
}
