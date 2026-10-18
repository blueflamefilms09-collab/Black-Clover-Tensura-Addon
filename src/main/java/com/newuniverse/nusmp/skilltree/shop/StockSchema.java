package com.newuniverse.nusmp.skilltree.shop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Why: {@code stock.json} is edited by people (the owner, a server owner with a datapack), and a typo in it must never reach the shelf as a price of zero, a negative
 * stock, or a book nobody can name, nor stop the game from loading. This is the one place that decides what a valid line is, written with no game type so that it is
 * unit tested with good files and deliberately bad ones. It never throws: every problem comes back as a readable sentence naming the line, the line is skipped when it is
 * broken (an unknown key only warns, because a typo in an optional key is usually harmless and the sentence tells the author), and the rest of the file still loads. The
 * limits are generous but finite on purpose: a price above 10^12 or a stock above 99 is a mistake, and a bound makes the loader, the roll and the packet sizes provable.
 */
public final class StockSchema {
    private StockSchema() {}

    /** At most this many lines are read. */
    public static final int MAX_ENTRIES = 64;
    /** The largest price a line may name (a trillion gold); far below {@link GoldLedger#CAP}, so a price can never be the thing that overflows a purse. */
    public static final long MAX_PRICE = 1_000_000_000_000L;
    /** The largest number of copies of one line on the shelf. */
    public static final int MAX_STOCK = 99;

