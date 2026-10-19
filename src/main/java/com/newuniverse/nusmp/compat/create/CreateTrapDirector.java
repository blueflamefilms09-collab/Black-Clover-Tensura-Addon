package com.newuniverse.nusmp.compat.create;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.SectorListener;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Why: the one conductor of the Create traps. It is a {@link SectorListener}, so when a sector of the Mega-Dungeon has been generated it plans that sector's traps
 * ({@link TrapPlanner}, pure and seeded) and writes the plans to {@link CreateTrapData}; nothing is placed yet. A slow tick then ARMS the plans: a plan whose room is in loaded
 * chunks looks for a choke ({@link LaneFinder} over a {@link WorldGrid}), proves the drawing fits ({@link TrapBuilder#check}) and builds it, so a trap is never built in a place
 * nobody has come near and never forces a chunk to load; a room with no choke drops its plan. A fast tick runs the traps that have a player within 40 blocks
 * ({@link TrapRuntime}) and puts the others to rest. It is registered only when Create is installed; with Create absent the dungeon is not touched at all.
 */
public final class CreateTrapDirector implements SectorListener {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<String, TrapRuntime.Live> LIVE = new HashMap<>();
    private static final double NEAR = 40.0;

    /** Is the compatibility working: Create is installed and the master switch is on? */
    public static boolean active() { return PartResolver.createLoaded() && CreateConfig.get(CreateConfig.ENABLED); }

    // ---------------------------------------------------------------- planning
    @Override
    public void onSectorGenerated(ServerLevel level, Sector sector) {
        if (!active()) return;
        try {
            int n = planSector(level.getServer(), sector);
            if (n > 0) LOG.info("[nusmp] Create compat: sector {} planned {} trap(s)", sector.id, n);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] Create compat: planning sector {} failed: {}", sector.id, e.toString());
        }
    }

    /** Plans the traps of one sector (once per sector) and records them as PLANNED. Returns how many were planned. */
    public static int planSector(MinecraftServer server, Sector s) {
        CreateTrapData data = CreateTrapData.get(server);
        if (data.planned(s.id)) return 0;
        List<TrapPlanner.RoomInfo> infos = new ArrayList<>();
        Map<Integer, Integer> depths = new HashMap<>();
        for (Room r : s.rooms) {
            int depth = Math.max(1, Math.abs(r.floor != 0 ? r.floor : s.firstFloor()));
            depths.put(r.id, depth);
            infos.add(new TrapPlanner.RoomInfo(r.id, r.kind.folder(), r.kind.isSafe(), depth));
        }
        List<TrapPlanner.Plan> plans = TrapPlanner.plan(infos, s.edges, s.theme.id(), s.threat, s.seed, CreateConfig.get(CreateConfig.DENSITY), CreateConfig.families());
        int n = 0;
        for (TrapPlanner.Plan p : plans) {
            TrapSite site = new TrapSite(TrapSite.idOf(s.id, p.roomId()), s.id, p.roomId(), p.family(), p.trigger(), p.core(), p.seed());
            site.depth = depths.getOrDefault(p.roomId(), 1);
            site.threat = Math.max(1, s.threat);
            if (data.add(site)) n++;
        }
        data.markPlanned(s.id);
        return n;
    }

    /** Plans every sector of the dungeon that was not planned yet (for sectors that existed before Create was installed). Returns the number of traps planned. */
    public static int planAll(MinecraftServer server) {
        int n = 0;
        for (Sector s : new ArrayList<>(MegaData.get(server).sectors())) n += planSector(server, s);
        return n;
    }

    // ---------------------------------------------------------------- arming
    /**
     * Finds a choke in the box of a room and builds the site's trap there. Returns true and fills the site (origin, direction, headroom, state BUILT) when it worked; false
     * and a note in the site when not (the site is then DROPPED by the caller, or kept PLANNED when the reason is only that chunks are not loaded: see {@link #LOADING}).
     */
    public static boolean tryArm(ServerLevel level, TrapSite site, int minX, int maxX, int minZ, int maxZ, int floorY) {
        if (!level.hasChunksAt(new BlockPos(minX, floorY, minZ), new BlockPos(maxX, floorY, maxZ))) {
            site.note = LOADING;
            return false;
        }
        WorldGrid grid = new WorldGrid(level, floorY);
        int n = TrapRules.stripLength(site.core);
        boolean flip = (site.seed & 1L) != 0L;
        List<LaneFinder.Site> found = LaneFinder.find(grid, minX, maxX, minZ, maxZ, n, flip);
        if (found.isEmpty()) {
            site.note = "no choke";
            return false;
        }
        String why = "no candidate fitted";
        for (int i = 0; i < Math.min(6, found.size()); i++) {
            LaneFinder.Site c = found.get(i);
            int headroom = Math.max(TrapRules.MIN_HEADROOM, Math.min(TrapRules.MAX_HEADROOM, grid.stripHeadroom(c)));
            Frame frame = new Frame(new BlockPos(c.originX(), floorY, c.originZ()), Frame.direction(c.forward()));
            Blueprint bp = TrapBlueprints.build(site.family, site.core, headroom, site.trigger, site.seed);
            TrapBuilder.Result r = TrapBuilder.build(level, frame, bp);
            if (r.ok()) {
                site.origin = frame.origin();
                site.forward = frame.forward();
                site.headroom = headroom;
                site.state = TrapSite.State.BUILT;
                site.note = "";
                return true;
            }
            why = r.reason();
        }
        site.note = why;
        return false;
    }

    /** The note of a site that is waiting for its chunks. */
    public static final String LOADING = "waiting for chunks";

    private static void armOne(MinecraftServer server, ServerLevel level, CreateTrapData data) {
        MegaData mega = MegaData.get(server);
        for (TrapSite site : data.in(TrapSite.State.PLANNED)) {
            Sector s = mega.sector(site.sector);
            Room room = null;
            if (s != null) for (Room r : s.rooms) if (r.id == site.room) { room = r; break; }
            if (room == null) {
                site.state = TrapSite.State.DROPPED;
                site.note = "no such room";
                data.setDirty();
                continue;
            }
            boolean ok = tryArm(level, site, room.box.minX(), room.box.maxX(), room.box.minZ(), room.box.maxZ(), MegaGrid.FLOOR_Y);
            if (ok) {
                data.setDirty();
                LOG.info("[nusmp] Create compat: armed {} {} in sector {} room {} at {} facing {}", site.family.id(), site.trigger.id(), site.sector, site.room, site.origin.toShortString(), site.forward);
                return;                                                                  // one build per look
            }
            if (!LOADING.equals(site.note)) {
                site.state = TrapSite.State.DROPPED;
                data.setDirty();
                LOG.debug("[nusmp] Create compat: dropped {}: {}", site.id, site.note);
            }
        }
    }

    // ---------------------------------------------------------------- the tick
    public static void onServerTick(ServerTickEvent.Post e) {
        if (!PartResolver.createLoaded()) return;
        MinecraftServer server = e.getServer();
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return;
        long now = level.getGameTime();
        if (now % 2 != 0) return;
        try {
            CreateTrapData data = CreateTrapData.get(server);
            if (data.sites().isEmpty()) return;
            if (active() && now % 40 == 0 && !level.players().isEmpty()) armOne(server, level, data);
            if (now % 10 == 0) refreshActive(level, data, now);
            for (TrapRuntime.Live lv : new ArrayList<>(LIVE.values())) TrapRuntime.step(level, lv, now, false, null);
        } catch (RuntimeException ex) {
            PartResolver.logOnce("tick-err", "the trap tick failed once: " + ex);
        }
    }

    /** Wakes the traps with a player near and puts the ones nobody is near to rest. */
    private static void refreshActive(ServerLevel level, CreateTrapData data, long now) {
        List<ServerPlayer> ps = level.players();
        Map<String, Boolean> near = new HashMap<>();
        if (!ps.isEmpty()) {
            for (TrapSite s : data.sites().values()) {
                if (s.state != TrapSite.State.BUILT || s.origin == null) continue;
                for (ServerPlayer p : ps) {
                    double dx = p.getX() - s.origin.getX(), dz = p.getZ() - s.origin.getZ();
                    if (dx * dx + dz * dz <= NEAR * NEAR && Math.abs(p.getY() - s.origin.getY()) <= 24) {
                        near.put(s.id, true);
                        break;
                    }
                }
            }
        }
        for (String id : near.keySet()) {
            if (!LIVE.containsKey(id)) LIVE.put(id, new TrapRuntime.Live(data.site(id)));
            LIVE.get(id).lastNear = now;
        }
        for (Iterator<Map.Entry<String, TrapRuntime.Live>> it = LIVE.entrySet().iterator(); it.hasNext(); ) {
            TrapRuntime.Live lv = it.next().getValue();
            if (near.containsKey(lv.site.id) || now - lv.lastNear < 100) continue;
            if (TrapBuilder.loaded(level, lv.frame, lv.bp)) TrapRuntime.idle(level, lv);
            it.remove();
        }
    }

    /** The trap nearest to a position within a radius (a built, live-or-not site), for the admin command; null when there is none. */
    public static TrapSite nearest(MinecraftServer server, BlockPos pos, double radius) {
        TrapSite best = null;
        double bd = radius * radius;
        for (TrapSite s : CreateTrapData.get(server).sites().values()) {
            if (s.state != TrapSite.State.BUILT || s.origin == null) continue;
            double d = s.origin.distSqr(pos);
            if (d <= bd) {
                bd = d;
                best = s;
            }
        }
        return best;
    }

    /** The live state of a site, waking it when needed (the admin command fires through it). */
    public static TrapRuntime.Live live(TrapSite s) { return LIVE.computeIfAbsent(s.id, k -> new TrapRuntime.Live(s)); }

    public static int liveCount() { return LIVE.size(); }

    /** Forgets every live state (a server stopped). */
    public static void reset() { LIVE.clear(); }
}
