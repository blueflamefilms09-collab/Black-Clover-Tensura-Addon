package com.newuniverse.nusmp.fairytail.storm;

/**
 * Why: a storm is not one effect, it is a few weather parts laid on top of each other. Each spell of the Stormcaller Staff is a mask of these parts, and the same mask is
 * sent to the clients (inside the effect's seed, see {@link StormMath#packSeed}) so the sky they draw has exactly the parts the server runs. Pure; the bits only ever grow.
 */
public final class StormComp {
    private StormComp() {}

    /** A curtain of rain under the cloud: wets, slows, puts out fire, a light patter of damage. */
    public static final int RAIN = 1;
    /** Hail stones falling on the area in volleys. */
    public static final int HAIL = 2;
    /** Forked bolts that strike from the cloud (real damage, no vanilla lightning entity: no fire, no charged creepers). */
    public static final int BOLTS = 4;
    /** Gust rings that roll out from the middle of a big cell and push everything outward. */
    public static final int GUST = 8;
    /** A funnel that hangs from the cloud, drifts along the ground and draws things in. */
    public static final int FUNNEL = 16;
    /** The calm eye in the middle of a big cell: friends in it are looked after. */
    public static final int EYE = 32;
    /** One gust front: a wall of wind that sweeps along a line and shoves everything on it. */
    public static final int FRONT = 64;
    /** One storm surge: a roll of rain water that runs along the ground. */
    public static final int SURGE = 128;

    public static boolean has(int mask, int part) { return (mask & part) == part; }
}
