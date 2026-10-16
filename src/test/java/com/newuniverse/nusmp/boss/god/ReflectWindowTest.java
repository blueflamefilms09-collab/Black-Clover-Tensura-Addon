package com.newuniverse.nusmp.boss.god;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The two seconds after a spatial swap in which the swapped player's blows come back at them. */
class ReflectWindowTest {
    @Test
    void theWindowIsTwoSecondsFromTheSwap() {
        ReflectWindow w = new ReflectWindow();
        w.open("p", 100);
        assertTrue(w.reflecting("p", 100));
        assertTrue(w.reflecting("p", 139));
        assertFalse(w.reflecting("p", 140));
    }

    @Test
    void itBelongsToOnePlayer() {
        ReflectWindow w = new ReflectWindow();
        w.open("p", 0);
        assertFalse(w.reflecting("q", 5));
    }

    @Test
    void closeAndClear() {
        ReflectWindow w = new ReflectWindow();
        w.open("p", 0);
        w.open("q", 0);
        w.close("p");
        assertFalse(w.reflecting("p", 1));
        assertTrue(w.reflecting("q", 1));
        w.clear();
        assertFalse(w.reflecting("q", 1));
    }
}
