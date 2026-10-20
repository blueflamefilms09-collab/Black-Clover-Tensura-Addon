package com.newuniverse.nusmp.labyrinth.mega.admin;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.gate.LabyrinthTravel;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaConfig;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.floors.FloorTracker;
import com.newuniverse.nusmp.labyrinth.mega.floors.FloorsConfig;
import com.newuniverse.nusmp.labyrinth.mega.floors.LockPolicy;
import com.newuniverse.nusmp.labyrinth.mega.gen.ExpansionQueue;
import com.newuniverse.nusmp.labyrinth.mega.gen.SectorGenerator;
import com.newuniverse.nusmp.labyrinth.mega.hub.HubTravel;
import com.newuniverse.nusmp.labyrinth.mega.party.PartyService;
import com.newuniverse.nusmp.labyrinth.mega.rift.Waypoint;
import com.newuniverse.nusmp.labyrinth.mega.travel.MegaTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: {@code /dungeon tp <floor>} is the admin's way to any floor from -1000 to +1000, and the dungeon builds floors lazily, one at a time, over many server ticks (a floor is only
 * queued once the floor before it stands). A teleport command that built the road itself would freeze the server for as long as it took, and one that only worked on built floors
 * would be useless for the very thing it exists for (testing floor 400 without walking 399 floors). So this class never generates anything on the command's own tick: a request
 * becomes a TICKET, and a small clock (every five ticks) asks {@link TpRules} what the ticket needs: queue the road's first floor, queue the next floor through the generator's own
 * API ({@code ExpansionQueue.tryFirst / tryNext}, the calls {@code /multiverse mega expand} and the Gatekeeper make; the generator then paces itself with its per-tick time
 * budget), wait, give up with the reason, or arrive. A floor that already stands is reached at once, in the command's own tick. The arrival follows the pattern of the existing
 * floor teleporter ({@code MegaTravel.go}, which generates the one landing chunk before the player appears and remembers the way home), preceded by a portal ticket on the landing's
 * chunks so they are loaded while the player is still where they were, and followed by writing the player's waypoint (the {@code Waypoint} record the Return Crystal reads) with the
 * landing, because the periodic sampler refuses to save a position on a stair pad. Floor 0 is the Sanctuary Hub and goes through the Hub's own travel. Everything runs on the server
 * thread; one ticket per player (a new request replaces the old one).
 */
public final class FloorTeleport {
    private FloorTeleport() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** Ticks between two looks at the open tickets. */
    private static final int CHECK_EVERY = 5;
    /** How many looks an arrival waits for its landing chunk to be loaded before it goes anyway (the single chunk is then loaded by the existing teleporter). */
    private static final int CHUNK_LOOKS = 12;
    /** Ticks between two progress lines on the action bar. */
    private static final int PROGRESS_EVERY = 40;
    /** A guess for the countdown shown to the admin: seconds the generator needs per floor. */
    private static final double SECONDS_PER_FLOOR = 2.0;

    /**
     * {@code mega.expansion = false} only switches off the growth that a Gatekeeper door starts ("the dungeon only grows with /multiverse mega expand"); an admin's own command
     * still grows it, as {@code /multiverse mega expand} does, so a teleport to a floor that does not exist yet may queue floors whatever the switch says. The dungeon's size
     * limit {@code mega.maxSectors} still holds.
     */
    static final boolean ADMIN_GROWS_ANYWAY = true;

    private enum Outcome { ARRIVED, WAITING, FAILED }

    private static final class Ticket {
        final UUID player;
        final int floor;
        final int party;
        final long created;
        final int total;
        long lastChange;
        long lastTold;
        int signature = Integer.MIN_VALUE;
        int chunkLooks;
        boolean chunkTold;

        Ticket(UUID player, int floor, int party, long created, int total) {
            this.player = player;
            this.floor = floor;
            this.party = party;
            this.created = created;
            this.lastChange = created;
            this.total = total;
        }
    }

    private static final Map<UUID, Ticket> PENDING = new LinkedHashMap<>();

