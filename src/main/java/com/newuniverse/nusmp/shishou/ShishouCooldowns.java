package com.newuniverse.nusmp.shishou;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: every cast of a skill is gated by a cooldown that is decided on the server and keyed by the caster's UUID (the wielder), never by a "current player". One small table of ready times per
 * caster, one slot per skill. Pure. A caster is forgotten at logout, death, dimension change and server stop (the caller), so the table never grows past the number of players online.
 */
public final class ShishouCooldowns {
    private final Map<UUID, long[]> ready = new HashMap<>();

    private long[] row(UUID who) { return ready.computeIfAbsent(who, k -> new long[ShishouSkill.values().length]); }

    public boolean ready(UUID who, ShishouSkill s, long now) {
        long[] r = ready.get(who);
        return r == null || now >= r[s.ordinal()];
    }

    public void start(UUID who, ShishouSkill s, long now, int ticks) { row(who)[s.ordinal()] = now + Math.max(0, ticks); }

    public int left(UUID who, ShishouSkill s, long now) {
        long[] r = ready.get(who);
        return r == null ? 0 : (int) Math.max(0L, r[s.ordinal()] - now);
    }

    /** The cooldown of a skill as a share 0..1 of its full length (for the HUD bar). */
    public double share(UUID who, ShishouSkill s, long now) {
        if (s.cooldown() <= 0) return 0.0;
        return Math.min(1.0, left(who, s, now) / (double) s.cooldown());
    }

    public void forget(UUID who) { ready.remove(who); }

    public void clear() { ready.clear(); }

    public int tracked() { return ready.size(); }

    /**
     * Can {@code who} cast {@code s} now with {@code mana} in the pool? 0 = yes, 1 = on cooldown, 2 = not enough mana. The cost and the cooldown are the table's ({@link ShishouSkill}) unless the caller
     * passes its own (a config).
     */
    public int check(UUID who, ShishouSkill s, long now, double mana, double cost) {
        if (!ready(who, s, now)) return 1;
        if (mana + 1e-9 < cost) return 2;
        return 0;
    }
}