    /** The result of reading a file: the valid lines in file order, and every problem met on the way. */
    public record Parsed(List<StockEntry> entries, List<String> problems) {}

    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]{0,47}");
    private static final Pattern ROUTE = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern SP_KIND = Pattern.compile("[a-z][a-z0-9_]{0,31}");
    private static final Set<String> KEYS = Set.of("id", "kind", "route", "routes", "sp_kind", "price_min", "price_max", "band_low", "stock", "weight", "flagship");

    /** Reads the text of a file. */
    public static Parsed parse(String json) {
        List<String> problems = new ArrayList<>();
        JsonElement root;
        try {
            root = JsonParser.parseString(json == null ? "" : json);
        } catch (JsonParseException | IllegalStateException e) {
            problems.add("not valid JSON: " + e.getMessage());
            return new Parsed(List.of(), problems);
        }
        return parse(root, problems);
    }

    /** Reads an already parsed file. */
    public static Parsed parse(JsonElement root) {
        return parse(root, new ArrayList<>());
    }

    private static Parsed parse(JsonElement root, List<String> problems) {
        if (root == null || !root.isJsonObject() || !root.getAsJsonObject().has("entries") || !root.getAsJsonObject().get("entries").isJsonArray()) {
            problems.add("the file must be an object with an \"entries\" array");
            return new Parsed(List.of(), problems);
        }
        JsonArray array = root.getAsJsonObject().getAsJsonArray("entries");
        List<StockEntry> out = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        Set<String> routes = new HashSet<>();
        boolean allSeen = false;
        for (int i = 0; i < array.size(); i++) {
            if (i >= MAX_ENTRIES) {
                problems.add("entries: only the first " + MAX_ENTRIES + " lines are read, " + (array.size() - MAX_ENTRIES) + " ignored");
                break;
            }
            String where = "entries[" + i + "]";
            StockEntry e = entry(array.get(i), where, problems);
            if (e == null) continue;
            where = where + " '" + e.id() + "'";
            if (!ids.add(e.id())) {
                problems.add(where + ": the id is used by an earlier line, skipped");
                continue;
            }
            if (e.allRoutes()) {
                if (allSeen) {
                    problems.add(where + ": only one line may stock all routes, skipped");
                    continue;
                }
                allSeen = true;
            } else if (e.kind() == StockEntry.Kind.ROUTE_BOOK && !routes.add(e.route())) {
                problems.add(where + ": the route " + e.route() + " already has a line, skipped");
                continue;
            }
            out.add(e);
        }
        return new Parsed(List.copyOf(out), problems);
    }

    private static StockEntry entry(JsonElement el, String where, List<String> problems) {
        if (el == null || !el.isJsonObject()) {
            problems.add(where + ": a line must be an object");
            return null;
        }
        JsonObject o = el.getAsJsonObject();
        for (Map.Entry<String, JsonElement> k : o.entrySet()) {
            if (k.getKey().startsWith("_")) continue;                 // a key that starts with an underscore is a comment (the repo's convention for data files): the file explains itself
            if (!KEYS.contains(k.getKey())) problems.add(where + ": unknown key '" + k.getKey() + "' (known: " + String.join(", ", new java.util.TreeSet<>(KEYS)) + ")");
        }
        String id = string(o, "id");
        if (id == null || !ID.matcher(id).matches()) {
            problems.add(where + ": \"id\" must be lower-case snake_case (letters, digits, underscore; it starts with a letter), skipped");
            return null;
        }
        where = where + " '" + id + "'";
        StockEntry.Kind kind = StockEntry.Kind.fromId(string(o, "kind"));
        if (kind == null) {
            problems.add(where + ": \"kind\" must be \"route_book\" or \"sp_book\", skipped");
            return null;
        }
        String route = "";
        String spKind = "";
        boolean all = false;
        if (kind == StockEntry.Kind.ROUTE_BOOK) {
            String r = string(o, "route");
            boolean wantsAll = o.has("routes");
            if (wantsAll) {
                if (!"all".equals(string(o, "routes"))) {
                    problems.add(where + ": \"routes\" can only be \"all\", skipped");
                    return null;
                }
                all = true;
            }
            if (all == (r != null)) {
                problems.add(where + ": a route book needs exactly one of \"route\" (one route id) or \"routes\": \"all\", skipped");
                return null;
            }
            if (r != null) {
                if (!ROUTE.matcher(r).matches()) {
                    problems.add(where + ": \"route\" must be a lower-case resource location like nusmp:fire_dragon_slayer, skipped");
                    return null;
                }
                route = r;
            }
            if (o.has("sp_kind")) problems.add(where + ": \"sp_kind\" means nothing on a route book");
        } else {
            String s = string(o, "sp_kind");
            if (s == null || !SP_KIND.matcher(s).matches()) {
                problems.add(where + ": an SP book needs \"sp_kind\" in lower-case snake_case (beginner_archery, magic_booster ...), skipped");
                return null;
            }
            spKind = s;
            if (o.has("route") || o.has("routes")) problems.add(where + ": \"route\" and \"routes\" mean nothing on an SP book");
        }
        Long min = whole(o, "price_min", where, problems);
        Long max = whole(o, "price_max", where, problems);
        if (min == null || max == null) return null;
        if (min < 1 || max < 1 || min > MAX_PRICE || max > MAX_PRICE) {
            problems.add(where + ": prices must be between 1 and " + MAX_PRICE + ", skipped");
            return null;
        }
        if (min > max) {
            problems.add(where + ": price_min " + min + " is above price_max " + max + ", skipped");
            return null;
        }
        double bandLow = 0.0;
        if (o.has("band_low")) {
            Double b = real(o.get("band_low"));
            if (b == null || b < 0.0 || b >= 1.0) {
                problems.add(where + ": \"band_low\" must be a number from 0 up to (not including) 1, skipped");
                return null;
            }
            bandLow = b;
        }
        int stockMin = 1;
        int stockMax = 1;
        if (o.has("stock")) {
            JsonElement s = o.get("stock");
            int[] range = stock(s);
            if (range == null) {
                problems.add(where + ": \"stock\" must be a number from 1 to " + MAX_STOCK + " or a [min, max] pair of them, skipped");
                return null;
            }
            stockMin = range[0];
            stockMax = range[1];
        }
        int weight = 100;
        if (o.has("weight")) {
            Long w = whole(o, "weight", where, problems);
            if (w == null || w < 0 || w > 100) {
                problems.add(where + ": \"weight\" is the chance in percent to be on the shelf, 0 to 100, skipped");
                return null;
            }
            weight = w.intValue();
        }
        boolean flagship = false;
        if (o.has("flagship")) {
            JsonElement f = o.get("flagship");
            if (f.isJsonPrimitive() && f.getAsJsonPrimitive().isBoolean()) flagship = f.getAsBoolean();
            else problems.add(where + ": \"flagship\" must be true or false, ignored");
        }
        return new StockEntry(id, kind, route, all, spKind, min, max, bandLow, stockMin, stockMax, weight, flagship);
    }

    private static String string(JsonObject o, String name) {
        JsonElement e = o.get(name);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
    }

    /** A whole number, or null (with a problem written) when the key is missing or is not one. */
    private static Long whole(JsonObject o, String name, String where, List<String> problems) {
        JsonElement e = o.get(name);
        if (e == null) {
            problems.add(where + ": \"" + name + "\" is missing, skipped");
            return null;
        }
        Long v = whole(e);
        if (v == null) problems.add(where + ": \"" + name + "\" must be a whole number, skipped");
        return v;
    }

    private static Long whole(JsonElement e) {
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return null;
        try {
            BigDecimal d = e.getAsBigDecimal().stripTrailingZeros();
            if (d.scale() > 0) return null;
            return d.longValueExact();
        } catch (NumberFormatException | ArithmeticException bad) {
            return null;
        }
    }

    private static Double real(JsonElement e) {
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return null;
        double d = e.getAsDouble();
        return Double.isFinite(d) ? d : null;
    }

    /** {@code 3} or {@code [3, 5]} to a pair {@code {min, max}} within 1..{@value #MAX_STOCK}, or null. */
    private static int[] stock(JsonElement e) {
        if (e.isJsonPrimitive()) {
            Long n = whole(e);
            return n == null || n < 1 || n > MAX_STOCK ? null : new int[]{n.intValue(), n.intValue()};
        }
        if (!e.isJsonArray() || e.getAsJsonArray().size() != 2) return null;
        Long lo = whole(e.getAsJsonArray().get(0));
        Long hi = whole(e.getAsJsonArray().get(1));
        if (lo == null || hi == null || lo < 1 || hi > MAX_STOCK || lo > hi) return null;
        return new int[]{lo.intValue(), hi.intValue()};
    }
}
