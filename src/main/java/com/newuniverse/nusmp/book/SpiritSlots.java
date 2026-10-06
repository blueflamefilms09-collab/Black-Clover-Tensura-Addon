package com.newuniverse.nusmp.book;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-wide spirit ownership: one mage per spirit (Salamander, Undine, Sylph, Gnome). */
public final class SpiritSlots extends SavedData {
    private final Map<String, UUID> owners = new HashMap<>();

    public static SpiritSlots get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SpiritSlots::new, SpiritSlots::load, null), "nusmp_spirits");
    }

    private static SpiritSlots load(CompoundTag tag, HolderLookup.Provider provider) {
        SpiritSlots s = new SpiritSlots();
        for (String k : tag.getAllKeys()) s.owners.put(k, tag.getUUID(k));
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        owners.forEach(tag::putUUID);
        return tag;
    }

    public UUID owner(String spirit) { return owners.get(spirit); }

    /** Claims the spirit for this player. False if another mage already holds it. */
    public boolean claim(String spirit, UUID player) {
        UUID cur = owners.get(spirit);
        if (cur != null && !cur.equals(player)) return false;
        if (cur == null) { owners.put(spirit, player); setDirty(); }
        return true;
    }

    public boolean release(String spirit) {
        boolean had = owners.remove(spirit) != null;
        if (had) setDirty();
        return had;
    }
}
