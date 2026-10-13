package com.newuniverse.nusmp.dungeon.nexus.nav;

/**
 * The smoothing of a compass needle, the same spring vanilla's compass uses: the needle chases the wobble amount it is given, once per
 * game tick, gaining speed towards it and losing a fifth of that speed each tick, so it swings and settles instead of snapping. Pure.
 */
public final class NeedleWobble {
    private double rotation, delta;
    private long lastTick = Long.MIN_VALUE;

    /** Has this game tick not been taken into account yet? (The needle is asked once per frame but moves once per tick.) */
    public boolean shouldUpdate(long gameTime) { return lastTick != gameTime; }

    /** One step towards {@code amount} (in turns, 0 to 1). */
    public void update(long gameTime, double amount) {
        lastTick = gameTime;
        double d = amount - rotation;
        d = CompassMath.wrap01(d + 0.5) - 0.5;                 // the short way round
        delta += d * 0.1;
        delta *= 0.8;
        rotation = CompassMath.wrap01(rotation + delta);
    }

    /** Where the needle is now, in turns. */
    public double rotation() { return rotation; }
}
