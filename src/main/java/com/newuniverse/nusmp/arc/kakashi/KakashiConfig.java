package com.newuniverse.nusmp.arc.kakashi;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the settings of the Kakashi Hatake boss, {@code nusmp-kakashi-boss-server.toml}, section {@code [kakashi_boss]}. All of them change gameplay, so they are SERVER settings (rule 9).
 * Every value is a plain {@code define} / {@code defineInRange} and every reader goes through {@link #get}, which answers the default while the spec is not loaded yet. The three "technique"
 * switches (Kamui, Susanoo, Sharingan copy) turn the strongest parts of his kit off one by one, so a server that finds him too hard can take them away without disabling him.
 */
public final class KakashiConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, KAMUI, SUSANOO, COPY, SPEAK, CANON_KIT;
    public static final ModConfigSpec.DoubleValue HEALTH_MULTIPLIER, DAMAGE_MULTIPLIER, MAX_HIT_PERCENT;
    public static final ModConfigSpec.IntValue GLOBAL_CAP;

    public static final double DEFAULT_MAX_HIT = 0.30;
    public static final int DEFAULT_GLOBAL_CAP = 2;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("kakashi_boss");
        ENABLED = b.comment("The Kakashi Hatake boss may be summoned (command, spawn egg, arc summon). False: he does not spawn and everything else stays as it was.")
                .define("enabled", true);
        HEALTH_MULTIPLIER = b.comment("Multiplies his health (28,000 base).")
                .defineInRange("healthMultiplier", 1.0, 0.1, 50.0);
        DAMAGE_MULTIPLIER = b.comment("Multiplies his damage (one hit on a player still ends at maxHitPercent of their health).")
                .defineInRange("damageMultiplier", 1.0, 0.1, 20.0);
        MAX_HIT_PERCENT = b.comment("One hit of his takes at most this share of a player's maximum health (0.30 = 30%): he cannot one-shot through the mod's own balance law.")
                .defineInRange("maxHitPercent", DEFAULT_MAX_HIT, 0.05, 0.9);
        KAMUI = b.comment("Kamui: from the Mangekyo phase he can phase through a blow (intangibility, only while he is not attacking, once per 4.5 s). False: he never phases; the Kamui attacks stay.")
                .define("kamui", true);
        SUSANOO = b.comment("The Perfect Susanoo of the last phase (a shield of 7% of his health and sword slashes). False: the phase has Purple Lightning only.")
                .define("susanoo", true);
        COPY = b.comment("Sharingan Copy: he throws back a weaker copy of the strongest hit a player landed on him. False: the move is never chosen.")
                .define("copy", true);
        SPEAK = b.comment("He speaks (chat lines, and the recorded voice lines once their files are added). False: silent; the fight is unchanged.")
                .define("speak", true);
        CANON_KIT = b.comment("The canon kit pass (docs/bosses/kakashi.md): the fourteen extra moves (Lightning Cutter, Lightning Clone, the Purple Electricity modes, Distant Warp, Earth Fang, Eight Gates, "
                + "Kubikiribocho, the Chakra Sabre, the kunai wire, Flying Thunder God, Great Fireball, the Six Paths awakening), the observe-first and counter behaviour, the Sharingan strain and the "
                + "charge limits. False: exactly the first fifteen moves and the behaviour before the pass.").define("canonKit", true);
        GLOBAL_CAP = b.comment("How many Kakashi bosses may stand in the whole server at once (a hard cap so it cannot lag).")
                .defineInRange("globalCap", DEFAULT_GLOBAL_CAP, 1, 4);
        b.pop();
        SPEC = b.build();
    }

    private KakashiConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static boolean kamui() { return get(KAMUI); }

    public static boolean susanoo() { return get(SUSANOO); }

    public static boolean copy() { return get(COPY); }

    public static boolean speak() { return get(SPEAK); }

    public static boolean canonKit() { return get(CANON_KIT); }

    public static double maxHitPercent() { return get(MAX_HIT_PERCENT); }

    public static double healthMultiplier() { return get(HEALTH_MULTIPLIER); }

    public static double damageMultiplier() { return get(DAMAGE_MULTIPLIER); }

    public static int globalCap() { return get(GLOBAL_CAP); }
}
