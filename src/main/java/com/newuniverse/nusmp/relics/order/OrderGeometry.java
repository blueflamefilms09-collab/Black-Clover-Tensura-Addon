package com.newuniverse.nusmp.relics.order;

/** The small maths of the God's Writ and its facing (pure, unit tested). */
public final class OrderGeometry {
    private OrderGeometry() {}

    /** Distance in the horizontal plane from the point (px, pz) to the segment a-b. */
    public static double distanceToSegment(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az;
        double len2 = dx * dx + dz * dz;
        if (len2 < 1e-12) return Math.hypot(px - ax, pz - az);
        double t = ((px - ax) * dx + (pz - az) * dz) / len2;
        t = Math.max(0.0, Math.min(1.0, t));
        return Math.hypot(px - (ax + t * dx), pz - (az + t * dz));
    }

    /**
     * Is the point in the corridor that starts at (ox, oy, oz) and runs 'length' blocks in the horizontal direction (dx, dz) (unit length),
     * 'halfWidth' to each side and 'vertical' above and below?
     */
    public static boolean inCorridor(double px, double py, double pz, double ox, double oy, double oz, double dx, double dz, double length,
                                     double halfWidth, double vertical) {
        if (Math.abs(py - oy) > vertical) return false;
        return distanceToSegment(px, pz, ox, oz, ox + dx * length, oz + dz * length) <= halfWidth;
    }

    /** The yaw in degrees (Minecraft's convention: 0 faces +z, 90 faces -x) of an entity at the origin that looks towards (dx, dz). */
    public static float yawToward(double dx, double dz) {
        return (float) (Math.toDegrees(Math.atan2(-dx, dz)));
    }
}
