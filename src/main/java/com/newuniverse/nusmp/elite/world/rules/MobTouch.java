package com.newuniverse.nusmp.elite.world.rules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the "mob replacement handler" of the owner's inventory, as pure rules. In a chunk dense with magicule a share of the natural spawns of ordinary (vanilla) monsters is replaced by a
 * stronger "magicule-touched" variant. The entity type stays the same on purpose (a touched zombie is a zombie, so every other mod's rules about it still hold); what changes is its tier:
 * health, damage, speed and armor multipliers, a visible marker and better drops. The chance rises with the density above a threshold and is capped; the tier rises with the density.
 * The tables are data ({@code data/nusmp/elite/world/mob_touch.json}) so a pack can retune them.
 */
public final class MobTouch {
    private MobTouch() {}

    /** The multipliers of one tier, and the number of bonus drops rolled for it. */
    public record Stats(double health, double damage, double speed, double armorAdd, int bonusDrops, double xpMult) {}

    /** The default tiers (index 0 = tier 1). A table file can replace them. */
    public static final List<Stats> DEFAULT_TIERS = List.of(
            new Stats(1.5, 1.25, 1.03, 2.0, 1, 1.5),
            new Stats(2.2, 1.55, 1.06, 4.0, 2, 2.2),
            new Stats(3.2, 2.0, 1.10, 6.0, 3, 3.0));
    /** The density at which tier 2 and tier 3 begin (tier 1 begins at the config threshold). */
    public static final double TIER2_DENSITY = 0.80, TIER3_DENSITY = 0.92;

    /** The chance that one natural spawn is touched: 0 under the threshold, then {@code base} growing to {@code max} at density 1. */
    public static double chance(double density, double threshold, double base, double max) {
        double d = MagiculeModel.clamp01(density);
        if (d < threshold || threshold >= 1.0 || base <= 0.0 || max <= 0.0) return 0.0;
        double t = (d - threshold) / (1.0 - threshold);
        return Math.min(Math.min(1.0, max), base * (1.0 + 3.0 * t));
    }

    /** 1..3 from the density (0 when under the threshold). */
    public static int tier(double density, double threshold) {
        double d = MagiculeModel.clamp01(density);
        if (d < threshold) return 0;
        if (d >= TIER3_DENSITY) return 3;
        if (d >= TIER2_DENSITY) return 2;
        return 1;
    }

    /** The data table: who may be touched and the tier stats. */
    public static final class Table {
        public final Set<String> eligible = new HashSet<>();
        public final Set<String> exclude = new HashSet<>();
        public List<Stats> tiers = DEFAULT_TIERS;

        public Table() {}

        public boolean allows(String entityId) {
            if (entityId == null || exclude.contains(entityId)) return false;
            return eligible.contains(entityId);
        }

        public Stats stats(int tier) {
            if (tiers.isEmpty()) return DEFAULT_TIERS.get(0);
            return tiers.get(Math.max(0, Math.min(tiers.size() - 1, tier - 1)));
        }

        /** Reads one file into this table: the lists are added to, the tier list (if present) replaces the current one. */
        public void add(JsonObject root) {
            if (root == null) return;
            readIds(root, "eligible", eligible);
            readIds(root, "exclude", exclude);
            if (root.has("tiers") && root.get("tiers").isJsonArray()) {
                List<Stats> out = new ArrayList<>();
                for (JsonElement el : root.getAsJsonArray("tiers")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    out.add(new Stats(num(o, "health", 1.5), num(o, "damage", 1.25), num(o, "speed", 1.0), num(o, "armor", 2.0), (int) num(o, "bonusDrops", 1), num(o, "xp", 1.5)));
                }
                if (!out.isEmpty()) tiers = List.copyOf(out);
            }
        }

        private static void readIds(JsonObject root, String key, Set<String> into) {
            if (!root.has(key) || !root.get(key).isJsonArray()) return;
            JsonArray a = root.getAsJsonArray(key);
            for (JsonElement e : a) if (e.isJsonPrimitive()) into.add(e.getAsString());
        }

        private static double num(JsonObject o, String k, double d) { return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsDouble() : d; }
    }
}
