package com.newuniverse.nusmp.client.mega.system;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: how the holographic System window sits on SCREEN is a taste, not a rule of the game, so it is a CLIENT config ({@code nusmp-labyrinth-mega-system-client.toml},
 * section {@code [system]}), never the server's business: whether the window is shown at all, how big, in which corner and how far from it (the player moves and resizes
 * the window with the mouse in the edit screen, and the place is saved here as {@code anchor}, {@code offsetX}, {@code offsetY}, the two centre flags and {@code scale}), how see-through,
 * whether the stat row and the pop-up lines show. Plain {@code define} / {@code defineInRange} only, and every reader answers its default while the config is not loaded yet (the first frames,
 * a unit test), exactly like the other client option classes of the mod. The window's toggle KEY is separate ({@link SystemClient#TOGGLE}); this file says what it
 * toggles back to.
 */
public final class SystemClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, SHOW_STATS, TOASTS, CENTER_X, CENTER_Y;
    public static final ModConfigSpec.DoubleValue SCALE, ALPHA;
    public static final ModConfigSpec.IntValue ANCHOR, OFFSET_X, OFFSET_Y;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("system");
        ENABLED = b.comment("Draw the holographic System window in the dungeon (the Mega-Dungeon, both roads, and the Hub). false = never drawn (the quests still run).")
                .define("enabled", true);
        SCALE = b.comment("Size of the window (0.5 to 2.5). Set it by dragging a corner of the window, or scrolling over it, in the edit screen (the edit key), not by hand.")
                .defineInRange("scale", 1.0, 0.5, 2.5);
        ANCHOR = b.comment("Which corner the window sits in: 0 top-left, 1 top-right, 2 bottom-left, 3 bottom-right.")
                .defineInRange("anchor", 0, 0, 3);
        OFFSET_X = b.comment("How far the window is moved from its corner, sideways, in the window's own pixels, towards the middle of the screen (negative = towards the edge). "
                        + "Set it by dragging the window in the edit screen (the edit key in Controls > Multiverse: System), not by hand; the window is always kept on the screen.")
                .defineInRange("offsetX", 0, -OverlayMath.MAX_OFFSET, OverlayMath.MAX_OFFSET);
        OFFSET_Y = b.comment("The same, vertically.")
                .defineInRange("offsetY", 0, -OverlayMath.MAX_OFFSET, OverlayMath.MAX_OFFSET);
        CENTER_X = b.comment("Set by the edit screen when the window is dropped in the middle third of the screen: offsetX is then measured from the middle of the screen instead of from the corner's edge, "
                        + "so the window stays centred on any resolution. false = the corner's edge (the behaviour before the mouse placement).")
                .define("centerX", false);
        CENTER_Y = b.comment("The same, vertically (offsetY from the middle of the screen).")
                .define("centerY", false);
        ALPHA = b.comment("How opaque the window is (0.1 to 1.0).")
                .defineInRange("alpha", 0.67, 0.1, 1.0);
        SHOW_STATS = b.comment("Show the row of stat points (STR AGI VIT INT SENSE) at the bottom of the window.")
                .define("showStats", true);
        TOASTS = b.comment("Show the pop-up lines that slide in from the left (quest arrived, quest complete, rewards, floor reached, extraction ready).")
                .define("toasts", true);
        b.pop();
        SPEC = b.build();
    }

    private SystemClientConfig() {}

    public static boolean enabled() { return bool(ENABLED, true); }

    public static boolean showStats() { return bool(SHOW_STATS, true); }

    public static boolean toasts() { return bool(TOASTS, true); }

    /** The window scale, clamped to 0.5..2.0. */
    public static float scale() {
        try { return OverlayMath.clampScale(SCALE.get()); } catch (IllegalStateException notLoaded) { return 1.0f; }
    }

    /** The corner, 0..3. */
    public static int anchor() {
        try { return Math.max(0, Math.min(3, ANCHOR.get())); } catch (IllegalStateException notLoaded) { return 0; }
    }

    /** How far the window is moved sideways from its corner (window pixels, inwards), within the config's range. */
    public static int offsetX() { return offset(OFFSET_X); }

    /** How far the window is moved vertically from its corner (window pixels, inwards). */
    public static int offsetY() { return offset(OFFSET_Y); }

    /**
     * Puts the window at this corner and offset (limited to the valid range) in memory; {@link #save} writes the file. Called on every step of a nudge or a drag,
     * so it does not touch the disk; before the config is loaded it does nothing.
     */
    public static void setPlacement(int anchor, int offsetX, int offsetY) {
        try {
            ANCHOR.set(OverlayMath.safeAnchor(anchor));
            OFFSET_X.set(OverlayMath.clampOffset(offsetX));
            OFFSET_Y.set(OverlayMath.clampOffset(offsetY));
        } catch (RuntimeException notLoaded) {
            // the config is not loaded yet (or is being reloaded): the window simply stays where it is
        }
    }

    /** Writes the client config file (the place of the window). Never throws: a failed write only loses the saved place. */
    public static void save() {
        try {
            SPEC.save();
        } catch (RuntimeException notSaved) {
            // read-only file or the spec is not loaded: the place stays valid for this session
        }
    }

    /** Is the sideways offset measured from the middle of the screen? */
    public static boolean centerX() { return bool(CENTER_X, false); }

    /** Is the vertical offset measured from the middle of the screen? */
    public static boolean centerY() { return bool(CENTER_Y, false); }

    /** The whole saved place: the corner, the two centre flags and the offsets. */
    public static OverlayDrag.Place place() { return new OverlayDrag.Place(anchor(), centerX(), centerY(), offsetX(), offsetY()); }

    /** Puts the window at this place in memory ({@link #save} writes the file). Before the config is loaded it does nothing. */
    public static void setPlace(OverlayDrag.Place p) {
        try {
            ANCHOR.set(OverlayMath.safeAnchor(p.anchor()));
            CENTER_X.set(p.centerX());
            CENTER_Y.set(p.centerY());
            OFFSET_X.set(OverlayMath.clampOffset(p.offX()));
            OFFSET_Y.set(OverlayMath.clampOffset(p.offY()));
        } catch (RuntimeException notLoaded) {
            // the config is not loaded yet (or is being reloaded): the window simply stays where it is
        }
    }

    /** Sets the window's size in memory (0.5 to 2.5); {@link #save} writes the file. */
    public static void setScale(float scale) {
        try {
            SCALE.set((double) OverlayMath.clampScale(scale));
        } catch (RuntimeException notLoaded) {
            // not loaded yet: the size stays as it is
        }
    }

    /** The window's opacity, 0.1..1.0. */
    public static float alpha() {
        try { return (float) Math.max(0.1, Math.min(1.0, ALPHA.get())); } catch (IllegalStateException notLoaded) { return 0.67f; }
    }

    private static int offset(ModConfigSpec.IntValue v) {
        try { return OverlayMath.clampOffset(v.get()); } catch (IllegalStateException notLoaded) { return 0; }
    }

    private static boolean bool(ModConfigSpec.BooleanValue v, boolean fallback) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return fallback; }
    }
}
