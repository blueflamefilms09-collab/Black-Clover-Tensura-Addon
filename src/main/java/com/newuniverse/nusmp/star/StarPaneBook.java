package com.newuniverse.nusmp.star;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quartile Scutum: the pure list of the panes that stand in the world right now (docs/vfx_remake/star_ingame.md). One pane per player (a new one replaces the old), at most {@link StarRules#PANES_SERVER}
 * on the whole server (the oldest goes first), so the per-tick test of the game side is a loop over at most eight boxes, and nothing at all while the list is empty. All calls come from the server thread.
 */
public final class StarPaneBook {
    /** One pane: its body, its owner, the level, its box, how much it can still take and when it ends. */
    public static final class Pane {
        public final UUID entity, owner;
        public final String dim;
        public final StarMath.Pane geom;
        public final float max;
        public float pool;
        public final long expiry;

        Pane(UUID entity, UUID owner, String dim, StarMath.Pane geom, float pool, long expiry) {
            this.entity = entity;
            this.owner = owner;
            this.dim = dim;
            this.geom = geom;
            this.pool = pool;
            this.max = pool;
            this.expiry = expiry;
        }

        /** Share of the pool left, 0 to 100. */
        public int percent() { return Math.max(0, Math.min(100, Math.round(100f * pool / Math.max(1e-3f, max)))); }
    }

    private final Map<UUID, Pane> byEntity = new LinkedHashMap<>();

    public int size() { return byEntity.size(); }

    /** Adds a pane and returns the panes that had to go (the owner's older one, then the oldest of anyone over the server cap): the caller discards their bodies. */
    public List<Pane> add(UUID entity, UUID owner, String dim, StarMath.Pane geom, float pool, long expiry) {
        List<Pane> gone = new ArrayList<>(2);
        for (Iterator<Pane> it = byEntity.values().iterator(); it.hasNext(); ) {
            Pane p = it.next();
            if (p.owner.equals(owner)) { gone.add(p); it.remove(); }
        }
        byEntity.put(entity, new Pane(entity, owner, dim, geom, pool, expiry));
        while (byEntity.size() > StarRules.PANES_SERVER) {
            Iterator<Pane> it = byEntity.values().iterator();
            Pane oldest = it.next();
            it.remove();
            gone.add(oldest);
        }
        return gone;
    }

    public Pane get(UUID entity) { return byEntity.get(entity); }

    public boolean remove(UUID entity) { return byEntity.remove(entity) != null; }

    public void clearOwner(UUID owner, List<Pane> out) {
        for (Iterator<Pane> it = byEntity.values().iterator(); it.hasNext(); ) {
            Pane p = it.next();
            if (p.owner.equals(owner)) { out.add(p); it.remove(); }
        }
    }

    public List<Pane> clearAll() {
        List<Pane> out = new ArrayList<>(byEntity.values());
        byEntity.clear();
        return out;
    }

    /** Takes {@code amount} off the pane's pool. True when the pane is used up (the caller breaks it). */
    public boolean damage(UUID entity, float amount) {
        Pane p = byEntity.get(entity);
        if (p == null) return false;
        p.pool -= Math.max(0f, amount);
        return p.pool <= 0f;
    }

    /** The first pane in this dimension that the segment a to b passes through and whose owner is not {@code friend}-side ({@code isFriend} true = skip it), or null. At most eight boxes are tested. */
    public Pane firstCrossed(String dim, StarMath.P a, StarMath.P b, java.util.function.Predicate<UUID> isFriendOwner) {
        for (Pane p : byEntity.values()) {
            if (!p.dim.equals(dim) || isFriendOwner.test(p.owner)) continue;
            if (StarMath.crosses(p.geom, a, b)) return p;
        }
        return null;
    }
}
