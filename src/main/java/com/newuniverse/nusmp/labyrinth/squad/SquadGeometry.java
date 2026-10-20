package com.newuniverse.nusmp.labyrinth.squad;

/**
 * Why: where a tank stands, where a flanker circles to, how far a healer keeps back and where a follower takes its place are all POINTS on the floor, and a point is
 * arithmetic on two numbers (the dungeon is walked, not flown, so the squad thinks in x and z). Keeping that arithmetic here, with no Minecraft type in sight, is what
 * lets every rule be tested to the decimal ("the intercept point is 1.5 blocks from the ward on the segment to the target", "a flank point is 110 to 150 degrees off
 * the target's facing") instead of being judged by eye in a game nobody can run during the build. The goals only turn a live entity into a {@link V2}, ask this class,
 * and walk to the answer.
 *
 * <p>Angles follow Minecraft: a yaw of 0 faces +z (south), 90 faces -x (west); the facing vector of a yaw is {@code (-sin, cos)}, and the right-hand vector is
 * {@code (-cos, -sin)}. An angle "off the facing" is measured from the facing vector: 0 is straight ahead of the target, 180 is straight behind it, and the sign picks
 * the side (positive turns the same way a positive yaw turns).
 */
public final class SquadGeometry {
    private SquadGeometry() {}

    /** A point or a vector on the floor plane. */
    public record V2(double x, double z) {
        public V2 plus(V2 o) { return new V2(x + o.x, z + o.z); }

        public V2 minus(V2 o) { return new V2(x - o.x, z - o.z); }

        public V2 times(double k) { return new V2(x * k, z * k); }

        public double len() { return Math.sqrt(x * x + z * z); }

        public double dist(V2 o) { return minus(o).len(); }

        public double dot(V2 o) { return x * o.x + z * o.z; }

        /** The unit vector in this direction, or the zero vector when it has no length. */
        public V2 unit() {
            double l = len();
            return l < 1e-9 ? new V2(0, 0) : new V2(x / l, z / l);
        }
    }

    /** The unit facing vector of a yaw in degrees. */
    public static V2 facing(double yawDeg) {
        double r = Math.toRadians(yawDeg);
        return new V2(-Math.sin(r), Math.cos(r));
    }

    /** The unit vector to the right-hand side of a yaw in degrees. */
    public static V2 right(double yawDeg) {
        double r = Math.toRadians(yawDeg);
        return new V2(-Math.cos(r), -Math.sin(r));
    }

    /** The yaw in degrees (Minecraft's convention) that faces from {@code from} toward {@code to}; 0 when the two are the same point. */
    public static double yawToward(V2 from, V2 to) {
        V2 d = to.minus(from);
        if (d.len() < 1e-9) return 0.0;
        return Math.toDegrees(Math.atan2(-d.x(), d.z()));
    }

    /** The smallest signed difference between two angles in degrees, in (-180, 180]. */
    public static double angleDiff(double a, double b) {
        double d = (a - b) % 360.0;
        if (d > 180.0) d -= 360.0;
        if (d <= -180.0) d += 360.0;
        return d;
    }

    /** A point {@code forward} blocks ahead of {@code centre} along the yaw and {@code lateral} to its right (negative = left): the world place of a formation slot. */
    public static V2 slot(V2 centre, double yawDeg, double forward, double lateral) {
        return centre.plus(facing(yawDeg).times(forward)).plus(right(yawDeg).times(lateral));
    }

    // ---------------------------------------------------------------- segments
    /** Where along the segment a-b the point p projects, 0 at a and 1 at b, held to that range. A zero-length segment answers 0. */
    public static double projectT(V2 p, V2 a, V2 b) {
        V2 ab = b.minus(a);
        double l2 = ab.dot(ab);
        if (l2 < 1e-12) return 0.0;
        return Math.max(0.0, Math.min(1.0, p.minus(a).dot(ab) / l2));
    }

    /** The point of the segment a-b nearest to p. */
    public static V2 closestOnSegment(V2 p, V2 a, V2 b) {
        return a.plus(b.minus(a).times(projectT(p, a, b)));
    }

    /** The distance from p to the segment a-b. */
    public static double distToSegment(V2 p, V2 a, V2 b) {
        return p.dist(closestOnSegment(p, a, b));
    }

