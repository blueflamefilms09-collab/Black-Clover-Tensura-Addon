package com.newuniverse.nusmp.spiritspace;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Why: the waypoints of a player have to survive a restart and a death. They are kept in the player's own persistent data (so they are per player, keyed by who the player is, never by anything
 * global) under {@link #KEY}; this class turns that tag into the pure {@link Waypoints.Waypoint} book and back, and cleans the book on every load (a malformed entry or one whose dimension no
 * longer exists is dropped). It does no world work beyond looking up a level by its id.
 */
final class WaypointStore {
    private WaypointStore() {}

    static final String KEY = "nusmp_spirit_space";

    /** The cleaned book of the player: exactly {@link SpiritSpaceConfig#waypoints()} slots, null where nothing is set. */
    static Waypoints.Waypoint[] load(ServerPlayer p) {
        int slots = SpiritSpaceConfig.waypoints();
        Waypoints.Waypoint[] raw = new Waypoints.Waypoint[SpiritSpaceRules.WAYPOINTS_HARD];
        CompoundTag root = p.getPersistentData().getCompound(KEY);
        ListTag list = root.getList("wp", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            int slot = c.getInt("slot");
            if (slot < 0 || slot >= raw.length) continue;
            raw[slot] = new Waypoints.Waypoint(c.getString("name"), c.getString("dim"), c.getDouble("x"), c.getDouble("y"), c.getDouble("z"), c.getFloat("yaw"));
        }
        var server = p.getServer();
        return Waypoints.clean(raw, slots, -2048, 4096, dim -> {
            ResourceLocation id = ResourceLocation.tryParse(dim);
            return id != null && server != null && server.getLevel(ResourceKey.create(Registries.DIMENSION, id)) != null;
        });
    }

    static void save(ServerPlayer p, Waypoints.Waypoint[] book) {
        ListTag list = new ListTag();
        for (int i = 0; i < book.length; i++) {
            Waypoints.Waypoint w = book[i];
            if (w == null) continue;
            CompoundTag c = new CompoundTag();
            c.putInt("slot", i);
            c.putString("name", w.name());
            c.putString("dim", w.dim());
            c.putDouble("x", w.x());
            c.putDouble("y", w.y());
            c.putDouble("z", w.z());
            c.putFloat("yaw", w.yaw());
            list.add(c);
        }
        CompoundTag root = new CompoundTag();
        root.put("wp", list);
        p.getPersistentData().put(KEY, root);
    }

    /** A new body (death, the End): the book goes with the player. */
    static void copy(Player from, Player to) {
        CompoundTag old = from.getPersistentData();
        if (old.contains(KEY, Tag.TAG_COMPOUND)) to.getPersistentData().put(KEY, old.getCompound(KEY).copy());
    }

    static String dimensionId(Level l) { return l.dimension().location().toString(); }

    static int minY(ServerLevel sl) { return sl.getMinBuildHeight(); }

    static int maxY(ServerLevel sl) { return sl.getMaxBuildHeight(); }
}
