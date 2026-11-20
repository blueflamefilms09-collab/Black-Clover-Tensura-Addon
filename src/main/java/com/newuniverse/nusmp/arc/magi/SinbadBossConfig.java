package com.newuniverse.nusmp.arc.magi;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the settings of the Sinbad boss, {@code nusmp-sinbad-boss-server.toml}, section {@code [sinbad_boss]}. Both change gameplay, so they are SERVER settings (rule 9). The strength of the
 * fight is the arcs' own (tier 3 buff row, power curve, balance law); there is no scaling setting of his own. Every value is a plain {@code define}/{@code defineInRange} and every reader
 * goes through {@link #get}, which answers the default while the spec is not loaded yet.
 */
public final class SinbadBossConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue GLOBAL_CAP;

    public static final int DEFAULT_GLOBAL_CAP = 2;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("sinbad_boss");
        ENABLED = b.comment("The Sinbad boss may be summoned (command, spawn egg, arc summon). False: he does not spawn and everything else stays as it was.")
                .define("enabled", true);
        GLOBAL_CAP = b.comment("How many Sinbad bosses may stand in the whole server at once (a hard cap so it cannot lag).")
                .defineInRange("globalCap", DEFAULT_GLOBAL_CAP, 1, 4);
        b.pop();
        SPEC = b.build();
    }

    private SinbadBossConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static int globalCap() { return get(GLOBAL_CAP); }
}
