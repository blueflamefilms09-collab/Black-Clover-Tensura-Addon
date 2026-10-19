package com.newuniverse.nusmp.celestial;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The per-player record: a key is collected once, the twelfth gold key is noticed once, a contract binds, rewards are claimed once, the record round-trips and old saves load. */
class CelestialDataTest {
    @Test
    void aKeyIsCollectedOnce() {
        CelestialData d = new CelestialData();
        assertTrue(d.collect(CelestialKey.LEO));
        assertFalse(d.collect(CelestialKey.LEO));
        assertTrue(d.has(CelestialKey.LEO));
        assertFalse(d.has(CelestialKey.VIRGO));
    }

    @Test
    void theGateKeyIsDueExactlyOnceWhenTheTwelfthGoldKeyIsFound() {
        CelestialData d = new CelestialData();
        for (CelestialKey k : CelestialKey.gold()) {
            assertFalse(d.gateKeyDue(), "before " + k.id);
            d.collect(k);
        }
        assertTrue(d.complete());
        assertTrue(d.gateKeyDue());
        d.gateKeyHandedOver();
        assertFalse(d.gateKeyDue(), "given once");
        assertTrue(d.gateKeyGiven());
    }

    @Test
    void aContractBindsAndCountsAsCollectedAndCanBeBroken() {
        CelestialData d = new CelestialData();
        d.bind(CelestialKey.TAURUS);
        assertTrue(d.isBound(CelestialKey.TAURUS));
        assertTrue(d.has(CelestialKey.TAURUS));
        d.unbind(CelestialKey.TAURUS);
        assertFalse(d.isBound(CelestialKey.TAURUS));
        assertTrue(d.has(CelestialKey.TAURUS), "the key was held, the contract is gone");
    }

    @Test
    void bondRisesAndSaysWhenALevelIsGained() {
        CelestialData d = new CelestialData();
        assertFalse(d.addBond(CelestialKey.LYRA, 1));
        assertFalse(d.addBond(CelestialKey.LYRA, 1));
        assertTrue(d.addBond(CelestialKey.LYRA, 1), "three points: level 1");
        assertEquals(1, d.bondLevel(CelestialKey.LYRA));
        assertEquals(0, d.bondLevel(CelestialKey.CRUX));
        assertFalse(d.addBond(CelestialKey.LYRA, 0));
        assertFalse(d.addBond(CelestialKey.LYRA, -5));
        for (int i = 0; i < 200; i++) d.addBond(CelestialKey.LYRA, 10);
        assertEquals(5, d.bondLevel(CelestialKey.LYRA));
    }

    @Test
    void rewardsAreClaimedOncePerOutcome() {
        CelestialData d = new CelestialData();
        assertFalse(d.claimWin(), "nothing to claim before the king fell");
        assertTrue(d.markWon());
        assertFalse(d.markWon());
        assertTrue(d.claimWin());
        assertFalse(d.claimWin());
        assertTrue(d.markRecognised());
        assertTrue(d.claimRecognition());
        assertFalse(d.claimRecognition());
        assertTrue(d.blessingMode() && d.summonMode());
        CelestialData r = new CelestialData();
        r.markRecognised();
        r.claimRecognition();
        assertTrue(r.blessingMode());
        assertFalse(r.summonMode(), "recognition opens the Blessing only");
    }

    @Test
    void theRecordRoundTripsThroughItsListForm() {
        CelestialData d = new CelestialData();
        for (CelestialKey k : CelestialKey.gold()) { d.collect(k); d.bind(k); d.addBond(k, k.ordinal() + 1); }
        d.gateKeyHandedOver();
        d.setKingCooldown(123_456_789_012L);
        d.setGateOpenUntil(999L);
        d.markWon(); d.claimWin(); d.markRecognised(); d.sceptreHandedOver();
        d.setDress(CelestialKey.LIBRA.ordinal(), 5000L);
        d.fought(); d.fought();
        CelestialData c = CelestialData.fromList(d.toList());
        assertEquals(d.owned(), c.owned());
        assertEquals(d.bound(), c.bound());
        assertEquals(123_456_789_012L, c.kingCooldownUntil());
        assertEquals(999L, c.gateOpenUntil());
        assertTrue(c.gateKeyGiven() && c.won() && c.recognised() && c.winClaimed() && c.sceptreGiven() && !c.recognitionClaimed());
        assertEquals(CelestialKey.LIBRA.ordinal(), c.dressKey());
        assertEquals(5000L, c.dressUntil());
        assertEquals(2, c.fights());
        for (CelestialKey k : CelestialKey.values()) assertEquals(d.bondXp(k), c.bondXp(k), k.id);
        assertTrue(c.dressActive(4999) && !c.dressActive(5000));
    }

    @Test
    void anOlderShorterRecordStillLoadsAndNewKeysStartEmpty() {
        CelestialData old = CelestialData.fromList(List.of(3L, 1L));                       // a record from before any later field existed
        assertEquals(3, old.owned());
        assertEquals(1, old.bound());
        assertEquals(0, old.bondXp(CelestialKey.SPIRIT_KING_GATE));
        assertEquals(-1, old.dressKey());
        assertNotNull(CelestialData.fromList(null));
        assertNotNull(CelestialData.fromList(List.of()));
    }

    @Test
    void aDeathKeepsEverythingAndAnAdminResetForgetsIt() {
        CelestialData a = new CelestialData();
        a.bind(CelestialKey.SCORPIO); a.addBond(CelestialKey.SCORPIO, 9); a.markWon(); a.claimWin();
        CelestialData b = new CelestialData();
        b.copyFrom(a);
        assertTrue(b.isBound(CelestialKey.SCORPIO) && b.winClaimed());
        assertEquals(a.bondXp(CelestialKey.SCORPIO), b.bondXp(CelestialKey.SCORPIO));
        b.reset();
        assertEquals(0, b.owned());
        assertFalse(b.won() || b.winClaimed());
        assertEquals(0, b.bondXp(CelestialKey.SCORPIO));
        assertTrue(a.isBound(CelestialKey.SCORPIO), "the copy was independent");
    }
}
