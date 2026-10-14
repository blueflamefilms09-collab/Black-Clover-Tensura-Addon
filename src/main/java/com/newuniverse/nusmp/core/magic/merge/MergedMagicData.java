package com.newuniverse.nusmp.core.magic.merge;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The world's merged magics ("nusmp_merged_magic"): a {@link MergeStore} saved as its encoded lines. The store here is the working
 * copy that commands change; after every change {@link #publish()} gives {@link MergedRegistry} a fresh copy (which is what casting,
 * profiles and the sync read), so readers never see a half-made change.
 */
public final class MergedMagicData extends SavedData {
    private MergeStore store = new MergeStore();

    public static MergedMagicData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(MergedMagicData::new, MergedMagicData::load, null), "nusmp_merged_magic");
    }

    private static MergedMagicData load(CompoundTag tag, HolderLookup.Provider provider) {
        MergedMagicData d = new MergedMagicData();
        List<String> lines = new ArrayList<>();
        for (Tag t : tag.getList("lines", Tag.TAG_STRING)) lines.add(t.getAsString());
        d.store = MergeStore.decode(lines);
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (String line : store.encode()) list.add(StringTag.valueOf(line));
        tag.put("lines", list);
        return tag;
    }

    public MergeStore store() { return store; }

    /** Creates (or finds) the merged magic of a parent set; publishes and marks the data dirty only when something was created. */
    public MergeStore.Outcome create(List<String> parents, String customName, Predicate<String> isRealMagic, Set<String> reservedNames,
                                     Function<String, String> partName) {
        MergeStore.Outcome o = store.create(parents, customName, isRealMagic, reservedNames, partName);
        if (o.status() == MergeStore.Status.CREATED) { setDirty(); publish(); }
        return o;
    }

    /** Deletes a definition (only on request). */
    public boolean remove(String id) {
        boolean had = store.remove(id);
        if (had) { setDirty(); publish(); }
        return had;
    }

    /** Hands the readers a fresh copy of the current definitions. */
    public void publish() { MergedRegistry.replace(MergeStore.decode(store.encode())); }

    public List<String> lines() { return store.encode(); }
}
