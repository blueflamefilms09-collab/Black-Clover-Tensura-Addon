package com.newuniverse.nusmp.client.gui.bossbar;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client options of the animated boss bar: config/nusmp-bossbar-client.toml. {@code bossbar.animations} off hands every boss bar back to
 * vanilla (nothing is cancelled, nothing is drawn by the mod); {@code bossbar.shake} off keeps the animated bar but without the shake,
 * glitch, flash and shatter (for people who dislike motion); {@code bossbar.percent} shows the health percentage above the bar.
 */
public final class BossBarOptions {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ANIMATIONS;
    public static final ModConfigSpec.BooleanValue SHAKE;
    public static final ModConfigSpec.BooleanValue PERCENT;
    // 1.106.0: the custom boss bar (CustomBarOverlay): bossbar.custom, bossbar.scale, bossbar.showPhaseMarkers, bossbar.showHp, bossbar.portrait, bossbar.showDps
    public static final ModConfigSpec.BooleanValue CUSTOM, SHOW_PHASE_MARKERS, SHOW_HP, PORTRAIT, SHOW_DPS;
    public static final ModConfigSpec.DoubleValue SCALE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("bossbar");
        ANIMATIONS = b.comment("The animated boss bar of the mod's bosses (scrolling glow, flames, shine, phase gems). Off = every boss bar is drawn by vanilla.")
                .define("animations", true);
        SHAKE = b.comment("Shake, glitch slices and white flash when a boss loses a lot of health, and the shatter when it changes phase.")
                .define("shake", true);
        PERCENT = b.comment("Show the boss's health percentage at the right end of the bar's name line.")
                .define("percent", true);
        CUSTOM = b.comment("The custom boss bars (a frame, emblem and colours of their own per boss family, phase notches, shield, lair action warning, wind-up bar). Off = the animated bar above, or vanilla's.")
                .define("custom", true);
        SCALE = b.comment("Size of the custom boss bars, 0.5 to 2.0 (on top of the GUI Size screen).").defineInRange("scale", 1.0, 0.5, 2.0);
        SHOW_PHASE_MARKERS = b.comment("Draw the phase notches on the custom bar and the phase number beside the name.").define("showPhaseMarkers", true);
        SHOW_HP = b.comment("Also show the boss's health as a number (current / maximum) next to the percentage. Off: the percentage only.").define("showHp", false);
        PORTRAIT = b.comment("Draw the boss itself (its head and shoulders) in the round socket of the custom bar while it is near enough to be seen; off = the emblem only.").define("portrait", true);
        SHOW_DPS = b.comment("Show how much of the boss's health the party has taken off per second under the bar.").define("showDps", false);
        b.pop();
        SPEC = b.build();
    }

    private BossBarOptions() {}

    public static boolean animations() { return get(ANIMATIONS, true); }
    public static boolean shake() { return get(SHAKE, true); }
    public static boolean percent() { return get(PERCENT, true); }

    public static boolean custom() { return get(CUSTOM, true); }
    public static boolean showPhaseMarkers() { return get(SHOW_PHASE_MARKERS, true); }
    public static boolean showHp() { return get(SHOW_HP, false); }
    public static boolean portrait() { return get(PORTRAIT, true); }
    public static boolean showDps() { return get(SHOW_DPS, false); }

    public static float scale() {
        try { return (float) (double) SCALE.get(); } catch (IllegalStateException notLoaded) { return 1f; }
    }

    private static boolean get(ModConfigSpec.BooleanValue v, boolean fallback) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return fallback; }
    }
}
