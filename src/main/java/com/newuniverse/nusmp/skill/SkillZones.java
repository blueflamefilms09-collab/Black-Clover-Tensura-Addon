package com.newuniverse.nusmp.skill;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the DanMachi chants are not single hits: a blizzard keeps freezing, flame pillars erupt wave after wave, a gravity field keeps
 * crushing. Each of those is a short-lived "zone" that pulses a few times. This scheduler owns them: pulses run on the OWNER's own tick
 * (so nothing runs when the owner is offline or in another dimension), each owner has at most {@link #MAX_PER_OWNER} zones (the oldest
 * is dropped), every pulse is wrapped so one bad pulse ends only its own zone, and {@link #sweep} forgets the zones of owners who left.
 * Zones are in memory only: a restart simply ends them, which is fine for effects that last seconds.
 */
final class SkillZones {
    private SkillZones() {}

    static final int MAX_PER_OWNER = 4;

    /** One pulse of a zone; {@code index} counts from 0. */
    interface Pulse { void run(ServerPlayer owner, ServerLevel level, int index); }

    private static final class Zone {
        final ResourceKey<Level> dimension;
        final int interval;
        final int total;
        final Pulse pulse;
        long next;
        int index;

        Zone(ResourceKey<Level> dimension, long first, int interval, int total, Pulse pulse) {
            this.dimension = dimension;
            this.next = first;
            this.interval = interval;
            this.total = total;
            this.pulse = pulse;
        }
    }

    private static final Map<UUID, List<Zone>> ZONES = new HashMap<>();

    /** Starts a zone that pulses {@code pulses} times, one every {@code interval} ticks, the first one right away. */
    static void start(ServerPlayer owner, int interval, int pulses, Pulse pulse) {
        if (owner == null || pulses <= 0 || pulse == null) return;
        List<Zone> mine = ZONES.computeIfAbsent(owner.getUUID(), k -> new ArrayList<>());
        while (mine.size() >= MAX_PER_OWNER) mine.remove(0);
        mine.add(new Zone(owner.level().dimension(), owner.level().getGameTime(), Math.max(1, interval), pulses, pulse));
    }

    /** Called every tick for a player: runs the pulses that are due. */
    static void tick(ServerPlayer owner) {
        List<Zone> mine = ZONES.get(owner.getUUID());
        if (mine == null || mine.isEmpty()) return;
        if (!owner.isAlive()) { mine.clear(); return; }
        long now = owner.level().getGameTime();
        Iterator<Zone> it = mine.iterator();
        while (it.hasNext()) {
            Zone z = it.next();
            if (z.dimension != owner.level().dimension()) { it.remove(); continue; }   // the owner left the dimension: the zone ends
            if (now < z.next) continue;
            try {
                z.pulse.run(owner, owner.serverLevel(), z.index);
            } catch (RuntimeException | LinkageError broken) {
                it.remove();
                continue;
            }
            z.index++;
            z.next = now + z.interval;
            if (z.index >= z.total) it.remove();
        }
        if (mine.isEmpty()) ZONES.remove(owner.getUUID());
    }

    /** Drops the zones of players who are no longer online. */
    static void sweep(MinecraftServer server) {
        if (server == null || ZONES.isEmpty()) return;
        ZONES.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }

    static void clear(UUID owner) { ZONES.remove(owner); }

    static void clearAll() { ZONES.clear(); }
}
