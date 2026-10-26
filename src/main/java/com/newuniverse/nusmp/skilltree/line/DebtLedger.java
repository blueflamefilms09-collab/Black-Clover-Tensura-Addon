package com.newuniverse.nusmp.skilltree.line;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why (3.26.0, the Evolution skills): "Debt" is the shared mechanic of the ten Evolution skills: when the player hits a foe a share of the damage is not lost but STORED on that foe, it may tick down by itself
 * while it waits, and a Debt art later consumes all of it for a burst. The ledger is where it is stored, and the rules of a public server are written into it: it is keyed by the owner player's UUID and then the
 * foe's UUID (never a static "current player"), at most {@link #MAX_FOES} foes are tracked for one owner (a new foe beyond that pushes out the oldest), the Debt on one foe is capped at a share of that foe's
 * maximum health (so a boss cannot be made to owe more than the catalogue allows), every entry expires after its hold time, and every way out is a method (the foe died, the owner left, died or changed dimension,
 * the server stopped). The ticking rule is a hard bound too: one tick takes at most a quarter of what is owed and at most 5 % of the foe's maximum health, and never more often than once a second, so the
 * Debt cannot kill by itself faster than 20 seconds. Pure (UUIDs, numbers and game ticks as longs, no Minecraft type), memory only, never saved, so it is unit tested without a game; {@link Debts} is the
 * game-facing class that owns the one instance and feeds it from the damage events. Server thread only.
 */
public final class DebtLedger {
    /** At most this many foes carry Debt for one owner at a time. */
    public static final int MAX_FOES = 20;
    /** One tick takes at most this share of the Debt that is owed. */
    public static final double TICK_FRACTION = 0.25;
    /** ... and at most this share of the foe's maximum health. */
    public static final double TICK_MAX_HEALTH_PCT = 0.05;
    /** The fastest the Debt of one foe ticks (ticks): once a second, whatever a data file says. */
    public static final int MIN_TICK_TICKS = 20;
    /** Debt below this is not worth keeping (the entry is dropped). */
    public static final double MIN_DEBT = 0.01;

    /** The Debt on one foe. */
    public static final class Entry {
        private final UUID foe;
        private double debt;
        private double foeMaxHealth;
        private long expiresAt;
        private long nextTickAt;
        private int tickTicks;

        private Entry(UUID foe) { this.foe = foe; }

        public UUID foe() { return foe; }

        public double debt() { return debt; }

        public long expiresAt() { return expiresAt; }
    }

    /** One tick of Debt that is due: deal {@code amount} to the foe. */
    public record Tick(UUID foe, double amount) {}

    private final Map<UUID, LinkedHashMap<UUID, Entry>> owners = new HashMap<>();

    // ---------------------------------------------------------------- storing
    /**
     * Stores {@code storePct} of {@code damage} as Debt on {@code foe} for {@code owner}, but never more than {@code maxPct} of the foe's maximum health in all. The entry lives until {@code now + holdTicks}
     * (every new blow renews it). {@code tickTicks} 0 means the Debt does not tick, otherwise it ticks every that many ticks (never faster than {@link #MIN_TICK_TICKS}). Returns the Debt that was added (0
     * when the foe is already at its cap, or the inputs make no sense).
     */
    public double store(UUID owner, UUID foe, double damage, double storePct, double maxPct, double foeMaxHealth, long now, long holdTicks, int tickTicks) {
        if (owner == null || foe == null || !(damage > 0) || !(storePct > 0) || !(foeMaxHealth > 0) || !Double.isFinite(damage) || !Double.isFinite(storePct) || !Double.isFinite(foeMaxHealth)) return 0.0;
        double cap = Math.max(0.0, Math.min(1.0, maxPct)) * foeMaxHealth;
        if (cap < MIN_DEBT) return 0.0;
        LinkedHashMap<UUID, Entry> mine = owners.get(owner);
        if (mine != null) pruneMap(mine, now);
        Entry e = mine == null ? null : mine.get(foe);
        if (e == null) {
            if (mine == null) {
                mine = new LinkedHashMap<>();
                owners.put(owner, mine);
            }
            if (mine.size() >= MAX_FOES) evictOldest(mine);
            e = new Entry(foe);
            e.tickTicks = tickTicks <= 0 ? 0 : Math.max(MIN_TICK_TICKS, tickTicks);
            e.nextTickAt = e.tickTicks == 0 ? Long.MAX_VALUE : now + e.tickTicks;
            mine.put(foe, e);
        }
        double room = Math.max(0.0, cap - e.debt);
        double add = Math.min(damage * storePct, room);
        e.foeMaxHealth = foeMaxHealth;
        e.expiresAt = now + Math.max(1L, holdTicks);
        if (add > 0) e.debt += add;
        if (e.debt < MIN_DEBT) {
            mine.remove(foe);
            if (mine.isEmpty()) owners.remove(owner);
            return 0.0;
        }
        return add;
    }

