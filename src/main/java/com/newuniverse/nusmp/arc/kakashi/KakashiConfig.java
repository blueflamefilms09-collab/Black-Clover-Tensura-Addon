package com.newuniverse.nusmp.arc.kakashi;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the settings of the Kakashi Hatake boss, {@code nusmp-kakashi-boss-server.toml}, section {@code [kakashi_boss]}. All of them change gameplay, so they are SERVER settings (rule 9).
 * Every value is a plain {@code define} / {@code defineInRange} and every reader goes through {@link #get}, which answers the default while the spec is not loaded yet. The three "technique"
 * switches (Kamui, Susanoo, Sharingan copy) turn the strongest parts of his kit off one by one, so a server that finds him too hard can take them away without disabling him.
 */
public final class KakashiConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, KAMUI, SUSANOO, COPY, SPEAK, CANON_KIT, MISS_STAGGER, EYE_RIP;
    public static final ModConfigSpec.DoubleValue HEALTH_MULTIPLIER, DAMAGE_MULTIPLIER, MAX_HIT_PERCENT, EYE_BACKLASH;
    public static final ModConfigSpec.IntValue GLOBAL_CAP, MISS_STAGGER_TICKS, EYE_TICKS, EYE_PULLS, EYE_CAP;

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
        MISS_STAGGER = b.comment("5.4.0: a Raikiri / Lightning Cutter dash that hits nobody staggers him (open to every blow, no Kamui) for missStaggerTicks. False: a miss costs him nothing, as before.")
                .define("missStagger", true);
        MISS_STAGGER_TICKS = b.comment("How long a missed Raikiri leaves him staggered (20 ticks = 1 s).")
                .defineInRange("missStaggerTicks", 36, 0, 100);
        EYE_RIP = b.comment("5.8.0: each time he enters a new phase (three times a fight) he is stunned for eyeWindowTicks and a player who runs up and right-clicks him can try the eye-rip mini-game; ripping out his eye gives the unique skill Sharingan. False: no windows, as before 5.8.0.")
                .define("eyeRip", true);
        EYE_TICKS = b.comment("How long the stun window of one eye-rip opportunity lasts (20 ticks = 1 s).").defineInRange("eyeWindowTicks", 160, 40, 600);
        EYE_PULLS = b.comment("How many clean pulls (a right click while the marker is in the zone) rip the eye out.").defineInRange("eyePulls", 3, 1, 6);
        EYE_BACKLASH = b.comment("The damage a player takes for a bad pull (the pulls start again). 0 = none.").defineInRange("eyeBacklash", 4.0, 0.0, 20.0);
        EYE_CAP = b.comment("The most players who may play the mini-game at once on one Kakashi.").defineInRange("eyeSessions", 4, 1, 8);
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

    public static boolean eyeRip() { return get(EYE_RIP); }

    public static int eyeWindowTicks() { return get(EYE_TICKS); }

    public static int eyePulls() { return get(EYE_PULLS); }

    public static double eyeBacklash() { return get(EYE_BACKLASH); }

    public static int eyeSessions() { return get(EYE_CAP); }

    public static boolean missStagger() { return get(MISS_STAGGER); }

    public static int missStaggerTicks() { return get(MISS_STAGGER_TICKS); }

    public static boolean canonKit() { return get(CANON_KIT); }

    public static double maxHitPercent() { return get(MAX_HIT_PERCENT); }

    public static double healthMultiplier() { return get(HEALTH_MULTIPLIER); }

    public static double damageMultiplier() { return get(DAMAGE_MULTIPLIER); }

    public static int globalCap() { return get(GLOBAL_CAP); }
}
