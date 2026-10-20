package com.newuniverse.nusmp.client.relics.vu;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client option of the charms / Genesis worn relics visual upgrade: config/nusmp-visual-charms-client.toml, key {@code visual.upgrade} (default true). Off = every relic of the
 * slice is held and worn as the older library models of 1.101.x (the upgrade files stay on disk and nothing else changes). Registered once from the client section of the mod
 * constructor: {@code modContainer.registerConfig(ModConfig.Type.CLIENT, VuCharmsOptions.SPEC, "nusmp-visual-charms-client.toml")}.
 */
public final class VuCharmsOptions {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue UPGRADE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("visual");
        UPGRADE = b.comment("Draw the God and Genesis charms, the Infinity Gauntlet, the two crowns and the Gate of Babylon crest with the upgraded 3D models, skins, glow passes and ambient effects. "
                + "Off = the older models of 1.101.x. A resource reload (F3+T) or re-entering the world applies a change at once.")
                .define("upgrade", true);
        b.pop();
        SPEC = b.build();
    }

    private VuCharmsOptions() {}

    /** True when the upgraded models are used ({@code visual.upgrade}); true when the config is not loaded yet. */
    public static boolean upgrade() {
        try {
            return UPGRADE.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }
}
