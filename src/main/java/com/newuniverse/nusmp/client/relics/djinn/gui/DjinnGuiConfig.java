package com.newuniverse.nusmp.client.relics.djinn.gui;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client options of the active Djinn panel (file nusmp-djinn-gui-client.toml, registered as a CLIENT config by {@code DjinnClient.init}): which
 * renderer to use (the new themed panel or the classic bar of earlier versions) and where the panel sits.
 */
public final class DjinnGuiConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<GuiStyle> STYLE;
    public static final ModConfigSpec.EnumValue<GuiLayout.Anchor> ANCHOR;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        STYLE = b.comment("NEW: the themed, animated Djinn panel (frame, emblem and bars follow the Djinn you hold). CLASSIC: the small bar of earlier versions.")
                .defineEnum("style", GuiStyle.NEW);
        ANCHOR = b.comment("Where the NEW panel sits: BOTTOM_CENTER (above the status icons), BOTTOM_LEFT or BOTTOM_RIGHT.")
                .defineEnum("anchor", GuiLayout.Anchor.BOTTOM_CENTER);
        SPEC = b.build();
    }

    private DjinnGuiConfig() {}

    public static GuiStyle style() {
        try { return STYLE.get(); } catch (IllegalStateException notLoaded) { return GuiStyle.NEW; }
    }

    public static GuiLayout.Anchor anchor() {
        try { return ANCHOR.get(); } catch (IllegalStateException notLoaded) { return GuiLayout.Anchor.BOTTOM_CENTER; }
    }
}
