package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;

import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wave 15: what stays in the world between ticks. Three small, capped lists, all cleared when the server stops:
 * <ul>
 *   <li>the swarms of the Cape of Bees (one per owner, six seconds, a sting a second on at most four creatures);</li>
 *   <li>creatures flung by the Bracers of Manhandling that have yet to land (at most sixteen);</li>
 *   <li>players phased by the Bracers of Phasing (at most as many as there are wearers; their game mode is saved and put back, also after a crash).</li>
 * </ul>
 * Phasing is the spectator game mode for thirty seconds, which is the one way the game lets a client walk through stone: the player's earlier mode
 * is kept in their persistent data, restored when the time is up (after stepping out of any wall), on logout, on the next login and at shutdown.
 */
final class Wave15Field {
    private Wave15Field() {}

    private static final int MAX_THROWN = 16, MAX_SWARMS = 32, PHASE_TICKS = 600, PHASE_GRACE = 100, PHASE_EXTENSIONS = 3, SEARCH = 5;
    private static final String K_PREV = "phase_prev", K_X = "phase_x", K_Y = "phase_y", K_Z = "phase_z";

    private static final class Swarm {
        final UUID owner;
        final ResourceKey<Level> dim;
        UUID target;
        Vec3 at;
        int age;

        Swarm(UUID owner, ResourceKey<Level> dim, UUID target, Vec3 at) {
            this.owner = owner;
            this.dim = dim;
            this.target = target;
            this.at = at;
        }
    }

    private static final class Thrown {
        final UUID thrower;
        final int born;

        Thrown(UUID thrower, int born) {
            this.thrower = thrower;
            this.born = born;
        }
    }

    private static final class Phase {
        int until, extensions;
        final Vec3 start;

        Phase(int until, Vec3 start) {
            this.until = until;
            this.start = start;
        }
    }

    private static final Map<UUID, Swarm> SWARMS = new LinkedHashMap<>();
    private static final Map<UUID, Thrown> THROWN = new LinkedHashMap<>();
    private static final Map<UUID, Phase> PHASED = new LinkedHashMap<>();
    /** friend -> (wearer, until): who a creature is bonded to by a Faithful Friendship bracelet, renewed every second by the wearer; capped. */
    private static final Map<UUID, Bond> BONDS = new LinkedHashMap<>();

    record Bond(UUID wearer, long until) {}

    static void bond(UUID friend, UUID wearer, long until) {
        if (BONDS.size() >= 256 && !BONDS.containsKey(friend)) BONDS.entrySet().removeIf(en -> en.getValue().until() < until - 60);
        if (BONDS.size() < 256 || BONDS.containsKey(friend)) BONDS.put(friend, new Bond(wearer, until));
    }

    static Bond bondOf(UUID friend) { return BONDS.get(friend); }

    // ---------------------------------------------------------------- the tick
    static void tick(MinecraftServer server) {
        if (SWARMS.isEmpty() && THROWN.isEmpty() && PHASED.isEmpty()) return;
        int now = server.getTickCount();
        if (!SWARMS.isEmpty()) tickSwarms(server);
        if (!THROWN.isEmpty() && now % 2 == 0) tickThrown(server, now);
        if (!PHASED.isEmpty() && now % 5 == 0) tickPhases(server, now);
    }

