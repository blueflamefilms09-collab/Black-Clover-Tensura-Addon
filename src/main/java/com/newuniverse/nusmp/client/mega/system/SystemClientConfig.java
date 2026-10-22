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
    public static final ModConfigSpec.BooleanValue ENABLED, SHOW_STATS, TOASTS, CENTER_X, CENTER_Y, FLOOR_CARD;
    public static final ModConfigSpec.DoubleValue SCALE, ALPHA;
    public static final ModConfigSpec.IntValue ANCHOR, OFFSET_X, OFFSET_Y, FLOOR_CARD_SECONDS;

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
        FLOOR_CARD = b.comment("1.94.0: show the floor report on every arrival: the System window that says which floor this is and which way it goes, the theme of the floor and the mechanics in effect "
                        + "(what each does and how to beat it). false = it is not shown by itself; the floor report key (Controls > Multiverse: System) and /dungeon floor still show it.")
                .define("floorCard", true);
        FLOOR_CARD_SECONDS = b.comment("1.94.0: how many seconds the floor report stays on screen after an arrival (a report you asked for stays longer, a mid-floor update shorter).")
                .defineInRange("floorCardSeconds", 14, 3, 60);
        b.pop();
        SPEC = b.build();
    }

    private SystemClientConfig() {}

    public static boolean enabled() { return bool(ENABLED, true); }

    public static boolean showStats() { return bool(SHOW_STATS, true); }

    public static boolean toasts() { return bool(TOASTS, true); }

    /** 1.94.0: is the floor report shown by itself on an arrival? */
    public static boolean floorCard() { return bool(FLOOR_CARD, true); }

    /** 1.94.0: seconds the floor report stays up after an arrival (3..60). */
    public static int floorCardSeconds() {
        try { return Math.max(3, Math.min(60, FLOOR_CARD_SECONDS.get())); } catch (IllegalStateException notLoaded) { return 14; }
    }

    /**
     * GUI Move: the window's place and size live in the layout store of GUI Move ({@code nusmp-gui-layout-client.toml}, element {@code system_hud}) since the global edit mode
     * moves it like every other GUI. The keys of this file ({@code scale}, {@code anchor}, {@code offsetX}, {@code offsetY}, {@code centerX}, {@code centerY}) are KEPT and
     * are the fallback: while the store has no place for the window (a player who never placed it with the new code), the old keys decide, so every saved position of earlier
     * versions still applies exactly. The first time the window is placed or sized (by either edit screen) the store takes over, with the old position as the starting point.
     */
    private static com.newuniverse.nusmp.client.ui.move.GuiMoveLayout.State stored() {
        com.newuniverse.nusmp.client.ui.move.GuiMoveRegistry.Element e = com.newuniverse.nusmp.client.ui.move.GuiMoveRegistry.byId(GUI_MOVE_ID);
        com.newuniverse.nusmp.client.ui.move.GuiMoveLayout.State st = e == null ? null : e.state;
        return st != null && st.placed ? st : null;
    }

    /** The id of the window in the GUI Move registry. */
    public static final String GUI_MOVE_ID = "system_hud";

    /** The window scale, clamped to 0.5..2.5. */
    public static float scale() {
        com.newuniverse.nusmp.client.ui.move.GuiMoveLayout.State st = stored();
        if (st != null) return OverlayMath.clampScale(st.scale);
        return legacyScale();
    }

    private static float legacyScale() {
        try { return OverlayMath.clampScale(SCALE.get()); } catch (IllegalStateException notLoaded) { return 1.0f; }
    }

    /** The corner, 0..3. */
    public static int anchor() {
        com.newuniverse.nusmp.client.ui.move.GuiMoveLayout.State st = stored();
        if (st != null) return OverlayMath.safeAnchor(st.place.anchor());
        try { return Math.max(0, Math.min(3, ANCHOR.get())); } catch (IllegalStateException notLoaded) { return 0; }
    }

    /** How far the window is moved sideways from its corner (window pixels, inwards), within the config's range. */
    public static int offsetX() {
        com.newuniverse.nusmp.client.ui.move.GuiMoveLayout.State st = stored();
        return st != null ? OverlayMath.clampOffset(st.place.offX()) : offset(OFFSET_X);
    }

    /** How far the window is moved vertically from its corner (window pixels, inwards). */
    public static int offsetY() {
        com.newuniverse.nusmp.client.ui.move.GuiMoveLayout.State st = stored();
        return st != null ? OverlayMath.clampOffset(st.place.offY()) : offset(OFFSET_Y);
    }

    /**
     * Puts the window at this corner and offset (limited to the valid range) in memory; {@link #save} writes the file. Called on every step of a nudge or a drag,
     * so it does not touch the disk; before the config is loaded it does nothing.
     */
    public static void setPlacement(int anchor, int offsetX, int offsetY) {
        setPlace(new OverlayDrag.Place(OverlayMath.safeAnchor(anchor), centerX(), centerY(), OverlayMath.clampOffset(offsetX), OverlayMath.clampOffset(offsetY)));
    }

    /** Writes the layout file (the place of the window, with every other GUI's). Never throws: a failed write only loses the saved place. */
    public static void save() {
        com.newuniverse.nusmp.client.ui.move.GuiMoveOptions.save();
    }

    /** Is the sideways offset measured from the middle of the screen? */
    public static boolean centerX() {
        com.newuniverse.nusmp.client.ui.move.GuiMoveLayout.State st = stored();
        return st != null ? st.place.centerX() : bool(CENTER_X, false);
    }

    /** Is the vertical offset measured from the middle of the screen? */
    public static boolean centerY() {
        com.newuniverse.nusmp.client.ui.move.GuiMoveLayout.State st = stored();
        return st != null ? st.place.centerY() : bool(CENTER_Y, false);
    }

    /** The whole saved place: the corner, the two centre flags and the offsets. */
    public static OverlayDrag.Place place() { return new OverlayDrag.Place(anchor(), centerX(), centerY(), offsetX(), offsetY()); }

    /** The place the OLD keys of this file hold (what the window had before GUI Move): used by the migration test and as the fallback. */
    public static OverlayDrag.Place legacyPlace() {
        try {
            return new OverlayDrag.Place(Math.max(0, Math.min(3, ANCHOR.get())), CENTER_X.get(), CENTER_Y.get(), OverlayMath.clampOffset(OFFSET_X.get()), OverlayMath.clampOffset(OFFSET_Y.get()));
        } catch (IllegalStateException notLoaded) {
            return OverlayDrag.Place.DEFAULT;
        }
    }

    /** Puts the window at this place in memory ({@link #save} writes the file). Before the config is loaded it does nothing. */
    public static void setPlace(OverlayDrag.Place p) {
        com.newuniverse.nusmp.client.ui.move.GuiMove.storePlace(GUI_MOVE_ID,
                com.newuniverse.nusmp.client.ui.move.GuiMoveMath.fromLegacy(p.anchor(), p.centerX(), p.centerY(), p.offX(), p.offY()), scale());
    }

    /** Sets the window's size in memory (0.5 to 2.5); {@link #save} writes the file. The place it has now is kept. */
    public static void setScale(float scale) {
        OverlayDrag.Place p = place();
        com.newuniverse.nusmp.client.ui.move.GuiMove.storePlace(GUI_MOVE_ID,
                com.newuniverse.nusmp.client.ui.move.GuiMoveMath.fromLegacy(p.anchor(), p.centerX(), p.centerY(), p.offX(), p.offY()), OverlayMath.clampScale(scale));
    }

    /**
     * Puts the old keys of this file back to their defaults (top-left corner, no offset, size 1) and writes them. Called when GUI Move resets the window: without it the
     * old keys would show again as the fallback of a window that has no place in the store.
     */
    public static void forgetLegacyPlace() {
        try {
            ANCHOR.set(0);
            CENTER_X.set(false);
            CENTER_Y.set(false);
            OFFSET_X.set(0);
            OFFSET_Y.set(0);
            SCALE.set(1.0);
            SPEC.save();
        } catch (RuntimeException notLoaded) {
            // the config is not loaded yet (or is being reloaded): nothing to forget
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
