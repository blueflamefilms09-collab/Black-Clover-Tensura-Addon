package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.enc.Band;
import com.newuniverse.nusmp.labyrinth.mega.enc.Sanctuary;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.Instance;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicsDirector;
import com.newuniverse.nusmp.labyrinth.mega.system.ToastKind;
import com.newuniverse.nusmp.labyrinth.trial.TrialRegistry;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Why: this is the half of the anti-speedrun layer that CLOSES doors. Twice a second it looks at where the players of the Mega-Dungeon stand; a room that a party is well
 * inside of is asked its questions in order (the pure {@link LockdownMath#decide}): is it a challenge room (the room data, {@link LockdownRooms}), is it still fresh (not
 * cleared, not locked, not cooling down after an escape) and is anybody deep enough inside. If it passes, {@link RoomLockdownEvent} is posted (a listener may change the
 * wave or cancel the lock), the doorways of the room are found in the loaded world (the passable cells of the wall that go all the way through it, judged by
 * {@link OpeningScan}: a cavern or a ruined hall has no wall to close and is left open, and a dead niche behind a door cap is not a doorway), the first wave is spawned
 * through the Dungeon Master ({@link LockdownWaves}) BEFORE anything is placed (a room whose wave could not be placed never locks: nobody is ever sealed in with nothing to
 * fight), and then the barrier blocks go in, the first of them the room's brain ({@link LockdownBarrierBlockEntity}). Rooms that are already sealed by something else (a
 * trial hall's Sealed Archway, a puzzle's bars) are left to it. Everything is bounded: a room is looked at once per pass, the scan runs once per arming, a room that cannot
 * lock is remembered for the session so it is never scanned twice, and every refusal has a name that {@code /multiverse mega lockdown status} prints. Server thread only.
 */
public final class LockdownDirector {
    private LockdownDirector() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final int EVERY = 10;
    private static final Random RND = new Random();

    /** Rooms cooling down after an escape or a refusal: key to the game time they may be looked at again. */
    private static final Map<Long, Long> COOLDOWN = new HashMap<>();
    /** Rooms that cannot lock in this session, with the reason (open plan, a seal of their own). */
    private static final Map<Long, String> REJECTED = new LinkedHashMap<>();
    /** Rooms locked now: key to the position of the master barrier. */
    private static final Map<Long, Long> LOCKED = new HashMap<>();
    /** The last verdict of each room, for the status command. */
    private static final Map<Long, String> LAST = new HashMap<>();
    private static int armedTotal;
    private static long passes;

    /** What an arming attempt came to. */
    public record ArmResult(boolean armed, String why, int cells, int mobs) {
        static ArmResult no(String why) { return new ArmResult(false, why, 0, 0); }
    }

