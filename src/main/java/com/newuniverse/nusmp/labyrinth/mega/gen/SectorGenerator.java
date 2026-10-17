package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.Marker;
import com.newuniverse.nusmp.labyrinth.mega.MegaConfig;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Why: this is where the pure plan meets the saved sector and the game's piece classes. It does four jobs and nothing else. (1) It turns a saved
 * {@link Sector} (seed, entry door, growth direction, relic count) plus the other sectors (their regions and tunnels, read from {@link MegaData}) into the
 * plain-value inputs of {@link SectorRequests} and runs the {@link SectorSolver}: so the plan of a sector is a function of data that is SAVED, which is what
 * makes the restart repair and {@code /multiverse mega regen} reproduce exactly the sector that was placed. (2) It turns the winning {@link Result} into
 * {@code PoolElementStructurePiece}s (the same class vanilla Jigsaw places) with the processor list named by each pool element, and into world {@link Marker}s,
 * computed with this package's own rotation maths from the template-local marker cells (never with the game's marker reader, so a marker lands where the plan
 * says whatever vanilla does with it). (3) It fills the {@link Sector} with rooms, doorways, markers and the landmarks of the boss chain (the arrival cell,
 * the Guardian's spawn, the Gatekeeper, the vault's centre, the pedestals, the rubble seal and the way it points). (4) It reads the OTHER direction too:
 * from a saved sector back to the obstacles it makes for its neighbours. Server thread only.
 */
public final class SectorGenerator {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Rotation[] ROTATIONS = {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90};
    private static final Set<String> WARNED_PROCESSORS = new HashSet<>();

    private SectorGenerator() {}

    /** What the planning step produced: the request, the solver's outcome and how long it took. */
    public record Planned(SectorRequests.Prepared prepared, SectorSolver.Solved solved, long nanos, String failure) {
        public boolean ok() { return failure == null && solved != null && solved.ok(); }

        public Result result() { return solved == null ? null : solved.result(); }
    }

    /** The pieces and markers of a plan, ready to place. */
    public record Built(Planned planned, List<PoolElementStructurePiece> pieces, List<Marker> markers, Map<BlockPos, Marker> markersByCell) {}

    // ---------------------------------------------------------------- conversions

    static Vec vec(BlockPos p) { return new Vec(p.getX(), p.getY(), p.getZ()); }

    static BlockPos pos(Vec v) { return new BlockPos(v.x(), v.y(), v.z()); }

    static Box box(BoundingBox b) { return new Box(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()); }

    static BoundingBox bb(Box b) { return new BoundingBox(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()); }

    static Facing facing(Direction d) { return Facing.byName(d.getName()); }

    static Direction dir(Facing f) { return Direction.byName(f.lower()); }

    static Rotation rotation(int quarterTurns) { return ROTATIONS[quarterTurns & 3]; }

    // ---------------------------------------------------------------- neighbours

    /** The sector whose Way Onward leads to {@code s}, or null (sector 0, or a sector whose predecessor was deleted by hand). */
    public static Sector prevOf(MegaData d, Sector s) {
        for (Sector p : d.sectors()) if (p.nextId == s.id && p.id != s.id) return p;
        return null;
    }

    /**
     * The regions and tunnels a sector must keep clear of when it is planned: those of the sectors with a LOWER id than {@code before}, which are exactly the
     * ones that existed when it was first planned (so a regeneration sees what the original saw, see {@link WorldObstacles}). {@code skipRegion} leaves one
     * region out: the planner of the next sector adds the region of the sector it hangs from itself. Pass {@code Integer.MAX_VALUE} to see every sector.
     */
    public static List<SectorPlanner.Obstacle> obstacles(MegaData d, int before, int skipRegion) { return obstacles(d, before, skipRegion, null); }

