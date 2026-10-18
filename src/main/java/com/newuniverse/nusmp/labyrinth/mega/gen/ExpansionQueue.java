package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.Expander;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaConfig;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.floors.FloorsConfig;
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
 *
 * <p>Two roads (1.55.0): the dungeon is two chains of sectors in the one dimension, the Holy road (floors +1, +2 ...) whose first sector stands at the origin and the
 * Abyssal road (floors -1, -2 ...) whose first sector stands {@code MegaGrid.LANE_Z} blocks away, and either can be opened first. So {@code requestFirst} with a polarity
 * creates the first sector of the named road and {@link #requestNext} always continues the road of the sector it is given. Sector ids stay global (the newest + 1, the one
 * number every other module keys its data by), so the road and the place in it (polarity and chain index) are what say where a sector belongs; those two, the number of
 * floors, the threat, the theme, the relic count and the seed are decided by {@link SectorDraft} when the sector is created. The next sector may be requested at any time
 * after the Guardian is dead (the rule is {@link ExpandRule#refusalForNext}): the facade holds the request back until the Crucible is resolved (so the relic taken can pick
 * the next theme, which {@code MegaDungeon.themeOverride} answers here), and the admin command asks at once.
 *
 * <p>The floor model (1.58.0, {@code floors.perSector = 1}, the default): a sector is ONE floor and floors are joined by stairs, not by tunnels. A floor stands in a
 * fixed slot of its road's lane ({@link FloorPlan#entry}), so creating one needs no neighbour planning at all ({@link #tryNextFloor}); the next floor is queued when a
 * party ENTERS a floor ({@link #onFloorReached}), so the stairs are normally ready by the time they are found, except behind a boss floor, whose next floor waits for the
 * Crucible exactly as before. A road whose sectors were saved with ten floors each keeps growing the old way (tunnel and Way Onward): the length of a sector is that of
 * the sector before it.
 *
 * <p>Parties (1.58.0): the dungeon is instanced per party, so a sector belongs to a party's chain ({@code Sector.party}) and every question the queue asks about "the
 * road" is asked about one party's road: the first floor of a road is created on the lane of the party that asked ({@link MegaGrid#laneZ(Polarity, int)}), the next floor
 * always continues the chain of the sector it follows, the limit {@code mega.maxSectors} is counted per party, and the "Labyrinth shifts" line goes only to the party that
 * is waiting for the floor. There is still ONE queue and one job at a time for the whole server, so parties that grow at the same moment wait for each other, in the
 * order they asked.
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
    public boolean requestFirst(MinecraftServer server) { return requestFirst(server, Polarity.HOLY); }

    @Override
    public boolean requestFirst(MinecraftServer server, Polarity polarity) { return requestFirst(server, polarity, 0); }

    @Override
    public boolean requestFirst(MinecraftServer server, Polarity polarity, int party) {
        String why = tryFirst(server, polarity, party);
        if (why != null) LOG.info("[nusmp] Mega first sector of the {} road of party {} refused: {}", polarity == null ? "?" : polarity.id(), party, why);
        return why == null;
    }

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

    /** Queues the first sector of the Holy road (1.54.0's sector 0). Returns null on success, else the reason it was refused. */
    public String tryFirst(MinecraftServer server) { return tryFirst(server, Polarity.HOLY); }

    /**
     * Queues the FIRST sector of a road: its Landing's door is the fixed cell of the road's lane ({@code (0, 64, MegaGrid.laneZ(road))}) and the sector grows east. The
     * sector gets the next global id, whichever road was opened first. Returns null on success, else the reason it was refused (the road already has a sector, even a
     * failed one: retry that one with {@code regen}).
     */
    public String tryFirst(MinecraftServer server, Polarity road) { return tryFirst(server, road, 0); }

    /**
     * 1.58.0 (party): the first sector of a road on the lane of party {@code party} ({@link MegaGrid#laneZ(Polarity, int)}); party 0 is the shared lane and behaves exactly
     * as before. A party has a road of its own, so a road that another party opened is no reason to refuse this one.
     */
    public String tryFirst(MinecraftServer server, Polarity road, int party) {
        if (road == null) return "no road named";
        if (party < 0 || party >= MegaGrid.MAX_PARTIES) return "party " + party + " has no lane (the lanes know the parties 0 to " + (MegaGrid.MAX_PARTIES - 1) + ")";
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return "the Mega-Dungeon dimension is not loaded";
        MegaData data = MegaData.get(server);
        Sector tail = data.chainTail(party, road);
        if (tail != null) return "the " + road.id() + " road " + ofParty(party) + "already has sector " + tail.id + " (" + tail.state.name() + ")";
        if (data.sectorCount(party) >= MegaConfig.get(MegaConfig.MAX_SECTORS)) return "the dungeon " + ofParty(party) + "reached maxSectors (" + MegaConfig.get(MegaConfig.MAX_SECTORS) + ")";
        if (data.seed == 0L) {
            long seed = Rng.mix(server.overworld().getSeed(), 0x4D454741L);
            data.seed = seed == 0L ? 1L : seed;
            data.setDirty();
        }
        Sector latest = data.latest();
        int id = latest == null ? 0 : latest.id + 1;
        int per = FloorMap.floorsPerSector(FloorsConfig.get(FloorsConfig.PER_SECTOR));
        SectorDraft draft = SectorDraft.first(data.seed, id, road, per, party);
        SectorPlanner.Plan plan;
        if (FloorMap.isFloorModel(per)) {
            Request.Floor spec = FloorPlan.spec(road, 0, MegaConfig.get(MegaConfig.FLOOR_ROOMS));
            plan = SectorPlanner.floor(data.seed, id, road, 0, spec.rooms(), spec.boss(), party);
        } else {
            plan = SectorPlanner.first(data.seed, id, road, party);
        }
        Sector s = create(draft);
        s.seed = plan.seed();
        s.region = SectorGenerator.bb(plan.region());
        s.entry = SectorGenerator.pos(plan.entryFloor());
        s.forward = SectorGenerator.dir(plan.forward());
        data.add(s);
        queue.add(new Job(id, false, null));
        LOG.info("[nusmp] Mega sector {} queued: the first of the {} road {}(floors {}..{}, theme {}, seed {}).", id, road.id(), ofParty(party), FloorMap.label(s.firstFloor()), FloorMap.label(s.lastFloor()),
                s.theme.id(), s.seed);
        return null;
    }

    /** {@code "of party 3 "} for a party's own chain, {@code ""} for the shared lane, so the messages of a world without parties read exactly as they always did. */
    static String ofParty(int party) { return party == 0 ? "" : "of party " + party + " "; }

    /** Queues the sector behind {@code from}'s Way Onward. Returns null on success, else the reason it was refused. */
    public String tryNext(ServerLevel level, Sector from) {
        if (level == null || from == null) return "no sector";
        if (from.isFloor()) return tryNextFloor(level, from);
        MinecraftServer server = level.getServer();
        MegaData data = MegaData.get(server);
        boolean slotTaken = data.chainSector(from.party, from.polarity, from.chainIndex + 1) != null;
        String why = ExpandRule.refusalForNext(rowOf(from), data.sectorCount(from.party), MegaConfig.get(MegaConfig.MAX_SECTORS), slotTaken);
        if (why != null) return why;
        SectorPlanner.Next n = planBehind(data, from);
        if (n == null) return "no room behind the seal of sector " + from.id + " (older sectors are in the way)";
        Sector s = draft(data, from, n);
        int id = s.id;
        from.nextId = id;
        data.add(s);
        queue.add(new Job(id, false, null));
        LOG.info("[nusmp] Mega sector {} queued behind sector {} on the {} road {}(floors {}..{}, theme {}, {} tunnel segments, seed {}).", id, from.id, s.polarity.id(), ofParty(s.party),
                FloorMap.label(s.firstFloor()), FloorMap.label(s.lastFloor()), s.theme.id(), n.linkModules(), s.seed);
        return null;
    }

    /**
     * 1.58.0: queues the floor above (or below) {@code from}. It needs no room planning: the next floor stands in the next slot of the road's lane. The theme is the
     * floor's own draft ({@link SectorDraft}: only the first floor of a decade draws one, or takes the Crucible's choice). Returns null on success, else the reason.
     */
    private String tryNextFloor(ServerLevel level, Sector from) {
        MinecraftServer server = level.getServer();
        MegaData data = MegaData.get(server);
        boolean slotTaken = data.chainSector(from.party, from.polarity, from.chainIndex + 1) != null;
        String why = ExpandRule.refusalForNext(rowOf(from), data.sectorCount(from.party), MegaConfig.get(MegaConfig.MAX_SECTORS), slotTaken);
        if (why != null) return why;
        Sector s = draftFloor(data, from);
        from.nextId = s.id;
        data.add(s);
        queue.add(new Job(s.id, false, null));
        LOG.info("[nusmp] Mega floor {} queued after sector {} on the {} road {}(floor {}{}, theme {}, seed {}).", s.id, from.id, s.polarity.id(), ofParty(s.party), FloorMap.label(s.firstFloor()),
                s.isBossFloor() ? ", a boss floor" : "", s.theme.id(), s.seed);
        return null;
    }

    /**
     * The floor after {@code from} as a new, unsaved sector: the next global id, the same road, the next place in it, its slot of the road's lane, its region (sized for
     * its room count), its theme (its decade's, or the Crucible's choice on the first floor of a decade), its seed (the seed of its place). Not added to the data; the
     * self-test {@code core.expand} plans exactly this as a dry run.
     */
    static Sector draftFloor(MegaData data, Sector from) {
        int idx = from.chainIndex + 1;
        Sector latest = data.latest();
        int id = latest == null ? 0 : latest.id + 1;
        RegionTheme forced = FloorMap.startsDecade(idx, from.perRegion) ? MegaDungeon.themeOverride(from) : null;
        SectorDraft d = SectorDraft.of(data.seed, id, from.polarity, idx, from.perRegion, from.theme, forced, from.party);
        Request.Floor spec = FloorPlan.spec(from.polarity, idx, MegaConfig.get(MegaConfig.FLOOR_ROOMS));
        SectorPlanner.Plan plan = SectorPlanner.floor(data.seed, id, from.polarity, idx, spec.rooms(), spec.boss(), from.party);
        Sector s = create(d);
        s.seed = d.seed();
        s.region = SectorGenerator.bb(plan.region());
        s.entry = SectorGenerator.pos(plan.entryFloor());
        s.forward = SectorGenerator.dir(plan.forward());
        return s;
    }

    /**
     * 1.58.0: a party entered the floor of {@code s} (by its arrival stairs, by the Hub's gate, by an admin): the floor after it is queued now, so the stairs are normally
     * ready by the time they are found. Silent when there is nothing to do (the next floor exists or is queued, the dungeon is full, the floor is of the old model) and on a
     * boss floor (its next floor waits for the Crucible, {@code mega.expandAfterVault}, which {@code MegaDungeon.unseal} handles). Returns true when a floor was queued.
     */
    public boolean onFloorReached(ServerLevel level, Sector s) {
        if (level == null || s == null || !s.isFloor() || s.isBossFloor()) return false;
        if (s.nextId >= 0 || working(s.id) || !SectorGenerator.playable(s)) return false;
        if (!MegaConfig.get(MegaConfig.EXPANSION)) return false;
        String why = tryNextFloor(level, s);
        if (why != null) LOG.info("[nusmp] Mega floor after {} not queued: {}", s.id, why);
        return why == null;
    }

    /** What {@link ExpandRule} needs to know about a sector: a floor of the floor model has a way on when it has forward stairs, any other sector when its Way Onward has a seal. */
    static ExpandRule.Row rowOf(Sector s) {
        boolean way = (s.expansionPos != null && s.expansionFacing != null) || (s.isFloor() && s.stairForward != null);
        return new ExpandRule.Row(s.id, way, s.nextId, s.state == Sector.State.GENERATING, s.state == Sector.State.FAILED);
    }

    /** Where the sector behind {@code from}'s Way Onward would go (region, entry door, tunnel length), or null when older sectors leave no room. Changes nothing. */
    static SectorPlanner.Next planBehind(MegaData data, Sector from) {
        return SectorPlanner.planNext(SectorGenerator.vec(from.expansionPos), SectorGenerator.facing(from.expansionFacing), SectorGenerator.box(from.region),
                SectorGenerator.obstacles(data, Integer.MAX_VALUE, from.id, from.polarity, from.party), MegaConfig.get(MegaConfig.MAX_LINK_MODULES));
    }

    /**
     * The sector that is created for the plan {@code n} behind {@code from}: the next global id, the same road, the next place in it, the same number of floors,
     * the threat of its floors, the seed of its place, and the theme the Crucible's chosen relic forces ({@code MegaDungeon.themeOverride}), else the seeded draw of
     * the road. Not added to the data.
     */
    static Sector draft(MegaData data, Sector from, SectorPlanner.Next n) {
        Sector latest = data.latest();
        int id = latest == null ? 0 : latest.id + 1;
        SectorDraft d = SectorDraft.of(data.seed, id, from.polarity, from.chainIndex + 1, from.perRegion, from.theme, MegaDungeon.themeOverride(from), from.party);
        Sector s = create(d);
        s.seed = d.seed();
        s.region = SectorGenerator.bb(n.region());
        s.entry = SectorGenerator.pos(n.entry().add(0, -1, 0));
        s.forward = SectorGenerator.dir(n.forward());
        return s;
    }

    /** A new sector with everything {@link SectorDraft} decides (the geometry is the caller's): GENERATING, its road, place, floors, threat, theme and relic count. */
    private static Sector create(SectorDraft d) {
        Sector s = new Sector(d.id());
        s.polarity = d.road();
        s.party = d.party();
        s.chainIndex = d.chainIndex();
        s.perRegion = d.perRegion();
        s.theme = d.theme();
        s.threat = d.threat();
        s.state = Sector.State.GENERATING;
        s.relicCount = d.relicCount();
        s.cyclic = CyclicSettings.enabled();                       // 1.83.0: decided ONCE, when the sector is created; saved with it ("cyc"), so a regeneration plans the same way
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
                if (ticks % 2 == 0) {
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
            if (current != null && ticks % ANNOUNCE_EVERY == 0) announce(level, s.party, current.progress());
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
        LOG.info("MEGA sector {} {} rooms={} edges={} pieces={} loops={} tries={} ms={} road={} chain={}", s.id, s.state.name(), s.rooms.size(), s.edges.size(), st.pieces, st.loops, st.tries, ms,
                s.polarity.id(), s.chainIndex);
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
        data.setDirty();                                       // the listeners band the floors and assign the factions into the saved sector: make sure what they wrote is saved
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

    /**
     * The "the Labyrinth shifts" line, to everybody who is not standing on the floors of ANOTHER party (a party that is not waiting for this floor has nothing to do with
     * it; with one party, which is how the dungeon worked before 1.58.0, that is everybody).
     */
    private static void announce(ServerLevel level, int party, float progress) {
        int pct = Math.round(progress * 100f);
        Component line = Component.translatable("message.nusmp.mega.core.shifting", pct);
        for (ServerPlayer p : level.players()) {
            Sector at = MegaDungeon.sectorAt(level, p.blockPosition());
            if (at != null && at.party != party) continue;
            p.displayClientMessage(line, true);
        }
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
