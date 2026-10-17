package com.newuniverse.nusmp.labyrinth.mega.hub;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Why: the plaza is stamped ONCE, the first time somebody arrives, and a second stamp would wipe whatever a creative player changed on the square in the
 * meantime. "Once" has to survive a restart, so it is a saved record, {@code <sanctuary dimension>/data/nusmp_hub.dat}: whether the plaza was built, with
 * which build version of the template, and whether it was the programmatic fallback. The record lives in the SANCTUARY's own data storage, not the
 * Overworld's (where {@code MegaData} lives) because it describes that dimension's chunks: an admin who deletes the dimension's folder to start Floor 0
 * afresh takes the record with it and the plaza is stamped again, instead of the record claiming a plaza that no longer exists. The version is shown by
 * {@code /multiverse mega hub status} next to the current one; nothing re-stamps by itself when it is older, because a new template must never silently
 * overwrite a base somebody built beside the square. Server thread only (no locking).
 */
public final class HubData extends SavedData {
    /** 0 = the plaza was never stamped; otherwise the {@link HubPlazaLayout#BUILD_VERSION} of the template that was. */
    public int plazaVersion;
    /** True when the programmatic fallback was stamped because the template was missing or unreadable. */
    public boolean fallback;

    public HubData() {}

    public static HubData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(HubData::new, HubData::load, null), HubKeys.DATA_NAME);
    }

    public boolean built() { return plazaVersion > 0; }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("version", plazaVersion);
        tag.putBoolean("fallback", fallback);
        return tag;
    }

    public static HubData load(CompoundTag tag, HolderLookup.Provider registries) {
        HubData d = new HubData();
        d.plazaVersion = tag.getInt("version");
        d.fallback = tag.getBoolean("fallback");
        return d;
    }
}
