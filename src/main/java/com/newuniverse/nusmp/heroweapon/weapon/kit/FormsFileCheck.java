package com.newuniverse.nusmp.heroweapon.weapon.kit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Why: a forms file (design section 6) is the only place where a mistake costs the whole weapon: a wrong skill id or a form that asks for the impossible
 * leaves the player with the base form, and a wrong item id fails the build's in-game check. The forms system has its own validator, but it arrives with
 * another builder; this one is the weapon builder's own gate and runs in plain JUnit. It checks everything that can be known without the game's registries:
 * the structure and the base form, the tiers and parents (no cycle), the requirements (one to four entries, 4 to 64 of an item or tag, up to 256 for a top
 * form, well-formed ids), the stat keys and their budgets (the ability bonus at most a quarter), the slots, that every skill belongs to the weapon and a form
 * has at most one ultimate, that every passive is one the weapon implements, and that nothing the weapon built is left out of every form. Item and tag ids are
 * compared to a list the caller passes only when it has one (the registry check itself happens in the game).
 */
public final class FormsFileCheck {
    private FormsFileCheck() {}

    /** What the weapon promises: its item id, its skill ids (and which are ultimates), and the passive ids its logic implements. */
    public record Rules(String weaponId, Set<String> skills, Set<String> ultimates, Set<String> passives) {}

    private static final Set<String> SLOTS = Set.of("use", "sneak", "key1", "key2", "key3");
    private static final Pattern FORM_ID = Pattern.compile("[a-z][a-z0-9_]*");
    private static final Pattern RESOURCE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Map<String, double[]> STAT_RANGE = new HashMap<>();

    static {
        STAT_RANGE.put("armor", new double[]{0.0, 8.0});
        STAT_RANGE.put("armor_toughness", new double[]{0.0, 4.0});
        STAT_RANGE.put("max_health", new double[]{0.0, 12.0});
        STAT_RANGE.put("attack_damage", new double[]{0.0, 14.0});
        STAT_RANGE.put("attack_speed", new double[]{-3.0, 1.0});
        STAT_RANGE.put("movement_speed", new double[]{0.0, 0.2});
        STAT_RANGE.put("knockback_resistance", new double[]{0.0, 0.6});
        STAT_RANGE.put("reach", new double[]{0.0, 2.0});
        STAT_RANGE.put("luck", new double[]{0.0, 3.0});
    }

