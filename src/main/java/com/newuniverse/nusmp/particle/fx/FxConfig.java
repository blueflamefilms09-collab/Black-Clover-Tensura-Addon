package com.newuniverse.nusmp.particle.fx;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the owner (and a server operator) must be able to switch the custom particles off and get the old look back: {@code nusmp-particles-client.toml} ({@code particles.custom}, this
 * machine only: client-spawned particles and the way server particles are shown) and {@code nusmp-particles-server.toml} ({@code particles.custom}, what the server sends). Both default to
 * true. With a switch off, {@link Fx} spawns the old vanilla particle of the mapping instead of the custom one. Hot code reads the two volatile booleans, which {@link #apply} refreshes when a
 * file loads or changes (reading a config value before the file is loaded throws, so every read is guarded).
 */
public final class FxConfig {
    private FxConfig() {}

    public static final ModConfigSpec CLIENT;
    public static final ModConfigSpec SERVER;

    private static final ModConfigSpec.BooleanValue CLIENT_CUSTOM;
    private static final ModConfigSpec.DoubleValue UPGRADE_DENSITY;
    private static final ModConfigSpec.BooleanValue SERVER_CUSTOM;

    private static volatile boolean clientCustom = true;
    private static volatile float upgradeDensity = 0.5f;
    private static volatile boolean serverCustom = true;
    private static volatile Boolean clientForce;
    private static volatile Boolean serverForce;

    static {
        ModConfigSpec.Builder c = new ModConfigSpec.Builder();
        c.push("particles");
        CLIENT_CUSTOM = c.comment("Use the mod's own custom particles (embers, runes, feathers, ...). Off = the old vanilla particles they replaced. Applies to particles this client spawns and to the way it shows server particles.")
                .define("custom", true);
        UPGRADE_DENSITY = c.comment("How many particles the upgraded weapons and relics (ambient motes, swing trails, act bursts, tiny Djinn) spawn: 0.0 none, 0.5 the default (half of what the item tables ask for), 1.0 everything the tables ask for. "
                        + "They are also capped per item, per player and in total, and scaled by the effect quality, the frame rate and the distance (see docs/visual_upgrade/upgrade_particles.md).")
                .defineInRange("upgradeDensity", 0.5, 0.0, 1.0);
        c.pop();
        CLIENT = c.build();
        ModConfigSpec.Builder s = new ModConfigSpec.Builder();
        s.push("particles");
        SERVER_CUSTOM = s.comment("Send the mod's own custom particles from the server. Off = the server sends the old vanilla particles they replaced (for players without the custom textures, or to save bandwidth).")
                .define("custom", true);
        s.pop();
        SERVER = s.build();
    }

    /** Copies the files into the fast fields; safe to call at any time (a file that is not loaded keeps the previous value). */
    public static void apply() {
        try {
            clientCustom = CLIENT_CUSTOM.get();
        } catch (IllegalStateException | NullPointerException ignored) {
            // not loaded yet
        }
        try {
            upgradeDensity = (float) (double) UPGRADE_DENSITY.get();
        } catch (IllegalStateException | NullPointerException ignored) {
            // not loaded yet
        }
        try {
            serverCustom = SERVER_CUSTOM.get();
        } catch (IllegalStateException | NullPointerException ignored) {
            // not loaded yet
        }
    }

    /** May this client spawn custom particles? */
    public static boolean clientCustom() {
        Boolean f = clientForce;
        return f != null ? f : clientCustom;
    }

    /** {@code particles.upgradeDensity} (0 to 1, default 0.5): the share of the table rates the upgraded items may spawn. Client file; the default while the file is not loaded. */
    public static float upgradeDensity() {
        return upgradeDensity;
    }

    /** May the server send custom particles? */
    public static boolean serverCustom() {
        Boolean f = serverForce;
        return f != null ? f : serverCustom;
    }

    /**
     * For options that are built as plain values (a particle handed to a helper) and not sent through a facade call: on a dedicated server the server switch decides; on a client (single
     * player included) both switches must be on, so switching either side off brings the old particles back.
     */
    public static boolean valueCustom() {
        if (net.neoforged.fml.loading.FMLEnvironment.dist != net.neoforged.api.distmarker.Dist.CLIENT) return serverCustom();
        return serverCustom() && clientCustom();
    }

    /** Self-test hook: forces a side on or off (null = follow the file again). */
    public static void force(boolean server, Boolean value) {
        if (server) serverForce = value;
        else clientForce = value;
    }
}
