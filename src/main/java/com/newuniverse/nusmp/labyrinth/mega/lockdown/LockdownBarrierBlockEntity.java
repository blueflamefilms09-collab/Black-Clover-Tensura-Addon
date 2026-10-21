package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.LabyrinthConfig;
import com.newuniverse.nusmp.labyrinth.mega.enc.Band;
import com.newuniverse.nusmp.labyrinth.mega.lock.EscapeRules;
import com.newuniverse.nusmp.labyrinth.mega.lock.LockBook;
import com.newuniverse.nusmp.labyrinth.mega.lock.Lockable;
import com.newuniverse.nusmp.labyrinth.trial.TrialMath;
import com.newuniverse.nusmp.labyrinth.trial.TrialState;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Why: the brain of a locked room, and the heir of the trial chamber's ({@code SealedArchwayBlockEntity}): it is saved with its chunk, ticks only while its chunk
 * ticks, and keeps what the room did (the wave it is on, the monsters it spawned by UUID, the barrier cells it filled, when it closed). The difference is WHERE it
 * lives. A trial's keystone is a block in the template; a room that locks has nothing in its template, so the director places the first barrier and that block is the
 * brain (the MASTER); every other barrier of the room is a SLAVE that only remembers where the master is and what block it replaced. A slave whose master is gone
 * (a crash in the middle of arming, an operator's /setblock) takes itself away within five seconds, once the master's chunk is loaded, so a barrier can never be left
 * in the world with nobody to open it. Every block it places or removes runs inside {@link DungeonGuard#internal(Runnable)}, like the trial's. The life of a room is the
 * trial's three-state machine, {@link TrialState#step}, unchanged: the counting rule (a monster that cannot be found in a loaded chunk three checks in a row is dead,
 * one in an unloaded chunk is alive) and the abandoned-room rule (no player inside for the trial reset time: the monsters are discarded, the barriers fall, the room
 * stays UNcleared and waits for its cooldown) are the trial's, so the two seals behave alike. What a lockdown adds: waves (when one is dead the next appears after a pause,
 * until the last), the cleared mark ({@link LockdownData}, permanent) and the event ({@link RoomLockdownClearedEvent}), and the guarantee of the owner's escape rules:
 * the master is a {@link Lockable}, kept in the {@link LockBook} while closed, so the release rune, the timers and {@code /dungeon release} open it like any other seal;
 * escaping that way is not free (the room stays uncleared and the {@link DynamicAmbushDirector} is told). Server thread only.
 */
public final class LockdownBarrierBlockEntity extends BlockEntity implements Lockable {
    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final long NO_MASTER = Long.MIN_VALUE;
    private static final Random RND = new Random();

    /** A mob this room spawned: its UUID, the last block it was seen in, and how many checks in a row it has been missing. */
    private static final class Spawned {
        final UUID uuid;
        long lastPos;
        int strikes;

        Spawned(UUID uuid, long lastPos) {
            this.uuid = uuid;
            this.lastPos = lastPos;
        }
    }

    // ---------------------------------------------------------------- every barrier
    /** The block this barrier replaced (null = air); put back when the barrier goes. */
    private BlockState previous;
    /** Where the master is; the master's own position for the master itself. */
    private long masterPos = NO_MASTER;
    // ---------------------------------------------------------------- the master only
    private int sectorId = -1, roomId = -1, wave = 1, waves = 1;
    private Band band = Band.STANDARD;
    private int[] box = new int[6];
    private final List<Long> cells = new ArrayList<>();
    private final List<Spawned> spawned = new ArrayList<>();
    private long lastPlayerTick, activatedTick, lastHostileTick, nextWaveAt;
    private int lastAlive;

    public LockdownBarrierBlockEntity(BlockPos pos, BlockState state) { super(LockdownRegistry.LOCKDOWN_BARRIER_BE.get(), pos, state); }

    public boolean isMaster() { return masterPos == worldPosition.asLong(); }

    public int sectorId() { return sectorId; }

    public int roomId() { return roomId; }

    public int wave() { return wave; }

    public int waves() { return waves; }

    public int cellCount() { return cells.size(); }

    public int trackedMobs() { return spawned.size(); }

    public int lastAlive() { return lastAlive; }

