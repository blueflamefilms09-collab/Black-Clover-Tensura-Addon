package com.newuniverse.nusmp.client.multiverse;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client config: config/nusmp-multiverse-client.toml */
public final class MultiverseClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> SCREEN_PREFIX;
    public static final ModConfigSpec.ConfigValue<String> PANEL_TEXT;
    public static final ModConfigSpec.ConfigValue<String> PANEL_RECT;
    public static final ModConfigSpec.BooleanValue BUTTON_FALLBACK;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("status_panel");
        SCREEN_PREFIX = b.comment("Screens whose class name starts with this get the Multiverse status panel (the Tensura status / magic menu).")
                .define("screenClassPrefix", "io.github.manasmods.tensura");
        PANEL_TEXT = b.comment("Text of the placeholder widget the panel replaces (case-insensitive).").define("placeholderText", "coming soon");
        PANEL_RECT = b.comment("Force the panel to this rectangle instead: x,y,width,height in GUI pixels from the screen's top-left (empty = auto).")
                .define("panelRect", "");
        BUTTON_FALLBACK = b.comment("When no placeholder is found, add a small 'Multiverse' button that opens the full status screen.")
                .define("buttonFallback", true);
        b.pop();
        SPEC = b.build();
    }

    private MultiverseClientConfig() {}

    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