    // ================================================================ the request
    /** Starts a teleport of {@code p} to {@code floor}. Returns 1 when it went (or is queued), 0 when it was refused (the player was told why). */
    public static int request(ServerPlayer p, int floor) {
        MinecraftServer server = p.getServer();
        if (server == null) return 0;
        if (!DungeonAdminMath.validFloor(floor)) {
            AdminMsg.tell(p, AdminMsg.red(AdminText.TP_RANGE, FloorMap.label(floor)));
            return 0;
        }
        Ticket old = PENDING.remove(p.getUUID());
        if (old != null) AdminMsg.tell(p, AdminMsg.gray(AdminText.TP_REPLACED, FloorMap.label(old.floor)));
        if (floor == 0) return hub(p);
        ServerLevel mega = MegaDungeon.level(server);
        if (mega == null) {
            AdminMsg.tell(p, AdminMsg.red(AdminText.NO_DIM));
            return 0;
        }
        Polarity road = Polarity.ofFloor(floor);
        if (!LockPolicy.allows(p, road)) {
            AdminMsg.tell(p, AdminMsg.red(AdminText.TP_LOCKED, AdminMsg.text(road == Polarity.HOLY ? AdminText.ROAD_HOLY : AdminText.ROAD_ABYSSAL)));
            return 0;
        }
        int party = partyFor(p);
        if (party < 0) {
            AdminMsg.tell(p, AdminMsg.red(AdminText.TP_NO_PARTY));
            return 0;
        }
        MegaData data = MegaData.get(server);
        List<Sector> chain = data.chain(party, road);
        int per = perSector(chain);
        int target = DungeonAdminMath.chainIndexOf(floor, per);
        int total = TpRules.remaining(chain.isEmpty(), chain.isEmpty() ? -1 : data.chainTail(party, road).chainIndex, target);
        Ticket t = new Ticket(p.getUUID(), floor, party, server.getTickCount(), total);
        Outcome o = step(server, p, t);
        if (o == Outcome.FAILED) return 0;
        if (o == Outcome.WAITING) {
            PENDING.put(t.player, t);
            AdminMsg.tell(p, AdminMsg.yellow(AdminText.TP_QUEUED, FloorMap.label(floor), total, DungeonAdminMath.etaSeconds(total, SECONDS_PER_FLOOR)));
        }
        return 1;
    }

    /** Floor 0: the Sanctuary Hub, through the Hub's own travel; a player who comes out of the dungeon also drops the dungeon's remembered way home, as leaving by the stairs does. */
    private static int hub(ServerPlayer p) {
        if (!HubTravel.available(p.getServer())) {
            AdminMsg.tell(p, AdminMsg.red(AdminText.TP_HUB_NONE));
            return 0;
        }
        boolean fromDungeon = Labyrinth.inMega(p.level());
        HubTravel.returnToPlaza(p);
        if (fromDungeon) LabyrinthTravel.forget(p);
        AdminMsg.tell(p, AdminMsg.green(AdminText.TP_HUB));
        return 1;
    }

    /** The party whose road the teleport is about: the party under the player's feet inside the dungeon, else the one the gate would put them in (handed a lane the first time). */
    private static int partyFor(ServerPlayer p) { return MegaDungeon.in(p.level()) ? PartyService.partyOf(p) : PartyService.enter(p); }

    /** Floors per sector of this chain: what its first sector was built with, else what the config asks for next. */
    private static int perSector(List<Sector> chain) {
        return chain.isEmpty() ? FloorMap.floorsPerSector(FloorsConfig.get(FloorsConfig.PER_SECTOR)) : chain.get(0).perRegion;
    }

    // ================================================================ the facts and one step
    /** The facts {@link TpRules} decides on, read from the saved data. {@code idle} is how many ticks nothing has moved. */
    static TpRules.View viewOf(MinecraftServer server, int party, int floor, long idle) {
        MegaData data = MegaData.get(server);
        Polarity road = Polarity.ofFloor(floor);
        List<Sector> chain = data.chain(party, road);
        int idx = DungeonAdminMath.chainIndexOf(floor, perSector(chain));
        Sector target = data.chainSector(party, road, idx);
        Sector tail = data.chainTail(party, road);
        boolean empty = chain.isEmpty() || tail == null;
        return new TpRules.View(empty, empty ? -1 : tail.chainIndex, !empty && SectorGenerator.playable(tail), !empty && tail.state == Sector.State.FAILED, !empty && tail.nextId >= 0,
                idx, target != null, target != null && SectorGenerator.playable(target), target != null && target.state == Sector.State.FAILED, data.sectorCount(party),
                MegaConfig.get(MegaConfig.MAX_SECTORS), ADMIN_GROWS_ANYWAY || MegaConfig.get(MegaConfig.EXPANSION), idle);
    }

