package com.newuniverse.nusmp.boss;

import java.util.Locale;

/**
 * Lucius's nullifications as fair damage reductions (pure, unit tested). The real Tensura skills (magic, physical-attack and
 * spatial-attack nullification) make a body untouchable by their kind, which cannot be lifted for the exposed window and would leave
 * the fight unwinnable; here each is a cut of the incoming hit that the framework's own damage law then still bounds
 * (ZogratisBoss.hurt runs the cut before PowerCurve.clampDamage). The cut is skipped for the exposed window, Julius's window,
 * anti-magic blows and anything that bypasses invulnerability, all in the glue.
 */
public final class LuciusMitigation {
    private LuciusMitigation() {}

    /** What a hit is, for the cut. NONE = environment and damage over time (nothing is cut). */
    public enum DamageClass { NONE, PHYSICAL, MAGIC, SPATIAL }

    /** Classifies a damage type by its registry path (lower case) and whether a living attacker is behind it. */
    public static DamageClass classify(String typePath, boolean hasAttacker) {
        String p = typePath == null ? "" : typePath.toLowerCase(Locale.ROOT);
        if (p.contains("space") || p.contains("spatial") || p.contains("dimension")) return DamageClass.SPATIAL;
        for (String k : new String[]{"magic", "elemental", "holy", "spirit", "darkness", "light", "lightning", "dragon_breath", "sonic", "soul"})
            if (p.contains(k)) return DamageClass.MAGIC;
        return hasAttacker ? DamageClass.PHYSICAL : DamageClass.NONE;
    }

    /** The fraction of the hit removed for a class at a strength (the global multiplier, 0..): never above {@link LuciusNumbers#REDUCE_CEILING}. */
    public static double cut(DamageClass c, double strength) {
        double base = switch (c) {
            case MAGIC -> LuciusNumbers.REDUCE_MAGIC;
            case PHYSICAL -> LuciusNumbers.REDUCE_PHYSICAL;
            case SPATIAL -> LuciusNumbers.REDUCE_SPATIAL;
            default -> 0;
        };
        double s = Double.isNaN(strength) ? 0 : Math.max(0, Math.min(BossBuffTable.MAX_MULTIPLIER, strength));
        return Math.max(0, Math.min(LuciusNumbers.REDUCE_CEILING, base * s));
    }

    /** The hit after the cut. Non-positive hits pass through unchanged; the result is never under a quarter of the hit. */
    public static float apply(DamageClass c, float amount, double strength) {
        if (amount <= 0f) return amount;
        float left = (float) (amount * (1.0 - cut(c, strength)));
        return Math.max(left, amount * (float) (1.0 - LuciusNumbers.REDUCE_CEILING));
    }
}
