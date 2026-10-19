package com.newuniverse.nusmp.client.ui.scale;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: the player's GUI size choices. File: nusmp-gui-client.toml (register {@link #SPEC} as a CLIENT config).
 * {@code guiScale} is the global size (0.5 to 2.0), {@code [overrides]} has one entry per GUI (0 = follow the global value),
 * {@code [anchors]} is the point of the screen each HUD element grows from.
 */
public final class GuiScaleOptions {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue GLOBAL;
    public static final ModConfigSpec.BooleanValue FIT, HUD_FOLLOWS_GLOBAL;
    private static final Map<String, ModConfigSpec.DoubleValue> OVERRIDES = new LinkedHashMap<>();
    private static final Map<String, ModConfigSpec.EnumValue<GuiScaleMath.Anchor>> ANCHORS = new LinkedHashMap<>();

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        GLOBAL = b.comment("Size of every GUI of the mod (screens and HUD), 0.5 to 2.0. 1.0 is the normal size.")
                .defineInRange("guiScale", 1.0, GuiScaleMath.MIN, GuiScaleMath.MAX);
        FIT = b.comment("Fit to window: never draw a screen larger than the window, however high the size is set.")
                .define("fitToWindow", true);
        HUD_FOLLOWS_GLOBAL = b.comment("Whether HUD elements (quest tracker, boss bar, System window ...) follow the global size. Off: they only change with their own entry.")
                .define("hudFollowsGlobal", true);
        b.comment("Per-GUI size. 0 = follow the global size, otherwise 0.5 to 2.0. The ids are listed in docs/gui_scale.md.").push("overrides");
        for (GuiScaleRegistry.Entry e : GuiScaleRegistry.all()) OVERRIDES.put(e.id(), b.defineInRange(e.id(), 0.0, 0.0, GuiScaleMath.MAX));
        b.pop();
        b.comment("The point of the screen a HUD element grows from when it is made bigger.").push("anchors");
        for (GuiScaleRegistry.Entry e : GuiScaleRegistry.of(GuiScaleRegistry.Kind.HUD)) {
            if (e.mode() == GuiScaleRegistry.Mode.LAYER) ANCHORS.put(e.id(), b.defineEnum(e.id(), e.anchor()));
        }
        b.pop();
        SPEC = b.build();
    }

    private GuiScaleOptions() {}

    public static double global() {
        try { return GuiScaleMath.clamp(GLOBAL.get()); } catch (IllegalStateException notLoaded) { return GuiScaleMath.DEFAULT; }
    }

    public static boolean fit() {
        try { return FIT.get(); } catch (IllegalStateException notLoaded) { return true; }
    }

    public static boolean hudFollowsGlobal() {
        try { return HUD_FOLLOWS_GLOBAL.get(); } catch (IllegalStateException notLoaded) { return true; }
    }

    /** The per-GUI override of an id; 0 when it follows the global value. */
    public static double override(String id) {
        ModConfigSpec.DoubleValue v = OVERRIDES.get(id);
        if (v == null) return 0.0;
        try {
            double d = v.get();
            return GuiScaleMath.hasOverride(d) ? GuiScaleMath.clamp(d) : 0.0;
        } catch (IllegalStateException notLoaded) { return 0.0; }
    }

    /** Where a HUD layer grows from: the configured anchor, else the registry's. */
    public static GuiScaleMath.Anchor anchor(String id) {
        GuiScaleRegistry.Entry e = GuiScaleRegistry.byId(id);
        GuiScaleMath.Anchor def = e == null ? GuiScaleMath.Anchor.CENTER : e.anchor();
        ModConfigSpec.EnumValue<GuiScaleMath.Anchor> v = ANCHORS.get(id);
        if (v == null) return def;
        try { return v.get(); } catch (IllegalStateException notLoaded) { return def; }
    }

    public static void setGlobal(double s) { try { GLOBAL.set(GuiScaleMath.snap(s)); } catch (IllegalStateException notLoaded) { /* config not loaded yet */ } }

    public static void setFit(boolean on) { try { FIT.set(on); } catch (IllegalStateException notLoaded) { /* config not loaded yet */ } }

    public static void setHudFollowsGlobal(boolean on) { try { HUD_FOLLOWS_GLOBAL.set(on); } catch (IllegalStateException notLoaded) { /* config not loaded yet */ } }

    /** 0 (or below) puts the GUI back on the global size. */
    public static void setOverride(String id, double s) {
        ModConfigSpec.DoubleValue v = OVERRIDES.get(id);
        if (v == null) return;
        try { v.set(GuiScaleMath.hasOverride(s) ? GuiScaleMath.snap(s) : 0.0); } catch (IllegalStateException notLoaded) { /* config not loaded yet */ }
    }

    public static void save() {
        try { SPEC.save(); } catch (RuntimeException ignored) { /* the values still apply for this session */ }
    }
}
