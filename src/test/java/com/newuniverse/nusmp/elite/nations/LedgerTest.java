package com.newuniverse.nusmp.elite.nations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.elite.nations.econ.InterestPolicy;
import com.newuniverse.nusmp.elite.nations.econ.Ledger;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Why: the treasury ledger is money; every path (deposit, withdraw, cap, transfer, interest, history cap, restore) is checked. */
class LedgerTest {
    private static final UUID A = UUID.nameUUIDFromBytes("a".getBytes()), B = UUID.nameUUIDFromBytes("b".getBytes());

    @Test
    void depositAndWithdrawKeepBalanceAndHistory() {
        Ledger l = new Ledger(50);
        assertEquals(100L, l.deposit(1, A, 100, "gift", Ledger.NO_CAP));
        assertEquals(100L, l.balance());
        assertEquals(30L, l.withdraw(2, B, 30, "rent"));
        assertEquals(70L, l.balance());
        assertEquals(2, l.size());
        Ledger.Entry newest = l.recent(1).get(0);
        assertEquals(Ledger.Kind.WITHDRAW, newest.kind());
        assertEquals(-30L, newest.amount());
        assertEquals(70L, newest.balanceAfter());
        assertEquals(B, newest.actor());
        assertEquals("rent", newest.note());
        assertEquals(2L, newest.time());
    }

    @Test
    void refusesBadAmountsAndOverdraft() {
        Ledger l = new Ledger();
        assertEquals(0L, l.deposit(1, A, 0, "x", Ledger.NO_CAP));
        assertEquals(0L, l.deposit(1, A, -5, "x", Ledger.NO_CAP));
        l.deposit(1, A, 10, "x", Ledger.NO_CAP);
        assertEquals(0L, l.withdraw(2, A, 11, "x"));
        assertEquals(0L, l.withdraw(2, A, 0, "x"));
        assertEquals(0L, l.withdraw(2, A, -1, "x"));
        assertEquals(10L, l.balance());
        assertEquals(1, l.size());
    }

    @Test
    void capClampsDeposits() {
        Ledger l = new Ledger();
        assertEquals(60L, l.deposit(1, A, 100, "x", 60));
        assertEquals(60L, l.balance());
        assertEquals(0L, l.deposit(2, A, 5, "x", 60));
        assertEquals(0L, l.room(60));
        assertEquals(0L, l.room(10), "a balance above a lowered cap has no room");
        l.withdraw(3, A, 20, "x");
        assertEquals(20L, l.room(60));
    }

    @Test
    void transferMovesWhatFitsAndRefundsTheRest() {
        Ledger from = new Ledger(), to = new Ledger();
        from.deposit(1, A, 100, "x", Ledger.NO_CAP);
        to.deposit(1, A, 40, "x", Ledger.NO_CAP);
        assertEquals(10L, Ledger.transfer(from, to, 2, A, 50, "out", "in", 50));
        assertEquals(90L, from.balance());
        assertEquals(50L, to.balance());
        assertEquals(Ledger.Kind.TRANSFER_OUT, from.recent(1).get(0).kind());
        assertEquals(Ledger.Kind.TRANSFER_IN, to.recent(1).get(0).kind());
        assertEquals(0L, Ledger.transfer(from, to, 3, A, 5, "out", "in", 50), "full receiver");
        assertEquals(0L, Ledger.transfer(from, from, 3, A, 5, "out", "in", 1000), "self transfer");
        assertEquals(0L, Ledger.transfer(from, to, 3, A, 1000, "out", "in", 5000), "more than the sender has");
    }

    @Test
    void historyIsCappedOldestDropped() {
        Ledger l = new Ledger(3);
        for (int i = 1; i <= 5; i++) l.deposit(i, A, 1, "n" + i, Ledger.NO_CAP);
        assertEquals(3, l.size());
        assertEquals(5L, l.balance());
        assertEquals("n5", l.recent(5).get(0).note());
        assertEquals("n3", l.all().get(0).note());
    }

    @Test
    void interestIsCreditedAsInterestEntry() {
        Ledger l = new Ledger();
        l.deposit(1, A, 10_000, "x", Ledger.NO_CAP);
        InterestPolicy p = new InterestPolicy.Simple(5, 1000, 30);
        long i = p.interest("n", l.balance());
        assertEquals(30L, i, "5 per mille of 10000 is 50, capped at 30");
        assertEquals(30L, l.credit(2, Ledger.Kind.INTEREST, null, i, "interest", Ledger.NO_CAP));
        assertEquals(10_030L, l.balance());
        assertEquals(Ledger.Kind.INTEREST, l.recent(1).get(0).kind());
        assertEquals(null, l.recent(1).get(0).actor());
    }

    @Test
    void simpleInterestEdgeCases() {
        InterestPolicy p = new InterestPolicy.Simple(10, 500, 0);
        assertEquals(0L, p.interest("n", 499), "below the minimum");
        assertEquals(5L, p.interest("n", 500));
        assertEquals(1000L, p.interest("n", 100_000), "no max: 10 per mille");
        assertEquals(0L, new InterestPolicy.Simple(0, 0, 0).interest("n", 1_000_000));
        assertEquals(0L, InterestPolicy.NONE.interest("n", 1_000_000));
        assertEquals(0L, p.interest("n", -5));
    }

    @Test
    void restoreAndHistorySum() {
        Ledger l = new Ledger(2);
        l.restore(500, java.util.List.of(
                new Ledger.Entry(1, Ledger.Kind.DEPOSIT, A, "a", 100, 100),
                new Ledger.Entry(2, Ledger.Kind.DEPOSIT, A, "b", 200, 300),
                new Ledger.Entry(3, Ledger.Kind.DEPOSIT, A, "c", 200, 500)));
        assertEquals(500L, l.balance());
        assertEquals(2, l.size());
        assertEquals(400L, l.historySum());
        l.restore(-9, null);
        assertEquals(0L, l.balance());
        assertTrue(l.recent(3).isEmpty());
        assertFalse(l.recent(0).iterator().hasNext());
    }
}
