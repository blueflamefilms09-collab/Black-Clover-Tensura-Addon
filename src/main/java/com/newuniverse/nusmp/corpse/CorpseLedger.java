package com.newuniverse.nusmp.corpse;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Reanimation raises "whatever died near the caster a moment ago", so something has to remember the dead. That memory has three
 * rules that are easy to get wrong and must not depend on the game running: it only reaches back {@value #WINDOW_TICKS} ticks (60 s),
 * it never holds more than a cap (config {@code corpse.ledgerMax}, default 24) and when it is full the OLDEST entry goes first. It is a
 * plain class with no Minecraft types, so those rules are unit tested; the glue ({@link Corpses}) turns a dying mob into a
 * {@link Fallen} and a {@link Fallen} back into a spawned body. One ledger belongs to one caster.
 */
public final class CorpseLedger {
    /** How far back the ledger remembers: 60 seconds. */
    public static final int WINDOW_TICKS = 1200;

    /**
     * What is kept of a dead creature: its type id, its name, three numbers to scale the new body by, where it fell and when. {@code pet}
     * is true for a creature that belonged to this ledger's caster (the second window of True Soul Corpse re-raises only those).
     */
    public record Fallen(String type, String name, double maxHealth, double attack, double speed, String dim, double x, double y, double z, long tick,
                         boolean pet) {}

    private final List<Fallen> entries = new ArrayList<>();      // oldest first
    private int cap;

    public CorpseLedger(int cap) { this.cap = Math.max(1, cap); }

    public int cap() { return cap; }

    /** The config may change while the game runs; a smaller cap drops the oldest at once. */
    public void setCap(int cap) {
        this.cap = Math.max(1, cap);
        trim();
    }

    public int size() { return entries.size(); }

    public boolean isEmpty() { return entries.isEmpty(); }

    /** Remembers one dead creature; the oldest entries are evicted when the cap is exceeded. */
    public void record(Fallen f) {
        if (f == null || f.type() == null) return;
        entries.add(f);
        trim();
    }

    private void trim() {
        while (entries.size() > cap) entries.remove(0);
    }

    /** Forgets everything older than the window. */
    public void prune(long now) {
        entries.removeIf(f -> now - f.tick() > WINDOW_TICKS);
    }

    /** The entries still inside the window, newest first (not removed). */
    public List<Fallen> fresh(long now) {
        prune(now);
        List<Fallen> out = new ArrayList<>(entries);
        java.util.Collections.reverse(out);
        return out;
    }

    /** Removes and returns up to 'n' of the newest entries inside the window. */
    public List<Fallen> take(long now, int n) {
        List<Fallen> out = new ArrayList<>();
        for (Fallen f : fresh(now)) {
            if (out.size() >= n) break;
            out.add(f);
        }
        entries.removeAll(out);
        return out;
    }

    /** Removes and returns the pets (creatures of this caster) that fell within 'windowTicks' of 'now', newest first. */
    public List<Fallen> takePets(long now, int windowTicks) {
        List<Fallen> out = new ArrayList<>();
        for (Fallen f : fresh(now)) if (f.pet() && now - f.tick() <= windowTicks) out.add(f);
        entries.removeAll(out);
        return out;
    }

    /** A copy of the entries, oldest first, for saving. */
    public List<Fallen> entries() { return new ArrayList<>(entries); }

    /** Puts saved entries back (oldest first); the cap applies. */
    public void restore(List<Fallen> saved) {
        entries.clear();
        if (saved != null) entries.addAll(saved);
        trim();
    }
}
