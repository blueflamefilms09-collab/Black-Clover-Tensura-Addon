package com.newuniverse.nusmp.dungeon.floor.goliath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The arithmetic behind the Goliath's moves (pure, unit tested): the ring waves of the slam, the stomp, the roar and the quake, the line of
 * the Seismic Line, where the Bedrock Fist lands, the circles of the Rubble Fall, and the budget that keeps a move of several hits from
 * taking more than its share of a victim's health. {@link GoliathMoves} applies the numbers to the world; nothing here knows what a block
 * or an entity is.
 */
public final class GoliathMath {
    private GoliathMath() {}

    // ---------------------------------------------------------------- ring waves
    /** The radius of a ring wave `age` ticks after it began, running at `speed` blocks a tick. */
    public static double waveRadius(int age, double speed) { return GoliathTable.WAVE_START + Math.max(0, age) * speed; }

    /** The ticks a ring wave lives to cover `reach` blocks (and a little more, so the last foe at the edge is passed). */
    public static int waveTicks(double reach, double speed) { return (int) Math.ceil((reach + GoliathTable.WAVE_THICK - GoliathTable.WAVE_START) / speed) + 1; }

    /** Is something `dist` blocks from the middle being passed by a wave of radius `waveR` right now? */
    public static boolean waveHits(double dist, double waveR) { return Math.abs(dist - waveR) <= GoliathTable.WAVE_THICK; }

    /** Has a foe whose feet are `feetAboveFloor` above the floor under them jumped a ring wave? */
    public static boolean jumpsWave(double feetAboveFloor) { return feetAboveFloor > GoliathTable.WAVE_JUMP; }

    /**
     * Is a foe at height `dy` above the plane of the wave (the floor the giant stands on) inside its reach? A ring on the floor does not
     * reach a foe up on a gallery or down in a pit: from a little under the floor to just over a jump.
     */
    public static boolean inBand(double dy) { return dy >= -1.5 && dy <= 2.6; }

    /**
     * The ring the foes are shown for a wave that will reach `reach`: where its edge is `ticksLeft` of `total` ticks before it lands (it starts at
     * the middle and grows to the reach).
     */
    public static double tellRadius(double reach, int ticksLeft, int total) {
        double done = (total - Math.max(0, Math.min(ticksLeft, total))) / (double) Math.max(1, total);
        return Math.max(1.5, reach * done);
    }

    // ---------------------------------------------------------------- the line of the Seismic Line
    /** How long the line is for a foe `distance` blocks away: it runs past them a little, never under 6 or over 30. */
    public static double lineLength(double distance) { return Math.max(6.0, Math.min(30.0, distance + 4.0)); }

    /** The distance along the line at which the k-th stretch (k = 1, 2 ...) of the spikes rises: two blocks apart. */
    public static double stretchAlong(int k) { return 2.0 * k; }

    /** How many stretches a line of this length has. */
    public static int stretches(double length) { return (int) Math.floor(length / 2.0); }

    /** The tick (after the line begins) at which the k-th stretch rises. */
    public static int stretchDelay(int k) { return k * GoliathTable.LINE_STEP; }

    /**
     * The position of (px, pz) in the frame of a line that starts at (ox, oz) and runs along the unit vector (dx, dz): {along, lateral}, lateral
     * positive to the left of the direction.
     */
    public static double[] lineFrame(double ox, double oz, double dx, double dz, double px, double pz) {
        double rx = px - ox, rz = pz - oz;
        return new double[]{rx * dx + rz * dz, rx * -dz + rz * dx};
    }

    /** Is a point at `along` / `lateral` in the frame of a line hit by the stretch that rises at `stretchAlong` (a stretch covers one block either side of its middle)? */
    public static boolean stretchHits(double along, double lateral, double stretchAlong, double halfWidth) {
        return Math.abs(along - stretchAlong) <= 1.2 && Math.abs(lateral) <= halfWidth;
    }

    /** 1.35.0: the yaws (radians, about the line's start) of the lines of one Seismic Line: just the one, and at the Gates a fork of three. */
    public static double[] forkAngles(boolean gates) {
        return gates ? new double[]{0.0, -GoliathTable.FORK_ANGLE, GoliathTable.FORK_ANGLE} : new double[]{0.0};
    }

    /** (x, z) turned by 'yaw' radians about the origin (counter-clockwise seen from above when x is east and z is south, as the world's maths go): {x', z'}. */
    public static double[] rotate(double x, double z, double yaw) {
        double c = Math.cos(yaw), s = Math.sin(yaw);
        return new double[]{x * c - z * s, x * s + z * c};
    }

    /** How many rings a Twin Stomp has: two, and three at the Gates. */
    public static int stompRings(boolean gates) { return gates ? 3 : 2; }

