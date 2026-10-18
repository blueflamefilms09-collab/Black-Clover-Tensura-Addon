package com.newuniverse.nusmp.elite.hub;

import com.newuniverse.nusmp.labyrinth.mega.HubLayout;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Dungeon Map shows the right rooms (this floor only), the right doors (both ends kept, no duplicates), never more than the cap, marks what the player has seen, and the memory behind
 * "seen" is bounded and survives a save. The two Hub NPC spots are proven to be on the plaza, away from the gate and from each other.
 */
class MapViewTest {
    private static MapView.Source room(int id, int floor, int x, int z) { return new MapView.Source(id, 3, floor, x, z, x + 10, z + 8); }

    @Test
    void onlyTheCurrentFloorsRoomsAndTheirDoorsAreKept() {
        List<MapView.Source> src = List.of(room(0, 0, 0, 0), room(1, 5, 20, 0), room(2, 5, 40, 0), room(3, 6, 60, 0), room(4, 0, 80, 0));
        List<int[]> edges = List.of(new int[] {0, 1}, new int[] {1, 2}, new int[] {2, 3}, new int[] {2, 1}, new int[] {4, 4}, new int[] {9, 1});
        MapView v = MapView.build(src, edges, 5, 5, "holy", 100, id -> id == 1, 5, 4, true, 45, 4);
        assertEquals(4, v.rooms().size(), "rooms 0 and 4 have no floor of their own (first floor = 5), 1 and 2 are floor 5; 3 is floor 6");
        assertEquals(List.of(0, 1, 2, 4), v.rooms().stream().map(MapView.Room::id).toList());
        assertEquals(2, v.edges().size(), "0-1 and 1-2; the duplicate 2-1, the loop 4-4, the edge to a missing room and the edge to another floor are gone");
        assertEquals(1, v.visitedCount());
        assertTrue(v.rooms().get(1).visited() && !v.rooms().get(0).visited());
        assertEquals(5, v.floor());
        assertEquals("holy", v.road());
        assertTrue(v.hasExit());
    }

    @Test
    void theCapKeepsTheLowestIdsAndTheBoundsAreRight() {
        List<MapView.Source> src = new ArrayList<>();
        for (int i = 50; i >= 0; i--) src.add(room(i, 1, i * 12, 0));
        MapView v = MapView.build(src, List.of(new int[] {0, 1}, new int[] {49, 50}), 1, 1, "holy", 10, id -> false, 0, 0, false, 0, 0);
        assertEquals(10, v.rooms().size());
        assertEquals(0, v.rooms().get(0).id(), "the Landing (lowest id) is never the one dropped");
        assertEquals(1, v.edges().size(), "an edge to a dropped room goes with it");
        assertEquals(List.of(0, 0, 9 * 12 + 10, 8), java.util.Arrays.stream(v.bounds()).boxed().toList());
        List<MapView.Source> many = new ArrayList<>();
        for (int i = 0; i < 3000; i++) many.add(room(i, 1, i, 0));
        assertEquals(MapView.HARD_CAP, MapView.build(many, List.of(), 1, 1, "", 99999, id -> false, 0, 0, false, 0, 0).rooms().size(), "the wire cap holds whatever the config says");
        assertEquals(0, MapView.build(List.of(), List.of(), 1, 1, "", 10, id -> false, 0, 0, false, 0, 0).bounds()[2]);
    }

    @Test
    void roomAtFindsTheRoomAPositionIsIn() {
        MapView v = MapView.build(List.of(room(0, 1, 0, 0), room(1, 1, 20, 0)), List.of(), 1, 1, "holy", 10, id -> false, 0, 0, false, 0, 0);
        assertNotNull(v.roomAt(5, 5));
        assertEquals(1, v.roomAt(25, 3).id());
        assertNull(v.roomAt(15, 5));
    }

    @Test
    void theMemoryIsBoundedKeepsTheNewestAndRoundTrips() {
        MapMemory m = new MapMemory();
        assertTrue(m.visit(7, 3));
        assertFalse(m.visit(7, 3), "a repeat is not news");
        assertTrue(m.visited(7, 3));
        assertFalse(m.visited(8, 3), "another sector's room 3 is another room");
        for (int i = 0; i < MapMemory.MAX_VISITED + 100; i++) m.visit(1, i);
        assertEquals(MapMemory.MAX_VISITED, m.visitedCount());
        assertFalse(m.visited(7, 3), "the oldest is forgotten first");
        assertTrue(m.visited(1, MapMemory.MAX_VISITED + 99));
        assertTrue(m.markMapped(4));
        assertFalse(m.markMapped(4));
        assertTrue(m.isMapped(4));
        MapMemory back = MapMemory.of(m.toData());
        assertEquals(m.toData(), back.toData());
        assertTrue(back.isMapped(4));
        assertFalse(MapMemory.of(null).hasContent());
        assertTrue(MapMemory.key(1, -1) != MapMemory.key(1, 0));
    }

    @Test
    void theNpcSpotsAreOnThePlazaAwayFromTheGateAndEachOther() {
        HubNpcLayout.Spot q = HubNpcLayout.questGiver(), m = HubNpcLayout.merchant();
        assertTrue(HubNpcLayout.onPlaza(q), "the Quest Giver stands on the plaza");
        assertTrue(HubNpcLayout.onPlaza(m), "the Merchant stands on the plaza");
        double gateDx = HubLayout.HOME_GATE_X - q.x(), gateDz = HubLayout.HOME_GATE_Z - q.z();
        assertTrue(Math.sqrt(gateDx * gateDx + gateDz * gateDz) > 15, "not in the way of the home gate");
        gateDx = HubLayout.HOME_GATE_X - m.x();
        gateDz = HubLayout.HOME_GATE_Z - m.z();
        assertTrue(Math.sqrt(gateDx * gateDx + gateDz * gateDz) > 5, "the merchant is not in the gate");
        assertTrue(Math.hypot(q.x() - m.x(), q.z() - m.z()) > 10, "far enough apart to be two places");
        assertEquals(180.0f, q.yaw(), "the Quest Giver looks north, back at the arrival dais");
        assertTrue(q.z() > HubLayout.SPAWN_Z, "south of the arrival cell: straight ahead of a newcomer");
        assertEquals(6, HubNpcLayout.searchBox().length);
    }
}
