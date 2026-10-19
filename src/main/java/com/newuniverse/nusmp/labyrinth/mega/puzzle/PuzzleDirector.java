package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.Marker;
import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.enc.Sanctuary;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.Area;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.MechCategory;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicDef;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicsData;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicsDirector;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.Planner;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.PuzzleBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the server-side conductor of the puzzle rooms, the sibling of the mechanics Director and built the same way: when a delver stands in a room that the PLAN gave a puzzle, it makes sure
 * the puzzle exists (a {@link PuzzleInstance} with a stage found in the real room), ticks every live puzzle each tick with the players inside it, and ends it when it is solved and has been
 * admired, when nobody has been inside for ten seconds, when it winds down by itself, when an admin clears it, when the switch in the weights file is turned off or when the server stops;
 * ending always restores every block. The plan is {@link PuzzlePlanner}'s pure answer to the sector's rooms, theme, road and depth under the weights file; this class only turns the sector
 * into the planner's input (a room is "busy" when the mechanics library planned a sealing or puzzle mechanic there; a room whose template carries a PUZZLE marker is a marker room) and the
 * plan into live puzzles. It never re-arms a solved puzzle ({@code MechanicsData}, key {@code puzzle:...}), never acts in a safe room, the Hub or a rift pocket, and a puzzle that cannot be
 * placed is retried after a minute, never forced.
 */
public final class PuzzleDirector {
    private PuzzleDirector() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final int SCAN_EVERY = 10, GRACE = 200;
    static final Map<String, PuzzleInstance> ACTIVE = new LinkedHashMap<>();
    private static final Map<Integer, Plan> PLANS = new HashMap<>();
    private static final Map<String, Long> COOLDOWN = new HashMap<>();
    private static final Set<Integer> SUPPRESSED = new HashSet<>();
    private static final Map<Integer, Long> LAST_INSIDE = new HashMap<>();
    private static boolean recovered;
    private static int adminSeq;

    private record Plan(int sectorId, int stamp, int rooms, List<PuzzlePlanner.Slot> slots) {}

    public static List<PuzzleInstance> active() { return new ArrayList<>(ACTIVE.values()); }

    public static PuzzleInstance byId(int id) {
        for (PuzzleInstance in : ACTIVE.values()) if (in.id == id) return in;
        return null;
    }

    // ---------------------------------------------------------------- the plan
    private static boolean eligible(Room r) {
        RoomKind k = r.kind;
        if (k.isSafe() || Sanctuary.is(k)) return false;
        return k == RoomKind.HALL || k == RoomKind.SET_PIECE || k == RoomKind.CAVERN || k == RoomKind.TRIAL || k == RoomKind.TRAP || k == RoomKind.PUZZLE || k == RoomKind.TREASURE;
    }

    private static boolean sealing(MechanicDef d) {
        if (d.category() == MechCategory.PUZZLE) return true;
        for (MechanicDef.Effect e : d.effects()) {
            String op = e.op();
            if (op.equals("seal_doors") || op.equals("cage") || op.equals("flood") || op.equals("ceiling")) return true;
        }
        return false;
    }

    /** The floor as the planner sees it. */
    public static PuzzlePlanner.FloorInfo floorInfo(Sector s) {
        Set<Integer> busy = new HashSet<>();
        try {
            for (Planner.Slot m : MechanicsDirector.planFor(s)) if (m.roomId() >= 0 && sealing(m.def())) busy.add(m.roomId());
        } catch (RuntimeException ignored) {
            // the mechanics library not being loaded only means no room is busy
        }
        List<Marker> markers = s.markers(MarkerKind.PUZZLE);
        List<PuzzlePlanner.RoomInfo> rooms = new ArrayList<>();
        for (Room r : s.rooms) {
            String forced = "";
            boolean marked = false;
            for (Marker m : markers) if (r.contains(m.pos())) { marked = true; forced = m.arg("type", ""); }
            rooms.add(new PuzzlePlanner.RoomInfo(r.id, r.kind.folder(), r.template, Math.min(r.box.getXSpan(), r.box.getZSpan()), eligible(r), busy.contains(r.id), marked, forced));
        }
        return new PuzzlePlanner.FloorInfo(s.firstFloor(), s.polarity == Polarity.ABYSSAL, s.theme.id(), s.seed, s.isBossFloor(), rooms);
    }

