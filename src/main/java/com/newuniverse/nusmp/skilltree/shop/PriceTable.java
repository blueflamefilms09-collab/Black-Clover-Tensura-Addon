package com.newuniverse.nusmp.skilltree.shop;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Why: the witch BUYS the dungeon's goods, and what she pays for each item is the whole economy of the shop (it is how a player earns the millions a route book costs), so the
 * numbers are DATA in {@code data/nusmp/black_market/prices.json} that the owner can retune or a server owner can replace with a datapack, never constants in code. This class
 * is that table as an immutable value with no game types (the items are plain resource-location strings, so a price may name an item of an addon that is not installed: the game
 * side drops the rows whose item does not exist), plus the reader that validates the file. It never throws on a bad file: a broken row is skipped with a readable sentence and
 * the rest still loads. A row is either a bare number (gold per item) or {@code {"gold": n, "category": "stones"}}; the category only groups rows and has no effect on the price.
 * The file's {@code "bulk"} list names the categories that get a "Sell all" button: a pile of Magic Stones or monster meat is sold in one click, but a nether star or an
 * enchanted golden apple is something the player should choose one at a time, so its category is left out of the list and the server refuses a bulk sale of it.
 */
public final class PriceTable {
    /** The category of a row that names none. */
    public static final String DEFAULT_CATEGORY = "goods";
    /** The most any single item may be worth, in gold (a trillion), far below the purse cap. */
    public static final long MAX_PRICE = 1_000_000_000_000L;
    /** At most this many rows are read. */
    public static final int MAX_ROWS = 512;
    /** The empty table: the witch buys nothing. */
    public static final PriceTable EMPTY = new PriceTable(Map.of(), Set.of());

    /** What one item sells for and which button group it belongs to. */
    public record Price(long gold, String category) {}

    /** The result of reading a file: the table of the valid rows and every problem met on the way. */
    public record Parsed(PriceTable table, List<String> problems) {}

    private static final Pattern ITEM = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern CATEGORY = Pattern.compile("[a-z][a-z0-9_]{0,23}");

    private final Map<String, Price> rows;
    private final Set<String> bulk;

    private PriceTable(Map<String, Price> rows, Set<String> bulk) {
        this.rows = rows;
        this.bulk = bulk;
    }

    /** A table from rows (item id to price) and the categories that may be sold in bulk, for tests and for the game side when it filters. */
    public static PriceTable of(Map<String, Price> rows, Set<String> bulk) {
        return new PriceTable(Collections.unmodifiableMap(new LinkedHashMap<>(rows)), Collections.unmodifiableSet(new LinkedHashSet<>(bulk)));
    }

    /** A table with no bulk categories. */
    public static PriceTable of(Map<String, Price> rows) {
        return of(rows, Set.of());
    }

    /** True when the category may be sold with one "Sell all" click. */
    public boolean isBulk(String category) {
        return category != null && bulk.contains(category);
    }

    /** The categories with a "Sell all" button, in file order. */
    public Set<String> bulkCategories() { return bulk; }

    /** The price of an item, or null when the witch does not buy it. */
    public Price get(String itemId) {
        return itemId == null ? null : rows.get(itemId);
    }

    /** Every row, in file order. Unmodifiable. */
    public Map<String, Price> rows() { return rows; }

    public int size() { return rows.size(); }

    public boolean isEmpty() { return rows.isEmpty(); }

    /** The categories in use, in the order they first appear. */
    public Set<String> categories() {
        Set<String> out = new LinkedHashSet<>();
        for (Price p : rows.values()) out.add(p.category());
        return out;
    }

    /** The table without the rows whose item id the predicate refuses (the game side passes "is a registered item"); the refused ids are not returned. */
    public PriceTable filtered(java.util.function.Predicate<String> keep) {
        Map<String, Price> out = new LinkedHashMap<>();
        for (Map.Entry<String, Price> e : rows.entrySet()) if (keep.test(e.getKey())) out.put(e.getKey(), e.getValue());
        return new PriceTable(Collections.unmodifiableMap(out), bulk);
    }

