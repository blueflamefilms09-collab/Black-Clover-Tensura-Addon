package com.newuniverse.nusmp.mana.zone;

import com.newuniverse.nusmp.mana.zone.ZoneUpRules.OriginMode;

import java.util.Locale;

/**
 * The maths and the rules of the Mana Zone upgrade, with no Minecraft types (unit tested): is an aimed target inside the zone, is a sender too fast, is an aim still fresh, where a
 * zone-cast spell is born, which way the caster must look, which pages may be zone-cast, and the evasion (will a projectile hit within a few ticks, how likely is the dodge, which way to step).
 * Nothing here keeps state and nothing allocates (results are written into caller arrays).
 */
public final class ZoneUpMath {
    private ZoneUpMath() {}

    /** The share of the zone's radius an origin may use (the boundary itself is not "inside" after rounding). */
    public static final double INSIDE = 0.97;
    /** The farthest an {@link OriginMode#EDGE} origin may lie from its target: the pages' own ranges are 24 or so. */
    public static final double MAX_EDGE_RUN = 16.0;
    /** The largest coordinate a packet may carry (a world is 30,000,000 wide). */
    public static final double MAX_COORD = 3.0e7;

    // ---------------------------------------------------------------- the aim packet
    /** Why a server drops an aim packet (OK = keep it). */
    public enum Aim { OK, DISABLED, NO_ZONE, WRONG_DIMENSION, NOT_FINITE, TOO_FAST, OUTSIDE_ZONE }

    public static boolean finite(double... v) {
        for (double d : v) if (Double.isNaN(d) || Double.isInfinite(d) || Math.abs(d) > MAX_COORD) return false;
        return true;
    }

    /** An entity id on the wire: -1 = none, otherwise 0 or more (and far under the int range a world ever reaches). */
    public static boolean entityIdOk(int id) { return id >= -1 && id < (1 << 26); }

    /** True when at least {@code gap} ticks have passed since {@code last}; a clock that went backwards (a reloaded world) counts as passed. */
    public static boolean rateOk(long now, long last, int gap) { return now < last || now - last >= gap; }

    /** True while an aim that arrived at {@code at} is still usable at {@code now}. */
    public static boolean fresh(long now, long at, int ttl) { return now >= at && now - at <= ttl; }

    /**
     * The whole validation of an aim packet, cheapest check first. The zone's centre and radius are the SERVER's (its own record and its owner's position), never the packet's.
     */
    public static Aim checkAim(boolean enabled, boolean hasZone, boolean sameDimension, double tx, double ty, double tz, double cx, double cy, double cz, double radius,
                               double tolerance, long now, long last, int gap) {
        if (!enabled) return Aim.DISABLED;
        if (!hasZone) return Aim.NO_ZONE;
        if (!sameDimension) return Aim.WRONG_DIMENSION;
        if (!finite(tx, ty, tz)) return Aim.NOT_FINITE;
        if (!rateOk(now, last, gap)) return Aim.TOO_FAST;
        return ManaZoneMath.inside(tx - cx, ty - cy, tz - cz, Math.max(0, radius) + Math.max(0, tolerance)) ? Aim.OK : Aim.OUTSIDE_ZONE;
    }

    // ---------------------------------------------------------------- the origin of a zone-cast spell
    /** Pages that move or change the caster itself (a dash, a blink, an armour, a summon, a mode): never zone-cast. A page that is not here and still moves the caster is undone after it ran. */
    private static final String[] SELF_PAGES = {"zone", "dash", "blink", "teleport", "warp", "flight", "fly", "leap", "jump", "dive", "armor", "armour", "mode", "summon", "store", "draw", "union",
            "form", "shift", "step", "spirit_", "devil", "grimoire"};

    public static boolean pageEligible(String id) {
        if (id == null || id.isEmpty()) return false;
        String s = id.toLowerCase(Locale.ROOT);
        for (String bad : SELF_PAGES) if (s.contains(bad)) return false;
        return true;
    }

    /** Pulls a point back inside the sphere of {@code radius * INSIDE} round (cx,cy,cz). */
    public static void clampInside(double cx, double cy, double cz, double radius, double[] p) {
        double dx = p[0] - cx, dy = p[1] - cy, dz = p[2] - cz, r = Math.max(0.0, radius) * INSIDE;
        double len2 = dx * dx + dy * dy + dz * dz;
        if (len2 <= r * r || len2 <= 1e-12) return;
        double k = r / Math.sqrt(len2);
        p[0] = cx + dx * k;
        p[1] = cy + dy * k;
        p[2] = cz + dz * k;
    }

