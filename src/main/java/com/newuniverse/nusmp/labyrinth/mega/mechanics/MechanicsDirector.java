package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.enc.Sanctuary;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Why: the server-side conductor of the mechanics library. When a delver steps onto a floor (or into a room, or an arena) it makes sure the floor has a PLAN (a pure function of the
 * sector's seed, {@link Planner}: reproducible, cached in memory, rebuilt when the config or the library changes), turns the planned mechanic of that place into a live
 * {@link Instance} (an anchor in a free cell, an area, the props its counter needs) and ticks every live instance each tick with the players inside it. It ends an instance when
 * its area has been empty for five seconds, when the guardian of a boss floor is dead, when an admin clears it, when the switch is turned off, or when the server stops; ending
 * restores every block (see {@link Instance#end}). It never fires inside safe rooms, the Hub (another dimension) or rift pockets ({@link Safety}), never re-arms a mechanic that was
 * beaten ({@link MechanicsData}), and does all its work on the server thread with bounded cost (a scan of the players every ten ticks, a cached plan per sector).
 */
public final class MechanicsDirector {
    private MechanicsDirector() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final int SCAN_EVERY = 10, GRACE_TICKS = 100;

    static final Map<String, Instance> ACTIVE = new LinkedHashMap<>();
    private static final Map<Integer, Plan> PLANS = new java.util.HashMap<>();
    private static final Set<Integer> SUPPRESSED = new HashSet<>();
    private static boolean recovered;
    private static int adminSeq;

    /** The plan of one sector and what it was built from. */
    record Plan(int sectorId, int cfgHash, int libVersion, int rooms, List<Planner.Slot> slots) {}

    public static List<Instance> active() { return new ArrayList<>(ACTIVE.values()); }

    // ---------------------------------------------------------------- the plan
    private static boolean eligibleKind(Room r) {
        RoomKind k = r.kind;
        if (k.isSafe() || Sanctuary.is(k)) return false;
        return k == RoomKind.HALL || k == RoomKind.SET_PIECE || k == RoomKind.CAVERN || k == RoomKind.TRIAL || k == RoomKind.TRAP || k == RoomKind.PUZZLE || k == RoomKind.TREASURE
                || k == RoomKind.CORRIDOR || k == RoomKind.JUNCTION;
    }

    /** The floor description the planner works from. */
    public static Planner.FloorInfo floorInfo(Sector s) {
        List<Planner.RoomInfo> rooms = new ArrayList<>();
        int arena = -1;
        for (Room r : s.rooms) {
            if (r.kind == RoomKind.ARENA && arena < 0) arena = r.id;
            boolean big = Math.min(r.box.getXSpan(), r.box.getZSpan()) >= 9;
            rooms.add(new Planner.RoomInfo(r.id, r.kind.folder(), eligibleKind(r) && big));
        }
        return new Planner.FloorInfo(s.firstFloor(), s.polarity, s.theme.id(), s.seed, s.isBossFloor(), arena, rooms);
    }

    /** The (cached) plan of a sector; empty when the library is not loaded. */
    public static List<Planner.Slot> planFor(Sector s) {
        OpCatalog cat = MechanicLibrary.catalog();
        if (cat == null || MechanicLibrary.size() == 0) return List.of();
        Planner.Config cfg = MechanicsConfig.planner();
        int hash = Objects.hash(cfg.perFloor(), cfg.perRoom(), cfg.budgetMult(), cfg.categories(), cfg.blacklist());
        Plan p = PLANS.get(s.id);
        if (p != null && p.cfgHash() == hash && p.libVersion() == MechanicLibrary.version() && p.rooms() == s.rooms.size()) return p.slots();
        List<Planner.Slot> slots = Planner.plan(MechanicLibrary.all(), cat, floorInfo(s), cfg);
        PLANS.put(s.id, new Plan(s.id, hash, MechanicLibrary.version(), s.rooms.size(), slots));
        return slots;
    }

    static String key(Sector s, Planner.Slot slot) { return s.id + ":" + slot.roomId() + ":" + slot.def().id(); }

    // ---------------------------------------------------------------- the tick
    public static void onServerTick(ServerTickEvent.Post e) {
        try {
            tick(e.getServer());
        } catch (Throwable t) {
            LOG.warn("[nusmp] mechanics tick failed: {}", t.toString());
        }
    }

    private static void tick(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return;
        if (!recovered) {
            recovered = true;
            int n = MechanicsData.get(server).recover(level);
            if (n > 0) LOG.info("[nusmp] mechanics: restored {} journals left over from the last run", n);
        }
        long now = level.getGameTime();
        if (!MechanicsConfig.enabled()) {
            if (!ACTIVE.isEmpty()) clearAll();
            return;
        }
        List<ServerPlayer> players = level.players();
        if (now % SCAN_EVERY == 0 && !players.isEmpty()) scan(level, players);
        if (ACTIVE.isEmpty()) return;
        for (Instance in : new ArrayList<>(ACTIVE.values())) {
            try {
                List<ServerPlayer> inside = playersIn(in, players);
                in.update(inside, now);
                boolean over = in.state == Instance.State.ENDED || (inside.isEmpty() && now - in.lastEmpty > GRACE_TICKS && !in.admin)
                        || (in.def.scope() == Scope.BOSS && in.sector != null && in.sector.state != Sector.State.ACTIVE) || in.errors > 40;
                if (over) end(in);
            } catch (RuntimeException ex) {                                      // one broken mechanic must not stop the others, nor flood the log every tick
                LOG.warn("[nusmp] mechanic {} failed and was ended: {}", in.def.id(), ex.toString());
                end(in);
            }
        }
        if (now % 40 == 0) {
            MechanicsData d = MechanicsData.get(server);
            if (d.anyJournal()) d.setDirty();
        }
    }

    private static boolean counts(ServerPlayer p) {
        if (!p.isAlive() || p.isSpectator()) return false;
        return !p.isCreative() || MechanicsConfig.get(MechanicsConfig.CREATIVE_ACTIVATES);
    }

    static List<ServerPlayer> playersIn(Instance in, List<ServerPlayer> all) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : all) {
            if (!counts(p)) continue;
            boolean inArea = in.area.kind == Area.Kind.ROOM ? in.area.containsWithMargin(p.getX(), p.getY(), p.getZ(), 1) : in.area.contains(p);
            if (inArea && Safety.allowedAt(in.level, p.blockPosition())) out.add(p);
        }
        return out;
    }

    private static void scan(ServerLevel level, List<ServerPlayer> players) {
        MechanicsData data = MechanicsData.get(level.getServer());
        for (ServerPlayer p : players) {
            if (!counts(p)) continue;
            BlockPos pos = p.blockPosition();
            if (!Safety.allowedAt(level, pos)) continue;
            Sector s = MegaDungeon.sectorAt(level, pos);
            if (s == null || s.rooms.isEmpty() || s.state == Sector.State.GENERATING || s.state == Sector.State.FAILED || SUPPRESSED.contains(s.id)) continue;
            Room here = s.roomAt(pos);
            for (Planner.Slot slot : planFor(s)) {
                String k = key(s, slot);
                if (ACTIVE.containsKey(k) || data.isSolved(k)) continue;
                if (slot.roomId() >= 0 && (here == null || here.id != slot.roomId())) continue;
                if (slot.def().scope() == Scope.BOSS && s.state != Sector.State.ACTIVE) continue;
                if (slot.roomId() >= 0 && slot.def().category() == MechCategory.PUZZLE && com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleDirector.claims(s, slot.roomId())) continue;   // 1.94.0: the room holds one of the new puzzles instead
                try {
                    Instance in = build(level, s, slot, here, p, k);
                    if (in != null) start(in);
                } catch (RuntimeException ex) {
                    LOG.warn("[nusmp] mechanic {} could not start: {}", slot.def().id(), ex.toString());
                }
            }
        }
    }

    private static Instance build(ServerLevel level, Sector s, Planner.Slot slot, Room here, ServerPlayer who, String key) {
        MechanicDef d = slot.def();
        SplittableRandom rng = Seeds.rng(slot.seed());
        Room target = slot.roomId() >= 0 ? s.room(slot.roomId()) : null;
        Area area;
        BlockPos anchor;
        switch (d.scope()) {
            case FLOOR -> {
                Room base = pickNear(s, who, rng, here);
                anchor = base == null ? who.blockPosition() : findAnchor(level, base, rng);
                area = Area.floor(s.region);
            }
            case ZONE -> {
                if (target == null) return null;
                anchor = findAnchor(level, target, rng);
                area = Area.zone(anchor, 4 + rng.nextInt(3) + Math.min(2, Scaling.tierOf(s.firstFloor()) / 2), target.box);
            }
            default -> {                                                                    // ROOM, BOSS
                if (target == null) return null;
                anchor = findAnchor(level, target, rng);
                area = Area.room(target.box);
            }
        }
        return new Instance(key, d, level, s, target, area, anchor, MegaDungeon.floorAt(level, anchor) != 0 ? MegaDungeon.floorAt(level, anchor) : s.firstFloor(), slot.seed(), MechanicsConfig.difficulty());
    }

    private static void start(Instance in) {
        ACTIVE.put(in.key, in);
        MechanicsData.get(in.level.getServer()).track(in);
        in.activate();
    }

    /** A seeded eligible room within reach of the player (so the anchor's chunks are loaded), else the room the player is in. */
    private static Room pickNear(Sector s, ServerPlayer who, SplittableRandom rng, Room here) {
        List<Room> near = new ArrayList<>();
        for (Room r : s.rooms) {
            if (!eligibleKind(r) || Math.min(r.box.getXSpan(), r.box.getZSpan()) < 9) continue;
            BlockPos c = r.center();
            if (c.distSqr(who.blockPosition()) < 56.0 * 56.0) near.add(r);
        }
        near.sort(java.util.Comparator.comparingInt(r -> r.id));
        if (!near.isEmpty()) return near.get(rng.nextInt(near.size()));
        return here;
    }

    /** A free standing cell in the room, the middle first, else a random one a few blocks clear of the walls; the room's middle when nothing is free. */
    static BlockPos findAnchor(ServerLevel level, Room room, SplittableRandom rng) {
        BlockPos c = room.center();
        for (int i = 0; i < 24; i++) {
            BlockPos p = i == 0 ? c : new BlockPos(room.box.minX() + 4 + rng.nextInt(Math.max(1, room.box.getXSpan() - 8)), MegaGrid.FLOOR_Y + 1, room.box.minZ() + 4 + rng.nextInt(Math.max(1, room.box.getZSpan() - 8)));
            if (!level.hasChunkAt(p)) continue;
            if (level.getBlockState(p.below()).getCollisionShape(level, p.below()).isEmpty()) continue;
            if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty() || !level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()) continue;
            if (!level.getFluidState(p).isEmpty()) continue;
            return p;
        }
        return c;
    }

    // ---------------------------------------------------------------- ending
    static void end(Instance in) {
        try {
            in.end();
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] mechanic {} did not end cleanly: {}", in.def.id(), e.toString());
        }
        ACTIVE.remove(in.key);
    }

    /** Ends every live instance (switch off, admin clear, server stop). */
    public static int clearAll() {
        int n = ACTIVE.size();
        for (Instance in : new ArrayList<>(ACTIVE.values())) end(in);
        ACTIVE.clear();
        return n;
    }

    /** Ends the live instances of one sector and keeps it quiet until reset (the admin command). */
    public static int clearSector(int sectorId) {
        int n = 0;
        for (Instance in : new ArrayList<>(ACTIVE.values())) {
            if (in.sector != null && in.sector.id == sectorId) { end(in); n++; }
        }
        SUPPRESSED.add(sectorId);
        return n;
    }

    public static void resetSuppression() { SUPPRESSED.clear(); }

    public static boolean suppressed(int sectorId) { return SUPPRESSED.contains(sectorId); }

    /** Ends the boss-scope instances of a sector (its Guardian has died). */
    public static void endBoss(int sectorId) {
        for (Instance in : new ArrayList<>(ACTIVE.values())) if (in.def.scope() == Scope.BOSS && in.sector != null && in.sector.id == sectorId) end(in);
    }

    public static void onServerStopping() {
        clearAll();
        PLANS.clear();
        SUPPRESSED.clear();
        recovered = false;
    }

    // ---------------------------------------------------------------- for the admin command and the self-tests
    /**
     * Starts a mechanic at a position on purpose. The area is a disc of {@code radius} round it (or the room / the floor for a room / floor scope when the position is inside a
     * sector). Returns the instance, or null with {@code why[0]} set when the place is not allowed (a safe room, the Hub, a rift pocket).
     */
    public static Instance applyAt(ServerLevel level, MechanicDef d, BlockPos pos, int radius, String[] why) {
        if (!Safety.allowedAt(level, pos)) {
            if (why != null) why[0] = "that place is a safe room, the Hub or a rift pocket";
            return null;
        }
        Sector s = MegaDungeon.sectorAt(level, pos);
        Room room = s == null ? null : s.roomAt(pos);
        Area area;
        Room target = null;
        if (d.scope() == Scope.FLOOR && s != null) area = Area.floor(s.region);
        else if ((d.scope() == Scope.ROOM || d.scope() == Scope.BOSS) && room != null) { area = Area.room(room.box); target = room; }
        else area = Area.zone(pos, Math.max(2, radius), room == null ? null : room.box);
        int floor = MegaDungeon.floorAt(level, pos);
        String key = "admin:" + (adminSeq++) + ":" + d.id();
        Instance in = new Instance(key, d, level, s, target != null ? target : room, area, pos, floor != 0 ? floor : 1, level.getRandom().nextLong(), MechanicsConfig.difficulty());
        in.admin = true;
        start(in);
        return in;
    }

    /** Ends the instance with this key. */
    public static boolean endKey(String key) {
        Instance in = ACTIVE.get(key);
        if (in == null) return false;
        end(in);
        return true;
    }

    public static Instance get(String key) { return ACTIVE.get(key); }

    public static void forgetPlans() { PLANS.clear(); }
}
