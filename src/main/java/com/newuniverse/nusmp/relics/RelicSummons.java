package com.newuniverse.nusmp.relics;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The allies {@link Fx#summon} calls up: at most six per player, each gone when its time is up, when its owner leaves or dies, or when the
 * server stops. Tensura's mobs are brain-driven, so they are pointed at a foe from outside (as the Dungeon's drivers do): a target, a path, and a
 * blow when in reach. A sweep every ten ticks looks at a few dozen entities at most.
 */
final class RelicSummons {
    private RelicSummons() {}

    private static final int MAX_PER_PLAYER = 6, EVERY = 10, STRIKE_EVERY = 20;
    private static final Map<UUID, ArrayDeque<UUID>> BY_OWNER = new HashMap<>();

    static void track(ServerPlayer owner, LivingEntity summon) {
        BY_OWNER.computeIfAbsent(owner.getUUID(), k -> new ArrayDeque<>()).addLast(summon.getUUID());
    }

    /** Called before a new summon: the oldest goes if the player already has the most they may. */
    static void makeRoom(ServerPlayer owner) {
        ArrayDeque<UUID> mine = BY_OWNER.get(owner.getUUID());
        if (mine == null) return;
        MinecraftServer server = owner.getServer();
        while (mine.size() >= MAX_PER_PLAYER) {
            UUID oldest = mine.pollFirst();
            Entity e = server == null ? null : find(server, oldest);
            if (e != null) e.discard();
        }
    }

    static void clear() { BY_OWNER.clear(); }

    private static Entity find(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }

    /** Server tick: the sweep, every ten ticks. */
    static void tick(MinecraftServer server) {
        if (BY_OWNER.isEmpty() || server.getTickCount() % EVERY != 3) return;
        Iterator<Map.Entry<UUID, ArrayDeque<UUID>>> owners = BY_OWNER.entrySet().iterator();
        while (owners.hasNext()) {
            Map.Entry<UUID, ArrayDeque<UUID>> entry = owners.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            List<LivingEntity> foes = owner != null && owner.isAlive() ? Fx.foesAround(owner, 14) : List.of();
            Iterator<UUID> it = entry.getValue().iterator();
            while (it.hasNext()) {
                Entity e = find(server, it.next());
                if (!(e instanceof LivingEntity living) || !living.isAlive()) { it.remove(); continue; }
                long now = living.level().getGameTime();
                if (owner == null || !owner.isAlive() || now >= living.getPersistentData().getLong(Fx.UNTIL)) {
                    Fx.burst((ServerLevel) living.level(), living.position(), 0x8888AA);
                    living.discard();
                    it.remove();
                    continue;
                }
                if (living instanceof Mob m && m.level() == owner.level()) drive(m, owner, foes, now);
            }
            if (entry.getValue().isEmpty()) owners.remove();
        }
    }

    private static void drive(Mob m, ServerPlayer owner, List<LivingEntity> foes, long now) {
        try {
            LivingEntity target = m.getTarget();
            if (target == null || !target.isAlive() || target == owner) {
                target = null;
                double best = Double.MAX_VALUE;
                for (LivingEntity f : foes) {
                    double d = f.distanceToSqr(m);
                    if (d < best) { best = d; target = f; }
                }
                m.setTarget(target);
                if (target != null) m.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
            }
            if (target == null) {
                if (m.distanceToSqr(owner) > 64) m.getNavigation().moveTo(owner, 1.2);
                return;
            }
            m.getNavigation().moveTo(target, 1.2);
            if (now - m.getPersistentData().getLong("nusmp_relic_strike") >= STRIKE_EVERY && m.getBoundingBox().inflate(1.1, 0.7, 1.1).intersects(target.getBoundingBox())) {
                m.getPersistentData().putLong("nusmp_relic_strike", now);
                m.swing(InteractionHand.MAIN_HAND);
                m.doHurtTarget(target);
            }
        } catch (RuntimeException | LinkageError ignored) {
            // a creature of an unexpected shape keeps its own mind
        }
    }
}
