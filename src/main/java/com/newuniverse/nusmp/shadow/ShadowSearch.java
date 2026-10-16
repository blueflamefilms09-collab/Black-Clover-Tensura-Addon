package com.newuniverse.nusmp.shadow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: when the aimed spot is not a valid shadow, Shadow Travel falls back to "the nearest shadow in front of you". That search must be bounded
 * (a cast may never freeze the server tick), deterministic and nearest-first, so the order of the candidate cells is computed here, purely, and
 * the world code only walks the list until it finds a spot or the budget is spent.
 *
 * <p>The cells come in rings of growing distance along the look direction; each ring is a small fan of directions (a cone of about 30 degrees to
 * each side and 24 degrees up and down). Inside a ring the straight-ahead cell comes first, then the ones near it. Duplicate cells are dropped
 * and the list is cut at {@code cap} entries, which is the number of candidate checks one cast may spend.
 */
public final class ShadowSearch {
    private ShadowSearch() {}

    /** The number of candidate cells one cast may check when the config says nothing. */
    public static final int DEFAULT_CAP = 400;
    /** The nearest ring (blocks): cells closer than this are the caster's own. */
    public static final int FIRST_RING = 2;

    private static final double[] YAW = {0, 10, -10, 20, -20, 30, -30};
    private static final double[] PITCH = {0, 12, -12, 24, -24};

    /**
     * Offsets (dx, dy, dz) from the caster's eyes, nearest ring first, at most {@code cap} of them, none farther than {@code range} blocks.
     * {@code (lx, ly, lz)} is the look direction (any length but not zero; a zero vector looks north).
     */
    public static List<int[]> candidates(double lx, double ly, double lz, int range, int cap) {
        List<int[]> out = new ArrayList<>();
        if (cap <= 0 || range < FIRST_RING) return out;
        double len = Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (len < 1e-9) { lx = 0; ly = 0; lz = -1; len = 1; }
        double yaw0 = Math.atan2(-lx / len, lz / len), pitch0 = Math.asin(clamp(-ly / len, -1, 1));
        Set<Long> seen = new HashSet<>();
        for (int d = FIRST_RING; d <= range; d++) {
            for (double dp : PITCH) {
                for (double dy : YAW) {
                    double yaw = yaw0 + Math.toRadians(dy), pitch = clamp(pitch0 + Math.toRadians(dp), -Math.PI / 2, Math.PI / 2);
                    double cp = Math.cos(pitch);
                    int x = (int) Math.round(-Math.sin(yaw) * cp * d), y = (int) Math.round(-Math.sin(pitch) * d), z = (int) Math.round(Math.cos(yaw) * cp * d);
                    if (seen.add(key(x, y, z))) {
                        out.add(new int[]{x, y, z});
                        if (out.size() >= cap) return out;
                    }
                }
            }
        }
        return out;
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** The extra magicule cost of a jump, as a percent of the caster's maximum: {@code perBlock} percent for every block travelled (capped at 8%). */
    public static double extraManaPercent(double distance, double perBlock) {
        return Math.max(0, Math.min(8.0, Math.max(0, distance) * Math.max(0, perBlock)));
    }
}
