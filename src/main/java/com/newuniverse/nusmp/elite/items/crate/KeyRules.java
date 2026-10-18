package com.newuniverse.nusmp.elite.items.crate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;

/**
 * Why: the decision of the key loot modifier with no Minecraft in it, so the chances are testable: for one chest table, each tier has its own chance (0 to 1) from the data file, multiplied by the
 * operator's scale and capped at 1; each tier is rolled once, independently, so one chest can (rarely) hold two keys. Returns the tiers whose key is added. A table the file does not list pays nothing.
 */
public final class KeyRules {
    private KeyRules() {}

    /** The effective chance of a tier: the file's value times the scale, kept in 0..1; 0 for a missing or non-finite value. */
    public static double chance(Map<String, Double> perTier, String tier, double scale) {
        if (perTier == null) return 0.0;
        Double c = perTier.get(tier);
        if (c == null || c.isNaN() || scale <= 0 || Double.isNaN(scale)) return 0.0;
        double v = c * scale;
        return v <= 0 ? 0.0 : Math.min(1.0, v);
    }

    /** Which tiers' keys to add for a chest whose table has these chances. */
    public static List<CrateTier> roll(Map<String, Double> perTier, double scale, DoubleSupplier dice) {
        List<CrateTier> out = new ArrayList<>();
        if (perTier == null) return out;
        for (CrateTier t : CrateTier.values()) {
            double c = chance(perTier, t.id(), scale);
            if (c > 0 && dice.getAsDouble() < c) out.add(t);
        }
        return out;
    }
}
