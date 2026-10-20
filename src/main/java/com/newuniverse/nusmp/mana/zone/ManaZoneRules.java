package com.newuniverse.nusmp.mana.zone;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The numbers of the Mana Zone, read from {@code data/nusmp/mana_zone.json} (pure: no Minecraft types, so JUnit and the CI self-test
 * check the same file the game loads). Every field falls back to the default below when the file leaves it out, so a datapack may
 * override a single number. The gate numbers are FLOORS in {@link ManaZoneGate}: a file or a config value cannot make the technique early.
 */
public record ManaZoneRules(Gate gate, Zone zone) {

    /** What a player must have done. {@code minEp} is Tensura existence points (BalanceLaw.epScale: x9.2 at 2,000,000). */
    public record Gate(double minEp, int minKnightStep, int minBossKills, int minDeepestFloor, double grimoireMastery, double reinforcementMastery,
                       int attuneSeconds) {}

    /** The mechanics of an open zone. All times are in ticks, all percents are of the caster's maximum magicule. */
    public record Zone(int growTicks, double baseRadius, double maxRadius, double radiusPerEpDecade, int maxDurationTicks,
                       double openCostPercent, double startMinPercent, double upkeepBasePercent, double upkeepPerBlockPercent,
                       int cooldownBaseTicks, int cooldownPerBlockTicks, int exhaustionBaseTicks, int exhaustionMaxTicks,
                       double costMult, double powerBonus, double powerDensityBonus, double rangeMult, int extraTargets, double speedBonus,
                       double predictBase, double predictReinforced, double predictBossCap, double allyPressure,
                       double bubbleReduction, double bubbleRadius, int bubbleTicks, int maxBubbles,
                       int maxProjectilesPerScan, double bossProjectileSlow, int scanInterval, int refreshInterval,
                       int volleyMaxTargets, int volleyBoltsPerTarget, double volleyDamage, double volleyBossDamageMult) {}

    public static final Gate DEFAULT_GATE = new Gate(2_000_000.0, 14, 15, 40, 1.0, 0.6, 1800);

    public static final Zone DEFAULT_ZONE = new Zone(60, 14.0, 40.0, 8.0, 4800,
            5.0, 25.0, 0.15, 0.02,
            600, 20, 100, 400,
            0.8, 0.25, 0.1, 1.6, 3, 0.2,
            0.15, 0.35, 0.15, 0.15,
            0.45, 5.0, 160, 2,
            6, 0.55, 4, 100,
            6, 3, 6.0, 0.5);

    public static ManaZoneRules defaults() { return new ManaZoneRules(DEFAULT_GATE, DEFAULT_ZONE); }

    /** Parses the file's text; a broken file gives the defaults (the reload listener logs it). */
    public static ManaZoneRules parse(String json) {
        try {
            JsonElement root = JsonParser.parseString(json);
            return root.isJsonObject() ? parse(root.getAsJsonObject()) : defaults();
        } catch (RuntimeException e) {
            return defaults();
        }
    }

