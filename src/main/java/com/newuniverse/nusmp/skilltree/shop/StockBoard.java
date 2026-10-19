package com.newuniverse.nusmp.skilltree.shop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    /** One place on the shelf: a stable id, the line it comes from, and what it sells (the route id of a route book, the kind of an SP book). */
    public record Slot(String id, StockEntry entry, String ref) {}

    /** One offer on the shelf right now. */
    public record Offer(String id, StockEntry.Kind kind, String ref, long price, int total, int bought, boolean flagship) {
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
        Set<String> named = new HashSet<>();
        for (StockEntry e : entries) if (e.kind() == StockEntry.Kind.ROUTE_BOOK && !e.allRoutes()) named.add(e.route());
        List<Slot> out = new ArrayList<>();
        for (StockEntry e : entries) {
            if (e.kind() == StockEntry.Kind.SP_BOOK) {
                continue;                                                       // 1.96.0: the SP Books are gone with the Skill Points; an old stock line for one makes no slot
            } else if (e.allRoutes()) {
                for (String r : routeIds) if (!named.contains(r)) add(out, e.id() + "@" + r, e, r);
            } else if (routeIds.contains(e.route())) {
                add(out, e.id(), e, e.route());
            }
        }
        return out;
    }

    private static void add(List<Slot> out, String id, StockEntry entry, String ref) {
        if (id.length() <= MAX_ID && ref.length() <= MAX_ID) out.add(new Slot(id, entry, ref));
    }

    /** The shelf of one restock: the slots that rolled "present", with their rolled price and count and what was already bought. */
    public static List<Offer> build(long seed, long period, int epoch, List<Slot> slots, ToIntFunction<String> bought) {
        List<Offer> out = new ArrayList<>();
        for (Slot s : slots) {
            StockRoll.Roll r = StockRoll.roll(seed, period, epoch, s.id(), s.entry());
            if (!r.present()) continue;
            out.add(new Offer(s.id(), s.entry().kind(), s.ref(), r.price(), r.count(), Math.max(0, bought.applyAsInt(s.id())), s.entry().flagship()));
        }
        return out;
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
