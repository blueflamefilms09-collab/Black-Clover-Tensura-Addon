package com.newuniverse.nusmp.dragonballs;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * The server half of the Dragon Balls, all decided here (the client only draws): the registry of ball places, the scatter, the stone lock, the summoning check, the radar, and the handlers that keep the
 * balls from being lost. Cost, worst case: a pass every 5 seconds over at most 16 rows (4 item spawns at most, only where a player has already loaded the chunk: {@code getChunkNow}, never a chunk
 * load), a one-second pass over at most two ritual sessions, one registry write per carried ball per 5 seconds, and one entity scan (a box of {@code gatherRadius} blocks) per accepted spoken phrase.
 * Nothing runs per tick per player.
 */
public final class DbManager {
    private DbManager() {}

    static final Logger LOG = LogUtils.getLogger();
    private static final Random RNG = new Random();
    private static final int MAX_DEATHS = 256;

    /** Where a player last died (revive wish). Keyed by the player's UUID, bounded, expired by the revive window. */
    record Death(String dim, double x, double y, double z, long atMs) {}

    private static final Map<UUID, Death> DEATHS = new HashMap<>();

    // ================================================================ data helpers
    static DbData data(MinecraftServer s) { return DbData.get(s); }

    public static boolean locked(MinecraftServer s, BallSet set) { return DbRules.locked(data(s).lockedUntil[set.ordinal()], System.currentTimeMillis()); }

    public static long lockRemainingMs(MinecraftServer s, BallSet set) { return DbRules.remainingMs(data(s).lockedUntil[set.ordinal()], System.currentTimeMillis()); }

    /** Sets the stone lock of a set to {@code minutes} from now (0 = open). Admin command and the end of a ritual. */
    public static void setLock(MinecraftServer s, BallSet set, long minutes) {
        DbData d = data(s);
        d.lockedUntil[set.ordinal()] = DbRules.lockUntil(System.currentTimeMillis(), minutes);
        d.setDirty();
    }

    static DbData.Ball ball(DbData d, BallSet set, int star) { return star < 1 || star > DbRules.MAX_COUNT ? null : d.balls[set.ordinal()][star - 1]; }

    /** A ball made by the current scatter (or a creative one with no generation) of this set. */
    static boolean current(MinecraftServer s, ItemStack st, BallSet set) {
        int g = DragonBallItem.gen(st);
        return g < 0 || g == data(s).gen[set.ordinal()];
    }

    /** The Unique Guardian's doors. Is this ball of the current scatter? */
    public static boolean isCurrent(MinecraftServer s, ItemStack st, BallSet set) { return current(s, st, set); }

    /** Does the registry say this ball exists as an item (carried or lying), not waiting to be placed? */
    public static boolean ballExists(MinecraftServer s, BallSet set, int star) {
        DbData.Ball b = ball(data(s), set, star);
        return b != null && !b.pending && star <= DbConfig.count();
    }

    /** The only copy of this ball is gone for good: it returns to the world at a new far place of its own, the way a ball lost in the void does. */
    public static void returnToWorld(MinecraftServer s, BallSet set, int star) {
        if (star >= 1 && star <= DbConfig.count()) relocate(s, set, star);
    }

    static ServerLevel levelOf(MinecraftServer s, String dim) {
        ResourceLocation rl = ResourceLocation.tryParse(dim);
        return rl == null ? null : s.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
    }

