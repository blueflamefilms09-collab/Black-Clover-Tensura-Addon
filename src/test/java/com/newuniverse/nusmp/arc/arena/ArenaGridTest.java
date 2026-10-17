package com.newuniverse.nusmp.arc.arena;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: every arena has a site that never moves and never touches another; these tests pin the arithmetic (slot to centre and back, the next free slot, the edge
 * of the grid) and that the widest arena fits its site with room for the neighbour's.
 */
class ArenaGridTest {
    @Test
    void aSlotAndItsCentreRoundTripForTheWholeGrid() {
        for (int slot = 0; slot < ArenaGrid.MAX_SLOTS; slot++) {
            int x = ArenaGrid.centreX(slot), z = ArenaGrid.centreZ(slot);
            assertEquals(slot, ArenaGrid.slotAt(x, z), "slot " + slot);
            assertEquals(slot, ArenaGrid.slotAt(x + 100.0, z - 100.0), "inside the site");
        }
        assertEquals(0, ArenaGrid.centreX(0));
        assertEquals(ArenaGrid.SPACING, ArenaGrid.centreX(1));
        assertEquals(ArenaGrid.SPACING, ArenaGrid.centreZ(ArenaGrid.PER_ROW));
    }

    @Test
    void thePlacesBetweenAndBeyondTheSitesBelongToNobody() {
        assertEquals(-1, ArenaGrid.slotAt(-200, 0));
        assertEquals(-1, ArenaGrid.slotAt(0, -200));
        assertEquals(-1, ArenaGrid.slotAt(ArenaGrid.SPACING * ArenaGrid.PER_ROW + 10, 0));
        assertEquals(-1, ArenaGrid.slotAt(0, ArenaGrid.SPACING * (ArenaGrid.MAX_SLOTS / ArenaGrid.PER_ROW) + 10));
        assertEquals(1, ArenaGrid.slotAt(129, 0), "x = 129 is inside the site of slot 1");
        assertEquals(0, ArenaGrid.slotAt(127, 0), "x = 127 is inside the site of slot 0");
    }

    @Test
    void theNextFreeSlotIsTheLowestOneAndAFullGridAnswersMinusOne() {
        Map<String, Integer> taken = new HashMap<>();
        assertEquals(0, ArenaGrid.nextFree(taken));
        taken.put("a", 0);
        taken.put("b", 2);
        assertEquals(1, ArenaGrid.nextFree(taken));
        taken.put("c", 1);
        assertEquals(3, ArenaGrid.nextFree(taken));
        Map<String, Integer> full = new HashMap<>();
        for (int s = 0; s < ArenaGrid.MAX_SLOTS; s++) full.put("c" + s, s);
        assertEquals(-1, ArenaGrid.nextFree(full));
        taken.put("junk", -5);
        taken.put("more", 99999);
        assertEquals(3, ArenaGrid.nextFree(taken), "a corrupt slot in the save is ignored");
    }

    @Test
    void theWidestArenaFitsItsSiteAndTwoNeighboursNeverShareChunks() {
        int widest = 0;
        for (ArenaShape s : ArenaShapes.all()) widest = Math.max(widest, s.radius());
        assertTrue(2 * widest + 1 < ArenaGrid.SPACING / 2 + 10, "the widest arena is " + (2 * widest + 1) + " blocks across");
        assertTrue(ArenaGrid.SPACING - 2 * widest > 160, "the gap between two arenas is more than ten chunks");
    }
}
