package com.newuniverse.nusmp.elite.nations.core;

/**
 * Why: the tier scaling of the nation guards ("nation spawner trackers, entity tags, tier scaling"), pure. How many guards a nation of a level keeps around its capital, which tier of the entity tags
 * they are drawn from, and how much tougher a higher tier makes them. Always bounded: a hard cap per nation and a global cap protect the server.
 */
public final class GuardRules {
    private GuardRules() {}

    public static final int MAX_TIER = 5;

    /** Guard tier of a nation level: 1 plus one per two levels, up to 5. */
    public static int tierOf(int nationLevel) { return Math.max(1, Math.min(MAX_TIER, 1 + (Math.max(1, nationLevel) - 1) / 2)); }

    /** Wanted guards: {@code base + perLevel * (level - 1) + perkExtra}, clamped to [0, hardCap]. */
    public static int desired(int nationLevel, int base, double perLevel, int perkExtra, int hardCap) {
        int d = (int) Math.floor(base + perLevel * (Math.max(1, nationLevel) - 1)) + perkExtra;
        return Math.max(0, Math.min(Math.max(0, hardCap), d));
    }

    /** How many to spawn this round: the shortfall, at most {@code perRound}, and never past the global cap. */
    public static int toSpawn(int desired, int alive, int perRound, int globalAlive, int globalCap) {
        int shortfall = desired - alive;
        if (shortfall <= 0) return 0;
        int room = globalCap <= 0 ? Integer.MAX_VALUE : Math.max(0, globalCap - globalAlive);
        return Math.max(0, Math.min(Math.min(shortfall, Math.max(0, perRound)), room));
    }

    /** Multiplier on max health for a tier: +25 percent per tier above 1. */
    public static double healthFactor(int tier) { return 1.0 + 0.25 * (Math.max(1, Math.min(MAX_TIER, tier)) - 1); }

    /** Bonus attack damage for a tier: +1 per tier above 1. */
    public static double damageBonus(int tier) { return Math.max(0, Math.min(MAX_TIER, tier) - 1); }

    /** Armour bonus for a tier: +1 per tier above 1. */
    public static double armorBonus(int tier) { return Math.max(0, Math.min(MAX_TIER, tier) - 1); }

    /**
     * Is a point inside the capital zone? Squared horizontal distance against radius, vertical slack {@code vertical}. Pure helper for the guard leash and the siege.
     */
    public static boolean inZone(int px, int py, int pz, int cx, int cy, int cz, int radius, int vertical) {
        long dx = px - cx, dz = pz - cz;
        return dx * dx + dz * dz <= (long) radius * radius && Math.abs(py - cy) <= vertical;
    }
}
