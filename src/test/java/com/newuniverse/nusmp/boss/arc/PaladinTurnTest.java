package com.newuniverse.nusmp.boss.arc;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The bookkeeping of turned companions: cap, 20-second loans, release on every path, restore after a load. */
class PaladinTurnTest {
    private static UUID id(int n) { return new UUID(0, n); }

    @Test
    void aTurnLastsTwentySecondsAndIsNotExtended() {
        PaladinTurn<UUID> t = new PaladinTurn<>();
        assertEquals(PaladinTurn.Result.TURNED, t.turn(id(1), id(100), 1000));
        assertEquals(PaladinTurn.Result.ALREADY, t.turn(id(1), id(100), 1200));
        assertEquals(400, t.remaining(id(1), 1000));
        assertEquals(200, t.remaining(id(1), 1200), "the second application did not restart it");
        assertTrue(t.due(1399).isEmpty());
        assertEquals(1, t.due(1400).size());
    }

    @Test
    void atMostSixAtOnce() {
        PaladinTurn<UUID> t = new PaladinTurn<>();
        for (int i = 0; i < 6; i++) assertEquals(PaladinTurn.Result.TURNED, t.turn(id(i), id(100), 0));
        assertEquals(PaladinTurn.Result.FULL, t.turn(id(6), id(100), 0));
        assertEquals(6, t.size());
        t.release(id(0));
        assertEquals(PaladinTurn.Result.TURNED, t.turn(id(6), id(100), 0), "a freed place can be used");
    }

    @Test
    void releaseGivesTheLoanBackOnce() {
        PaladinTurn<UUID> t = new PaladinTurn<>();
        t.turn(id(1), id(100), 0);
        PaladinTurn.Loan<UUID> l = t.release(id(1));
        assertEquals(id(100), l.owner());
        assertNull(t.release(id(1)));
        assertFalse(t.contains(id(1)));
    }

    @Test
    void releaseAllReturnsEveryOwnerForTheDeathAndUnloadPaths() {
        PaladinTurn<UUID> t = new PaladinTurn<>();
        t.turn(id(1), id(100), 0);
        t.turn(id(2), id(101), 0);
        t.turn(id(3), id(100), 0);
        assertEquals(2, t.ownedBy(id(100)));
        List<PaladinTurn.Loan<UUID>> all = t.releaseAll();
        assertEquals(3, all.size());
        assertTrue(t.isEmpty());
        assertTrue(t.releaseAll().isEmpty());
    }

    @Test
    void restoreAfterALoadKeepsTheSavedEnd() {
        PaladinTurn<UUID> t = new PaladinTurn<>();
        t.turn(id(1), id(100), 50);
        List<PaladinTurn.Loan<UUID>> saved = t.loans();
        PaladinTurn<UUID> back = new PaladinTurn<>();
        for (PaladinTurn.Loan<UUID> l : saved) assertTrue(back.restore(l.id(), l.owner(), l.until()));
        assertEquals(450L, back.loans().get(0).until());
        assertFalse(back.restore(id(1), id(100), 1), "no duplicates");
        for (int i = 10; i < 20; i++) back.restore(id(i), id(100), 9);
        assertEquals(PaladinTurn.MAX, back.size());
    }

    @Test
    void aNullIdIsRefused() {
        assertEquals(PaladinTurn.Result.FULL, new PaladinTurn<UUID>().turn(null, id(1), 0));
    }
}