    // ---------------------------------------------------------------- the Bedrock Fist
    /**
     * Where the fist lands for a foe at (fx, fz) when the giant stands at (bx, bz): on the foe, but never farther from the giant than its reach:
     * {x, z}. A foe on top of the giant is struck a block in front of it along (facingX, facingZ).
     */
    public static double[] fistMark(double bx, double bz, double fx, double fz, double reach, double facingX, double facingZ) {
        double dx = fx - bx, dz = fz - bz, d = Math.sqrt(dx * dx + dz * dz);
        if (d < 1e-3) {
            double fl = Math.sqrt(facingX * facingX + facingZ * facingZ);
            return fl < 1e-6 ? new double[]{bx, bz + 1.0} : new double[]{bx + facingX / fl, bz + facingZ / fl};
        }
        if (d <= reach) return new double[]{fx, fz};
        return new double[]{bx + dx / d * reach, bz + dz / d * reach};
    }

    // ---------------------------------------------------------------- the Rubble Fall
    /** How many circles a Rubble Fall drops: the foes' own and a few more, 3 to 8, more at higher power (castSlots 1 to 4). */
    public static int rubbleCount(int foes, int castSlots) {
        return Math.max(3, Math.min(8, Math.max(foes, 0) + 1 + Math.max(1, castSlots)));
    }

    /** {@link #rubbleCount(int, int)} and, at the Gates, one more circle (still at most 8). */
    public static int rubbleCount(int foes, int castSlots, boolean gates) { return Math.min(8, rubbleCount(foes, castSlots) + (gates ? 1 : 0)); }

    /**
     * Where the circles go: one on each foe in the order given (a foe within `minGap` of a circle already placed is covered by it), then the rest
     * on rings round the middle of the pit, `phase` radians round, each at least `minGap` from every other circle and 4 blocks from the giant,
     * inside `pitRadius` less a margin. Never more than `count`. Each result is {x, z}.
     */
    public static List<double[]> rubbleSpots(List<double[]> foes, int count, double centreX, double centreZ, double pitRadius, double minGap, double bossX, double bossZ, double phase) {
        List<double[]> out = new ArrayList<>();
        int want = Math.max(0, Math.min(8, count));
        if (foes != null) {
            for (double[] f : foes) {
                if (out.size() >= want) break;
                if (f == null || f.length < 2 || nearAny(out, f[0], f[1], minGap)) continue;
                out.add(new double[]{f[0], f[1]});
            }
        }
        double[] rings = {7.0, 13.0, 19.0, Math.max(8.0, pitRadius - 4.0)};
        for (int ring = 0; ring < rings.length && out.size() < want; ring++) {
            int slots = 3 + ring * 2;
            for (int k = 0; k < slots && out.size() < want; k++) {
                double a = phase + (k + 0.5 * (ring & 1)) * Math.PI * 2 / slots;
                double x = centreX + Math.cos(a) * rings[ring], z = centreZ + Math.sin(a) * rings[ring];
                double dx = x - centreX, dz = z - centreZ;
                if (dx * dx + dz * dz > (pitRadius - 3.0) * (pitRadius - 3.0)) continue;
                double bdx = x - bossX, bdz = z - bossZ;
                if (bdx * bdx + bdz * bdz < 16.0 || nearAny(out, x, z, minGap)) continue;
                out.add(new double[]{x, z});
            }
        }
        return out;
    }

    private static boolean nearAny(List<double[]> marks, double x, double z, double minDist) {
        for (double[] m : marks) {
            double dx = m[0] - x, dz = m[1] - z;
            if (dx * dx + dz * dz < minDist * minDist) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- a budget of health for a move of several hits
    /**
     * A move that hits the same victim twice or three times (the Twin Stomp, the Quake) may take at most a share of that victim's maximum health in
     * all, however many of its hits land: {@link #allow} says how much of a hit is left, {@link #spend} notes what was dealt. One budget per use of
     * the move, one entry per victim (by entity id).
     */
    public static final class DamageBudget {
        private final double share;
        private final Map<Integer, Double> spent = new HashMap<>();

        public DamageBudget(double share) { this.share = Math.max(0.0, Math.min(1.0, share)); }

        /** The part (0 to 1) of a hit expected to deal `expected` that victim `id` of `maxHealth` may still take: 1 while the total stays within the share. */
        public double allow(int id, double maxHealth, double expected) {
            if (!(expected > 0.0) || !(maxHealth > 0.0)) return 0.0;
            double left = share * maxHealth - spent.getOrDefault(id, 0.0);
            if (left <= 0.0) return 0.0;
            return expected <= left ? 1.0 : left / expected;
        }

        public void spend(int id, double amount) { if (amount > 0.0) spent.merge(id, amount, Double::sum); }

        public double spent(int id) { return spent.getOrDefault(id, 0.0); }

        public double share() { return share; }
    }
}
