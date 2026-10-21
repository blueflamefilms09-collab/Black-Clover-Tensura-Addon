package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.Marker;
import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.enc.Band;
import com.newuniverse.nusmp.labyrinth.mega.enc.EncounterDirector;
import com.newuniverse.nusmp.labyrinth.mega.enc.Sanctuary;
import com.newuniverse.nusmp.labyrinth.mega.enc.SpawnLayout;
import com.newuniverse.nusmp.labyrinth.mega.lock.LockBook;
import com.newuniverse.nusmp.labyrinth.mega.system.ToastKind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Why: the second half of the anti-speedrun layer. {@link LockdownDirector} shuts the doors of the rooms it can shut; this one answers the ways round them. Every half
 * second it looks at each player of the Mega-Dungeon and turns what they did into heat ({@link AmbushRules}): they LEFT a room with its monsters still alive and not
 * after them (the door was never shut, or they slipped past), they ran through the corridors faster than a sprint-jump (the Dungeon Master never gets his roll), they
 * TELEPORTED across the floor, or they ESCAPED a lockdown with the release rune. Heat cools by itself, so a party that fights fairly never reaches the threshold; when it
 * does, and the budget allows (a cooldown, a count per window, a cap on how many of the Director's monsters already live round the party, and never a safe room, the Hub,
 * a party that is itself locked in, Creative or Peaceful), a squad is placed in the corridor AHEAD ({@link AmbushGeometry}: the floor plan's rooms in a cone of the
 * player's heading, a corridor preferred) through the Dungeon Master's own spawner ({@link EncounterDirector#spawnAt}: the sector's table, the squad of 5 to 8 with roles,
 * the ground checks), one band stronger each time the same player does it again. It only puts monsters where the player was going: no effect, nothing taken from the player's body.
 * Everything per player is in memory and forgotten at logout, at dimension change and at server stop. Server thread only.
 */
public final class DynamicAmbushDirector {
    private DynamicAmbushDirector() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final int EVERY = 10;
    private static final double PARTY_RADIUS = 48.0, MIN_COS = 0.35, WIDE_COS = -0.25;
    private static final Random RND = new Random();

    private static final Map<UUID, AmbushLedger> LEDGERS = new HashMap<>();
    private static final Map<UUID, SpeedTracker> TRACKS = new HashMap<>();
    /** The room each player was in at the last sample: sector id, room id, room kind ordinal, floor row (y). */
    private static final Map<UUID, int[]> PREV_ROOM = new HashMap<>();
    private static final Map<UUID, double[]> PREV_POS = new HashMap<>();
    private static int firedTotal;
    private static String lastText = "";

    /** What an attempt to ambush came to. */
    public record Result(AmbushRules.Why why, boolean fired, int mobs, Band band, String text) {}

    // ---------------------------------------------------------------- the tick
    public static void onServerTick(MinecraftServer server) {
        if (server.getTickCount() % EVERY != 0 || !LockdownConfig.ambushEnabled()) return;
        ServerLevel level = MegaDungeon.level(server);
        if (level == null || level.players().isEmpty()) return;
        try {
            pass(level, level.getGameTime());
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] The ambush director failed: {}", e.toString());
        }
    }

    private static void pass(ServerLevel level, long now) {
        AmbushRules.Config cfg = LockdownConfig.ambush();
        for (ServerPlayer p : level.players()) {
            if (!LockdownDirector.eligible(p)) continue;
            AmbushLedger ledger = LEDGERS.computeIfAbsent(p.getUUID(), k -> new AmbushLedger());
            SpeedTracker track = TRACKS.computeIfAbsent(p.getUUID(), k -> new SpeedTracker());
            Sector s = MegaDungeon.sectorAt(level, p.blockPosition());
            Room r = s == null ? null : s.roomAt(p.blockPosition());
            double[] before = PREV_POS.get(p.getUUID());
            int[] prev = PREV_ROOM.get(p.getUUID());
            double hx = before == null ? 0.0 : p.getX() - before[0], hz = before == null ? 0.0 : p.getZ() - before[1];
            PREV_POS.put(p.getUUID(), new double[] {p.getX(), p.getZ()});
            PREV_ROOM.put(p.getUUID(), r == null || s == null ? null : new int[] {s.id, r.id, r.kind.ordinal(), p.getBlockY()});

            SpeedTracker.Move move = track.add(now, p.getX(), p.getZ(), cfg.teleportBlocks());
            if (move == SpeedTracker.Move.TELEPORT && LockdownConfig.get(LockdownConfig.AMBUSH_TELEPORT) && isTeleportPast(prev, s, r, p)) {
                ledger.add(AmbushRules.Signal.TELEPORT, 0, now, cfg);
            }
            if (LockdownConfig.get(LockdownConfig.AMBUSH_SPEED) && (r == null || r.kind.isCirculation()) && !(r != null && Sanctuary.is(r.kind))
                    && track.speed(now, cfg.speedWindowTicks()) > cfg.speedLimit()) {
                ledger.add(AmbushRules.Signal.FAST_TRAVEL, 0, now, cfg);
            }
            if (LockdownConfig.get(LockdownConfig.AMBUSH_LEFT) && prev != null && s != null && r != null && prev[0] == s.id && prev[1] != r.id) {
                Room left = s.room(prev[1]);
                if (left != null && !Sanctuary.is(left.kind)) {
                    int live = bypassed(level, left, p);
                    if (live >= cfg.minLiveMobs()) ledger.add(AmbushRules.Signal.LEFT_LIVE_ROOM, live, now, cfg);
                }
            }
            if (ledger.heat(now, cfg) >= cfg.threshold() && ledger.cooldownLeft(now) == 0L) attempt(level, p, ledger, cfg, now, hx, hz, false);
        }
    }

    /**
     * A teleport is a signal only when it crossed the floor on the same level of the same sector, from and to an ordinary room: a stair (which joins two floors 48 blocks
     * apart), the Hub, a safe room and {@code /dungeon unstuck} (which lands in a safe room) all move a player a long way on purpose.
     */
    private static boolean isTeleportPast(int[] prev, Sector s, Room r, ServerPlayer p) {
        if (prev == null || s == null || r == null || prev[0] != s.id) return false;
        RoomKind was = RoomKind.values()[prev[2]];
        if (was == RoomKind.STAIRWELL || Sanctuary.is(was) || r.kind == RoomKind.STAIRWELL || Sanctuary.is(r.kind)) return false;
        return Math.abs(prev[3] - p.getBlockY()) < 24;
    }

    /** The monsters of the Director, of a lockdown or of an ambush that are alive in the room and are not after any player (they were slipped past, not led away or fought by the party). */
    static int bypassed(ServerLevel level, Room room, ServerPlayer p) {
        AABB bb = new AABB(room.box.minX(), room.box.minY(), room.box.minZ(), room.box.maxX() + 1.0, room.box.maxY() + 1.0, room.box.maxZ() + 1.0);
        return level.getEntitiesOfClass(Mob.class, bb, m -> m.isAlive() && ours(m) && !(m.getTarget() instanceof net.minecraft.world.entity.player.Player)).size();
    }

    private static boolean ours(Entity e) {
        return MegaDungeon.isDungeonMonster(e) || e.getTags().contains(LockdownRegistry.MOB_TAG) || e.getTags().contains(LockdownRegistry.AMBUSH_TAG);
    }

    // ---------------------------------------------------------------- the signal from a lockdown that was escaped
    /** The players of a locked room left it by the release rune or a timer, with the room uncleared: that is a bypass, and it is answered. */
    public static void noteEscape(ServerLevel level, List<ServerPlayer> players) {
        if (!LockdownConfig.ambushEnabled() || !LockdownConfig.get(LockdownConfig.AMBUSH_ESCAPE)) return;
        AmbushRules.Config cfg = LockdownConfig.ambush();
        long now = level.getGameTime();
        for (ServerPlayer p : players) {
            if (!LockdownDirector.eligible(p)) continue;
            LEDGERS.computeIfAbsent(p.getUUID(), k -> new AmbushLedger()).add(AmbushRules.Signal.ESCAPED_LOCKDOWN, 0, now, cfg);
        }
    }

    // ---------------------------------------------------------------- one attempt
    /**
     * Decides and, if the rules say so, places the ambush for a player whose heat is up. {@code force} skips the heat and the cooldown (the admin command and the
     * self-test) but never the exclusions: a safe room, a locked-in party, Creative, Peaceful and the crowd cap still refuse it.
     */
    public static Result attempt(ServerLevel level, ServerPlayer p, AmbushLedger ledger, AmbushRules.Config cfg, long now, double hx, double hz, boolean force) {
        Sector sector = MegaDungeon.sectorAt(level, p.blockPosition());
        Room room = sector == null ? null : sector.roomAt(p.blockPosition());
        List<ServerPlayer> party = partyOf(level, p);
        boolean safe = (room != null && Sanctuary.is(room.kind)) || MegaDungeon.inSafeZone(level, p.blockPosition());
        boolean locked = LockBook.isSealed(p) || (sector != null && room != null && LockdownDirector.isLocked(level, sector, room));
        int live = EncounterDirector.liveNearParty(level, party);
        double heat = force ? Math.max(ledger.heat(now, cfg), cfg.threshold()) : ledger.heat(now, cfg);
        long cool = force ? 0L : ledger.cooldownLeft(now);
        AmbushRules.Facts facts = new AmbushRules.Facts(LockdownConfig.ambushEnabled(), level.getDifficulty() == Difficulty.PEACEFUL, LockdownDirector.eligible(p), Labyrinth.inMega(level),
                safe, locked, cool, force ? 0 : ledger.firedInWindow(now, cfg), live, heat);
        AmbushRules.Why why = AmbushRules.decide(facts, cfg);
        if (why != AmbushRules.Why.FIRE) return new Result(why, false, 0, Band.NOTHING, why.name());
        if (sector == null || sector.rooms.isEmpty()) return new Result(AmbushRules.Why.NO_CELLS, false, 0, Band.NOTHING, "no sector");

        int depth = room != null ? LockdownDirector.depthOf(level, room) : Math.abs(MegaDungeon.floorAt(level, p.blockPosition()));
        Band band = AmbushRules.band(ledger.strikes(now, cfg), depth);
        if (Math.hypot(hx, hz) < 0.5) {                                                       // not moving: ahead is where the player looks
            Vec3 look = p.getLookAngle();
            hx = look.x;
            hz = look.z;
        }
        SpawnLayout layout = layoutOf(sector);
        double min = LockdownConfig.get(LockdownConfig.AMBUSH_MIN_DIST), max = Math.max(min + 8.0, LockdownConfig.get(LockdownConfig.AMBUSH_MAX_DIST));
        int own = room == null ? -1 : room.id;
        List<AmbushGeometry.Pick> picks = AmbushGeometry.ahead(layout, p.getX(), p.getZ(), hx, hz, own, min, max, MIN_COS, 4, 12);
        if (picks.isEmpty()) picks = AmbushGeometry.ahead(layout, p.getX(), p.getZ(), hx, hz, own, min, max, WIDE_COS, 4, 12);
        if (picks.isEmpty()) {
            ledger.noPlace(now, cfg);
            return new Result(AmbushRules.Why.NO_CELLS, false, 0, band, "no room ahead in " + (int) min + "-" + (int) max + " blocks");
        }
        List<BlockPos> group = new ArrayList<>(), reserve = new ArrayList<>();
        for (int i = 0; i < picks.size(); i++) {
            AmbushGeometry.Pick k = picks.get(i);
            (i < 5 ? group : reserve).add(new BlockPos(k.x(), k.y(), k.z()));
        }
        EncounterDirector.Outcome o = EncounterDirector.spawnAt(level, party, group, reserve, min, band, true, RND);
        if (!o.spawned()) {
            ledger.noPlace(now, cfg);
            lastText = o.kind() + ": " + o.text();
            return new Result(AmbushRules.Why.NO_CELLS, false, 0, band, lastText);
        }
        for (Mob m : o.mobs()) m.addTag(LockdownRegistry.AMBUSH_TAG);
        ledger.fired(now, cfg);
        firedTotal++;
        lastText = o.text();
        LockdownMsg.tellAll(party, LockdownMsg.text(LockdownText.AMBUSH).withStyle(ChatFormatting.RED));
        LockdownMsg.toast(party, ToastKind.INFO, LockdownText.TOAST_AMBUSH);
        LockdownMsg.say(party, "system_ambush_warning");                                       // 1.110.0: spoken line
        LOG.info("[nusmp] Ambush for {}: {} ({} monsters ahead, strikes before {})", p.getGameProfile().getName(), band.label(), o.mobs().size(), ledger.strikes(now, cfg) - 1);
        return new Result(AmbushRules.Why.FIRE, true, o.mobs().size(), band, o.text());
    }

    /** The players the ambush is for: the trigger and every other eligible player within the party radius. */
    static List<ServerPlayer> partyOf(ServerLevel level, ServerPlayer p) {
        List<ServerPlayer> out = new ArrayList<>();
        out.add(p);
        for (ServerPlayer q : level.players()) {
            if (q != p && LockdownDirector.eligible(q) && q.distanceToSqr(p) <= PARTY_RADIUS * PARTY_RADIUS) out.add(q);
        }
        return out;
    }

    /** The floor plan of a sector as plain numbers, for {@link AmbushGeometry}. */
    static SpawnLayout layoutOf(Sector sector) {
        List<SpawnLayout.RoomBox> rooms = new ArrayList<>(sector.rooms.size());
        for (Room r : sector.rooms) rooms.add(new SpawnLayout.RoomBox(r.id, r.kind, r.box.minX(), r.box.minY(), r.box.minZ(), r.box.maxX(), r.box.maxY(), r.box.maxZ()));
        List<SpawnLayout.Spot> spots = new ArrayList<>();
        for (Marker m : sector.markers) {
            if (m.kind() == MarkerKind.SPAWN) spots.add(new SpawnLayout.Spot(m.pos().getX(), m.pos().getY(), m.pos().getZ(), m.intArg("weight", 1)));
        }
        return new SpawnLayout(rooms, spots, sector.edges);
    }

    // ---------------------------------------------------------------- state
    /** The ledger of a player (created on first use). */
    public static AmbushLedger ledger(UUID id) { return LEDGERS.computeIfAbsent(id, k -> new AmbushLedger()); }

    public static int firedTotal() { return firedTotal; }

    public static String lastText() { return lastText; }

    /** Forgets what is remembered of a player (logout, dimension change). */
    public static void forget(UUID id) {
        LEDGERS.remove(id);
        TRACKS.remove(id);
        PREV_ROOM.remove(id);
        PREV_POS.remove(id);
    }

    /** Server stop. */
    public static void reset() {
        LEDGERS.clear();
        TRACKS.clear();
        PREV_ROOM.clear();
        PREV_POS.clear();
        firedTotal = 0;
        lastText = "";
    }
}
