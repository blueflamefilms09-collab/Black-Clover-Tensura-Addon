package com.newuniverse.nusmp.arc.early;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: staggers, wounds and the armour are all one timed flag, so opening, extending (never cutting short) and closing it are proven once. */
class WindowTest {
    @Test
    void aWindowIsOpenUntilItsEnd() {
        Window w = new Window();
        assertFalse(w.active(0));
        w.open(100, 40);
        assertTrue(w.active(100));
        assertTrue(w.active(139));
        assertFalse(w.active(140), "the end tick itself is closed");
        assertEquals(40, w.left(100));
        assertEquals(1, w.left(139));
        assertEquals(0, w.left(200));
        assertEquals(140, w.end());
    }

    @Test
    void aLaterOpeningExtendsButNeverShortens() {
        Window w = new Window();
        w.open(0, 100);
        w.open(10, 20);
        assertEquals(100, w.end(), "a shorter window does not cut the longer one");
        w.open(50, 100);
        assertEquals(150, w.end(), "a longer one extends it");
        w.open(60, -5);
        assertEquals(150, w.end(), "a negative time opens nothing");
        w.close();
        assertFalse(w.active(60));
    }
}
