package com.newuniverse.nusmp.vfx.client.portal;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the 3D portal is a look, so its switches are client-side: {@code spatial.portal3d} falls back to the old flat gate on any
 * machine that dislikes the new one, {@code spatial.warpStrength} scales (or removes) the full-screen ripple near a gate, and the
 * refraction and particle switches let weak GPUs keep the shape without the expensive parts. Plain {@code define} values only. The
 * lead registers {@link #SPEC} as a CLIENT config; loading this class installs it as the {@link PortalSettings} source.
 * File: config/nusmp-spatial-portal.toml
 */
public final class SpatialPortalConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue PORTAL_3D;
    public static final ModConfigSpec.DoubleValue WARP_STRENGTH;
    public static final ModConfigSpec.BooleanValue REFRACTION;
    public static final ModConfigSpec.BooleanValue PARTICLES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("spatial");
        PORTAL_3D = b.comment("Draw spatial gates as the 3D animated portal. False keeps the old flat portal.").define("portal3d", true);
        WARP_STRENGTH = b.comment("Strength of the faint full-screen warp ripple when you stand within 3 blocks of a gate (0 = off, 1 = strongest).")
                .defineInRange("warpStrength", 0.35, 0.0, 1.0);
        REFRACTION = b.comment("Bend a copy of the screen through the event horizon (needs the portal shader; costs one screen copy per frame while a gate is within 16 blocks).")
                .define("refraction", true);
        PARTICLES = b.comment("Emit Rukh at the event horizon.").define("portalParticles", true);
        b.pop();
        SPEC = b.build();
        PortalSettings.source = new PortalSettings.Source() {
            @Override public boolean portal3d() { return get(PORTAL_3D, true); }
            @Override public float warpStrength() { return get(WARP_STRENGTH, 0.35).floatValue(); }
            @Override public boolean refraction() { return get(REFRACTION, true); }
            @Override public boolean particles() { return get(PARTICLES, true); }
        };
    }

    private SpatialPortalConfig() {}

    private static <T> T get(ModConfigSpec.ConfigValue<T> v, T fallback) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return fallback; }
    }
}
