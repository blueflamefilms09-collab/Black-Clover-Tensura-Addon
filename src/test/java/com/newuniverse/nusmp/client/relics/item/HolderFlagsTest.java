package com.newuniverse.nusmp.client.relics.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The packed render flags the server tells the client (relics/RelicHolderSync). */
class HolderFlagsTest {
    @Test
    void packingRoundTrips() {
        for (boolean g : new boolean[]{false, true}) for (int leaves = 0; leaves <= 6; leaves++) {
            int p = HolderFlags.pack(g, leaves);
            assertEquals(g, HolderFlags.genesis(p));
            assertEquals(leaves, HolderFlags.leaves(p));
        }
        assertEquals(0, HolderFlags.pack(false, 0));
        assertEquals(15, HolderFlags.leaves(HolderFlags.pack(false, 99)), "clamped to four bits");
        assertEquals(0, HolderFlags.leaves(HolderFlags.pack(true, -3)));
    }

    @Test
    void theStoreForgetsZeroAndIsBounded() {
        HolderFlags.clear();
        assertEquals(0, HolderFlags.get(7));
        HolderFlags.put(7, HolderFlags.pack(true, 5));
        assertTrue(HolderFlags.genesis(HolderFlags.get(7)));
        assertEquals(5, HolderFlags.leaves(HolderFlags.get(7)));
        HolderFlags.put(7, 0);
        assertEquals(0, HolderFlags.get(7));
        for (int i = 1; i < 3000; i++) HolderFlags.put(i, 1);
        assertTrue(HolderFlags.get(2999) == 1, "the newest survives a purge");
        HolderFlags.clear();
    }
}
