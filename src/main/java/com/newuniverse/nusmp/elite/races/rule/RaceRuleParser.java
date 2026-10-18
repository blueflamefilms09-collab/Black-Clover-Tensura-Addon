package com.newuniverse.nusmp.elite.races.rule;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Why: the rules come from data files (data/nusmp/elite/races/*.json, which a datapack may replace), and a bad file must be refused with a sentence that names the file's field,
 * not crash the server or silently evolve everyone for free. The parser is strict (an unknown type, a missing field, a count below one, a weight outside 0..100, a nesting deeper
 * than two are errors) and pure (Gson only), so the bundled files can be checked by a plain unit test. Every number is range-checked so a typo cannot ask for 1e300 EP.
 */
public final class RaceRuleParser {
    private RaceRuleParser() {}

    public static final int MAX_RULES = 12;
    public static final Set<String> KILL_KEYS = Set.of("any", "hostile", "boss", "player");
    private static final double MAX_EP = 1.0e15;

    public static RaceRuleSet parseSet(JsonArray array, String where) {
        if (array == null) return RaceRuleSet.EMPTY;
        if (array.size() > MAX_RULES) throw new IllegalArgumentException(where + ": more than " + MAX_RULES + " rules");
        List<RaceEvolutionRule> out = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonElement e = array.get(i);
            if (!e.isJsonObject()) throw new IllegalArgumentException(where + "[" + i + "]: a rule must be an object");
            out.add(parse(e.getAsJsonObject(), where + "[" + i + "]", 0));
        }
        return new RaceRuleSet(out);
    }

    public static RaceEvolutionRule parse(JsonObject o, String where, int depth) {
        String type = str(o, "type", where);
        double weight = o.has("weight") ? num(o, "weight", where, 0.0, 100.0) : 10.0;
        switch (type) {
            case "ep":
                return new EvolutionRules.EpGate(num(o, "min", where, 0.0, MAX_EP), weight);
            case "kills": {
                String key = o.has("key") ? str(o, "key", where) : "any";
                if (!KILL_KEYS.contains(key)) throw new IllegalArgumentException(where + ": kills key '" + key + "' is not one of " + KILL_KEYS);
                return new EvolutionRules.Kills(key, (int) num(o, "count", where, 1, 1_000_000), weight);
            }
            case "mastery": {
                String skill = id(str(o, "skill", where), where, "skill");
                return new EvolutionRules.Mastery(skill, num(o, "fraction", where, 0.01, 1.0), weight);
            }
            case "item": {
                String item = id(str(o, "item", where), where, "item");
                boolean consume = o.has("consume") && o.get("consume").getAsBoolean();
                return new EvolutionRules.Item(item, (int) num(o, "count", where, 1, 4096), consume, weight);
            }
            case "record":
                return new EvolutionRules.RecordUnlocked(str(o, "id", where), weight);
            case "any_of": {
                if (depth >= 1) throw new IllegalArgumentException(where + ": any_of may not be nested inside any_of");
                if (!o.has("rules") || !o.get("rules").isJsonArray() || o.getAsJsonArray("rules").size() < 2)
                    throw new IllegalArgumentException(where + ": any_of needs a 'rules' array of at least two rules");
                List<RaceEvolutionRule> kids = new ArrayList<>();
                JsonArray arr = o.getAsJsonArray("rules");
                for (int i = 0; i < arr.size(); i++) {
                    if (!arr.get(i).isJsonObject()) throw new IllegalArgumentException(where + ".rules[" + i + "]: a rule must be an object");
                    kids.add(parse(arr.get(i).getAsJsonObject(), where + ".rules[" + i + "]", depth + 1));
                }
                return new EvolutionRules.AnyOf(kids, weight);
            }
            default:
                throw new IllegalArgumentException(where + ": unknown rule type '" + type + "' (ep, kills, mastery, item, record, any_of)");
        }
    }

    // ---- small checked readers, shared with the template parser
    public static String str(JsonObject o, String key, String where) {
        if (!o.has(key) || !o.get(key).isJsonPrimitive()) throw new IllegalArgumentException(where + ": missing text field '" + key + "'");
        String s = o.get(key).getAsString().trim();
        if (s.isEmpty() || s.length() > 128) throw new IllegalArgumentException(where + ": '" + key + "' must be 1 to 128 characters");
        return s;
    }

    public static double num(JsonObject o, String key, String where, double min, double max) {
        if (!o.has(key) || !o.get(key).isJsonPrimitive()) throw new IllegalArgumentException(where + ": missing number '" + key + "'");
        double v;
        try { v = o.get(key).getAsDouble(); } catch (NumberFormatException e) { throw new IllegalArgumentException(where + ": '" + key + "' is not a number"); }
        if (Double.isNaN(v) || Double.isInfinite(v) || v < min || v > max)
            throw new IllegalArgumentException(where + ": '" + key + "' = " + v + " is outside " + min + ".." + max);
        return v;
    }

    /** A namespaced id such as {@code tensura:strength}; the namespace defaults to minecraft only for items, so it is required here. */
    public static String id(String s, String where, String what) {
        int c = s.indexOf(':');
        if (c <= 0 || c == s.length() - 1 || !s.equals(s.toLowerCase(java.util.Locale.ROOT)) || s.indexOf(' ') >= 0)
            throw new IllegalArgumentException(where + ": " + what + " '" + s + "' is not a lowercase namespaced id (namespace:path)");
        return s;
    }
}
