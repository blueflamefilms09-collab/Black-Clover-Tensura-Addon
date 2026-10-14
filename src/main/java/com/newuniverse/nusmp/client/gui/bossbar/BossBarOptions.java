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

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("bossbar");
        ANIMATIONS = b.comment("The animated boss bar of the mod's bosses (scrolling glow, flames, shine, phase gems). Off = every boss bar is drawn by vanilla.")
                .define("animations", true);
        SHAKE = b.comment("Shake, glitch slices and white flash when a boss loses a lot of health, and the shatter when it changes phase.")
                .define("shake", true);
        PERCENT = b.comment("Show the boss's health percentage at the right end of the bar's name line.")
                .define("percent", true);
        b.pop();
        SPEC = b.build();
    }

    private BossBarOptions() {}

    public static boolean animations() { return get(ANIMATIONS, true); }
    public static boolean shake() { return get(SHAKE, true); }
    public static boolean percent() { return get(PERCENT, true); }

    private static boolean get(ModConfigSpec.BooleanValue v, boolean fallback) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return fallback; }
    }
}
