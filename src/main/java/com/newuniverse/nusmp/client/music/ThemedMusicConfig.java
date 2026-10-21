package com.newuniverse.nusmp.client.music;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: how the themed dungeon music sounds is a taste, not a rule of the game, so it is a CLIENT config ({@code nusmp-music-client.toml}, section {@code [music]}):
 * {@code themed} is the master switch (off = the vanilla music plays as before and none of this runs), {@code themedVolume} scales the themed music on top of the vanilla
 * music volume slider (the tracks play in the MUSIC sound category, so the slider already applies; this is a second, mod-only knob), {@code fadeSeconds} and
 * {@code cooldownSeconds} shape the cross-fade ({@link com.newuniverse.nusmp.audio.music.MusicDirector.Params}), {@code stopVanilla} says whether the vanilla music is silenced while
 * the themed music plays, and {@code showTitle} shows the name of a track when it starts. Plain {@code define} / {@code defineInRange} only, and every reader answers its default
 * while the config is not loaded yet (the first frames, a unit test), like the other client option classes of the mod.
 */
public final class ThemedMusicConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue THEMED, STOP_VANILLA, SHOW_TITLE;
    public static final ModConfigSpec.DoubleValue VOLUME, FADE_SECONDS, COOLDOWN_SECONDS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("music");
        THEMED = b.comment("Play the themed dungeon music (a loop per theme family, a fight loop, boss, safe-zone, hub and rift loops, cross-faded). false = only the vanilla music.")
                .define("themed", true);
        VOLUME = b.comment("Volume of the themed music on top of the Music slider of the sound settings (0 to 1).")
                .defineInRange("themedVolume", 0.8, 0.0, 1.0);
        FADE_SECONDS = b.comment("How many seconds a cross-fade between two tracks lasts (0.5 to 20).")
                .defineInRange("fadeSeconds", 4.0, 0.5, 20.0);
        COOLDOWN_SECONDS = b.comment("How many seconds after a switch the music will not change to a calmer or equal track (a fight starting is never held back). 0 to 120.")
                .defineInRange("cooldownSeconds", 12.0, 0.0, 120.0);
        STOP_VANILLA = b.comment("Silence the vanilla music while the themed music plays.")
                .define("stopVanilla", true);
        SHOW_TITLE = b.comment("Show the name of a track above the hotbar when it starts.")
                .define("showTitle", false);
        b.pop();
        SPEC = b.build();
    }

    private ThemedMusicConfig() {}

    public static boolean themed() { return bool(THEMED, true); }

    public static boolean stopVanilla() { return bool(STOP_VANILLA, true); }

    public static boolean showTitle() { return bool(SHOW_TITLE, false); }

    public static float volume() { return (float) num(VOLUME, 0.8, 0.0, 1.0); }

    public static double fadeSeconds() { return num(FADE_SECONDS, 4.0, 0.5, 20.0); }

    public static double cooldownSeconds() { return num(COOLDOWN_SECONDS, 12.0, 0.0, 120.0); }

    private static boolean bool(ModConfigSpec.BooleanValue v, boolean dflt) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return dflt; }
    }

    private static double num(ModConfigSpec.DoubleValue v, double dflt, double lo, double hi) {
        try { return Math.max(lo, Math.min(hi, v.get())); } catch (IllegalStateException notLoaded) { return dflt; }
    }
}
