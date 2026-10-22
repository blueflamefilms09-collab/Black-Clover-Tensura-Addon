package com.newuniverse.nusmp.client.ui.move;

import com.newuniverse.nusmp.client.ui.move.GuiMoveLayout.State;
import com.newuniverse.nusmp.client.ui.move.GuiMoveRegistry.Element;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: where each player put each GUI is a taste, never the server's business, so it is a CLIENT config of its own, {@code nusmp-gui-layout-client.toml}: one short text per
 * registered element in section {@code [elements]} (empty = never touched, see {@link GuiMoveLayout}) and the edit mode's own switches. The texts are parsed into the
 * elements ({@link Element#state}) ONCE, when the file loads or changes, so a frame never reads the config; a change made in edit mode updates the element at once and the
 * text, and the file is written when a drag ends or the screen closes, never per mouse move. Every reader answers its default while the file is not loaded yet (the first
 * frames), and a failed write only loses the saved place: the layout stays valid for the session.
 */
public final class GuiMoveOptions {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SNAP, SHOW_ALL;
    private static final Map<String, ModConfigSpec.ConfigValue<String>> VALUES = new LinkedHashMap<>();

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        SNAP = b.comment("Edit mode (the GUI Move key): snap an element to the screen's edges and middle and to the other elements while you drag it.")
                .define("snap", true);
        SHOW_ALL = b.comment("Edit mode: also show the elements that are not on screen right now (dimmed), so a GUI you cannot see at the moment can be placed too.")
                .define("showAll", true);
        b.comment("One entry per movable GUI: empty = never moved (it draws where it always did). Set by dragging in edit mode (the GUI Move key, or /nusmp gui move), not by hand: "
                + "p=1 placed; a=corner 0..3 (top-left, top-right, bottom-left, bottom-right); cx/cy=1 measured from the middle of the screen; ox/oy=offset in the element's own pixels; "
                + "s=size 0.5..2.5; h=1 hidden. The ids are listed in docs/ui/gui_move.md.").push("elements");
        for (Element e : GuiMoveRegistry.all()) VALUES.put(e.id, b.define(e.id, ""));
        b.pop();
        SPEC = b.build();
    }

    private GuiMoveOptions() {}

    public static boolean snap() { return bool(SNAP, true); }

    public static boolean showAll() { return bool(SHOW_ALL, true); }

    public static void setSnap(boolean on) { set(SNAP, on); }

    public static void setShowAll(boolean on) { set(SHOW_ALL, on); }

    /** Reads every element's text from the config into its state. Called when the file loads or changes. */
    public static void loadAll() {
        for (Element e : GuiMoveRegistry.all()) {
            ModConfigSpec.ConfigValue<String> v = VALUES.get(e.id);
            if (v == null) continue;
            try {
                e.state = GuiMoveLayout.parse(v.get());
            } catch (IllegalStateException notLoaded) {
                return;                                                    // the file is not loaded yet: every element stays untouched
            }
        }
    }

    /** Writes one element's state into the config value (in memory; {@link #save} writes the file). */
    public static void store(Element e) {
        ModConfigSpec.ConfigValue<String> v = VALUES.get(e.id);
        if (v == null) return;
        try {
            v.set(GuiMoveLayout.format(e.state));
        } catch (RuntimeException notLoaded) {
            // the config is not loaded yet (or is being reloaded): the element keeps its state for this session
        }
    }

    /** Writes the file. Never throws. */
    public static void save() {
        try {
            SPEC.save();
        } catch (RuntimeException notSaved) {
            // read-only file or the spec is not loaded: the layout stays valid for this session
        }
    }

    private static boolean bool(ModConfigSpec.BooleanValue v, boolean fallback) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return fallback; }
    }

    private static void set(ModConfigSpec.BooleanValue v, boolean on) {
        try { v.set(on); } catch (RuntimeException notLoaded) { /* not loaded yet */ }
    }
}
