package com.newuniverse.nusmp.heroweapon.weapon.carriage;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the rule is ONE carriage per wielder, and the techniques need to find the wielder's carriage without scanning every entity of every dimension. This is the
 * small book that maps a wielder's UUID to the UUID of the carriage entity they own; the entity enters itself when it is spawned and leaves when it is removed
 * for any reason, and a lookup that finds a stale entry (the entity unloaded or gone) forgets it. It lives in memory only, because the carriage entity is not
 * saved with the world (a restart simply has no carriage), and it is cleared when the server stops. Server thread only.
 */
final class CarriageRegistry {
    private CarriageRegistry() {}

    private static final Map<UUID, UUID> BY_OWNER = new HashMap<>();

    static void register(UUID owner, UUID entity) { BY_OWNER.put(owner, entity); }

    /** Forgets the entry of {@code owner} when it still names {@code entity} (a newer carriage keeps its entry). */
    static void unregister(UUID owner, UUID entity) {
        if (owner != null && entity.equals(BY_OWNER.get(owner))) BY_OWNER.remove(owner);
    }

    /** The wielder's living carriage in any loaded dimension, or null. */
    static CarriageEntity find(MinecraftServer server, UUID owner) {
        UUID id = BY_OWNER.get(owner);
        if (id == null || server == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e instanceof CarriageEntity c && c.isAlive() && !c.isRemoved()) return c;
        }
        BY_OWNER.remove(owner);
        return null;
    }

    static void clear() { BY_OWNER.clear(); }

    static int size() { return BY_OWNER.size(); }
}
