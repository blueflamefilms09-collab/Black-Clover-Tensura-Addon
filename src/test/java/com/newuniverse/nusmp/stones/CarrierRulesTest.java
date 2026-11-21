package com.newuniverse.nusmp.stones;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phase B of the Infinity Stones, pure rules only (no Minecraft): the socket mask, the circuit, the surge, the Snap, the caps. */
class CarrierRulesTest {
    @Test
    void theMaskCountsAndHoldsSixStones() {
        int m = 0;
        for (InfinityStone s : InfinityStone.values()) {
            assertFalse(CarrierRules.has(m, s));
            m = CarrierRules.with(m, s);
            assertTrue(CarrierRules.has(m, s));
        }
        assertTrue(CarrierRules.full(m));
        assertEquals(6, CarrierRules.count(m));
        assertEquals(5, CarrierRules.count(CarrierRules.without(m, InfinityStone.SOUL)));
        assertEquals(0, CarrierRules.bit(null));
        assertFalse(CarrierRules.has(m, null));
    }

    @Test
    void theCircuitIsTheComicsFeedbackLoop() {
        assertEquals(InfinityStone.SOUL, CarrierRules.predecessor(InfinityStone.MIND));
        assertEquals(InfinityStone.MIND, CarrierRules.predecessor(InfinityStone.POWER));
        assertEquals(InfinityStone.POWER, CarrierRules.predecessor(InfinityStone.SPACE));
        assertEquals(InfinityStone.SPACE, CarrierRules.predecessor(InfinityStone.TIME));
        assertEquals(InfinityStone.TIME, CarrierRules.predecessor(InfinityStone.REALITY));
        assertEquals(InfinityStone.REALITY, CarrierRules.predecessor(InfinityStone.SOUL));
        int mindPower = CarrierRules.with(CarrierRules.bit(InfinityStone.MIND), InfinityStone.POWER);
        assertTrue(CarrierRules.boosted(mindPower, InfinityStone.POWER));
        assertFalse(CarrierRules.boosted(mindPower, InfinityStone.MIND), "Mind needs Soul");
        assertEquals(1.25, CarrierRules.effectFactor(mindPower, InfinityStone.POWER, true));
        assertEquals(0.85, CarrierRules.cooldownFactor(mindPower, InfinityStone.POWER, true));
        assertEquals(1.0, CarrierRules.effectFactor(mindPower, InfinityStone.POWER, false), "off = no bonus");
        assertEquals(6, CarrierRules.boostedCount(CarrierRules.ALL), "all six: every stone is boosted");
        assertEquals(1, CarrierRules.boostedCount(mindPower));
    }

    @Test
    void theSurgeIsInverseToStrengthAndNeverLethal() {
        double g = 1.0e9;
        assertEquals(0.10, CarrierRules.surgeFraction(g, g, false), 1e-9);
        assertEquals(0.20, CarrierRules.surgeFraction(g, g, true), 1e-9);
        assertTrue(CarrierRules.surgeFraction(g * 5, g, false) < CarrierRules.surgeFraction(g, g, false));
        assertEquals(0.02, CarrierRules.surgeFraction(g * 100, g, false), 1e-9, "a hundred times the gate: a fifth of the base surge");
        assertTrue(CarrierRules.surgeFraction(1, 1e18, true) <= 0.30);
        assertTrue(CarrierRules.surgeFraction(0, g, true) <= 0.30);
        assertTrue(StoneRules.recoil(40f, 3f, CarrierRules.surgeFraction(g, g, true)) <= 2f, "the surge leaves at least one health point");
    }

    @Test
    void theSnapNeedsEverythingAndNeverDustsAPlayer() {
        long now = 5000;
        assertNull(CarrierRules.snapRefusal(CarrierRules.ALL, true, true, false, now, 0));
        assertNotNull(CarrierRules.snapRefusal(63 & ~1, true, true, false, now, 0), "five stones");
        assertNotNull(CarrierRules.snapRefusal(CarrierRules.ALL, false, true, false, now, 0), "setting off");
        assertNotNull(CarrierRules.snapRefusal(CarrierRules.ALL, true, false, false, now, 0), "gate");
        assertNotNull(CarrierRules.snapRefusal(CarrierRules.ALL, true, true, true, now, 0), "held hand");
        assertNotNull(CarrierRules.snapRefusal(CarrierRules.ALL, true, true, false, now, now + 1), "cooldown");
        assertEquals(now + 7 * 24000L, CarrierRules.snapReadyAt(now, 7));
        assertEquals(now + 24000L, CarrierRules.snapReadyAt(now, 0), "never less than a day");
        assertEquals(CarrierRules.Fate.NONE, CarrierRules.fate(false, true, false, 5f));
        assertEquals(CarrierRules.Fate.NONE, CarrierRules.fate(true, true, false, 5f), "not even a boss-sized player");
        assertEquals(CarrierRules.Fate.NONE, CarrierRules.fate(false, false, true, 5f), "pets and named mobs are spared");
        assertEquals(CarrierRules.Fate.TENTH, CarrierRules.fate(true, false, false, 2000f));
        assertEquals(200f, CarrierRules.eraseDamage(CarrierRules.Fate.TENTH, 2000f, 2000f), 1e-4);
        assertEquals(CarrierRules.Fate.DUST, CarrierRules.fate(false, false, false, 20f));
        assertEquals(CarrierRules.Fate.HALF, CarrierRules.fate(false, false, false, 121f));
        assertEquals(10f, CarrierRules.eraseDamage(CarrierRules.Fate.HALF, 200f, 20f), 1e-4);
        assertEquals(20f, CarrierRules.eraseDamage(CarrierRules.Fate.DUST, 20f, 20f), 1e-4);
        assertEquals(0f, CarrierRules.eraseDamage(CarrierRules.Fate.NONE, 20f, 20f));
        assertEquals(40, CarrierRules.SNAP_TARGET_CAP);
    }