    // ================================================================ scatter
    /**
     * Picks fresh far-apart places for every ball of a set (the overworld, a ring round the world spawn, inside the world border) and bumps the set's generation, so every ball of the old scatter that is still
     * somewhere turns to dust the next time it is touched. The balls are not spawned here: each row is {@code pending} until a player loads its chunk ({@link #sweep}). {@code stone}: they come as grey stones
     * (the lock after a wish). Returns the number of balls placed.
     */
    public static int scatter(MinecraftServer s, BallSet set, boolean stone) {
        DbData d = data(s);
        int o = set.ordinal(), n = DbConfig.count();
        for (int star = 1; star <= DbRules.MAX_COUNT; star++) {                              // the old balls on the ground go now; the ones in bags go when they are next touched (generation)
            DbData.Ball old = d.balls[o][star - 1];
            if (old != null && old.entity != null) {
                ServerLevel l = levelOf(s, old.dim);
                Entity e = l == null ? null : l.getEntity(old.entity);
                if (e instanceof ItemEntity) e.discard();
            }
            d.balls[o][star - 1] = null;
        }
        d.gen[o]++;
        ServerLevel ow = s.overworld();
        BlockPos sp = ow.getSharedSpawnPos();
        WorldBorder wb = ow.getWorldBorder();
        java.util.List<int[]> pts = DbRules.scatter(RNG, n, sp.getX(), sp.getZ(), DbConfig.scatterMin(), DbConfig.scatterMax(), DbConfig.scatterSeparation());   // the whole set in one go, so the separation holds
        for (int star = 1; star <= n; star++) {
            DbData.Ball b = d.balls[o][star - 1] = new DbData.Ball();
            int[] pt = pts.get(star - 1);
            b.dim = ow.dimension().location().toString();
            b.x = Math.max(wb.getMinX() + 16, Math.min(wb.getMaxX() - 16, pt[0])) + 0.5;
            b.z = Math.max(wb.getMinZ() + 16, Math.min(wb.getMaxZ() - 16, pt[1])) + 0.5;
            b.y = 0;
            b.pending = true;
            b.stone = stone;
            b.entity = null;
        }
        d.setDirty();
        LOG.info("[dragonballs] scattered the {} set: {} balls, generation {}, stone={}", set.id(), n, d.gen[o], stone);
        return n;
    }

    /** Scatters a set again only if some ball of it has no place at all (first start, or the count was raised). */
    static boolean incomplete(DbData d, BallSet set) {
        for (int star = 1; star <= DbConfig.count(); star++) if (d.balls[set.ordinal()][star - 1] == null) return true;
        return false;
    }

    /** One lost ball (it fell into the void) gets a fresh far place of its own. */
    static void relocate(MinecraftServer s, BallSet set, int star) {
        DbData d = data(s);
        DbData.Ball b = ball(d, set, star);
        if (b == null) b = d.balls[set.ordinal()][star - 1] = new DbData.Ball();
        ServerLevel ow = s.overworld();
        BlockPos sp = ow.getSharedSpawnPos();
        int[] pt = DbRules.scatter(RNG, 1, sp.getX(), sp.getZ(), DbConfig.scatterMin(), DbConfig.scatterMax(), 0).get(0);
        WorldBorder wb = ow.getWorldBorder();
        b.dim = ow.dimension().location().toString();
        b.x = Math.max(wb.getMinX() + 16, Math.min(wb.getMaxX() - 16, pt[0])) + 0.5;
        b.z = Math.max(wb.getMinZ() + 16, Math.min(wb.getMaxZ() - 16, pt[1])) + 0.5;
        b.y = 0;
        b.pending = true;
        b.stone = DbRules.locked(d.lockedUntil[set.ordinal()], System.currentTimeMillis());
        b.entity = null;
        d.setDirty();
        LOG.info("[dragonballs] the {}-star {} lost (void); it returns at a new place", star, set.id());
    }

