package com.newuniverse.nusmp.client.doublesui;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the Real Double stack only changes how the player's own screen looks, so its settings are a CLIENT file, {@code nusmp-doubles-ui-client.toml}: {@code doublesStackedUi}
 * (true = the stack; false = the old screen, which drew nothing for the doubles), how many cards are shown before the rest fold into "+N more", and whether the distance is
 * written on each card. Plain define / defineInRange read through {@link #get}, which answers the default while the file is not loaded yet.
 */
public final class DoublesClientConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue STACKED, DISTANCE;
    private static final ModConfigSpec.IntValue MAX_ROWS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("doublesUi");
        STACKED = b.comment("Draw the stack of your Real Doubles (name, health, time left) on the screen. false = the old screen: nothing is drawn for the doubles (the selection keys still work).").define("doublesStackedUi", true);
        MAX_ROWS = b.comment("How many doubles are listed before the rest fold into a '+N more' line (the selected one is always listed first).").defineInRange("maxRows", 4, 1, 16);
        DISTANCE = b.comment("Write how far each double is (in blocks) on its card.").define("showDistance", true);
        b.pop();
        SPEC = b.build();
    }

    private DoublesClientConfig() {}

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean stackedUi() { return get(STACKED); }

    public static int maxRows() { return get(MAX_ROWS); }

    public static boolean showDistance() { return get(DISTANCE); }
}
