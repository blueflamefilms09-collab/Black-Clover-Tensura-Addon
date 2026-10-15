package com.newuniverse.nusmp.boss.arc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the Soul Touch turns the victim's tamed wolves, summons and spirits against them for 20 seconds. That is a loan, not a theft:
 * every turned creature must be given back on schedule, when Lucius dies, and on any unload, and there may never be more than six at
 * once. The bookkeeping (who is turned, whose pet it was, when it ends, the cap, the release on every path) is separate from the
 * entity work (which goals and targets change), so it is a small generic class with no Minecraft types that is tested on its own;
 * the glue stores the same facts in the creature's own data so a reload cannot lose them.
 *
 * @param <K> the creature id type (a UUID in the game)
 */
public final class PaladinTurn<K> {
    /** Most creatures turned at once by one Lucius, and how long a turn lasts (20 s). */
    public static final int MAX = 6, TURN_TICKS = 400;

    public enum Result { TURNED, ALREADY, FULL }

    /** One loan: the creature, whose it was, and the tick it is due back. */
    public record Loan<K>(K id, K owner, long until) {}

    private final Map<K, Loan<K>> loans = new LinkedHashMap<>();

    public int size() { return loans.size(); }

    public boolean contains(K id) { return loans.containsKey(id); }

    public boolean isEmpty() { return loans.isEmpty(); }

    /** Turns 'id' (owned by 'owner') until now + {@link #TURN_TICKS}. A creature already turned keeps its original end (no extension). */
    public Result turn(K id, K owner, long now) {
        if (id == null) return Result.FULL;
        if (loans.containsKey(id)) return Result.ALREADY;
        if (loans.size() >= MAX) return Result.FULL;
        loans.put(id, new Loan<>(id, owner, now + TURN_TICKS));
        return Result.TURNED;
    }

    /** Puts a loan back after a load (same cap, same rules; the end tick is the saved one). */
    public boolean restore(K id, K owner, long until) {
        if (id == null || loans.containsKey(id) || loans.size() >= MAX) return false;
        loans.put(id, new Loan<>(id, owner, until));
        return true;
    }

    /** The loans that are due back at 'now' (not removed). */
    public List<Loan<K>> due(long now) {
        List<Loan<K>> out = new ArrayList<>();
        for (Loan<K> l : loans.values()) if (now >= l.until()) out.add(l);
        return out;
    }

    /** Gives one back. Returns its loan, or null if it was not turned. */
    public Loan<K> release(K id) { return loans.remove(id); }

    /** Gives every one back (Lucius died, the fight ended, the world is closing). Returns what was released. */
    public List<Loan<K>> releaseAll() {
        List<Loan<K>> out = new ArrayList<>(loans.values());
        loans.clear();
        return out;
    }

    /** Ticks left on a loan (0 if not turned). */
    public int remaining(K id, long now) {
        Loan<K> l = loans.get(id);
        return l == null ? 0 : (int) Math.max(0, l.until() - now);
    }

    /** The loans in the order they were made (for saving). */
    public List<Loan<K>> loans() { return new ArrayList<>(loans.values()); }

    /** Everything turned by or on behalf of one owner, for the "your companions are turned" message. */
    public int ownedBy(K owner) {
        int n = 0;
        for (Loan<K> l : loans.values()) if (l.owner() != null && l.owner().equals(owner)) n++;
        return n;
    }
}
