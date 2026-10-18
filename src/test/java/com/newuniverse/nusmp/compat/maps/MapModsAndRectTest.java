package com.newuniverse.nusmp.compat.maps;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Map mod detection by id, and the minimap geometry the client helper uses. */
class MapModsAndRectTest {
    @Test
    void detectionByModId() {
        assertTrue(MapMods.detect(id -> false).isEmpty());
        assertTrue(MapMods.detect(null).isEmpty());
        List<MapMods.Found> f = MapMods.detect(Set.of("xaerominimapfair", "ftbchunks", "neoforge")::contains);
        assertEquals(2, f.size());
        assertEquals("xaero_minimap", f.get(0).mod().key());
        assertEquals("xaerominimapfair", f.get(0).id());
        assertEquals("ftb_chunks", f.get(1).mod().key());
        assertTrue(MapMods.anyMinimap(f));
        assertFalse(MapMods.anyMinimap(MapMods.detect(Set.of("xaeroworldmap")::contains)), "the world map alone draws no minimap");
        assertTrue(MapMods.isLoaded(Set.of("journeymap")::contains, "journeymap"));
        assertFalse(MapMods.isLoaded(Set.of("journeymap")::contains, "voxelmap"));
    }

    @Test
    void aThrowingPredicateCountsAsNotLoaded() {
        assertTrue(MapMods.detect(id -> { throw new IllegalStateException("mod list not ready"); }).isEmpty());
    }

    @Test
    void everyModHasUniqueKeyAndAtLeastOneId() {
        Set<String> keys = new java.util.HashSet<>();
        for (MapMods.Mod m : MapMods.ALL) {
            assertTrue(keys.add(m.key()), m.key());
            assertFalse(m.ids().isEmpty());
            assertFalse(m.does().isBlank());
        }
    }

    @Test
    void theMinimapSitsTopRight() {
        MinimapRect.Rect r = MinimapRect.topRight(480, 270, 140);
        assertEquals(new MinimapRect.Rect(480 - 140 - MinimapRect.MARGIN, MinimapRect.MARGIN, 140, 140), r);
        assertEquals(480 - MinimapRect.MARGIN, r.right());
    }

    @Test
    void theSizeIsClampedToTheScreen() {
        MinimapRect.Rect r = MinimapRect.topRight(100, 60, 500);
        assertEquals(60, r.w());
        assertTrue(r.x() >= 0);
        assertEquals(0, MinimapRect.topRight(100, 60, -5).w());
    }

    @Test
    void aBoxThatDoesNotTouchStaysAndOneThatDoesMovesLeft() {
        MinimapRect.Rect mini = MinimapRect.topRight(480, 270, 140);                  // x 336..476, y 4..144
        assertEquals(10, MinimapRect.avoidX(10, 10, 100, 20, mini), "left side: clear");
        assertEquals(400, MinimapRect.avoidX(400, 200, 60, 20, mini), "below the minimap: clear");
        int moved = MinimapRect.avoidX(400, 20, 60, 20, mini);
        assertEquals(336 - MinimapRect.MARGIN - 60, moved);
        assertFalse(new MinimapRect.Rect(moved, 20, 60, 20).overlaps(mini));
        assertEquals(0, MinimapRect.avoidX(0, 20, 600, 20, mini), "never below 0");
        assertEquals(7, MinimapRect.avoidX(7, 20, 50, 20, null));
        assertEquals(7, MinimapRect.avoidX(7, 20, 50, 20, new MinimapRect.Rect(0, 0, 0, 0)));
    }

    @Test
    void avoidYMovesBelow() {
        MinimapRect.Rect mini = MinimapRect.topRight(480, 270, 140);
        assertEquals(mini.bottom() + MinimapRect.MARGIN, MinimapRect.avoidY(400, 10, 50, 20, mini));
        assertEquals(200, MinimapRect.avoidY(10, 200, 50, 20, mini));
    }
}
