package com.newuniverse.nusmp.elite.forge.rules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Why: a forge recipe is data ({@code data/nusmp/elite/forge/recipes/*.json}), so a pack can add, change or remove recipes without code. This is the pure,
 * Minecraft-free reading of one file into a record, with every rule of the format checked in one place (and tested against all the shipped files): items are plain
 * resource-location strings here (the server resolves them against the registries when it loads and marks a recipe whose item does not exist as unavailable).
 * <pre>
 * { "tier": 0..3,                      station tier needed (0 basic, 1 magisteel, 2 mythril, 3 dragonforge)
 *   "category": "weapon|armor|tool|rune",
 *   "output": { "item": "minecraft:iron_sword", "count": 1 },
 *   "inputs": [ { "item": "minecraft:iron_ingot", "count": 2 }, ... ],        1..6 rows, count 1..64
 *   "premium": [ { "item": "...", "count": 1, "bonus": 1..8 }, ... ],         optional extras that add quality points
 *   "minigames": [ { "type": "hammer_timing", "difficulty": 1..5 }, ... ],    1..4 steps in order
 *   "base_quality": 0..30,
 *   "engrave": true|false }                                                    the forge engraves the result (weapons and armour)
 * </pre>
 */
public record ForgeRecipeDef(String id, int tier, Category category, Stack output, List<Stack> inputs, List<Premium> premium, List<SessionCheck.Step> steps,
                             int baseQuality, boolean engrave) {
    private static final Pattern ITEM = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");

    /** An item id and a count. */
    public record Stack(String item, int count) {}

    /** An optional material and the quality points it adds when used. */
    public record Premium(String item, int count, int bonus) {}

    /** Reads one recipe file. Throws {@link IllegalArgumentException} with a message naming the problem. */
    public static ForgeRecipeDef parse(String id, JsonObject o) {
        int tier = intOf(o, "tier", -1);
        if (tier < 0 || tier > 3) throw new IllegalArgumentException(id + ": tier must be 0..3");
        Category category = Category.byId(strOf(o, "category"));
        if (category == null) throw new IllegalArgumentException(id + ": unknown category");
        JsonObject out = o.has("output") && o.get("output").isJsonObject() ? o.getAsJsonObject("output") : null;
        if (out == null) throw new IllegalArgumentException(id + ": no output");
        Stack output = stack(id, out, "output");
        List<Stack> inputs = new ArrayList<>();
        JsonArray in = arr(o, "inputs");
        if (in == null || in.isEmpty() || in.size() > 6) throw new IllegalArgumentException(id + ": inputs must hold 1..6 rows");
        for (JsonElement e : in) {
            if (!e.isJsonObject()) throw new IllegalArgumentException(id + ": an input row is not an object");
            inputs.add(stack(id, e.getAsJsonObject(), "input"));
        }
        List<Premium> premium = new ArrayList<>();
        JsonArray pr = arr(o, "premium");
        if (pr != null) {
            if (pr.size() > 4) throw new IllegalArgumentException(id + ": at most 4 premium rows");
            for (JsonElement e : pr) {
                if (!e.isJsonObject()) throw new IllegalArgumentException(id + ": a premium row is not an object");
                Stack s = stack(id, e.getAsJsonObject(), "premium");
                int bonus = intOf(e.getAsJsonObject(), "bonus", 0);
                if (bonus < 1 || bonus > 8) throw new IllegalArgumentException(id + ": premium bonus must be 1..8");
                premium.add(new Premium(s.item(), s.count(), bonus));
            }
        }
        List<SessionCheck.Step> steps = new ArrayList<>();
        JsonArray mg = arr(o, "minigames");
        if (mg == null || mg.isEmpty() || mg.size() > 4) throw new IllegalArgumentException(id + ": minigames must hold 1..4 steps");
        for (JsonElement e : mg) {
            if (!e.isJsonObject()) throw new IllegalArgumentException(id + ": a minigame step is not an object");
            JsonObject s = e.getAsJsonObject();
            MinigameType t = MinigameType.byId(strOf(s, "type"));
            if (t == null) throw new IllegalArgumentException(id + ": unknown minigame type");
            int d = intOf(s, "difficulty", 0);
            if (d < 1 || d > 5) throw new IllegalArgumentException(id + ": difficulty must be 1..5");
            steps.add(new SessionCheck.Step(t, d));
        }
        int base = intOf(o, "base_quality", -1);
        if (base < 0 || base > 30) throw new IllegalArgumentException(id + ": base_quality must be 0..30");
        boolean engrave = o.has("engrave") && o.get("engrave").isJsonPrimitive() && o.get("engrave").getAsBoolean();
        return new ForgeRecipeDef(id, tier, category, output, List.copyOf(inputs), List.copyOf(premium), List.copyOf(steps), base, engrave);
    }

    private static Stack stack(String id, JsonObject o, String what) {
        String item = strOf(o, "item");
        if (item == null || !ITEM.matcher(item).matches()) throw new IllegalArgumentException(id + ": bad " + what + " item id");
        int count = intOf(o, "count", 1);
        if (count < 1 || count > 64) throw new IllegalArgumentException(id + ": " + what + " count must be 1..64");
        return new Stack(item, count);
    }

    private static String strOf(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
    }

    private static int intOf(JsonObject o, String k, int def) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsInt() : def;
    }

    private static JsonArray arr(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : null;
    }
}
