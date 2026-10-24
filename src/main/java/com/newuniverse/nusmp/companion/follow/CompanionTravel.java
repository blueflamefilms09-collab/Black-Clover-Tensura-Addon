package com.newuniverse.nusmp.companion.follow;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.companion.CompanionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: a follower must come with her player through a dimension change, whatever the cause (a portal, a command, another mod's crossing), and a server must not pay for it when nobody follows anybody. It works in three
 * small steps with the technique the Avatars already use ({@code Entity.changeDimension(DimensionTransition)}, which re-creates the entity from its saved data, and a standable spot two to four blocks from the player):
 * (1) when the player is ABOUT to travel ({@link EntityTravelToDimensionEvent}) the followers within {@link TravelRules#COLLECT_RADIUS} blocks are noted by UUID (at most the server setting, never more than
 * {@link TravelRules#HARD_CAP}, nearest first) and their chunk gets the vanilla 300-tick portal ticket, so it is still loaded a moment later; (2) when the player has ARRIVED ({@link PlayerEvent.PlayerChangedDimensionEvent})
 * the record moves to the arrivals; (3) from the next tick on, as soon as the chunk the player stands in is loaded (it never forces one to load), each follower is put down beside them; an arrival that waits longer than
 * {@link TravelRules#STALE_TICKS} ticks, or whose player left, is dropped. No chunk is ever kept loaded for good, no world is scanned (one box query per crossing, around the player), and the tick work is nothing at all
 * while no crossing is in progress. Off with {@code companions.travel}. Server thread only.
 */
public final class CompanionTravel {
    private CompanionTravel() {}

    private static final Logger LOG = LogUtils.getLogger();

    private record Pending(List<UUID> followers, ResourceKey<Level> from, long at) {}

    /** Noted before the crossing (by the travelling player), and waiting to be put down after it. */
    private static final Map<UUID, Pending> COLLECTED = new HashMap<>(), ARRIVED = new HashMap<>();

    // ================================================================ the followers of a player
    /** The followers of the player near them (within {@code radius}), nearest first. One box query. */
    public static List<Mob> followersOf(ServerPlayer player, double radius) {
        List<Mob> found = new ArrayList<>(player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius),
                m -> m instanceof PortalFollower f && player.getUUID().equals(f.followedPlayer()) && m.isAlive() && !m.isPassenger()));
        found.sort(Comparator.comparingDouble(m -> m.distanceToSqr(player)));
        return found;
    }

    // ================================================================ step one: the player is about to travel
    public static void onTravel(EntityTravelToDimensionEvent e) {
        if (e.isCanceled() || !CompanionConfig.travel() || !(e.getEntity() instanceof ServerPlayer p)) return;
        collect(p);
    }

    /** Notes the followers that will come along (public for the self-test). Returns how many. */
    public static int collect(ServerPlayer p) {
        List<Mob> found = followersOf(p, TravelRules.COLLECT_RADIUS);
        int n = TravelRules.carried(found.size(), CompanionConfig.maxFollowers());
        if (n <= 0) return 0;
        ServerLevel level = p.serverLevel();
        List<UUID> ids = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Mob m = found.get(i);
            ids.add(m.getUUID());
            BlockPos at = m.blockPosition();
            level.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(at), 2, at);
        }
        COLLECTED.put(p.getUUID(), new Pending(ids, level.dimension(), level.getGameTime()));
        return n;
    }

    // ================================================================ step two: the player has arrived
    public static void onArrived(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) markArrived(p);
    }

    /** Moves the player's noted crossing to the arrivals (public for the self-test). */
    public static boolean markArrived(ServerPlayer p) {
        Pending pending = COLLECTED.remove(p.getUUID());
        if (pending == null || !CompanionConfig.travel() || TravelRules.stale(pending.at(), p.serverLevel().getGameTime())) return false;
        ARRIVED.put(p.getUUID(), pending);
        return true;
    }

    // ================================================================ step three: put them down
    /** Every server tick: costs nothing while no crossing is waiting. */
    public static void tick(MinecraftServer server) {
        if (ARRIVED.isEmpty()) return;
        long now = server.overworld().getGameTime();
        for (Iterator<Map.Entry<UUID, Pending>> it = ARRIVED.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Pending> en = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            if (p == null || !p.isAlive() || TravelRules.stale(en.getValue().at(), now)) {
                it.remove();
                continue;
            }
            if (!p.serverLevel().hasChunkAt(p.blockPosition())) continue;                // the chunk they stand in is not loaded yet: wait, never force it
            it.remove();
            place(server, p, p.serverLevel(), en.getValue());
        }
    }

    /** Puts the player's waiting followers down beside them in {@code dest} now (public for the self-test). Returns how many were moved. */
    public static int arriveNow(MinecraftServer server, ServerPlayer p, ServerLevel dest) {
        Pending pending = ARRIVED.remove(p.getUUID());
        return pending == null ? 0 : place(server, p, dest, pending);
    }

    private static int place(MinecraftServer server, ServerPlayer p, ServerLevel dest, Pending pending) {
        int moved = 0;
        try {
            ServerLevel from = server.getLevel(pending.from());
            if (from == null) return 0;
            for (UUID id : pending.followers()) {
                Entity e = from.getEntity(id);
                if (!(e instanceof Mob m) || !m.isAlive() || !(m instanceof PortalFollower f) || !p.getUUID().equals(f.followedPlayer())) continue;
                Vec3 spot = findSpot(dest, p.position(), m);
                m.getNavigation().stop();
                if (from == dest) m.teleportTo(spot.x, spot.y, spot.z);
                else m.changeDimension(new DimensionTransition(dest, spot, Vec3.ZERO, m.getYRot(), m.getXRot(), DimensionTransition.DO_NOTHING));
                moved++;
            }
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] carrying followers through a dimension change failed: {}", ex.toString());
        }
        return moved;
    }

    /**
     * A standable spot two to four blocks from {@code around}: solid floor and room for the body; the point itself when none is found. At most 140 cheap checks, none in a chunk that is not loaded (so it never
     * forces a chunk to load).
     */
    public static Vec3 findSpot(ServerLevel level, Vec3 around, Entity body) {
        var rnd = level.getRandom();
        var dims = body.getDimensions(Pose.STANDING);
        for (int i = 0; i < 20; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2, d = 2.0 + rnd.nextDouble() * 2.0;
            double x = around.x + Math.cos(ang) * d, z = around.z + Math.sin(ang) * d;
            for (int dy : new int[]{0, 1, -1, 2, -2, 3, -3}) {
                BlockPos pos = BlockPos.containing(x, around.y + dy, z);
                if (!level.hasChunkAt(pos)) break;
                BlockPos below = pos.below();
                if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && level.noCollision(dims.makeBoundingBox(x, pos.getY(), z))) return new Vec3(x, pos.getY(), z);
            }
        }
        return around;
    }

    // ================================================================ clean-up
    /** The player left: nothing of theirs is kept. */
    public static void forget(UUID player) {
        COLLECTED.remove(player);
        ARRIVED.remove(player);
    }

    /** Once a second: drops crossings that never finished. */
    public static void sweep(long now) {
        COLLECTED.values().removeIf(p -> TravelRules.stale(p.at(), now));
        ARRIVED.values().removeIf(p -> TravelRules.stale(p.at(), now));
    }

    public static void clear() {
        COLLECTED.clear();
        ARRIVED.clear();
    }

    /** Is any crossing being kept (so the per-tick work has something to do)? */
    public static boolean busy() { return !COLLECTED.isEmpty() || !ARRIVED.isEmpty(); }

    /** How many followers are noted for the player's coming crossing (self-test). */
    public static int collectedFor(UUID player) {
        Pending p = COLLECTED.get(player);
        return p == null ? 0 : p.followers().size();
    }

    /** How many crossings are being kept (self-test). */
    public static int kept() { return COLLECTED.size() + ARRIVED.size(); }
}
