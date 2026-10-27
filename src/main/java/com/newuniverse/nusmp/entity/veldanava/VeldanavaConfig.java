package com.newuniverse.nusmp.entity.veldanava;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config of Veldanava, the Creator God: world/serverconfig/nusmp-veldanava-server.toml. These change the fight (gameplay), so they are SERVER settings; how
 * he looks is not configurable here. Each call reads the default when the config is not loaded yet (a unit test, the first tick of a server).
 */
public final class VeldanavaConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, SOUL_SEIZING, CLASSIC;
    public static final ModConfigSpec.DoubleValue BOSS_HEALTH, DAMAGE_SCALE, HEALTH_TOTAL, REGEN_PERCENT;
    public static final ModConfigSpec.IntValue ORB_SECONDS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("veldanava");
        ENABLED = b.comment("false = /multiverse boss veldanava and his spawn egg do nothing.")
                .define("enabled", true);
        BOSS_HEALTH = b.comment("Classic mode only (classicMode = true): health of each of his two health bars (the dragon's, then the Creator God's). The per-second damage cap of the boss budget applies on top.")
                .defineInRange("bossHealth", 4000.0, 100.0, 1.0E6);
        CLASSIC = b.comment("true = the Veldanava of 3.11.0 - 3.29.0: 4000 health, the old stats, no immunities, no Ultraspeed Regeneration, the plain bar and only his eleven old moves. false (default) = the anime Veldanava of 3.30.0.")
                .define("classicMode", false);
        HEALTH_TOTAL = b.comment("Health of each of his two health bars in the anime mode (default 5,000,000, as in the anime). The per-second damage cap of the boss budget (6% of it) applies on top.")
                .defineInRange("healthTotal", 5_000_000.0, 100.0, 1.0E9);
        REGEN_PERCENT = b.comment("Ultraspeed Regeneration (anime mode): percent of his maximum health he heals each second while he fights, until a hit of the counter-element (nusmp:veldanava_counter damage types) stops it for 6 seconds. 0 = off.")
                .defineInRange("regenPercentPerSecond", 0.1, 0.0, 5.0);
        DAMAGE_SCALE = b.comment("Multiplier of everything he deals to players (1 = as designed).")
                .defineInRange("damageScale", 1.0, 0.0, 10.0);
        SOUL_SEIZING = b.comment("Anti-Totem: a player who would die to the Creator God or the Dark Avatar is put in spectator mode until the fight is over instead of dying (a Totem of Undying cannot save them). false = they take the blow.")
                .define("soulSeizing", true);
        ORB_SECONDS = b.comment("How long the Will of the Creator waits for the fighters to choose their boon after he falls, in seconds, before it gives up its loot.")
                .defineInRange("willOfTheCreatorSeconds", 300, 30, 1800);
        b.pop();
        SPEC = b.build();
    }

    private VeldanavaConfig() {}

    public static boolean enabled() { return get(ENABLED); }

    public static boolean soulSeizing() { return get(SOUL_SEIZING); }

    /** True = the Veldanava of 3.11.0 - 3.29.0. */
    public static boolean classic() { return get(CLASSIC); }

    /** The health he is spawned with, for the mode the server chose. */
    public static double bossHealth() { return VeldanavaStats.health(classic(), get(BOSS_HEALTH), get(HEALTH_TOTAL)); }

    public static double regenPercent() { return get(REGEN_PERCENT); }

    public static float damageScale() { return get(DAMAGE_SCALE).floatValue(); }

    public static int orbSeconds() { return get(ORB_SECONDS); }

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
