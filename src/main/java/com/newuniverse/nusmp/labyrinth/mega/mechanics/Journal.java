package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: "always restore blocks". Every block a mechanic changes goes through a journal that remembers what stood there first, so ending the mechanic (it was beaten, the floor was
 * left, the server stops) puts the dungeon back exactly. The dungeon's blocks are unbreakable by guard, so every write runs inside {@link DungeonGuard#internal}; the journal
 * refuses what must never be touched (unbreakable blocks such as bedrock, and anything with a block entity: chests, spawners, the Gatekeeper) and stops at {@link #MAX} entries
 * so a bad number in a datapack cannot rewrite a hall. The first state seen at a position is the one restored, however often it was changed meanwhile. A journal can be saved
 * and loaded (the SavedData mirror in {@link MechanicsData}) so a crash in the middle of a flood cannot leave the water behind.
 */
public final class Journal {
    public static final int MAX = 4096;

    private final ServerLevel level;
    private final Map<Long, BlockState> before = new LinkedHashMap<>();

    public Journal(ServerLevel level) { this.level = level; }

    public int size() { return before.size(); }

    public boolean isEmpty() { return before.isEmpty(); }

    /** Sets a block, remembering the old state the first time. False when the position may not be touched (not loaded, a block entity, unbreakable, journal full). */
    public boolean set(BlockPos pos, BlockState state) {
        if (!level.hasChunkAt(pos)) return false;
        long key = pos.asLong();
        BlockState old = level.getBlockState(pos);
        if (!before.containsKey(key)) {
            if (before.size() >= MAX) return false;
            if (old.getDestroySpeed(level, pos) < 0.0f || level.getBlockEntity(pos) != null) return false;
            before.put(key, old);
        }
        if (old == state) return true;
        final BlockPos at = pos.immutable();
        DungeonGuard.internal(() -> level.setBlock(at, state, 3));
        return true;
    }

    /** Sets a block only where the position is empty air (or already ours). The usual way to place a prop or a wall. */
    public boolean placeInAir(BlockPos pos, BlockState state) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState cur = level.getBlockState(pos);
        if (!cur.isAir() && !before.containsKey(pos.asLong())) return false;
        return set(pos, state);
    }

    /** Puts every position back, newest first, loading chunks that were unloaded meanwhile. Empties the journal. */
    public void restore() {
        List<Map.Entry<Long, BlockState>> list = new ArrayList<>(before.entrySet());
        before.clear();
        for (int i = list.size() - 1; i >= 0; i--) {
            BlockPos pos = BlockPos.of(list.get(i).getKey());
            BlockState old = list.get(i).getValue();
            try {
                if (!level.hasChunkAt(pos)) level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                if (level.getBlockState(pos) != old) DungeonGuard.internal(() -> level.setBlock(pos, old, 3));
            } catch (RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().warn("[nusmp] mechanics: could not restore {}: {}", pos.toShortString(), e.toString());
            }
        }
    }

    /** Puts one position back and forgets it (a bridge block that has faded). */
    public void restoreOne(BlockPos pos) {
        BlockState old = before.remove(pos.asLong());
        if (old == null || !level.hasChunkAt(pos)) return;
        if (level.getBlockState(pos) != old) DungeonGuard.internal(() -> level.setBlock(pos, old, 3));
    }

    /** Was this position changed by this journal? */
    public boolean touched(BlockPos pos) { return before.containsKey(pos.asLong()); }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        ListTag l = new ListTag();
        for (Map.Entry<Long, BlockState> e : before.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putLong("p", e.getKey());
            c.put("s", NbtUtils.writeBlockState(e.getValue()));
            l.add(c);
        }
        t.put("b", l);
        return t;
    }

    /** Reads a saved journal. Entries whose block no longer exists are dropped. */
    public static Journal load(ServerLevel level, CompoundTag t) {
        Journal j = new Journal(level);
        ListTag l = t.getList("b", Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size(); i++) {
            CompoundTag c = l.getCompound(i);
            try {
                j.before.put(c.getLong("p"), NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), c.getCompound("s")));
            } catch (RuntimeException e) {
                // an unknown block: nothing to restore for this entry
            }
        }
        return j;
    }
}
