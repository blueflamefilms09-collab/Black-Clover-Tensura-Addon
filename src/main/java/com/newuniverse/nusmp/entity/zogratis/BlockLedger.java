package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Every block a Zogratis boss changes goes in here first, so the fight can be undone: when the boss dies, is removed, or the fight
 * is abandoned, {@link #restoreAll} and {@link #restoreStep} put the world back (inside {@link DungeonGuard#internal}, so even the
 * Living Dungeon accepts it). The ledger keeps the state from before the fight for each position and the state the boss put there;
 * a block someone else has changed since is left alone. It holds at most {@link #MAX_ENTRIES} positions and refuses new ones when
 * full, which bounds both the world damage and the size of the boss's save data (positions and states are saved as packed arrays).
 */
public final class BlockLedger {
    public static final int MAX_ENTRIES = 3072;

    private final LedgerQueue<Long, BlockState> queue = new LedgerQueue<>(MAX_ENTRIES);
    private final Supplier<ServerLevel> levelSource;

    /** A ledger that restores into the level 'levelSource' gives (the boss's), for the no-argument restore methods. */
    public BlockLedger(Supplier<ServerLevel> levelSource) { this.levelSource = levelSource; }

    /** A ledger whose restore methods take the level as an argument. */
    public BlockLedger() { this(null); }

    /** A block destroyed: 'old' was there, air is there now. False if the ledger is full (do not make the change). */
    public boolean record(BlockPos pos, BlockState old) { return record(pos, old, Blocks.AIR.defaultBlockState()); }

    /** A block replaced: 'old' was there, 'placed' is there now. False if the ledger is full. */
    public boolean record(BlockPos pos, BlockState old, BlockState placed) { return queue.record(pos.asLong(), old, placed); }

    public int size() { return queue.size(); }
    public boolean isEmpty() { return queue.isEmpty(); }
    public boolean isFull() { return queue.isFull(); }
    public boolean has(BlockPos pos) { return queue.contains(pos.asLong()); }

    /** Puts back up to 'budget' of the oldest changes whose chunks are loaded. Returns how many blocks were restored. */
    public int restoreStep(ServerLevel level, int budget) { return restore(level, budget, false); }

    /** Puts back everything now (the boss is gone). Returns how many blocks were restored. */
    public int restoreAll(ServerLevel level) { return restore(level, Integer.MAX_VALUE, true); }

    /** {@link #restoreStep(ServerLevel, int)} in the ledger's own level. */
    public int restoreStep(int budget) {
        ServerLevel level = levelSource == null ? null : levelSource.get();
        return level == null ? 0 : restore(level, budget, false);
    }

    /** {@link #restoreAll(ServerLevel)} in the ledger's own level. */
    public int restoreAll() {
        ServerLevel level = levelSource == null ? null : levelSource.get();
        return level == null ? 0 : restore(level, Integer.MAX_VALUE, true);
    }

    private int restore(ServerLevel level, int budget, boolean force) {
        if (queue.isEmpty()) return 0;
        List<LedgerQueue.Entry<Long, BlockState>> batch = queue.take(budget);
        batch.sort(Comparator.comparingInt(e -> BlockPos.getY(e.key())));        // bottom up, so supported blocks find their support
        int[] restored = {0};
        DungeonGuard.internal(() -> {
            for (LedgerQueue.Entry<Long, BlockState> e : batch) {
                BlockPos p = BlockPos.of(e.key());
                if (!force && !level.isLoaded(p)) { queue.putBack(e); continue; }
                BlockState now = level.getBlockState(p), placed = e.placed();
                if (now == e.old()) continue;
                boolean ours = placed == null || now.is(placed.getBlock()) || (placed.isAir() && (now.isAir() || now.canBeReplaced()));
                if (!ours) continue;                                              // someone built or broke here since: leave it
                level.setBlock(p, e.old(), 3);
                restored[0]++;
            }
        });
        return restored[0];
    }

    // ---------------------------------------------------------------- save data
    public void save(CompoundTag tag) {
        List<LedgerQueue.Entry<Long, BlockState>> entries = queue.snapshot();
        int n = entries.size();
        long[] pos = new long[n];
        int[] old = new int[n], placed = new int[n];
        Map<BlockState, Integer> index = new HashMap<>();
        ListTag palette = new ListTag();
        for (int i = 0; i < n; i++) {
            LedgerQueue.Entry<Long, BlockState> e = entries.get(i);
            pos[i] = e.key();
            old[i] = paletteIndex(index, palette, e.old());
            placed[i] = paletteIndex(index, palette, e.placed());
        }
        tag.put("Pal", palette);
        tag.putLongArray("Pos", pos);
        tag.putIntArray("Old", old);
        tag.putIntArray("New", placed);
    }

    private static int paletteIndex(Map<BlockState, Integer> index, ListTag palette, BlockState state) {
        BlockState s = state == null ? Blocks.AIR.defaultBlockState() : state;
        Integer known = index.get(s);
        if (known != null) return known;
        palette.add(NbtUtils.writeBlockState(s));
        index.put(s, palette.size() - 1);
        return palette.size() - 1;
    }

    public void load(CompoundTag tag, HolderGetter<Block> blocks) {
        queue.clear();
        ListTag palette = tag.getList("Pal", Tag.TAG_COMPOUND);
        BlockState[] states = new BlockState[palette.size()];
        for (int i = 0; i < states.length; i++) states[i] = NbtUtils.readBlockState(blocks, palette.getCompound(i));
        long[] pos = tag.getLongArray("Pos");
        int[] old = tag.getIntArray("Old"), placed = tag.getIntArray("New");
        int n = Math.min(pos.length, Math.min(old.length, placed.length));
        for (int i = 0; i < n; i++) {
            if (old[i] < 0 || old[i] >= states.length) continue;
            BlockState was = states[old[i]];
            BlockState now = placed[i] >= 0 && placed[i] < states.length ? states[placed[i]] : Blocks.AIR.defaultBlockState();
            queue.record(pos[i], was, now);
        }
    }
}
