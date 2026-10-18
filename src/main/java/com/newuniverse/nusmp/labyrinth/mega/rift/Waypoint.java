package com.newuniverse.nusmp.labyrinth.mega.rift;

import net.minecraft.nbt.CompoundTag;

/**
 * Why: the owner's {@code PlayerDungeonWaypoint}: the exact place a player last stood in the Mega-Dungeon, so a Return Crystal, a Gate Key or the Return Portal on Floor 0 can put them back on
 * the same block. It holds the dimension key, the level (the decade the floor belongs to, 1 to 100), the floor number (signed: negative on the Abyssal road), the position and the rotation.
 * NeoForge 1.21 has no Forge-style capability for a player's own data; this mod keeps a player's records in the player's persistent data (the Hub's origin, the landings, the party), which
 * survives death, logout and restart and is copied to a new body on respawn by the persistent-data merge of {@code Player#restoreFrom}, so the waypoint is stored the same way: one compound
 * under {@link #KEY}. Pure apart from the compound.
 */
public record Waypoint(String dimension, int sector, int level, int floor, double x, double y, double z, float yaw, float pitch, long savedAt) {
    /** The key in the player's persistent data. */
    public static final String KEY = "nusmp_mega_waypoint";

    /** The level (decade) of a signed floor: floors 1..10 are level 1, 11..20 level 2 ... the same for the negative floors; floor 0 is level 0. */
    public static int levelOf(int floor) {
        return floor == 0 ? 0 : (Math.abs(floor) - 1) / 10 + 1;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("Dim", dimension);
        t.putInt("Sector", sector);
        t.putInt("Level", level);
        t.putInt("Floor", floor);
        t.putDouble("X", x);
        t.putDouble("Y", y);
        t.putDouble("Z", z);
        t.putFloat("Yaw", yaw);
        t.putFloat("Pitch", pitch);
        t.putLong("At", savedAt);
        return t;
    }

    /** The waypoint of a compound, or null when it holds none. */
    public static Waypoint load(CompoundTag t) {
        if (t == null || !t.contains("Dim") || t.getString("Dim").isEmpty()) return null;
        return new Waypoint(t.getString("Dim"), t.getInt("Sector"), t.getInt("Level"), t.getInt("Floor"), t.getDouble("X"), t.getDouble("Y"), t.getDouble("Z"),
                t.getFloat("Yaw"), t.getFloat("Pitch"), t.getLong("At"));
    }
}
