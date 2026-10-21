package com.newuniverse.nusmp.audio.voice;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: how often the server may speak to one player is a rule of the game on a server (a quest-heavy fight must not turn into a stream of lines), so it is a SERVER config
 * ({@code nusmp-voice-server.toml}, section {@code [voice]}): {@code maxPerSecond} is the most lines one player is sent in a second (0 = the server sends no spoken lines at all),
 * {@code lineCooldownSeconds} the time before the same line can be sent to the same player again, and {@code range} how far from the speaker a nearby player still hears it. Whether a
 * player wants to HEAR the lines is a taste and lives in the client config ({@code client/voice/VoiceClientConfig}). Every reader answers its default while the config is not loaded.
 */
public final class VoiceConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MAX_PER_SECOND;
    public static final ModConfigSpec.DoubleValue LINE_COOLDOWN_SECONDS, RANGE;

    public static final int DEFAULT_MAX_PER_SECOND = 2;
    public static final double DEFAULT_COOLDOWN = 8.0, DEFAULT_RANGE = 48.0, MAX_RANGE = 128.0;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("voice");
        MAX_PER_SECOND = b.comment("The most spoken lines the server sends one player in a second (0 = no spoken lines at all). The text of every message is shown either way.")
                .defineInRange("maxPerSecond", DEFAULT_MAX_PER_SECOND, 0, 10);
        LINE_COOLDOWN_SECONDS = b.comment("Seconds before the same spoken line is sent to the same player again (0 to 600).")
                .defineInRange("lineCooldownSeconds", DEFAULT_COOLDOWN, 0.0, 600.0);
        RANGE = b.comment("How far from the speaker (a boss, an NPC, a Djinn) a player still hears its line, in blocks (4 to 128).")
                .defineInRange("range", DEFAULT_RANGE, 4.0, MAX_RANGE);
        b.pop();
        SPEC = b.build();
    }

    private VoiceConfig() {}

    public static int maxPerSecond() {
        try { return Math.max(0, Math.min(10, MAX_PER_SECOND.get())); } catch (IllegalStateException notLoaded) { return DEFAULT_MAX_PER_SECOND; }
    }

    public static long lineCooldownTicks() {
        try { return Math.round(Math.max(0.0, Math.min(600.0, LINE_COOLDOWN_SECONDS.get())) * 20.0); } catch (IllegalStateException notLoaded) { return Math.round(DEFAULT_COOLDOWN * 20.0); }
    }

    public static double range() {
        try { return Math.max(4.0, Math.min(MAX_RANGE, RANGE.get())); } catch (IllegalStateException notLoaded) { return DEFAULT_RANGE; }
    }
}
