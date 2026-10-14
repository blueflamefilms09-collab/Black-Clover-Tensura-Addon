package com.newuniverse.nusmp.boss;

import java.util.HashMap;
import java.util.Map;

/** The pure geometry and bookkeeping of Lucius's three cast spells (unit tested). */
public final class SpellMath {
    private SpellMath() {}

    /**
     * Is the point (px, pz) inside the lane that starts at (ox, oz), runs along the unit direction (dx, dz) for 'length' blocks and is
     * 'halfWidth' wide to each side? The direction is normalised here, a zero direction gives an empty lane.
     */
    public static boolean inLane(double ox, double oz, double dx, double dz, double px, double pz, double length, double halfWidth) {
        double n = Math.sqrt(dx * dx + dz * dz);
        if (n < 1e-9) return false;
        double ux = dx / n, uz = dz / n;
        double rx = px - ox, rz = pz - oz;
        double along = rx * ux + rz * uz;
        if (along < 0 || along > length) return false;
        double side = Math.abs(rx * -uz + rz * ux);
        return side <= halfWidth;
    }

    /** Counts hits per victim inside one cast so that no foe is hit more than 'max' times by a multi-blast spell. */
    public static final class HitBudget<K> {
        private final int max;
        private final Map<K, Integer> hits = new HashMap<>();

        public HitBudget(int max) { this.max = Math.max(1, max); }

        /** Tries to spend one hit on 'victim': true if it may be struck. */
        public boolean take(K victim) {
            int n = hits.getOrDefault(victim, 0);
            if (n >= max) return false;
            hits.put(victim, n + 1);
            return true;
        }

        public int used(K victim) { return hits.getOrDefault(victim, 0); }
    }

    /** The darkness cloud's radius at 'left' ticks of a wind-up of 'total': it grows from 4 blocks to the full radius over the telegraph. */
    public static double cloudRadius(int left, int total, double full) {
        if (total <= 0) return full;
        double progress = Math.max(0, Math.min(1, 1.0 - left / (double) total));
        return 4 + (full - 4) * progress;
    }
}
