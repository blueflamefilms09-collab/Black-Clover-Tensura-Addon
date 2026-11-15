package com.newuniverse.nusmp.quests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Why: the data generator and the game must build the placeholder advancement id the same way, or a boss would never unlock its quest. */
class FutureBossIdsTest {
    @Test
    void idFormat() {
        assertEquals("anime_multiverse:future/spade_raid_dante", FutureBossIds.id("spade_raid", "dante"));
        assertEquals("future/devil_arc_lucifero", FutureBossIds.path("devil_arc", "lucifero"));
    }

    @Test
    void partsAreCleaned() {
        assertEquals("anime_multiverse:future/witches_forest_vanica_z", FutureBossIds.id("Witches Forest", "Vanica-Z!"));
        assertEquals("anime_multiverse:future/_", FutureBossIds.id(null, null));
    }
}