    // ---------------------------------------------------------------- the tank
    /**
     * The place a tank walks to: {@code ahead} blocks from the ward on the segment ward-to-target. When the target is closer than twice that, the middle of the
     * segment (a tank must never stand beyond the target). With the target on top of the ward it is the ward's own place (nothing to step in front of).
     */
    public static V2 interceptPoint(V2 ward, V2 target, double ahead) {
        V2 d = target.minus(ward);
        double len = d.len();
        if (len < 1e-9) return ward;
        double step = Math.min(Math.max(0.0, ahead), len / 2.0);
        return ward.plus(d.times(step / len));
    }

    /**
     * Is the tank already covering the ward: is it within {@code tolerance} blocks of the segment ward-target, and not behind the ward (its projection is past the
     * ward's own end)? A tank behind the ward is not between it and the target, however near the line it stands.
     */
    public static boolean covers(V2 tank, V2 ward, V2 target, double tolerance) {
        if (distToSegment(tank, ward, target) > tolerance) return false;
        return projectT(tank, ward, target) > 0.0 || tank.dist(ward) <= tolerance;
    }

    // ---------------------------------------------------------------- the flanker
    /** The place on the circle round the target at {@code relDeg} degrees off the target's facing (see the class comment) and {@code radius} blocks away. */
    public static V2 flankPoint(V2 target, double targetYawDeg, double relDeg, double radius) {
        return target.plus(facing(targetYawDeg + relDeg).times(radius));
    }

    /**
     * The signed angle off the facing a flanker of this index aims for: between {@code MIN_FLANK} and {@code MAX_FLANK} degrees, the side alternating with the index
     * (so two flankers come from opposite flanks) and the magnitude stepping through the band by the golden ratio (so they never choose the same angle). A
     * {@code flip} turns the side round (the flanker that was seen tries the other flank).
     */
    public static double flankAngle(int index, boolean flip) {
        double frac = (index * 0.6180339887498949) % 1.0;
        double mag = MIN_FLANK + (MAX_FLANK - MIN_FLANK) * frac;
        boolean left = (index % 2 == 0) != flip;
        return left ? mag : -mag;
    }

    public static final double MIN_FLANK = 110.0, MAX_FLANK = 150.0;

    /** Is {@code me} inside the cone the target is looking along, within {@code maxDist}? ({@code coneDeg} is the full width of the cone.) */
    public static boolean seenBy(V2 target, double targetYawDeg, V2 me, double coneDeg, double maxDist) {
        V2 d = me.minus(target);
        double dist = d.len();
        if (dist > maxDist) return false;
        if (dist < 1e-9) return true;
        double off = Math.abs(angleDiff(yawToward(target, me), targetYawDeg));
        return off <= coneDeg / 2.0;
    }

    /** How many degrees off the target's facing a point stands, 0 (in front) to 180 (behind). */
    public static double offFacing(V2 target, double targetYawDeg, V2 p) {
        if (p.minus(target).len() < 1e-9) return 0.0;
        return Math.abs(angleDiff(yawToward(target, p), targetYawDeg));
    }

    // ---------------------------------------------------------------- the healer
    /**
     * Where a healer or buffer goes. Inside {@code minDist} of the nearest threat it backs away, along the line from the threat, bent toward the squad's centre
     * (a healer that flees into a corner is a dead healer; it flees to its friends) and never closer to the threat than where it stands; beyond {@code maxDist} it
     * comes back toward the centre; between the two it stays where it is (null). The point returned is {@code (minDist + maxDist) / 2} from the threat when
     * backing away, so one step is enough to be comfortable again.
     */
    public static V2 retreatPoint(V2 self, V2 threat, V2 centre, double minDist, double maxDist) {
        double d = self.dist(threat);
        if (d >= minDist && d <= maxDist) return null;
        double want = (minDist + maxDist) / 2.0;
        if (d < minDist) {
            V2 away = self.minus(threat).unit();
            if (away.len() < 1e-9) away = centre.minus(threat).unit();
            V2 toCentre = centre.minus(self).unit();
            V2 dir = away.times(0.7).plus(toCentre.times(0.3)).unit();
            if (dir.len() < 1e-9) dir = away;
            V2 p = threat.plus(dir.times(want));
            if (p.dist(threat) < d) p = threat.plus(away.times(want));
            return p;
        }
        V2 toThreat = threat.minus(centre).unit();
        return threat.minus(toThreat.times(want));
    }

    // ---------------------------------------------------------------- the follower
    /** The middle of a group of points; the origin for an empty group. */
    public static V2 centroid(Iterable<V2> points) {
        double x = 0, z = 0;
        int n = 0;
        for (V2 p : points) {
            x += p.x();
            z += p.z();
            n++;
        }
        return n == 0 ? new V2(0, 0) : new V2(x / n, z / n);
    }
}
