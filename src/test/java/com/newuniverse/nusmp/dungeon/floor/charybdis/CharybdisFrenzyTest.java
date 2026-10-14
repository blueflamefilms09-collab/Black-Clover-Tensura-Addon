package com.newuniverse.nusmp.dungeon.floor.charybdis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 1.35.0: the Frenzy of the Gates: wider sweep, whirlpool and crush (all drawn in the wind-up), more megalodons, and the tougher build. */
class CharybdisFrenzyTest {
    @Test
    void theFrenzyWidensTheSweepTheWhirlpoolAndTheCrush() {
        assertEquals(CharybdisMath.SWEEP_REACH, CharybdisMath.sweepReach(false));
        assertEquals(CharybdisMath.WHIRL_RADIUS, CharybdisMath.whirlRadius(false));
        assertEquals(CharybdisMath.CRUSH_RADIUS, CharybdisMath.crushRadius(false));
        assertEquals(CharybdisMath.SWEEP_REACH * 1.2, CharybdisMath.sweepReach(true), 1e-9);
        assertEquals(CharybdisMath.WHIRL_RADIUS * 1.25, CharybdisMath.whirlRadius(true), 1e-9);
        assertEquals(CharybdisMath.CRUSH_RADIUS * 1.4, CharybdisMath.crushRadius(true), 1e-9);
        assertTrue(CharybdisMath.crushRadius(true) < 6.0, "still a circle a swimmer leaves in the wind-up");
        assertTrue(CharybdisMath.whirlRadius(true) < CharybdisMath.MAELSTROM_RADIUS + 1.0, "the whirlpool does not outgrow the Maelstrom");
    }

    @Test
    void aCastBringsThreeMegalodonsInTheFrenzyAndTheCapIsFive() {
        assertEquals(CharybdisMath.minionsToSummon(0), CharybdisMath.minionsToSummon(0, false));
        assertEquals(CharybdisTable.MINIONS_PER_CAST, CharybdisMath.minionsToSummon(0, false));
        assertEquals(3, CharybdisMath.minionsToSummon(0, true));
        for (int alive = 0; alive <= 8; alive++) {
            int plain = CharybdisMath.minionsToSummon(alive, false), frenzy = CharybdisMath.minionsToSummon(alive, true);
            assertTrue(frenzy >= plain);
            assertTrue(Math.max(0, alive) + frenzy <= CharybdisTable.MINION_CAP + 1 || frenzy == 0, "never past the cap of five: " + alive + "+" + frenzy);
            assertTrue(Math.max(0, alive) + plain <= CharybdisTable.MINION_CAP || plain == 0);
        }
        assertEquals(0, CharybdisMath.minionsToSummon(5, true));
        assertEquals(1, CharybdisMath.minionsToSummon(4, true));
        assertEquals(0, CharybdisMath.minionsToSummon(4, false));
    }

    @Test
    void theBuildIsTougherAndTheLastMoveIsStillTheUltimate() {
        assertTrue(CharybdisTable.HEALTH > 1100);
        assertEquals(CharybdisTable.MAELSTROM, CharybdisTable.specs().get(CharybdisTable.specs().size() - 1).id());
    }
}
