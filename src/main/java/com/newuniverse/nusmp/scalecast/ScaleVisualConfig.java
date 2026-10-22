package com.newuniverse.nusmp.scalecast;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: Scale casting (docs/magic/scale_casting.md) has two looks-only switches, a CLIENT config ({@code config/nusmp-scale-casting-client.toml}, this machine only): {@code visual.scale3d}
 * (default true) draws the floating 3D balance scale; false draws none of it (the server still runs the aura; only the picture is gone). {@code visual.scaleReadout} (default true) is the small
 * weight readout beside the Scale and the plate above the hotbar. Whether the aura exists at all is the SERVER config {@link ScaleCastConfig} ({@code magic.scaleCasting}). No gameplay depends
 * on anything here. Registered from the client HUD class.
 */
public final class ScaleVisualConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue SCALE_3D, READOUT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("visual");
        SCALE_3D = b.comment("Draw the floating 3D balance scale of Scale Magic (beam, pans, chains, glow). Off draws nothing of it; the aura still works (it is decided by the server).")
                .define("scale3d", true);
        READOUT = b.comment("Show the weights the Scale measured: a small readout above the Scale and a plate over the hotbar while your own Scale weighs a foe.")
                .define("scaleReadout", true);
        b.pop();
        SPEC = b.build();
    }

    private ScaleVisualConfig() {}

    /** {@code visual.scale3d}; true while the file is not loaded. */
    public static boolean scale3d() {
        try { return SCALE_3D.get(); } catch (IllegalStateException notLoaded) { return true; }
    }

    public static boolean readout() {
        try { return READOUT.get(); } catch (IllegalStateException notLoaded) { return true; }
    }
}
