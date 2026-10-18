package com.newuniverse.nusmp.heroweapon.weapon.pierce;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the forms file of a hero weapon is data nobody can play-test here, and the forms system will refuse a malformed form at start-up (skipping it, so the
 * weapon quietly loses part of its tree). The rules are written in design.md section 6; this checker reads a shipped file with plain Gson and applies them
 * all, so a wrong item id, a parent that does not exist, a requirement above the allowed counts, a stat above its budget, a skill of another weapon or a
 * passive nobody implements fails in the unit tests with the name of the form. It is a test helper (not shipped): the Spear and the Bow tests both use it,
 * each with its own lists of skills, passives and ultimates.
 */
public final class FormsFileCheck {
    private FormsFileCheck() {}

    /** What a weapon's file is checked against. */
    public record Spec(String file, String weapon, List<String> skills, Set<String> ultimates, List<String> passives, String sin, boolean drawStyle,
                int minOrdinary, int maxOrdinary, int minCurse, int maxCurse) {}

    public static final Set<String> SLOTS = Set.of("use", "sneak", "key1", "key2", "key3");
    static final Map<String, Double> STAT_BUDGET = Map.of("armor", 8.0, "armor_toughness", 4.0, "max_health", 12.0, "attack_damage", 14.0, "movement_speed", 0.2,
            "knockback_resistance", 0.6, "reach", 2.0, "luck", 3.0);
    /**
     * The Tensura item ids the forms may name: each is used elsewhere in the repo (so it has been seen to work): tensura:magic_stone (the Mega-Dungeon loot
     * tables and Wave0Contract), tensura:medium_quality_magic_crystal and tensura:high_quality_magic_crystal (the same), tensura:hihiirokane_ingot and
     * tensura:adamantite_ingot (relics/tier/TierRules).
     */
    static final Set<String> TENSURA_ITEMS = Set.of("tensura:magic_stone", "tensura:medium_quality_magic_crystal", "tensura:high_quality_magic_crystal",
            "tensura:hihiirokane_ingot", "tensura:adamantite_ingot");

    /** Items that make a requirement "rare": the top form of a tree must ask for at least two of them (a real task, not a pile of common things). */
    static final Set<String> RARE = Set.of("minecraft:nether_star", "minecraft:echo_shard", "minecraft:dragon_breath", "minecraft:dragon_egg", "minecraft:netherite_ingot",
            "minecraft:netherite_scrap", "minecraft:heart_of_the_sea", "tensura:adamantite_ingot", "tensura:hihiirokane_ingot", "tensura:high_quality_magic_crystal");

    public static Set<String> vanillaItems() throws Exception {
        Set<String> out = new HashSet<>();
        for (String line : Files.readAllLines(Path.of("src/test/resources/mega/gear/vanilla_items.txt"))) if (!line.isBlank()) out.add(line.trim());
        return out;
    }