    // ================================================================ the five second sweep (called from the server tick)
    /** Spawns the pending balls whose chunk is already loaded (at most 4 per pass) and lifts the stone from balls on the ground when the lock has run out. At most 16 rows. */
    static void sweep(MinecraftServer s) {
        DbData d = data(s);
        long now = System.currentTimeMillis();
        int spawned = 0;
        for (BallSet set : BallSet.values()) {
            int o = set.ordinal();
            boolean open = !DbRules.locked(d.lockedUntil[o], now);
            for (int star = 1; star <= DbRules.MAX_COUNT; star++) {
                DbData.Ball b = d.balls[o][star - 1];
                if (b == null) continue;
                ServerLevel l = levelOf(s, b.dim);
                if (l == null) continue;
                if (b.pending) {
                    if (spawned >= 4 || star > DbConfig.count()) continue;
                    int bx = (int) Math.floor(b.x), bz = (int) Math.floor(b.z);
                    if (l.getChunkSource().getChunkNow(bx >> 4, bz >> 4) == null) continue;           // never load a chunk for a ball
                    int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
                    if (y <= l.getMinBuildHeight()) continue;
                    ItemEntity ie = new ItemEntity(l, b.x, y + 0.4, b.z, DragonBallItem.make(set, star, d.gen[o], b.stone));
                    ie.setDeltaMovement(0, 0, 0);
                    l.addFreshEntity(ie);
                    b.pending = false;
                    b.y = y;
                    b.entity = ie.getUUID();
                    spawned++;
                    d.setDirty();
                } else if (b.stone && open) {
                    b.stone = false;
                    d.setDirty();
                    Entity e = b.entity == null ? null : l.getEntity(b.entity);
                    if (e instanceof ItemEntity ie && ie.getItem().getItem() instanceof DragonBallItem) {
                        ItemStack c = ie.getItem().copy();
                        DragonBallItem.setStone(c, false);
                        ie.setItem(c);
                    }
                }
            }
        }
    }

    // ================================================================ the registry, kept by the balls themselves
    /** A carried ball reports where it is (called every 5 seconds per ball by the item). A ball of an old scatter turns to dust; a stone ball whose lock ended wakes up. */
    static void onHeld(ServerLevel level, ServerPlayer p, ItemStack stack, BallSet set, int star) {
        MinecraftServer s = level.getServer();
        DbData d = data(s);
        if (!current(s, stack, set)) {
            stack.shrink(1);
            p.sendSystemMessage(DbMsg.info("crumble"));
            return;
        }
        if (star > DbConfig.count()) return;
        DbData.Ball b = ball(d, set, star);
        if (b == null) b = d.balls[set.ordinal()][star - 1] = new DbData.Ball();
        String dim = level.dimension().location().toString();
        boolean moved = !dim.equals(b.dim) || b.pending || b.entity != null || Math.abs(b.x - p.getX()) > 2 || Math.abs(b.z - p.getZ()) > 2;
        b.dim = dim;
        b.x = p.getX();
        b.y = p.getY();
        b.z = p.getZ();
        b.pending = false;
        b.entity = null;
        if (moved) d.setDirty();
        if (DragonBallItem.isStone(stack) && !DbRules.locked(d.lockedUntil[set.ordinal()], System.currentTimeMillis())) DragonBallItem.setStone(stack, false);
    }

