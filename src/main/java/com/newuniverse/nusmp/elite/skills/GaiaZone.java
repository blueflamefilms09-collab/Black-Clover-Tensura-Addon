package com.newuniverse.nusmp.elite.skills;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: Gaia's domain is a place: a centre, a radius, a dimension and an end time, owned by one player. Everything that reacts to it (the block guard, the blessing, the war check, the
 * bloom, the pillars) asks the same two questions, "is there a domain here" and "whose is it", so the answer lives in one Minecraft-free class: an immutable zone plus a small registry
 * (one zone per owner; raising a new one replaces the old). The registry is memory only: a restart ends the domains, which suits an effect that lasts minutes.
 */
public record GaiaZone(UUID owner, String dim, double cx, double cy, double cz, double radius, long until) {
    private static final Map<UUID, GaiaZone> ZONES = new ConcurrentHashMap<>();

    public boolean active(long now) { return now < until; }

    /** Horizontal distance within the radius and vertical distance within the radius too (a cylinder two radii tall). */
    public boolean contains(String dimension, double x, double y, double z) {
        if (!dim.equals(dimension)) return false;
        double dx = x - cx, dz = z - cz;
        return dx * dx + dz * dz <= radius * radius && Math.abs(y - cy) <= radius;
    }

    public GaiaZone endingAt(long newUntil) { return new GaiaZone(owner, dim, cx, cy, cz, radius, newUntil); }

    // ---------------------------------------------------------------- registry
    public static void put(GaiaZone z) { if (z != null) ZONES.put(z.owner(), z); }

    public static GaiaZone of(UUID owner) { return owner == null ? null : ZONES.get(owner); }

    public static GaiaZone remove(UUID owner) { return owner == null ? null : ZONES.remove(owner); }

    public static void clear() { ZONES.clear(); }

    public static int count() { return ZONES.size(); }

    /** The first live zone that contains the point, or null. Expired zones met on the way are dropped. */
    public static GaiaZone at(String dimension, double x, double y, double z, long now) {
        if (ZONES.isEmpty()) return null;
        GaiaZone found = null;
        for (var it = ZONES.entrySet().iterator(); it.hasNext(); ) {
            GaiaZone zone = it.next().getValue();
            if (!zone.active(now)) { it.remove(); continue; }
            if (found == null && zone.contains(dimension, x, y, z)) found = zone;
        }
        return found;
    }
}
