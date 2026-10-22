package com.newuniverse.nusmp.firecast;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the server owner decides whether the four Vermillion and Magna casting styles apply ({@code magic.fireCasting}, default true; false = every caster casts the Fire grimoire the old generic
 * way, nothing else changes) and how many of the things the styles put in the world may exist across the whole server at once. These change gameplay, so they are SERVER settings. File:
 * nusmp-firecast-server.toml (registered by {@link FireCastBoot}).
 */
public final class FireCastConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue FIRE_CASTING, SALAMANDER_MOODS, SALAMANDER_SOUNDS;
    public static final ModConfigSpec.IntValue MAX_BALLS, MAX_SPIRALS, MAX_SPIRITS, MAX_DRAGONS;
    public static final ModConfigSpec.DoubleValue CHARGE_COST_SCALE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("magic");
        FIRE_CASTING = b.comment("The casting styles of the Fire grimoire for the canon books of Magna (the Soul Bat swing), Mereoleona (punch combos), Fuegoleon (the Salamander) and Leopold (the spiral charge).",
                "false = every caster casts the generic way; the pages, their numbers and their cooldowns are the same either way.")
                .define("fireCasting", true);
        SALAMANDER_MOODS = b.comment("Fuegoleon's Salamander shows how it feels: poses (a curious tilt, a yawn, a flinch, a roar ...) and faces (alert, angry, sleepy, smug, roaring) chosen by the server from what happens.",
                "false = the Salamander only perches, flies and breathes, as before.")
                .define("salamanderMoods", true);
        SALAMANDER_SOUNDS = b.comment("The Salamander's creature sounds (trills, hisses, a roar ...), heard by players within 32 blocks, at most one every three seconds for a listener.",
                "false = no creature sounds, and the big form's summon keeps the plain Ravager roar it had before.")
                .define("salamanderSounds", true);
        b.pop();
        b.push("caps");
        MAX_BALLS = b.comment("Most Magna Swing balls alive across the whole server (one per caster, a few ticks each).").defineInRange("maxBalls", 32, 1, 256);
        MAX_SPIRALS = b.comment("Most Leopold charge spirals alive across the whole server (one per caster).").defineInRange("maxSpirals", 48, 1, 256);
        MAX_SPIRITS = b.comment("Most small Salamander spirits linked across the whole server (one per caster).").defineInRange("maxSpirits", 48, 1, 256);
        MAX_DRAGONS = b.comment("Most big Salamander dragons out across the whole server (one per caster, a few seconds each).").defineInRange("maxDragons", 6, 1, 64);
        CHARGE_COST_SCALE = b.comment("Multiplies the mana drawn every few ticks while Leopold's spiral winds past the plain cast (0 = free).").defineInRange("chargeCostScale", 1.0, 0.0, 5.0);
        b.pop();
        SPEC = b.build();
    }

    private FireCastConfig() {}

    /** Reads a value; before the config is loaded (tests, early startup) the default stands in. */
    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(FIRE_CASTING); }
    public static boolean moods() { return get(FIRE_CASTING) && get(SALAMANDER_MOODS); }
    public static boolean sounds() { return get(FIRE_CASTING) && get(SALAMANDER_SOUNDS); }
    public static int maxBalls() { return get(MAX_BALLS); }
    public static int maxSpirals() { return get(MAX_SPIRALS); }
    public static int maxSpirits() { return get(MAX_SPIRITS); }
    public static int maxDragons() { return get(MAX_DRAGONS); }
    public static double chargeCostScale() { return get(CHARGE_COST_SCALE); }
}
