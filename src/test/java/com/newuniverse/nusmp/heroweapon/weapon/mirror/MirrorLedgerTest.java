package com.newuniverse.nusmp.heroweapon.weapon.mirror;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the image and the rebound decide how a blow is answered, so the wielder's working memory is proved exactly: the image (life, one echo), the rebound (time and budget) and what putting the weapon away forgets. */
class MirrorLedgerTest {
    @Test
    void anImageLivesItsLifeAndRepeatsOnce() {
        MirrorLedger l = new MirrorLedger();
        assertFalse(l.imageLive(0));
        l.placeImage(100, 1, 2, 3, 90f, 240);
        assertTrue(l.imageLive(339));
        assertFalse(l.imageLive(340));
        assertEquals(1.0, l.imageX(), 1e-9);
        assertEquals(3.0, l.imageZ(), 1e-9);
        assertEquals(90f, l.imageYaw(), 1e-6f);
        assertTrue(l.imageCanEcho(150));
        l.markEchoed();
        assertFalse(l.imageCanEcho(150), "an image repeats a technique only once");
        assertTrue(l.imageLive(150), "but it still stands");
    }

    @Test
    void aNewImageReplacesTheOldAndCanEchoAgain() {
        MirrorLedger l = new MirrorLedger();
        l.placeImage(0, 0, 0, 0, 0f, 100);
        l.markEchoed();
        l.placeImage(10, 5, 0, 0, 0f, 100);
        assertTrue(l.imageCanEcho(20));
        assertEquals(5.0, l.imageX(), 1e-9);
    }

    @Test
    void clearingTheImageRemovesIt() {
        MirrorLedger l = new MirrorLedger();
        l.placeImage(0, 0, 0, 0, 0f, 100);
        l.clearImage();
        assertFalse(l.imageLive(1));
        assertFalse(l.imageCanEcho(1));
    }

    @Test
    void aReboundIsOpenForItsTimeAndWhileItHasBudget() {
        MirrorLedger l = new MirrorLedger();
        assertFalse(l.reboundOpen(0));
        l.openRebound(100, 80, 20.0);
        assertTrue(l.reboundOpen(179));
        assertFalse(l.reboundOpen(180));
        assertEquals(20.0, l.reboundLeft(150), 1e-9);
        l.spendRebound(12.0);
        assertEquals(8.0, l.reboundLeft(150), 1e-9);
        l.spendRebound(100.0);
        assertFalse(l.reboundOpen(150), "closed once the budget is gone");
        assertEquals(0.0, l.reboundLeft(150), 1e-9);
    }

    @Test
    void spendingANegativeAmountDoesNotRefillTheBudget() {
        MirrorLedger l = new MirrorLedger();
        l.openRebound(0, 80, 10.0);
        l.spendRebound(-5.0);
        assertEquals(10.0, l.reboundLeft(1), 1e-9);
    }

    @Test
    void puttingTheWeaponAwayForgetsTheImageTheGleamAndTheReboundButNotThePanes() {
        MirrorLedger l = new MirrorLedger();
        l.placeImage(0, 0, 0, 0, 0f, 100);
        l.gleam.add(0, 40);
        l.openRebound(0, 80, 20.0);
        l.panes.place(0, 0, 0, 0, 0, 0, 1, 1.7, 120, 3);
        l.putAway();
        assertFalse(l.imageLive(1));
        assertEquals(0, l.gleam.value(1));
        assertFalse(l.reboundOpen(1));
        assertEquals(1, l.panes.size(1), "glass already standing in the air stays until its time is up");
    }

    @Test
    void aWholeFightSequenceKeepsTheNumbersConsistent() {
        MirrorLedger l = new MirrorLedger();
        long t = 1000;
        for (int i = 0; i < 2; i++) {
            for (PaneBook.Pane old : l.panes.makeRoom(t)) assertTrue(old.broken);
            l.panes.place(t, i * 3, 0, 0, 0, 0, 1, MirrorNumbers.PANE_RADIUS, MirrorNumbers.PANE_LIFE, MirrorNumbers.PANE_CHARGES);
            l.gleam.add(t, MirrorNumbers.GLEAM_PER_CATCH);
            t += 20;
        }
        assertEquals(2, l.panes.size(t));
        assertEquals(24, l.gleam.value(t));
        assertEquals(MirrorNumbers.rayDamage(24), 7.0 + 6.0 * 0.24, 1e-9);
        assertEquals(24, l.gleam.spendAll(t));
        assertEquals(7.0, MirrorNumbers.rayDamage(l.gleam.value(t)), 1e-9);
        for (PaneBook.Pane old : l.panes.makeRoom(t)) assertTrue(old.broken, "the third pane pushes the oldest out");
        assertEquals(1, l.panes.size(t));
    }
}
