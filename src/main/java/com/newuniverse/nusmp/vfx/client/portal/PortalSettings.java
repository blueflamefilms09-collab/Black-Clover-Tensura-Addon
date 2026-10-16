package com.newuniverse.nusmp.vfx.client.portal;

/**
 * Why: the portal drawing code runs in the headless preview as well as in the game, where there is no mod config to read, so it asks
 * this plain holder instead. {@code SpatialPortalConfig} (the real client config) replaces {@link #source} when it loads; until then,
 * and in the preview, the defaults here apply.
 */
public final class PortalSettings {
    private PortalSettings() {}

    /** Where the values come from; every call must be safe before the config has loaded. */
    public interface Source {
        boolean portal3d();
        float warpStrength();
        boolean refraction();
        boolean particles();
    }

    private static final Source DEFAULTS = new Source() {
        @Override public boolean portal3d() { return true; }
        @Override public float warpStrength() { return 0.35f; }
        @Override public boolean refraction() { return true; }
        @Override public boolean particles() { return true; }
    };

    public static volatile Source source = DEFAULTS;

    /** {@code spatial.portal3d}: false draws the old flat portal. */
    public static boolean portal3d() { return source.portal3d(); }
    /** {@code spatial.warpStrength} clamped to 0..1. */
    public static float warpStrength() { return Math.max(0f, Math.min(1f, source.warpStrength())); }
    /** {@code spatial.refraction}: the screen-copy look-through pass. */
    public static boolean refraction() { return source.refraction(); }
    /** {@code spatial.portalParticles}: Rukh at the event horizon. */
    public static boolean particles() { return source.particles(); }
}
