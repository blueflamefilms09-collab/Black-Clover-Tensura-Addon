package com.newuniverse.nusmp.entity.veldanava;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config of Veldanava, the Creator God: world/serverconfig/nusmp-veldanava-server.toml. These change the fight (gameplay), so they are SERVER settings; how
 * he looks is not configurable here. Each call reads the default when the config is not loaded yet (a unit test, the first tick of a server).
 */
public final class VeldanavaConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, SOUL_SEIZING;
    public static final ModConfigSpec.DoubleValue BOSS_HEALTH, DAMAGE_SCALE;
    public static final ModConfigSpec.IntValue ORB_SECONDS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("veldanava");
        ENABLED = b.comment("false = /multiverse boss veldanava and his spawn egg do nothing.")
                .define("enabled", true);
        BOSS_HEALTH = b.comment("Health of each of his two health bars (the dragon's, then the Creator God's). The per-second damage cap of the boss budget applies on top.")
                .defineInRange("bossHealth", 4000.0, 100.0, 1.0E6);
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

    public static double bossHealth() { return get(BOSS_HEALTH); }

    public static float damageScale() { return get(DAMAGE_SCALE).floatValue(); }

    public static int orbSeconds() { return get(ORB_SECONDS); }

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
