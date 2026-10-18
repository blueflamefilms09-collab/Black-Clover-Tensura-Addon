package com.newuniverse.nusmp.heroweapon.weapon.sword;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: a technique that strikes in several pulses (a rain of blades, floating swords, a barrage of stars) must not let one target take more than the technique's
 * budget in total, however the pulses overlap, and must never touch more than twelve targets. This is the running total per target for ONE cast: {@link #take}
 * says how much of a wanted hit may still land. Pure, no Minecraft types, tested alone.
 */
public final class HitCap {
    private final double limit;
    private final int maxTargets;
    private final Map<UUID, Double> taken = new HashMap<>();

    public HitCap(double limit) { this(limit, SwordNumbers.MAX_TARGETS); }

    public HitCap(double limit, int maxTargets) {
        this.limit = Math.max(0.0, limit);
        this.maxTargets = Math.max(1, maxTargets);
    }

    /** How much of a wanted {@code raw} hit may still land on this target (0 when it has had its share, or the cap is full of other targets); the amount is counted. */
    public double take(UUID target, double raw) {
        if (target == null || !(raw > 0)) return 0.0;
        Double so = taken.get(target);
        if (so == null && taken.size() >= maxTargets) return 0.0;
        double used = so == null ? 0.0 : so;
        double allowed = Math.max(0.0, Math.min(raw, limit - used));
        if (allowed > 0) taken.put(target, used + allowed);
        return allowed;
    }

    public double total(UUID target) { return taken.getOrDefault(target, 0.0); }

    public int targets() { return taken.size(); }
}
