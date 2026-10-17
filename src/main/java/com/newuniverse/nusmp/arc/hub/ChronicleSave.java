package com.newuniverse.nusmp.arc.hub;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Why: the Promenade and the Chronicle are laid once, on a visit to the Hub, and must not be laid again over what players have built beside them: this tiny
 * record of the Sanctuary's saved data says which build of the site is in the world. It is the Hub's own file ({@code <sanctuary>/data/nusmp_arc_chronicle.dat}),
 * like the plaza's record, so an old world that has never had the site simply reads "nothing built" and gets it on the next visit.
 */
public final class ChronicleSave extends SavedData {
    public static final String NAME = "nusmp_arc_chronicle";

    private int version;

    public static ChronicleSave get(ServerLevel hub) {
        return hub.getDataStorage().computeIfAbsent(new SavedData.Factory<>(ChronicleSave::new, ChronicleSave::load, null), NAME);
    }

    private static ChronicleSave load(CompoundTag tag, HolderLookup.Provider provider) {
        ChronicleSave s = new ChronicleSave();
        s.version = Math.max(0, tag.getInt("Version"));
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("Version", version);
        return tag;
    }

    /** The build of the site in the world (0 = none). */
    public int version() { return version; }

    public void version(int v) {
        version = v;
        setDirty();
    }
}
