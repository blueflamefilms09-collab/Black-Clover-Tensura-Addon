package com.newuniverse.nusmp.entity.zogratis;

import java.util.HashMap;
import java.util.Map;

/**
 * How many times one volley may hit the same victim (pure, unit tested). A volley of nine bone spears is a wall to dodge, not nine
 * separate kills: each victim takes at most {@code maxHits} of them, and the rest fly on. One budget per volley, shared by all of
 * its projectiles.
 */
public final class VolleyBudget {
    private final int maxHits;
    private final Map<Integer, Integer> hits = new HashMap<>();

    public VolleyBudget(int maxHits) { this.maxHits = Math.max(1, maxHits); }

    /** May the projectile hit victim 'id' now? Counts the hit if so. */
    public boolean tryHit(int id) {
        int n = hits.getOrDefault(id, 0);
        if (n >= maxHits) return false;
        hits.put(id, n + 1);
        return true;
    }

    public int hitsOn(int id) { return hits.getOrDefault(id, 0); }
    public int maxHits() { return maxHits; }
}
