package com.newuniverse.nusmp.skill.modes;

import com.newuniverse.nusmp.skill.modes.ModeSpec.CostClass;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The reusable mode table of a multi-mode skill (docs/saiyan_prodigy.md): validity, the wrapped lookup and the wheel walk that skips locked modes. Pure. */
class ModeBundleTest {
    private static ModeBundle five() {
        return new ModeBundle("t", List.of(ModeSpec.of("a", "A", CostClass.FREE, 1), ModeSpec.of("b", "B", CostClass.LIGHT, 2).sneak(7), ModeSpec.of("c", "C", CostClass.HEAVY, 3).hold(4),
                ModeSpec.of("d", "D", CostClass.MEDIUM, 5).needs(2), ModeSpec.of("e", "E", CostClass.ULTIMATE, 6)));
    }

    @Test
    void aSpecKnowsItsShape() {
        ModeBundle b = five();
        assertFalse(b.at(0).isHold());
        assertTrue(b.at(2).isHold());
        assertEquals(4, b.at(2).releaseAction());
        assertEquals(7, b.at(1).sneakAction());
        assertEquals(2, b.at(3).need());
    }

    @Test
    void lookupWrapsAndFindsById() {
        ModeBundle b = five();
        assertEquals("a", b.at(5).id());
        assertEquals("e", b.at(-1).id());
        assertEquals(3, b.index("d"));
        assertEquals(-1, b.index("nope"));
        assertEquals(5, b.size());
    }

    @Test
    void theWheelSkipsClosedModesAndStopsWhenNothingElseIsOpen() {
        ModeBundle b = five();
        assertEquals(1, b.next(0, false, i -> true));
        assertEquals(4, b.next(0, true, i -> true));
        assertEquals(2, b.next(0, false, i -> i == 2 || i == 0));                 // 1 is closed
        assertEquals(0, b.next(0, false, i -> i == 0));                            // nothing else open: stays put, no endless loop
        assertEquals(4, b.next(2, false, i -> i != 3));                            // 3 is skipped
        assertEquals(2, b.next(4, true, i -> i != 3));
        assertEquals(2, b.next(-4, false, i -> true));                             // a stale saved number wraps
    }

    @Test
    void badTablesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new ModeBundle("t", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ModeBundle("t", List.of(ModeSpec.of("a", "A", CostClass.FREE, 1), ModeSpec.of("a", "A2", CostClass.FREE, 2))));
        assertThrows(IllegalArgumentException.class, () -> ModeSpec.of("Bad Id", "x", CostClass.FREE, 1));
        assertThrows(IllegalArgumentException.class, () -> ModeSpec.of("ok", " ", CostClass.FREE, 1));
        assertThrows(IllegalArgumentException.class, () -> new ModeBundle("", List.of(ModeSpec.of("a", "A", CostClass.FREE, 1))));
    }
}