    public static ManaZoneRules parse(JsonObject root) {
        JsonObject g = obj(root, "gate"), z = obj(root, "zone"), skin = obj(g, "mana_skin");
        Gate d = DEFAULT_GATE;
        Gate gate = new Gate(num(g, "min_ep", d.minEp), (int) num(g, "min_knight_step", d.minKnightStep), (int) num(g, "min_boss_kills", d.minBossKills),
                (int) num(g, "min_deepest_floor", d.minDeepestFloor), num(skin, "grimoire_mastery", d.grimoireMastery),
                num(skin, "or_reinforcement_mastery", d.reinforcementMastery), (int) num(g, "attune_seconds", d.attuneSeconds));
        Zone q = DEFAULT_ZONE;
        Zone zone = new Zone((int) num(z, "grow_ticks", q.growTicks), num(z, "base_radius", q.baseRadius), num(z, "max_radius", q.maxRadius),
                num(z, "radius_per_ep_decade", q.radiusPerEpDecade), (int) num(z, "max_duration_ticks", q.maxDurationTicks),
                num(z, "open_cost_percent", q.openCostPercent), num(z, "start_min_percent", q.startMinPercent),
                num(z, "upkeep_base_percent", q.upkeepBasePercent), num(z, "upkeep_per_block_percent", q.upkeepPerBlockPercent),
                (int) num(z, "cooldown_base_ticks", q.cooldownBaseTicks), (int) num(z, "cooldown_per_block_ticks", q.cooldownPerBlockTicks),
                (int) num(z, "exhaustion_base_ticks", q.exhaustionBaseTicks), (int) num(z, "exhaustion_max_ticks", q.exhaustionMaxTicks),
                num(z, "cost_mult", q.costMult), num(z, "power_bonus", q.powerBonus), num(z, "power_density_bonus", q.powerDensityBonus),
                num(z, "range_mult", q.rangeMult), (int) num(z, "extra_targets", q.extraTargets), num(z, "speed_bonus", q.speedBonus),
                num(z, "predict_base", q.predictBase), num(z, "predict_reinforced", q.predictReinforced), num(z, "predict_boss_cap", q.predictBossCap),
                num(z, "ally_pressure", q.allyPressure), num(z, "bubble_reduction", q.bubbleReduction), num(z, "bubble_radius", q.bubbleRadius),
                (int) num(z, "bubble_ticks", q.bubbleTicks), (int) num(z, "max_bubbles", q.maxBubbles),
                (int) num(z, "max_projectiles_per_scan", q.maxProjectilesPerScan), num(z, "boss_projectile_slow", q.bossProjectileSlow),
                (int) num(z, "scan_interval", q.scanInterval), (int) num(z, "refresh_interval", q.refreshInterval),
                (int) num(z, "volley_max_targets", q.volleyMaxTargets), (int) num(z, "volley_bolts_per_target", q.volleyBoltsPerTarget),
                num(z, "volley_damage", q.volleyDamage), num(z, "volley_boss_damage_mult", q.volleyBossDamageMult));
        return new ManaZoneRules(gate, sanitize(zone));
    }

    /** Keeps every number inside a range in which the mechanics stay sane and cheap (a datapack typo cannot make a zone a lag machine). */
    static Zone sanitize(Zone z) {
        double max = clamp(z.maxRadius, 8.0, 64.0), base = clamp(z.baseRadius, 4.0, max);
        return new Zone((int) clamp(z.growTicks, 10, 400), base, max, clamp(z.radiusPerEpDecade, 0.0, 16.0), (int) clamp(z.maxDurationTicks, 200, 24_000),
                clamp(z.openCostPercent, 0.0, 50.0), clamp(z.startMinPercent, 0.0, 100.0), clamp(z.upkeepBasePercent, 0.0, 10.0), clamp(z.upkeepPerBlockPercent, 0.0, 1.0),
                (int) clamp(z.cooldownBaseTicks, 0, 72_000), (int) clamp(z.cooldownPerBlockTicks, 0, 1200), (int) clamp(z.exhaustionBaseTicks, 0, 6000),
                (int) clamp(z.exhaustionMaxTicks, 0, 12_000), clamp(z.costMult, 0.1, 1.0), clamp(z.powerBonus, 0.0, 1.0), clamp(z.powerDensityBonus, 0.0, 0.5),
                clamp(z.rangeMult, 1.0, 2.5), (int) clamp(z.extraTargets, 0, 8), clamp(z.speedBonus, 0.0, 0.6),
                clamp(z.predictBase, 0.0, 0.6), clamp(z.predictReinforced, 0.0, 0.7), clamp(z.predictBossCap, 0.0, 0.3), clamp(z.allyPressure, 0.0, 0.4),
                clamp(z.bubbleReduction, 0.0, 0.7), clamp(z.bubbleRadius, 2.0, 12.0), (int) clamp(z.bubbleTicks, 20, 1200), (int) clamp(z.maxBubbles, 1, 4),
                (int) clamp(z.maxProjectilesPerScan, 1, 24), clamp(z.bossProjectileSlow, 0.1, 1.0), (int) clamp(z.scanInterval, 2, 20), (int) clamp(z.refreshInterval, 40, 400),
                (int) clamp(z.volleyMaxTargets, 1, 12), (int) clamp(z.volleyBoltsPerTarget, 1, 6), clamp(z.volleyDamage, 1.0, 40.0), clamp(z.volleyBossDamageMult, 0.1, 1.0));
    }

    private static JsonObject obj(JsonObject o, String key) {
        JsonElement e = o == null ? null : o.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
    }

    private static double num(JsonObject o, String key, double fallback) {
        JsonElement e = o == null ? null : o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return fallback;
        double v = e.getAsDouble();
        return Double.isNaN(v) || Double.isInfinite(v) ? fallback : v;
    }

    private static double clamp(double v, double lo, double hi) { return v < lo ? lo : Math.min(v, hi); }
}
