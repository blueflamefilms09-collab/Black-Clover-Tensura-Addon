package com.newuniverse.nusmp.labyrinth.mega.system;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the System (the Solo Leveling layer of the Holy road), {@code world/serverconfig/nusmp-labyrinth-mega-system-server.toml}, section
 * {@code [system]}. Its own file (the pattern of {@code DndConfig}, {@code MegaConfig} and {@code EcoConfig}) so this builder's switches never share a spec with
 * another builder's. Everything the owner will want to tune after the first play is here and nowhere in code: whether the System exists at all, whether the
 * Abyssal road gets it too (the brief puts it on the Holy road, so the default is no), how often a floor has a quest, the weights of the two rewards, whether
 * a reward recharges a held core before it gives a new one, how long and how far a death stays extractable, and the strength, number and lifetime of the
 * shadows. Every value is a plain {@code define} / {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is still
 * loading) and every reader goes through {@link #get}, which answers the default while the spec is not loaded yet (item registration and data reloads run
 * before SERVER configs are read).
 */
public final class SystemConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, ABYSSAL_TOO, RECHARGE_FIRST, AMBUSH_QUESTS;
    public static final ModConfigSpec.DoubleValue QUEST_CHANCE, MINION_STRENGTH;
    public static final ModConfigSpec.IntValue STAT_WEIGHT, SHADOW_CORE_WEIGHT, EXTRACT_WINDOW_TICKS, EXTRACT_RADIUS, MAX_MINIONS, MINION_MINUTES, MAX_STAT_POINTS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("system");
        ENABLED = b.comment("The System: the holographic window, the floor quests, the stat rewards and the Shadow Core. false = none of it exists (a held core still opens no extraction).")
                .define("enabled", true);
        ABYSSAL_TOO = b.comment("The brief puts the System on the Holy road. true = the Abyssal floors (-1, -2 ...) have the window, the quests and the shadows too.")
                .define("abyssalToo", false);
        QUEST_CHANCE = b.comment("The chance a new floor has a quest at all (0 to 1). 1 = every floor that is not a boss floor.")
                .defineInRange("questChance", 1.0, 0.0, 1.0);
        AMBUSH_QUESTS = b.comment("Offer the 'survive the ambush' quest. It counts creatures of the Dungeon Master's ambushes, so switch it off if the encounter director is switched off.")
                .define("ambushQuests", true);
        STAT_WEIGHT = b.comment("Weight of a permanent stat point in the reward roll (against shadowCoreWeight). 70 and 30 give 70 percent stat points.")
                .defineInRange("statWeight", 70, 0, 1000);
        SHADOW_CORE_WEIGHT = b.comment("Weight of a Shadow Core in the reward roll. 0 = rewards are always stat points.")
                .defineInRange("shadowCoreWeight", 30, 0, 1000);
        RECHARGE_FIRST = b.comment("A Shadow Core reward first recharges a core the player holds (by one charge) and only gives a new core when every held core is full.")
                .define("rechargeFirst", true);
        MAX_STAT_POINTS = b.comment("The most points one stat can hold. The strength of a point is in data/nusmp/mega/system/stats.json.")
                .defineInRange("maxStatPoints", 30, 1, 1000);
        EXTRACT_WINDOW_TICKS = b.comment("How long (ticks) a slain creature stays extractable by its killer. 100 = five seconds.")
                .defineInRange("extractWindowTicks", 100, 20, 600);
        EXTRACT_RADIUS = b.comment("How close (blocks) the player must be to the spot where the creature fell.")
                .defineInRange("extractRadius", 12, 2, 32);
        MAX_MINIONS = b.comment("The most shadows one player can have. Raising another releases the oldest. 0 = the core cannot raise any.")
                .defineInRange("maxMinions", 3, 0, 10);
        MINION_STRENGTH = b.comment("A shadow's health and attack damage as a share of what the creature had when it fell.")
                .defineInRange("minionStrength", 0.7, 0.1, 2.0);
        MINION_MINUTES = b.comment("Minutes a shadow lives on after its owner left (logged out, or went out of the Mega-Dungeon). It lives until it dies while the owner is there.")
                .defineInRange("minionMinutes", 30, 1, 1440);
        b.pop();
        SPEC = b.build();
    }

    private SystemConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** Convenience: is the System switched on? */
    public static boolean enabled() { return get(ENABLED); }
}
