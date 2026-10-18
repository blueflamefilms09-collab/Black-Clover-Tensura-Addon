package com.newuniverse.nusmp.labyrinth.mega.foundry;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the Mob Foundry, {@code world/serverconfig/nusmp-labyrinth-mega-foundry-server.toml}, section {@code [foundry]}. Its own file (the pattern of
 * {@code HubConfig}, {@code RiftConfig}) so a server owner can retune or switch off the 400 custom mobs without touching the Dungeon Master's file. {@code weight} is the share
 * (percent) of the Dungeon Master's spawns that are replaced by a foundry mob of the floor's tier and road; the other spawns are the old tables, unchanged. Every value is a plain
 * {@code define} / {@code defineInRange} and every reader goes through {@link #get}, which answers the default while the spec is not loaded yet.
 */
public final class FoundryConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, ABILITIES, FACTION_WARS, EXTRA_DROPS;
    public static final ModConfigSpec.IntValue WEIGHT, MINION_CAP, WAR_SCAN_TICKS;
    public static final ModConfigSpec.DoubleValue HEALTH_MULT, DAMAGE_MULT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("foundry");
        ENABLED = b.comment("The Dungeon Master may spawn foundry mobs. false = only the old encounter tables spawn (the foundry mobs that exist keep working, and /multiverse mega foundry spawn still makes them).")
                .define("enabled", true);
        WEIGHT = b.comment("Percent of the Dungeon Master's spawned monsters that are replaced by a foundry mob of the floor's tier and road (0 = none, 100 = all). The rest come from the existing tables.")
                .defineInRange("weight", 60, 0, 100);
        ABILITIES = b.comment("Foundry mobs use their abilities. false = they only walk and hit (a quick way to see whether an ability is what is hurting).")
                .define("abilities", true);
        FACTION_WARS = b.comment("Foundry mobs of rival factions fight each other when no player is near (the turf wars of the eco package, for the foundry's single entity type).")
                .define("factionWars", true);
        WAR_SCAN_TICKS = b.comment("Ticks between two looks for a rival of a foundry mob.")
                .defineInRange("warScanTicks", 40, 10, 400);
        EXTRA_DROPS = b.comment("Foundry mobs also leave their faction's cut of meat and a threat-scaled bonus of Magic Stones (on top of the standard economy and gear drops).")
                .define("extraDrops", true);
        MINION_CAP = b.comment("The most minions one summoner keeps alive at once.")
                .defineInRange("minionCap", 6, 1, 20);
        HEALTH_MULT = b.comment("Multiplier on the health of every foundry mob (a server that finds them too hard or too soft).")
                .defineInRange("healthMultiplier", 1.0, 0.1, 10.0);
        DAMAGE_MULT = b.comment("Multiplier on the damage of every foundry mob.")
                .defineInRange("damageMultiplier", 1.0, 0.1, 10.0);
        b.pop();
        SPEC = b.build();
    }

    private FoundryConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try {
            return v.get();
        } catch (IllegalStateException notLoaded) {
            return v.getDefault();
        }
    }
}
