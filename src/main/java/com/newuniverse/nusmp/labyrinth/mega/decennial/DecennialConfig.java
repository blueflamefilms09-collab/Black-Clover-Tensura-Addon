package com.newuniverse.nusmp.labyrinth.mega.decennial;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the Decennial bosses, {@code world/serverconfig/nusmp-labyrinth-mega-decennial-server.toml}, section {@code [decennial]}. Its own spec (the {@code GuardConfig} pattern) so a server owner
 * who finds the 200 bosses too much, or the halls too heavy to build, switches one part off in one small file, and nothing else in the dungeon changes: {@code enabled = false} gives the Guardian of 1.98.0
 * on every boss floor, {@code arenas = false} keeps the bosses but leaves the old hall, {@code seal = false} never closes the entry, {@code bossRelic = false} pays no relic. Only plain
 * {@code define} / {@code defineInRange}, and every reader goes through {@link #get}, which answers the default while the spec is not loaded yet (the registries run before SERVER configs are read).
 */
public final class DecennialConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, ARENAS, SEAL, BOSS_RELIC, ELEMENTS;
    public static final ModConfigSpec.IntValue SEAL_MAX_MINUTES, WIPE_SECONDS, ARENA_BLOCKS_PER_TICK;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("decennial");
        ENABLED = b.comment("The 200 Decennial bosses (100 Holy, 100 Evil, one per level of ten floors). false = the boss floors keep the Guardian and the D&D profile of 1.98.0.")
                .define("enabled", true);
        ARENAS = b.comment("Build the boss's thematic hall in place of the Guardian's hall when a boss floor is generated (36 hardcoded halls). false = the old hall stays.")
                .define("arenas", true);
        ELEMENTS = b.comment("The interactive elements of the halls (tombstones, pillars, root nodes ...) may be broken by the party. false = the halls are as unbreakable as the rest of the dungeon.")
                .define("elements", true);
        SEAL = b.comment("The entry of the hall is closed (iron bars) while the fight lasts, and opens when the boss falls, the party wipes or the seal has held too long.")
                .define("seal", true);
        SEAL_MAX_MINUTES = b.comment("The most minutes the entry seal may hold: after this it opens whatever happens (the anti soft-lock).")
                .defineInRange("sealMaxMinutes", 25, 2, 240);
        WIPE_SECONDS = b.comment("Seconds with no living survival player inside the hall before the fight counts as lost: the hall resets, the boss heals and goes home.")
                .defineInRange("wipeSeconds", 12, 4, 120);
        BOSS_RELIC = b.comment("The boss pays one relic or weapon of its level's grade once per floor, to the killer (an explicit exception to the Crucible being the only source of Unique and higher relics; docs/labyrinth/decennial_bosses.md). false = no relic.")
                .define("bossRelic", true);
        ARENA_BLOCKS_PER_TICK = b.comment("How many blocks of a hall are placed per server tick when a boss floor is generated (the hall is built in steps so no tick stalls).")
                .defineInRange("arenaBlocksPerTick", 6000, 500, 60000);
        b.pop();
        SPEC = b.build();
    }

    private DecennialConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
