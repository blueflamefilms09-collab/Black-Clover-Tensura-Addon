package com.newuniverse.nusmp.elite.hub;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Why: the Merchant's goods and prices are DATA ({@code data/nusmp/elite/hub/shop.json}) so the owner can retune a price or add a ware, and a datapack can replace the file, without a mod update.
 * This is that table as an immutable value with no game type (an item is a plain id string, so a row may name an item of an addon that is not installed: the game side drops the rows whose item
 * does not exist) plus the reader, which never throws on a bad file: a broken row is skipped with a readable sentence and the rest still load. A row is {@code id} (the offer's own id, what the
 * client sends back), {@code item}, {@code count} (stack size sold), {@code price} (bronze coins for the whole stack), {@code category} ({@code gear}, {@code food}, {@code utility}, {@code map};
 * any word, only a grouping), {@code dailyLimit} (how many stacks ONE player may buy per world day, 0 = unlimited: this is the stock, see docs/elite/hub.md for why it is per player) and
 * {@code minFloor} (the deepest floor the player must have reached; 0 = always). Every trade is priced in coins and nothing else.
 */
public final class ShopTable {
    /** The empty shop. */
    public static final ShopTable EMPTY = new ShopTable(List.of());

    /** One ware. */
    public record Offer(String id, String item, int count, long price, String category, int dailyLimit, int minFloor) {
        public Offer {
            id = id == null ? "" : id;
            item = item == null ? "" : item;
            count = Math.max(1, Math.min(64, count));
            price = Math.max(0, Math.min(CoinMath.CAP, price));
            category = category == null || category.isEmpty() ? "goods" : category;
            dailyLimit = Math.max(0, dailyLimit);
            minFloor = Math.max(0, minFloor);
        }
    }

    /** The result of reading a file. */
    public record Parsed(ShopTable table, List<String> problems) {}

    private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,40}");
    private static final Pattern ITEM = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern CATEGORY = Pattern.compile("[a-z][a-z0-9_]{0,23}");
    /** At most this many offers are read. */
    public static final int MAX_ROWS = 256;

    private final Map<String, Offer> byId = new LinkedHashMap<>();

    public ShopTable(List<Offer> offers) {
        for (Offer o : offers) byId.put(o.id(), o);
    }

    public Offer get(String id) { return id == null ? null : byId.get(id); }

    public List<Offer> offers() { return Collections.unmodifiableList(new ArrayList<>(byId.values())); }

    public int size() { return byId.size(); }

    public boolean isEmpty() { return byId.isEmpty(); }

    /** The categories in use, in the order they first appear. */
    public Set<String> categories() {
        Set<String> out = new LinkedHashSet<>();
        for (Offer o : byId.values()) out.add(o.category());
        return out;
    }

    /** The table without the rows whose item the predicate refuses (the game side passes "is a registered item"). */
    public ShopTable filtered(java.util.function.Predicate<String> keepItem) {
        List<Offer> out = new ArrayList<>();
        for (Offer o : byId.values()) if (keepItem.test(o.item())) out.add(o);
        return new ShopTable(out);
    }

    // ---------------------------------------------------------------- reading
    public static Parsed parse(String json) {
        List<String> problems = new ArrayList<>();
        JsonElement root;
        try {
            root = JsonParser.parseString(json == null ? "" : json);
        } catch (JsonParseException | IllegalStateException e) {
            problems.add("not valid JSON: " + e.getMessage());
            return new Parsed(EMPTY, problems);
        }
        if (root == null || !root.isJsonObject() || !root.getAsJsonObject().has("offers") || !root.getAsJsonObject().get("offers").isJsonArray()) {
            problems.add("the file must be an object with an \"offers\" array");
            return new Parsed(EMPTY, problems);
        }
        JsonArray rows = root.getAsJsonObject().getAsJsonArray("offers");
        List<Offer> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        int n = 0;
        for (JsonElement el : rows) {
            if (++n > MAX_ROWS) {
                problems.add("more than " + MAX_ROWS + " offers: the rest are ignored");
                break;
            }
            if (!el.isJsonObject()) {
                problems.add("offer #" + n + " is not an object");
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            String id = str(o, "id");
            String item = str(o, "item");
            if (!ID.matcher(id).matches()) {
                problems.add("offer #" + n + ": the id '" + id + "' must be lower-case letters, digits and underscores (1 to 40)");
                continue;
            }
            if (!ITEM.matcher(item).matches()) {
                problems.add("offer '" + id + "': '" + item + "' is not an item id");
                continue;
            }
            long price = lng(o, "price", -1);
            if (price < 0 || price > 1_000_000_000_000L) {
                problems.add("offer '" + id + "': price (bronze coins) must be 0 to 10^12");
                continue;
            }
            long count = lng(o, "count", 1);
            if (count < 1 || count > 64) {
                problems.add("offer '" + id + "': count must be 1 to 64");
                continue;
            }
            String category = str(o, "category").toLowerCase();
            if (!category.isEmpty() && !CATEGORY.matcher(category).matches()) {
                problems.add("offer '" + id + "': category '" + category + "' must be a lower-case word");
                continue;
            }
            if (!seen.add(id)) {
                problems.add("offer '" + id + "': duplicate id, skipped");
                continue;
            }
            out.add(new Offer(id, item, (int) count, price, category, (int) Math.max(0, lng(o, "dailyLimit", 0)), (int) Math.max(0, lng(o, "minFloor", 0))));
        }
        return new Parsed(new ShopTable(out), problems);
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e == null || !e.isJsonPrimitive() ? "" : e.getAsString().trim();
    }

    private static long lng(JsonObject o, String key, long dflt) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive()) return dflt;
        try {
            return e.getAsLong();
        } catch (NumberFormatException | UnsupportedOperationException ex) {
            return dflt;
        }
    }
}