    /**
     * {@link #obstacles(MegaData, int, int)} for the sectors of one road only (null: every road). A sector keeps out of the sectors of ITS OWN road: the two roads
     * start {@code MegaGrid.LANE_Z} blocks apart and cannot meet, and a plan that looked at the other road too would depend on how far that road had grown.
     */
    public static List<SectorPlanner.Obstacle> obstacles(MegaData d, int before, int skipRegion, Polarity road) {
        List<WorldObstacles.Known> known = new ArrayList<>();
        for (Sector o : d.sectors()) {
            if (road != null && o.polarity != road) continue;
            Sector prev = prevOf(d, o);
            Vec seal = null;
            Facing seals = null;
            if (prev != null && prev.expansionPos != null && prev.expansionFacing != null) {
                seal = vec(prev.expansionPos);
                seals = facing(prev.expansionFacing);
            }
            known.add(new WorldObstacles.Known(o.id, box(o.region), seal, seals, vec(o.entry.above()), o.polarity));
        }
        return WorldObstacles.of(known, before, skipRegion, road);
    }

    // ---------------------------------------------------------------- planning

    /**
     * Plans a saved sector: builds the request from the saved fields and the neighbours, runs the solver and reports. Nothing is placed and the sector is not
     * changed. Safe to call again for the same sector (the plan is a function of saved data).
     */
    public static Planned plan(ServerLevel level, MegaData data, Sector s) { return plan(level, data, s, prevOf(data, s)); }

    /**
     * {@link #plan(ServerLevel, MegaData, Sector)} with the predecessor given by the caller: the dry run of an expansion plans a sector that has not been
     * saved, so no saved sector points at it yet, and passing the sector it WOULD hang from gives the plan the real request would get.
     */
    public static Planned plan(ServerLevel level, MegaData data, Sector s, Sector prev) {
        long t0 = System.nanoTime();
        VanillaPools pools = VanillaPools.get();
        pools.bind(level.getStructureManager());
        Recipe recipe = pools.recipe();
        Vec entryJigsaw = new Vec(s.entry.getX(), s.entry.getY() + 1, s.entry.getZ());
        Facing forward = facing(s.forward);
        Box region = SectorPlanner.regionFor(entryJigsaw, forward);
        Vec prevSeal = null;
        Facing prevFacing = null;
        Box prevRegion = null;
        if (prev != null) {
            if (prev.expansionPos == null || prev.expansionFacing == null) {
                return new Planned(null, null, System.nanoTime() - t0, "sector " + prev.id + " has no Way Onward to hang sector " + s.id + " from");
            }
            prevSeal = vec(prev.expansionPos);
            prevFacing = facing(prev.expansionFacing);
            prevRegion = box(prev.region);
        } else if (s.chainIndex != 0) {
            LOG.warn("[nusmp] Mega sector {} ({} road, chain index {}) has no predecessor that points at it; it is planned as a free-standing sector.", s.id, s.polarity.id(), s.chainIndex);
        }
        SectorRequests.Inputs in = new SectorRequests.Inputs(pools, recipe, MegaConfig.get(MegaConfig.MAX_PIECES), MegaConfig.get(MegaConfig.MAX_LINK_MODULES), s.seed,
                entryJigsaw, forward, region, prevSeal, prevFacing, prevRegion, obstacles(data, s.id, -1, s.polarity), s.relicCount);
        SectorRequests.Prepared prep = SectorRequests.prepare(in);
        for (String n : prep.notes()) LOG.warn("[nusmp] Mega sector {} plan: {}", s.id, n);
        if (!prep.ok()) return new Planned(prep, null, System.nanoTime() - t0, prep.failure());
        SectorSolver.Solved solved = SectorSolver.solve(prep.request(), MegaConfig.get(MegaConfig.ASSEMBLE_TRIES));
        long dt = System.nanoTime() - t0;
        if (!solved.ok()) return new Planned(prep, solved, dt, "no sector could be assembled: " + SectorSolver.summary(solved.failures(), 4));
        for (String w : solved.result().warnings) LOG.warn("[nusmp] Mega sector {} plan: {}", s.id, w);
        return new Planned(prep, solved, dt, null);
    }