    private static void evictOldest(LinkedHashMap<UUID, Entry> mine) {
        UUID oldest = null;
        long at = Long.MAX_VALUE;
        for (Entry e : mine.values()) {
            if (e.expiresAt < at) {
                at = e.expiresAt;
                oldest = e.foe;
            }
        }
        if (oldest != null) mine.remove(oldest);
    }

    // ---------------------------------------------------------------- the burst's own bound
    /** A boss never takes more than this share of its maximum health from the Debt part of one burst. */
    public static final double BOSS_BURST_PCT = 0.25;

    /**
     * The extra damage a Debt burst adds on top of its base blow: {@code mult} times the Debt it consumed, but never more than the foe's maximum health (a quarter of it for a boss), so a high
     * multiplier on a full Debt cannot one-shot anything. Not finite or negative inputs give 0.
     */
    public static double burstExtra(double consumed, double mult, double foeMaxHealth, boolean boss) {
        if (!(consumed > 0) || !(mult > 0) || !(foeMaxHealth > 0) || !Double.isFinite(consumed) || !Double.isFinite(mult) || !Double.isFinite(foeMaxHealth)) return 0.0;
        return Math.min(consumed * mult, foeMaxHealth * (boss ? BOSS_BURST_PCT : 1.0));
    }

    /**
     * Which foes a burst reaches when it may hit {@code n}: the ones that carry Debt first (nearest first, as the list is given), then the rest nearest first, in all at most {@code n}. {@code sorted}
     * is the foes nearest first; the list is not changed.
     */
    public static <T> List<T> prefer(List<T> sorted, java.util.function.Predicate<T> hasDebt, int n) {
        if (n <= 0 || sorted.isEmpty()) return List.of();
        List<T> out = new ArrayList<>(Math.min(n, sorted.size()));
        for (T t : sorted) {
            if (out.size() >= n) return out;
            if (hasDebt.test(t)) out.add(t);
        }
        for (T t : sorted) {
            if (out.size() >= n) break;
            if (!hasDebt.test(t)) out.add(t);
        }
        return out;
    }

    // ---------------------------------------------------------------- reading and consuming
    /** The Debt on this foe now (0 when there is none or it expired). */
    public double debtOf(UUID owner, UUID foe, long now) {
        Entry e = entry(owner, foe);
        return e == null || e.expiresAt <= now ? 0.0 : e.debt;
    }

    /** True when this foe carries live Debt of this owner. */
    public boolean has(UUID owner, UUID foe, long now) { return debtOf(owner, foe, now) > 0; }

    /** Takes ALL the Debt on this foe (0 when there is none or it expired) and forgets the entry. */
    public double consume(UUID owner, UUID foe, long now) {
        LinkedHashMap<UUID, Entry> mine = owners.get(owner);
        if (mine == null) return 0.0;
        Entry e = mine.remove(foe);
        if (mine.isEmpty()) owners.remove(owner);
        return e == null || e.expiresAt <= now ? 0.0 : e.debt;
    }