    /** Every problem found in the file's text (empty when the file passes). */
    public static List<String> check(String json, Rules rules) {
        List<String> bad = new ArrayList<>();
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            bad.add("the file is not a JSON object: " + e.getMessage());
            return bad;
        }
        if (!root.has("weapon") || !rules.weaponId().equals(str(root, "weapon"))) bad.add("the \"weapon\" key must be " + rules.weaponId());
        JsonArray array = root.has("forms") && root.get("forms").isJsonArray() ? root.getAsJsonArray("forms") : new JsonArray();
        if (array.size() < 3 || array.size() > 5) bad.add("a Seven Star or Vassal weapon has 3 to 5 forms, this file has " + array.size());
        Map<String, JsonObject> byId = new HashMap<>();
        Map<String, Integer> tierOf = new HashMap<>();
        List<String> order = new ArrayList<>();
        for (JsonElement e : array) {
            if (!e.isJsonObject()) {
                bad.add("a form is not an object");
                continue;
            }
            JsonObject f = e.getAsJsonObject();
            String id = str(f, "id");
            if (id == null || !FORM_ID.matcher(id).matches()) {
                bad.add("a form has the bad id " + id);
                continue;
            }
            if (byId.put(id, f) != null) bad.add("the form id " + id + " appears twice");
            order.add(id);
            tierOf.put(id, f.has("tier") && f.get("tier").isJsonPrimitive() ? f.get("tier").getAsInt() : -1);
        }
        if (order.isEmpty() || !"base".equals(order.get(0))) bad.add("the first form must be \"base\"");
        Set<String> usedSkills = new HashSet<>(), usedPassives = new HashSet<>();
        for (String id : order) form(id, byId.get(id), byId, tierOf, rules, bad, usedSkills, usedPassives);
        for (String s : rules.skills()) if (!usedSkills.contains(s)) bad.add("the skill " + s + " is built but no form lists it");
        for (String p : rules.passives()) if (!usedPassives.contains(p)) bad.add("the passive " + p + " is implemented but no form lists it");
        return bad;
    }

    private static void form(String id, JsonObject f, Map<String, JsonObject> byId, Map<String, Integer> tierOf, Rules rules, List<String> bad,
                             Set<String> usedSkills, Set<String> usedPassives) {
        String who = "form " + id + ": ";
        int tier = tierOf.get(id);
        if (tier < 0 || tier > 5) bad.add(who + "the tier " + tier + " is outside 0..5");
        if (str(f, "name") == null || str(f, "name").isBlank()) bad.add(who + "no name");
        if (str(f, "desc") == null || str(f, "desc").isBlank()) bad.add(who + "no description");
        if (f.has("curse") && f.get("curse").getAsBoolean()) bad.add(who + "this weapon has no curse series");
        if (f.has("look") && !"base".equals(str(f, "look"))) bad.add(who + "a vassal weapon has only the base look");
        List<String> parents = strings(f, "parents");
        if ("base".equals(id)) {
            if (tier != 0 || !parents.isEmpty() || size(f, "requires") != 0) bad.add(who + "the base form has tier 0, no parents and no requirements");
        } else {
            if (parents.isEmpty()) bad.add(who + "needs at least one parent");
            for (String p : parents) {
                if (!byId.containsKey(p)) bad.add(who + "the parent " + p + " does not exist");
                else if (tierOf.get(p) >= tier) bad.add(who + "the parent " + p + " is not of a lower tier");
            }
            int mastery = f.has("mastery") ? f.get("mastery").getAsInt() : 0;
            if (mastery < 40 || mastery > 400) bad.add(who + "mastery " + mastery + " is outside 40..400");
        }
        requirements(who, f, tier, bad);
        stats(who, f, "stats", 1.0, bad);
        stats(who, f, "abilityBonus", 0.25, bad);
        int ultimates = 0;
        if (f.has("skills") && f.get("skills").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : f.getAsJsonObject("skills").entrySet()) {
                String skill = e.getValue().getAsString();
                if (!SLOTS.contains(e.getKey())) bad.add(who + "the slot " + e.getKey() + " is not one of use, sneak, key1, key2, key3");
                if (!rules.skills().contains(skill)) bad.add(who + "the skill " + skill + " is not one of this weapon's");
                if (rules.ultimates().contains(skill)) ultimates++;
                usedSkills.add(skill);
            }
        }
        if (ultimates > 1) bad.add(who + "has " + ultimates + " ultimates, at most one");
        if (!f.has("skills") || f.getAsJsonObject("skills").size() < 3) bad.add(who + "lists fewer than three skills");
        for (String p : strings(f, "passives")) {
            if (!rules.passives().contains(p)) bad.add(who + "the passive " + p + " is not implemented by this weapon");
            usedPassives.add(p);
        }
    }

    private static void requirements(String who, JsonObject f, int tier, List<String> bad) {
        if (!f.has("requires")) return;
        JsonArray req = f.getAsJsonArray("requires");
        if (req.size() > 4) bad.add(who + "asks for more than four materials");
        int total = 0;
        for (JsonElement e : req) {
            JsonObject r = e.getAsJsonObject();
            boolean item = r.has("item"), tag = r.has("tag");
            if (item == tag) bad.add(who + "a requirement needs exactly one of item and tag");
            String ref = item ? str(r, "item") : str(r, "tag");
            if (ref == null || !RESOURCE_ID.matcher(ref).matches()) bad.add(who + "the id " + ref + " is not a namespaced lower-case id");
            int count = r.has("count") ? r.get("count").getAsInt() : 0;
            int max = tier >= 3 ? 256 : 64;
            if (count < 4 || count > max) bad.add(who + "the count " + count + " of " + ref + " is outside 4.." + max);
            total += count;
        }
        if (tier == 1 && (total < 20 || total > 100)) bad.add(who + "a tier 1 form asks for " + total + " items, the fair range is 20 to 100");
        if (tier >= 3 && total < 40) bad.add(who + "a top form must be a real task, it asks for only " + total + " items");
        if (tier >= 1 && req.size() == 0) bad.add(who + "a form above the base needs at least one requirement");
    }

    private static void stats(String who, JsonObject f, String key, double share, List<String> bad) {
        if (!f.has(key)) return;
        for (Map.Entry<String, JsonElement> e : f.getAsJsonObject(key).entrySet()) {
            double[] range = STAT_RANGE.get(e.getKey());
            if (range == null) {
                bad.add(who + key + " uses the unknown stat " + e.getKey());
                continue;
            }
            double v = e.getValue().getAsDouble();
            double lo = range[0] * (range[0] < 0 ? share : 1.0), hi = range[1] * share;
            if (v < lo - 1e-9 || v > hi + 1e-9) bad.add(who + key + "." + e.getKey() + " = " + v + " is outside " + lo + ".." + hi);
        }
    }

    private static String str(JsonObject o, String key) { return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null; }

    private static int size(JsonObject o, String key) { return o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key).size() : 0; }

    private static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        if (o.has(key) && o.get(key).isJsonArray()) for (JsonElement e : o.getAsJsonArray(key)) out.add(e.getAsString());
        return out;
    }
}
