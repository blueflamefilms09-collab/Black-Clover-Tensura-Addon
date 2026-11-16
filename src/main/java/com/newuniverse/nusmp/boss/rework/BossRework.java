package com.newuniverse.nusmp.boss.rework;

import java.util.List;

/**
 * Why: boss rework wave 1 (docs/bosses/rework_wave1.md) appends moves to the move tables of the Goliath, the God of Order, the Charybdis Spawn and seven Tier List bosses.
 * The client reads the move being wound up by its index, so new rows only ever go at the end, and the server setting {@code bossRework1Moves} off must give back exactly the
 * old list: the first {@code oldCount} rows. Pure (no game types), unit tested.
 */
public final class BossRework {
    private BossRework() {}

    /** The rows a boss fights with: all of them, or with the setting off only the first {@code oldCount} (the list as it was before the rework). */
    public static <T> List<T> trim(List<T> all, int oldCount, boolean reworkOn) {
        if (reworkOn || all.size() <= oldCount) return all;
        return all.subList(0, Math.max(0, oldCount));
    }

    /** What is left of a hit that a stone skin (or another shield of this wave) lets {@code passShare} of through: never under a tenth, never over the whole hit. */
    public static float shielded(float amount, float passShare) {
        if (amount <= 0f) return amount;
        return amount * Math.max(0.1f, Math.min(1f, passShare));
    }

    /**
     * The points of a chain of {@code n} stomps (or circles) from (fx, fz) toward (tx, tz): the first {@code first} blocks out, each next one {@code step} further on,
     * never past {@code maxReach}. With the target on top of the caster the chain runs along (dirX, dirZ). At most 8 points. Returns {x, z} pairs.
     */
    public static double[][] chain(double fx, double fz, double tx, double tz, double dirX, double dirZ, int n, double first, double step, double maxReach) {
        double dx = tx - fx, dz = tz - fz, len = Math.hypot(dx, dz);
        if (len < 1e-3) {
            dx = dirX;
            dz = dirZ;
            len = Math.hypot(dx, dz);
            if (len < 1e-3) { dx = 0; dz = 1; len = 1; }
        }
        dx /= len;
        dz /= len;
        int count = Math.max(0, Math.min(8, n));
        double[][] out = new double[count][];
        for (int i = 0; i < count; i++) {
            double d = Math.min(maxReach, first + step * i);
            out[i] = new double[]{fx + dx * d, fz + dz * d};
        }
        return out;
    }

    /** The speed (blocks per pull step) a current drags a foe {@code dist} blocks from its centre with: strongest at the rim of {@code radius}, nothing in the core or outside. */
    public static double pull(double dist, double radius, double core, double maxSpeed) {
        if (dist <= core || dist > radius || radius <= core) return 0.0;
        return maxSpeed * (dist - core) / (radius - core);
    }
}
