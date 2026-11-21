package com.newuniverse.nusmp.dragonballs.voice;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config of the spoken Dragon Ball words: {@code serverconfig/nusmp-dbvoice-server.toml}. Every setting changes gameplay or what the server does with audio, so every one is a SERVER setting.
 * {@code voice.enabled = false} is the old behaviour exactly (typed chat only; nothing listens, nothing loads). Reads answer the default while the config is not loaded (a self-test, a unit test).
 */
public final class DbVoiceConfig {
    private DbVoiceConfig() {}

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED, WHISPER, TOLERANCE, GRAMMAR;
    private static final ModConfigSpec.IntValue MAX_LISTENERS;
    private static final ModConfigSpec.ConfigValue<String> MODEL_PATH, LIB_PATH;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("voice");
        ENABLED = b.comment("Spoken Dragon Ball words through Simple Voice Chat (and the /multiverse dragonballs say relay). false = typed chat only, exactly as before; nothing listens and nothing is loaded.").define("enabled", true);
        WHISPER = b.comment("A whisper in Simple Voice Chat counts as speech (you can say the words quietly).").define("whisperCounts", true);
        MAX_LISTENERS = b.comment("Most players listened to at the same moment (a player is listened to only at a complete, unlocked set of balls, or as the summoner of a running ritual). Others use chat.")
                .defineInRange("maxListeners", 4, 0, 16);
        MODEL_PATH = b.comment("Folder of the unpacked Vosk model (e.g. vosk-model-small-en-us from https://alphacephei.com/vosk/models). Relative to the server folder.").define("modelPath", "config/nusmp/vosk-model");
        LIB_PATH = b.comment("Folder where the server admin puts the vosk-java jar and its jna jar if they are not already on the server's class path. Relative to the server folder.").define("libraryPath", "config/nusmp/vosk-lib");
        TOLERANCE = b.comment("Fuzzy matching of recognised words (sounds-alike, one or two wrong letters, split words), so a recogniser that mangles the chant still works. false = the recognised words must be exactly the configured ones.")
                .define("matchTolerance", true);
        GRAMMAR = b.comment("Give the recogniser only the ritual and wish words (accurate and cheap). Vosk drops words its model does not know (the Namekian chant is not in the small English model); false = free recognition, "
                + "which hears more but needs the fuzzy matching to turn 'tack are a doppo' back into the chant.").define("useGrammar", true);
        b.pop();
        SPEC = b.build();
    }

    public static boolean enabled() { return get(ENABLED); }

    public static boolean whisperCounts() { return get(WHISPER); }

    public static int maxListeners() { return get(MAX_LISTENERS); }

    public static String modelPath() { return get(MODEL_PATH); }

    public static String libraryPath() { return get(LIB_PATH); }

    public static boolean matchTolerance() { return get(TOLERANCE); }

    public static boolean useGrammar() { return get(GRAMMAR); }

    /** Reads a value; before the config is loaded (tests, early start-up) the default stands in. */
    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
