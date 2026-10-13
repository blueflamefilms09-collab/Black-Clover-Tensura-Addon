package com.newuniverse.nusmp.relics;

/**
 * What an ability costs out of a pool measured in Tensura's own units: a percentage of the pool's maximum, refused if it cannot be paid or
 * would leave the pool under a floor. Pure, so the rules (mana may be spent to nothing, existence points keep 10%) are tested.
 */
public final class CostMath {
    private CostMath() {}

    /** Whether a cost can be paid and what the pool holds afterwards. */
    public record Spend(boolean ok, double after) {}

    /** The fraction of the maximum an existence-point cost may never take a pool below. */
    public static final double EP_FLOOR = 0.10;

    /**
     * @param current  the pool now
     * @param max      the pool's maximum; a pool with no maximum (no Tensura data) is free
     * @param percent  the cost, in percent of {@code max}
     * @param floorFrac the share of {@code max} that must remain
     */
    public static Spend spend(double current, double max, double percent, double floorFrac) {
        if (percent <= 0 || max <= 0) return new Spend(true, current);
        double cost = max * percent / 100.0;
        double after = current - cost;
        if (current < cost || after < max * Math.max(0.0, floorFrac) - 1.0e-9) return new Spend(false, current);
        return new Spend(true, Math.max(0.0, after));
    }

    /** What two pools hold after a cost taken from the first and, for what it cannot cover, the second; ok is false (nothing taken) if both together cannot pay. */
    public record Pooled(boolean ok, double first, double second) {}

    /** Magicules first, then aura for the rest (how the Kotodama words pay). A cost of zero or less is free. */
    public static Pooled spendPooled(double first, double second, double cost) {
        if (cost <= 0) return new Pooled(true, first, second);
        if (first + second < cost) return new Pooled(false, first, second);
        double fromFirst = Math.min(cost, first);
        return new Pooled(true, first - fromFirst, cost > fromFirst ? Math.max(0.0, second - (cost - fromFirst)) : second);
    }
}
