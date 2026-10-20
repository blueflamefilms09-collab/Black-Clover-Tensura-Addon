package com.newuniverse.nusmp.client.relics.djinn.tiny;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client options of the Tiny Djinn (file {@code nusmp-djinn-tiny-client.toml}, registered by {@link TinyDjinnClient#init}). Under {@code [djinn]}:
 * {@code tinyModel} (the figure on or off: off brings back the old translucent bust), {@code tinyMax} (the most figures drawn in one frame), {@code tinyNameTag},
 * {@code tinyTimerBar} and {@code tinyPortrait} (the head in the panel's circle). A value read before the config is loaded gives its default.
 */
public final class TinyDjinnConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue TINY_MODEL, TINY_NAME, TINY_BAR, TINY_PORTRAIT;
    public static final ModConfigSpec.IntValue TINY_MAX;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("djinn");
        TINY_MODEL = b.comment("The Lesser Summon is drawn as a real tiny Djinn (a small 3D figure of your Djinn at your shoulder). False: the old translucent bust.")
                .translation("nusmp.configuration.djinn.tinyModel").define("tinyModel", true);
        TINY_MAX = b.comment("The most Tiny Djinn drawn in one frame (the nearest ones are kept). 0 draws none.")
                .translation("nusmp.configuration.djinn.tinyMax").defineInRange("tinyMax", TinyDjinnMath.DEFAULT_MAX, 0, TinyDjinnMath.MAX_LIMIT);
        TINY_NAME = b.comment("Float the Djinn's name over the figure (within 16 blocks).")
                .translation("nusmp.configuration.djinn.tinyNameTag").define("tinyNameTag", true);
        TINY_BAR = b.comment("A thin bar under the name that shows the seconds the Lesser Summon has left.")
                .translation("nusmp.configuration.djinn.tinyTimerBar").define("tinyTimerBar", true);
        TINY_PORTRAIT = b.comment("Draw the figure's head in the circle of the Djinn panel during a Lesser Summon, instead of the silhouette.")
                .translation("nusmp.configuration.djinn.tinyPortrait").define("tinyPortrait", true);
        b.pop();
        SPEC = b.build();
    }

    private TinyDjinnConfig() {}

    public static boolean enabled() {
        try { return TINY_MODEL.get(); } catch (IllegalStateException notLoaded) { return true; }
    }

    public static int max() {
        try { return TinyDjinnMath.clampMax(TINY_MAX.get()); } catch (IllegalStateException notLoaded) { return TinyDjinnMath.DEFAULT_MAX; }
    }

    public static boolean nameTag() {
        try { return TINY_NAME.get(); } catch (IllegalStateException notLoaded) { return true; }
    }

    public static boolean timerBar() {
        try { return TINY_BAR.get(); } catch (IllegalStateException notLoaded) { return true; }
    }

    public static boolean portrait() {
        try { return TINY_PORTRAIT.get(); } catch (IllegalStateException notLoaded) { return true; }
    }
}
