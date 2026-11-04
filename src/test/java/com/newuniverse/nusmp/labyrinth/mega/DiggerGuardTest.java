package com.newuniverse.nusmp.labyrinth.mega;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** 3.48.2: the Armorsaurus is kept out of the Mega-Dungeon only, only while the switch is on. */
class DiggerGuardTest {
    private static final ResourceLocation ARMORSAURUS = ResourceLocation.fromNamespaceAndPath("tensura", "armorsaurus");

    @Test
    void refusesTheArmorsaurusInTheMegaDungeonOnly() {
        assertTrue(DiggerGuard.refuses(true, true, ARMORSAURUS));
        assertFalse(DiggerGuard.refuses(true, false, ARMORSAURUS));
        assertFalse(DiggerGuard.refuses(false, true, ARMORSAURUS));
        assertFalse(DiggerGuard.refuses(true, true, ResourceLocation.fromNamespaceAndPath("minecraft", "zombie")));
        assertFalse(DiggerGuard.refuses(true, true, null));
    }
}
