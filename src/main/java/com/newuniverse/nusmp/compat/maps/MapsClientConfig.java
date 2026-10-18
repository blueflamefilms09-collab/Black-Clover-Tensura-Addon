package com.newuniverse.nusmp.compat.maps;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: how much of the top-right corner a minimap takes is a taste of the player's screen, so it is a CLIENT config, {@code nusmp-compat-maps-client.toml}, section
 * {@code [compat.maps]}. Read by the client helper {@code MapHudAvoid}; the readers answer their defaults while the spec is not loaded.
 */
public final class MapsClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MINIMAP_SIZE;
    public static final ModConfigSpec.BooleanValue AVOID_MINIMAP;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("compat").push("maps");
        MINIMAP_SIZE = b.comment("Size in GUI pixels of the square a minimap (Xaero's, FTB Chunks, JourneyMap, VoxelMap) takes in the TOP-RIGHT corner. Hud elements of this mod that default to "
                        + "the top-right corner stay clear of it. A position you chose yourself is never moved.")
                .defineInRange("minimapSize", 140, 40, 400);
        AVOID_MINIMAP = b.comment("Keep this mod's top-right hud elements clear of a detected minimap.").define("avoidMinimap", true);
        b.pop(2);
        SPEC = b.build();
    }

    private MapsClientConfig() {}

    public static int minimapSize() {
        try { return MINIMAP_SIZE.get(); } catch (IllegalStateException | NullPointerException notLoaded) { return 140; }
    }

    public static boolean avoidMinimap() {
        try { return AVOID_MINIMAP.get(); } catch (IllegalStateException | NullPointerException notLoaded) { return true; }
    }
}
