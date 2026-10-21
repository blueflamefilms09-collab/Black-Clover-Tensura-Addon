package com.newuniverse.nusmp.labyrinth.trial;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.LabyrinthConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: the brain of a trial chamber, saved with its chunk. It keeps what the template wrote (the box, the mob list, the reset time), what
 * the trial did (the mobs it spawned by UUID, the barrier cells it filled, the state) and nothing about WHERE the doors and spawn points
 * are: those it finds by scanning its box for {@link WardRuneBlock} and {@link SpawnGlyphBlock} tiles, because a Jigsaw piece is rotated
 * at placement and a stored offset would point the wrong way three times out of four, while a floor tile rotates with the piece. Every
 * block it places or removes runs inside {@link DungeonGuard#internal(Runnable)}: the Labyrinth is held by the guard (nothing may be set
 * to air there), and the guard lets the mod's own builders through only inside that scope. Mobs are spawned with
 * {@link MobSpawnType#STRUCTURE} and {@link Mob#setPersistenceRequired()}: persistent mobs never despawn when the party backs off, and
 * they are not counted against the natural spawn caps, so a full chamber never starves the rest of the floor of spawns. The counting
 * rule treats a mob that cannot be found while its last chunk is loaded as dead after three misses (entities load a few ticks after their
 * chunk) and a mob in an unloaded chunk as alive, so a trial is never cleared by a mob that merely wandered out of view. The reset rule
 * (no player inside for {@code reset} seconds while ACTIVE: discard what is left, drop the barriers, back to IDLE) means an abandoned trial
 * can never soft-lock the room, and the doorway rule (no activation while a player's hitbox touches a cell a barrier would fill) means
 * nobody is ever sealed inside a wall. A chamber with runes also waits until some player has been SEEN in a rune column since the last
 * IDLE (the {@code armed} flag, saved, cleared on clear and reset): the Spire's arrival stairs come up through the middle of a box, and a
 * party must walk through a door, not merely be inside, to start a trial; that check runs every tick while IDLE because a sprinting player
 * crosses a one-block doorway between two ten-tick checks. CLEARED is permanent and saved. Everything that is only arithmetic lives in {@link TrialMath}
 * and {@link TrialState}, which are tested without a level.
 */
public final class SealedArchwayBlockEntity extends BlockEntity implements com.newuniverse.nusmp.labyrinth.mega.lock.Lockable {
    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** Markers are rescanned at most every ten seconds while a player stands inside an idle box: the piece is static, but an admin may edit it. */
    private static final int RESCAN_TICKS = 200;
    private static final String MSG_SEALED = "message.nusmp.trial.sealed", MSG_FADES = "message.nusmp.trial.fades";
    private static final Set<String> WARNED = new HashSet<>();

    /** One line of the template's {@code mobs} list: an entity id, how many for one player, and optional NBT merged onto each. */
    static final class MobEntry {
        final String id;
        final int count;
        final CompoundTag nbt;

        MobEntry(String id, int count, CompoundTag nbt) {
            this.id = id;
            this.count = count;
            this.nbt = nbt;
        }
    }

    /** A mob this trial spawned: its UUID, the last block it was seen in, and how many checks in a row it has been missing. */
    static final class Spawned {
        final UUID uuid;
        long lastPos;
        int strikes;

        Spawned(UUID uuid, long lastPos) {
            this.uuid = uuid;
            this.lastPos = lastPos;
        }
    }

    // ---------------------------------------------------------------- what the template wrote
    private int radius = TrialMath.DEFAULT_RADIUS, height = TrialMath.DEFAULT_HEIGHT;
    /** Seconds before an abandoned trial resets; 0 (or absent) means the server config's {@code trialResetSeconds}. */
    private int reset = 0;
    private final List<MobEntry> mobs = new ArrayList<>();
    // ---------------------------------------------------------------- what the trial did
    private TrialState state = TrialState.IDLE;
    private final List<Spawned> spawned = new ArrayList<>();
    private final List<Long> barriers = new ArrayList<>();
    private long lastPlayerTick;
    /** Has a player been seen standing in a rune column since the last IDLE? Only a chamber with runes needs it to activate. */
    private boolean armed;
    /** 1.94.0 (escape rules): the game tick the trial sealed the doorways (0 = not known, e.g. a chamber saved before the rules; its clock then starts when it is first seen sealed). */
    private long activatedTick;
    /** 1.94.0: how many of its mobs the last count found alive (the escape rules' "hostiles"). */
    private int lastAlive;
    // ---------------------------------------------------------------- the marker scan (not saved: found again after a reload)
    private List<BlockPos> runes, glyphs;
    private long scanTick = Long.MIN_VALUE;

    public SealedArchwayBlockEntity(BlockPos pos, BlockState state) { super(TrialRegistry.SEALED_ARCHWAY_BE.get(), pos, state); }

    public TrialState state() { return state; }

    public int resetSeconds() { return reset > 0 ? reset : LabyrinthConfig.get(LabyrinthConfig.TRIAL_RESET_SECONDS); }

    public TrialMath.Box box() { return TrialMath.box(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), radius, height); }

    // ---------------------------------------------------------------- the ticker
    /**
     * Called every tick by the block's server ticker. The cheap part (who is inside, is someone in a doorway: this arms the trial) runs every
     * tick while IDLE; the state step and the mob count run every tenth tick, spread over the cycle by position.
     */
    void serverTick(Level level, BlockPos pos, BlockState blockState) {
        if (!(level instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        boolean due = TrialMath.dueThisTick(now, pos.asLong());
        if (state == TrialState.IDLE || due) tick(sl, pos, blockState, now, due);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState blockState, long now, boolean due) {
        TrialMath.Box box = box();
        List<ServerPlayer> inside = playersInside(level, box, false);
        if (!inside.isEmpty()) lastPlayerTick = now;
        boolean doorway = false;
        if (state == TrialState.IDLE && !inside.isEmpty()) {
            scan(level, box, now);
            doorway = anyInDoorway(level, inside);
            if (doorway && !armed) {
                armed = true;
                setChanged();
            }
        }
        if (!due) return;
        boolean hasRunes = runes != null && !runes.isEmpty();
        int alive = state == TrialState.ACTIVE ? countAlive(level) : 0;
        TrialState.Step step = TrialState.step(state, !inside.isEmpty(), doorway, hasRunes, armed, alive, now - lastPlayerTick, TrialMath.resetTicks(resetSeconds()));
        switch (step.action()) {
            case ACTIVATE -> activate(level, pos, box, inside, now);
            case CLEAR -> clear(level, pos, box);
            case RESET -> reset(level, pos);
            case NONE -> {}
        }
        if (step.next() != state) {
            state = step.next();
            setPhase(level, pos, SealedArchwayBlock.Phase.of(state));
            setChanged();
        } else if (step.action() != TrialState.Action.NONE || state == TrialState.ACTIVE) {
            setChanged();                                                           // the tracked mobs and barrier cells changed
        }
        if (state == TrialState.ACTIVE) {                                           // 1.94.0: a sealed chamber is in the escape book while it is closed (timers, release rune, /dungeon release)
            lastAlive = alive;
            if (activatedTick == 0L) activatedTick = now;
            if (com.newuniverse.nusmp.labyrinth.mega.lock.LockConfig.get(com.newuniverse.nusmp.labyrinth.mega.lock.LockConfig.TRIAL_RULES)) {
                com.newuniverse.nusmp.labyrinth.mega.lock.LockBook.keep(this, now);
            }
        }
    }

    // ---------------------------------------------------------------- 1.94.0: the escape rules (labyrinth/mega/lock): a sealed chamber always has a way out
    @Override public String lockKey() { return "trial:" + worldPosition.asLong(); }

    @Override public com.newuniverse.nusmp.labyrinth.mega.lock.EscapeRules.Kind lockKind() { return com.newuniverse.nusmp.labyrinth.mega.lock.EscapeRules.Kind.TRIAL; }

    @Override public String lockName() { return "the trial chamber"; }

    @Override public ServerLevel lockLevel() { return getLevel() instanceof ServerLevel sl ? sl : null; }

    @Override
    public net.minecraft.world.phys.Vec3 runePos() {
        ServerLevel sl = lockLevel();
        if (sl == null) return net.minecraft.world.phys.Vec3.atBottomCenterOf(worldPosition);
        List<BlockPos> spots = fallbackSpots(sl, worldPosition, box());
        return net.minecraft.world.phys.Vec3.atBottomCenterOf(spots.get(0).above());
    }

    @Override
    public List<ServerPlayer> prisoners() {
        ServerLevel sl = lockLevel();
        return sl == null ? List.of() : playersInside(sl, box(), false);
    }

    @Override
    public com.newuniverse.nusmp.labyrinth.mega.lock.EscapeRules.Facts facts(long now) {
        long sealed = activatedTick == 0L ? 0L : Math.max(0L, now - activatedTick);
        return new com.newuniverse.nusmp.labyrinth.mega.lock.EscapeRules.Facts(com.newuniverse.nusmp.labyrinth.mega.lock.EscapeRules.Kind.TRIAL, sealed, prisoners().size(),
                Math.max(0L, now - lastPlayerTick), lastAlive, 0L, false, true);
    }

    /** The chamber keeps its own reset time as the "nobody inside" time, so the escape rules never open a trial sooner than the template says. */
    @Override
    public com.newuniverse.nusmp.labyrinth.mega.lock.EscapeRules.Config config(com.newuniverse.nusmp.labyrinth.mega.lock.EscapeRules.Config base) {
        return new com.newuniverse.nusmp.labyrinth.mega.lock.EscapeRules.Config(resetSeconds(), base.runeAfterSeconds(), base.idleRuneSeconds(), base.rescueMinutes(),
                base.maxSealSeconds(), base.bossMaxSeconds(), base.unsolvableSeconds());
    }

    /** Opens the chamber the way an abandoned trial opens: the mobs that can be reached are removed, the barriers fall, the trial goes back to IDLE for the next party. */
    @Override
    public void open(ServerPlayer by, com.newuniverse.nusmp.labyrinth.mega.lock.EscapeRules.Why why) {
        ServerLevel sl = lockLevel();
        if (sl == null || state != TrialState.ACTIVE) return;
        reset(sl, worldPosition);
        state = TrialState.IDLE;
        activatedTick = 0L;
        setPhase(sl, worldPosition, SealedArchwayBlock.Phase.of(state));
        setChanged();
    }

    // ---------------------------------------------------------------- who is inside
    /** Survival and adventure players whose feet are inside the box; with {@code all}, creative and spectator players too (for messages). */
    private static List<ServerPlayer> playersInside(ServerLevel level, TrialMath.Box box, boolean all) {
        List<ServerPlayer> out = new ArrayList<>();
        for (Player pl : level.players()) {
            if (!(pl instanceof ServerPlayer p) || !p.isAlive() || (!all && (p.isCreative() || p.isSpectator()))) continue;
            if (box.contains(p.getX(), p.getY(), p.getZ())) out.add(p);
        }
        return out;
    }

    /** Is any of these players' hitboxes touching a cell a barrier column would fill? */
    private boolean anyInDoorway(ServerLevel level, List<ServerPlayer> inside) {
        if (runes == null) return false;
        for (BlockPos r : runes) {
            int h = TrialMath.columnHeight(airAbove(level, r));
            for (ServerPlayer p : inside) {
                AABB bb = p.getBoundingBox();
                if (TrialMath.overlapsColumn(r.getX(), r.getY(), r.getZ(), h, bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ)) return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- the marker scan
    private void scan(ServerLevel level, TrialMath.Box box, long now) {
        if (runes != null && now - scanTick < RESCAN_TICKS) return;
        List<BlockPos> r = new ArrayList<>(), g = new ArrayList<>();
        Block rune = TrialRegistry.WARD_RUNE.get(), glyph = TrialRegistry.SPAWN_GLYPH.get();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                for (int y = box.minY(); y <= box.maxY(); y++) {
                    BlockState s = level.getBlockState(m.set(x, y, z));
                    if (s.is(rune)) r.add(m.immutable());
                    else if (s.is(glyph)) g.add(m.immutable());
                }
            }
        }
        runes = r;
        glyphs = g;
        scanTick = now;
    }

    /** How many cells straight over a rune are air, up to the column cap. */
    private static int airAbove(ServerLevel level, BlockPos rune) {
        int n = 0;
        while (n < TrialMath.MAX_COLUMN && level.getBlockState(rune.above(n + 1)).isAir()) n++;
        return n;
    }

    // ---------------------------------------------------------------- the transitions
    private void activate(ServerLevel level, BlockPos pos, TrialMath.Box box, List<ServerPlayer> inside, long now) {
        scan(level, box, now);
        List<BlockPos> cells = new ArrayList<>();
        for (BlockPos r : runes) {
            int h = TrialMath.columnHeight(airAbove(level, r));
            for (int i = 1; i <= h; i++) cells.add(r.above(i));
        }
        BlockState barrier = TrialRegistry.MAGIC_BARRIER.get().defaultBlockState();
        DungeonGuard.internal(() -> {
            for (BlockPos c : cells) if (level.getBlockState(c).isAir() && level.setBlock(c, barrier, Block.UPDATE_ALL)) barriers.add(c.asLong());
        });
        List<BlockPos> spots = glyphs.isEmpty() ? fallbackSpots(level, pos, box) : glyphs;
        int slot = 0;
        for (MobEntry m : mobs) {
            int n = TrialMath.scaledCount(m.count, inside.size());
            for (int i = 0; i < n; i++) spawnOne(level, m, spots.get(slot++ % spots.size()).above());
        }
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 0.8f);
        Fx.spray(level, FxThemes.DUNGEON_TRIAL, FxOld.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 60, 1.0, 1.0, 1.0, 0.5);
        for (long c : barriers) {
            BlockPos b = BlockPos.of(c);
            Fx.spray(level, FxThemes.DUNGEON_TRIAL, FxOld.END_ROD, b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.02);
        }
        Component msg = Component.translatable(MSG_SEALED).withStyle(ChatFormatting.AQUA);
        for (ServerPlayer p : inside) p.displayClientMessage(msg, false);
        com.newuniverse.nusmp.labyrinth.mega.lockdown.LockdownDirector.trialSealed(level, pos, inside);          // 1.107.0: the event bus sees the trial hall as an entered challenge room too
        lastPlayerTick = now;
        activatedTick = now;
        lastAlive = spawned.size();
        if (spawned.isEmpty()) LOG.warn("[nusmp] Sealed Archway at {} activated but spawned nothing (mob list: {} entries): it will clear at once", pos, mobs.size());
    }

    /** One mob: created and finalized as a structure spawn, the template's extra NBT merged on top, tagged, made persistent, added. */
    private void spawnOne(ServerLevel level, MobEntry entry, BlockPos at) {
        Optional<EntityType<?>> type = EntityType.byString(entry.id);
        if (type.isEmpty()) {
            if (WARNED.add(entry.id)) LOG.warn("[nusmp] Sealed Archway: unknown entity id '{}' in a trial's mob list", entry.id);
            return;
        }
        Entity e = type.get().create(level, null, at, MobSpawnType.STRUCTURE, false, false);
        if (e == null) return;
        if (entry.nbt != null && !entry.nbt.isEmpty()) {
            CompoundTag full = e.saveWithoutId(new CompoundTag());
            CompoundTag extra = entry.nbt.copy();
            extra.remove("UUID");                                                   // a fixed id in a template would collide on the second spawn
            extra.remove("Pos");                                                    // the glyph decides where it stands
            full.merge(extra);
            e.load(full);
        }
        e.addTag(TrialRegistry.MOB_TAG);
        if (e instanceof Mob mob) mob.setPersistenceRequired();
        if (level.addFreshEntity(e)) spawned.add(new Spawned(e.getUUID(), at.asLong()));
    }

    /** Where mobs stand in a chamber with no glyph: the first standable cell found going down from the keystone at eight offsets round it. */
    private static List<BlockPos> fallbackSpots(ServerLevel level, BlockPos pos, TrialMath.Box box) {
        List<BlockPos> out = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int[] o : TrialMath.FALLBACK_OFFSETS) {
            int x = pos.getX() + o[0], z = pos.getZ() + o[1];
            for (int y = pos.getY(); y > box.minY(); y--) {
                if (level.getBlockState(m.set(x, y, z)).isAir() && level.getBlockState(m.set(x, y + 1, z)).isAir() && !level.getBlockState(m.set(x, y - 1, z)).isAir()) {
                    out.add(new BlockPos(x, y - 1, z));
                    break;
                }
            }
        }
        if (out.isEmpty()) out.add(pos);                                            // last resort: on the keystone itself
        return out;
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
            if (!level.isLoaded(BlockPos.of(s.lastPos))) {                          // out of view: it is alive until proven otherwise
                alive++;
                continue;
            }
            if (++s.strikes >= TrialMath.MISSING_STRIKES) it.remove();
            else alive++;
        }
        return alive;
    }

    private void clear(ServerLevel level, BlockPos pos, TrialMath.Box box) {
        dropBarriers(level);
        spawned.clear();
        armed = false;
        activatedTick = 0L;
        com.newuniverse.nusmp.labyrinth.mega.lock.LockBook.drop(level, lockKey());
        level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);
        Fx.spray(level, FxThemes.DUNGEON_TRIAL, FxOld.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 40, 1.5, 1.0, 1.5, 0.05);
        Component msg = Component.translatable(MSG_FADES).withStyle(ChatFormatting.GRAY);
        for (ServerPlayer p : playersInside(level, box, true)) p.displayClientMessage(msg, false);
    }

    /** An abandoned trial: whatever it spawned that can still be reached is discarded, the doors open, and it waits for the next party. */
    private void reset(ServerLevel level, BlockPos pos) {
        for (Spawned s : spawned) {
            Entity e = level.getEntity(s.uuid);
            if (e != null) e.discard();
        }
        spawned.clear();
        armed = false;
        dropBarriers(level);
        com.newuniverse.nusmp.labyrinth.mega.lock.LockBook.drop(level, lockKey());
        level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.6f, 0.7f);
    }

    /** Takes down the cells this trial filled, and only those that still hold a barrier (a cell something else changed is left alone). */
    private void dropBarriers(ServerLevel level) {
        Block barrier = TrialRegistry.MAGIC_BARRIER.get();
        List<Long> cells = new ArrayList<>(barriers);
        barriers.clear();
        DungeonGuard.internal(() -> {
            for (long c : cells) {
                BlockPos b = BlockPos.of(c);
                if (level.getBlockState(b).is(barrier)) level.removeBlock(b, false);
            }
        });
    }

    /** Mirrors the state into the block's {@code phase} property (same block, so the block entity stays; the chunk packet carries it to clients). */
    private void setPhase(ServerLevel level, BlockPos pos, SealedArchwayBlock.Phase phase) {
        BlockState bs = level.getBlockState(pos);
        if (!(bs.getBlock() instanceof SealedArchwayBlock) || bs.getValue(SealedArchwayBlock.PHASE) == phase) return;
        DungeonGuard.internal(() -> level.setBlock(pos, bs.setValue(SealedArchwayBlock.PHASE, phase), Block.UPDATE_ALL));
    }

    // ---------------------------------------------------------------- operator readout
    public Component describe() {
        StringBuilder sb = new StringBuilder("Sealed Archway ").append(state).append(": radius ").append(radius).append(", height ").append(height)
                .append(", reset ").append(resetSeconds()).append("s, mobs [");
        for (int i = 0; i < mobs.size(); i++) {
            MobEntry m = mobs.get(i);
            if (i > 0) sb.append(", ");
            sb.append(m.count).append("x ").append(m.id);
            if (m.nbt != null && !m.nbt.isEmpty()) sb.append("+nbt");
        }
        sb.append("], armed ").append(armed).append(", tracked ").append(spawned.size()).append(", barrier cells ").append(barriers.size())
                .append(", runes ").append(runes == null ? "?" : runes.size()).append(", glyphs ").append(glyphs == null ? "?" : glyphs.size());
        return Component.literal(sb.toString()).withStyle(ChatFormatting.AQUA);
    }

    // ---------------------------------------------------------------- NBT
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("radius", radius);
        tag.putInt("height", height);
        tag.putInt("reset", reset);
        ListTag ml = new ListTag();
        for (MobEntry m : mobs) {
            CompoundTag t = new CompoundTag();
            t.putString("id", m.id);
            t.putInt("count", m.count);
            if (m.nbt != null && !m.nbt.isEmpty()) t.put("nbt", m.nbt.copy());
            ml.add(t);
        }
        tag.put("mobs", ml);
        tag.putString("state", state.name());
        ListTag sl = new ListTag();
        for (Spawned s : spawned) {
            CompoundTag t = new CompoundTag();
            t.putUUID("uuid", s.uuid);
            t.putLong("pos", s.lastPos);
            t.putInt("strikes", s.strikes);
            sl.add(t);
        }
        tag.put("spawned", sl);
        long[] b = new long[barriers.size()];
        for (int i = 0; i < b.length; i++) b[i] = barriers.get(i);
        tag.putLongArray("barriers", b);
        tag.putLong("lastPlayerTick", lastPlayerTick);
        tag.putBoolean("armed", armed);
        tag.putLong("activatedTick", activatedTick);
    }

    /** Reads a full save, a template's tag (settings only) or a client update tag (state only): every key is optional. */
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("radius", Tag.TAG_ANY_NUMERIC)) radius = TrialMath.clampRadius(tag.getInt("radius"));
        if (tag.contains("height", Tag.TAG_ANY_NUMERIC)) height = TrialMath.clampHeight(tag.getInt("height"));
        if (tag.contains("reset", Tag.TAG_ANY_NUMERIC)) reset = Math.max(0, tag.getInt("reset"));
        if (tag.contains("mobs", Tag.TAG_LIST)) {
            mobs.clear();
            ListTag ml = tag.getList("mobs", Tag.TAG_COMPOUND);
            for (int i = 0; i < ml.size(); i++) {
                CompoundTag t = ml.getCompound(i);
                String id = t.getString("id");
                if (id.isEmpty()) continue;
                int count = t.contains("count", Tag.TAG_ANY_NUMERIC) ? t.getInt("count") : 1;
                CompoundTag nbt = t.contains("nbt", Tag.TAG_COMPOUND) ? t.getCompound("nbt").copy() : null;
                mobs.add(new MobEntry(id, count, nbt));
            }
        }
        if (tag.contains("state", Tag.TAG_STRING)) state = TrialState.byName(tag.getString("state"));
        if (tag.contains("spawned", Tag.TAG_LIST)) {
            spawned.clear();
            ListTag sl = tag.getList("spawned", Tag.TAG_COMPOUND);
            for (int i = 0; i < sl.size(); i++) {
                CompoundTag t = sl.getCompound(i);
                if (!t.hasUUID("uuid")) continue;
                Spawned s = new Spawned(t.getUUID("uuid"), t.getLong("pos"));
                s.strikes = t.getInt("strikes");
                spawned.add(s);
            }
        }
        if (tag.contains("barriers", Tag.TAG_LONG_ARRAY)) {
            barriers.clear();
            for (long c : tag.getLongArray("barriers")) barriers.add(c);
        }
        if (tag.contains("lastPlayerTick", Tag.TAG_ANY_NUMERIC)) lastPlayerTick = tag.getLong("lastPlayerTick");
        if (tag.contains("armed", Tag.TAG_ANY_NUMERIC)) armed = tag.getBoolean("armed");
        if (tag.contains("activatedTick", Tag.TAG_ANY_NUMERIC)) activatedTick = tag.getLong("activatedTick");
    }

    /** What a client gets: the state and how many mobs are still tracked (for a renderer or a HUD; the face itself is the block state). */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        t.putString("state", state.name());
        t.putInt("tracked", spawned.size());
        t.putBoolean("armed", armed);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