    /** Every problem of the file against the spec; empty when the file is fine. */
    public static List<String> problems(Spec spec, JsonObject root, Set<String> vanilla) {
        List<String> bad = new ArrayList<>();
        if (!spec.weapon().equals(str(root, "weapon"))) bad.add("weapon is '" + str(root, "weapon") + "', not " + spec.weapon());
        JsonArray arr = root.has("forms") && root.get("forms").isJsonArray() ? root.getAsJsonArray("forms") : null;
        if (arr == null || arr.isEmpty()) {
            bad.add("no forms");
            return bad;
        }
        Map<String, JsonObject> byId = new HashMap<>();
        List<JsonObject> list = new ArrayList<>();
        for (JsonElement e : arr) {
            if (!e.isJsonObject()) {
                bad.add("a form is not an object");
                continue;
            }
            JsonObject f = e.getAsJsonObject();
            list.add(f);
            String id = str(f, "id");
            if (!id.matches("[a-z][a-z0-9_]*")) bad.add("bad form id '" + id + "'");
            if (byId.put(id, f) != null) bad.add("duplicate form id " + id);
        }
        JsonObject first = list.get(0);
        if (!"base".equals(str(first, "id")) || num(first, "tier") != 0 || !arr(first, "parents").isEmpty() || !arr(first, "requires").isEmpty() || num(first, "mastery") != 0
                || bool(first, "curse")) bad.add("the first form must be the plain base form (tier 0, no parents, no requirements, no mastery, not a curse form)");

        int ordinary = 0, curse = 0;
        Set<String> usedSkills = new HashSet<>(), usedPassives = new HashSet<>(), names = new HashSet<>();
        Map<String, Integer> tier = new HashMap<>();
        for (JsonObject f : list) {
            String id = str(f, "id");
            boolean isCurse = bool(f, "curse");
            if (isCurse) curse++;
            else ordinary++;
            if (str(f, "name").isBlank() || !names.add(str(f, "name"))) bad.add(id + ": the name is empty or repeated");
            if (str(f, "desc").isBlank() || str(f, "desc").length() > 400) bad.add(id + ": the description is empty or longer than 400 characters");
            int t = (int) num(f, "tier");
            tier.put(id, t);
            if (t < 0 || t > 5) bad.add(id + ": tier " + t + " is outside 0..5");
            // parents: all exist, no cycle (a parent must come earlier in the file), tier is one deeper than the deepest parent
            int deepest = -1;
            for (JsonElement p : arr(f, "parents")) {
                String pid = p.getAsString();
                if (!byId.containsKey(pid)) bad.add(id + ": the parent " + pid + " does not exist");
                else if (list.indexOf(byId.get(pid)) >= list.indexOf(f)) bad.add(id + ": the parent " + pid + " must come earlier in the file");
                else deepest = Math.max(deepest, tier.getOrDefault(pid, 0));
            }
            if (!"base".equals(id)) {
                if (arr(f, "parents").isEmpty()) bad.add(id + ": no parents");
                if (t != deepest + 1) bad.add(id + ": tier " + t + " but its deepest parent is at " + deepest);
                double m = num(f, "mastery");
                if (m < 40 || m > 400) bad.add(id + ": mastery " + m + " is outside 40..400");
                JsonArray rq = arr(f, "requires");
                if (rq.size() < 1 || rq.size() > 4) bad.add(id + ": " + rq.size() + " requirements (1 to 4)");
                Set<String> seen = new HashSet<>();
                int total = 0, rare = 0;
                for (JsonElement r : rq) {
                    JsonObject o = r.getAsJsonObject();
                    String item = str(o, "item"), tag = str(o, "tag");
                    int n = (int) num(o, "count");
                    if (item.isEmpty() == tag.isEmpty()) bad.add(id + ": a requirement needs exactly one of item or tag");
                    int cap = t >= 5 ? 256 : 64;
                    if (n < 4 || n > cap) bad.add(id + ": the count " + n + " of " + item + tag + " is outside 4.." + cap);
                    if (!item.isEmpty() && !(vanilla.contains(item) || TENSURA_ITEMS.contains(item))) bad.add(id + ": the item " + item + " is not a known item id");
                    if (!seen.add(item + tag)) bad.add(id + ": the requirement " + item + tag + " is listed twice");
                    total += n;
                    if (RARE.contains(item)) rare++;
                }
                if (t == 1 && !isCurse && (total < 20 || total > 60)) bad.add(id + ": a first-tier form asks for " + total + " items (20 to 60)");
                if (t == 5 && !isCurse && rare < 2) bad.add(id + ": the top form asks for only " + rare + " rare materials (at least two: a real task)");
            }
            // stats
            checkStats(bad, id, obj(f, "stats"), 1.0);
            checkStats(bad, id, obj(f, "abilityBonus"), 0.25);
            // skills
            JsonObject skills = obj(f, "skills");
            Set<String> inForm = new HashSet<>();
            int ultimates = 0;
            for (Map.Entry<String, JsonElement> e : skills.entrySet()) {
                if (!SLOTS.contains(e.getKey())) bad.add(id + ": unknown slot '" + e.getKey() + "'");
                if (spec.drawStyle() && e.getKey().equals("use")) bad.add(id + ": a drawn weapon has no use slot (the use key is the draw)");
                String sid = e.getValue().getAsString();
                if (!spec.skills().contains(sid)) bad.add(id + ": the skill " + sid + " is not one of this weapon's");
                if (!inForm.add(sid)) bad.add(id + ": the skill " + sid + " is listed in two slots");
                if (spec.ultimates().contains(sid)) ultimates++;
                usedSkills.add(sid);
            }
            if (ultimates > 1) bad.add(id + ": " + ultimates + " ultimates (at most one per form)");
            if (skills.size() < 2 && !"base".equals(id)) bad.add(id + ": only " + skills.size() + " techniques");
            // passives
            Set<String> inFormPassives = new HashSet<>();
            for (JsonElement p : arr(f, "passives")) {
                String ps = p.getAsString();
                if (!spec.passives().contains(ps)) bad.add(id + ": the passive " + ps + " is not implemented");
                if (!inFormPassives.add(ps)) bad.add(id + ": the passive " + ps + " is listed twice");
                usedPassives.add(ps);
            }
            // look and the curse fields
            if (isCurse) {
                if (!"curse".equals(str(f, "look"))) bad.add(id + ": a curse form has the curse look");
                if (!spec.sin().equals(str(f, "sin"))) bad.add(id + ": the sin is '" + str(f, "sin") + "', not " + spec.sin());
                if (num(f, "corruptionPerSecond") <= 0 || num(f, "corruptionPerSecond") > 20) bad.add(id + ": corruptionPerSecond must be 1..20");
                if (!obj(f, "abilityBonus").isEmpty()) bad.add(id + ": a curse form gives no permanent bonus");
            } else {
                if (!"base".equals(str(f, "look"))) bad.add(id + ": an ordinary form has the base look");
                if (f.has("sin") && !f.get("sin").isJsonNull()) bad.add(id + ": an ordinary form has no sin");
                if (num(f, "corruptionPerSecond") != 0) bad.add(id + ": an ordinary form has no corruption");
            }
        }
        if (ordinary < spec.minOrdinary() || ordinary > spec.maxOrdinary()) bad.add(ordinary + " ordinary forms (" + spec.minOrdinary() + " to " + spec.maxOrdinary() + ")");
        if (curse < spec.minCurse() || curse > spec.maxCurse()) bad.add(curse + " curse forms (" + spec.minCurse() + " to " + spec.maxCurse() + ")");
        for (String s : spec.skills()) if (!usedSkills.contains(s)) bad.add("the skill " + s + " is in no form");
        for (String p : spec.passives()) if (!usedPassives.contains(p)) bad.add("the passive " + p + " is in no form");
        return bad;
    }

