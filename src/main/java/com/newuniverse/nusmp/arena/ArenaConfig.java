package com.newuniverse.nusmp.arena;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the owner must be able to give the old free spawning of Lucius back, and to set how long the arena rests after he falls, without
 * touching code. Own spec file ({@code nusmp-lucius-arena-server.toml}, SERVER); the lead registers it with
 * {@code modContainer.registerConfig(ModConfig.Type.SERVER, ArenaConfig.SPEC, "nusmp-lucius-arena-server.toml")}. Plain
 * {@code define}/{@code defineInRange} only. Reads fall back to the defaults before the config is loaded, so the gate holds from the first tick.
 */
public final class ArenaConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ARENA_ONLY;
    public static final ModConfigSpec.IntValue COOLDOWN_MINUTES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("luciusArena");
        ARENA_ONLY = b.comment("Lucius Zogratis (and his clones) can exist only in the dimension nusmp:zogratis_arena. A spawn egg, /summon, a structure or a portal anywhere else is refused",
                        "with a message; operators may still /summon him inside the arena. false = the old behaviour (he can be spawned anywhere); the arena and the Hourglass of the Final Hour still work.")
                .define("luciusArenaOnly", true);
        COOLDOWN_MINUTES = b.comment("Minutes the arena rests after Lucius is defeated: no new boss spawns until it is over.")
                .defineInRange("luciusArenaCooldownMinutes", 20, 1, 1440);
        b.pop();
        SPEC = b.build();
    }

    private ArenaConfig() {}

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean arenaOnly() { return get(ARENA_ONLY); }

    public static int cooldownMinutes() { return get(COOLDOWN_MINUTES); }

    public static long cooldownTicks() { return cooldownMinutes() * 1200L; }
}
