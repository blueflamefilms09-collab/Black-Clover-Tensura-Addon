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
    /** 1.107.0, {@code [foundry.poise]}: poise and super armor, the desperation phase, telegraphed attacks, and the passive dungeon (docs/labyrinth/poise_desperation.md). */
    public static final ModConfigSpec.BooleanValue POISE, DESPERATION, TELEGRAPHS, DUNGEON_PASSIVE;
    public static final ModConfigSpec.IntValue ELITE_TIER, STAGGER_TICKS;
    public static final ModConfigSpec.DoubleValue DESPERATE_BELOW, ARMOR_REDUCTION;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("foundry");
        ENABLED = b.comment("The Dungeon Master may spawn foundry mobs. false = only the old encounter tables spawn (the foundry mobs that exist keep working, and /multiverse mega foundry spawn still makes them).")
                .define("enabled", true);
        WEIGHT = b.comment("Percent of the Dungeon Master's spawned monsters that are replaced by a foundry mob of the floor's tier and road (0 = none, 100 = all). The rest come from the existing tables.")
                .defineInRange("weight", 60, 0, 100);
        ABILITIES = b.comment("Foundry mobs use their abilities. false = they only walk and hit (a quick way to see whether an ability is what is hurting).")
                .define("abilities", true);
        FACTION_WARS = b.comment("Foundry mobs of rival factions fight each other when no player is near (the turf wars of the eco package, for the foundry's single entity type). Default OFF since 1.107.0: the dungeon's monsters are one side and hunt the player. It also needs foundry.poise.dungeonPassive = false to run.")
                .define("factionWars", false);
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
        b.push("poise");
        POISE = b.comment("Elites (foundry mobs of the elite tier, anything tagged nusmp_elite) and squad captains carry a poise bar: super armor while they wind up a telegraphed attack, until the bar breaks and they stagger. false = no bars, no super armor.")
                .define("enabled", true);
        TELEGRAPHS = b.comment("Foundry mobs warn before their attacks: the ground the attack will hit is drawn with particles, the wind-up is long enough to read and the aim freezes for the last third. false = the old ability goal (short wind-ups, no floor warning). Read when a mob is created.")
                .define("telegraphedAttacks", true);
        DESPERATION = b.comment("An elite or captain under the desperation share of health attacks faster, shows a cue and (foundry mobs) unlocks its archetype's ultimate. false = no desperation phase.")
                .define("desperation", true);
        DESPERATE_BELOW = b.comment("The health share under which desperation starts (it ends again 15 percent higher, so a healer cannot make it flicker).")
                .defineInRange("desperateBelow", 0.30, 0.05, 0.60);
        ELITE_TIER = b.comment("A foundry mob of this tier or above (1 to 5; 200 floors per tier) is an elite and carries a poise bar. 6 = none by tier (the tag and the squad captains still do).")
                .defineInRange("eliteTier", 3, 1, 6);
        ARMOR_REDUCTION = b.comment("The share of a blow that super armor takes off (the blow still hurts, but never staggers or pushes while the armor is up).")
                .defineInRange("armorReduction", 0.35, 0.0, 0.9);
        STAGGER_TICKS = b.comment("How long a broken poise bar leaves the bearer helpless, in ticks (20 = one second).")
                .defineInRange("staggerTicks", 50, 10, 200);
        DUNGEON_PASSIVE = b.comment("The dungeon's monsters are one faction: none of them targets or hurts another (arrows, blasts, abilities included), and the old quarrels (eco.turfWarsEnabled, foundry.factionWars) do not run, whatever those two say. Servants and pets of players are not affected. false = the old game: those two switches decide again (both default to false).")
                .define("dungeonPassive", true);
        b.pop();
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
