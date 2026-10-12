package com.newuniverse.nusmp.dungeon.city;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What the Golden Cities remember (saved with the world, in the Overworld's data storage as "nusmp_golden_city"): for each city cell, how far
 * its building got (so a restart picks it up again), where the doors of its ways in are, and which players have already taken the gift of its altar.
 */
public final class CitySave extends SavedData {
    public static final int BUILDING = 1, DONE = 2;

    /** One city: its state, the next op and cell to build, the doors of its ways in (block positions), and the players whose claim on the altar is spent. */
    public static final class Entry {
        public int state = BUILDING;
        public int op;
        public long cell;
        public long[] ways = new long[0];
        public final Set<UUID> claims = new LinkedHashSet<>();
    }

    private final Map<Long, Entry> cities = new HashMap<>();

    public static CitySave get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(CitySave::new, CitySave::load, null), "nusmp_golden_city");
    }

    /** The key of a map cell. */
    public static long key(int cellX, int cellZ) { return ((long) cellX << 32) | (cellZ & 0xFFFFFFFFL); }

    public static int cellX(long key) { return (int) (key >> 32); }

    public static int cellZ(long key) { return (int) key; }

    /** The record of a city, or null if nothing was ever started or claimed there. */
    public Entry lookup(long key) { return cities.get(key); }

    private Entry entry(long key) { return cities.computeIfAbsent(key, k -> new Entry()); }

    /** The doors of a city's ways in, as block positions (BlockPos.asLong); empty if none was opened. */
    public long[] ways(long key) {
        Entry e = cities.get(key);
        return e == null ? new long[0] : e.ways;
    }

    /** Remembers the doors of a city's ways in. */
    public void setWays(long key, long[] doors) {
        entry(key).ways = doors;
        setDirty();
    }

    public boolean isDone(long key) {
        Entry e = cities.get(key);
        return e != null && e.state == DONE;
    }

    /** Remembers how far the building got. */
    public void progress(long key, int op, long cell) {
        Entry e = entry(key);
        e.op = op;
        e.cell = cell;
        setDirty();
    }

    public void finish(long key) {
        Entry e = entry(key);
        e.state = DONE;
        setDirty();
    }

    /** Spends a player's claim on a city's altar. False if they had spent it already. */
    public boolean claim(long key, UUID player) {
        boolean fresh = entry(key).claims.add(player);
        if (fresh) setDirty();
        return fresh;
    }

    public int claims(long key) {
        Entry e = cities.get(key);
        return e == null ? 0 : e.claims.size();
    }

    /** Forgets a city: it counts as unbuilt again, and every claim on its altar is given back. */
    public boolean forget(long key) {
        boolean had = cities.remove(key) != null;
        if (had) setDirty();
        return had;
    }

    private static CitySave load(CompoundTag tag, HolderLookup.Provider provider) {
        CitySave s = new CitySave();
        ListTag list = tag.getList("Cities", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Entry e = new Entry();
            e.state = t.getInt("State");
            e.op = t.getInt("Op");
            e.cell = t.getLong("Cell");
            e.ways = t.getLongArray("Ways");
            ListTag claims = t.getList("Claims", Tag.TAG_COMPOUND);
            for (int c = 0; c < claims.size(); c++) {
                try { e.claims.add(UUID.fromString(claims.getCompound(c).getString("U"))); } catch (IllegalArgumentException bad) { /* a damaged claim is dropped */ }
            }
            s.cities.put(t.getLong("K"), e);
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, Entry> me : cities.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putLong("K", me.getKey());
            t.putInt("State", me.getValue().state);
            t.putInt("Op", me.getValue().op);
            t.putLong("Cell", me.getValue().cell);
            t.putLongArray("Ways", me.getValue().ways);
            ListTag claims = new ListTag();
            for (UUID id : me.getValue().claims) {
                CompoundTag c = new CompoundTag();
                c.putString("U", id.toString());
                claims.add(c);
            }
            t.put("Claims", claims);
            list.add(t);
        }
        tag.put("Cities", list);
        return tag;
    }
}