    /**
     * Plans a free-standing "probe" sector at the position of sector 0 with no neighbours, for the self-tests and the simulator-in-game: it exercises the real
     * pools and the real templates through the real request and solver, and places nothing and saves nothing. The relic count is the one the road's first sector
     * would have (three since 1.55.0), so the vault pool a real sector draws from is the one the probe exercises.
     */
    public static Planned probe(ServerLevel level, long dungeonSeed) { return probe(level, dungeonSeed, Polarity.HOLY); }

    /** {@link #probe(ServerLevel, long)} at the lane origin of a road: the Abyssal probe stands {@code MegaGrid.LANE_Z} blocks from the Holy one, exactly where that road's first sector would. */
    public static Planned probe(ServerLevel level, long dungeonSeed, Polarity road) {
        long t0 = System.nanoTime();
        VanillaPools pools = VanillaPools.get();
        pools.bind(level.getStructureManager());
        SectorPlanner.Plan plan = SectorPlanner.first(dungeonSeed, 0, road);
        SectorRequests.Inputs in = new SectorRequests.Inputs(pools, pools.recipe(), MegaConfig.get(MegaConfig.MAX_PIECES), MegaConfig.get(MegaConfig.MAX_LINK_MODULES), plan.seed(),
                plan.entry(), plan.forward(), plan.region(), null, null, null, List.of(), SectorDraft.first(dungeonSeed, 0, road).relicCount());
        SectorRequests.Prepared prep = SectorRequests.prepare(in);
        if (!prep.ok()) return new Planned(prep, null, System.nanoTime() - t0, prep.failure());
        SectorSolver.Solved solved = SectorSolver.solve(prep.request(), MegaConfig.get(MegaConfig.ASSEMBLE_TRIES));
        long dt = System.nanoTime() - t0;
        if (!solved.ok()) return new Planned(prep, solved, dt, "no sector could be assembled: " + SectorSolver.summary(solved.failures(), 4));
        return new Planned(prep, solved, dt, null);
    }

    // ---------------------------------------------------------------- pieces and markers

    /** Turns a successful plan into game pieces and world markers. */
    public static Built build(ServerLevel level, Planned planned) {
        Result r = planned.result();
        StructureTemplateManager mgr = level.getStructureManager();
        Registry<StructureProcessorList> registry = level.registryAccess().registryOrThrow(Registries.PROCESSOR_LIST);
        Map<String, Holder<StructureProcessorList>> holders = new HashMap<>();
        List<PoolElementStructurePiece> pieces = new ArrayList<>(r.pieces.size());
        List<Marker> markers = new ArrayList<>();
        Map<BlockPos, Marker> byCell = new LinkedHashMap<>();
        for (Placed p : r.pieces) {
            Holder<StructureProcessorList> h = holders.computeIfAbsent(p.processors, id -> processors(registry, id));
            StructurePoolElement element = StructurePoolElement.single(p.shape.id, h).apply(StructureTemplatePool.Projection.RIGID);
            pieces.add(new PoolElementStructurePiece(mgr, element, pos(p.origin), 0, rotation(p.rot), bb(p.box), LiquidSettings.APPLY_WATERLOGGING));
            for (ShapeMarker m : p.shape.markers) {
                Vec w = p.world(m.cell());
                BlockPos mp = pos(w);
                Marker marker = Marker.of(m.spec(), mp, rotation(p.rot));
                markers.add(marker);
                byCell.put(mp, marker);
            }
        }
        return new Built(planned, pieces, markers, byCell);
    }

