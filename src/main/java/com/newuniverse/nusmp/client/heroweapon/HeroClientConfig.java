package com.newuniverse.nusmp.client.heroweapon;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: where the hero weapon's HUD sits and whether it shows at all is each player's taste, not the server's, so it is a CLIENT config,
 * {@code nusmp-heroweapon-client.toml}: a switch, two offsets from the bottom-left corner of the screen (in pixels, so the panel can be moved clear of the chat or
 * another mod's overlay), and a scale. Plain {@code define} / {@code defineInRange} values read through {@link #get}, which answers the default while the file is not
 * loaded yet.
 */
public final class HeroClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue HUD;
    public static final ModConfigSpec.IntValue HUD_X, HUD_Y;
    public static final ModConfigSpec.DoubleValue HUD_SCALE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("hud");
        HUD = b.comment("Show the hero weapon's HUD (name, level, form, techniques with their cooldowns) at the bottom left.").define("show", true);
        HUD_X = b.comment("Pixels from the left edge of the screen.").defineInRange("x", 6, 0, 4000);
        HUD_Y = b.comment("Pixels from the bottom edge of the screen to the bottom of the panel.").defineInRange("y", 6, 0, 4000);
        HUD_SCALE = b.comment("Size of the panel (1.0 is the normal size).").defineInRange("scale", 1.0, 0.5, 3.0);
        b.pop();
        SPEC = b.build();
    }

    private HeroClientConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try {
            return v.get();
        } catch (IllegalStateException notLoaded) {
            return v.getDefault();
        }
    }

    public static boolean hud() { return get(HUD); }

    public static int x() { return get(HUD_X); }

    public static int y() { return get(HUD_Y); }

    public static float scale() { return get(HUD_SCALE).floatValue(); }
}
