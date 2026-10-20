package com.newuniverse.nusmp.client.relics.item.vu;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The client switches of the visual upgrade of the Genesis weapons: config/nusmp-visual-upgrade-client.toml.
 * {@code visual.upgrade} (default true): draw the upgraded meshes; off = the old meshes of the 1.97.0 weapons, unchanged (the fallback).
 * {@code visual.particles} (default true): the ambient motes, ground glow and signature bursts of the upgraded weapons (still limited by the client particle budget and the effect quality).
 * {@code visual.trails} (default true): the swing trails of the upgraded weapons.
 * {@code visual.guiModel} (default false): show the upgraded 3D mesh, tilted, in inventory slots instead of the old flat sprite (the owner keeps the old sprite as the icon, so it is off).
 * Registered from {@link VuClient#init}.
 */
public final class VuOptions {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue UPGRADE, PARTICLES, TRAILS, GUI_MODEL;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("visual");
        UPGRADE = b.comment("Draw the upgraded 3D models, glow passes and effects of the Genesis weapons. Off = the models the weapons had in 1.97.0.").define("upgrade", true);
        PARTICLES = b.comment("The ambient particles and signature bursts of the upgraded weapons.").define("particles", true);
        TRAILS = b.comment("The swing trails of the upgraded weapons.").define("trails", true);
        GUI_MODEL = b.comment("Show the upgraded 3D model (tilted) in inventory slots instead of the old flat sprite. Off keeps the old sprite as the icon.").define("guiModel", false);
        b.pop();
        SPEC = b.build();
    }

    private VuOptions() {}

    public static boolean upgrade() { return get(UPGRADE, true); }

    public static boolean particles() { return get(PARTICLES, true); }

    public static boolean trails() { return get(TRAILS, true); }

    public static boolean guiModel() { return get(GUI_MODEL, false); }

    private static boolean get(ModConfigSpec.BooleanValue v, boolean fallback) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return fallback; }
    }
}
