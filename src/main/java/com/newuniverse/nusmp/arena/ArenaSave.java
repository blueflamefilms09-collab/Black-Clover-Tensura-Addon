package com.newuniverse.nusmp.arena;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the arena has to remember four things across restarts: that its courtyard is already built (it is built once, on the first
 * arrival), the state of the fight ({@link ArenaState}: ready, fighting or resting, and until when), which boss belongs to the fight, and
 * where each traveller must be returned ({@link ReturnBook}). One saved-data object on the overworld's storage holds them, the way the other
 * server-wide books of the mod do; the logic is in the pure classes and this is only the wrapper that writes them into a tag.
 */
public final class ArenaSave extends SavedData {
    public static final String NAME = "nusmp_zogratis_arena";

    private boolean built;
    private UUID bossId;
    private final ArenaState state = new ArenaState();
    private final ReturnBook returns = new ReturnBook();

    public static ArenaSave get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ArenaSave::new, ArenaSave::load, null), NAME);
    }

    private static ArenaSave load(CompoundTag tag, HolderLookup.Provider provider) {
        ArenaSave s = new ArenaSave();
        s.built = tag.getBoolean("Built");
        s.state.restore(tag.getLongArray("State"));
        if (tag.hasUUID("Boss")) s.bossId = tag.getUUID("Boss");
        CompoundTag r = tag.getCompound("Returns");
        Map<String, String> in = new LinkedHashMap<>();
        for (String k : r.getAllKeys()) in.put(k, r.getString(k));
        s.returns.fromStrings(in);
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("Built", built);
        tag.putLongArray("State", state.save());
        if (bossId != null) tag.putUUID("Boss", bossId);
        CompoundTag r = new CompoundTag();
        for (Map.Entry<String, String> e : returns.toStrings().entrySet()) r.putString(e.getKey(), e.getValue());
        tag.put("Returns", r);
        return tag;
    }

    public boolean built() { return built; }

    public void built(boolean b) {
        built = b;
        setDirty();
    }

    public ArenaState state() { return state; }

    public ReturnBook returns() { return returns; }

    public UUID bossId() { return bossId; }

    public void bossId(UUID id) {
        bossId = id;
        setDirty();
    }
}
