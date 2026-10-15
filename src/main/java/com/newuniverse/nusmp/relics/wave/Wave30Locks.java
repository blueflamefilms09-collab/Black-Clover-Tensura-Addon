package com.newuniverse.nusmp.relics.wave;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: a lock that vanished on relog would be useless; the table must be saved data, and bounded so a player cannot lock a whole town.
 * <p>
 * Wave 30: the Arcane Locks of Warren's Key, kept with the world (so a lock holds while its owner is away and across restarts): which door, gate,
 * trapdoor or container (by dimension and position) is locked, and for whom. At most sixteen per owner; the oldest lock gives way to a new one.
 */
final class Wave30Locks extends SavedData {
    static final String NAME = "nusmp_w30_locks";

    private final Map<String, UUID> owners = new LinkedHashMap<>();

    static Wave30Locks get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Wave30Locks::new, Wave30Locks::load, null), NAME);
    }

    static String key(String dim, long pos) { return dim + "|" + pos; }

    /** The owner of the lock at this place, or null. */
    UUID ownerAt(String dim, long pos) { return owners.get(key(dim, pos)); }

    /** Locks the place for the owner; the owner's oldest lock is released if they already hold {@code max}. */
    void lock(String dim, long pos, UUID owner, int max) {
        String k = key(dim, pos);
        owners.remove(k);
        List<String> mine = new ArrayList<>();
        for (Map.Entry<String, UUID> e : owners.entrySet()) if (e.getValue().equals(owner)) mine.add(e.getKey());
        while (mine.size() >= max) owners.remove(mine.remove(0));
        owners.put(k, owner);
        setDirty();
    }

    boolean unlock(String dim, long pos) {
        boolean had = owners.remove(key(dim, pos)) != null;
        if (had) setDirty();
        return had;
    }

    int size() { return owners.size(); }

    static Wave30Locks load(CompoundTag tag, HolderLookup.Provider provider) {
        Wave30Locks l = new Wave30Locks();
        ListTag list = tag.getList("locks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            if (c.hasUUID("o")) l.owners.put(key(c.getString("d"), c.getLong("p")), c.getUUID("o"));
        }
        return l;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<String, UUID> e : owners.entrySet()) {
            int bar = e.getKey().lastIndexOf('|');
            CompoundTag c = new CompoundTag();
            c.putString("d", e.getKey().substring(0, bar));
            c.putLong("p", Long.parseLong(e.getKey().substring(bar + 1)));
            c.putUUID("o", e.getValue());
            list.add(c);
        }
        tag.put("locks", list);
        return tag;
    }
}
