package com.newuniverse.nusmp.elite.nations.econ;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * Why: the Treasury Ledger of a nation (owner's inventory: "ledger systems (Treasury Ledger)"). A balance in copper coins plus a CAPPED history of every movement: who, when, why, how much and the balance
 * afterwards. Pure (no Minecraft types, time is a plain long supplied by the caller) so the accounting can be tested exhaustively. Every mutator returns the amount that really moved (0 = refused) so the
 * caller can refund or tell the player; none throws on bad input. The balance never goes negative and never above the cap the caller passes (the nation's treasury cap from its level).
 */
public final class Ledger {
    public enum Kind { DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT, INTEREST, TAX, REWARD, SPOILS, FEE, ADMIN }

    /** One line of the history. {@code actor} is null for the system (interest, spoils). */
    public record Entry(long time, Kind kind, UUID actor, String note, long amount, long balanceAfter) {}

    public static final int DEFAULT_MAX_ENTRIES = 200;
    public static final long NO_CAP = Long.MAX_VALUE;

    private final int maxEntries;
    private final Deque<Entry> history = new ArrayDeque<>();
    private long balance;

    public Ledger() { this(DEFAULT_MAX_ENTRIES); }

    public Ledger(int maxEntries) { this.maxEntries = Math.max(1, maxEntries); }

    public long balance() { return balance; }

    public int size() { return history.size(); }

    /** Room left under the cap (never negative). A balance already above a lowered cap has no room. */
    public long room(long cap) { return Math.max(0L, cap - balance); }

    /** Pays in. The amount is clamped to the room under the cap; returns what was accepted (0 if the treasury is full or the amount is not positive). */
    public long deposit(long time, UUID actor, long amount, String note, long cap) {
        return credit(time, Kind.DEPOSIT, actor, amount, note, cap);
    }

    /** Pays in with an explicit kind (reward, spoils, interest, tax, transfer in). */
    public long credit(long time, Kind kind, UUID actor, long amount, String note, long cap) {
        if (amount <= 0) return 0L;
        long accepted = Math.min(amount, room(cap));
        if (accepted <= 0) return 0L;
        balance += accepted;
        record(time, kind, actor, note, accepted);
        return accepted;
    }

    /** Takes out exactly {@code amount}; refused (0) when the balance is too small or the amount is not positive. */
    public long withdraw(long time, UUID actor, long amount, String note) {
        return debit(time, Kind.WITHDRAW, actor, amount, note);
    }

    public long debit(long time, Kind kind, UUID actor, long amount, String note) {
        if (amount <= 0 || amount > balance) return 0L;
        balance -= amount;
        record(time, kind, actor, note, -amount);
        return amount;
    }

    /** Moves coins between two ledgers: the sender pays {@code amount}, the receiver gets what fits under its cap and the sender is refunded the rest. Returns what arrived. */
    public static long transfer(Ledger from, Ledger to, long time, UUID actor, long amount, String noteOut, String noteIn, long toCap) {
        if (from == null || to == null || from == to || amount <= 0 || amount > from.balance) return 0L;
        long fit = Math.min(amount, to.room(toCap));
        if (fit <= 0) return 0L;
        from.debit(time, Kind.TRANSFER_OUT, actor, fit, noteOut);
        to.credit(time, Kind.TRANSFER_IN, actor, fit, noteIn, toCap);
        return fit;
    }

    /** The newest {@code n} entries, newest first. */
    public List<Entry> recent(int n) {
        List<Entry> out = new ArrayList<>();
        var it = history.descendingIterator();
        while (it.hasNext() && out.size() < Math.max(0, n)) out.add(it.next());
        return out;
    }

    /** All entries, oldest first (for saving). */
    public List<Entry> all() { return Collections.unmodifiableList(new ArrayList<>(history)); }

    /** Restores a saved state (load). The history is trimmed to the cap; the balance is trusted. */
    public void restore(long savedBalance, List<Entry> savedHistory) {
        balance = Math.max(0L, savedBalance);
        history.clear();
        if (savedHistory != null) for (Entry e : savedHistory) record(e);
    }

    /** Sets the balance without a history line (admin repair). Prefer {@link #credit}/{@link #debit}. */
    public void forceBalance(long value) { balance = Math.max(0L, value); }

    private void record(long time, Kind kind, UUID actor, String note, long signed) {
        record(new Entry(time, kind, actor, note == null ? "" : note, signed, balance));
    }

    private void record(Entry e) {
        history.addLast(e);
        while (history.size() > maxEntries) history.removeFirst();
    }

    /** Sum of the signed amounts of the kept history (a consistency check for tests). */
    public long historySum() {
        long s = 0;
        for (Entry e : history) s += e.amount();
        return s;
    }
}
