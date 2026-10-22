package com.newuniverse.nusmp.compass3d;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the owner asked for Compass Magic to be one 3D compass with every spell coming out of it, and for nothing to be removed: {@code visual.compass3d} (a CLIENT config,
 * config/nusmp-compass3d-client.toml, this machine only, default true) draws the 3D compass and its 3D spell props and hides the old flat effects (the brass compasses in an arc, the flat needle,
 * the flat lock sigil); false draws exactly the old flat effects and none of the 3D props. The server sends both (the old VFX payloads as before, the props as entities); only the client
 * chooses what to draw, so it is a looks-only setting and no gameplay depends on it. {@code visual.compass3d_aura} (default true) is the spiky yellow aura card behind the compass
 * and the glint that runs round its rim. Registered from NUSMP (client side only).
 */
public final class CompassVisualConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue COMPASS_3D, AURA;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("visual");
        COMPASS_3D = b.comment("Compass Magic as one floating 3D compass beside the caster with every spell coming out of it as a 3D model (needles, seal discs, routes). "
                        + "Off = the old flat effects (compasses in an arc behind the caster, flat needle, flat lock sigil) and no 3D props.")
                .define("compass3d", true);
        AURA = b.comment("The spiky yellow aura behind the 3D compass and the glint that runs round its rim (only with compass3d on).")
                .define("compass3d_aura", true);
        b.pop();
        SPEC = b.build();
    }

    private CompassVisualConfig() {}

    /** {@code visual.compass3d}; true while the file is not loaded (the safe default is the new look). */
    public static boolean compass3d() {
        try { return COMPASS_3D.get(); } catch (IllegalStateException notLoaded) { return true; }
    }

    public static boolean aura() {
        try { return AURA.get(); } catch (IllegalStateException notLoaded) { return true; }
    }
}
