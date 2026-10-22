package com.newuniverse.nusmp.client.ui.move;

import com.newuniverse.nusmp.client.ui.move.GuiMoveLayout.State;
import com.newuniverse.nusmp.client.ui.move.GuiMoveMath.Place;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the layout file holds one short text per element; reading it must never fail, never put an element out of reach, and a saved state must come back unchanged. */
class GuiMoveLayoutTest {
    @Test
    void anEmptyTextIsAnElementThatWasNeverTouched() {
        assertNull(GuiMoveLayout.parse(""));
        assertNull(GuiMoveLayout.parse("   "));
        assertNull(GuiMoveLayout.parse(null));
        assertEquals("", GuiMoveLayout.format(null));
        assertEquals("", GuiMoveLayout.format(new State()));
    }

    @Test
    void aSavedStateComesBackUnchanged() {
        for (int anchor = 0; anchor < 4; anchor++) for (boolean cx : new boolean[]{false, true}) for (boolean cy : new boolean[]{false, true})
            for (int ox : new int[]{-300, 0, 12, 4096}) for (float scale : new float[]{0.5f, 1f, 1.25f, 2.5f}) for (boolean hidden : new boolean[]{false, true}) {
                State s = new State();
                s.placed = true;
                s.place = new Place(anchor, cx, cy, ox, -ox / 2);
                s.scale = scale;
                s.hidden = hidden;
                State back = GuiMoveLayout.parse(GuiMoveLayout.format(s));
                assertNotNull(back);
                assertTrue(back.placed);
                assertEquals(s.place, back.place);
                assertEquals(s.scale, back.scale, 0.006f);
                assertEquals(s.hidden, back.hidden);
            }
    }

    @Test
    void aPlaceAtTheDefaultStillCountsAsPlaced() {
        State s = new State();
        s.placed = true;
        assertEquals("p=1;a=0;cx=0;cy=0;ox=0;oy=0;s=1.00", GuiMoveLayout.format(s));      // the System window's reset must not fall back to its old keys
        assertTrue(GuiMoveLayout.parse(GuiMoveLayout.format(s)).placed);
    }

    @Test
    void aHiddenElementKeepsItsDefaultPlace() {
        State s = new State();
        s.hidden = true;
        assertEquals("h=1", GuiMoveLayout.format(s));
        State back = GuiMoveLayout.parse("h=1");
        assertNotNull(back);
        assertTrue(back.hidden);
        assertFalse(back.placed);
        assertEquals(Place.DEFAULT, back.place);
        assertEquals(1f, back.scale, 0f);
    }

    @Test
    void garbageNeverBreaksAndNeverEscapesTheLimits() {
        assertNull(GuiMoveLayout.parse("hello"));
        assertNull(GuiMoveLayout.parse(";;;==;"));
        assertNull(GuiMoveLayout.parse("p=0"));
        State s = GuiMoveLayout.parse("p=1;a=99;cx=true;cy=nope;ox=99999999;oy=-99999999;s=77;future=1");
        assertNotNull(s);
        assertTrue(s.placed);
        assertEquals(0, s.place.anchor());
        assertTrue(s.place.centerX());
        assertFalse(s.place.centerY());
        assertEquals(GuiMoveMath.MAX_OFFSET, s.place.offX());
        assertEquals(-GuiMoveMath.MAX_OFFSET, s.place.offY());
        assertEquals(GuiMoveMath.MAX_SCALE, s.scale, 0f);
        State t = GuiMoveLayout.parse("p=1;s=NaN;ox=abc");
        assertEquals(1f, t.scale, 0f);
        assertEquals(0, t.place.offX());
        State u = GuiMoveLayout.parse("p=1;s=0,75;ox=3,9");
        assertEquals(0.75f, u.scale, 1e-6f);                                              // a comma reads as a point
    }

    @Test
    void aTextWithoutPlacedDropsStraySizeAndPlace() {
        State s = GuiMoveLayout.parse("a=3;ox=40;s=2;h=1");
        assertNotNull(s);
        assertFalse(s.placed);
        assertEquals(Place.DEFAULT, s.place);
        assertEquals(1f, s.scale, 0f);
        assertTrue(s.hidden);
    }

    @Test
    void theCopyIsIndependent() {
        State s = GuiMoveLayout.parse("p=1;a=1;ox=5;s=1.5");
        State c = s.copy();
        c.hidden = true;
        c.scale = 2f;
        assertFalse(s.hidden);
        assertEquals(1.5f, s.scale, 1e-6f);
    }
}