    /** The (cached) plan of a sector. */
    public static List<PuzzlePlanner.Slot> planFor(Sector s) {
        PuzzleWeights w = PuzzleWeights.get();
        int stamp = System.identityHashCode(w);
        Plan p = PLANS.get(s.id);
        if (p != null && p.stamp() == stamp && p.rooms() == s.rooms.size()) return p.slots();
        List<PuzzlePlanner.Slot> slots = PuzzlePlanner.plan(floorInfo(s), w);
        PLANS.put(s.id, new Plan(s.id, stamp, s.rooms.size(), slots));
        return slots;
    }

    public static void forgetPlans() { PLANS.clear(); }

    /** Does the plan give this room a puzzle of the new kind? (The mechanics Director asks this so a puzzle mechanic does not also sit in the room.) */
    public static boolean claims(Sector s, int roomId) {
        if (s == null) return false;
        for (PuzzlePlanner.Slot slot : planFor(s)) if (slot.roomId() == roomId) return true;
        return false;
    }

    static String key(Sector s, PuzzlePlanner.Slot slot) { return s.id + ":" + slot.roomId() + ":" + slot.type(); }

    // ---------------------------------------------------------------- the tick
    public static void onServerTick(ServerTickEvent.Post e) {
        try {
            tick(e.getServer());
        } catch (Throwable t) {
            LOG.warn("[nusmp] puzzle tick failed: {}", t.toString());
        }
    }

    private static void tick(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return;
        if (!recovered) {
            recovered = true;
            int n = PuzzleData.get(server).recover(level);
            if (n > 0) LOG.info("[nusmp] puzzles: restored {} journals left over from the last run", n);
        }
        long now = level.getGameTime();
        if (!PuzzleWeights.get().enabled) {
            if (!ACTIVE.isEmpty()) clearAll();
            return;
        }
        List<ServerPlayer> players = level.players();
        if (now % SCAN_EVERY == 3 && !players.isEmpty()) scan(level, players, now);
        if (ACTIVE.isEmpty()) return;
        for (PuzzleInstance in : new ArrayList<>(ACTIVE.values())) {
            try {
                List<ServerPlayer> inside = playersIn(in, players);
                in.update(inside, now);
                if (!inside.isEmpty()) LAST_INSIDE.put(in.id, now);
                long last = LAST_INSIDE.getOrDefault(in.id, now);
                LAST_INSIDE.putIfAbsent(in.id, now);
                boolean over = in.state == PuzzleInstance.State.ENDED || in.solvedLongEnough(now) || (inside.isEmpty() && now - last > GRACE && !in.admin) || in.errors > 40;
                if (over) end(in, now);
            } catch (RuntimeException ex) {
                LOG.warn("[nusmp] puzzle {} failed and was ended: {}", in.type, ex.toString());
                end(in, now);
            }
        }
        if (now % 40 == 0) {
            PuzzleData d = PuzzleData.get(server);
            if (d.anyJournal()) d.setDirty();
        }
    }

    private static boolean counts(ServerPlayer p) { return p.isAlive() && !p.isSpectator(); }

