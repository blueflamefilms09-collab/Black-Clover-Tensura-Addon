package com.newuniverse.nusmp.dungeon.placed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Why: the Dungeon holds every block it generated, but a player must be able to take back what THEY put down (a torch, a crafting table, a sleeping
 * bag, a backpack, a bridge of cobblestone over a pit). The only way to tell the two apart is to remember which positions a player placed, so this is
 * that memory as a plain data structure with no game classes in it (it is unit tested without Minecraft): a set of packed block positions in the order
 * they were placed, indexed by chunk. It is bounded two ways, because a save file must never grow without limit: a cap for the whole dimension and a
 * smaller one for each chunk. When a cap is hit the OLDEST mark is dropped (the block it described becomes part of the dungeon's stone again), never the
 * newest, so what a person just built always works. Positions are packed the way {@code BlockPos.asLong} packs them (26 bits of x, 26 of z, 12 of y), so a
 * caller with a BlockPos can pass {@code pos.asLong()} to the {@code *Key} methods; this class does not import it.
 */
public final class PlacedSet {
    public static final int DEFAULT_CAP = 16384, DEFAULT_PER_CHUNK = 4096, MIN_CAP = 16, MAX_CAP = 1_000_000;

    private final LinkedHashSet<Long> all = new LinkedHashSet<>();
    private final Map<Long, LinkedHashSet<Long>> byChunk = new HashMap<>();
    private int cap, perChunk;
    private long evicted;

    public PlacedSet() { this(DEFAULT_CAP, DEFAULT_PER_CHUNK); }

    public PlacedSet(int cap, int perChunk) {
        this.cap = clampCap(cap);
        this.perChunk = Math.max(1, Math.min(perChunk, this.cap));
    }

    // ---------------------------------------------------------------- packing (the same layout as BlockPos.asLong)
    public static long pack(int x, int y, int z) { return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFFL); }

    public static int unpackX(long p) { return (int) (p >> 38); }

    public static int unpackY(long p) { return (int) (p << 52 >> 52); }

    public static int unpackZ(long p) { return (int) (p << 26 >> 38); }

    /** The chunk of a packed position, packed like {@code ChunkPos.asLong}. */
    public static long chunkOfKey(long p) { return chunkKey(unpackX(p) >> 4, unpackZ(p) >> 4); }

    public static long chunkKey(int cx, int cz) { return (cx & 0xFFFFFFFFL) | ((long) cz << 32); }

    public static int chunkX(long chunkKey) { return (int) chunkKey; }

    public static int chunkZ(long chunkKey) { return (int) (chunkKey >> 32); }

    public static int clampCap(int cap) { return Math.max(MIN_CAP, Math.min(MAX_CAP, cap)); }

    // ---------------------------------------------------------------- reading
    public boolean contains(int x, int y, int z) { return containsKey(pack(x, y, z)); }

    public boolean containsKey(long key) { return !all.isEmpty() && all.contains(key); }

    public int size() { return all.size(); }

    public boolean isEmpty() { return all.isEmpty(); }

    public int cap() { return cap; }

    public int perChunkCap() { return perChunk; }

    /** How many marks the caps have dropped since this set was built (the report of the pruning). */
    public long evictions() { return evicted; }

    public int chunkSize(int cx, int cz) {
        LinkedHashSet<Long> c = byChunk.get(chunkKey(cx, cz));
        return c == null ? 0 : c.size();
    }

    /** The marks of one chunk, oldest first. */
    public long[] chunkKeys(int cx, int cz) {
        LinkedHashSet<Long> c = byChunk.get(chunkKey(cx, cz));
        if (c == null) return new long[0];
        long[] out = new long[c.size()];
        int i = 0;
        for (long k : c) out[i++] = k;
        return out;
    }

    /** Every mark, oldest first (the order the save file keeps, so a reload prunes the same ones). */
    public long[] toArray() {
        long[] out = new long[all.size()];
        int i = 0;
        for (long k : all) out[i++] = k;
        return out;
    }

    public List<Long> chunks() { return new ArrayList<>(byChunk.keySet()); }

    // ---------------------------------------------------------------- writing
    /** Marks a position. Returns how many OLDER marks the caps dropped to make room (0 almost always); -1 when it was already marked. */
    public int add(int x, int y, int z) { return addKey(pack(x, y, z)); }

    public int addKey(long key) { return addKey(key, null); }

    /** Like {@link #addKey(long)}, and tells {@code sink} the key of every older mark the caps dropped (so a client mirror can be told). */
    public int addKey(long key, java.util.function.LongConsumer sink) {
        if (!all.add(key)) return -1;
        long ck = chunkOfKey(key);
        LinkedHashSet<Long> c = byChunk.computeIfAbsent(ck, k -> new LinkedHashSet<>());
        c.add(key);
        int dropped = 0;
        while (c.size() > perChunk) {
            Iterator<Long> it = c.iterator();
            long old = it.next();
            it.remove();
            all.remove(old);
            evicted++;
            dropped++;
            if (sink != null) sink.accept(old);
        }
        while (all.size() > cap) {
            Iterator<Long> it = all.iterator();
            long old = it.next();
            it.remove();
            dropIndex(old);
            evicted++;
            dropped++;
            if (sink != null) sink.accept(old);
        }
        return dropped;
    }

    public boolean remove(int x, int y, int z) { return removeKey(pack(x, y, z)); }

    public boolean removeKey(long key) {
        if (!all.remove(key)) return false;
        dropIndex(key);
        return true;
    }

    private void dropIndex(long key) {
        long ck = chunkOfKey(key);
        LinkedHashSet<Long> c = byChunk.get(ck);
        if (c == null) return;
        c.remove(key);
        if (c.isEmpty()) byChunk.remove(ck);
    }

    /** Forgets every mark of one chunk (the chunk was rebuilt). Returns how many. */
    public int removeChunk(int cx, int cz) {
        LinkedHashSet<Long> c = byChunk.remove(chunkKey(cx, cz));
        if (c == null) return 0;
        for (long k : c) all.remove(k);
        return c.size();
    }

    /** Forgets every mark inside a box (both corners inclusive, any order). Returns how many. */
    public int removeBox(int x0, int y0, int z0, int x1, int y1, int z1) {
        int ax = Math.min(x0, x1), bx = Math.max(x0, x1), ay = Math.min(y0, y1), by = Math.max(y0, y1), az = Math.min(z0, z1), bz = Math.max(z0, z1);
        int cx0 = ax >> 4, cx1 = bx >> 4, cz0 = az >> 4, cz1 = bz >> 4;
        List<Long> doomed = new ArrayList<>();
        for (Map.Entry<Long, LinkedHashSet<Long>> e : byChunk.entrySet()) {
            int cx = chunkX(e.getKey()), cz = chunkZ(e.getKey());
            if (cx < cx0 || cx > cx1 || cz < cz0 || cz > cz1) continue;
            for (long k : e.getValue()) {
                int x = unpackX(k), y = unpackY(k), z = unpackZ(k);
                if (x >= ax && x <= bx && y >= ay && y <= by && z >= az && z <= bz) doomed.add(k);
            }
        }
        for (long k : doomed) removeKey(k);
        return doomed.size();
    }

    public void clear() {
        all.clear();
        byChunk.clear();
    }

    /** Replaces one chunk's marks with exactly these (the client mirror takes the server's list of a chunk). */
    public void replaceChunk(int cx, int cz, long[] keys) {
        removeChunk(cx, cz);
        if (keys != null) for (long k : keys) addKey(k);
    }

    /** Changes the caps (the config was edited) and drops the oldest marks that no longer fit. Returns how many were dropped. */
    public int resize(int newCap, int newPerChunk) {
        int c = clampCap(newCap), pc = Math.max(1, Math.min(newPerChunk, c));
        if (c == cap && pc == perChunk) return 0;
        cap = c;
        perChunk = pc;
        long[] before = toArray();
        PlacedSet rebuilt = new PlacedSet(cap, perChunk);
        for (long k : before) rebuilt.addKey(k);
        int dropped = before.length - rebuilt.size();
        if (dropped > 0) {
            all.clear();
            byChunk.clear();
            for (long k : rebuilt.toArray()) {
                all.add(k);
                byChunk.computeIfAbsent(chunkOfKey(k), q -> new LinkedHashSet<>()).add(k);
            }
            evicted += dropped;
        }
        return dropped;
    }

    public static PlacedSet fromArray(long[] keys, int cap, int perChunk) {
        PlacedSet s = new PlacedSet(cap, perChunk);
        if (keys != null) for (long k : keys) s.addKey(k);
        return s;
    }
}
