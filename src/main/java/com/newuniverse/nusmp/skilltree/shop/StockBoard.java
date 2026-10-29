package com.newuniverse.nusmp.skilltree.shop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Why: the shelf the player sees is the data file's lines, expanded against the routes that exist, rolled for this restock, minus what has already been bought. Doing that as
 * a pure function of plain values ({@code build}) means the shelf is never stored whole: the save holds only the restock period, the restock counter and a bought-count per
 * slot (see {@code MarketState}), and everything else is recomputed on demand, so a data reload or a new route changes the shelf exactly as the file says, an old world has
 * nothing to migrate, and the self-test can rebuild the shelf from the real registry and check it. The restock clock is counted in game ticks of the overworld (24000 per
 * in-game day) and NOT in day time: {@code /time set}, the daylight cycle rule and a night skipped by sleeping must neither freeze nor rush the market.
 */
public final class StockBoard {
    private StockBoard() {}

    /** Ticks in one in-game day. */
    public static final long DAY = 24_000L;

    /** The longest slot id or ref the shop's packet can carry; a slot with a longer one is left off the shelf (see {@link #slots}) instead of making the whole picture unsendable. */
    public static final int MAX_ID = 96;

    /** The most slots one shop's shelf ever holds; the lines past it are left off (the shelf, its packet and its roll stay bounded whatever a datapack says). */
    public static final int MAX_SLOTS = 32;

    /** One item of an ITEM slot's pool: its id and the biggest stack it comes in. */
    public record PoolItem(String id, int maxStack) {}

    /**
     * One place on the shelf: a stable id, the line it comes from, and what it sells (the route id of a route book, the kind of an SP book). An ITEM slot also carries its {@code pool}, the
     * sorted items that exist, belong to the shop's type and cannot be sold back for more than they cost; the slot rolls one of them at every restock. {@code ref} is empty for an ITEM slot.
     */
    public record Slot(String id, StockEntry entry, String ref, List<PoolItem> pool) {
        public Slot(String id, StockEntry entry, String ref) {
            this(id, entry, ref, List.of());
        }
    }

    /**
     * One offer on the shelf right now. {@code price} is what ONE purchase costs (before the server's price scale) and {@code amount} is how many items that purchase hands over (always 1 for a
     * book; the rolled stack size for an item). For an ITEM offer {@code ref} is the rolled item id.
     */
    public record Offer(String id, StockEntry.Kind kind, String ref, long price, int total, int bought, boolean flagship, int amount) {
        public Offer(String id, StockEntry.Kind kind, String ref, long price, int total, int bought, boolean flagship) {
            this(id, kind, ref, price, total, bought, flagship, 1);
        }

        /** Copies still for sale. */
        public int left() { return Math.max(0, total - bought); }
    }

    /**
     * Expands the lines into slots, in file order. A line naming one route makes a slot only when that route exists; the all-routes line makes one slot per route that has no line
     * of its own (so the flagship is never offered twice); an SP line makes one slot. A slot whose id or ref is longer than {@link #MAX_ID} is skipped (the packet could not carry
     * it; route ids are short, so this only ever guards against a freak datapack).
     *
     * @param routeIds the ids of the routes in the registry
     */
    public static List<Slot> slots(List<StockEntry> entries, List<String> routeIds) {
        return slots(entries, routeIds, e -> List.of(), "");
    }

    /**
     * The same, for a shop that sells items: an ITEM line makes one slot when {@code pools} answers a non-empty pool for it (a line whose tag is empty or unknown makes none), the slot ids get
     * {@code idPrefix} (empty for the witch, whose purchases are already counted under the bare ids; another shop passes {@code "<shop>/"} so two shops never share a counter), and the shelf is
     * cut at {@link #MAX_SLOTS}.
     */
    public static List<Slot> slots(List<StockEntry> entries, List<String> routeIds, Function<StockEntry, List<PoolItem>> pools, String idPrefix) {
        Set<String> named = new HashSet<>();
        for (StockEntry e : entries) if (e.kind() == StockEntry.Kind.ROUTE_BOOK && !e.allRoutes()) named.add(e.route());
        List<Slot> out = new ArrayList<>();
        for (StockEntry e : entries) {
            if (out.size() >= MAX_SLOTS) break;
            if (e.kind() == StockEntry.Kind.ITEM) {
                List<PoolItem> pool = pools.apply(e);
                if (pool != null && !pool.isEmpty()) addItem(out, idPrefix + e.id(), e, pool);
            } else if (e.kind() == StockEntry.Kind.SP_BOOK) {
                continue;                                                       // 1.96.0: the SP Books are gone with the Skill Points; an old stock line for one makes no slot
            } else if (e.allRoutes()) {
                for (String r : routeIds) if (!named.contains(r)) add(out, e.id() + "@" + r, e, r);
            } else if (routeIds.contains(e.route())) {
                add(out, e.id(), e, e.route());
            }
        }
        return out;
    }

