package com.newuniverse.nusmp.bossdrops;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the owner and the server operators tune the boss drops without code. SERVER config {@code nusmp-bossdrops-server.toml}, section {@code [bossdrops]}. Every reader goes through {@link #get},
 * which answers the default while the spec is not loaded yet. The drop tables themselves are data (docs/bosses/boss_drops.md).
 */
public final class BossDropConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue CHANCE_SCALE;
    public static final ModConfigSpec.IntValue STACK_CAP;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("bossdrops");
        ENABLED = b.comment("The data-driven boss drops (trophies, coins, Lucifero's armour). False: bosses drop what they dropped before this feature (the Charter Grimoire and the loot modifier are not affected either way).")
                .define("enabled", true);
        CHANCE_SCALE = b.comment("Factor on every chance of every boss drop table (1.0 = as the data files say; a chance never goes above 100 percent).")
                .defineInRange("chanceScale", 1.0, 0.0, 5.0);
        STACK_CAP = b.comment("The most stacks one kill pays, on top of each table's own cap.")
                .defineInRange("maxStacksPerKill", 12, 1, BossDropTable.HARD_MAX_STACKS);
        b.pop();
        SPEC = b.build();
    }

    private BossDropConfig() {}

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static double chanceScale() { return get(CHANCE_SCALE); }

    public static int stackCap() { return get(STACK_CAP); }
}
