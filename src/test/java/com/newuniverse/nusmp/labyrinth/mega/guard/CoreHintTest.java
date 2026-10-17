package com.newuniverse.nusmp.labyrinth.mega.guard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Mana Core tells its holder which way the sector's door lies in compass words, and a swapped axis would send a party the wrong way for good;
 * this test pins the eight directions to Minecraft's axes (north is minus z), the 45-degree width of each and the rounded horizontal distance, with no
 * game running.
 */
class CoreHintTest {
    @Test
    void theEightDirectionsFollowMinecraftsAxes() {
        assertEquals("north", CoreHint.direction(0, -10));
        assertEquals("north_east", CoreHint.direction(10, -10));
        assertEquals("east", CoreHint.direction(10, 0));
        assertEquals("south_east", CoreHint.direction(10, 10));
        assertEquals("south", CoreHint.direction(0, 10));
        assertEquals("south_west", CoreHint.direction(-10, 10));
        assertEquals("west", CoreHint.direction(-10, 0));
        assertEquals("north_west", CoreHint.direction(-10, -10));
    }

    @Test
    void theSectorsAreFortyFiveDegreesWide() {
        assertEquals("north", CoreHint.direction(3, -10), "just east of north is still north");
        assertEquals("north_east", CoreHint.direction(5, -10), "past 22.5 degrees it is north east");
        assertEquals("east", CoreHint.direction(10, -3));
        assertEquals("north", CoreHint.direction(-3, -10));
        assertEquals("south", CoreHint.direction(-3, 10));
    }

    @Test
    void distanceIsRoundedAndHorizontal() {
        assertEquals(5, CoreHint.distance(3, 4));
        assertEquals(0, CoreHint.distance(0, 0));
        assertEquals(14, CoreHint.distance(10, 10));
        assertEquals("north", CoreHint.direction(0, 0), "a zero offset never throws");
    }
}
