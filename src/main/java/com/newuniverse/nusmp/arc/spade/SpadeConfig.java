package com.newuniverse.nusmp.arc.spade;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the owner and the server operators must be able to tune how hard the two bosses of the Spade Kingdom Raid lean on the players without editing code:
 * how long a forced kneel holds, whether the air really goes flat (no jumping, no gliding) or only slows, how forgiving the Bow's crouch check is, whether
 * Morris re-forms his body at all and how much it takes off, and a damage factor per boss. {@code nusmp-spade-server.toml}, section {@code [spade]}. Its own
 * file (not more keys in the arc config) because the two bosses ship as a pair and a server that finds the kneel too harsh changes one key here and
 * nothing else. Every value is a plain {@code define} / {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server
 * is still loading) and every reader goes through {@link #get}, which answers the default while the spec is not loaded yet. The Balance Law's three-second
 * cap on a hard control still holds whatever is set here.
 */
public final class SpadeConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue FLATTENED_AIR, TUNING;
    public static final ModConfigSpec.DoubleValue KNEEL_SECONDS, TUNING_CUT, MORRIS_DAMAGE, LUCIFERO_DAMAGE;
    public static final ModConfigSpec.IntValue BOW_GRACE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("spade");
        FLATTENED_AIR = b.comment("Lucifero's Flatten the Air takes jumping and gliding away for a few seconds (the effect Flattened Air). False: it only drags the air down, nobody loses a jump.")
                .define("flattenedAir", true);
        KNEEL_SECONDS = b.comment("Seconds a foe is made to kneel by Lucifero (the Bow that was not obeyed, Kneel Before the King). The Balance Law's cap of 3 seconds on a hard control still applies.")
                .defineInRange("kneelSeconds", 2.5, 0.5, 3.0);
        BOW_GRACE = b.comment("Ticks of lag allowed in the Bow's crouch check: crouching up to this many ticks before the weight falls still counts as bowing.")
                .defineInRange("bowGraceTicks", BowRules.DEFAULT_GRACE, 0, 20);
        TUNING = b.comment("Morris's Structural Tuning: he re-forms his body against the kind of hit that hurts him most (and is open to the others). False: he never tunes.")
                .define("structuralTuning", true);
        TUNING_CUT = b.comment("The base share of a hit that a tuning takes off the tuned kind (the cut grows a little with his power and never passes 40 percent).")
                .defineInRange("tuningCut", 0.30, 0.05, StructuralTuning.MAX_REDUCTION);
        MORRIS_DAMAGE = b.comment("Factor on everything Morris Libardirt deals, on top of the framework's own damage law (1.0 = as designed).")
                .defineInRange("morrisDamage", 1.0, 0.25, 3.0);
        LUCIFERO_DAMAGE = b.comment("Factor on everything Lucifero deals, on top of the framework's own damage law (1.0 = as designed).")
                .defineInRange("luciferoDamage", 1.0, 0.25, 3.0);
        b.pop();
        SPEC = b.build();
    }

    private SpadeConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean flattenedAir() { return get(FLATTENED_AIR); }

    public static double kneelSeconds() { return get(KNEEL_SECONDS); }

    public static int bowGrace() { return get(BOW_GRACE); }

    public static boolean tuning() { return get(TUNING); }

    public static double tuningCut() { return get(TUNING_CUT); }

    public static float morrisDamage() { return get(MORRIS_DAMAGE).floatValue(); }

    public static float luciferoDamage() { return get(LUCIFERO_DAMAGE).floatValue(); }
}
