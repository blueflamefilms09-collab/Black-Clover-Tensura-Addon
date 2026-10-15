package com.newuniverse.nusmp.labyrinth.seal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongPredicate;

/**
 * Why: a Gatekeeper doorway is a column pair (2 wide, 2..4 tall) of independent blocks with no block entity and no stored neighbour
 * offsets, so "open the door" has to find the other blocks of the same door at the moment the key turns. A 6-connected breadth-first
 * flood from the clicked block over "is a closed Gatekeeper block of the same kind" finds every block of that doorway whatever rotation
 * Jigsaw gave the template, and stops at the first block that is not part of it (masonry, air, a door of the other kind). The flood is
 * written over opaque {@code long} position keys and a {@link LongPredicate} so it has no Minecraft dependency and is unit-tested
 * ({@code DoorFloodTest}); the block supplies the predicate as a lambda that reads the level. The limit is a hard cap on the number of
 * positions collected (the start counts), so a corrupted or hand-built wall of door blocks cannot turn one click into a chunk-wide write.
 */
public final class DoorFlood {
    private DoorFlood() {}

    /** The most blocks one key opens: a 2 x 4 doorway is 8; 64 leaves room for a grand triple arch and still bounds the work. */
    public static final int LIMIT = 64;

    private static final int BITS = 21, MASK = (1 << BITS) - 1;

    /** Packs a block position into one key (21 signed bits per axis: +-1,048,575, far beyond the world border). */
    public static long pack(int x, int y, int z) {
        return ((long) (x & MASK) << (2 * BITS)) | ((long) (y & MASK) << BITS) | (long) (z & MASK);
    }

    public static int x(long key) { return signExtend((int) (key >>> (2 * BITS)) & MASK); }

    public static int y(long key) { return signExtend((int) (key >>> BITS) & MASK); }

    public static int z(long key) { return signExtend((int) key & MASK); }

    private static int signExtend(int v) { return (v << (32 - BITS)) >> (32 - BITS); }

    /** The six face neighbours of a key, in a fixed order (+x, -x, +y, -y, +z, -z) so the flood is deterministic. */
    public static long[] neighbours(long key) {
        int x = x(key), y = y(key), z = z(key);
        return new long[]{pack(x + 1, y, z), pack(x - 1, y, z), pack(x, y + 1, z), pack(x, y - 1, z), pack(x, y, z + 1), pack(x, y, z - 1)};
    }

    /**
     * Breadth-first flood from {@code start} over the positions {@code member} accepts, 6-connected (faces only, never diagonals), at most
     * {@code limit} positions in all. The start is included only if it is a member; the result is in visiting order (start first).
     */
    public static List<Long> flood(long start, LongPredicate member, int limit) {
        List<Long> out = new ArrayList<>();
        if (limit <= 0 || !member.test(start)) return out;
        Set<Long> seen = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && out.size() < limit) {
            long cur = queue.poll();
            out.add(cur);
            if (out.size() >= limit) break;
            for (long n : neighbours(cur)) {
                if (seen.contains(n)) continue;
                seen.add(n);                                     // tested once, member or not: a non-member is a wall, never looked at again
                if (member.test(n)) queue.add(n);
            }
        }
        return out;
    }
}