    static void clear(MinecraftServer server) {
        if (server != null) {
            for (UUID id : new ArrayList<>(PHASED.keySet())) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) endPhase(p, true);
            }
        }
        SWARMS.clear();
        THROWN.clear();
        PHASED.clear();
        BONDS.clear();
    }

    // ---------------------------------------------------------------- bees
    /** Lets the bees go from the player's cloak: onto {@code target} (followed while it lives) or at a point. Replaces the player's earlier swarm. */
    static void release(ServerPlayer p, LivingEntity target, Vec3 at) {
        if (SWARMS.size() >= MAX_SWARMS && !SWARMS.containsKey(p.getUUID())) return;
        SWARMS.put(p.getUUID(), new Swarm(p.getUUID(), p.serverLevel().dimension(), target == null ? null : target.getUUID(), at));
    }

    private static void tickSwarms(MinecraftServer server) {
        Iterator<Swarm> it = SWARMS.values().iterator();
        while (it.hasNext()) {
            Swarm s = it.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(s.owner);
            if (owner == null || !owner.isAlive() || owner.serverLevel().dimension() != s.dim) {
                it.remove();
                continue;
            }
            ServerLevel lv = owner.serverLevel();
            if (s.target != null) {
                Entity t = lv.getEntity(s.target);
                if (t instanceof LivingEntity le && le.isAlive()) s.at = le.position().add(0, le.getBbHeight() * 0.6, 0);
                else s.target = null;
            }
            s.age++;
            Vec3 c = s.at;
            if (s.age % 3 == 0) {
                RukhFx.spray(lv, Wave15Util.align(0xF2C230), c.x, c.y, c.z, 6, 0.6, 0.45, 0.6, 0.02);
                RukhFx.spray(lv, Wave15Util.align(0x1E1C1A), c.x, c.y, c.z, 4, 0.6, 0.45, 0.6, 0.02);
            }
            if (s.age % Wave15Rules.SWARM_PULSE == 0) pulse(owner, lv, c, s.target);
            if (s.age >= Wave15Rules.SWARM_TICKS) it.remove();
        }
    }

    private static void pulse(ServerPlayer owner, ServerLevel lv, Vec3 c, UUID first) {
        List<LivingEntity> foes = Wave15Util.near(lv, c, 3.5, e -> Wave15Util.target(owner, e));
        int n = 0;
        for (LivingEntity t : foes) {
            if (n++ >= 4) break;
            Fx.damage(owner, t, Wave15Rules.SWARM_STING, "minecraft:sting");
            if (t instanceof net.minecraft.world.entity.player.Player) Wave15Util.give(t, 40, 0, "minecraft:slowness");
            else Wave15Util.give(t, 60, 0, "minecraft:poison");
        }
    }

    // ---------------------------------------------------------------- flung creatures
    static void thrown(ServerPlayer p, LivingEntity t) {
        if (THROWN.size() >= MAX_THROWN && !THROWN.containsKey(t.getUUID())) return;
        THROWN.put(t.getUUID(), new Thrown(p.getUUID(), p.getServer().getTickCount()));
    }

    private static void tickThrown(MinecraftServer server, int now) {
        Iterator<Map.Entry<UUID, Thrown>> it = THROWN.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Thrown> en = it.next();
            Thrown th = en.getValue();
            ServerPlayer owner = server.getPlayerList().getPlayer(th.thrower);
            int age = now - th.born;
            if (owner == null || !owner.isAlive() || age > 100) {
                it.remove();
                continue;
            }
            Entity e = owner.serverLevel().getEntity(en.getKey());
            if (!(e instanceof LivingEntity le) || !le.isAlive()) {
                it.remove();
                continue;
            }
            if (age < 6 || !le.onGround()) continue;
            it.remove();
            ServerLevel lv = owner.serverLevel();
            Fx.damage(owner, le, Wave15Rules.THROW_IMPACT, "minecraft:fall");
            Fx.burst(lv, le.position(), 0xC8D4E4);
            int n = 0;
            for (LivingEntity o : Wave15Util.near(lv, le.position(), 1.8, x -> x != le && Wave15Util.target(owner, x))) {
                if (n++ >= 4) break;
                Fx.damage(owner, o, Wave15Rules.THROW_SPLASH, "minecraft:fall");
            }
        }
    }

    // ---------------------------------------------------------------- phasing
    static boolean phased(ServerPlayer p) { return PHASED.containsKey(p.getUUID()); }

    /** Starts thirty seconds of phasing; false (with a line to the player) if it cannot be done here. */
    static boolean startPhase(ServerPlayer p) {
        if (p.isCreative() || p.isSpectator()) {
            Wave15Util.tell(p, "You walk through walls already.", ChatFormatting.GRAY);
            return false;
        }
        if (p.isPassenger() || PHASED.containsKey(p.getUUID())) return false;
        if (LivingDungeon.in(p.level())) {
            Wave15Util.tell(p, "The Dungeon's stone is magic: the bracers cannot part it.", ChatFormatting.RED);
            return false;
        }
        CompoundTag d = Wave15Util.data(p);
        d.putString(K_PREV, p.gameMode.getGameModeForPlayer().getName());
        d.putDouble(K_X, p.getX());
        d.putDouble(K_Y, p.getY());
        d.putDouble(K_Z, p.getZ());
        PHASED.put(p.getUUID(), new Phase(p.getServer().getTickCount() + PHASE_TICKS, p.position()));
        p.setGameMode(GameType.SPECTATOR);
        Fx.burst(p.serverLevel(), p.position(), 0xFFD860);
        return true;
    }

    private static void tickPhases(MinecraftServer server, int now) {
        for (UUID id : new ArrayList<>(PHASED.keySet())) {
            Phase ph = PHASED.get(id);
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) {
                PHASED.remove(id);                                                   // gone: the next login puts the game mode back
                continue;
            }
            if (!p.isAlive()) {
                PHASED.remove(id);
                continue;
            }
            if (now % 20 == 0 || now % 20 == 5) RukhFx.spray(p.serverLevel(), Wave15Util.align(0xFFD860), p.getX(), p.getY() + 0.9, p.getZ(), 3, 0.3, 0.5, 0.3, 0.0);
            if (now < ph.until) continue;
            if (p.serverLevel().noCollision(p, p.getBoundingBox())) {
                endPhase(p, false);
            } else {
                Vec3 spot = nearestStand(p);
                if (spot != null) {
                    p.teleportTo(spot.x, spot.y, spot.z);
                    endPhase(p, false);
                } else if (ph.extensions < PHASE_EXTENSIONS) {
                    ph.extensions++;
                    ph.until = now + PHASE_GRACE;
                    Wave15Util.tell(p, "Still inside the stone: the bracers hold you a little longer.", ChatFormatting.GRAY);
                } else {
                    endPhase(p, true);
                }
            }
        }
    }

    /** Puts the game mode back. {@code force} also sends the player to where they started if they are still inside a wall. */
    static void endPhase(ServerPlayer p, boolean force) {
        Phase ph = PHASED.remove(p.getUUID());
        CompoundTag d = Wave15Util.peek(p);
        if (d == null || !d.contains(K_PREV)) return;
        if (force && !p.serverLevel().noCollision(p, p.getBoundingBox())) {
            Vec3 spot = nearestStand(p);
            if (spot == null && ph != null) spot = ph.start;
            if (spot == null && d.contains(K_X)) spot = new Vec3(d.getDouble(K_X), d.getDouble(K_Y), d.getDouble(K_Z));
            if (spot != null) p.teleportTo(spot.x, spot.y, spot.z);
        }
        GameType back = GameType.byName(d.getString(K_PREV), GameType.SURVIVAL);
        d.remove(K_PREV);
        p.setGameMode(back);
        p.fallDistance = 0;
        if (back != GameType.CREATIVE && back != GameType.SPECTATOR) Wave15Util.give(p, 100, 0, "minecraft:slow_falling");     // thirty seconds of ghost flight may end high in the air
        Fx.burst(p.serverLevel(), p.position(), 0xFFD860);
        Wave15Util.tell(p, "You are solid again.", ChatFormatting.GOLD);
    }

    /** A login after a crash or a kick while phased: the game mode comes back, out of the wall. */
    static void restoreStale(ServerPlayer p) {
        CompoundTag d = Wave15Util.peek(p);
        if (d == null || !d.contains(K_PREV) || PHASED.containsKey(p.getUUID())) return;
        endPhase(p, true);
    }

    static void onLogout(ServerPlayer p) {
        if (PHASED.containsKey(p.getUUID())) endPhase(p, true);
        SWARMS.remove(p.getUUID());
    }

    /** The nearest place within {@link #SEARCH} blocks where the player fits and has ground under their feet, or null. */
    static Vec3 nearestStand(ServerPlayer p) {
        ServerLevel lv = p.serverLevel();
        Vec3 best = null;
        double bestD = Double.MAX_VALUE;
        BlockPos o = p.blockPosition();
        for (int dx = -SEARCH; dx <= SEARCH; dx++) {
            for (int dy = -SEARCH; dy <= SEARCH; dy++) {
                for (int dz = -SEARCH; dz <= SEARCH; dz++) {
                    double d = dx * dx + dy * dy + dz * dz;
                    if (d >= bestD) continue;
                    double x = o.getX() + 0.5 + dx, y = o.getY() + dy, z = o.getZ() + 0.5 + dz;
                    if (!lv.noCollision(p, p.getBoundingBox().move(x - p.getX(), y - p.getY(), z - p.getZ()))) continue;
                    BlockState below = lv.getBlockState(BlockPos.containing(x, y - 1, z));
                    if (below.getCollisionShape(lv, BlockPos.containing(x, y - 1, z)).isEmpty() || below.is(Blocks.LAVA) || below.is(Blocks.MAGMA_BLOCK)) continue;
                    best = new Vec3(x, y, z);
                    bestD = d;
                }
            }
        }
        return best;
    }

    /** Snowflake or other flavour particles are sent by the callers; this one is the sparkle of a finished step. */
    static void sparkle(ServerLevel lv, Vec3 at) {
        RukhFx.spray(lv, RukhFx.Alignment.LIGHT, at.x, at.y + 0.8, at.z, 8, 0.3, 0.4, 0.3, 0.03);
    }
}