    /** What the next step for a teleport to {@code floor} of {@code party}'s road would be, with no side effect (the self-test asks). */
    public static TpRules.Step stepFor(MinecraftServer server, int party, int floor) { return TpRules.decide(viewOf(server, party, floor, 0L)); }

    private static Outcome step(MinecraftServer server, ServerPlayer p, Ticket t) {
        ServerLevel mega = MegaDungeon.level(server);
        if (mega == null) {
            AdminMsg.tell(p, AdminMsg.red(AdminText.NO_DIM));
            return Outcome.FAILED;
        }
        long now = server.getTickCount();
        TpRules.View probe = viewOf(server, t.party, t.floor, 0L);
        int signature = (probe.chainEmpty() ? -1 : probe.tailIndex()) * 4 + (probe.tailPlayable() ? 1 : 0) + (probe.targetExists() ? 2 : 0);
        if (signature != t.signature || ExpansionQueue.get().currentSector() >= 0) {      // the road grew, a floor finished, or the generator is busy: not stuck
            t.signature = signature;
            t.lastChange = now;
        }
        TpRules.View v = viewOf(server, t.party, t.floor, now - t.lastChange);
        TpRules.Step step = TpRules.decide(v);
        MegaData data = MegaData.get(server);
        Polarity road = Polarity.ofFloor(t.floor);
        switch (step) {
            case ARRIVE:
                return arrive(mega, p, t, data.chainSector(t.party, road, v.targetIndex()));
            case QUEUE_FIRST: {
                String why = ExpansionQueue.get().tryFirst(server, road, t.party);
                if (why != null) return refused(p, why);
                t.lastChange = now;
                return Outcome.WAITING;
            }
            case QUEUE_NEXT: {
                String why = ExpansionQueue.get().tryNext(mega, data.chainTail(t.party, road));
                if (why != null) return refused(p, why);
                t.lastChange = now;
                return Outcome.WAITING;
            }
            case WAIT:
                progress(server, p, t, v);
                return Outcome.WAITING;
            case FAIL_FAILED: {
                Sector bad = v.targetExists() ? data.chainSector(t.party, road, v.targetIndex()) : data.chainTail(t.party, road);
                AdminMsg.tell(p, AdminMsg.red(AdminText.TP_FAILED_GEN, bad == null ? -1 : bad.id));
                return Outcome.FAILED;
            }
            case FAIL_LIMIT:
                AdminMsg.tell(p, AdminMsg.red(AdminText.TP_LIMIT, v.maxSectors(), FloorMap.label(t.floor)));
                return Outcome.FAILED;
            case FAIL_OFF:
                AdminMsg.tell(p, AdminMsg.red(AdminText.TP_OFF, FloorMap.label(t.floor)));
                return Outcome.FAILED;
            case FAIL_STUCK:
            default:
                AdminMsg.tell(p, AdminMsg.red(AdminText.TP_STUCK, (now - t.lastChange) / 20, FloorMap.label(t.floor)));
                return Outcome.FAILED;
        }
    }

    private static Outcome refused(ServerPlayer p, String why) {
        AdminMsg.tell(p, AdminMsg.red(AdminText.TP_REFUSED, why));
        return Outcome.FAILED;
    }

    /** The progress line, on the action bar every two seconds: how far the road has grown towards the target floor. */
    private static void progress(MinecraftServer server, ServerPlayer p, Ticket t, TpRules.View v) {
        long now = server.getTickCount();
        if (now - t.lastTold < PROGRESS_EVERY) return;
        t.lastTold = now;
        int left = TpRules.remaining(v.chainEmpty(), v.tailIndex(), v.targetIndex());
        AdminMsg.bar(p, AdminMsg.yellow(AdminText.TP_PROGRESS, FloorMap.label(t.floor), DungeonAdminMath.percent(Math.max(0, t.total - left), t.total), left));
    }