    static List<ServerPlayer> playersIn(PuzzleInstance in, List<ServerPlayer> all) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : all) {
            if (!counts(p)) continue;
            boolean inArea = in.area.kind == Area.Kind.ROOM ? in.area.containsWithMargin(p.getX(), p.getY(), p.getZ(), 1) : in.area.contains(p);
            if (inArea && PuzzleBridge.allowedAt(in.level, p.blockPosition())) out.add(p);
        }
        return out;
    }

    private static void scan(ServerLevel level, List<ServerPlayer> players, long now) {
        MechanicsData data = MechanicsData.get(level.getServer());
        for (ServerPlayer p : players) {
            if (!counts(p)) continue;
            BlockPos pos = p.blockPosition();
            if (!PuzzleBridge.allowedAt(level, pos)) continue;
            Sector s = MegaDungeon.sectorAt(level, pos);
            if (s == null || s.rooms.isEmpty() || s.state == Sector.State.GENERATING || s.state == Sector.State.FAILED || SUPPRESSED.contains(s.id)) continue;
            Room here = s.roomAt(pos);
            if (here == null) continue;
            for (PuzzlePlanner.Slot slot : planFor(s)) {
                if (slot.roomId() != here.id) continue;
                String k = key(s, slot);
                if (ACTIVE.containsKey(k) || data.isSolved("puzzle:" + k) || COOLDOWN.getOrDefault(k, 0L) > now) continue;
                try {
                    PuzzleInstance in = build(level, s, slot, here, k);
                    List<ServerPlayer> inside = new ArrayList<>();
                    for (ServerPlayer q : players) if (counts(q) && in.area.containsWithMargin(q.getX(), q.getY(), q.getZ(), 1)) inside.add(q);
                    if (in.arm(inside, now)) start(in);
                    else {
                        COOLDOWN.put(k, now + 1200);
                        LOG.debug("[nusmp] puzzle {} does not fit in room {} of sector {}", slot.type(), here.id, s.id);
                    }
                } catch (RuntimeException ex) {
                    COOLDOWN.put(k, now + 1200);
                    LOG.warn("[nusmp] puzzle {} could not start: {}", slot.type(), ex.toString());
                }
            }
        }
    }

    private static PuzzleInstance build(ServerLevel level, Sector s, PuzzlePlanner.Slot slot, Room room, String key) {
        BlockPos anchor = room.center();
        for (Marker m : s.markers(MarkerKind.PUZZLE)) if (room.contains(m.pos())) anchor = m.pos();
        PuzzleWeights w = PuzzleWeights.get();
        int floor = MegaDungeon.floorAt(level, anchor) != 0 ? MegaDungeon.floorAt(level, anchor) : s.firstFloor();
        return new PuzzleInstance(key, slot.type(), level, s, room, Area.room(room.box), anchor, floor, s.theme.id(), w.flavourOf(s.theme.id()), slot.seed());
    }

    private static void start(PuzzleInstance in) {
        ACTIVE.put(in.key, in);
        PuzzleData.get(in.level.getServer()).track(in);
    }

    // ---------------------------------------------------------------- ending
    static void end(PuzzleInstance in, long now) {
        try {
            in.end();
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] puzzle {} did not end cleanly: {}", in.type, e.toString());
        }
        ACTIVE.remove(in.key);
        LAST_INSIDE.remove(in.id);
        if (in.bypassed || in.dismissed) COOLDOWN.put(in.key, now + 12000L);
    }

    /** Ends one puzzle (a self-test, the admin command). */
    public static void end(PuzzleInstance in) { end(in, in.level.getGameTime()); }

    /** Ends every live puzzle (switch off, admin clear, server stop). */
    public static int clearAll() {
        int n = ACTIVE.size();
        for (PuzzleInstance in : new ArrayList<>(ACTIVE.values())) end(in);
        ACTIVE.clear();
        return n;
    }

    public static void clearSector(int sectorId) {
        for (PuzzleInstance in : new ArrayList<>(ACTIVE.values())) if (in.sector != null && in.sector.id == sectorId) end(in);
        SUPPRESSED.add(sectorId);
    }

    public static void resetSuppression() {
        SUPPRESSED.clear();
        COOLDOWN.clear();
    }

    public static void onServerStopping() {
        clearAll();
        PLANS.clear();
        SUPPRESSED.clear();
        COOLDOWN.clear();
        LAST_INSIDE.clear();
        recovered = false;
    }

    // ---------------------------------------------------------------- for the admin command and the self-tests
    /**
     * Starts a puzzle at a position on purpose (an admin, a self-test). Returns the live instance, or null with {@code why[0]} set when the place is not allowed or has no room for it. The area
     * is the room the position is in, else a disc round it. The seed is the given one (a self-test wants the same board every run).
     */
    public static PuzzleInstance applyAt(ServerLevel level, String type, BlockPos pos, long seed, List<ServerPlayer> players, long now, String[] why) {
        if (!PuzzleCatalog.has(type)) {
            if (why != null) why[0] = "no such puzzle";
            return null;
        }
        if (!PuzzleBridge.allowedAt(level, pos)) {
            if (why != null) why[0] = "that place is a safe room, the Hub or a rift pocket";
            return null;
        }
        Sector s = MegaDungeon.sectorAt(level, pos);
        Room room = s == null ? null : s.roomAt(pos);
        Area area = room != null ? Area.room(room.box) : Area.zone(pos, 18, null);
        int floor = MegaDungeon.floorAt(level, pos);
        String theme = s == null ? "" : s.theme.id();
        String key = "admin:" + (adminSeq++) + ":" + type;
        PuzzleInstance in = new PuzzleInstance(key, type, level, s, room, area, pos, floor != 0 ? floor : 1, theme, PuzzleWeights.get().flavourOf(theme), seed);
        in.admin = true;
        if (!in.arm(players, now)) {
            if (why != null) why[0] = "no room for that puzzle here";
            return null;
        }
        start(in);
        return in;
    }
}
