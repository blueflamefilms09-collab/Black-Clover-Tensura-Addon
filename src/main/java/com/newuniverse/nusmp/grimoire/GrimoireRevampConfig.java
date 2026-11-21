package com.newuniverse.nusmp.grimoire;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the owner asked for the grimoire books to be rebuilt (a Blender-made 3D model and a richer summon / cast effect) and for the old look to stay available:
 * {@code visual.classicGrimoires} (a CLIENT config, config/nusmp-grimoire-client.toml, this machine only, default false) draws the old model (GrimoireBookPlan) and none of the new
 * effects (rune ring, glow halo, page flutter, cast flare). The server sends nothing different either way; only this client chooses what to draw, so it is a looks-only setting.
 * Registered from NUSMP (client side only).
 */
public final class GrimoireRevampConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue CLASSIC;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("visual");
        CLASSIC = b.comment("Draw the classic grimoires: the old box-built book model and the old summon / cast look, without the revamp's rune ring, glow halo, page flutter and cast flare. "
                + "Off (the default) = the revamped grimoire. Takes effect at once, no restart.")
                .define("classicGrimoires", false);
        b.pop();
        SPEC = b.build();
    }

    private GrimoireRevampConfig() {}

    /** {@code visual.classicGrimoires}; false while the file is not loaded (the safe default is the new look). */
    public static boolean classic() {
        try { return CLASSIC.get(); } catch (IllegalStateException notLoaded) { return false; }
    }
}
