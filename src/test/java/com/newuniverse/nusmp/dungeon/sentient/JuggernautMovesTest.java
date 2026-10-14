package com.newuniverse.nusmp.dungeon.sentient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Juggernaut's Antibody Pulse and Immune Surge (1.35.0): fair, bounded, once. */
class JuggernautMovesTest {
    @Test
    void theWarningIsNeverShorterThanAFullSecondAndAHalf() {
        assertTrue(JuggernautMoves.windup(false) >= JuggernautMoves.MIN_WARNING);
        assertTrue(JuggernautMoves.windup(true) > JuggernautMoves.windup(false), "surged, the mark stays longer");
        assertTrue(JuggernautMoves.radius(true) > JuggernautMoves.radius(false), "and is bigger");
    }

    @Test
    void theSurgeComesOnceAtAQuarter() {
        assertFalse(JuggernautMoves.surgeDue(300f, 1000f, false));
        assertTrue(JuggernautMoves.surgeDue(250f, 1000f, false));
        assertTrue(JuggernautMoves.surgeDue(1f, 1000f, false));
        assertFalse(JuggernautMoves.surgeDue(100f, 1000f, true), "never twice");
        assertFalse(JuggernautMoves.surgeDue(0f, 1000f, false), "a dead one does not surge");
        assertFalse(JuggernautMoves.surgeDue(10f, 0f, false));
    }

    @Test
    void aPulseWaitsForItsCooldownAndForSomeoneNear() {
        assertTrue(JuggernautMoves.pulseDue(100, 100, false, 5.0));
        assertFalse(JuggernautMoves.pulseDue(99, 100, false, 5.0));
        assertFalse(JuggernautMoves.pulseDue(100, 100, true, 5.0), "one at a time");
        assertFalse(JuggernautMoves.pulseDue(100, 100, false, JuggernautMoves.TRIGGER_DISTANCE + 1));
        assertTrue(JuggernautMoves.cooldown(true) < JuggernautMoves.cooldown(false));
    }

    @Test
    void theShockwaveOnlyCatchesThoseOnTheGroundInsideTheRing() {
        assertTrue(JuggernautMoves.catches(true, 3.0, 5.0, 0.3, 0.0));
        assertFalse(JuggernautMoves.catches(false, 3.0, 5.0, 0.3, 0.0), "jump it");
        assertFalse(JuggernautMoves.catches(true, 6.0, 5.0, 0.3, 0.0), "outside the ring");
        assertFalse(JuggernautMoves.catches(true, 3.0, 5.0, 0.3, 4.0), "another level");
        assertTrue(JuggernautMoves.share(true) > JuggernautMoves.share(false));
        assertTrue(JuggernautMoves.share(true) < 0.45f, "well under the ceiling every area attack of the Dungeon keeps");
    }
}
