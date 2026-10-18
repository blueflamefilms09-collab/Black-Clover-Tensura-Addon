package com.newuniverse.nusmp.heroweapon.curse;

import com.newuniverse.nusmp.heroweapon.Sin;

import java.util.Map;

/**
 * Why: each of the seven sins takes something different from its wielder while a curse form is worn (Wrath burns their blood, Pride refuses their help, Lust drags
 * them at the nearest foe, Envy takes one of their techniques, Greed weighs them down with treasure, Gluttony empties their stomach, Sloth slows their hands), and
 * the owner's rule is that every one of them is bounded and none of them can kill. The numbers behind that promise are here, as plain functions, so the promise
 * is a test and not a hope: a drain never goes below its floor, a slow never passes its clamp, a pull never points the wrong way or reaches past its range, a
 * heal is only ever reduced. The game glue ({@link CurseEffects}) reads the world, asks these functions, and applies the answer. No Minecraft types.
 */
public final class SinMath {
    private SinMath() {}

    /** One heart is two health points: the curse itself never takes the wielder below this. */
    public static final double ONE_HEART = 2.0;

    /** The largest share of speed a slow may take (60%), whatever the config says. */
    public static final double MAX_SLOW = 0.60;

    private static double clamp(double v, double lo, double hi) { return Double.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v)); }

    // ------------------------------------------------------------------ Wrath
    /**
     * The health Wrath will not drain below: the configured share of maximum health, but never less than one heart, and never more than the maximum itself (a
     * wielder with less than a heart of maximum health is not drained at all).
     */
    public static double wrathFloor(double maxHealth, double floorShare) {
        double share = clamp(floorShare, 0.0, 1.0);
        return Math.min(Math.max(0.0, maxHealth), Math.max(ONE_HEART, maxHealth * share));
    }

    /** The health Wrath wants this second: the larger of the share of maximum health and the minimum, times the sin's intensity. */
    public static double wrathWant(double maxHealth, double drainShare, double drainMin, double intensity) {
        return Math.max(Math.max(0.0, drainMin), Math.max(0.0, maxHealth) * clamp(drainShare, 0.0, 1.0)) * clamp(intensity, 0.0, 3.0);
    }

    /** What a drain may really take: the wanted amount, but never down below {@code floor}, and never negative. */
    public static double drain(double health, double floor, double want) { return Math.max(0.0, Math.min(want, health - Math.max(floor, ONE_HEART))); }

    // ------------------------------------------------------------------ healing (Wrath and Pride)
    /**
     * The factor a heal of {@code amount} health points is multiplied by while the sin is worn: Wrath's flames reduce every heal by {@code wrathCut}; Pride refuses
     * help, so a heal larger than {@code prideSelfMax} (natural recovery and the regeneration effect heal one point at a time, a potion or an ally's technique
     * heals more) is reduced by {@code prideCut}; every other sin leaves heals alone. Both cuts are scaled by the intensity; the result is between 0 and 1.
     */
    public static float healScale(Sin sin, double amount, double intensity, double wrathCut, double prideCut, double prideSelfMax) {
        double k = clamp(intensity, 0.0, 3.0);
        if (sin == Sin.WRATH) return (float) clamp(1.0 - clamp(wrathCut, 0.0, 1.0) * k, 0.0, 1.0);
        if (sin == Sin.PRIDE && amount > prideSelfMax) return (float) clamp(1.0 - clamp(prideCut, 0.0, 1.0) * k, 0.0, 1.0);
        return 1.0f;
    }

    // ------------------------------------------------------------------ Gluttony
    /**
     * The food exhaustion Gluttony adds this second: the configured amount times the intensity, but nothing once the stomach is at the floor, so the curse can
     * never starve its wielder.
     */
    public static float foodExhaust(int foodLevel, int floor, double perSecond, double intensity) {
        if (foodLevel <= Math.max(0, floor)) return 0f;
        return (float) Math.max(0.0, perSecond * clamp(intensity, 0.0, 3.0));
    }

    /** The food points a kill gives back, without passing the full stomach of 20. */
    public static int feed(int foodLevel, int gain) { return Math.max(0, Math.min(20 - foodLevel, gain)); }

    // ------------------------------------------------------------------ slows (Sloth and Greed)
    /** A share of speed to take, scaled by the intensity and clamped to {@link #MAX_SLOW}. */
    public static double slow(double share, double intensity) { return clamp(share * clamp(intensity, 0.0, 3.0), 0.0, MAX_SLOW); }

    /** Greed's slow: the share of {@code maxSlow} that the carried treasure ({@code score}) reaches out of {@code full}, scaled by the intensity, clamped. */
    public static double burden(int score, int full, double maxSlow, double intensity) {
        if (score <= 0 || full <= 0) return 0.0;
        return slow(Math.min(1.0, score / (double) full) * clamp(maxSlow, 0.0, 1.0), intensity);
    }

    private static final Map<String, Integer> TREASURE = Map.ofEntries(
            Map.entry("minecraft:gold_ingot", 1), Map.entry("minecraft:diamond", 1), Map.entry("minecraft:emerald", 1),
            Map.entry("minecraft:netherite_ingot", 3), Map.entry("minecraft:netherite_scrap", 1), Map.entry("minecraft:ancient_debris", 3),
            Map.entry("minecraft:gold_block", 9), Map.entry("minecraft:diamond_block", 9), Map.entry("minecraft:emerald_block", 9), Map.entry("minecraft:netherite_block", 27),
            Map.entry("minecraft:nether_star", 8), Map.entry("minecraft:enchanted_golden_apple", 4), Map.entry("minecraft:totem_of_undying", 4));

    /** How much one item of this registry id weighs on Greed (valuables only; everything else is 0). */
    public static int treasureWeight(String itemId) { return itemId == null ? 0 : TREASURE.getOrDefault(itemId, 0); }

    // ------------------------------------------------------------------ Lust
    /**
     * The sideways nudge toward a foe: a unit direction times {@code strength} from (px, pz) toward (tx, tz), or null when the foe is closer than {@code minDist}
     * (the wielder is already in reach), further than {@code range}, or exactly on top of the wielder (no direction).
     */
    public static double[] pull(double px, double pz, double tx, double tz, double minDist, double range, double strength) {
        double dx = tx - px, dz = tz - pz;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (!(dist > 1.0e-6) || dist < minDist || dist > range || strength <= 0.0) return null;
        return new double[]{dx / dist * strength, dz / dist * strength};
    }

    // ------------------------------------------------------------------ Sloth and Envy
    /** True while Sloth still holds the next technique back: less than {@code lockTicks} since the last technique (a clock that ran backwards counts as free). */
    public static boolean slothLocked(long now, long lastTechnique, int lockTicks) { return lastTechnique > 0 && now >= lastTechnique && now - lastTechnique < lockTicks; }

    /** The index Envy takes among {@code count} techniques, from a roll in [0, 1); -1 when there is nothing to take. */
    public static int envyPick(int count, double roll) {
        if (count <= 0) return -1;
        return Math.min(count - 1, (int) Math.floor(clamp(roll, 0.0, 0.999999999) * count));
    }
}
