package com.newuniverse.nusmp.heroweapon.weapon.shield;

/**
 * Why: the Air Strike Shield is a floating shield, and in a game with no entity for it the shield is a rectangle of space with a facing: an arrow, a fireball or a
 * foe that reaches it is stopped. Whether a fast projectile crossed that rectangle during the last tick is geometry (a segment against a thin slab), and getting it
 * wrong in either direction ruins the skill (an arrow that tunnels through at three blocks a tick, or a shield that stops things beside it), so the geometry is
 * pure maths, with no Minecraft types, tested alone. The panel stands upright: its normal is horizontal, it is {@code halfWidth} wide to either side of the centre
 * and {@code halfHeight} up and down, and it is {@code thickness} thick to either side of its plane.
 */
public final class PanelMath {
    private PanelMath() {}

    /** A panel: the centre, the horizontal unit normal (nx, nz), the half width, the half height and the half thickness. */
    public record Panel(double cx, double cy, double cz, double nx, double nz, double halfWidth, double halfHeight, double thickness) {
        /** A panel at the centre facing the horizontal direction (dx, dz); a zero direction faces +z. */
        public static Panel facing(double cx, double cy, double cz, double dx, double dz, double halfWidth, double halfHeight, double thickness) {
            double len = Math.sqrt(dx * dx + dz * dz);
            double nx = len < 1.0E-9 ? 0.0 : dx / len, nz = len < 1.0E-9 ? 1.0 : dz / len;
            return new Panel(cx, cy, cz, nx, nz, halfWidth, halfHeight, thickness);
        }

        /** The signed distance of a point from the panel's plane (positive on the side the normal points to). */
        public double depth(double x, double z) { return (x - cx) * nx + (z - cz) * nz; }

        /** How far along the panel's width the point lies from the centre (the horizontal direction at right angles to the normal). */
        public double across(double x, double z) { return -(x - cx) * nz + (z - cz) * nx; }

        /** True if the point is inside the thickened rectangle. */
        public boolean contains(double x, double y, double z) {
            return Math.abs(depth(x, z)) <= thickness && Math.abs(across(x, z)) <= halfWidth && Math.abs(y - cy) <= halfHeight;
        }
    }

    /**
     * True if something that moved in one tick from (ax, ay, az) to (bx, by, bz) was stopped by the panel: either an end of the move lies inside the thickened
     * rectangle, or the move crossed the plane at a point inside the rectangle.
     */
    public static boolean crosses(Panel p, double ax, double ay, double az, double bx, double by, double bz) {
        if (p.contains(ax, ay, az) || p.contains(bx, by, bz)) return true;
        double da = p.depth(ax, az), db = p.depth(bx, bz);
        if ((da > 0) == (db > 0) || da == db) return false;                           // both on the same side: it did not cross
        double s = da / (da - db);                                                     // where along the move the plane is crossed
        double x = ax + (bx - ax) * s, y = ay + (by - ay) * s, z = az + (bz - az) * s;
        return Math.abs(p.across(x, z)) <= p.halfWidth() && Math.abs(y - p.cy()) <= p.halfHeight();
    }
}
