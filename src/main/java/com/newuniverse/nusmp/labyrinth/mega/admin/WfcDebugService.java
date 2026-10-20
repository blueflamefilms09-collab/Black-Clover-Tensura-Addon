package com.newuniverse.nusmp.labyrinth.mega.admin;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.Roles;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.RoomCategory;
import com.newuniverse.nusmp.labyrinth.mega.gen.SectorGenerator;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: {@code /dungeon debug wfc} is the owner's way to look at a generated floor and SEE whether the anti-dead-end rule held. The judging and the geometry are pure
 * ({@link WfcDebugModel}); this class is the part that touches the game: it reads the SAVED sector under the admin (its rooms and its doorway pairs, so nothing is recomputed and
 * nothing is generated), builds the model once per floor, and every {@value WfcDebugModel#BURST_EVERY} ticks sends the admin, and only the admin, a small burst of coloured dust
 * particles through {@code ServerLevel.sendParticles(player, ...)} (the overload that addresses one player and may exceed the usual 32-block limit, since the view reaches 72
 * blocks). The throttle is deliberate because particles are one packet each: at most {@value WfcDebugModel#MAX_PER_BURST} points per burst, at most
 * {@value WfcDebugModel#MAX_PER_TICK} in one tick for all admins together, admins staggered by a hash of their id so their bursts do not fall on the same tick, the picture
 * built up over successive bursts from a cursor. The view ends by itself after five minutes, when the admin leaves the dimension, logs out or the server stops, so a forgotten debug
 * never keeps sending. Server thread only.
 */
public final class WfcDebugService {
    private WfcDebugService() {}

    private static final Logger LOG = LogUtils.getLogger();

    private static final class Session {
        final UUID id;
        final ResourceKey<Level> dimension;
        final long start;
        final WfcDebugModel.Cursor cursor = new WfcDebugModel.Cursor();
        int sectorId = -1;
        WfcDebugModel.Model model;

        Session(UUID id, ResourceKey<Level> dimension, long start) {
            this.id = id;
            this.dimension = dimension;
            this.start = start;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new LinkedHashMap<>();
    private static long budgetTick = -1;
    private static int budgetUsed;

    private static final DustParticleOptions[][] DUST = new DustParticleOptions[WfcDebugModel.Hue.values().length][WfcDebugModel.Kind.values().length];

    static {
        float[][] rgb = {{0.15f, 1.0f, 0.30f}, {1.0f, 0.90f, 0.10f}, {1.0f, 0.15f, 0.15f}};          // GREEN, YELLOW, RED (the order of WfcDebugModel.Hue)
        float[] scale = {1.7f, 0.8f, 1.0f};                                                              // NODE, EDGE, BOX (the order of WfcDebugModel.Kind)
        for (int h = 0; h < rgb.length; h++) for (int k = 0; k < scale.length; k++) DUST[h][k] = new DustParticleOptions(new Vector3f(rgb[h][0], rgb[h][1], rgb[h][2]), scale[k]);
    }

    // ================================================================ the switch
    public static boolean on(UUID id) { return SESSIONS.containsKey(id); }

    public static int sessions() { return SESSIONS.size(); }

    /** {@code debug wfc}: switches the view off when it is on, else on for the floor under the admin. Returns 1 when it changed, 0 when it was refused. */
    public static int toggle(ServerPlayer p) {
        if (SESSIONS.remove(p.getUUID()) != null) {
            AdminMsg.tell(p, AdminMsg.gray(AdminText.DEBUG_OFF));
            return 1;
        }
        ServerLevel level = p.serverLevel();
        if (!MegaDungeon.in(level)) {
            AdminMsg.tell(p, AdminMsg.red(AdminText.NOT_IN_DUNGEON, level.dimension().location().toString()));
            return 0;
        }
        Sector s = MegaDungeon.sectorAt(level, p.blockPosition());
        if (s == null || s.rooms.isEmpty() || !SectorGenerator.playable(s)) {
            AdminMsg.tell(p, AdminMsg.red(AdminText.DEBUG_NO_FLOOR));
            return 0;
        }
        Session session = new Session(p.getUUID(), level.dimension(), p.getServer() == null ? 0L : p.getServer().getTickCount());
        session.sectorId = s.id;
        session.model = modelOf(s);
        SESSIONS.put(p.getUUID(), session);
        AdminMsg.tell(p, AdminMsg.green(AdminText.DEBUG_ON, WfcDebugModel.TTL_TICKS / 1200L));
        summary(p, s, session.model);
        return 1;
    }

    // ================================================================ the model of a saved sector
    /** The model of a saved sector: its rooms and its doorway pairs, judged by the same graph the cyclic generator uses. */
    static WfcDebugModel.Model modelOf(Sector s) {
        List<WfcDebugModel.RoomIn> rooms = new ArrayList<>(s.rooms.size());
        for (Room r : s.rooms) {
            int roles = Roles.of(r.kind) | (r.kind == RoomKind.LINK ? Roles.ENTRANCE : 0);        // a tunnel piece of a ten-floor sector ends at the previous sector: not a dead end
            rooms.add(new WfcDebugModel.RoomIn(r.id, roles, RoomCategory.of(r.kind, r.template), r.box.minX(), r.box.minY(), r.box.minZ(), r.box.maxX(), r.box.maxY(), r.box.maxZ()));
        }
        return WfcDebugModel.build(rooms, s.edges, MegaGrid.FLOOR_Y);
    }

    /** The summary lines: the numbers, then the verdict (green when the floor has no dead end and is one piece, red with what is wrong). */
    static void summary(ServerPlayer p, Sector s, WfcDebugModel.Model m) {
        WfcDebugModel.Summary sum = m.summary;
        String floor = s.isFloor() ? FloorMap.label(s.firstFloor()) : FloorMap.label(s.firstFloor()) + ".." + FloorMap.label(s.lastFloor());
        AdminMsg.tell(p, AdminMsg.gray(AdminText.DEBUG_SUMMARY, floor, s.id, sum.rooms(), sum.edges(), sum.deadEnds(), sum.loops(), sum.onLoop(), sum.singleLink(), sum.components()));
        AdminMsg.tell(p, sum.clean() ? AdminMsg.green(AdminText.DEBUG_VERDICT_OK) : AdminMsg.red(AdminText.DEBUG_VERDICT_BAD, sum.deadEnds(), sum.components()));
    }

    // ================================================================ the clock
    /** Called once per server tick: ends the views that are over and sends the bursts that are due. */
    public static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) return;
        long t = server.getTickCount();
        if (budgetTick != t) {
            budgetTick = t;
            budgetUsed = 0;
        }
        for (Session s : new ArrayList<>(SESSIONS.values())) {
            ServerPlayer p = server.getPlayerList().getPlayer(s.id);
            if (p == null) {
                SESSIONS.remove(s.id);
                continue;
            }
            if (p.level().dimension() != s.dimension) {
                SESSIONS.remove(s.id);
                AdminMsg.tell(p, AdminMsg.gray(AdminText.DEBUG_AUTO_DIM));
                continue;
            }
            if (WfcDebugModel.expired(s.start, t)) {
                SESSIONS.remove(s.id);
                AdminMsg.tell(p, AdminMsg.gray(AdminText.DEBUG_AUTO_TIME, WfcDebugModel.TTL_TICKS / 1200L));
                continue;
            }
            if (!WfcDebugModel.burstNow(t, s.id.hashCode())) continue;
            try {
                burst(p, s);
            } catch (RuntimeException e) {
                SESSIONS.remove(s.id);
                LOG.warn("[nusmp] /dungeon debug wfc stopped for {}: {}", p.getGameProfile().getName(), e.toString());
                AdminMsg.tell(p, AdminMsg.red(AdminText.FAILED, e.getClass().getSimpleName()));
            }
        }
    }

    private static void burst(ServerPlayer p, Session s) {
        ServerLevel level = p.serverLevel();
        Sector at = MegaDungeon.sectorAt(level, p.blockPosition());
        if (at != null && at.id != s.sectorId && !at.rooms.isEmpty() && SectorGenerator.playable(at)) {          // the admin walked into another floor: show that one
            s.sectorId = at.id;
            s.model = modelOf(at);
            s.cursor.nodes = s.cursor.edges = s.cursor.boxes = 0;
            summary(p, at, s.model);
        }
        if (s.model == null) return;
        int room = Math.min(WfcDebugModel.MAX_PER_BURST, WfcDebugModel.MAX_PER_TICK - budgetUsed);
        if (room <= 0) return;                                                       // other admins used this tick's share: the next burst is ten ticks away
        WfcDebugModel.Burst b = WfcDebugModel.next(s.model, s.cursor, p.getX(), p.getY(), p.getZ(), WfcDebugModel.RANGE, room);
        for (int i = 0; i < b.count(); i++) {
            DustParticleOptions dust = DUST[b.hue()[i]][b.kind()[i]];
            level.sendParticles(p, dust, true, b.xyz()[3 * i], b.xyz()[3 * i + 1], b.xyz()[3 * i + 2], 1, 0.0, 0.0, 0.0, 0.0);
        }
        budgetUsed += b.count();
    }

    // ================================================================ housekeeping
    public static void forget(UUID id) { SESSIONS.remove(id); }

    public static void clear() {
        SESSIONS.clear();
        budgetTick = -1;
        budgetUsed = 0;
    }
}