    // ================================================================ the arrival
    private static Outcome arrive(ServerLevel mega, ServerPlayer p, Ticket t, Sector target) {
        BlockPos pad = target == null ? null : SectorGenerator.padOf(target);
        if (pad == null) {
            AdminMsg.tell(p, AdminMsg.red(AdminText.TP_NO_PAD, target == null ? -1 : target.id));
            return Outcome.FAILED;
        }
        ChunkPos at = new ChunkPos(pad);
        mega.getChunkSource().addRegionTicket(TicketType.PORTAL, at, 3, pad);               // the landing's chunks load while the player is still where they were
        if (!mega.getChunkSource().hasChunk(at.x, at.z) && t.chunkLooks++ < CHUNK_LOOKS) {
            if (!t.chunkTold) {
                t.chunkTold = true;
                AdminMsg.tell(p, AdminMsg.gray(AdminText.TP_CHUNK, FloorMap.label(t.floor)));
            }
            return Outcome.WAITING;
        }
        if (!MegaTravel.go(p, mega, target, false)) return Outcome.FAILED;                  // it tells the player why when it cannot
        writeWaypoint(p, mega, target, pad);
        int level = DungeonAdminMath.levelOfFloor(t.floor);
        AdminMsg.tell(p, AdminMsg.green(AdminText.TP_OK, FloorMap.label(t.floor), level, target.id, t.party));
        if (!target.isFloor() && target.firstFloor() != t.floor) AdminMsg.tell(p, AdminMsg.gray(AdminText.TP_OK_BAND, FloorMap.label(t.floor), FloorMap.label(target.firstFloor()), FloorMap.label(target.lastFloor())));
        if (DungeonAdminMath.isBossFloor(t.floor) || target.isBossFloor()) AdminMsg.tell(p, AdminMsg.gray(AdminText.TP_OK_BOSS));
        FloorTracker.forget(p.getUUID());                                                    // the tracker looks at the new floor within half a second and fires the floor events
        return Outcome.ARRIVED;
    }

    /**
     * Writes the player's waypoint (the record the Return Crystal, the Gate Key and the Return Portal read: dimension, sector, level, floor, position, rotation) with the landing the
     * teleport just put them on. The periodic sampler would not do it for a while: it never saves a spot on a stair pad, which is where every arrival is.
     */
    static void writeWaypoint(ServerPlayer p, ServerLevel mega, Sector s, BlockPos pad) {
        Waypoint w = new Waypoint(mega.dimension().location().toString(), s.id, Waypoint.levelOf(s.firstFloor()), s.firstFloor(), pad.getX() + 0.5, pad.getY(), pad.getZ() + 0.5, p.getYRot(), 0f,
                mega.getGameTime());
        p.getPersistentData().put(Waypoint.KEY, w.save());
    }

    // ================================================================ the clock and the housekeeping
    /** Called once per server tick: looks at the open tickets every five ticks. */
    public static void tick(MinecraftServer server) {
        if (PENDING.isEmpty() || server.getTickCount() % CHECK_EVERY != 0) return;
        for (Ticket t : new ArrayList<>(PENDING.values())) {
            ServerPlayer p = server.getPlayerList().getPlayer(t.player);
            if (p == null || !p.isAlive()) {
                PENDING.remove(t.player);
                continue;
            }
            Outcome o;
            try {
                o = step(server, p, t);
            } catch (RuntimeException e) {
                LOG.warn("[nusmp] /dungeon tp to floor {} failed for {}: {}", t.floor, p.getGameProfile().getName(), e.toString());
                AdminMsg.tell(p, AdminMsg.red(AdminText.FAILED, e.getClass().getSimpleName()));
                o = Outcome.FAILED;
            }
            if (o != Outcome.WAITING) PENDING.remove(t.player);
        }
    }

    /** Does this player have a teleport waiting for the dungeon to grow? */
    public static boolean pending(UUID id) { return PENDING.containsKey(id); }

    /** How many teleports wait. */
    public static int pendingCount() { return PENDING.size(); }

    public static void forget(UUID id) { PENDING.remove(id); }

    public static void clear() { PENDING.clear(); }
}
