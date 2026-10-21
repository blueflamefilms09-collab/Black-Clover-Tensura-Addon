package com.newuniverse.nusmp.client.voice;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: whether a player wants to HEAR the spoken lines, and how loud, is a taste, not a rule of the game, so it is a CLIENT config ({@code nusmp-voice-client.toml}, section
 * {@code [voice]}): {@code enabled} is the master switch (off = no line is ever played; the text of every message is shown regardless) and {@code volume} scales the lines on top of the
 * Voice slider of the sound settings (the lines play in the VOICE category, so the slider already applies; this is a second, mod-only knob). Every reader answers its default while the
 * config is not loaded yet (the first frames, a unit test).
 */
public final class VoiceClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue VOLUME;

    public static final boolean DEFAULT_ENABLED = true;
    public static final double DEFAULT_VOLUME = 0.9;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("voice");
        ENABLED = b.comment("Play the spoken lines of the System, the bosses, the NPCs and the Djinn. false = silent (the text is still shown).")
                .define("enabled", DEFAULT_ENABLED);
        VOLUME = b.comment("Volume of the spoken lines on top of the Voice slider of the sound settings (0 to 1).")
                .defineInRange("volume", DEFAULT_VOLUME, 0.0, 1.0);
        b.pop();
        SPEC = b.build();
    }

    private VoiceClientConfig() {}

    public static boolean enabled() {
        try { return ENABLED.get(); } catch (IllegalStateException notLoaded) { return DEFAULT_ENABLED; }
    }

    public static float volume() {
        try { return (float) Math.max(0.0, Math.min(1.0, VOLUME.get())); } catch (IllegalStateException notLoaded) { return (float) DEFAULT_VOLUME; }
    }
}
