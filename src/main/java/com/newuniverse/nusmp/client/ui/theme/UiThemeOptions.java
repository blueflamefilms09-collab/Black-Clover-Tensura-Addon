package com.newuniverse.nusmp.client.ui.theme;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client options of the menu theme: config/nusmp-ui-client.toml. {@code ui.theme} picks the look ("anime": translucent panels, Great-Sage
 * text, animated magic circles, the default; "classic": the book and parchment drawing the mod had before, kept as it was). {@code ui.motion}
 * off keeps the anime look but stops the animation (circles stand still, bars do not shimmer, text appears at once). {@code ui.typing} off shows
 * the analysis text immediately instead of typing it.
 */
public final class UiThemeOptions {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> THEME;
    public static final ModConfigSpec.BooleanValue MOTION;
    public static final ModConfigSpec.BooleanValue TYPING;
    public static final ModConfigSpec.BooleanValue PAGE_TOAST;
    public static final ModConfigSpec.BooleanValue TENSURA_OVERLAY;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("ui");
        THEME = b.comment("The look of the mod's menus: 'anime' (translucent panels, Great-Sage analysis text, animated magic circles) or 'classic' (the old book and parchment look).")
                .define("theme", "anime", o -> o instanceof String s && (s.equals("anime") || s.equals("classic")));   // not defineInList(List.of(..)): List.of(..).contains(null) throws, which aborted the whole config load when the file did not exist yet
        MOTION = b.comment("Animate the anime look (rotating magic circles, shimmering bars). Off = everything stands still.")
                .define("motion", true);
        TYPING = b.comment("Type the Great-Sage analysis text out letter by letter. Off = it appears at once.")
                .define("typing", true);
        PAGE_TOAST = b.comment("Show the analysis panel with a magic circle when a grimoire page opens through mastery (anime look only).")
                .define("pageToast", true);
        TENSURA_OVERLAY = b.comment("Draw the magic circle, cyan brackets and the Great-Sage analysis readout over Tensura's own Skills windows (ability selection, sub-ability mode, categories). Off by default: the analysis look belongs to the mod's own menus (the Codex) only.")
                .define("tensuraOverlay", false);
        b.pop();
        SPEC = b.build();
    }

    private UiThemeOptions() {}

    public static UiThemeMode mode() {
        try { return UiThemeMode.parse(THEME.get()); } catch (IllegalStateException notLoaded) { return UiThemeMode.ANIME; }
    }

    public static boolean motion() { return get(MOTION); }

    public static boolean typing() { return get(TYPING); }

    public static boolean pageToast() { return get(PAGE_TOAST); }

    /** Whether the analysis overlay may be drawn over Tensura's own screens. Default false, also when the config is not loaded yet. */
    public static boolean tensuraOverlay() {
        try { return TENSURA_OVERLAY.get(); } catch (IllegalStateException notLoaded) { return false; }
    }

    private static boolean get(ModConfigSpec.BooleanValue v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return true; }
    }
}
