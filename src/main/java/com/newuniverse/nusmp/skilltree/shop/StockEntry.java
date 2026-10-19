package com.newuniverse.nusmp.skilltree.shop;

/**
 * Why: one line of the witch's shelf, as the data file {@code data/nusmp/black_market/stock.json} states it, kept as a plain immutable record with no game types so the
 * validator, the price roll and the shelf builder can all be unit tested without a running game. The shelf is DATA (not code) for the same reason the skill trees are: the
 * owner wants to retune a price band, add a primer or remove a book with a datapack and {@code /reload}, with no rebuild. A line describes what MAY be on the shelf and at
 * what terms (a price band, a stock range, a chance to appear); what actually IS on it this restock is decided by {@link StockRoll} from the world seed and the restock period.
 *
 * @param id         the line's id, lower-case snake; it is the key of the purchases counted against this line, so renaming it forgets what was bought this period
 * @param kind       a Skill Book that opens a route, or an SP Book that gives Skill Points
 * @param route      the route id of a single route book ({@code nusmp:lady_luck}), or empty
 * @param allRoutes  true for the line that stocks one book for EVERY route of the registry that has no line of its own, so a new magic added by a datapack is on sale with no edit here
 * @param spKind     the kind of an SP Book ({@code magic_booster}), or empty
 * @param priceMin   the cheapest the line may be rolled at, in gold
 * @param priceMax   the dearest the line may be rolled at, in gold
 * @param bandLow    the lowest share of the band the roll may land on (0 = the whole band, 0.6667 = only the top third): how the flagship is kept at the top of the market
 * @param stockMin   the fewest copies a restock puts out
 * @param stockMax   the most copies a restock puts out
 * @param weight     the chance in percent (0 to 100) that the line is on the shelf at a restock at all
 * @param flagship   true to show the line with the gold frame of the shop's showpiece
 */
public record StockEntry(String id, Kind kind, String route, boolean allRoutes, String spKind, long priceMin, long priceMax, double bandLow,
                         int stockMin, int stockMax, int weight, boolean flagship) {

    /** What a line sells. Only ever APPEND entries (the ids are written in data files). */
    public enum Kind {
        ROUTE_BOOK("route_book"), SP_BOOK("sp_book");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        /** The name used in the data file. */
        public String id() { return id; }

        /** The kind with this file name, or null. */
        public static Kind fromId(String id) {
            if (id != null) for (Kind k : values()) if (k.id.equals(id)) return k;
            return null;
        }
    }

    /** The lowest price of the line's band that the roll may land on: the band start, lifted by {@link #bandLow}. */
    public double bandStart() {
        return priceMin + (priceMax - priceMin) * bandLow;
    }
}
