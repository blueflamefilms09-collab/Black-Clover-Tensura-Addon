package com.newuniverse.nusmp.elite.items.crate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Why: a crate's reward table is DATA ({@code data/nusmp/elite/items/crates/<tier>.json}), and data from a datapack must never be trusted: this parser (pure, so a test can feed it bad files) reads the
 * table, drops and reports every bad entry instead of failing the whole table, and enforces the owner's "command-free" rule by refusing any key it does not know, so a {@code command}, {@code function}
 * or {@code nbt} key can never reach the opening code. A table with no usable entry is reported as an error and the loader keeps the built-in fallback for that tier.
 *
 * <pre>
 * { "rolls": 2,                 // or [min, max]
 *   "entries": [ { "type": "item", "id": "minecraft:diamond", "weight": 8, "min": 1, "max": 3 },
 *                { "type": "coins", "weight": 10, "min": 100, "max": 400 } ] }
 * </pre>
 * {@code count} and {@code amount} are accepted as a fixed {@code min = max}.
 */
public final class CrateTableParser {
    private CrateTableParser() {}

    /** The keys an entry may have. Anything else is refused (this is what keeps a table from carrying a command). */
    static final Set<String> ENTRY_KEYS = Set.of("type", "weight", "id", "min", "max", "count", "amount", "label");

    /** The parse outcome: the table (null when nothing is usable) and one line per problem found. */
    public record Result(CrateTable table, List<String> errors) {
        public boolean ok() { return table != null && errors.isEmpty(); }
    }

    public static Result parse(String tier, String json, Predicate<String> knownType) {
        List<String> errors = new ArrayList<>();
        JsonObject root;
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonObject()) return new Result(null, List.of(tier + ": the file is not a JSON object"));
            root = el.getAsJsonObject();
        } catch (RuntimeException ex) {
            return new Result(null, List.of(tier + ": not valid JSON (" + ex.getMessage() + ")"));
        }
        return parse(tier, root, knownType, errors);
    }

    public static Result parse(String tier, JsonObject root, Predicate<String> knownType) {
        return parse(tier, root, knownType, new ArrayList<>());
    }

    private static Result parse(String tier, JsonObject root, Predicate<String> knownType, List<String> errors) {
        int rollsMin = 1, rollsMax = 1;
        JsonElement r = root.get("rolls");
        if (r != null) {
            try {
                if (r.isJsonArray() && r.getAsJsonArray().size() == 2) {
                    rollsMin = r.getAsJsonArray().get(0).getAsInt();
                    rollsMax = r.getAsJsonArray().get(1).getAsInt();
                } else {
                    rollsMin = rollsMax = r.getAsInt();
                }
            } catch (RuntimeException ex) {
                errors.add(tier + ": 'rolls' must be a number or [min, max]");
            }
        }
        if (rollsMin < 1 || rollsMax < rollsMin || rollsMax > 16) {
            errors.add(tier + ": 'rolls' must satisfy 1 <= min <= max <= 16 (got " + rollsMin + ", " + rollsMax + ")");
            rollsMin = Math.max(1, Math.min(rollsMin, 16));
            rollsMax = Math.max(rollsMin, Math.min(rollsMax, 16));
        }
        List<RewardEntry> entries = new ArrayList<>();
        JsonElement arr = root.get("entries");
        if (arr == null || !arr.isJsonArray()) {
            errors.add(tier + ": 'entries' must be an array");
        } else {
            JsonArray a = arr.getAsJsonArray();
            for (int i = 0; i < a.size(); i++) {
                String where = tier + " entry " + i;
                if (!a.get(i).isJsonObject()) { errors.add(where + ": not an object"); continue; }
                JsonObject o = a.get(i).getAsJsonObject();
                RewardEntry e = entry(where, o, knownType, errors);
                if (e != null) entries.add(e);
            }
        }
        if (entries.isEmpty()) {
            errors.add(tier + ": no usable entry");
            return new Result(null, errors);
        }
        return new Result(new CrateTable(tier, rollsMin, rollsMax, entries), errors);
    }

    private static RewardEntry entry(String where, JsonObject o, Predicate<String> knownType, List<String> errors) {
        for (String k : o.keySet()) {
            if (!ENTRY_KEYS.contains(k)) {
                errors.add(where + ": unknown key '" + k + "' (reward entries are command-free; only " + ENTRY_KEYS + " are allowed)");
                return null;
            }
        }
        String type = str(o, "type");
        if (type.isEmpty() || !knownType.test(type)) { errors.add(where + ": unknown reward type '" + type + "'"); return null; }
        int weight, min, max;
        try {
            weight = o.has("weight") ? o.get("weight").getAsInt() : 1;
            if (o.has("count")) min = max = o.get("count").getAsInt();
            else if (o.has("amount")) min = max = o.get("amount").getAsInt();
            else {
                min = o.has("min") ? o.get("min").getAsInt() : 1;
                max = o.has("max") ? o.get("max").getAsInt() : min;
            }
        } catch (RuntimeException ex) {
            errors.add(where + ": weight, min, max, count and amount must be whole numbers");
            return null;
        }
        if (weight <= 0) { errors.add(where + ": weight must be positive"); return null; }
        if (min < 1 || max < min) { errors.add(where + ": need 1 <= min <= max (got " + min + ", " + max + ")"); return null; }
        if (max > 1_000_000) { errors.add(where + ": max is above 1,000,000"); return null; }
        return new RewardEntry(type, weight, str(o, "id"), min, max, str(o, "label"));
    }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e == null || !e.isJsonPrimitive() ? "" : e.getAsString();
    }
}
