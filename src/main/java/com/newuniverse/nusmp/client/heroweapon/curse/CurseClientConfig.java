package com.newuniverse.nusmp.client.heroweapon.curse;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the Curse Series paints the player's own screen (a tint at the edges in the colour of the sin, a corruption bar, a whisper line, a heartbeat sound), and some
 * players find a tint tiring or want the bar somewhere else. This client-side file, {@code nusmp-heroweapon-curse-client.toml}, lets each player switch every part
 * off and move the bar, without touching the server's rules. Every value is a plain {@code define} / {@code defineInRange} (never {@code defineInList}) read through
 * {@link #get}, which answers the default while the file is not loaded yet.
 */
public final class CurseClientConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue OVERLAY, VIGNETTE, BAR, WHISPERS, HEARTBEAT;
    private static final ModConfigSpec.DoubleValue VIGNETTE_MAX;
    private static final ModConfigSpec.IntValue BAR_X, BAR_BOTTOM;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("curse");
        OVERLAY = b.comment("Draw anything of the Curse Series on the screen at all.").define("overlay", true);
        VIGNETTE = b.comment("The tint at the edges of the screen, in the colour of the sin, growing with corruption.").define("vignette", true);
        VIGNETTE_MAX = b.comment("The strongest the tint may get (0 to 0.92). Lower it if the tint tires your eyes.").defineInRange("vignetteMax", 0.8, 0.0, 0.92);
        BAR = b.comment("The corruption bar (and the despair line under it) at the bottom left.").define("bar", true);
        BAR_X = b.comment("Pixels from the left edge of the screen to the corruption bar (the hero weapon HUD panel is at 6).").defineInRange("barX", 6, 0, 4000);
        BAR_BOTTOM = b.comment("Pixels from the bottom edge of the screen to the corruption bar. The default stacks it just above the tallest the hero weapon HUD panel gets (92 pixels with five techniques listed, 103 with the repelled line too).").defineInRange("barBottom", 120, 0, 4000);
        WHISPERS = b.comment("The weapon's whispered lines in the middle of the screen while it offers a curse or corruption is high.").define("whispers", true);
        HEARTBEAT = b.comment("A heartbeat sound at high corruption.").define("heartbeat", true);
        b.pop();
        SPEC = b.build();
    }

    private CurseClientConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean overlay() { return get(OVERLAY); }

    public static boolean vignette() { return get(VIGNETTE); }

    public static float vignetteMax() { return get(VIGNETTE_MAX).floatValue(); }

    public static boolean bar() { return get(BAR); }

    public static int barX() { return get(BAR_X); }

    public static int barBottom() { return get(BAR_BOTTOM); }

    public static boolean whispers() { return get(WHISPERS); }

    public static boolean heartbeat() { return get(HEARTBEAT); }
}