    // ---------------------------------------------------------------- the tick
    public static void onServerTick(MinecraftServer server) {
        if (server.getTickCount() % EVERY != 5 || !LockdownConfig.enabled()) return;
        ServerLevel level = MegaDungeon.level(server);
        if (level == null || level.players().isEmpty()) return;
        try {
            pass(level, level.getGameTime());
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] The lockdown director failed: {}", e.toString());
        }
    }

    /** Is this player one the lockdown counts (alive, and in a mode that is not Spectator or, unless the config says so, Creative)? */
    static boolean eligible(ServerPlayer p) {
        return p.isAlive() && !p.isSpectator() && (!p.isCreative() || LockdownConfig.get(LockdownConfig.FOR_CREATIVE));
    }

    private static void pass(ServerLevel level, long now) {
        passes++;
        Map<Long, List<ServerPlayer>> byRoom = new LinkedHashMap<>();
        Map<Long, Room> rooms = new HashMap<>();
        Map<Long, Sector> sectors = new HashMap<>();
        for (ServerPlayer p : level.players()) {
            if (!eligible(p)) continue;
            Sector s = MegaDungeon.sectorAt(level, p.blockPosition());
            if (s == null) continue;
            Room r = s.roomAt(p.blockPosition());
            if (r == null) continue;
            long key = LockdownMath.key(s.id, r.id);
            byRoom.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
            rooms.put(key, r);
            sectors.put(key, s);
        }
        for (Map.Entry<Long, List<ServerPlayer>> e : byRoom.entrySet()) {
            long key = e.getKey();
            Sector s = sectors.get(key);
            Room r = rooms.get(key);
            LockdownMath.Verdict v = verdict(level, s, r, e.getValue(), now);
            if (!v.arms()) {
                LAST.put(key, v.name());
                continue;
            }
            ArmResult res = arm(level, s, r, LockdownRooms.ruleFor(r.template, r.kind), e.getValue(), now);
            LAST.put(key, res.armed() ? "ARM" : res.why());
        }
    }

    /** The gates a room is asked, with the facts read from the world. */
    static LockdownMath.Verdict verdict(ServerLevel level, Sector s, Room r, List<ServerPlayer> party, long now) {
        long key = LockdownMath.key(s.id, r.id);
        LockdownRule rule = LockdownRooms.ruleFor(r.template, r.kind);
        boolean ready = s.state != Sector.State.GENERATING && s.state != Sector.State.FAILED;
        int inset = LockdownConfig.get(LockdownConfig.ENTRY_INSET);
        boolean deep = false;
        for (ServerPlayer p : party) {
            if (LockdownMath.deepInside(r.box.minX(), r.box.minZ(), r.box.maxX(), r.box.maxZ(), p.getX(), p.getZ(), inset)) {
                deep = true;
                break;
            }
        }
        Long cool = COOLDOWN.get(key);
        long coolLeft = cool == null ? 0L : Math.max(0L, cool - now);
        if (cool != null && coolLeft == 0L) COOLDOWN.remove(key);
        LockdownMath.Facts f = new LockdownMath.Facts(true, level.getDifficulty() == Difficulty.PEACEFUL, Sanctuary.is(r.kind),
                ready && rule.lock() && !REJECTED.containsKey(key), LockdownMath.chanceOk(s.seed, r.id, rule.chance()), depthOf(level, r), rule.minFloor(),
                LockdownMath.area(r.box.minX(), r.box.minZ(), r.box.maxX(), r.box.maxZ()), rule.minArea(), LockdownData.get(level.getServer()).isCleared(s.id, r.id),
                isLocked(level, s, r), coolLeft, !party.isEmpty(), deep);
        return LockdownMath.decide(f);
    }

    /** The absolute floor of a room (0 when unknown). */
    static int depthOf(ServerLevel level, Room r) {
        int f = r.floor != 0 ? r.floor : MegaDungeon.floorAt(level, r.center());
        return Math.abs(f);
    }

    // ---------------------------------------------------------------- arming
    /**
     * Locks a room now, asking every question except the cooldown, the chance and the inset (the caller decided those), but never skipping a safety: a room with a seal of its
     * own, a doorway with a player in it, a wave that cannot be placed, a wall that cannot be closed. Public for the self-test and the admin command.
     */
    public static ArmResult arm(ServerLevel level, Sector sector, Room room, LockdownRule rule, List<ServerPlayer> party, long now) {
        long key = LockdownMath.key(sector.id, room.id);
        if (party.isEmpty()) return ArmResult.no("nobody to lock in");
        if (ownSeal(level, room)) {
            REJECTED.put(key, "the room has a Sealed Archway of its own (a trial hall)");
            return ArmResult.no(REJECTED.get(key));
        }
        if (mechanicSeals(sector, room)) return ArmResult.no("a mechanic is sealing the room");
        // the doorways
        BoundingBox b = room.box;
        if (!level.hasChunksAt(new BlockPos(b.minX(), b.minY(), b.minZ()), new BlockPos(b.maxX(), b.maxY(), b.maxZ()))) return ArmResult.no("the room's chunks are not all loaded");
        boolean cut = isCut(level, sector);                                                // 1.107.0: the doorways of this sector are 7 x 7 through both wall layers
        OpeningScan.Scan scan = scanOpenings(level, room, cut);
        OpeningScan.Result judged = OpeningScan.judge(scan.plane(), OpeningScan.limitsFor(LockdownConfig.limits(), cut));
        if (!judged.ok()) {
            REJECTED.put(key, judged.why());
            LOG.info("[nusmp] Lockdown: room {} of sector {} ({}) does not lock: {}", room.id, sector.id, room.template, judged.why());
            return ArmResult.no(judged.why());
        }
        List<BlockPos> cells = new ArrayList<>(scan.fill().size());
        for (OpeningScan.Cell c : scan.fill()) cells.add(new BlockPos(c.x(), c.y(), c.z()));
        cells.sort((p, q) -> p.getX() != q.getX() ? Integer.compare(p.getX(), q.getX()) : p.getZ() != q.getZ() ? Integer.compare(p.getZ(), q.getZ()) : Integer.compare(p.getY(), q.getY()));
        for (ServerPlayer p : party) {                                                    // nobody is ever sealed inside a wall
            AABB bb = p.getBoundingBox();
            for (BlockPos c : cells) {
                if (bb.intersects(c.getX(), c.getY(), c.getZ(), c.getX() + 1.0, c.getY() + 1.0, c.getZ() + 1.0)) return ArmResult.no("a player stands in a doorway");
            }
        }

        int depth = depthOf(level, room);
        Band band = LockdownMath.band(rule.band(), depth, party.size());
        int waves = LockdownMath.waves(rule.waves(), depth);
        RoomLockdownEvent event = new RoomLockdownEvent(level, sector, room, party, band, waves, RoomLockdownEvent.Source.DIRECTOR);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            coolDown(sector.id, room.id, now);
            return ArmResult.no("a listener cancelled the lockdown");
        }
        band = event.getBand();
        waves = event.getWaves();

        // the first wave, before anything is placed
        LockdownWaves.Result wave = LockdownWaves.spawn(level, sector.id, room.id, party, band, RND, cells);
        if (wave.mobs().isEmpty()) {
            coolDownFor(sector.id, room.id, now, 400L);
            LOG.info("[nusmp] Lockdown: room {} of sector {} would lock but its wave could not be placed ({})", room.id, sector.id, wave.text());
            return ArmResult.no("no wave could be placed: " + wave.text());
        }

        // the barriers: all placed first (each remembering the block it replaced), then the first is made the master
        BlockState barrier = LockdownRegistry.LOCKDOWN_BARRIER.get().defaultBlockState();
        List<BlockPos> placed = new ArrayList<>(cells.size());
        Map<BlockPos, BlockState> old = new HashMap<>();
        int[] boxArr = {b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()};
        final Band waveBand = band;
        final int waveCount = waves;
        try {
            DungeonGuard.internal(() -> {
                for (BlockPos c : cells) {
                    BlockState before = level.getBlockState(c);
                    if (level.setBlock(c, barrier, Block.UPDATE_ALL)) {
                        placed.add(c);
                        old.put(c, before);
                    }
                }
                if (placed.isEmpty()) return;
                BlockPos masterPos = placed.get(0);
                for (BlockPos c : placed) {
                    if (!(level.getBlockEntity(c) instanceof LockdownBarrierBlockEntity lb)) continue;
                    if (c.equals(masterPos)) lb.initMaster(sector.id, room.id, waveBand, waveCount, boxArr, placed, wave.mobs(), old.get(c), now);
                    else lb.initSlave(masterPos.asLong(), old.get(c));
                }
            });
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] Lockdown of room {} of sector {} failed while placing the barriers: {}", room.id, sector.id, ex.toString());
            undo(level, placed, old);
            for (var m : wave.mobs()) m.discard();
            return ArmResult.no("placing the barriers failed: " + ex);
        }
        if (placed.isEmpty() || !(level.getBlockEntity(placed.get(0)) instanceof LockdownBarrierBlockEntity)) {
            undo(level, placed, old);
            for (var m : wave.mobs()) m.discard();
            return ArmResult.no("no barrier block entity was created");
        }
        LOCKED.put(key, placed.get(0).asLong());
        armedTotal++;
        announce(level, sector, room, party, placed, waveCount);
        LOG.info("[nusmp] Lockdown: room {} of sector {} ({}) is sealed: {} cells, {} waves of band {}, first wave {} monsters", room.id, sector.id, room.template, placed.size(), waveCount, waveBand.label(), wave.mobs().size());
        return new ArmResult(true, "sealed", placed.size(), wave.mobs().size());
    }

    private static void undo(ServerLevel level, List<BlockPos> placed, Map<BlockPos, BlockState> old) {
        DungeonGuard.internal(() -> {
            for (BlockPos c : placed) {
                BlockState was = old.get(c);
                if (was == null || was.isAir()) level.removeBlock(c, false);
                else level.setBlock(c, was, Block.UPDATE_ALL);
            }
        });
    }

    private static void announce(ServerLevel level, Sector sector, Room room, List<ServerPlayer> party, List<BlockPos> cells, int waves) {
        BlockPos c = room.center();
        level.playSound(null, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 0.6f);
        Fx.spray(level, FxThemes.DUNGEON_SEAL, FxOld.ENCHANT, c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5, 60, 1.5, 1.0, 1.5, 0.5);
        for (BlockPos b : cells) Fx.spray(level, FxThemes.DUNGEON_SEAL, FxOld.END_ROD, b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.02);
        LockdownMsg.tellAll(party, LockdownMsg.text(LockdownText.SEALED, 1, waves).withStyle(ChatFormatting.GOLD));
        LockdownMsg.toast(party, ToastKind.INFO, LockdownText.TOAST_SEALED, "1", Integer.toString(waves));
        LockdownMsg.say(party, "system_lockdown_start");                                       // 1.110.0: spoken line
    }

    // ---------------------------------------------------------------- the doorways
    /**
     * The passable cells of the room's wall that go all the way through it. The wall is read in the plane {@code wallDepth} blocks in from each of the four faces of the
     * room's box (the corners left out), from the floor up {@code openHeight} blocks; a cell counts when it AND every cell between it and the outside of the box, along the
     * same line, has no collision (a doorway; a niche behind a door cap has a back wall and is not one). Reads only loaded chunks.
     */
    static OpeningScan.Scan scanOpenings(ServerLevel level, Room room, boolean cut) {
        BoundingBox b = room.box;
        int depth = LockdownConfig.get(LockdownConfig.WALL_DEPTH), height = LockdownConfig.get(LockdownConfig.OPEN_HEIGHT);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        Block barrier = LockdownRegistry.LOCKDOWN_BARRIER.get();
        OpeningScan.Free free = (x, y, z) -> {                                              // loaded, no collision, not one of our own barriers
            m.set(x, y, z);
            if (!level.hasChunk(m.getX() >> 4, m.getZ() >> 4)) return false;
            BlockState s = level.getBlockState(m);
            return !s.is(barrier) && s.getCollisionShape(level, m).isEmpty();
        };
        return OpeningScan.scan(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ(), depth, height, MegaGrid.FLOOR_ROW, cut, free);
    }

    /**
     * Were the doorways of this room's sector cut to 7 x 7 through both wall layers ({@code gen.clearance7} was on when it was made)? Then the scan reads the wall one block closer to the
     * corner, fills the whole depth of the doorway and takes the limits of {@link OpeningScan#limitsFor}; otherwise (the switch was off, an older sector) the lockdown works exactly as before.
     */
    static boolean isCut(ServerLevel level, Sector sector) { return com.newuniverse.nusmp.labyrinth.mega.gen.ClearanceSettings.sectorCut(level.getServer(), sector.id); }

    // ---------------------------------------------------------------- a room that is sealed already
    /** Does the room hold a trial chamber's keystone? (Such a room locks itself; it is a template of its own.) */
    static boolean ownSeal(ServerLevel level, Room room) {
        BoundingBox b = room.box;
        for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++) {
            for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be.getType() == TrialRegistry.SEALED_ARCHWAY_BE.get() && room.contains(be.getBlockPos())) return true;
                }
            }
        }
        return false;
    }

    /** Is a mechanic that bars the room's doorways or cages its party running in this room now? */
    private static boolean mechanicSeals(Sector sector, Room room) {
        for (Instance in : MechanicsDirector.active()) {
            if (in.room != null && in.sector == sector && in.room.id == room.id && in.sealsPlayers()) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- the state of rooms
    /** Is this room locked now? A stale entry (its master is gone) is dropped. */
    static boolean isLocked(ServerLevel level, Sector s, Room r) {
        long key = LockdownMath.key(s.id, r.id);
        Long mp = LOCKED.get(key);
        if (mp == null) return false;
        BlockPos p = BlockPos.of(mp);
        if (level.isLoaded(p) && !(level.getBlockEntity(p) instanceof LockdownBarrierBlockEntity be && be.isMaster() && be.roomId() == r.id && be.sectorId() == s.id)) {
            LOCKED.remove(key);
            return false;
        }
        return true;
    }

    /** A master barrier came back with its chunk (or was just placed): the room is locked again after a restart. */
    static void noteLocked(int sectorId, int roomId, BlockPos master) { LOCKED.put(LockdownMath.key(sectorId, roomId), master.asLong()); }

    /** A room was cleared: it is no longer locked. */
    static void noteCleared(int sectorId, int roomId) { LOCKED.remove(LockdownMath.key(sectorId, roomId)); }

    /** A room opened without being cleared (abandoned, escaped, cancelled): it may lock again after the config's cooldown. */
    static void coolDown(int sectorId, int roomId, long now) {
        coolDownFor(sectorId, roomId, now, LockdownConfig.get(LockdownConfig.COOLDOWN_SECONDS) * 20L);
    }

    static void coolDownFor(int sectorId, int roomId, long now, long ticks) {
        long key = LockdownMath.key(sectorId, roomId);
        LOCKED.remove(key);
        COOLDOWN.put(key, now + ticks);
    }

    /** The position of the master barrier of a locked room, or null (for the self-test and the status command). */
    static BlockPos masterOf(int sectorId, int roomId) {
        Long l = LOCKED.get(LockdownMath.key(sectorId, roomId));
        return l == null ? null : BlockPos.of(l);
    }

    /** The reason a room cannot lock in this session, or null. */
    static String rejection(int sectorId, int roomId) { return REJECTED.get(LockdownMath.key(sectorId, roomId)); }

    /** The last verdict the director gave a room, or null. */
    static String lastVerdict(int sectorId, int roomId) { return LAST.get(LockdownMath.key(sectorId, roomId)); }

    public static int lockedCount() { return LOCKED.size(); }

    public static int armedTotal() { return armedTotal; }

    public static long passes() { return passes; }

    /** Forgets the session's refusals and cooldowns (the admin command, the self-test). */
    static void forgetRefusals() {
        REJECTED.clear();
        COOLDOWN.clear();
        LAST.clear();
    }

    /** Server stop. */
    public static void reset() {
        COOLDOWN.clear();
        REJECTED.clear();
        LOCKED.clear();
        LAST.clear();
        armedTotal = 0;
        passes = 0L;
    }

    // ---------------------------------------------------------------- the trial hook
    /**
     * A trial chamber's keystone has just sealed its doorways (a challenge room was entered the old way): tell the event bus, so a listener sees every challenge room whether
     * the trial machinery or the director closed it. Informational; never throws.
     */
    public static void trialSealed(ServerLevel level, BlockPos keystone, List<ServerPlayer> inside) {
        try {
            Sector s = MegaDungeon.sectorAt(level, keystone);
            Room r = s == null ? null : s.roomAt(keystone);
            NeoForge.EVENT_BUS.post(new RoomLockdownEvent(level, s, r, inside, Band.STANDARD, 1, RoomLockdownEvent.Source.TRIAL));
        } catch (RuntimeException e) {
            LOG.debug("[nusmp] The trial lockdown event failed: {}", e.toString());
        }
    }

    // ---------------------------------------------------------------- operators
    /** Opens every locked room the master of which is loaded (the admin command), whatever the escape rules say; returns how many. */
    public static int releaseAll(ServerLevel level) {
        int n = 0;
        for (long mp : new ArrayList<>(LOCKED.values())) {
            BlockPos p = BlockPos.of(mp);
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof LockdownBarrierBlockEntity be && be.isMaster()) {
                com.newuniverse.nusmp.labyrinth.mega.lock.LockBook.drop(level, be.lockKey());
                be.open(null, com.newuniverse.nusmp.labyrinth.mega.lock.EscapeRules.Why.NONE);
                n++;
            }
        }
        return n;
    }
}
