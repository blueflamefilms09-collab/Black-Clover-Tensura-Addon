package com.newuniverse.nusmp.client.ui.theme;

import com.newuniverse.nusmp.client.ui.gs.GsCrestPos;
import com.newuniverse.nusmp.client.ui.gs.GsSkin;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client options of the menu theme: config/nusmp-ui-client.toml. {@code ui.theme} picks the look ("anime": translucent panels, Great-Sage
 * text, animated magic circles, the default; "classic": the book and parchment drawing the mod had before, kept as it was; "great_spirit": the
 * anime look with the Parchment-and-Ink / Slime-Crest skins, filigree corners, clover frames, the compile animation, the slime ripple and the alert
 * crest). {@code ui.motion} off keeps the anime look but stops the animation (circles stand still, bars do not shimmer, text appears at once).
 * {@code ui.typing} off shows the analysis text immediately instead of typing it. The {@code ui.greatSpirit.*} options only matter in the
 * great_spirit look and are all looks: nothing here reaches the server.
 */
public final class UiThemeOptions {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> THEME;
    public static final ModConfigSpec.BooleanValue MOTION;
    public static final ModConfigSpec.BooleanValue TYPING;
    public static final ModConfigSpec.BooleanValue PAGE_TOAST;
    public static final ModConfigSpec.BooleanValue TENSURA_OVERLAY;
    // ---- ui.greatSpirit.*
    public static final ModConfigSpec.ConfigValue<String> GS_SKIN, GS_QUALITY, GS_NOTICE_CORNER;
    public static final ModConfigSpec.BooleanValue GS_NOTICES, GS_CLOVER_ICONS;
    public static final ModConfigSpec.IntValue GS_NOTICE_X, GS_NOTICE_Y;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("ui");
        THEME = b.comment("The look of the mod's menus: 'anime' (translucent panels, Great-Sage analysis text, animated magic circles), 'classic' (the old book and parchment look) or 'great_spirit' (the anime look with the Parchment-and-Ink / Slime-Crest skins, filigree corners, clover frames, the ink-to-matrix compile animation, the slime ripple and the alert crest; see the ui.greatSpirit options).")
                .define("theme", "anime", o -> o instanceof String s && (s.equals("anime") || s.equals("classic") || s.equals("great_spirit")));   // not defineInList(List.of(..)): List.of(..).contains(null) throws, which aborted the whole config load when the file did not exist yet
        MOTION = b.comment("Animate the anime look (rotating magic circles, shimmering bars). Off = everything stands still.")
                .define("motion", true);
        TYPING = b.comment("Type the Great-Sage analysis text out letter by letter. Off = it appears at once.")
                .define("typing", true);
        PAGE_TOAST = b.comment("Show the analysis panel with a magic circle when a grimoire page opens through mastery (anime look only).")
                .define("pageToast", true);
        TENSURA_OVERLAY = b.comment("Draw the magic circle, cyan brackets and the Great-Sage analysis readout over Tensura's own Skills windows (ability selection, sub-ability mode, categories). Off by default: the analysis look belongs to the mod's own menus (the Codex) only.")
                .define("tensuraOverlay", false);
        b.push("greatSpirit");
        GS_SKIN = b.comment("The skin of the great_spirit look: 'AUTO' = Parchment and Ink (B) for everybody and the Slime Crest (C) for devil users (a forbidden or devil-bound grimoire, Devil Union active or used, a Devil or Daemon race), or force 'B' or 'C'. Five-leaf clovers always use the Slime Crest styling.")
                .define("skin", "AUTO", o -> o instanceof String s && (s.equalsIgnoreCase("AUTO") || s.equalsIgnoreCase("B") || s.equalsIgnoreCase("C")));
        GS_NOTICES = b.comment("The alert crest and its notices ('[Notice... Great Sage has ...]'): page openings and a short list of the mod's own messages are mirrored on screen. Chat is never changed or hidden. Off = no crest.")
                .define("notices", true);
        b.comment("Where the alert crest sits: a screen corner (TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT) and an offset inward from it in GUI pixels. Set it by dragging the crest on the move screen (/nusmp_gui_crest), not by hand.").push("noticePos");
        GS_NOTICE_CORNER = b.define("corner", "TOP_RIGHT", o -> o instanceof String s && GsCrestPos.Corner.parse(s).name().equalsIgnoreCase(s.trim().replace('-', '_').replace(' ', '_')));
        GS_NOTICE_X = b.defineInRange("x", 0, -GsCrestPos.MAX_OFFSET, GsCrestPos.MAX_OFFSET);
        GS_NOTICE_Y = b.defineInRange("y", 0, -GsCrestPos.MAX_OFFSET, GsCrestPos.MAX_OFFSET);
        b.pop();
        GS_QUALITY = b.comment("Drawing quality of the great_spirit look: 'HIGH' (all effects) or 'LOW' (no ripple rings, no glow pulses, no scrambled-glyph band; the compile animation becomes a plain fade). Pick LOW on a weak machine.")
                .define("quality", "HIGH", o -> o instanceof String s && (s.equalsIgnoreCase("HIGH") || s.equalsIgnoreCase("LOW")));
        GS_CLOVER_ICONS = b.comment("Draw the 3/4/5-leaf clover frames around the icons of the mod's own menus (the tier comes from the grimoire cover).")
                .define("cloverIcons", true);
        b.pop();
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

    // ---------------------------------------------------------------- ui.greatSpirit.*
    public static GsSkin.Choice skinChoice() {
        try { return GsSkin.Choice.parse(GS_SKIN.get()); } catch (IllegalStateException notLoaded) { return GsSkin.Choice.AUTO; }
    }

    public static boolean notices() { return get(GS_NOTICES); }

    public static boolean cloverIcons() { return get(GS_CLOVER_ICONS); }

    /** True for quality HIGH (the default, also when the config is not loaded yet). */
    public static boolean highQuality() {
        try { return !"LOW".equalsIgnoreCase(GS_QUALITY.get()); } catch (IllegalStateException notLoaded) { return true; }
    }

    /** The stored crest place (the default top-right corner, zero offset, while the config is not loaded). */
    public static GsCrestPos.Place crestPlace() {
        try {
            return GsCrestPos.sanitize(new GsCrestPos.Place(GsCrestPos.Corner.parse(GS_NOTICE_CORNER.get()), GS_NOTICE_X.get(), GS_NOTICE_Y.get()));
        } catch (IllegalStateException notLoaded) {
            return GsCrestPos.Place.DEFAULT;
        }
    }

    /** Stores the crest place and writes the file. Never throws (a read-only file only loses the saved place; the session keeps it in memory). */
    public static void setCrestPlace(GsCrestPos.Place place) {
        GsCrestPos.Place p = GsCrestPos.sanitize(place);
        try {
            GS_NOTICE_CORNER.set(p.corner().name());
            GS_NOTICE_X.set(p.dx());
            GS_NOTICE_Y.set(p.dy());
            SPEC.save();
        } catch (RuntimeException notLoaded) {
            // the config is not loaded yet or the file cannot be written: the move screen keeps the place for this session
        }
    }

    private static boolean get(ModConfigSpec.BooleanValue v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return true; }
    }
}
