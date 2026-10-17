package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.Expander;
import com.newuniverse.nusmp.labyrinth.mega.MegaConfig;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: this is the Expansion Tracker's engine and the only {@link Expander}. Opening a sector's Gatekeeper door (or an admin's {@code /multiverse mega expand})
 * asks for the next sector; the request must return at once, because it arrives from a block interaction or a command, so it only records the new sector in
 * the saved data (state GENERATING, with the seed, the entry door and the region the planner chose) and queues a job. The job then runs over the following
 * server ticks: one tick to plan (a few milliseconds of pure arithmetic), then a few pieces per tick within a time budget so the server keeps its pace, then
 * one tick to finish (fill the sector's rooms and landmarks, open the old sector's rubble seal so the tunnel is walkable, mark the sector ACTIVE, save, tell the
 * listeners). One job runs at a time and the queue is first in, first out, so sectors always appear in order. Players in the dimension see a "the Labyrinth
 * shifts" line with the progress, and the players standing in the old sector's antechamber or Way Onward are told when the way is open. A sector that was
 * GENERATING when the server stopped is simply planned and placed again from its saved seed (the plan is a function of saved data, so it is the same sector;
 * decorative entities of the unfinished run are cleared first). A sector that cannot be built is marked FAILED with the reasons in the log and can be retried
 * with {@code /multiverse mega regen}. Nothing here ever throws out of the tick.
 */
public final class ExpansionQueue implements Expander {
    private static final Logger LOG = LogUtils.getLogger();
    private static final ExpansionQueue INSTANCE = new ExpansionQueue();
    private static final int ANNOUNCE_EVERY = 40;
    private static final int WARM_PER_TICK = 6;

    private enum Stage { PLAN, PLACE, FINISH }

    private static final class Job {
        final int sectorId;
        final boolean repair;
        /** The state the sector had before an admin regenerated it (a sector past ACTIVE keeps its progress), or null for a sector that is being generated for the first time. */
        final Sector.State restore;
        Stage stage = Stage.PLAN;
        SectorGenerator.Planned planned;
        SectorGenerator.Built built;
        SectorPlacer placer;
        long startNanos = -1;

        Job(int sectorId, boolean repair, Sector.State restore) {
            this.sectorId = sectorId;
            this.repair = repair;
            this.restore = restore;
        }

        float progress() {
            if (stage == Stage.PLAN || placer == null) return 0f;
            if (stage == Stage.FINISH) return 0.99f;
            return Math.min(0.99f, 0.02f + 0.97f * placer.placed() / Math.max(1, placer.total()));
        }
    }

    private final ArrayDeque<Job> queue = new ArrayDeque<>();
    private Job current;
    private long ticks;

    private ExpansionQueue() {}

    public static ExpansionQueue get() { return INSTANCE; }

    // ================================================================================================================ Expander

    @Override
    public boolean requestFirst(MinecraftServer server) { return tryFirst(server) == null; }

    @Override
    public boolean requestNext(ServerLevel level, Sector from) {
        String why = tryNext(level, from);
        if (why != null) LOG.info("[nusmp] Mega expansion off sector {} refused: {}", from == null ? -1 : from.id, why);
        return why == null;
    }

    @Override
    public float progress() {
        if (current != null) return current.progress();
        return queue.isEmpty() ? -1f : 0f;
    }

    /** Queues sector 0. Returns null on success, else the reason it was refused. */
    public String tryFirst(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return "the Mega-Dungeon dimension is not loaded";
        MegaData data = MegaData.get(server);
        if (!data.sectors().isEmpty()) return "sector 0 already exists";
        if (data.seed == 0L) {
            long seed = Rng.mix(server.overworld().getSeed(), 0x4D454741L);
            data.seed = seed == 0L ? 1L : seed;
            data.setDirty();
        }
        SectorPlanner.Plan plan = SectorPlanner.first(data.seed);
        Sector s = new Sector(0);
        s.theme = RegionTheme.GRAVITY;
        s.threat = 1;
        s.state = Sector.State.GENERATING;
        s.seed = plan.seed();
        s.region = SectorGenerator.bb(plan.region());
        s.entry = SectorGenerator.pos(plan.entryFloor());
        s.forward = SectorGenerator.dir(plan.forward());
        s.relicCount = s.theme.relicCount(data.seed, 0);
        data.add(s);
        queue.add(new Job(0, false, null));
        LOG.info("[nusmp] Mega sector 0 queued (seed {}).", s.seed);
        return null;
    }

    /** Queues the sector behind {@code from}'s Way Onward. Returns null on success, else the reason it was refused. */
    public String tryNext(ServerLevel level, Sector from) {
        if (level == null || from == null) return "no sector";
        MinecraftServer server = level.getServer();
        MegaData data = MegaData.get(server);
        if (from.nextId != -1) return "sector " + from.id + " already leads to sector " + from.nextId;
        if (from.state == Sector.State.GENERATING) return "sector " + from.id + " is still generating";
        if (from.state == Sector.State.FAILED) return "sector " + from.id + " failed to generate (retry it with /multiverse mega regen " + from.id + ")";
        if (from.expansionPos == null || from.expansionFacing == null) return "sector " + from.id + " has no Way Onward";
        if (data.sectors().size() >= MegaConfig.get(MegaConfig.MAX_SECTORS)) return "the dungeon reached maxSectors (" + MegaConfig.get(MegaConfig.MAX_SECTORS) + ")";
        SectorPlanner.Next n = planBehind(data, from);
        if (n == null) return "no room behind the seal of sector " + from.id + " (older sectors are in the way)";
        Sector s = draft(data, from, n);
        int id = s.id;
        from.nextId = id;
        data.add(s);
        queue.add(new Job(id, false, null));
        LOG.info("[nusmp] Mega sector {} queued behind sector {} (theme {}, {} tunnel segments, seed {}).", id, from.id, s.theme.id(), n.linkModules(), s.seed);
        return null;
    }

    /** Where the sector behind {@code from}'s Way Onward would go (region, entry door, tunnel length), or null when older sectors leave no room. Changes nothing. */
    static SectorPlanner.Next planBehind(MegaData data, Sector from) {
        return SectorPlanner.planNext(SectorGenerator.vec(from.expansionPos), SectorGenerator.facing(from.expansionFacing), SectorGenerator.box(from.region),
                SectorGenerator.obstacles(data, Integer.MAX_VALUE, from.id), MegaConfig.get(MegaConfig.MAX_LINK_MODULES));
    }

    /** The sector that is created for the plan {@code n} behind {@code from}: next id, theme by the dungeon seed, threat, the seed of its plan. Not added to the data. */
    static Sector draft(MegaData data, Sector from, SectorPlanner.Next n) {
        Sector latest = data.latest();
        int id = latest == null ? 0 : latest.id + 1;
        Sector s = new Sector(id);
        s.theme = RegionTheme.pick(data.seed, id, from.theme);
        s.threat = 1 + id;
        s.state = Sector.State.GENERATING;
        s.seed = SectorPlanner.sectorSeed(data.seed, id);
        s.region = SectorGenerator.bb(n.region());
        s.entry = SectorGenerator.pos(n.entry().add(0, -1, 0));
        s.forward = SectorGenerator.dir(n.forward());
        s.relicCount = s.theme.relicCount(data.seed, id);
        return s;
    }

    /** Queues a fresh generation of an existing sector from its saved data ({@code /multiverse mega regen}, and the repair after a crash). */
    public String tryRegen(MinecraftServer server, int sectorId, boolean repair) {
        MegaData data = MegaData.get(server);
        Sector s = data.sector(sectorId);
        if (s == null) return "no sector " + sectorId;
        if (current != null && current.sectorId == sectorId) return "sector " + sectorId + " is being generated right now";
        for (Job j : queue) if (j.sectorId == sectorId) return "sector " + sectorId + " is already queued";
        Sector.State before = s.state == Sector.State.GENERATING || s.state == Sector.State.FAILED ? null : s.state;
        s.state = Sector.State.GENERATING;
        data.setDirty();
        queue.add(new Job(sectorId, true, before));
        LOG.info("[nusmp] Mega sector {} queued for {}.", sectorId, repair ? "repair" : "regeneration");
        return null;
    }

    /** Is a job queued or running for this sector? */
    public boolean working(int sectorId) {
        if (current != null && current.sectorId == sectorId) return true;
        for (Job j : queue) if (j.sectorId == sectorId) return true;
        return false;
    }

    /** The id of the sector being generated, or -1. */
    public int currentSector() { return current == null ? -1 : current.sectorId; }

    // ================================================================================================================ server events

    /** Sectors that were GENERATING when the server stopped are planned and placed again. */
    public void onServerStarted(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return;
        MegaData data = MegaData.get(server);
        VanillaPools.get().bind(level.getStructureManager());
        List<Sector> unfinished = new ArrayList<>();
        for (Sector s : data.sectors()) if (s.state == Sector.State.GENERATING && !working(s.id)) unfinished.add(s);
        for (Sector s : unfinished) {
            queue.add(new Job(s.id, true, null));
            LOG.info("[nusmp] Mega sector {} was not finished when the server stopped; it is generated again from its saved seed.", s.id);
        }
    }

    public void onServerStopped() {
        queue.clear();
        current = null;
        ticks = 0;
        VanillaPools.get().unbind();
    }

    /** Called once per server tick. */
    public void tick(MinecraftServer server) {
        ticks++;
        try {
            if (current == null && queue.isEmpty()) {
                if (com.newuniverse.nusmp.labyrinth.Labyrinth.layoutIsMega() && ticks % 2 == 0) {
                    ServerLevel lv = MegaDungeon.level(server);
                    if (lv != null) {
                        VanillaPools.get().bind(lv.getStructureManager());
                        VanillaPools.get().warm(WARM_PER_TICK);
                    }
                }
                return;
            }
            if (current == null) current = queue.poll();
            ServerLevel level = MegaDungeon.level(server);
            MegaData data = MegaData.get(server);
            Sector s = data.sector(current.sectorId);
            if (level == null || s == null) {
                LOG.error("[nusmp] Mega job for sector {} dropped: {}", current.sectorId, level == null ? "the dimension is not loaded" : "the sector no longer exists");
                if (s != null) fail(data, s, "the Mega-Dungeon dimension is not loaded");
                current = null;
                return;
            }
            switch (current.stage) {
                case PLAN -> plan(level, data, s);
                case PLACE -> place(level, s);
                case FINISH -> finish(server, level, data, s);
            }
            if (current != null && ticks % ANNOUNCE_EVERY == 0) announce(level, current.progress());
        } catch (Throwable t) {
            LOG.error("[nusmp] The Mega-Dungeon generator failed: {}", t.toString(), t);
            try {
                if (current != null) {
                    MegaData data = MegaData.get(server);
                    Sector s = data.sector(current.sectorId);
                    if (s != null) fail(data, s, "internal error: " + t);
                }
            } catch (Throwable ignored) {
                // the tick must never throw
            }
            current = null;
        }
    }

    // ================================================================================================================ the job

    private void plan(ServerLevel level, MegaData data, Sector s) {
        Job j = current;
        j.startNanos = System.nanoTime();
        SectorGenerator.Planned planned = SectorGenerator.plan(level, data, s);
        if (!planned.ok()) {
            fail(data, s, planned.failure());
            return;
        }
        j.planned = planned;
        j.built = SectorGenerator.build(level, planned);
        j.placer = new SectorPlacer(level, s, j.built, j.repair);
        j.stage = Stage.PLACE;
    }

    private void place(ServerLevel level, Sector s) {
        Job j = current;
        long budget = MegaConfig.get(MegaConfig.GEN_BUDGET_MS) * 1_000_000L;
        j.placer.step(MegaConfig.get(MegaConfig.PIECES_PER_TICK), System.nanoTime() + budget);
        if (j.placer.done()) j.stage = Stage.FINISH;
    }

    private void finish(MinecraftServer server, ServerLevel level, MegaData data, Sector s) {
        Job j = current;
        SectorGenerator.fill(s, j.built);
        s.state = j.restore != null ? j.restore : Sector.State.ACTIVE;
        Sector prev = SectorGenerator.prevOf(data, s);
        int opened = 0;
        if (prev != null) opened = SectorPlacer.clearSeal(level, prev);
        Sector next = s.nextId >= 0 ? data.sector(s.nextId) : null;
        if (next != null && SectorGenerator.playable(next)) SectorPlacer.clearSeal(level, s);       // a regenerated sector has its rubble back, but the tunnel to its successor exists
        long ms = (System.nanoTime() - j.startNanos) / 1_000_000L;
        GenData.Stat st = SectorGenerator.stat(j.planned, ms);
        GenData.get(server).put(s.id, st);
        data.setDirty();
        LOG.info("MEGA sector {} {} rooms={} edges={} pieces={} loops={} tries={} ms={}", s.id, s.state.name(), s.rooms.size(), s.edges.size(), st.pieces, st.loops, st.tries, ms);
        if (st.fallback) LOG.warn("MEGA sector {} used the FALLBACK layout (only the Landing and the boss chain) after {} assemble tries: {}", s.id, st.tries - 1, SectorSolver.summary(j.planned.solved().failures(), 3));
        else if (st.tries > 1) LOG.warn("MEGA sector {} needed {} assemble tries: {}", s.id, st.tries, SectorSolver.summary(j.planned.solved().failures(), 3));
        if (st.deadEnd) LOG.warn("MEGA sector {} is a DEAD END: its Way Onward faces older sectors, so no sector can follow it.", s.id);
        if (j.placer.failed() > 0) LOG.error("MEGA sector {} had {} piece(s) that failed to place; see the errors above.", s.id, j.placer.failed());
        current = null;
        try {
            MegaDungeon.generated(level, s);
        } catch (Throwable t) {
            LOG.error("[nusmp] A listener failed while sector {} was announced as generated: {}", s.id, t.toString(), t);
        }
        if (prev != null) tellWayOpen(level, prev, opened);
    }

    /**
     * Gives a sector up. A sector that was being generated for the first time becomes FAILED (the other systems ignore it, and {@code regen} can retry). A sector an admin
     * REGENERATED keeps the state it had: its old blocks and its progress are still in the world, and hiding it from every system because a re-plan failed would be worse
     * than keeping the old layout.
     */
    private void fail(MegaData data, Sector s, String why) {
        Job j = current;
        if (j != null && j.restore != null) {
            s.state = j.restore;
            LOG.error("MEGA sector {} regeneration FAILED, the old sector is kept as {}: {}", s.id, s.state.name(), why);
        } else {
            s.state = Sector.State.FAILED;
            LOG.error("MEGA sector {} FAILED: {}", s.id, why);
        }
        data.setDirty();
        current = null;
    }

    // ================================================================================================================ feedback

    private static void announce(ServerLevel level, float progress) {
        int pct = Math.round(progress * 100f);
        Component line = Component.translatable("message.nusmp.mega.core.shifting", pct);
        for (ServerPlayer p : level.players()) p.displayClientMessage(line, true);
    }

    /** Tells the players who stand in the old sector's antechamber or Way Onward that the way is open. */
    private static void tellWayOpen(ServerLevel level, Sector prev, int opened) {
        Component msg = Component.translatable("message.nusmp.mega.core.way_open");
        for (ServerPlayer p : level.players()) {
            BlockPos at = p.blockPosition();
            if (!prev.containsRegion(at)) continue;
            com.newuniverse.nusmp.labyrinth.mega.Room room = prev.roomAt(at);
            boolean near = room != null && (room.kind == RoomKind.ANTECHAMBER || room.kind == RoomKind.ONWARD || room.kind == RoomKind.VAULT);
            if (!near && prev.expansionPos != null) near = at.distSqr(prev.expansionPos) < 48.0 * 48.0;
            if (near) p.sendSystemMessage(msg);
        }
        if (opened > 0) LOG.info("[nusmp] Mega sector {}'s rubble seal was opened ({} blocks).", prev.id, opened);
    }

    /** One line about a facing, for logs and commands. */
    static String describe(Direction d) { return d == null ? "none" : d.getName().toLowerCase(Locale.ROOT); }
}