    /**
     * Where a spell aimed at (tx,ty,tz) is born (written into {@code out}, always inside the zone): BEHIND = {@code reach} blocks past the target on the line from the caster's eye
     * (ex,ey,ez); EDGE = on the zone's boundary on the line from the centre through the target, at most {@link #MAX_EDGE_RUN} from it; ABOVE = {@code reach + 1} blocks over it.
     */
    public static void origin(OriginMode mode, double cx, double cy, double cz, double radius, double ex, double ey, double ez, double tx, double ty, double tz, double reach, double[] out) {
        switch (mode == null ? OriginMode.BEHIND : mode) {
            case EDGE -> {
                double dx = tx - cx, dy = ty - cy, dz = tz - cz, len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len < 1e-6) { dx = tx - ex; dy = 0; dz = tz - ez; len = Math.sqrt(dx * dx + dz * dz); }
                if (len < 1e-6) { dx = 0; dy = 0; dz = 1; len = 1; }
                double run = Math.max(0.0, Math.min(MAX_EDGE_RUN, radius * INSIDE - Math.sqrt((tx - cx) * (tx - cx) + (ty - cy) * (ty - cy) + (tz - cz) * (tz - cz))));
                out[0] = tx + dx / len * run;
                out[1] = ty + dy / len * run;
                out[2] = tz + dz / len * run;
            }
            case ABOVE -> {
                out[0] = tx;
                out[1] = ty + reach + 1.0;
                out[2] = tz;
            }
            default -> {
                double dx = tx - ex, dy = ty - ey, dz = tz - ez, len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len < 1e-6) { dx = 0; dy = 0; dz = 1; len = 1; }
                out[0] = tx + dx / len * reach;
                out[1] = ty + dy / len * reach;
                out[2] = tz + dz / len * reach;
            }
        }
        clampInside(cx, cy, cz, radius, out);
    }

    /** The yaw and pitch (degrees, Minecraft's convention) of the look from (ox,oy,oz) to (tx,ty,tz), written into {@code out[0]}, {@code out[1]}. */
    public static void look(double ox, double oy, double oz, double tx, double ty, double tz, float[] out) {
        double dx = tx - ox, dy = ty - oy, dz = tz - oz, h = Math.sqrt(dx * dx + dz * dz);
        out[0] = (float) (Math.atan2(-dx, dz) * (180.0 / Math.PI));
        out[1] = (float) (-Math.atan2(dy, h) * (180.0 / Math.PI));
    }

    // ---------------------------------------------------------------- evasion
    /**
     * The tick (0..horizon, fractional) at which a projectile at (px,py,pz) moving (vx,vy,vz) per tick comes closest to (cx,cy,cz), when that closest approach is within
     * {@code radius}; -1 when it flies away, stands still, or passes wide. A projectile already inside the radius answers 0.
     */
    public static double timeToHit(double px, double py, double pz, double vx, double vy, double vz, double cx, double cy, double cz, double radius, double horizon) {
        double rx = px - cx, ry = py - cy, rz = pz - cz, rr = radius * radius;
        if (rx * rx + ry * ry + rz * rz <= rr) return 0.0;
        double vv = vx * vx + vy * vy + vz * vz;
        if (vv < 1e-8) return -1.0;
        double rv = rx * vx + ry * vy + rz * vz;
        if (rv >= 0) return -1.0;                                              // moving away
        double t = -rv / vv;
        if (t > horizon) return -1.0;
        double qx = rx + vx * t, qy = ry + vy * t, qz = rz + vz * t;
        return qx * qx + qy * qy + qz * qz <= rr ? t : -1.0;
    }

    /** The chance one threatening projectile is dodged: the base, up to +25% with full Reinforcement mastery, halved against a boss's attack, never over 95%. */
    public static double dodgeChance(double base, double reinforcement, boolean bossProjectile) {
        double r = Math.max(0.0, Math.min(1.0, reinforcement));
        double c = (base + 0.25 * r) * (bossProjectile ? 0.5 : 1.0);
        return Math.max(0.0, Math.min(0.95, c));
    }

    /**
     * The horizontal sidestep (x, z) of length {@code impulse}: perpendicular to the projectile's flight, towards the side the owner already stands on; a projectile that falls
     * straight down gets a step away from where it falls; {@code tie} (0 or 1) decides when the owner is exactly on the line.
     */
    public static void sidestep(double vx, double vz, double rx, double rz, double impulse, int tie, double[] out) {
        double vh = Math.sqrt(vx * vx + vz * vz);
        double nx, nz;
        if (vh > 1e-4) {
            nx = -vz / vh;
            nz = vx / vh;
            double side = rx * nx + rz * nz;
            if (Math.abs(side) < 1e-6) side = tie == 0 ? 1.0 : -1.0;
            if (side < 0) { nx = -nx; nz = -nz; }
        } else {
            double rh = Math.sqrt(rx * rx + rz * rz);
            if (rh > 1e-4) { nx = rx / rh; nz = rz / rh; }
            else { nx = tie == 0 ? 1.0 : -1.0; nz = 0.0; }
        }
        out[0] = nx * impulse;
        out[1] = nz * impulse;
    }
}
