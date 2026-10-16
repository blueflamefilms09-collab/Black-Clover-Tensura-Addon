package com.newuniverse.nusmp.song;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the little singing-level meter is a convenience, and some players do not want it on screen; this client-only switch hides it.
 * File: nusmp-song-client.toml (register {@link #SPEC} as a CLIENT config).
 */
public final class SongClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue HIDE_HUD;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        HIDE_HUD = b.comment("Hide the singing level meter shown while Song Magic is active.").define("hideHud", false);
        SPEC = b.build();
    }

    private SongClientConfig() {}

    public static boolean hideHud() {
        try { return HIDE_HUD.get(); } catch (IllegalStateException notLoaded) { return false; }
    }
}
