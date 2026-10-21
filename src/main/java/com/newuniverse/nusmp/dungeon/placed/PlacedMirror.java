package com.newuniverse.nusmp.dungeon.placed;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: the client decides whether to START the crack animation of a block (a client that predicts "unbreakable" never sends the mining packets at all, see
 * {@code DungeonGuard.predictsClient}), and it cannot read the server's saved list of player-placed blocks. So the server tells every client which blocks
 * round it are player-placed ({@link PlacedNet}: the marks of a chunk when the chunk is sent, then one small packet per placement or removal) and this holds
 * what it was told, per dimension. It is only a hint for the animation: the server decides, and a stale or missing entry costs a crack that does not start
 * or a crack that is refused, never a block that breaks. No game classes here, so it is safe to load on a dedicated server. Cleared when the player leaves
 * a world.
 */
public final class PlacedMirror {
    private PlacedMirror() {}

    /** Client-side caps, generous: the server's caps bound what it ever sends. */
    private static final int CAP = 1 << 17, PER_CHUNK = 1 << 14;
    private static final Map<String, PlacedSet> BY_DIM = new HashMap<>();

    public static synchronized boolean contains(String dim, int x, int y, int z) {
        PlacedSet s = BY_DIM.get(dim);
        return s != null && s.contains(x, y, z);
    }

    public static synchronized void replaceChunk(String dim, int cx, int cz, long[] keys) {
        PlacedSet s = BY_DIM.get(dim);
        if (s == null) {
            if (keys == null || keys.length == 0) return;
            s = new PlacedSet(CAP, PER_CHUNK);
            BY_DIM.put(dim, s);
        }
        s.replaceChunk(cx, cz, keys);
    }

    public static synchronized void add(String dim, long[] keys) {
        if (keys == null || keys.length == 0) return;
        PlacedSet s = BY_DIM.computeIfAbsent(dim, d -> new PlacedSet(CAP, PER_CHUNK));
        for (long k : keys) s.addKey(k);
    }

    public static synchronized void remove(String dim, long[] keys) {
        PlacedSet s = BY_DIM.get(dim);
        if (s == null || keys == null) return;
        for (long k : keys) s.removeKey(k);
    }

    public static synchronized int size(String dim) {
        PlacedSet s = BY_DIM.get(dim);
        return s == null ? 0 : s.size();
    }

    public static synchronized void clear() { BY_DIM.clear(); }
}
