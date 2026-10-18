package com.newuniverse.nusmp.client.skilltree.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the checkbox rule is the one thing the owner described word for word (ticked shows ON, empty shows OFF, a click flips it) and the texture rule is the one guard against drawing a wrong part of an image
 * nobody can inspect, so both are pinned here.
 */
class MenuCheckboxTest {
    @Test
    void tickedIsOnAndEmptyIsOff() {
        assertEquals("gui.nusmp.tmenu.on", MenuCheckbox.tooltipKey(true));
        assertEquals("gui.nusmp.tmenu.off", MenuCheckbox.tooltipKey(false));
    }

    @Test
    void aClickFlipsTheState() {
        assertFalse(MenuCheckbox.next(true));
        assertTrue(MenuCheckbox.next(false));
        assertTrue(MenuCheckbox.next(MenuCheckbox.next(true)));
    }

    @Test
    void aTextureIsUsedOnlyIfItIsAStripOfTwoOrMoreSquareFrames() {
        assertEquals(0, MenuCheckbox.stripV(16, 32, false));
        assertEquals(16, MenuCheckbox.stripV(16, 32, true));
        assertEquals(16, MenuCheckbox.stripV(16, 64, true), "more frames than two still use the second");
        assertEquals(-1, MenuCheckbox.stripV(16, 16, true), "one square frame: unknown layout, use the vanilla box");
        assertEquals(-1, MenuCheckbox.stripV(32, 16, false), "wide: unknown layout");
        assertEquals(-1, MenuCheckbox.stripV(0, 0, false));
        assertEquals(-1, MenuCheckbox.stripV(-4, 10, false));
    }
}