    public boolean holds(BlockPos p) { return cells.contains(p.asLong()); }

    public TrialMath.Box box() { return new TrialMath.Box(box[0], box[1], box[2], box[3], box[4], box[5]); }

    // ---------------------------------------------------------------- how the director sets it up
    /** Makes this barrier a slave of the master at {@code master}, remembering the block it replaced. */
    void initSlave(long master, BlockState replaced) {
        this.masterPos = master;
        this.previous = replaced == null || replaced.isAir() ? null : replaced;
        setChanged();
    }

    /**
     * Makes this barrier the master of a room: the room's ids and box, the band and the number of waves, the cells (this one included), the monsters of the first wave and
     * the game time it closed.
     */
    void initMaster(int sectorId, int roomId, Band band, int waves, int[] box, List<BlockPos> cellList, List<Mob> first, BlockState replaced, long now) {
        this.masterPos = worldPosition.asLong();
        this.previous = replaced == null || replaced.isAir() ? null : replaced;
        this.sectorId = sectorId;
        this.roomId = roomId;
        this.band = band;
        this.waves = Math.max(1, waves);
        this.wave = 1;
        this.box = box.clone();
        this.cells.clear();
        for (BlockPos c : cellList) this.cells.add(c.asLong());
        this.spawned.clear();
        for (Mob m : first) this.spawned.add(new Spawned(m.getUUID(), m.blockPosition().asLong()));
        this.lastPlayerTick = now;
        this.activatedTick = now;
        this.lastHostileTick = now;
        this.lastAlive = spawned.size();
        this.nextWaveAt = 0L;
        setChanged();
    }

