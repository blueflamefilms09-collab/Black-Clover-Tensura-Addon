package com.newuniverse.nusmp.book;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Recombination Magic, no lag on a public server: the caps of everything the magic puts in the world, and the small pure ledger that enforces them. Pure (no game classes), so JUnit pins it
 * ({@code RecombinationBudgetTest}); {@code prop.RecombinationProps} owns one ledger per kind of construct per level and evicts the OLDEST construct (it ends, as if its time had run out) when a new one
 * would go over a cap, so a spell that is cast again and again, or by many players, can never pile up unbounded entities, blocks or ticks.
 * <ul>
 *   <li>Golems (the Raging Black Bull, one per caster already): at most {@link #GOLEMS_PER_OWNER} per caster and {@link #GOLEMS_PER_LEVEL} per dimension; each one scans for foes at most every
 *       {@code entityScanInterval} ticks (never every tick), at most {@link #FOE_SCAN_MAX} foes are looked at, a volley hits at most 5 and a ram 6.</li>
 *   <li>Rooms (Bulwark's four corners, Room Swap's two ends, Room Rebuild's one): {@link #ROOMS_PER_OWNER} per caster, {@link #ROOMS_PER_LEVEL} per dimension; each lives 30 to 200 ticks.</li>
 *   <li>Rings of boards (Plank Snare, the flourish of the book's effects): {@link #BOARDS_PER_OWNER} / {@link #BOARDS_PER_LEVEL}. Drills (Mana Corkscrew): {@link #DRILLS_PER_OWNER} / {@link #DRILLS_PER_LEVEL}.</li>
 *   <li>Blocks: Bulwark places at most {@link #BULWARK_MAX_BLOCKS} temporary blocks per cast, at most {@link #BULWARK_BLOCKS_PER_TICK} in one tick (the rest over the next ticks), and only into air or
 *       replaceable blocks; they are removed by the spell runtime after 200 ticks.</li>
 *   <li>A construct whose caster logged out, died or left the dimension ends within {@link #OWNER_CHECK_TICKS} ticks (the Bull at once).</li>
 * </ul>
 */
public final class RecombinationBudget {
    private RecombinationBudget() {}

    public static final int GOLEMS_PER_OWNER = 1, GOLEMS_PER_LEVEL = 12;
    public static final int ROOMS_PER_OWNER = 8, ROOMS_PER_LEVEL = 48;
    public static final int BOARDS_PER_OWNER = 3, BOARDS_PER_LEVEL = 24;
    public static final int DRILLS_PER_OWNER = 3, DRILLS_PER_LEVEL = 24;
    public static final int FOE_SCAN_MAX = 16;
    public static final int BULWARK_MAX_BLOCKS = 32, BULWARK_BLOCKS_PER_TICK = 8;
    public static final int OWNER_CHECK_TICKS = 10;

    /** The kinds of construct the ledgers count (an index into the per-level ledger set). */
    public static final int GOLEM = 0, ROOM = 1, BOARDS = 2, DRILL = 3;

    public static int perOwner(int kind) {
        switch (kind) {
            case GOLEM: return GOLEMS_PER_OWNER;
            case ROOM: return ROOMS_PER_OWNER;
            case BOARDS: return BOARDS_PER_OWNER;
            default: return DRILLS_PER_OWNER;
        }
    }

    public static int perLevel(int kind) {
        switch (kind) {
            case GOLEM: return GOLEMS_PER_LEVEL;
            case ROOM: return ROOMS_PER_LEVEL;
            case BOARDS: return BOARDS_PER_LEVEL;
            default: return DRILLS_PER_LEVEL;
        }
    }

    /** How many ticks over which {@code n} blocks are placed, at most {@link #BULWARK_BLOCKS_PER_TICK} in one tick (0 = all in the first tick, 1 = over two ticks ...). */
    public static int placementTicks(int n) {
        int capped = Math.min(Math.max(0, n), BULWARK_MAX_BLOCKS);
        return capped <= BULWARK_BLOCKS_PER_TICK ? 0 : (capped - 1) / BULWARK_BLOCKS_PER_TICK;
    }

    /** Temporary blocks alive at once: per caster (two overlapping Bulwarks) and in the whole server. */
    public static final int BULWARK_LIVE_PER_OWNER = 64, BULWARK_LIVE_PER_SERVER = 512;

    /**
     * Counts the temporary blocks the magic has standing (a Bulwark puts up to 32 for 200 ticks): a cast that would put more than {@link #BULWARK_LIVE_PER_OWNER} of one caster or
     * {@link #BULWARK_LIVE_PER_SERVER} on the server standing is refused. Work is done at cast time only (a cast has a cooldown of 160 ticks), never per tick: old batches are dropped when the game time
     * has passed their end. Server thread only; the list never holds more than the caps allow (every batch is at least one block).
     */
    public static final class BlockLedger {
        private final java.util.ArrayList<long[]> batches = new java.util.ArrayList<>();           // {until tick, blocks}
        private final java.util.ArrayList<Object> owners = new java.util.ArrayList<>();

        public int live(long now) {
            prune(now);
            int n = 0;
            for (long[] b : batches) n += (int) b[1];
            return n;
        }

        public int liveOf(Object owner, long now) {
            prune(now);
            int n = 0;
            for (int i = 0; i < batches.size(); i++) if (owners.get(i).equals(owner)) n += (int) batches.get(i)[1];
            return n;
        }

        private void prune(long now) {
            for (int i = batches.size() - 1; i >= 0; i--) if (batches.get(i)[0] <= now) {
                batches.remove(i);
                owners.remove(i);
            }
        }

        /** Books {@code blocks} for {@code ticks} ticks if the caps allow; false (nothing booked) when they would be passed. */
        public boolean tryAdd(Object owner, long now, int blocks, int ticks) {
            if (blocks <= 0) return true;
            if (live(now) + blocks > BULWARK_LIVE_PER_SERVER || liveOf(owner, now) + blocks > BULWARK_LIVE_PER_OWNER) return false;
            batches.add(new long[]{now + ticks, blocks});
            owners.add(owner);
            return true;
        }
    }

    /**
     * The ledger of one kind of construct in one level: which keys (entity ids) are alive and whose they are. Adding one more than a cap allows hands back the oldest keys to end. Server thread only
     * (nothing is synchronised), a bounded size by construction: never more than {@code perLevel} keys.
     */
    public static final class Ledger<K> {
        private final int perOwner, perLevel;
        private final ArrayDeque<K> order = new ArrayDeque<>();
        private final Map<K, Object> ownerOf = new HashMap<>();

        public Ledger(int perOwner, int perLevel) {
            this.perOwner = Math.max(1, perOwner);
            this.perLevel = Math.max(this.perOwner, perLevel);
        }

        public int size() { return order.size(); }

        public boolean has(K key) { return ownerOf.containsKey(key); }

        /** Forgets every key the predicate says is gone (an entity removed some other way). */
        public void retain(Predicate<K> alive) {
            for (Iterator<K> it = order.iterator(); it.hasNext(); ) {
                K k = it.next();
                if (!alive.test(k)) {
                    it.remove();
                    ownerOf.remove(k);
                }
            }
        }

        /** Records a new construct of {@code owner} (null = nobody's) and returns the keys that must end now, oldest first (never {@code key} itself). */
        public List<K> add(Object owner, K key) {
            List<K> evict = new ArrayList<>(2);
            if (ownerOf.containsKey(key)) return evict;
            order.addLast(key);
            ownerOf.put(key, owner == null ? NOBODY : owner);
            Object o = owner == null ? NOBODY : owner;
            int mine = 0;
            for (K k : order) if (ownerOf.get(k).equals(o)) mine++;
            for (Iterator<K> it = order.iterator(); it.hasNext() && mine > perOwner; ) {
                K k = it.next();
                if (k != key && ownerOf.get(k).equals(o)) {
                    it.remove();
                    ownerOf.remove(k);
                    evict.add(k);
                    mine--;
                }
            }
            for (Iterator<K> it = order.iterator(); it.hasNext() && order.size() > perLevel; ) {
                K k = it.next();
                if (k != key) {
                    it.remove();
                    ownerOf.remove(k);
                    evict.add(k);
                }
            }
            return evict;
        }

        public void remove(K key) {
            if (ownerOf.remove(key) != null) order.remove(key);
        }

        private static final Object NOBODY = new Object();
    }
}