    private static void addItem(List<Slot> out, String id, StockEntry entry, List<PoolItem> pool) {
        if (id.length() <= MAX_ID) out.add(new Slot(id, entry, "", pool));
    }

    private static void add(List<Slot> out, String id, StockEntry entry, String ref) {
        if (id.length() <= MAX_ID && ref.length() <= MAX_ID) out.add(new Slot(id, entry, ref));
    }

    /** The shelf of one restock: the slots that rolled "present", with their rolled price and count and what was already bought. */
    public static List<Offer> build(long seed, long period, int epoch, List<Slot> slots, ToIntFunction<String> bought) {
        List<Offer> out = new ArrayList<>();
        Set<String> taken = new HashSet<>();                                           // the items already on this shelf: two slots never put out the same item
        for (Slot s : slots) {
            StockRoll.Roll r = StockRoll.roll(seed, period, epoch, s.id(), s.entry());
            if (!r.present()) continue;
            int bo = Math.max(0, bought.applyAsInt(s.id()));
            if (s.entry().kind() != StockEntry.Kind.ITEM) {
                out.add(new Offer(s.id(), s.entry().kind(), s.ref(), r.price(), r.count(), bo, s.entry().flagship()));
                continue;
            }
            Offer o = itemOffer(seed, period, epoch, s, r, bo, taken);
            if (o != null) out.add(o);
        }
        return out;
    }

    /**
     * The offer of an ITEM slot: the rolled pool member (the next member when an earlier slot already put that one out; none free = no offer), a stack size inside the line's count range capped by
     * the item's own stack size, and the price of the whole stack (the rolled price is PER ITEM, so a bigger stack costs more and the price per item stays inside the line's band).
     */
    private static Offer itemOffer(long seed, long period, int epoch, Slot s, StockRoll.Roll r, int bought, Set<String> taken) {
        StockEntry e = s.entry();
        StockRoll.ItemRoll ir = StockRoll.rollItem(seed, period, epoch, s.id(), s.pool().size(), e.countMin(), e.countMax());
        for (int k = 0; k < s.pool().size(); k++) {
            PoolItem item = s.pool().get((ir.index() + k) % s.pool().size());
            if (!taken.add(item.id())) continue;
            int amount = Math.max(1, Math.min(ir.amount(), Math.max(1, item.maxStack())));
            return new Offer(s.id(), e.kind(), item.id(), r.price() * amount, r.count(), bought, e.flagship(), amount);
        }
        return null;
    }

    /** The restock period a game time falls in: periods are {@code restockDays} in-game days long, counted from the creation of the world. */
    public static long period(long gameTime, int restockDays) {
        return Math.max(0, gameTime) / periodTicks(restockDays);
    }

    /** Ticks left until the next restock. */
    public static long ticksUntilRestock(long gameTime, int restockDays) {
        long t = Math.max(0, gameTime);
        long len = periodTicks(restockDays);
        return (t / len + 1) * len - t;
    }

    /** The in-game day number a game time falls in (the unit of the per-player daily limit). */
    public static long dayOf(long gameTime) {
        return Math.max(0, gameTime) / DAY;
    }

    private static long periodTicks(int restockDays) {
        return DAY * Math.max(1, restockDays);
    }

    /** The offer with this id, or null. */
    public static Offer find(List<Offer> offers, String id) {
        if (id == null) return null;
        for (Offer o : offers) if (o.id().equals(id)) return o;
        return null;
    }
}