    private static void checkStats(List<String> bad, String id, JsonObject stats, double share) {
        for (Map.Entry<String, JsonElement> e : stats.entrySet()) {
            String key = e.getKey();
            double v = e.getValue().getAsDouble();
            if (key.equals("attack_speed")) {
                double lo = -3.0 * share, hi = 1.0 * share;
                if (v < lo - 1e-9 || v > hi + 1e-9) bad.add(id + ": attack_speed " + v + " is outside " + lo + ".." + hi);
                continue;
            }
            Double budget = STAT_BUDGET.get(key);
            if (budget == null) {
                bad.add(id + ": unknown stat " + key);
                continue;
            }
            if (v < 0 || v > budget * share + 1e-9) bad.add(id + ": " + key + " " + v + " is outside 0.." + budget * share);
        }
    }

    // ---- tiny Gson helpers that never throw
    public static String str(JsonObject o, String k) { return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : ""; }

    public static double num(JsonObject o, String k) { return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsDouble() : 0.0; }

    public static boolean bool(JsonObject o, String k) { return o.has(k) && o.get(k).isJsonPrimitive() && o.get(k).getAsBoolean(); }

    public static JsonArray arr(JsonObject o, String k) { return o.has(k) && o.get(k).isJsonArray() ? o.getAsJsonArray(k) : new JsonArray(); }

    public static JsonObject obj(JsonObject o, String k) { return o.has(k) && o.get(k).isJsonObject() ? o.getAsJsonObject(k) : new JsonObject(); }

    public static JsonObject read(String file) throws Exception { return JsonParser.parseString(Files.readString(Path.of(file))).getAsJsonObject(); }
}
