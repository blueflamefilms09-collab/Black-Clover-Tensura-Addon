package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.skilltree.data.RouteIndex;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;

import java.util.List;

/**
 * Why: the drop code needs "which line does this book open" for every route book of every kill and chest, with 283 lines, and must not walk or copy the registry's list each time. The
 * registry already publishes its lines as ONE immutable {@link RouteIndex} snapshot that is replaced as a whole on a reload or a client sync, so that snapshot's identity is the
 * registry version. This holds the routes in index order and their cumulative {@code loot_weight} table ({@link WeightedTable}) for the snapshot in force, built once per snapshot
 * through a {@link VersionedCache}, so a drop costs a binary search and no allocation, and a datapack reload (or a line added later) is picked up on the very next drop with no
 * invalidation call to forget. Server side in practice (the client never drops loot), but it reads the same facade the rest of the tree uses.
 */
public final class RouteTable {
    private RouteTable() {}

    /** The lines of one registry version and their weight table: {@code routes[i]} is the line of table index {@code i}. */
    public record Snap(SkillRoute[] routes, WeightedTable table) {
        public static final Snap EMPTY = new Snap(new SkillRoute[0], WeightedTable.EMPTY);

        /** The line at a picked index, or null for -1 / out of range. */
        public SkillRoute at(int index) { return index < 0 || index >= routes.length ? null : routes[index]; }
    }

    private static final VersionedCache<RouteIndex, Snap> CACHE = new VersionedCache<>(RouteTable::build);

    private static Snap build(RouteIndex index) {
        List<SkillRoute> all = index == null ? List.of() : index.all();
        SkillRoute[] routes = all.toArray(new SkillRoute[0]);
        int[] weights = new int[routes.length];
        for (int i = 0; i < routes.length; i++) weights[i] = routes[i].lootWeight();
        return new Snap(routes, WeightedTable.of(weights));
    }

    /** The table of the registry version in force (built on the first call after a change, then shared). */
    public static Snap current() { return CACHE.get(RouteRegistry.snapshot()); }

    /** How many times the table was rebuilt (a self-test and a unit test read it: a thousand drops must not rebuild). */
    public static int builds() { return CACHE.builds(); }

    /** Drops the cached table (the server stopped; the next world starts from nothing). */
    public static void clear() { CACHE.clear(); }
}
