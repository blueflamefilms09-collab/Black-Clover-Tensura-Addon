package com.newuniverse.nusmp.labyrinth.mega.rift;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SpentRiftTest {
    @Test
    void spentRiftStaysShutOnlyWhileTheSwitchIsOn() {
        assertFalse(PocketRealm.mayOpenNew(true, Set.of(7L), 7L));
        assertTrue(PocketRealm.mayOpenNew(true, Set.of(7L), 8L));
        assertTrue(PocketRealm.mayOpenNew(false, Set.of(7L), 7L));
    }
}
