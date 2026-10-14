package com.newuniverse.nusmp.boss.budget;

/** The numbers behind the budget (pure, unit tested): the cap of a boss from its max health, the config and its phase. */
public final class BudgetPolicy {
    private BudgetPolicy() {}

    /**
     * Health per second a boss may lose: max health * fraction * phase scale, never below the absolute minimum (so a test boss of a few
     * hearts still dies) and never above its max health (a cap above the whole bar is no cap, but is still finite).
     */
    public static float capPerSecond(float maxHealth, double fraction, double minimum, double phaseScale) {
        if (!(maxHealth > 0f)) return 0f;
        double f = Double.isNaN(fraction) ? 0.06 : Math.max(0.0, fraction);
        double s = Double.isNaN(phaseScale) ? 1.0 : Math.max(1.0, phaseScale);
        double m = Double.isNaN(minimum) ? 0.0 : Math.max(0.0, minimum);
        double c = Math.max(maxHealth * f * s, m);
        return (float) Math.min(c, maxHealth);
    }

    /**
     * The scale of the cap by phase. Below the next-to-last stage 1; one stage before the final 1 + (finalScale - 1) / 2; in the final
     * (or enraged) stage finalScale. A finalScale under 1 is read as 1: the final phase never gets a tighter cap.
     */
    public static double phaseScale(int stage, int finalStage, double finalScale) {
        double fs = Double.isNaN(finalScale) ? 1.0 : Math.max(1.0, finalScale);
        if (finalStage <= 0) return 1.0;
        if (stage >= finalStage) return fs;
        if (stage == finalStage - 1) return 1.0 + (fs - 1.0) / 2.0;
        return 1.0;
    }

    /**
     * The health a boss ends on when something sets it from 'current' to 'requested'. A rise (heal, restore) or a fall to/above the max
     * health passes untouched; a fall is cut to what 'allowed' lets through. If the whole fall fits, 'requested' is returned exactly
     * (no float drift, so a fall to 0 really reaches 0 and the death logic runs).
     */
    public static float settle(float current, float requested, float allowed) {
        if (Float.isNaN(requested)) return current;
        if (requested >= current) return requested;
        float drop = current - requested;
        return allowed >= drop ? requested : current - allowed;
    }
}
