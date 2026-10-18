package com.newuniverse.nusmp.skill.rules;

/**
 * Why: cost, cooldown and clamp arithmetic that every skill repeats. Keeping it Minecraft-free means the numbers that decide whether a
 * cast is affordable or a cooldown is sane are covered by unit tests instead of being checked by hand in game.
 */
public final class SkillMath {
    private SkillMath() {}

    public static int clamp(int v, int lo, int hi) { return v < lo ? lo : Math.min(v, hi); }
    public static double clamp(double v, double lo, double hi) { return v < lo ? lo : Math.min(v, hi); }

    /** Cooldown in ticks after the pack-wide multiplier; never negative, 0 stays 0. */
    public static int cooldown(int baseTicks, double multiplier) {
        if (baseTicks <= 0) return 0;
        return (int) Math.max(0, Math.round(baseTicks * Math.max(0.0, multiplier)));
    }

    /** Magicule cost after the pack-wide multiplier; never negative. */
    public static double cost(double base, double multiplier) {
        return Math.max(0.0, base) * Math.max(0.0, multiplier);
    }

    /** Cooldown is given to ManasCore in whole seconds (rounded up, at least 1 for a real cooldown). */
    public static int ticksToSeconds(int ticks) { return ticks <= 0 ? 0 : Math.max(1, (ticks + 19) / 20); }

    /** Share (0..1) of a bar that is empty: used by the "stronger the closer to death" skills. */
    public static double missing(double health, double max) {
        if (max <= 0) return 0;
        return clamp(1.0 - health / max, 0.0, 1.0);
    }

    /** Linear interpolation. */
    public static double lerp(double a, double b, double t) { return a + (b - a) * clamp(t, 0.0, 1.0); }
}