    // ================================================================ reading
    /** Reads the text of a file. */
    public static Parsed parse(String json) {
        List<String> problems = new ArrayList<>();
        JsonElement root;
        try {
            root = JsonParser.parseString(json == null ? "" : json);
        } catch (JsonParseException | IllegalStateException e) {
            problems.add("not valid JSON: " + e.getMessage());
            return new Parsed(EMPTY, problems);
        }
        return parse(root, problems);
    }

    /** Reads an already parsed file. */
    public static Parsed parse(JsonElement root) {
        return parse(root, new ArrayList<>());
    }

    private static Parsed parse(JsonElement root, List<String> problems) {
        if (root == null || !root.isJsonObject() || !root.getAsJsonObject().has("prices") || !root.getAsJsonObject().get("prices").isJsonObject()) {
            problems.add("the file must be an object with a \"prices\" object (item id to gold, or to {\"gold\": n, \"category\": \"stones\"})");
            return new Parsed(EMPTY, problems);
        }
        Map<String, Price> out = new LinkedHashMap<>();
        int n = 0;
        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject().getAsJsonObject("prices").entrySet()) {
            if (e.getKey().startsWith("_")) continue;                 // a key that starts with an underscore is a comment, not an item (and does not count towards the row limit)
            if (++n > MAX_ROWS) {
                problems.add("prices: only the first " + MAX_ROWS + " rows are read, the rest ignored");
                break;
            }
            String item = e.getKey();
            if (!ITEM.matcher(item).matches()) {
                problems.add("prices '" + item + "': not a lower-case item id like minecraft:diamond, skipped");
                continue;
            }
            JsonElement v = e.getValue();
            Long gold;
            String category = DEFAULT_CATEGORY;
            if (v.isJsonObject()) {
                JsonObject o = v.getAsJsonObject();
                for (String k : o.keySet()) if (!k.equals("gold") && !k.equals("category") && !k.startsWith("_")) problems.add("prices '" + item + "': unknown key '" + k + "' (known: category, gold)");
                gold = whole(o.get("gold"));
                if (o.has("category")) {
                    JsonElement c = o.get("category");
                    if (c.isJsonPrimitive() && c.getAsJsonPrimitive().isString() && CATEGORY.matcher(c.getAsString()).matches()) category = c.getAsString();
                    else problems.add("prices '" + item + "': \"category\" must be lower-case snake_case, using \"" + DEFAULT_CATEGORY + "\"");
                }
            } else {
                gold = whole(v);
            }
            if (gold == null || gold < 1 || gold > MAX_PRICE) {
                problems.add("prices '" + item + "': the gold must be a whole number from 1 to " + MAX_PRICE + ", skipped");
                continue;
            }
            out.put(item, new Price(gold, category));
        }
        Set<String> bulk = new LinkedHashSet<>();
        JsonElement b = root.getAsJsonObject().get("bulk");
        if (b != null) {
            if (!b.isJsonArray()) {
                problems.add("\"bulk\" must be an array of category names");
            } else {
                Set<String> used = new LinkedHashSet<>();
                for (Price p : out.values()) used.add(p.category());
                for (JsonElement c : b.getAsJsonArray()) {
                    String name = c.isJsonPrimitive() && c.getAsJsonPrimitive().isString() ? c.getAsString() : null;
                    if (name == null || !CATEGORY.matcher(name).matches()) problems.add("bulk: " + c + " is not a category name, ignored");
                    else if (!used.contains(name)) problems.add("bulk '" + name + "': no row uses this category, ignored");
                    else bulk.add(name);
                }
            }
        }
        return new Parsed(new PriceTable(Collections.unmodifiableMap(out), Collections.unmodifiableSet(bulk)), problems);
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
}
