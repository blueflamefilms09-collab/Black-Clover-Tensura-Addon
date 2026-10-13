package com.newuniverse.nusmp.dungeon.sentient;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** A room is a chunk column over a band of 16 blocks of height: it packs into one long and comes back out. */
class RoomKeyTest {
    @Test
    void packingRoundTripsEverywhere() {
        int[] chunks = {0, 1, -1, 7, -7, 1234, -1234, 1_875_000, -1_875_000, 33_554_431, -33_554_432};
        int[] bands = {0, 1, -1, -4, 19, 100, -100, 2047, -2048};
        for (int cx : chunks) for (int cz : chunks) for (int b : bands) {
            long k = RoomKey.of(cx, cz, b);
            assertEquals(cx, RoomKey.chunkX(k), "chunk x");
            assertEquals(cz, RoomKey.chunkZ(k), "chunk z");
            assertEquals(b, RoomKey.band(k), "band");
        }
    }

    @Test
    void aBlockBelongsToTheRoomThatHoldsIt() {
        for (int x = -70; x <= 70; x += 7) for (int z = -70; z <= 70; z += 5) for (int y = -64; y <= 320; y += 9) {
            long k = RoomKey.at(x, y, z);
            assertTrue(RoomKey.contains(k, x, y, z), x + "," + y + "," + z);
            assertEquals(Math.floorDiv(x, 16), RoomKey.chunkX(k));
            assertEquals(Math.floorDiv(z, 16), RoomKey.chunkZ(k));
            assertEquals(Math.floorDiv(y, 16), RoomKey.band(k));
        }
    }

    @Test
    void theBoxIsOneChunkColumnOverOneBand() {
        long k = RoomKey.at(-1, -1, 16);
        assertEquals(-16, RoomKey.minX(k));
        assertEquals(-1, RoomKey.maxX(k));
        assertEquals(16, RoomKey.minZ(k));
        assertEquals(31, RoomKey.maxZ(k));
        assertEquals(-16, RoomKey.minY(k));
        assertEquals(-1, RoomKey.maxY(k));
        assertFalse(RoomKey.contains(k, 0, -1, 16), "x just outside");
        assertFalse(RoomKey.contains(k, -1, 0, 16), "y just outside");
        assertFalse(RoomKey.contains(k, -1, -1, 15), "z just outside");
    }

    @Test
    void differentRoomsGetDifferentKeys() {
        Set<Long> seen = new HashSet<>();
        for (int cx = -60; cx <= 60; cx++) for (int cz = -60; cz <= 60; cz++) for (int b = -4; b <= 19; b++) assertTrue(seen.add(RoomKey.of(cx, cz, b)));
        assertEquals(121 * 121 * 24, seen.size());
    }
}
