package com.newuniverse.nusmp.labyrinth.mega.system;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the System (the Solo Leveling layer of the dungeon), {@code world/serverconfig/nusmp-labyrinth-mega-system-server.toml}, section
 * {@code [system]}. Its own file (the pattern of {@code DndConfig}, {@code MegaConfig} and {@code EcoConfig}) so this builder's switches never share a spec with
 * another builder's. Everything the owner will want to tune after the first play is here and nowhere in code: whether the System exists at all, how often a floor
 * has a quest, the weights of the two rewards, whether a reward recharges a held core before it gives a new one, how long and how far a death stays extractable,
 * the strength, number and lifetime of the shadows, and (1.58.0) whether the stat buff works only inside the dungeon dimensions, whether a new player is told
 * how to move the window, and how far a party member's kill is shared. {@code abyssalToo} is kept so that old config files load, but it no longer matters: since
 * 1.58.0 the System covers every road. Every value is a plain {@code define} / {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is still
 * loading) and every reader goes through {@link #get}, which answers the default while the spec is not loaded yet (item registration and data reloads run
 * before SERVER configs are read).
 */
public final class SystemConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, ABYSSAL_TOO, RECHARGE_FIRST, AMBUSH_QUESTS, BUFF_ONLY_IN_DUNGEON, HUD_HINT_ON_ENTRY, SHARE_PROGRESS;
    public static final ModConfigSpec.DoubleValue QUEST_CHANCE, MINION_STRENGTH;
    public static final ModConfigSpec.IntValue STAT_WEIGHT, SHADOW_CORE_WEIGHT, EXTRACT_WINDOW_TICKS, EXTRACT_RADIUS, MAX_MINIONS, MINION_MINUTES, MAX_STAT_POINTS, SHARE_RADIUS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("system");
        ENABLED = b.comment("The System: the holographic window, the floor quests, the stat rewards and the Shadow Core. false = none of it exists (a held core still opens no extraction).")
                .define("enabled", true);
        ABYSSAL_TOO = b.comment("IGNORED since 1.58.0 (kept so that old config files load): the System now covers every player in the dungeon, on the Holy and on the Abyssal road alike.")
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
        BUFF_ONLY_IN_DUNGEON = b.comment("The permanent stat points (STR, AGI, VIT, INT, SENSE) give their bonus only while the player is inside the dungeon dimensions: the Mega-Dungeon and the Hub (Sanctuary). "
                        + "The points are never lost, only dormant outside. false = the bonus works everywhere, as it did before 1.58.0.")
                .define("buffOnlyInDungeon", true);
        HUD_HINT_ON_ENTRY = b.comment("Tell a player once, the first time they are in the dungeon, how to move the System window (the keys of the 'Multiverse: System' controls). false = never.")
                .define("hudHintOnEntry", true);
        SHARE_PROGRESS = b.comment("A monster slain by one player counts for the 'defeat N monsters' quest of every party member (the players of the same sector, later the FTB Team) on that floor. "
                        + "Quests that judge a player's own conduct (no damage, speed, rooms) always stay personal. false = only the killer is credited.")
                .define("shareProgress", true);
        SHARE_RADIUS = b.comment("How close (blocks) a party member must be to the kill to be credited by shareProgress. 0 = anywhere on the floor.")
                .defineInRange("shareRadius", 64, 0, 512);
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

    /** Convenience: does the stat buff work only inside the dungeon dimensions? */
    public static boolean buffOnlyInDungeon() { return get(BUFF_ONLY_IN_DUNGEON); }
}
