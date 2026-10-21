package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/**
 * Why: a cleared room must stay cleared. The state of a locked room lives in its barrier block entity while it is closed, but once the barriers come down the block
 * entities are gone, and without a memory the director would lock the same room again the next time anybody walked in (a room that locks for ever is a chore, not a
 * challenge). So the rooms that were cleared are kept in the overworld's data storage, in the file {@code data/nusmp_lockdown.dat}, next to {@code nusmp_mega}: a set of
 * keys ({@link LockdownMath#key}: sector id and room id), saved when it changes, pruned never (a few thousand longs at most). A sector that is regenerated gets new
 * ids, so its rooms are new rooms. Server thread only.
 */
public final class LockdownData extends SavedData {
    public static final String NAME = "nusmp_lockdown";

    private final Set<Long> cleared = new HashSet<>();

    public LockdownData() {}

    public static LockdownData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(LockdownData::new, LockdownData::load, null), NAME);
    }

    public boolean isCleared(int sectorId, int roomId) { return cleared.contains(LockdownMath.key(sectorId, roomId)); }

    public boolean markCleared(int sectorId, int roomId) {
        boolean added = cleared.add(LockdownMath.key(sectorId, roomId));
        if (added) setDirty();
        return added;
    }

    /** Forgets a room's cleared mark (an operator's reset); returns whether it was there. */
    public boolean forget(int sectorId, int roomId) {
        boolean removed = cleared.remove(LockdownMath.key(sectorId, roomId));
        if (removed) setDirty();
        return removed;
    }

    /** Forgets every cleared mark; returns how many there were. */
    public int forgetAll() {
        int n = cleared.size();
        if (n > 0) {
            cleared.clear();
            setDirty();
        }
        return n;
    }

    public int clearedCount() { return cleared.size(); }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        long[] a = new long[cleared.size()];
        int i = 0;
        for (long k : cleared) a[i++] = k;
        tag.putLongArray("cleared", a);
        return tag;
    }

    public static LockdownData load(CompoundTag tag, HolderLookup.Provider registries) {
        LockdownData d = new LockdownData();
        for (long k : tag.getLongArray("cleared")) d.cleared.add(k);
        return d;
    }
}
