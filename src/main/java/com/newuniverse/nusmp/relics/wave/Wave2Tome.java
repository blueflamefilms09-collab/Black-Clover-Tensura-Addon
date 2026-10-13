package com.newuniverse.nusmp.relics.wave;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.IntUnaryOperator;

/**
 * The Tome of "Wait, That's Illegal": the d20 table of the Petition for Retroactive Nullification and the ledger of recent failures it is
 * filed against. Pure, no Minecraft imports. A "failure" is a harmful effect that landed on a player; the tome's wearer may appeal one that
 * hit them or a player near them.
 */
public final class Wave2Tome {
    private Wave2Tome() {}

    public enum Outcome { DENIED, PENDING, REROLL, GRANTED, EXPEDITED }

    /** 1-3 denied, 4-9 pending review, 10-15 processed (a re-roll), 16-19 granted, 20 expedited. */
    public static Outcome outcome(int d20) {
        int r = Math.max(1, Math.min(20, d20));
        if (r <= 3) return Outcome.DENIED;
        if (r <= 9) return Outcome.PENDING;
        if (r <= 15) return Outcome.REROLL;
        if (r <= 19) return Outcome.GRANTED;
        return Outcome.EXPEDITED;
    }

    /** A d20 (1 to 20); with advantage, the better of two. */
    public static int d20(IntUnaryOperator rng, boolean advantage) {
        int a = 1 + Math.floorMod(rng.applyAsInt(20), 20);
        if (!advantage) return a;
        int b = 1 + Math.floorMod(rng.applyAsInt(20), 20);
        return Math.max(a, b);
    }

    /** Clause 66B: on a d100 of 5 or less a Cosmic Auditor takes notice. */
    public static boolean auditorNotices(int d100) { return d100 <= 5; }

    /** The paper imp's fine: 1d6, but it never takes the wearer's last health. */
    public static float fine(int d6, float currentHealth) {
        float fine = Math.max(1, Math.min(6, d6));
        return Math.max(0f, Math.min(fine, currentHealth - 1f));
    }

    /** The "request processed" re-roll: an even chance that the failure is undone. */
    public static boolean rerollUndoes(int coin) { return Math.floorMod(coin, 2) == 0; }

    // ---------------------------------------------------------------- the ledger
    /** One failure on file: who it hit, which effect, how strong and long, when, and who caused it (null if unknown). */
    public record Failure(UUID target, String effectId, int amplifier, int duration, long at, UUID source) {}

    /** The most recent failure per player, at most {@link #CAPACITY} players; older ones fall out first. */
    public static final class Ledger {
        public static final int CAPACITY = 48;

        private final List<Failure> entries = new ArrayList<>();

        public synchronized void record(UUID target, String effectId, int amplifier, int duration, long now, UUID source) {
            if (target == null || effectId == null) return;
            entries.removeIf(f -> f.target().equals(target));
            entries.add(new Failure(target, effectId, amplifier, duration, now, source));
            while (entries.size() > CAPACITY) entries.remove(0);
        }

        /** The newest failure among {@code candidates} that is no older than {@code maxAge} ticks, or null. */
        public synchronized Failure latest(Collection<UUID> candidates, long now, long maxAge) {
            Failure best = null;
            for (Failure f : entries) {
                if (!candidates.contains(f.target()) || now - f.at() > maxAge || f.at() > now) continue;
                if (best == null || f.at() >= best.at()) best = f;
            }
            return best;
        }

        public synchronized void remove(Failure f) { entries.remove(f); }

        public synchronized void prune(long now, long maxAge) { entries.removeIf(x -> now - x.at() > maxAge || x.at() > now); }

        public synchronized int size() { return entries.size(); }

        public synchronized void clear() { entries.clear(); }
    }
}