    @Test
    void theSnapModesCycleAndEraseCanBeForbidden() {
        CarrierRules.SnapMode m = CarrierRules.SnapMode.RESTORE;
        for (int i = 0; i < 12; i++) {
            m = CarrierRules.nextMode(m, false);
            assertTrue(m != CarrierRules.SnapMode.ERASE, "Erase is skipped while the server forbids it");
        }
        m = CarrierRules.SnapMode.RESTORE;
        assertEquals(CarrierRules.SnapMode.ERASE, CarrierRules.nextMode(m, true));
        assertEquals(CarrierRules.SnapMode.RESTORE, CarrierRules.snapMode(4), "ordinals wrap");
        assertEquals(CarrierRules.SnapMode.GRANT, CarrierRules.snapMode(-1));
    }

    @Test
    void telekinesisAndMindLinkAreCapped() {
        assertFalse(CarrierRules.liftable(true, false, true), "never a player");
        assertFalse(CarrierRules.liftable(false, true, true), "never a boss");
        assertFalse(CarrierRules.liftable(false, false, false), "never a dead thing");
        assertTrue(CarrierRules.liftable(false, false, true));
        assertTrue(CarrierRules.liftRoom(23, 24));
        assertFalse(CarrierRules.liftRoom(24, 24));
        assertEquals(CarrierRules.LINK_MAX_AMP, CarrierRules.linkAmplifier(10));
        assertEquals(0, CarrierRules.linkAmplifier(-3));
        assertEquals(CarrierRules.LINK_EFFECT_TICKS, CarrierRules.linkTicks(1_000_000));
        assertEquals(1, CarrierRules.linkTicks(0));
        assertTrue(CarrierRules.masteryDue(1600, 0));
        assertFalse(CarrierRules.masteryDue(1599, 0));
    }

    @Test
    void carriersHoldTheRightStonesAndKeysAreStable() {
        assertEquals(CarrierRules.ALL, CarrierRules.allowedMask(CarrierRules.INFINITY_ID));
        assertEquals(CarrierRules.ALL, CarrierRules.allowedMask(CarrierRules.NANO_ID));
        assertEquals(CarrierRules.bit(InfinityStone.MIND), CarrierRules.allowedMask(CarrierRules.SCEPTER_ID), "the scepter holds the Mind Stone only");
        assertEquals(0, CarrierRules.allowedMask("the_mind_stone"));
        assertEquals("cs_space_1", CarrierRules.carrierKey(InfinityStone.SPACE, 1));
        assertEquals(InfinityStone.MIND, CarrierRules.carrierStone("cs_mind_4"));
        assertNull(CarrierRules.carrierStone("cs_snap"));
        assertNull(CarrierRules.carrierStone("inf_mind_1"));
        assertTrue(CarrierRules.isCarrierAct("cs_socket"));
        assertFalse(CarrierRules.isCarrierAct("snap"));
        assertEquals(4, CarrierRules.slotActs(InfinityStone.MIND));
        assertEquals(2, CarrierRules.slotActs(InfinityStone.SOUL));
        assertTrue(CarrierRules.mendReady(CarrierRules.bit(InfinityStone.SOUL) | CarrierRules.bit(InfinityStone.TIME) | CarrierRules.bit(InfinityStone.REALITY)));
        assertFalse(CarrierRules.mendReady(CarrierRules.bit(InfinityStone.SOUL)));
        assertTrue(CarrierRules.riftReady(CarrierRules.bit(InfinityStone.SPACE) | CarrierRules.bit(InfinityStone.POWER)));
        assertFalse(CarrierRules.altarOffer("ep"));
        assertTrue(CarrierRules.altarOffer("pet"));
    }
}
