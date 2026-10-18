package com.newuniverse.nusmp.elite.hub;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Why: a quest file is written by hand (or by a datapack author), so the reader must never throw and never trust a number: a broken row is skipped with a readable sentence, the rest still load,
 * and every value is validated against the same bounds {@link QuestDef} clamps to. The file may be an object with a {@code "quests"} array (the shipped files), a single quest object, or a bare
 * array. A row needs {@code id}, {@code type} ({@code daily} | {@code progression}), {@code kind} ({@code kill} | {@code clear_floors} | {@code deliver} | {@code reach_floor}), {@code target}
 * (for reach_floor the target is 1 and {@code floor} says which) and {@code reward} (bronze coins); the rest is optional filters. Pure: Gson and plain Java, so it is unit tested with strings and
 * the shipped files are parsed by a test too.
 */
public final class QuestParser {
    private QuestParser() {}

    /** The rows read and every problem met on the way. */
    public record Result(List<QuestDef> quests, List<String> problems) {}

    private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,48}");
    private static final Pattern ITEM = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern FACTION = Pattern.compile("[a-z][a-z0-9_]{0,31}");
    /** At most this many quests are read from one file. */
    public static final int MAX_ROWS = 512;

    public static Result parse(String json) {
        List<String> problems = new ArrayList<>();
        JsonElement root;
        try {
            root = JsonParser.parseString(json == null ? "" : json);
        } catch (JsonParseException | IllegalStateException e) {
            problems.add("not valid JSON: " + e.getMessage());
            return new Result(List.of(), problems);
        }
        return parse(root, problems);
    }

    public static Result parse(JsonElement root, List<String> problems) {
        List<QuestDef> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        JsonArray rows;
        if (root != null && root.isJsonArray()) rows = root.getAsJsonArray();
        else if (root != null && root.isJsonObject() && root.getAsJsonObject().has("quests") && root.getAsJsonObject().get("quests").isJsonArray()) rows = root.getAsJsonObject().getAsJsonArray("quests");
        else if (root != null && root.isJsonObject() && root.getAsJsonObject().has("id")) {
            rows = new JsonArray();
            rows.add(root);
        } else {
            problems.add("the file must be a quest object, an array of them, or an object with a \"quests\" array");
            return new Result(List.of(), problems);
        }
        int n = 0;
        for (JsonElement el : rows) {
            if (++n > MAX_ROWS) {
                problems.add("more than " + MAX_ROWS + " quests: the rest are ignored");
                break;
            }
            QuestDef q = row(el, n, problems);
            if (q == null) continue;
            if (!seen.add(q.id())) {
                problems.add("quest #" + n + " '" + q.id() + "': duplicate id in this file, skipped");
                continue;
            }
            out.add(q);
        }
        return new Result(out, problems);
    }

    private static QuestDef row(JsonElement el, int n, List<String> problems) {
        if (el == null || !el.isJsonObject()) {
            problems.add("quest #" + n + " is not an object");
            return null;
        }
        JsonObject o = el.getAsJsonObject();
        String id = str(o, "id", "");
        if (!ID.matcher(id).matches()) {
            problems.add("quest #" + n + ": the id '" + id + "' must be lower-case letters, digits and underscores (1 to 48)");
            return null;
        }
        String where = "quest '" + id + "': ";
        QuestType type = QuestType.fromId(str(o, "type", ""));
        if (type == null) {
            problems.add(where + "type must be 'daily' or 'progression'");
            return null;
        }
        ObjectiveKind kind = ObjectiveKind.fromId(str(o, "kind", ""));
        if (kind == null) {
            problems.add(where + "kind must be 'kill', 'clear_floors', 'deliver' or 'reach_floor'");
            return null;
        }
        long target = lng(o, "target", kind == ObjectiveKind.REACH_FLOOR ? 1 : -1);
        if (target < 1 || target > 1_000_000) {
            problems.add(where + "target must be 1 to 1000000");
            return null;
        }
        long reward = lng(o, "reward", -1);
        if (reward < 0 || reward > 1_000_000_000L) {
            problems.add(where + "reward (bronze coins) must be 0 to 1000000000");
            return null;
        }
        int tier = (int) lng(o, "tier", 0);
        if (tier < 0 || tier > CoinLoot.TIERS) {
            problems.add(where + "tier must be 0 (any) to " + CoinLoot.TIERS);
            return null;
        }
        String road = str(o, "road", "").toLowerCase();
        if (!road.isEmpty() && !road.equals("holy") && !road.equals("abyssal")) {
            problems.add(where + "road must be 'holy', 'abyssal' or empty");
            return null;
        }
        String faction = str(o, "faction", "").toLowerCase();
        if (!faction.isEmpty() && !FACTION.matcher(faction).matches()) {
            problems.add(where + "faction '" + faction + "' is not a faction id");
            return null;
        }
        String mob = str(o, "mob", "any").toLowerCase();
        if (!mob.equals("any") && !mob.equals("foundry") && !mob.equals("boss")) {
            problems.add(where + "mob must be 'any', 'foundry' or 'boss'");
            return null;
        }
        String item = str(o, "item", "").toLowerCase();
        if (kind == ObjectiveKind.DELIVER && !ITEM.matcher(item).matches()) {
            problems.add(where + "a deliver quest needs an item id such as 'minecraft:iron_ingot'");
            return null;
        }
        int floor = (int) lng(o, "floor", 0);
        if (kind == ObjectiveKind.REACH_FLOOR && (floor == 0 || Math.abs(floor) > 100_000)) {
            problems.add(where + "a reach_floor quest needs a signed 'floor' (Holy positive, Abyssal negative, not 0)");
            return null;
        }
        List<String> requires = new ArrayList<>();
        if (o.has("requires") && o.get("requires").isJsonArray()) {
            for (JsonElement r : o.getAsJsonArray("requires")) {
                if (r.isJsonPrimitive() && ID.matcher(r.getAsString()).matches()) requires.add(r.getAsString());
                else problems.add(where + "a 'requires' entry is not a quest id, ignored");
            }
        }
        if (type == QuestType.DAILY && !requires.isEmpty()) {
            problems.add(where + "a daily quest cannot require another quest, 'requires' ignored");
            requires.clear();
        }
        int minFloor = (int) lng(o, "minFloor", 0);
        int weight = (int) lng(o, "weight", 10);
        return new QuestDef(id, type, kind, str(o, "title", ""), str(o, "desc", ""), (int) target, tier, road, faction, mob, item, floor, reward, minFloor, requires, weight);
    }

    private static String str(JsonObject o, String key, String dflt) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive()) return dflt;
        String s = e.getAsString();
        return s == null ? dflt : s.trim();
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
