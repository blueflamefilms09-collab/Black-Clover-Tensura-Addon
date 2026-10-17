package com.newuniverse.nusmp.labyrinth.mega.macro;

/**
 * Why: the shape of the Hub depends on a handful of tuning numbers (the size of a Wave Function Collapse cell, how rough the Spire's shell and the Chasm's walls
 * are, how tall the rolling hills of the plains are, how many solver restarts are allowed). They live in the server config ({@link MacroConfig}), but the pure
 * model must not import the game, so the model is built from this plain immutable record instead and the generator copies the config into it. A record gives
 * the per-seed cache a correct key for free: two worlds with the same seed and the same numbers share one model, a server that changes a number gets a new one.
 * Pure: no Minecraft import.
 *
 * @param cell WFC cell size in blocks (the tile patterns are authored at 6 and resampled to this size)
 * @param spireNoise how many blocks the 3D noise may move the outside of the Spire's shell
 * @param chasmNoise how many blocks the 3D noise may push rock ribs out of the Chasm's wall
 * @param hillAmplitude the height of the plains' rolling hills in blocks
 * @param wfcTries how many restarts the solver may use before the documented plain-tile fallback
 * @param landmarks false = no Spire and no Chasm: plain plains with the two gates standing on small platforms at their {@link com.newuniverse.nusmp.labyrinth.mega.HubLayout}
 *                  positions (the switch that turns the landmark code off if it ever misbehaves; the Hub still works)
 */
public record MacroParams(int cell, double spireNoise, double chasmNoise, double hillAmplitude, int wfcTries, boolean landmarks) {
    /** The shipped defaults (also the numbers every unit test and the preview use). */
    public static final MacroParams DEFAULT = new MacroParams(6, 2.0, 3.0, 5.0, 12, true);

    public MacroParams {
        cell = Math.max(4, Math.min(8, cell));
        spireNoise = Math.max(0.0, Math.min(4.0, spireNoise));
        chasmNoise = Math.max(0.0, Math.min(6.0, chasmNoise));
        hillAmplitude = Math.max(0.0, Math.min(12.0, hillAmplitude));
        wfcTries = Math.max(1, Math.min(64, wfcTries));
    }
}