    /** A ball item enters the world (dropped, thrown, scattered, chunk load): it cannot be destroyed, never despawns, glows, and its place is recorded. A ball of an old scatter is refused. */
    public static void onItemJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ItemEntity ie) || !(ie.getItem().getItem() instanceof DragonBallItem db)) return;
        MinecraftServer s = ie.getServer();
        if (s == null) return;
        if (!current(s, ie.getItem(), db.set())) { e.setCanceled(true); return; }
        ie.setInvulnerable(true);
        ie.setUnlimitedLifetime();
        ie.setGlowingTag(true);
        if (db.star() > DbConfig.count()) return;
        DbData d = data(s);
        DbData.Ball b = ball(d, db.set(), db.star());
        if (b == null) b = d.balls[db.set().ordinal()][db.star() - 1] = new DbData.Ball();
        b.dim = e.getLevel().dimension().location().toString();
        b.x = ie.getX();
        b.y = ie.getY();
        b.z = ie.getZ();
        b.pending = false;
        b.entity = ie.getUUID();
        d.setDirty();
    }

    /** A ball item that left the world below the void floor is sent to a new far place. (A ball picked up or unloaded is not below the floor, so nothing happens.) */
    public static void onItemLeave(EntityLeaveLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ItemEntity ie) || !(ie.getItem().getItem() instanceof DragonBallItem db)) return;
        Entity.RemovalReason r = ie.getRemovalReason();
        if (r == null || !r.shouldDestroy() || ie.getY() > e.getLevel().getMinBuildHeight() - 32) return;
        MinecraftServer s = ie.getServer();
        if (s != null && current(s, ie.getItem(), db.set()) && db.star() <= DbConfig.count()) relocate(s, db.set(), db.star());
    }

    // ================================================================ the ritual: gather the balls and start a session
    static final class Gather {
        final int[] slot;
        final ItemEntity[] ground;

        Gather(int n) {
            slot = new int[n];
            ground = new ItemEntity[n];
            java.util.Arrays.fill(slot, -1);
        }

        boolean has(int i) { return slot[i] >= 0 || ground[i] != null; }

        int found() {
            int c = 0;
            for (int i = 0; i < slot.length; i++) if (has(i)) c++;
            return c;
        }
    }

    /** The balls of one set the player can bring to the ritual: in the inventory, and lying on the ground within the gather radius. One entity query, called only when the words were spoken. */
    static Gather gather(ServerPlayer p, BallSet set) {
        MinecraftServer s = p.getServer();
        int n = DbConfig.count();
        Gather g = new Gather(n);
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.getItem() instanceof DragonBallItem b && b.set() == set && b.star() <= n && !g.has(b.star() - 1) && current(s, st, set)) g.slot[b.star() - 1] = i;
        }
        AABB box = p.getBoundingBox().inflate(DbConfig.gatherRadius());
        for (ItemEntity ie : p.level().getEntitiesOfClass(ItemEntity.class, box, x -> x.getItem().getItem() instanceof DragonBallItem)) {
            DragonBallItem b = (DragonBallItem) ie.getItem().getItem();
            if (b.set() == set && b.star() <= n && !g.has(b.star() - 1) && current(s, ie.getItem(), set)) g.ground[b.star() - 1] = ie;
        }
        return g;
    }

    /** The words were spoken. Returns the reason the dragon does not come, or null when the ritual started. */
    public static Component trySummon(ServerPlayer p, BallSet set) {
        if (!DbConfig.enabled()) return DbMsg.bad("disabled");
        if (!p.isAlive() || p.isSpectator()) return DbMsg.bad("not_now");
        MinecraftServer s = p.getServer();
        if (DbSession.of(set) != null) return DbMsg.bad("busy", set.dragonName());
        long rem = lockRemainingMs(s, set);
        if (rem > 0) return DbMsg.bad("locked", set.ballName(), DbRules.format(rem));
        Gather g = gather(p, set);
        if (g.found() < DbConfig.count()) return DbMsg.bad("missing", DbConfig.count(), g.found(), set.ballName());
        Inventory inv = p.getInventory();
        for (int i = 0; i < g.slot.length; i++) {
            if (g.slot[i] >= 0) inv.setItem(g.slot[i], ItemStack.EMPTY);
            else g.ground[i].discard();
        }
        DbData d = data(s);
        d.active[set.ordinal()] = true;
        d.setDirty();
        DbSession.start(p, set);
        return null;
    }

    /** A ritual is over: the balls scatter (as stones, with the lock, when a wish was granted) and the set is free again. */
    static void finish(MinecraftServer s, BallSet set, boolean granted) {
        DbData d = data(s);
        boolean stone = granted && DbConfig.lockMinutes() > 0;
        scatter(s, set, stone);
        if (granted) d.lockedUntil[set.ordinal()] = DbRules.lockUntil(System.currentTimeMillis(), DbConfig.lockMinutes());
        d.active[set.ordinal()] = false;
        d.setDirty();
    }

    // ================================================================ deaths (revive wish)
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        long now = System.currentTimeMillis();
        if (DEATHS.size() >= MAX_DEATHS) DEATHS.values().removeIf(x -> now - x.atMs() > 3_600_000L * 24);
        if (DEATHS.size() >= MAX_DEATHS) {                                                    // still full: drop the oldest
            UUID oldest = null;
            long at = Long.MAX_VALUE;
            for (Map.Entry<UUID, Death> en : DEATHS.entrySet()) if (en.getValue().atMs() < at) { at = en.getValue().atMs(); oldest = en.getKey(); }
            if (oldest != null) DEATHS.remove(oldest);
        }
        DEATHS.put(p.getUUID(), new Death(p.level().dimension().location().toString(), p.getX(), p.getY(), p.getZ(), now));
    }

    /** The death of a player if it is inside the revive window, else null. */
    static Death death(UUID id) {
        Death d = DEATHS.get(id);
        return d != null && System.currentTimeMillis() - d.atMs() <= DbConfig.reviveMinutes() * 60_000L ? d : null;
    }

    static void forgetDeath(UUID id) { DEATHS.remove(id); }

    static void putDeath(UUID id, Death d) { DEATHS.put(id, d); }

    // ================================================================ radar
    private static final String[] DIRS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    /** The compass word of an offset (east +x, south +z). */
    static String direction(double dx, double dz) {
        double deg = Math.toDegrees(Math.atan2(dx, -dz));
        int i = (int) Math.round(((deg % 360) + 360) % 360 / 45.0) % 8;
        return DIRS[i];
    }

    /** The action bar line of the Dragon Radar: the nearest ball of either set within the radar range, from the registry (at most 16 rows). */
    public static Component radar(ServerPlayer p) {
        DbData d = data(p.getServer());
        String dim = p.level().dimension().location().toString();
        double best = Double.MAX_VALUE;
        BallSet bs = null;
        int bstar = 0;
        double bdx = 0, bdz = 0;
        for (BallSet set : BallSet.values())
            for (int star = 1; star <= DbConfig.count(); star++) {
                DbData.Ball b = d.balls[set.ordinal()][star - 1];
                if (b == null || !dim.equals(b.dim)) continue;
                double dx = b.x - p.getX(), dz = b.z - p.getZ(), q = dx * dx + dz * dz;
                if (q < best) { best = q; bs = set; bstar = star; bdx = dx; bdz = dz; }
            }
        double range = DbConfig.radarRange();
        if (bs == null || best > range * range) return DbMsg.info("radar.none", (int) range);
        return DbMsg.info("radar.found", bs.ballName(), bstar, (int) Math.round(Math.sqrt(best)), direction(bdx, bdz));
    }

    // ================================================================ server events
    public static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer s = e.getServer();
        long t = s.getTickCount();
        if (t % 20 == 0) DbSession.tickAll(s, t);
        if (t % 100 == 50 && DbConfig.enabled()) sweep(s);
    }

    public static void onServerStarted(ServerStartedEvent e) {
        MinecraftServer s = e.getServer();
        DbData d = data(s);
        for (BallSet set : BallSet.values()) {
            if (d.active[set.ordinal()]) {                                                    // a ritual was running when the server stopped: the balls scatter again, no lock
                LOG.info("[dragonballs] a {} ritual was interrupted; its balls scatter again", set.id());
                finish(s, set, false);
            } else if (DbConfig.enabled() && DbConfig.autoScatter() && incomplete(d, set)) {
                scatter(s, set, DbRules.locked(d.lockedUntil[set.ordinal()], System.currentTimeMillis()));
            }
        }
    }

    public static void onServerStopping(ServerStoppingEvent e) {
        DbSession.endAll(e.getServer(), "server stopping");
        DEATHS.clear();
        DbChat.clear();
    }
}
