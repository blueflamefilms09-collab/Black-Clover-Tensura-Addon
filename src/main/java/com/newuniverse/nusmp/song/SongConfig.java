package com.newuniverse.nusmp.song;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: every number a server owner may want to tune for Song Magic (how loud counts as singing, how often a pulse fires, how far it
 * reaches, whether the key stands in for a missing microphone) lives here as plain config values, so nothing is hard-wired into the
 * voice or spell code. File: nusmp-song-server.toml (register {@link #SPEC} as a SERVER config).
 */
public final class SongConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue GATE, CEILING, KEY_LEVEL, RADIUS, CRESCENDO_LOUD, CRESCENDO_RADIUS_MULT, PULSE_COST_MULT;
    public static final ModConfigSpec.IntValue HOLD_MS, PULSE_INTERVAL, PULSE_CAP_PER_LEVEL, CRESCENDO_TICKS, CRESCENDO_WINDOW;
    public static final ModConfigSpec.BooleanValue WHISPER_COUNTS, KEY_FALLBACK;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("song");
        GATE = b.comment("Noise gate: a microphone frame quieter than this RMS (0..1) counts as silence.")
                .defineInRange("gate", 0.08, 0.0, 0.9);
        CEILING = b.comment("The RMS that counts as full voice (level 1.0). Speech and singing rarely pass about 0.35.")
                .defineInRange("ceiling", 0.35, 0.1, 1.0);
        HOLD_MS = b.comment("How long (milliseconds) the singing level is held after the last loud frame, so pauses between words do not stop the song.")
                .defineInRange("holdMs", 400, 0, 3000);
        WHISPER_COUNTS = b.comment("Whispering in Simple Voice Chat counts as singing. false = only normal-volume speech does.")
                .define("whisperCounts", false);
        KEY_FALLBACK = b.comment("Without Simple Voice Chat installed on the server, the Sing key (hold) gives a steady singing level.")
                .define("keyFallback", true);
        KEY_LEVEL = b.comment("The singing level (0..1) the fallback key gives.")
                .defineInRange("keyLevel", 0.6, 0.1, 1.0);
        PULSE_INTERVAL = b.comment("Ticks between two pulses of a song mode for one singer (minimum 20).")
                .defineInRange("pulseInterval", 40, 20, 400);
        RADIUS = b.comment("Radius in blocks of a pulse.")
                .defineInRange("radius", 10.0, 2.0, 32.0);
        PULSE_CAP_PER_LEVEL = b.comment("Most pulses a single dimension may run in one tick, so a crowd of singers cannot spike the tick.")
                .defineInRange("pulseCapPerLevel", 6, 1, 64);
        PULSE_COST_MULT = b.comment("Multiplies the magicule cost of every pulse (0 = free).")
                .defineInRange("pulseCostMult", 1.0, 0.0, 10.0);
        CRESCENDO_LOUD = b.comment("Singing level (0..1) that counts as loud for Crescendo.")
                .defineInRange("crescendoLoud", 0.6, 0.2, 1.0);
        CRESCENDO_TICKS = b.comment("Ticks of loud singing needed to charge Crescendo (60 = 3 seconds).")
                .defineInRange("crescendoTicks", 60, 20, 400);
        CRESCENDO_WINDOW = b.comment("Ticks after casting Crescendo during which you may charge and release it.")
                .defineInRange("crescendoWindow", 400, 100, 1200);
        CRESCENDO_RADIUS_MULT = b.comment("The Crescendo burst reaches this many times the pulse radius.")
                .defineInRange("crescendoRadiusMult", 1.6, 1.0, 4.0);
        b.pop();
        SPEC = b.build();
    }

    private SongConfig() {}

    public static float gate() { return get(GATE).floatValue(); }
    public static float ceiling() { return get(CEILING).floatValue(); }
    public static long holdMs() { return get(HOLD_MS); }
    public static boolean whisperCounts() { return get(WHISPER_COUNTS); }
    public static boolean keyFallback() { return get(KEY_FALLBACK); }
    public static float keyLevel() { return get(KEY_LEVEL).floatValue(); }
    public static int pulseInterval() { return Math.max(20, get(PULSE_INTERVAL)); }
    public static double radius() { return get(RADIUS); }
    public static int pulseCap() { return get(PULSE_CAP_PER_LEVEL); }
    public static double costMult() { return get(PULSE_COST_MULT); }
    public static float crescendoLoud() { return get(CRESCENDO_LOUD).floatValue(); }
    public static int crescendoTicks() { return get(CRESCENDO_TICKS); }
    public static int crescendoWindow() { return get(CRESCENDO_WINDOW); }
    public static double crescendoRadiusMult() { return get(CRESCENDO_RADIUS_MULT); }

    /** Reads a value; before the config is loaded (tests, early startup) the default stands in. */
    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
