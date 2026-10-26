package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.skilltree.data.RouteIndex;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.rarity.BookFacts;
import com.newuniverse.nusmp.skilltree.rarity.BookRarity;
import com.newuniverse.nusmp.skilltree.rarity.BlockedFranchises;

import java.util.List;

/**
 * Why: the drop code needs "which line does this book open" for every route book of every kill and chest, with 254 lines, and must not walk or copy the registry's list each time. The
 * registry already publishes its lines as ONE immutable {@link RouteIndex} snapshot that is replaced as a whole on a reload or a client sync, so that snapshot's identity is the
 * registry version. This holds the routes in index order and their cumulative {@code loot_weight} table ({@link WeightedTable}) for the snapshot in force, built once per snapshot
 * through a {@link VersionedCache}, so a drop costs a binary search and no allocation, and a datapack reload (or a line added later) is picked up on the very next drop with no
 * invalidation call to forget. Server side in practice (the client never drops loot), but it reads the same facade the rest of the tree uses.
 *
 * <p>3.26.0 adds two things to the same cached build, neither of which costs anything per drop. (1) The franchise BLACKLIST: the weight of a line of a blocked franchise is zeroed in
 * every table, so no door that picks through this class can ever hand it out (the route files and the held books are untouched). The cache key is the registry snapshot AND the effective setting
 * text ({@link BlockedFranchises#setting()}), so the tables are rebuilt when either changes and never per drop: a steady state allocates nothing (the key object is kept while neither changed).
 * (2) One weight table PER RARITY over the same routes array ({@link Snap#rarityTable}): the same weights, zeroed for the lines of every other rarity, so a tier that has drawn a rarity picks a
 * line of exactly that rarity, still weighted by {@code loot_weight} and still reusing the pity code. A route the book table does not know (a data pack's) is common.
 */
public final class RouteTable {
    private RouteTable() {}

    /** The lines of one registry version and their weight tables: {@code routes[i]} is the line of table index {@code i}; {@code byRarity[r]} is the table of the lines of rank {@code r}. */
    public record Snap(SkillRoute[] routes, WeightedTable table, WeightedTable[] byRarity) {
        public static final Snap EMPTY = new Snap(new SkillRoute[0], WeightedTable.EMPTY);

        /** The two-table form of 3.25 (no rarity tables: every {@link #rarityTable} is empty); kept so old callers compile. */
        public Snap(SkillRoute[] routes, WeightedTable table) { this(routes, table, new WeightedTable[0]); }

        /** The line at a picked index, or null for -1 / out of range. */
        public SkillRoute at(int index) { return index < 0 || index >= routes.length ? null : routes[index]; }

        /** The weight table of the lines of one rarity (empty when none can drop). Indices are those of {@link #routes}. */
        public WeightedTable rarityTable(BookRarity rarity) {
            int r = rarity == null ? 0 : rarity.rank();
            return r < byRarity.length ? byRarity[r] : WeightedTable.EMPTY;
        }
    }

    /** What a build is made from: the registry version and the blacklist text in force. Compared by identity, like the registry snapshot. */
    private record Key(RouteIndex index, String setting) {}

    private static volatile Key lastKey;

    private static final VersionedCache<Key, Snap> CACHE = new VersionedCache<>(RouteTable::build);

    private static Snap build(Key key) {
        List<SkillRoute> all = key.index() == null ? List.of() : key.index().all();
        var blocked = BlockedFranchises.parse(key.setting());
        SkillRoute[] routes = all.toArray(new SkillRoute[0]);
        int n = routes.length;
        int[] weights = new int[n];
        BookRarity[] rarity = new BookRarity[n];
        for (int i = 0; i < n; i++) {
            boolean off = !blocked.isEmpty() && BlockedFranchises.blocked(blocked, BookFacts.franchiseOf(routes[i].id()));
            weights[i] = off ? 0 : routes[i].lootWeight();
            rarity[i] = BookFacts.rarityOf(routes[i].id());
        }
        WeightedTable[] byRarity = new WeightedTable[BookRarity.count()];
        for (int r = 0; r < byRarity.length; r++) {
            int[] w = new int[n];
            for (int i = 0; i < n; i++) w[i] = rarity[i].rank() == r ? weights[i] : 0;
            byRarity[r] = WeightedTable.of(w);
        }
        return new Snap(routes, WeightedTable.of(weights), byRarity);
    }

    /** The table of the registry version and the blacklist in force (built on the first call after either changes, then shared). */
    public static Snap current() {
        RouteIndex index = RouteRegistry.snapshot();
        String setting = BlockedFranchises.setting();
        Key k = lastKey;
        if (k == null || k.index() != index || !k.setting().equals(setting)) {
            k = new Key(index, setting);
            lastKey = k;
        }
        return CACHE.get(k);
    }

    /** How many times the table was rebuilt (a self-test and a unit test read it: a thousand drops must not rebuild). */
    public static int builds() { return CACHE.builds(); }

    /** Drops the cached table (the server stopped; the next world starts from nothing). */
    public static void clear() {
        lastKey = null;
        CACHE.clear();
    }
}
