package com.newuniverse.nusmp.dungeon.nexus.vault;

import com.newuniverse.nusmp.dungeon.LivingDungeon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Where the Descent Gates of the Living Dungeon are (saved with the world, in the Overworld's data storage as "nusmp_gates"). The gate
 * block records itself when it is placed and forgets itself when it goes; a gate that is found missing when its chunk is loaded is
 * forgotten on the spot, and {@link #repair} walks the records a few at a time. Two kinds are kept apart: the gates that lead down
 * (what the Ki-Resonance Lantern looks for) and the ones that lead back up, with the link from each of those to the gate it was reached from.
 */
public final class GateIndex extends SavedData {
    public static final String NAME = "nusmp_gates";

    private final GateGrid down = new GateGrid(), up = new GateGrid();
    /** A gate that leads up -> the gate down above it, and the other way. Block positions as longs. */
    private final Map<Long, Long> upToDown = new HashMap<>(), downToUp = new HashMap<>();
    private int repairCursor;

    public static GateIndex get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(GateIndex::new, GateIndex::load, null), NAME);
    }

    // ---------------------------------------------------------------- the contract other packages call
    /**
     * The nearest Descent Gate that leads down, within `radius` blocks of `from` (in three dimensions), or null. Only in the Dungeon. A record
     * whose block is gone (its chunk is loaded and holds something else) is dropped and the search goes on.
     */
    public static BlockPos nearest(ServerLevel level, BlockPos from, int radius) {
        if (level == null || from == null || !LivingDungeon.in(level)) return null;
        GateIndex idx = get(level.getServer());
        for (int i = 0; i < 8; i++) {
            GateGrid.P p = idx.down.nearest(from.getX(), from.getY(), from.getZ(), radius);
            if (p == null) return null;
            BlockPos at = new BlockPos(p.x(), p.y(), p.z());
            if (idx.stillThere(level, at, false)) return at;
            idx.drop(at);
        }
        return null;
    }

    // ---------------------------------------------------------------- the block's own hooks
    /** A gate block was placed (a rising one leads up). */
    static void noted(ServerLevel level, BlockPos pos, boolean rising) {
        if (!LivingDungeon.in(level)) return;
        GateIndex idx = get(level.getServer());
        if ((rising ? idx.up : idx.down).add(pos.getX(), pos.getY(), pos.getZ())) idx.setDirty();
    }

    /** A gate block went. */
    static void forgotten(ServerLevel level, BlockPos pos) {
        if (!LivingDungeon.in(level)) return;
        get(level.getServer()).drop(pos);
    }

    private void drop(BlockPos pos) {
        boolean changed = down.remove(pos.getX(), pos.getY(), pos.getZ());
        changed |= up.remove(pos.getX(), pos.getY(), pos.getZ());
        long k = pos.asLong();
        Long o = upToDown.remove(k);
        if (o != null) downToUp.remove(o, k);
        Long a = downToUp.remove(k);
        if (a != null) upToDown.remove(a, k);
        if (changed || o != null || a != null) setDirty();
    }

    // ---------------------------------------------------------------- links between the two ends of a way
    /** Records that the rising gate at `arrival` leads back to the descending gate at `origin`. */
    public void link(BlockPos origin, BlockPos arrival) {
        upToDown.put(arrival.asLong(), origin.asLong());
        downToUp.put(origin.asLong(), arrival.asLong());
        setDirty();
    }

    /** The rising gate that a descending gate leads to, or null if none was made. */
    public BlockPos arrivalOf(BlockPos origin) {
        Long a = downToUp.get(origin.asLong());
        return a == null ? null : BlockPos.of(a);
    }

    /** The descending gate that a rising gate leads back to, or null. */
    public BlockPos originOf(BlockPos arrival) {
        Long o = upToDown.get(arrival.asLong());
        return o == null ? null : BlockPos.of(o);
    }

    public int downCount() { return down.size(); }

    public int upCount() { return up.size(); }

    public boolean knowsDown(BlockPos pos) { return down.contains(pos.getX(), pos.getY(), pos.getZ()); }

    /** Every descending gate recorded, for the admin command. */
    public List<BlockPos> downGates() {
        List<BlockPos> out = new ArrayList<>();
        for (GateGrid.P p : down.all()) out.add(new BlockPos(p.x(), p.y(), p.z()));
        return out;
    }

    // ---------------------------------------------------------------- repair
    /** Is the gate really there? A chunk that is not loaded is taken on trust. */
    private boolean stillThere(ServerLevel level, BlockPos at, boolean rising) {
        if (level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4) == null) return true;
        var s = level.getBlockState(at);
        return s.is(NexusBlocks.DESCENT_GATE.get()) && s.getValue(DescentGateBlock.RISING) == rising;
    }

    /** Checks up to `budget` records against the world (those in loaded chunks) and drops the ones whose gate is gone. Returns how many were dropped. */
    public static int repair(ServerLevel level, int budget) {
        GateIndex idx = get(level.getServer());
        List<GateGrid.P> downs = idx.down.all(), ups = idx.up.all();
        int total = downs.size() + ups.size(), dropped = 0;
        if (total == 0) return 0;
        for (int i = 0; i < Math.min(budget, total); i++) {
            int n = Math.floorMod(idx.repairCursor++, total);
            boolean rising = n >= downs.size();
            GateGrid.P p = rising ? ups.get(n - downs.size()) : downs.get(n);
            BlockPos at = new BlockPos(p.x(), p.y(), p.z());
            if (!idx.stillThere(level, at, rising)) {
                idx.drop(at);
                dropped++;
            }
        }
        return dropped;
    }

    // ---------------------------------------------------------------- saving
    private static GateIndex load(CompoundTag tag, HolderLookup.Provider provider) {
        GateIndex g = new GateIndex();
        for (long l : tag.getLongArray("Down")) {
            BlockPos p = BlockPos.of(l);
            g.down.add(p.getX(), p.getY(), p.getZ());
        }
        for (long l : tag.getLongArray("Up")) {
            BlockPos p = BlockPos.of(l);
            g.up.add(p.getX(), p.getY(), p.getZ());
        }
        ListTag links = tag.getList("Links", Tag.TAG_COMPOUND);
        for (int i = 0; i < links.size(); i++) {
            CompoundTag t = links.getCompound(i);
            g.upToDown.put(t.getLong("U"), t.getLong("O"));
            g.downToUp.put(t.getLong("O"), t.getLong("U"));
        }
        return g;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLongArray("Down", longs(down));
        tag.putLongArray("Up", longs(up));
        ListTag links = new ListTag();
        for (Map.Entry<Long, Long> e : upToDown.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putLong("U", e.getKey());
            t.putLong("O", e.getValue());
            links.add(t);
        }
        tag.put("Links", links);
        return tag;
    }

    private static long[] longs(GateGrid g) {
        List<GateGrid.P> all = g.all();
        long[] out = new long[all.size()];
        for (int i = 0; i < out.length; i++) out[i] = BlockPos.asLong(all.get(i).x(), all.get(i).y(), all.get(i).z());
        return out;
    }
}
