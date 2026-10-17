package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the Shadow Core raises a creature the player has JUST slain on the Holy road, so the server has to remember, for a few seconds, what died where and who
 * killed it. That memory must be small (the dungeon is full of monsters dying all the time), short-lived (the brief says 100 ticks) and impossible to grow
 * without bound, so it is a bounded list with an expiry: recording a death beyond the cap pushes out the oldest, and every lookup ignores what is older than the
 * window. It stores plain values (the entity type id, the name, the health and attack it had, where it fell, in which dimension, who killed it, when), not
 * entities, so nothing keeps a dead creature alive and the whole class is testable without a game. A lookup is by KILLER: only the player who slew a creature
 * can extract it. Pure; the game side cleans it on server stop.
 */
public final class ExtractLedger {
    /** What is remembered of one slain creature. */
    public record Fallen(String type, String name, double maxHealth, double attack, double x, double y, double z, String dim, UUID killer, long tick) {}

    private static final class Entry {
        final Fallen fallen;
        boolean notified;

        Entry(Fallen f) { this.fallen = f; }
    }

    private final int cap;
    private final List<Entry> entries = new ArrayList<>();

    public ExtractLedger(int cap) { this.cap = Math.max(1, cap); }

    /** Remembers a death; the oldest memory goes when the ledger is full. */
    public void record(Fallen f) {
        entries.add(new Entry(f));
        while (entries.size() > cap) entries.remove(0);
    }

    /** Forgets what is older than {@code window} ticks. */
    public void expire(long now, int window) {
        entries.removeIf(e -> now - e.fallen.tick() > window);
    }

    /** The memory of a creature this player slew, within {@code radius} blocks of the given point in this dimension and not older than the window; the nearest one, or null. */
    public Fallen nearest(UUID killer, String dim, double x, double y, double z, long now, int window, double radius) {
        Fallen best = null;
        double bestD = radius * radius;
        for (Entry e : entries) {
            Fallen f = e.fallen;
            if (!f.killer().equals(killer) || !f.dim().equals(dim) || now - f.tick() > window) continue;
            double dx = f.x() - x, dy = f.y() - y, dz = f.z() - z;
            double d = dx * dx + dy * dy + dz * dz;
            if (d <= bestD) {
                bestD = d;
                best = f;
            }
        }
        return best;
    }

    /** Forgets this memory (it was used). Returns whether it was there. */
    public boolean take(Fallen f) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).fallen == f) {
                entries.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Marks this memory as announced ("extraction ready" was shown for it) and returns true the FIRST time only, so a toast is not repeated every few ticks
     * while the player stands next to the same corpse.
     */
    public boolean markNotified(Fallen f) {
        for (Entry e : entries) {
            if (e.fallen == f) {
                boolean first = !e.notified;
                e.notified = true;
                return first;
            }
        }
        return false;
    }

    public int size() { return entries.size(); }

    public boolean isEmpty() { return entries.isEmpty(); }

    public void clear() { entries.clear(); }
}
