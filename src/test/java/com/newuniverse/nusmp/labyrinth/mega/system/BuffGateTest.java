package com.newuniverse.nusmp.labyrinth.mega.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: "the buff works only in the dungeons" is one yes/no question asked from many places, and a wrong answer is either a bonus that follows players into the
 * Overworld (the owner said no) or a bonus that vanishes in the dungeon (the player loses what they earned). So every dimension that matters and the switch that
 * restores the old behaviour are pinned here, with the gate's bookkeeping (when the modifiers must be touched) and the party credit rule next to it.
 */
class BuffGateTest {
    @Test void theDungeonDimensionsHaveTheBuff() {
        assertTrue(BuffGate.active("nusmp:mega_dungeon", true));
        assertTrue(BuffGate.active("nusmp:sanctuary", true), "the Hub counts as the dungeon");
        assertTrue(BuffGate.isDungeonDimension(BuffGate.MEGA));
        assertTrue(BuffGate.isDungeonDimension(BuffGate.HUB));
    }

    @Test void everyOtherDimensionIsDormantWhileTheSwitchIsOn() {
        assertFalse(BuffGate.active("minecraft:overworld", true));
        assertFalse(BuffGate.active("minecraft:the_nether", true));
        assertFalse(BuffGate.active("minecraft:the_end", true));
        assertFalse(BuffGate.active("nusmp:labyrinth", true), "the old Spire is not a dungeon dimension of the System");
        assertFalse(BuffGate.active("nusmp:mega_dungeon_2", true), "an id that only starts like the dungeon's is not it");
        assertFalse(BuffGate.active("", true));
        assertFalse(BuffGate.active(null, true), "an unknown dimension is not the dungeon");
    }

    @Test void theSwitchOffRestoresTheOldAlwaysOnBehaviour() {
        assertTrue(BuffGate.active("minecraft:overworld", false));
        assertTrue(BuffGate.active("nusmp:mega_dungeon", false));
        assertTrue(BuffGate.active(null, false));
    }

    @Test void theModifiersAreTouchedOnlyWhenTheirStateHasToChange() {
        assertTrue(BuffGate.needsChange(null, true), "a player nobody has looked at needs a first look");
        assertTrue(BuffGate.needsChange(null, false));
        assertTrue(BuffGate.needsChange(Boolean.FALSE, true), "arrived in the dungeon");
        assertTrue(BuffGate.needsChange(Boolean.TRUE, false), "left the dungeon");
        assertFalse(BuffGate.needsChange(Boolean.TRUE, true));
        assertFalse(BuffGate.needsChange(Boolean.FALSE, false));
    }
}
