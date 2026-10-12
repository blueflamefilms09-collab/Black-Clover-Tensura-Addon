package com.newuniverse.nusmp.dungeon.nexus;

import com.newuniverse.nusmp.dungeon.DungeonConfig;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.dungeon.DungeonMaw;
import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.LegacySeal;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.city.CityBuilder;
import com.newuniverse.nusmp.dungeon.city.CitySave;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the Variable Core where players come near it: the Golden City's lazy builder (dungeon/city/CityBuilder), made zone-agnostic. Every
 * 40 ticks it looks for a player of the Dungeon within 56 blocks of the centre of a core cell that is not built yet; it picks the zone of
 * the cell ({@link NexusSave#zoneOf}), asks that zone's {@link ZoneSite} for its pure {@link ZonePlan}, and applies the plan's ops and the
 * zone's extra stages ({@link StageSequence}) over several ticks, a few thousand blocks at a time, only while every chunk of the footprint
 * is loaded, saving its progress so a restart carries on. When the last stage is done the cell is DONE and the site populates it.
 * It writes only into the Dungeon's own level, and every write is inside {@link DungeonGuard#internal}.
 *
 * <p>The same tick handler watches the seals (the first player into a core cell breaks its seal: a server-wide broadcast) and calls the
 * sites' {@link ZoneSite#tickPlayers} and {@link ZoneSite#tickNear}. It replaces {@code CityBuilder.onServerTick}, which must no
 * longer be registered; a Golden City of the older builder is adopted ({@link NexusZones#resolve}).
 */
public final class NexusBuilder {
    private NexusBuilder() {}

    /** Units of work a tick (a write is 4, a block that is already right is 1): at most 3000 blocks written. */
    private static final long BUDGET = 12_000L;
    private static final double RANGE = 56.0, WATCH_RANGE = 96.0;
    private static final int SCAN_EVERY = 40, SEAL_EVERY = 5, PLAYERS_EVERY = 20, NEAR_EVERY = 100;
    /** Blocks round the footprint that still count as inside it. */
    private static final int EDGE = 2;
    /** A player this close to the footprint's edge (outside it) starts the build even when the middle is farther than RANGE. */
    private static final int TRIGGER_MARGIN = 16;
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    private static Job job;
    private static ServerLevel seen;
    private static boolean primed;
    /** After a failure the builder rests until this tick, so one bad spot cannot flood the log. */
    private static long pausedFrom, pausedUntil;
    private static final Map<String, Long> LAST_WARN = new HashMap<>();

    /** The Nexus being built right now. */
    private static final class Job {
        final ServerLevel level;
        final long key;
        final ZoneSite site;
        final ZonePlan plan;
        final StageSequence seq;
        final List<int[]> chunks;
        int idle;

        Job(ServerLevel level, long key, ZoneSite site, ZonePlan plan, StageSequence seq) {
            this.level = level;
            this.key = key;
            this.site = site;
            this.plan = plan;
            this.seq = seq;
            this.chunks = plan.chunks();
        }
    }

    // ---------------------------------------------------------------- the server tick
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!DungeonConfig.get(DungeonConfig.ENABLED)) return;
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(LivingDungeon.DIMENSION);
        if (level == null) {
            clear();
            return;
        }
        long tick = server.getTickCount();
        if (level != seen) {                                               // a new world: forget what was left over from another
            clear();
            seen = level;
        }
        try {
            if (!primed) {
                NexusSave.get(server);                                     // made now, before anyone is down there: it notes whether the older seal flag was already set
                primed = true;
            }
            if (tick % SEAL_EVERY == 2) watchSeals(server, level);
            if (tick % PLAYERS_EVERY == 7) sweep(server, level, tick % NEAR_EVERY == 7);
        } catch (RuntimeException failure) {
            warn("watch", "[nusmp] The Variable Core's watch over its players failed", failure, tick);
        }
        if (tick >= pausedFrom && tick < pausedUntil) return;
        try {
            if (job != null && job.level != level) job = null;
            if (job != null) {
                if (!work(server, level, job)) job = null;
            } else if (tick % SCAN_EVERY == 29) {
                job = start(server, level);
            }
        } catch (RuntimeException | LinkageError failure) {
            LOG.error("[nusmp] The Variable Core builder failed; the job is dropped and tried again in a minute", failure);
            job = null;
            pausedFrom = tick;
            pausedUntil = tick + 1200;
        }
    }

    /** Forgets everything held about a world: the running job, the level it was for, the rest after a failure. */
    private static void clear() {
        job = null;
        seen = null;
        primed = false;
        pausedFrom = 0;
        pausedUntil = 0;
        LAST_WARN.clear();
    }

    /** The world is closing: nothing of it (the Dungeon's level, a half-done job) may stay behind in the statics. */
    public static void onServerStopping(ServerStoppingEvent event) { clear(); }

    private static void warn(String what, String message, Throwable failure, long tick) {
        Long last = LAST_WARN.get(what);
        if (last != null && tick >= last && tick - last < 1200) return;
        LAST_WARN.put(what, tick);
        LOG.warn(message, failure);
    }

    // ---------------------------------------------------------------- seals
    /** The first player into a core cell breaks its seal; the server is told, once. */
    private static void watchSeals(MinecraftServer server, ServerLevel level) {
        NexusSave save = null;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.getBlockY() >= DungeonTier.CITY_Y) continue;
            int cx = Math.floorDiv(p.getBlockX(), NexusZone.CELL), cz = Math.floorDiv(p.getBlockZ(), NexusZone.CELL);
            if (!DungeonTier.isCityCell(cx, cz)) continue;
            if (save == null) save = NexusSave.get(server);
            NexusZones.resolve(server, save, cx, cz);
            long key = NexusSave.key(cx, cz);
            if (save.sealBroken(key)) continue;
            boolean announce = NexusText.announceSeal(save.legacyWorld(), LegacySeal.broken(server), save.sealedCount());
            save.breakSeal(key);
            LegacySeal.markBroken(server);                                 // the older flow stays quiet from now on
            if (announce) {
                DungeonMaw.broadcast(server, Component.literal(NexusText.sealBreached(p.getName().getString(), save.zoneOf(cx, cz).label))
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            }
        }
    }

    // ---------------------------------------------------------------- players in and near the built Nexuses
    private static void sweep(MinecraftServer server, ServerLevel level, boolean withNear) {
        NexusSave save = null;
        Map<Long, List<ServerPlayer>> inside = new LinkedHashMap<>(), near = new LinkedHashMap<>();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            int pcx = Math.floorDiv(p.getBlockX(), NexusZone.CELL), pcz = Math.floorDiv(p.getBlockZ(), NexusZone.CELL);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int cx = pcx + dx, cz = pcz + dz;
                    if (!DungeonTier.isCityCell(cx, cz)) continue;
                    if (save == null) save = NexusSave.get(server);
                    NexusSave.Entry e = NexusZones.resolve(server, save, cx, cz);
                    if (e == null || e.state != NexusState.DONE || e.fp == null) continue;
                    long key = NexusSave.key(cx, cz);
                    boolean in = e.fp.contains(p.getBlockX(), p.getBlockZ(), EDGE) && p.getBlockY() >= e.floorY - ZonePlan.BELOW && p.getBlockY() <= e.topY + ZonePlan.ABOVE;
                    if (in) inside.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
                    double ex = p.getX() - e.fp.centreX(), ez = p.getZ() - e.fp.centreZ();
                    if (withNear && ex * ex + ez * ez <= WATCH_RANGE * WATCH_RANGE) near.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
                }
            }
        }
        if (save == null) return;
        for (Map.Entry<Long, List<ServerPlayer>> me : inside.entrySet()) callSite(save, level, me.getKey(), me.getValue(), false);
        for (Map.Entry<Long, List<ServerPlayer>> me : near.entrySet()) callSite(save, level, me.getKey(), me.getValue(), true);
    }

    private static void callSite(NexusSave save, ServerLevel level, long key, List<ServerPlayer> players, boolean isNear) {
        NexusSave.Entry e = save.lookup(key);
        if (e == null || e.zone == null || e.fp == null) return;
        int cx = NexusCells.cellX(key), cz = NexusCells.cellZ(key);
        BlockPos centre = new BlockPos(e.fp.centreX(), e.floorY, e.fp.centreZ());
        try {
            ZoneSite site = NexusZones.site(e.zone);
            if (isNear) site.tickNear(level, cx, cz, centre, players);
            else site.tickPlayers(level, cx, cz, centre, players);
        } catch (RuntimeException | LinkageError failure) {
            warn((isNear ? "near " : "players ") + e.zone, "[nusmp] The " + e.zone.label + " failed while players were " + (isNear ? "near" : "inside"), failure, level.getServer().getTickCount());
        }
    }

    // ---------------------------------------------------------------- starting a job
    /** A job for the first unbuilt Nexus that a player is close to and nobody stands inside of; null if there is none. */
    private static Job start(MinecraftServer server, ServerLevel level) {
        NexusSave save = null;
        for (ServerPlayer p : level.players()) {
            BlockPos at = p.blockPosition();
            int pcx = Math.floorDiv(at.getX(), NexusZone.CELL), pcz = Math.floorDiv(at.getZ(), NexusZone.CELL);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int cx = pcx + dx, cz = pcz + dz;
                    if (!DungeonTier.isCityCell(cx, cz)) continue;
                    if (save == null) save = NexusSave.get(server);
                    NexusSave.Entry e = NexusZones.resolve(server, save, cx, cz);
                    if (e != null && e.state.built()) continue;
                    NexusZone zone = save.zoneOf(cx, cz);
                    ZonePlan plan = NexusZones.plan(zone, cx, cz);
                    double ex = at.getX() - plan.centreX(), ez = at.getZ() - plan.centreZ();
                    boolean close = ex * ex + ez * ez <= RANGE * RANGE || plan.footprint.contains(at.getX(), at.getZ(), TRIGGER_MARGIN);        // a wide Nexus is more than RANGE from its middle to its edge
                    if (!close || someoneInside(level, plan)) continue;
                    long key = NexusSave.key(cx, cz);
                    e = save.begin(key, zone, plan);
                    return job(level, key, NexusZones.site(zone), plan, e.stage, e.op, e.cell);
                }
            }
        }
        return null;
    }

    private static Job job(ServerLevel level, long key, ZoneSite site, ZonePlan plan, int stage, int op, long cell) {
        List<ZoneStage> extra = site.stages(plan);
        NexusSink sink = new NexusSink(level);
        StageSequence.Opener opener = (index, savedOp, savedCell) -> index == 0 ? opsStage(NexusCursor.resume(plan, savedOp), sink) : extra.get(index - 1).begin(level, savedOp, savedCell);
        StageSequence seq = DungeonGuard.internalGet(() -> new StageSequence(1 + extra.size(), opener, stage, op, cell));     // the first stage is opened here
        return new Job(level, key, site, plan, seq);
    }

    /** The plan's own ops as stage 0. */
    private static StageSequence.Stage opsStage(NexusCursor cursor, NexusSink sink) {
        return new StageSequence.Stage() {
            @Override
            public long run(long budget) { return cursor.run(sink, budget); }

            @Override
            public boolean done() { return cursor.done(); }

            @Override
            public int op() { return cursor.op(); }

            @Override
            public long cell() { return cursor.cell(); }
        };
    }

    // ---------------------------------------------------------------- a job's tick
    /** One tick of work. False once the job is over (done, or given up because nobody is about). */
    private static boolean work(MinecraftServer server, ServerLevel level, Job j) {
        if (!someoneNear(level, j.plan, WATCH_RANGE)) {
            if (++j.idle > 400) return false;
        } else {
            j.idle = 0;
        }
        for (int[] c : j.chunks) if (level.getChunkSource().getChunkNow(c[0], c[1]) == null) return true;      // waits until the whole footprint is loaded
        NexusSave save = NexusSave.get(server);
        DungeonGuard.internal(() -> j.seq.run(BUDGET));
        if (!j.seq.done()) {
            save.progress(j.key, j.seq.stage(), j.seq.op(), j.seq.cell());
            return true;
        }
        finish(level, j.site, j.plan, j.key, save, true);
        return false;
    }

    private static boolean someoneNear(ServerLevel level, ZonePlan plan, double range) {
        for (ServerPlayer p : level.players()) {
            double ex = p.getX() - plan.centreX(), ez = p.getZ() - plan.centreZ();
            if (ex * ex + ez * ez <= range * range) return true;
        }
        return false;
    }

    /** Is a player standing in the Nexus's volume? Nothing is built round them. */
    private static boolean someoneInside(ServerLevel level, ZonePlan plan) {
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            BlockPos at = p.blockPosition();
            if (plan.contains(at.getX(), at.getY(), at.getZ(), EDGE)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- finishing
    private static void finish(ServerLevel level, ZoneSite site, ZonePlan plan, long key, NexusSave save, boolean populate) {
        save.finish(key);                                                  // first: whatever fails below, the Nexus is never built twice
        try {
            rescue(level, plan);
        } catch (RuntimeException failure) {                               // a failed rescue must not cost the Nexus its relics and creatures
            LOG.warn("[nusmp] Could not move players out of the new {} of cell ({}, {})", plan.zone.label, plan.cellX, plan.cellZ, failure);
        }
        if (!populate) return;
        try {
            ZonePlan.Spot c = plan.centre();
            BlockPos centre = new BlockPos(c.x(), c.y(), c.z());
            DungeonGuard.internal(() -> site.populate(level, plan.cellX, plan.cellZ, centre));       // pedestals, gates and the like are the mod's own building
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The {} of cell ({}, {}) could not be populated", plan.zone.label, plan.cellX, plan.cellZ, failure);
        }
    }

    /** Anyone who was caught inside the new stonework is set down at the plan's rescue point. */
    private static void rescue(ServerLevel level, ZonePlan plan) {
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            BlockPos at = p.blockPosition();
            if (!plan.contains(at.getX(), at.getY(), at.getZ(), 0)) continue;
            if (!level.noCollision(p, p.getBoundingBox())) {
                ZonePlan.Spot to = plan.rescuePoint();
                p.teleportTo(level, to.x() + 0.5, to.y(), to.z() + 0.5, p.getYRot(), p.getXRot());
            }
        }
    }

    // ---------------------------------------------------------------- for the admin command
    /**
     * Builds a Nexus right now, in one go: loads and generates its chunks, runs every stage, saves it as built and (unless it was
     * built already) populates it. A collapsed one is built as if new. Returns the number of ops of the plan, or -1 outside the Dungeon.
     */
    public static int buildNow(ServerLevel level, int cellX, int cellZ) {
        if (!LivingDungeon.in(level)) return -1;
        MinecraftServer server = level.getServer();
        NexusSave save = NexusSave.get(server);
        long key = NexusSave.key(cellX, cellZ);
        NexusSave.Entry before = NexusZones.resolve(server, save, cellX, cellZ);
        NexusState was = before == null ? NexusState.UNBUILT : before.state;
        NexusZone zone = save.zoneOf(cellX, cellZ);
        if (was == NexusState.COLLAPSED) {
            save.forget(key);
            save.forceZone(key, zone);
            was = NexusState.UNBUILT;
        }
        if (job != null && job.key == key) job = null;
        ZonePlan plan = NexusZones.plan(zone, cellX, cellZ);
        for (int[] c : plan.chunks()) level.getChunk(c[0], c[1]);
        save.begin(key, zone, plan);
        Job j = job(level, key, NexusZones.site(zone), plan, 0, 0, 0L);
        DungeonGuard.internal(() -> j.seq.run(Long.MAX_VALUE));
        if (!j.seq.done()) LOG.warn("[nusmp] The {} of cell ({}, {}) has a stage that is waiting for something; it was saved as built anyway", zone.label, cellX, cellZ);
        finish(level, j.site, plan, key, save, was != NexusState.DONE);
        return plan.ops.size();
    }

    /** Drops the running job if it is this cell's (its saved record is forgotten by the caller). */
    public static void forget(long key) {
        if (job != null && job.key == key) job = null;
    }

    /** Forgets a cell for the admin command: the Nexus record and the older Golden City record (so it is not adopted again), and the job. */
    public static boolean reset(MinecraftServer server, int cellX, int cellZ) {
        long key = NexusSave.key(cellX, cellZ);
        forget(key);
        CityBuilder.forget(CitySave.key(cellX, cellZ));
        boolean legacy = CitySave.get(server).forget(CitySave.key(cellX, cellZ));
        boolean had = NexusSave.get(server).forget(key);
        return had || legacy;
    }
}
