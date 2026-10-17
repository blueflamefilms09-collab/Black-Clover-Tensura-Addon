package com.newuniverse.nusmp.labyrinth.mega.flow;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;

/**
 * Why: the decennial Metal Vessel is promised once per sector, and "once" has to survive a restart: a Guardian that dies the second day of a server's life
 * must not pay again because the first day's payment was forgotten. The memory is a tiny saved record, file {@code data/nusmp_flow.dat} in the OVERWORLD's
 * data storage (the same always-loaded place {@code nusmp_mega} and the Order's ledger use, so a lookup never has to wake the dungeon dimension): the
 * sectors that have paid and the relic each one paid, kept by the pure {@link RewardLog}, which owns the rule; this class only turns it into NBT and back.
 * It adds nothing to {@code Sector} (the sector's save format belongs to the generator), and an old world has no such file, which reads as "nobody has
 * been paid", exactly right for a world that never had the feature. Server thread only.
 */
public final class FlowData extends SavedData {
    public static final String NAME = "nusmp_flow";

    private final RewardLog rewards = new RewardLog();

    public FlowData() {}

    public static FlowData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(FlowData::new, FlowData::load, null), NAME);
    }

    /** The memory of which sectors have paid (read-only use; change it through {@link #claim}, which saves). */
    public RewardLog rewards() { return rewards; }

    public boolean rewarded(int sectorId) { return rewards.contains(sectorId); }

    /** Records that the sector paid this relic and marks the file dirty. True the first time only. */
    public boolean claim(int sectorId, String relicId) {
        boolean first = rewards.claim(sectorId, relicId);
        if (first) setDirty();
        return first;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<Integer, String> e : rewards.entries().entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putInt("sector", e.getKey());
            c.putString("relic", e.getValue());
            list.add(c);
        }
        tag.put("rewarded", list);
        return tag;
    }

    public static FlowData load(CompoundTag tag, HolderLookup.Provider registries) {
        FlowData d = new FlowData();
        ListTag list = tag.getList("rewarded", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            d.rewards.restore(c.getInt("sector"), c.getString("relic"));
        }
        return d;
    }
}
