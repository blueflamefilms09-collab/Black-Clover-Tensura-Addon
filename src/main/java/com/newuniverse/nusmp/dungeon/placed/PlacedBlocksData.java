package com.newuniverse.nusmp.dungeon.placed;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Why: which positions of a dungeon dimension were placed by a player has to survive a restart, or every torch a player put down would turn into
 * unbreakable stone at the next login. It is a {@link SavedData} of the dimension's own data storage (so each dungeon level has its own, and it is saved
 * with that level), holding one {@link PlacedSet} in placing order as a long array under {@code placed}. Nothing is written until the first block is
 * placed. When the config's caps are lower than the saved list, the oldest marks fall away on load.
 */
public final class PlacedBlocksData extends SavedData {
    public static final String NAME = "nusmp_dungeon_placed";
    private static final int VERSION = 1;

    private final PlacedSet set;

    PlacedBlocksData(PlacedSet set) { this.set = set; }

    private static PlacedBlocksData fresh() {
        return new PlacedBlocksData(new PlacedSet(PlacedConfig.get(PlacedConfig.CAP), PlacedConfig.get(PlacedConfig.PER_CHUNK)));
    }

    private static SavedData.Factory<PlacedBlocksData> factory() { return new SavedData.Factory<>(PlacedBlocksData::fresh, PlacedBlocksData::load, null); }

    /** The data of this level, created (empty) on first use. */
    public static PlacedBlocksData get(ServerLevel level) {
        PlacedBlocksData d = level.getDataStorage().computeIfAbsent(factory(), NAME);
        if (d.set.resize(PlacedConfig.get(PlacedConfig.CAP), PlacedConfig.get(PlacedConfig.PER_CHUNK)) > 0) d.setDirty();
        return d;
    }

    /** The data of this level if there is any (a level where nobody ever placed a block has none); never creates it. */
    public static PlacedBlocksData peek(ServerLevel level) { return level.getDataStorage().get(factory(), NAME); }

    public PlacedSet set() { return set; }

    static PlacedBlocksData load(CompoundTag tag, HolderLookup.Provider provider) {
        long[] keys = tag.contains("placed", Tag.TAG_LONG_ARRAY) ? tag.getLongArray("placed") : new long[0];
        return new PlacedBlocksData(PlacedSet.fromArray(keys, PlacedConfig.get(PlacedConfig.CAP), PlacedConfig.get(PlacedConfig.PER_CHUNK)));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("v", VERSION);
        tag.putLongArray("placed", set.toArray());
        return tag;
    }
}
