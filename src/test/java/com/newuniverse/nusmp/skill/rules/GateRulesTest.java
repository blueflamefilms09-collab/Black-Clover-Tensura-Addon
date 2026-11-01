package com.newuniverse.nusmp.skill.rules;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Gate of Babylon (3.47.0) is an Ultimate with ten modes; every number and decision of it is pure, so the bounds and the trade-offs are pinned without a world. */
class GateRulesTest {
    @Test
    void tenModesWithUniqueIdsAndKeys() {
        assertEquals(10, GateRules.MODES);
        assertEquals(GateRules.MODES, GateRules.IDS.length);
        Set<String> ids = new HashSet<>(), keys = new HashSet<>();
        for (int m = 0; m < GateRules.MODES; m++) {
            assertTrue(GateRules.valid(m));
            assertTrue(ids.add(GateRules.IDS[m]), "duplicate id " + GateRules.IDS[m]);
            assertTrue(keys.add(GateRules.untilKey(m)), "duplicate tag key for mode " + m);
        }
        assertFalse(GateRules.valid(-1));
        assertFalse(GateRules.valid(GateRules.MODES));
        assertEquals("gate", GateRules.IDS[GateRules.GATE]);
        assertEquals("ea", GateRules.IDS[GateRules.EA]);
        assertEquals("elixir", GateRules.IDS[GateRules.ELIXIR]);
    }

    @Test
    void everyModeCostsAndWaitsInsideItsBand() {
        for (int m = 0; m < GateRules.MODES; m++) {
            assertTrue(GateRules.cooldown(m) > 0 && GateRules.cooldown(m) <= 6000, "cooldown of " + GateRules.IDS[m]);
            assertTrue(GateRules.cost(m) > 0 && GateRules.cost(m) <= 10000, "cost of " + GateRules.IDS[m]);
        }
        for (int m = 0; m < GateRules.MODES; m++) {
            if (m != GateRules.EA) assertTrue(GateRules.cooldown(m) < GateRules.cooldown(GateRules.EA), "Ea is the slowest to come back, not " + GateRules.IDS[m]);
            if (m != GateRules.EA) assertTrue(GateRules.cost(m) < GateRules.cost(GateRules.EA), "Ea is the dearest, not " + GateRules.IDS[m]);
        }
    }

    @Test
    void windowsAreShortAndOnlyTheArmourAndTheVimanaCancel() {
        for (int m = 0; m < GateRules.MODES; m++) {
            assertEquals(GateRules.duration(m) > 0, GateRules.timed(m));
            assertTrue(GateRules.duration(m) <= 1200, "window of " + GateRules.IDS[m] + " must stay short");
            if (GateRules.cancellable(m)) assertTrue(GateRules.timed(m), "only a window can be cancelled: " + GateRules.IDS[m]);
        }
        assertTrue(GateRules.cancellable(GateRules.ARMOR) && GateRules.cancellable(GateRules.VIMANA));
        assertFalse(GateRules.timed(GateRules.GATE) || GateRules.timed(GateRules.EA) || GateRules.timed(GateRules.KING) || GateRules.timed(GateRules.ELIXIR), "the instant modes have no window");
    }

    @Test
    void aWindowIsOpenUntilItsEndTime() {
        long end = GateRules.endsAt(1000, GateRules.ARMOR);
        assertEquals(1000 + GateRules.duration(GateRules.ARMOR), end);
        assertTrue(GateRules.active(end, 1000));
        assertTrue(GateRules.active(end, end - 1));
        assertFalse(GateRules.active(end, end));
        assertFalse(GateRules.active(0, 5), "an unset window is closed");
    }

    @Test
    void theArmourTakesTenthsOffButNeverMakesYouImmune() {
        assertEquals(1.0, GateRules.takenMultiplier(false));
        assertEquals(0.7, GateRules.takenMultiplier(true), 1e-9);
        assertTrue(GateRules.takenMultiplier(true) >= 0.5);
    }

    @Test
    void damageAndCapsStayBounded() {
        assertTrue(GateRules.gateDamage() > 0 && GateRules.gateDamage() <= 40);
        assertTrue(GateRules.volleyDamage() * GateRules.volleyMax() > GateRules.gateDamage(), "a volley is worth more than one weapon in total");
        assertTrue(GateRules.volleyDamage() < GateRules.gateDamage(), "but each weapon of the volley is lighter");
        assertTrue(GateRules.volleyMax() <= 12 && GateRules.rainMax() <= 12 && GateRules.gaePierce() <= 6);
        assertTrue(GateRules.gaeDamage() > GateRules.gateDamage(), "Gae Bolg hits harder than a falling weapon");
        assertTrue(GateRules.eaDamage() > GateRules.gaeDamage() && GateRules.eaDamage() <= 80, "Ea is the great blow, bounded");
        assertTrue(GateRules.eaRange() <= 48 && GateRules.eaWidth() <= 4);
        assertTrue(GateRules.rainRadius() <= 8 && GateRules.rainReach() <= 48 && GateRules.kingRadius() <= 16);
        assertTrue(GateRules.volleyCone() > 0 && GateRules.volleyCone() < 1);
        assertTrue(GateRules.kingBuffTicks() <= 1200 && GateRules.kingWeakTicks() <= 600 && GateRules.chainsWeakTicks() <= 600);
        assertTrue(GateRules.elixirHeal() > 0 && GateRules.elixirHeal() <= 0.5 && GateRules.elixirRegenTicks() <= 400);
        assertTrue(GateRules.vimanaSpeed() <= 3.0 && GateRules.vimanaLift() <= 1.0, "the leap cannot fling a player off the map");
    }

    @Test
    void chainsNeverHoldABossOrAPlayerForever() {
        int free = ControlRules.controlTicks(GateRules.chainsTicks(), false, false);
        int player = ControlRules.controlTicks(GateRules.chainsTicks(), false, true);
        int boss = ControlRules.controlTicks(GateRules.chainsTicks(), true, false);
        assertTrue(boss < player && player < free, "boss < player < ordinary creature");
        assertTrue(free <= ControlRules.MAX_HOLD);
        assertEquals(6, GateRules.chainsSlowAmp(), "Slowness VII holds a creature still");
    }
}
