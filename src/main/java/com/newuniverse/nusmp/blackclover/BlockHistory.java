package com.newuniverse.nusmp.blackclover;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Remembers recently destroyed blocks (broken or exploded) so Time Reversal can restore them. */
public final class BlockHistory {
    private record Entry(BlockPos pos, BlockState state, long time) {}

    private static final Map<ResourceKey<Level>, ArrayDeque<Entry>> LOG = new HashMap<>();
    private static final int MAX = 8192;
    private static final long KEEP_TICKS = 2400;

    private BlockHistory() {}

    private static void record(Level level, BlockPos pos, BlockState state) {
        if (state.isAir() || level.isClientSide) return;
        ArrayDeque<Entry> q = LOG.computeIfAbsent(level.dimension(), k -> new ArrayDeque<>());
        q.addLast(new Entry(pos.immutable(), state, level.getGameTime()));
        while (q.size() > MAX || (!q.isEmpty() && level.getGameTime() - q.peekFirst().time() > KEEP_TICKS)) q.removeFirst();
    }

    public static void onBreak(BlockEvent.BreakEvent e) {
        if (e.getLevel() instanceof Level level) record(level, e.getPos(), e.getState());
    }

    public static void onExplode(ExplosionEvent.Detonate e) {
        for (BlockPos p : e.getAffectedBlocks()) record(e.getLevel(), p, e.getLevel().getBlockState(p));
    }

    /** Restores at most maxBlocks destroyed within maxAgeTicks inside a cube of half-size 'half'. */
    public static int rewindCube(ServerLevel level, BlockPos center, int half, int maxBlocks, long maxAgeTicks) {
        ArrayDeque<Entry> q = LOG.get(level.dimension());
        if (q == null) return 0;
        java.util.List<Entry> pick = new java.util.ArrayList<>();
        Iterator<Entry> it = q.descendingIterator();
        while (it.hasNext() && pick.size() < maxBlocks) {
            Entry e = it.next();
            if (level.getGameTime() - e.time() > maxAgeTicks) break;
            BlockPos d = e.pos().subtract(center);
            if (Math.abs(d.getX()) <= half && Math.abs(d.getY()) <= half && Math.abs(d.getZ()) <= half) { pick.add(e); it.remove(); }
        }
        int restored = 0;
        for (int i = pick.size() - 1; i >= 0; i--) {
            Entry e = pick.get(i);
            BlockState now = level.getBlockState(e.pos());
            if (now.isAir() || now.canBeReplaced()) { level.setBlock(e.pos(), e.state(), 3); restored++; }
        }
        return restored;
    }

    /** Restores blocks destroyed within maxAgeTicks inside radius. Returns how many came back. */
    public static int rewind(ServerLevel level, BlockPos center, double radius, long maxAgeTicks) {
        ArrayDeque<Entry> q = LOG.get(level.dimension());
        if (q == null) return 0;
        int restored = 0;
        double r2 = radius * radius;
        Iterator<Entry> it = q.descendingIterator(); // newest first -> oldest state wins at the end
        java.util.List<Entry> toRestore = new java.util.ArrayList<>();
        while (it.hasNext()) {
            Entry e = it.next();
            if (level.getGameTime() - e.time() > maxAgeTicks) break;
            if (e.pos().distSqr(center) <= r2) { toRestore.add(e); it.remove(); }
        }
        for (int i = toRestore.size() - 1; i >= 0; i--) {
            Entry e = toRestore.get(i);
            BlockState now = level.getBlockState(e.pos());
            if (now.isAir() || now.canBeReplaced()) { level.setBlock(e.pos(), e.state(), 3); restored++; }
        }
        return restored;
    }
}