    /** The processor list a pool element names; an unknown or absent list becomes the empty list (once logged), so the sector is still placed, only unstyled. */
    private static Holder<StructureProcessorList> processors(Registry<StructureProcessorList> registry, String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl != null) {
            Optional<Holder.Reference<StructureProcessorList>> h = registry.getHolder(ResourceKey.create(Registries.PROCESSOR_LIST, rl));
            if (h.isPresent()) return h.get();
        }
        if (WARNED_PROCESSORS.add(id)) LOG.warn("[nusmp] Processor list {} does not exist; Mega pieces that name it are placed without processors.", id);
        return Holder.direct(new StructureProcessorList(List.of()));
    }

    // ---------------------------------------------------------------- the saved sector

    /**
     * Copies the plan into the sector: rooms, doorways, markers and the landmarks of the boss chain. Does not change the state. Which marker is "the" guardian
     * spawn and what the arrival cell is are decided by the pure {@link SectorFill}; this only moves its answer into the game's classes.
     */
    public static void fill(Sector s, Built b) {
        SectorFill f = SectorFill.of(b.planned().result(), box(s.region));
        s.rooms.clear();
        s.edges.clear();
        s.markers.clear();
        s.pedestals.clear();
        for (SectorFill.RoomOut r : f.rooms) s.rooms.add(new Room(r.id(), r.template(), r.kind(), bb(r.box())));
        s.edges.addAll(f.edges);
        s.markers.addAll(b.markers());
        s.arrival = f.arrival == null ? null : pos(f.arrival);
        s.guardianSpawn = f.guardianSpawn == null ? null : pos(f.guardianSpawn);
        s.gatekeeper = f.gatekeeper == null ? null : pos(f.gatekeeper);
        s.vaultCenter = f.vaultCenter == null ? null : pos(f.vaultCenter);
        for (Vec p : f.pedestals) s.pedestals.add(pos(p));
        Result.Expansion e = f.expansion;
        if (e != null) {
            s.expansionPos = pos(e.seal());
            s.expansionFacing = dir(e.facing());
            s.sealW = e.w();
            s.sealH = e.h();
            s.sealD = e.d();
        } else {
            s.expansionPos = null;
            s.expansionFacing = null;
        }
        s.bounds = bb(f.bounds);
    }

    /** The plain-value copy of a saved sector that {@link SavedAudit} checks. */
    public static SectorView view(Sector s) {
        List<SectorFill.RoomOut> rooms = new ArrayList<>();
        for (Room r : s.rooms) rooms.add(new SectorFill.RoomOut(r.id, r.template, r.kind, box(r.box)));
        List<SectorView.MarkerView> markers = new ArrayList<>();
        for (Marker m : s.markers) markers.add(new SectorView.MarkerView(m.kind(), vec(m.pos())));
        List<Vec> pedestals = new ArrayList<>();
        for (BlockPos p : s.pedestals) pedestals.add(vec(p));
        return new SectorView(s.id, s.state.name(), box(s.region), rooms, new ArrayList<>(s.edges), markers, s.relicCount, pedestals,
                s.arrival == null ? null : vec(s.arrival), s.guardianSpawn == null ? null : vec(s.guardianSpawn), s.gatekeeper == null ? null : vec(s.gatekeeper),
                s.vaultCenter == null ? null : vec(s.vaultCenter), s.expansionPos == null ? null : vec(s.expansionPos),
                s.expansionFacing == null ? null : facing(s.expansionFacing), s.nextId);
    }

    /** The statistics the status line and the log line report. */
    public static GenData.Stat stat(Planned p, long totalMillis) {
        Result r = p.result();
        GenData.Stat st = new GenData.Stat();
        st.pieces = r.pieces.size();
        st.caps = r.count(RoomKind.CAP);
        st.loops = r.loops.size();
        st.tries = p.solved().tries();
        st.fallback = p.solved().fallback();
        st.deadEnd = p.solved().deadEnd();
        st.millis = totalMillis;
        int link = 0;
        for (Placed x : r.pieces) if (x.link) link++;
        st.link = link;
        return st;
    }

    /** Can a player stand in this sector (it is finished and not failed)? */
    public static boolean playable(Sector s) { return s.state != Sector.State.GENERATING && s.state != Sector.State.FAILED; }

    /** The sector's standing cell for travel and the rest of the mod: the ARRIVAL marker's cell, else the middle of the first Landing room. */
    public static BlockPos padOf(Sector s) {
        if (s.arrival != null) return s.arrival;
        for (Room r : s.rooms) if (r.kind == RoomKind.LANDING) return r.center();
        return null;
    }
}
