package com.newuniverse.nusmp.arc.sds.kit;

/**
 * Why: a lane that follows its target for the whole wind-up can never be dodged, and a lane locked at the first tick is dodged by anyone who simply steps aside once;
 * the fair middle, used by every lane of the four Sins (Chastiefol's throw, Queen Embrace, Rewrite Light), is "track, then lock": the lane follows the target for the
 * first part of the wind-up and stops at a set fraction of it, and the telegraph turns white when it does so the player SEES the moment. This pure object holds the
 * origin and direction of such a lane and the lock; the glue feeds it the numbers each telegraph tick and reads the lane back when the move fires.
 */
public final class AimLock {
    private final double lockFraction;
    private double ox, oy, oz, dx, dz;
    private boolean has, locked;

    /** A lane that locks when the share of the wind-up still to run falls to {@code lockFraction} (0.4 = the last 40% of the wind-up is locked). */
    public AimLock(double lockFraction) { this.lockFraction = Math.max(0.0, Math.min(1.0, lockFraction)); }

    /**
     * One telegraph tick: while not yet locked, takes the origin and aims at (toX, toZ); locks once {@code left <= total * lockFraction}. Returns whether the lane is
     * locked now. A target on top of the origin keeps the previous direction (or +z if there is none).
     */
    public boolean update(int left, int total, double originX, double originY, double originZ, double toX, double toZ) {
        if (locked) return true;
        ox = originX;
        oy = originY;
        oz = originZ;
        double ax = toX - originX, az = toZ - originZ, d = Math.hypot(ax, az);
        if (d > 1e-6) {
            dx = ax / d;
            dz = az / d;
        } else if (!has) {
            dx = 0;
            dz = 1;
        }
        has = true;
        if (left <= total * lockFraction) locked = true;
        return locked;
    }

    public boolean has() { return has; }

    public boolean locked() { return locked; }

    /** The unit direction {dx, dz} of the lane (+z until it was first aimed). */
    public double[] dir() { return has ? new double[]{dx, dz} : new double[]{0, 1}; }

    /** The origin {x, y, z} of the lane. */
    public double[] origin() { return new double[]{ox, oy, oz}; }

    /** Forgets the lane (a new move, a fresh fight). */
    public void reset() {
        has = false;
        locked = false;
        dx = 0;
        dz = 1;
    }
}
