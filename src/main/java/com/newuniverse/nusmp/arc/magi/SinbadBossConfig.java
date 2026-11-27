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
    public static final ModConfigSpec.IntValue GLOBAL_CAP, SCREAM_SLOW_AMP;
    public static final ModConfigSpec.BooleanValue PHASES, MAGI_VFX;
    public static final ModConfigSpec.DoubleValue HEALTH_FACTOR;

    public static final int DEFAULT_GLOBAL_CAP = 2;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("sinbad_boss");
        ENABLED = b.comment("The Sinbad boss may be summoned (command, spawn egg, arc summon). False: he does not spawn and everything else stays as it was.")
                .define("enabled", true);
        GLOBAL_CAP = b.comment("How many Sinbad bosses may stand in the whole server at once (a hard cap so it cannot lag).")
                .defineInRange("globalCap", DEFAULT_GLOBAL_CAP, 1, 4);
        PHASES = b.comment("4.105.0: Sinbad fights in three phases: King of Sindria (power 20-40), Djinn Master (from power 60: a second health bar, shorter equips, a wider switch window), Seven Seas (the Gates, as before).",
                        "false: the fight of 4.104.x (one bar until the Gates, the health of the table).")
                .define("phases", true);
        HEALTH_FACTOR = b.comment("4.105.0: Sinbad's health is the table's 3200 times this while `phases` is on (the second bar and the Gates bar come on top). 1.0 keeps 3200.")
                .defineInRange("healthFactor", 2.5, 1.0, 20.0);
        MAGI_VFX = b.comment("4.105.0: Sinbad's Djinn Equip, Extreme Magic, Rukh flights, aura and phase breaks are drawn with the Magi layer (golden vessel sigil, Magoi, Rukh). false: the old magic circle only.")
                .define("magiVfx", true);
        SCREAM_SLOW_AMP = b.comment("5.3.2: the Slowness level of Zepar's Command Scream on anyone who looks at him (2 s). 4 = slowed by three quarters, never a full stun (brief). 9 = the old full root.")
                .defineInRange("screamSlowAmp", 4, 0, 9);
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

    public static boolean phases() { return get(PHASES); }

    public static double healthFactor() { return phases() ? get(HEALTH_FACTOR) : 1.0; }

    public static int screamSlowAmp() { return get(SCREAM_SLOW_AMP); }

    public static boolean magiVfx() { return get(MAGI_VFX); }
}
