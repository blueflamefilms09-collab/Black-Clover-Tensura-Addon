package com.newuniverse.nusmp.labyrinth.gate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The gate cooldown, the void check and the rescue's "back to the pad or out" decision, without the game. */
class GateRulesTest {
    @Test
    void aGateRefusesThePlayerItJustSetDownUntilTheStampPasses() {
        long used = GateRules.cooldownUntil(1000);
        assertEquals(1080, used);
        assertTrue(GateRules.cooldownHolds(1000, used), "the tick of the trip");
        assertTrue(GateRules.cooldownHolds(1079, used), "the last tick of the cooldown");
        assertFalse(GateRules.cooldownHolds(1080, used), "then the gate works again");
        assertFalse(GateRules.cooldownHolds(1000, 0), "a player who never travelled");
        assertFalse(GateRules.cooldownHolds(560, 560), "the stamp itself is already over");
    }

    @Test
    void theVoidCheckRunsEveryTenTicksAndOnlyForFallingSurvivors() {
        assertTrue(GateRules.checkDue(0));
        assertTrue(GateRules.checkDue(30));
        assertFalse(GateRules.checkDue(31));
        assertTrue(GateRules.falling(-3.0, false, true));
        assertTrue(GateRules.falling(0.99, false, true), "just under the lowest block");
        assertFalse(GateRules.falling(1.0, false, true), "on it");
        assertFalse(GateRules.falling(45.0, false, true), "standing on the pad");
        assertFalse(GateRules.falling(-3.0, true, true), "a spectator flies where they like");
        assertFalse(GateRules.falling(-3.0, false, false), "the rescue switched off: the void kills as usual");
    }

    @Test
    void aRescueGoesBackToThePadOnceAndOutWhenThePadLetsThemFallAgain() {
        assertFalse(GateRules.rescueTakesOut(true, false, 1000, 0), "first fall: back to the pad");
        assertFalse(GateRules.rescueTakesOut(true, true, 1000, 700), "a fall long after the last one: the pad again");
        assertTrue(GateRules.rescueTakesOut(true, true, 1000, 900), "twice within the window: the pad is unsafe, out");
        assertTrue(GateRules.rescueTakesOut(true, true, 1000, 801), "the window is exclusive");
        assertFalse(GateRules.rescueTakesOut(true, true, 1000, 800));
        assertTrue(GateRules.rescueTakesOut(false, false, 1000, 0), "no pad known: out");
    }

    @Test
    void theWayBackGoesHomeOnlyWhenTheOriginIsGoneOrIsTheLabyrinthItself() {
        assertFalse(GateRules.goesHome(true, false), "the Overworld spot they stepped in from");
        assertTrue(GateRules.goesHome(false, false), "the origin's dimension is gone");
        assertTrue(GateRules.goesHome(true, true), "a gate placed inside the Labyrinth: bed or spawn, never a loop");
    }

    @Test
    void theNumbersMatchTheDesign() {
        assertEquals(80, GateRules.GATE_COOLDOWN);
        assertEquals(4f, GateRules.RESCUE_DAMAGE);
        assertEquals(200, GateRules.RESCUE_RETRY_TICKS);
        assertEquals(1.0, GateRules.VOID_Y, 1e-9);
        assertEquals(10, GateRules.RESCUE_EVERY);
    }
}