    private Entry entry(UUID owner, UUID foe) {
        LinkedHashMap<UUID, Entry> mine = owners.get(owner);
        return mine == null ? null : mine.get(foe);
    }

    // ---------------------------------------------------------------- ticking
    /**
     * The Debt ticks of this owner that are due at {@code now}, at most {@code max} of them (the caller's budget). Each takes at most {@link #TICK_FRACTION} of what is owed and at most
     * {@link #TICK_MAX_HEALTH_PCT} of the foe's maximum health out of the ledger and schedules the next tick; the caller deals the amount. Expired entries are dropped first.
     */
    public List<Tick> due(UUID owner, long now, int max) {
        LinkedHashMap<UUID, Entry> mine = owners.get(owner);
        if (mine == null || max <= 0) return List.of();
        pruneMap(mine, now);
        List<Tick> out = null;
        List<UUID> spent = null;
        for (Entry e : mine.values()) {
            if (out != null && out.size() >= max) break;
            if (e.tickTicks <= 0 || now < e.nextTickAt) continue;
            double amount = Math.min(e.debt * TICK_FRACTION, e.foeMaxHealth * TICK_MAX_HEALTH_PCT);
            e.nextTickAt = now + e.tickTicks;
            if (amount <= 0) continue;
            e.debt -= amount;
            if (out == null) out = new ArrayList<>(4);
            out.add(new Tick(e.foe, amount));
            if (e.debt < MIN_DEBT) {
                if (spent == null) spent = new ArrayList<>(2);
                spent.add(e.foe);
            }
        }
        if (spent != null) for (UUID f : spent) mine.remove(f);
        if (mine.isEmpty()) owners.remove(owner);
        return out == null ? List.of() : out;
    }

    // ---------------------------------------------------------------- pruning and forgetting
    private static void pruneMap(LinkedHashMap<UUID, Entry> mine, long now) { mine.values().removeIf(e -> e.expiresAt <= now); }

    /** Drops this owner's expired entries (and the owner when nothing is left). Returns how many entries remain. */
    public int prune(UUID owner, long now) {
        LinkedHashMap<UUID, Entry> mine = owners.get(owner);
        if (mine == null) return 0;
        pruneMap(mine, now);
        if (mine.isEmpty()) {
            owners.remove(owner);
            return 0;
        }
        return mine.size();
    }

    /** One foe is gone (died, was removed): every owner forgets it. */
    public void forgetFoe(UUID foe) {
        if (owners.isEmpty() || foe == null) return;
        owners.values().removeIf(mine -> {
            mine.remove(foe);
            return mine.isEmpty();
        });
    }

    /** One entry only (the foe is no longer in the world the owner is in). */
    public void forget(UUID owner, UUID foe) {
        LinkedHashMap<UUID, Entry> mine = owners.get(owner);
        if (mine == null) return;
        mine.remove(foe);
        if (mine.isEmpty()) owners.remove(owner);
    }

    /** The owner left, died or changed dimension: everything they stored is forgotten. */
    public void forgetOwner(UUID owner) { owners.remove(owner); }

    /** The server stopped. */
    public void clear() { owners.clear(); }

    // ---------------------------------------------------------------- numbers for tests and the report
    public boolean isEmpty() { return owners.isEmpty(); }

    public boolean hasOwner(UUID owner) { return owners.containsKey(owner); }

    public int ownerCount() { return owners.size(); }

    /** How many foes carry Debt of this owner (expired ones included until pruned). */
    public int size(UUID owner) {
        LinkedHashMap<UUID, Entry> mine = owners.get(owner);
        return mine == null ? 0 : mine.size();
    }

    /** All entries held, for the bound check of the self-test. */
    public int totalEntries() {
        int n = 0;
        for (LinkedHashMap<UUID, Entry> m : owners.values()) n += m.size();
        return n;
    }
}
