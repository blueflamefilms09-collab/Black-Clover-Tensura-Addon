package com.newuniverse.nusmp.dungeon;

import net.minecraft.server.MinecraftServer;

/**
 * The 1.19.0 flag of the Golden City's seal ({@code DungeonSave.cityBreached}), for the Nexus code that lives in another package. The old
 * flow in {@link DungeonEcology} still sets it and still announces the first seal of a world; the Nexus reads it to adopt older worlds and
 * to avoid announcing the same first entrance twice.
 */
public final class LegacySeal {
    private LegacySeal() {}

    public static boolean broken(MinecraftServer server) { return DungeonSave.get(server).cityBreached; }

    /** Sets the flag (so the old flow stays quiet from now on). */
    public static void markBroken(MinecraftServer server) {
        DungeonSave s = DungeonSave.get(server);
        if (s.cityBreached) return;
        s.cityBreached = true;
        s.setDirty();
    }
}