    /** A master that comes back with its chunk tells the director its room is locked (the director's table is in memory and does not survive a restart). */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel && isMaster() && sectorId >= 0) LockdownDirector.noteLocked(sectorId, roomId, worldPosition);
    }

    // ---------------------------------------------------------------- the ticker
    void serverTick(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        if (!isMaster()) {
            if (Math.floorMod(now + (pos.asLong() & 63), 100) == 0) slaveTick(sl, pos);
            return;
        }
        if (Math.floorMod(now + (pos.asLong() & 7), TrialMath.TICK_EVERY) != 0) return;
        masterTick(sl, now);
    }

    /** One look at the room right now, whatever the ten-tick rhythm says (the self-test drives a whole lockdown with this). Master only. */
    void tickNow(ServerLevel level) {
        if (isMaster()) masterTick(level, level.getGameTime());
    }

    /** A slave checks every five seconds that its master still holds it; if the master's chunk is loaded and does not, the slave goes. */
    private void slaveTick(ServerLevel level, BlockPos pos) {
        if (masterPos == NO_MASTER) {
            vanish(level, pos);
            return;
        }
        BlockPos m = BlockPos.of(masterPos);
        if (!level.isLoaded(m)) return;
        if (!(level.getBlockEntity(m) instanceof LockdownBarrierBlockEntity master) || !master.isMaster() || !master.holds(pos)) vanish(level, pos);
    }

    private void vanish(ServerLevel level, BlockPos pos) {
        BlockState back = previous == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : previous;
        DungeonGuard.internal(() -> level.setBlock(pos, back, Block.UPDATE_ALL));
    }

    private void masterTick(ServerLevel level, long now) {
        List<ServerPlayer> inside = playersInside(level);
        if (!inside.isEmpty()) lastPlayerTick = now;
        boolean gone = inside.isEmpty() && TrialMath.abandoned(now, lastPlayerTick, TrialMath.resetTicks(resetSeconds()));
        if (gone) {
            reset(level, now);
            return;
        }
        LockBook.keep(this, now);
        if (nextWaveAt > 0L) {                                                       // between two waves: the room waits, the next one comes on time
            if (now >= nextWaveAt) {
                nextWaveAt = 0L;
                spawnWave(level, inside, now);
                setChanged();
            }
            return;
        }
        int alive = countAlive(level);
        lastAlive = alive;
        if (alive > 0) lastHostileTick = now;
        TrialState.Step step = TrialState.step(TrialState.ACTIVE, !inside.isEmpty(), false, false, false, alive, now - lastPlayerTick, TrialMath.resetTicks(resetSeconds()));
        if (step.action() == TrialState.Action.CLEAR) {
            if (LockdownMath.moreWaves(wave, waves)) {
                wave++;
                nextWaveAt = now + LockdownConfig.get(LockdownConfig.WAVE_PAUSE_TICKS);
                setChanged();
                if (nextWaveAt <= now) {
                    nextWaveAt = 0L;
                    spawnWave(level, inside, now);
                }
            } else {
                clear(level, inside, now);
            }
        } else if (alive != spawned.size() || step.action() != TrialState.Action.NONE) {
            setChanged();
        }
    }

    private int resetSeconds() { return LabyrinthConfig.get(LabyrinthConfig.TRIAL_RESET_SECONDS); }

    // ---------------------------------------------------------------- who is inside
    /** The players the room holds: alive, in a mode the lockdown counts, feet inside the box. */
    private List<ServerPlayer> playersInside(ServerLevel level) {
        List<ServerPlayer> out = new ArrayList<>();
        TrialMath.Box b = box();
        for (ServerPlayer p : level.players()) {
            if (LockdownDirector.eligible(p) && b.contains(p.getX(), p.getY(), p.getZ())) out.add(p);
        }
        return out;
    }

    // ---------------------------------------------------------------- the waves
    private void spawnWave(ServerLevel level, List<ServerPlayer> inside, long now) {
        if (inside.isEmpty()) {
            nextWaveAt = now + 20L;                                                  // nobody to fight right now: try again in a second (the abandoned rule ends the wait)
            return;
        }
        LockdownWaves.Result r = LockdownWaves.spawn(level, sectorId, roomId, inside, band, RND);
        for (Mob m : r.mobs()) spawned.add(new Spawned(m.getUUID(), m.blockPosition().asLong()));
        lastAlive = countAlive(level);
        lastHostileTick = now;
        if (r.mobs().isEmpty()) LOG.warn("[nusmp] Lockdown of room {} in sector {}: wave {} of {} spawned nothing ({}): it counts as beaten", roomId, sectorId, wave, waves, r.text());
        Component msg = LockdownMsg.text(LockdownText.WAVE, wave, waves).withStyle(ChatFormatting.GOLD);
        LockdownMsg.tellAll(inside, msg);
        LockdownMsg.toast(inside, com.newuniverse.nusmp.labyrinth.mega.system.ToastKind.INFO, LockdownText.TOAST_WAVE, Integer.toString(wave), Integer.toString(waves));
        Vec3 c = centre();
        level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0f, 0.7f);
    }

    private int countAlive(ServerLevel level) {
        int alive = 0;
        Iterator<Spawned> it = spawned.iterator();
        while (it.hasNext()) {
            Spawned s = it.next();
            Entity e = level.getEntity(s.uuid);
            if (e != null) {
                if (e.isAlive()) {
                    alive++;
                    s.lastPos = e.blockPosition().asLong();
                    s.strikes = 0;
                } else it.remove();
                continue;
            }
            if (!level.isLoaded(BlockPos.of(s.lastPos))) {                           // out of view: alive until proven otherwise
                alive++;
                continue;
            }
            if (++s.strikes >= TrialMath.MISSING_STRIKES) it.remove();
            else alive++;
        }
        return alive;
    }

    private void discardMobs(ServerLevel level) {
        for (Spawned s : spawned) {
            Entity e = level.getEntity(s.uuid);
            if (e != null) e.discard();
        }
        spawned.clear();
    }

    // ---------------------------------------------------------------- the ends
    /** The last wave is dead: the barriers fall, the room is marked cleared for good, the event is posted. */
    private void clear(ServerLevel level, List<ServerPlayer> inside, long now) {
        int sid = sectorId, rid = roomId, w = waves;
        long sealed = Math.max(0L, now - activatedTick);
        Vec3 c = centre();
        LockBook.drop(level, lockKey());
        spawned.clear();
        dropCells(level);                                                            // this block goes with the others: nothing below may use the block entity's state
        LockdownData.get(level.getServer()).markCleared(sid, rid);
        LockdownDirector.noteCleared(sid, rid);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);
        Fx.spray(level, FxThemes.DUNGEON_SEAL, FxOld.END_ROD, c.x, c.y + 1.0, c.z, 40, 2.0, 1.0, 2.0, 0.05);
        LockdownMsg.tellAll(inside, LockdownMsg.text(LockdownText.CLEARED).withStyle(ChatFormatting.GREEN));
        LockdownMsg.toast(inside, com.newuniverse.nusmp.labyrinth.mega.system.ToastKind.SUCCESS, LockdownText.TOAST_CLEARED);
        NeoForge.EVENT_BUS.post(new RoomLockdownClearedEvent(level, sid, rid, w, sealed, inside));
    }

    /** An abandoned room: whatever it spawned that can still be reached is discarded, the doorways open, the room stays uncleared and waits for its cooldown. */
    private void reset(ServerLevel level, long now) {
        int sid = sectorId, rid = roomId;
        LockBook.drop(level, lockKey());
        discardMobs(level);
        dropCells(level);
        LockdownDirector.coolDown(sid, rid, now);
        LOG.info("[nusmp] Lockdown of room {} in sector {} was abandoned: the doorways open, the room is not cleared", rid, sid);
    }

    // ---------------------------------------------------------------- the escape rules: a Lockable
    @Override public String lockKey() { return "lockdown:" + masterPos; }

    @Override public EscapeRules.Kind lockKind() { return EscapeRules.Kind.SEAL; }

    @Override public String lockName() { return "the sealed room"; }

    @Override public ServerLevel lockLevel() { return getLevel() instanceof ServerLevel sl ? sl : null; }

    @Override
    public Vec3 runePos() { return centre(); }

    @Override
    public List<ServerPlayer> prisoners() {
        ServerLevel sl = lockLevel();
        return sl == null ? List.of() : playersInside(sl);
    }

    @Override
    public EscapeRules.Facts facts(long now) {
        long sealed = activatedTick == 0L ? 0L : Math.max(0L, now - activatedTick);
        return new EscapeRules.Facts(EscapeRules.Kind.SEAL, sealed, prisoners().size(), Math.max(0L, now - lastPlayerTick), lastAlive, Math.max(0L, now - lastHostileTick), false, true);
    }

    /** The room keeps the trial's reset time as its "nobody inside" time, so the escape rules never open it sooner than an abandoned trial would. */
    @Override
    public EscapeRules.Config config(EscapeRules.Config base) {
        return new EscapeRules.Config(resetSeconds(), base.runeAfterSeconds(), base.idleRuneSeconds(), base.rescueMinutes(), base.maxSealSeconds(), base.bossMaxSeconds(), base.unsolvableSeconds());
    }

    /**
     * Opens the room the way the escape rules open any seal: the monsters that can be reached are removed, the barriers fall, and the room stays UNcleared. Escaping is not
     * free: unless the room was simply empty, the ambush director is told, and the room waits for its cooldown before it may lock again.
     */
    @Override
    public void open(ServerPlayer by, EscapeRules.Why why) {
        ServerLevel sl = lockLevel();
        if (sl == null || !isMaster()) return;
        List<ServerPlayer> were = prisoners();
        long now = sl.getGameTime();
        int sid = sectorId, rid = roomId;
        discardMobs(sl);
        dropCells(sl);
        LockdownDirector.coolDown(sid, rid, now);
        boolean operator = by == null && why == EscapeRules.Why.NONE;                // /multiverse mega lockdown release or /dungeon release by an operator: no penalty
        if (why != EscapeRules.Why.ABSENT && !operator && !were.isEmpty()) {
            LockdownMsg.tellAll(were, LockdownMsg.text(LockdownText.ESCAPED).withStyle(ChatFormatting.YELLOW));
            DynamicAmbushDirector.noteEscape(sl, were);
        }
    }

    // ---------------------------------------------------------------- the cells
    private Vec3 centre() {
        double x = (box[0] + box[3] + 1) / 2.0, z = (box[2] + box[5] + 1) / 2.0;
        int y = Math.max(box[1] + 1, Math.min(box[1] + com.newuniverse.nusmp.labyrinth.mega.MegaGrid.FLOOR_ROW + 1, box[4] - 2));
        return new Vec3(x, y, z);
    }

    /**
     * Takes down every cell of the room whose chunk is loaded, giving each back the block it replaced; a cell in a chunk that is not loaded is left to its own slave
     * check, which removes it once the master's cell is gone. This block is one of the cells.
     */
    private void dropCells(ServerLevel level) {
        List<Long> all = new ArrayList<>(cells);
        cells.clear();
        Block barrier = LockdownRegistry.LOCKDOWN_BARRIER.get();
        DungeonGuard.internal(() -> {
            for (long c : all) {
                BlockPos b = BlockPos.of(c);
                if (!level.isLoaded(b)) continue;
                BlockState s = level.getBlockState(b);
                if (!s.is(barrier)) continue;
                BlockState back = level.getBlockEntity(b) instanceof LockdownBarrierBlockEntity be && be.previous != null ? be.previous : null;
                if (back == null) level.removeBlock(b, false);
                else level.setBlock(b, back, Block.UPDATE_ALL);
            }
        });
    }

    // ---------------------------------------------------------------- operator readout
    public Component describe() {
        String head = isMaster()
                ? "Lockdown barrier (master): sector " + sectorId + " room " + roomId + ", wave " + wave + " of " + waves + " (" + band.label() + "), " + cells.size() + " cells, "
                + spawned.size() + " mobs tracked, " + lastAlive + " alive at the last count" + (nextWaveAt > 0L ? ", next wave pending" : "")
                : "Lockdown barrier (slave of " + (masterPos == NO_MASTER ? "nobody" : BlockPos.of(masterPos).toShortString()) + ")";
        return Component.literal(head).withStyle(ChatFormatting.GOLD);
    }

    // ---------------------------------------------------------------- NBT
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (masterPos != NO_MASTER) tag.putLong("master", masterPos);
        if (previous != null) tag.put("previous", NbtUtils.writeBlockState(previous));
        if (!isMaster()) return;
        tag.putInt("sector", sectorId);
        tag.putInt("room", roomId);
        tag.putInt("wave", wave);
        tag.putInt("waves", waves);
        tag.putString("band", band.label());
        tag.putIntArray("box", box);
        long[] c = new long[cells.size()];
        for (int i = 0; i < c.length; i++) c[i] = cells.get(i);
        tag.putLongArray("cells", c);
        ListTag sl = new ListTag();
        for (Spawned s : spawned) {
            CompoundTag t = new CompoundTag();
            t.putUUID("uuid", s.uuid);
            t.putLong("pos", s.lastPos);
            t.putInt("strikes", s.strikes);
            sl.add(t);
        }
        tag.put("spawned", sl);
        tag.putLong("lastPlayerTick", lastPlayerTick);
        tag.putLong("activatedTick", activatedTick);
        tag.putLong("lastHostileTick", lastHostileTick);
        tag.putLong("nextWaveAt", nextWaveAt);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        masterPos = tag.contains("master", Tag.TAG_LONG) ? tag.getLong("master") : NO_MASTER;
        previous = null;
        if (tag.contains("previous", Tag.TAG_COMPOUND)) {
            try {
                BlockState s = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("previous"));
                previous = s.isAir() ? null : s;
            } catch (RuntimeException e) {
                LOG.warn("[nusmp] A lockdown barrier could not read the block it replaced: {}", e.toString());
            }
        }
        if (!isMaster()) return;
        sectorId = tag.getInt("sector");
        roomId = tag.getInt("room");
        wave = Math.max(1, tag.getInt("wave"));
        waves = Math.max(1, tag.getInt("waves"));
        Band b = Band.byName(tag.getString("band"));
        band = b == null || b == Band.NOTHING ? Band.STANDARD : b;
        int[] bx = tag.getIntArray("box");
        if (bx.length == 6) box = bx;
        cells.clear();
        for (long c : tag.getLongArray("cells")) cells.add(c);
        spawned.clear();
        ListTag sl = tag.getList("spawned", Tag.TAG_COMPOUND);
        for (int i = 0; i < sl.size(); i++) {
            CompoundTag t = sl.getCompound(i);
            if (!t.hasUUID("uuid")) continue;
            Spawned s = new Spawned(t.getUUID("uuid"), t.getLong("pos"));
            s.strikes = t.getInt("strikes");
            spawned.add(s);
        }
        lastPlayerTick = tag.getLong("lastPlayerTick");
        activatedTick = tag.getLong("activatedTick");
        lastHostileTick = tag.getLong("lastHostileTick");
        nextWaveAt = tag.getLong("nextWaveAt");
    }
}
