package com.newuniverse.nusmp.client.mega.system;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: how the holographic System window sits on SCREEN is a taste, not a rule of the game, so it is a CLIENT config ({@code nusmp-labyrinth-mega-system-client.toml},
 * section {@code [system]}), never the server's business: whether the window is shown at all, how big, in which corner, how see-through, whether the stat row and the
 * pop-up lines show. Plain {@code define} / {@code defineInRange} only, and every reader answers its default while the config is not loaded yet (the first frames,
 * a unit test), exactly like the other client option classes of the mod. The window's toggle KEY is separate ({@link SystemClient#TOGGLE}); this file says what it
 * toggles back to.
 */
public final class SystemClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, SHOW_STATS, TOASTS;
    public static final ModConfigSpec.DoubleValue SCALE, ALPHA;
    public static final ModConfigSpec.IntValue ANCHOR;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("system");
        ENABLED = b.comment("Draw the holographic System window on the Holy floors of the Mega-Dungeon. false = never drawn (the quests still run).")
                .define("enabled", true);
        SCALE = b.comment("Size of the window (0.5 to 2.0).")
                .defineInRange("scale", 1.0, 0.5, 2.0);
        ANCHOR = b.comment("Which corner the window sits in: 0 top-left, 1 top-right, 2 bottom-left, 3 bottom-right.")
                .defineInRange("anchor", 0, 0, 3);
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

    /** The window's opacity, 0.1..1.0. */
    public static float alpha() {
        try { return (float) Math.max(0.1, Math.min(1.0, ALPHA.get())); } catch (IllegalStateException notLoaded) { return 0.67f; }
    }

    private static boolean bool(ModConfigSpec.BooleanValue v, boolean fallback) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return fallback; }
    }
}
