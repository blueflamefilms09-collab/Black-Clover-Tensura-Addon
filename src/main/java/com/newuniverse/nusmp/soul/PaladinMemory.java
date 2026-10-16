package com.newuniverse.nusmp.soul;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: when a caster dies their Paladins must revert, but some of those Paladins may be in chunks that are not loaded at that
 * moment, and they cannot be reached. So the death is written down (owner id and the game time of the death) in a small world
 * save, and a Paladin that loads later compares its own conversion time with it: converted before the owner's last death means it is
 * released as it loads. The same save is what makes "the owner died" survive a restart.
 */
public final class PaladinMemory extends SavedData {
    private final Map<UUID, Long> deaths = new HashMap<>();

    public static PaladinMemory get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PaladinMemory::new, PaladinMemory::load, null), "nusmp_paladin_memory");
    }

    private static PaladinMemory load(CompoundTag tag, HolderLookup.Provider provider) {
        PaladinMemory m = new PaladinMemory();
        CompoundTag d = tag.getCompound("Deaths");
        for (String k : d.getAllKeys()) {
            try { m.deaths.put(UUID.fromString(k), d.getLong(k)); } catch (IllegalArgumentException badKey) { /* a stray key: ignored */ }
        }
        return m;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag d = new CompoundTag();
        deaths.forEach((k, v) -> d.putLong(k.toString(), v));
        tag.put("Deaths", d);
        return tag;
    }

    /** The owner died at this game time. */
    public void died(UUID owner, long gameTime) {
        deaths.put(owner, gameTime);
        setDirty();
    }

    /** Was a Paladin converted at 'convertedAt' made before its owner's last death? */
    public boolean outlived(UUID owner, long convertedAt) {
        Long d = deaths.get(owner);
        return d != null && convertedAt <= d;
    }
}
