package com.newuniverse.nusmp.heroweapon.weapon.vassalkit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: nothing of the hero weapons can be played by its author, and a forms file that breaks one of the rules of design.md section 6 is skipped or refused at
 * start-up, which would leave a weapon with no forms and no skills. This is a second, independent check of exactly the rules the five vassal weapons built with
 * this kit rely on (base form first, parents before children, tiers one deeper than their parents, requirement counts, stat budgets, slot words, skills that exist,
 * at most one ultimate, the curse fields), written on plain Gson trees so a JUnit test reads the shipped files straight from the source tree and the weapon's
 * in-game self-test reads the same files from the jar. Pure apart from Gson: no game classes.
 */
public final class FormsFile {
    private FormsFile() {}

    /** The slot words of design.md section 6. */
    public static final List<String> SLOTS = List.of("use", "sneak", "key1", "key2", "key3");

    /** The closed set of stat keys with the allowed range {low, high} of one form's {@code stats}; {@code abilityBonus} may use a quarter of it. */
    public static final Map<String, double[]> STAT_RANGE = new LinkedHashMap<>();

    static {
        STAT_RANGE.put("attack_damage", new double[]{0, 14});
        STAT_RANGE.put("attack_speed", new double[]{-3.0, 1.0});
        STAT_RANGE.put("armor", new double[]{0, 8});
        STAT_RANGE.put("armor_toughness", new double[]{0, 4});
        STAT_RANGE.put("max_health", new double[]{0, 12});
        STAT_RANGE.put("movement_speed", new double[]{0, 0.2});
        STAT_RANGE.put("knockback_resistance", new double[]{0, 0.6});
        STAT_RANGE.put("reach", new double[]{0, 2.0});
        STAT_RANGE.put("luck", new double[]{0, 3});
    }

    /** What a weapon's file is expected to contain. */
    public record Expect(String weapon, Set<String> skillIds, Set<String> ultimateIds, Set<String> sins, int minOrdinary, int maxOrdinary, int minCurse, int maxCurse) {}

    /** Every problem found in the file, as short sentences; empty when the file passes. */
    public static List<String> check(JsonObject root, Expect want) {
        List<String> bad = new ArrayList<>();
        if (!want.weapon().equals(str(root, "weapon"))) bad.add("the file names the weapon '" + str(root, "weapon") + "', expected '" + want.weapon() + "'");
        JsonArray forms = root.has("forms") && root.get("forms").isJsonArray() ? root.getAsJsonArray("forms") : null;
        if (forms == null || forms.size() == 0) {
            bad.add("no forms array");
            return bad;
        }
        Map<String, Integer> tiers = new HashMap<>();
        Set<String> used = new LinkedHashSet<>();
        int ordinary = 0, curse = 0;
        for (int i = 0; i < forms.size(); i++) {
            if (!forms.get(i).isJsonObject()) {
                bad.add("form #" + i + " is not an object");
                continue;
            }
            JsonObject f = forms.get(i).getAsJsonObject();
            String id = str(f, "id");
            String at = "form '" + id + "'";
            if (!id.matches("[a-z][a-z0-9_]*")) bad.add(at + ": the id must be lower-case snake");
            if (tiers.containsKey(id)) bad.add(at + ": the id is used twice");
            boolean isCurse = f.has("curse") && f.get("curse").isJsonPrimitive() && f.get("curse").getAsBoolean();
            if (isCurse) curse++;
            else ordinary++;
            if (str(f, "name").isBlank()) bad.add(at + ": no name");
            if (str(f, "desc").isBlank()) bad.add(at + ": no description");
            int tier = intOf(f, "tier", -1);
            List<String> parents = strings(f, "parents");
            int expectTier = 0;
            for (String p : parents) {
                if (!tiers.containsKey(p)) bad.add(at + ": the parent '" + p + "' is not an earlier form");
                else expectTier = Math.max(expectTier, tiers.get(p) + 1);
            }
            if (i == 0) {
                if (!"base".equals(id)) bad.add("the first form must be 'base'");
                if (tier != 0 || !parents.isEmpty() || isCurse) bad.add("the base form must be tier 0, without parents, and not a curse form");
                if (arr(f, "requires").size() != 0) bad.add("the base form must have no requirements");
            } else {
                if (tier != expectTier) bad.add(at + ": tier " + tier + " but its parents make it tier " + expectTier);
                if (parents.isEmpty()) bad.add(at + ": a form after the base needs a parent");
                checkRequires(at, arr(f, "requires"), tier, bad);
                int mastery = intOf(f, "mastery", -1);
                if (mastery < 40 || mastery > 400) bad.add(at + ": mastery " + mastery + " is outside 40..400");
            }
            if (tier > 5) bad.add(at + ": deeper than tier 5");
            tiers.put(id, Math.max(0, tier));
            checkStats(at, "stats", obj(f, "stats"), 1.0, bad);
            checkStats(at, "abilityBonus", obj(f, "abilityBonus"), 0.25, bad);
            int ultimates = 0;
            Set<String> slotsSeen = new LinkedHashSet<>();
            for (Map.Entry<String, JsonElement> e : obj(f, "skills").entrySet()) {
                if (!SLOTS.contains(e.getKey())) bad.add(at + ": unknown slot '" + e.getKey() + "'");
                if (!slotsSeen.add(e.getKey())) bad.add(at + ": slot '" + e.getKey() + "' twice");
                String skill = e.getValue().isJsonPrimitive() ? e.getValue().getAsString() : "";
                if (!want.skillIds().contains(skill)) bad.add(at + ": slot '" + e.getKey() + "' names '" + skill + "', which is not a skill of this weapon");
                if (want.ultimateIds().contains(skill)) ultimates++;
                used.add(skill);
            }
            if (ultimates > 1) bad.add(at + ": " + ultimates + " ultimates (the limit is one per form)");
            for (String passive : strings(f, "passives")) if (!passive.matches("[a-z][a-z0-9_]*")) bad.add(at + ": the passive '" + passive + "' must be lower-case snake");
            String look = str(f, "look");
            if (!look.equals("base") && !look.equals("curse")) bad.add(at + ": look must be base or curse");
            String sin = f.has("sin") && f.get("sin").isJsonPrimitive() ? f.get("sin").getAsString() : "";
            int corruption = intOf(f, "corruptionPerSecond", 0);
            if (isCurse) {
                if (!want.sins().contains(sin)) bad.add(at + ": the sin '" + sin + "' is not one of " + want.sins());
                if (corruption < 1 || corruption > 20) bad.add(at + ": a curse form needs corruptionPerSecond between 1 and 20, not " + corruption);
            } else {
                if (!sin.isEmpty() || corruption != 0) bad.add(at + ": a form that is not a curse form has no sin and no corruption");
            }
        }
        if (ordinary < want.minOrdinary() || ordinary > want.maxOrdinary()) bad.add(ordinary + " ordinary forms, expected " + want.minOrdinary() + ".." + want.maxOrdinary());
        if (curse < want.minCurse() || curse > want.maxCurse()) bad.add(curse + " curse forms, expected " + want.minCurse() + ".." + want.maxCurse());
        for (String skill : want.skillIds()) if (!used.contains(skill)) bad.add("the skill '" + skill + "' is in no form, so nobody could ever cast it");
        return bad;
    }

    private static void checkRequires(String at, JsonArray req, int tier, List<String> bad) {
        if (req.size() < 1 || req.size() > 4) bad.add(at + ": " + req.size() + " requirements, expected 1..4");
        int cap = tier >= 3 ? 256 : 64;
        for (JsonElement el : req) {
            if (!el.isJsonObject()) {
                bad.add(at + ": a requirement is not an object");
                continue;
            }
            JsonObject r = el.getAsJsonObject();
            boolean hasItem = r.has("item"), hasTag = r.has("tag");
            if (hasItem == hasTag) bad.add(at + ": a requirement names exactly one of item and tag");
            String ref = hasItem ? str(r, "item") : str(r, "tag");
            if (!ref.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) bad.add(at + ": '" + ref + "' is not a resource id");
            int count = intOf(r, "count", -1);
            if (count < 4 || count > cap) bad.add(at + ": count " + count + " of '" + ref + "' is outside 4.." + cap);
        }
    }

    private static void checkStats(String at, String field, JsonObject stats, double share, List<String> bad) {
        for (Map.Entry<String, JsonElement> e : stats.entrySet()) {
            double[] range = STAT_RANGE.get(e.getKey());
            if (range == null) {
                bad.add(at + ": " + field + " uses the unknown key '" + e.getKey() + "'");
                continue;
            }
            double v = e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isNumber() ? e.getValue().getAsDouble() : Double.NaN;
            if (Double.isNaN(v) || v < range[0] * share - 1e-9 || v > range[1] * share + 1e-9)
                bad.add(at + ": " + field + "." + e.getKey() + " = " + e.getValue() + " is outside " + range[0] * share + ".." + range[1] * share);
        }
    }

    /** Every item id the file's requirements name. */
    public static List<String> itemIds(JsonObject root) {
        return refs(root, "item");
    }

    /** Every item tag the file's requirements name. */
    public static List<String> tags(JsonObject root) {
        return refs(root, "tag");
    }

    private static List<String> refs(JsonObject root, String key) {
        List<String> out = new ArrayList<>();
        if (!root.has("forms") || !root.get("forms").isJsonArray()) return out;
        for (JsonElement fe : root.getAsJsonArray("forms")) {
            if (!fe.isJsonObject()) continue;
            for (JsonElement re : arr(fe.getAsJsonObject(), "requires")) {
                if (re.isJsonObject() && re.getAsJsonObject().has(key)) out.add(str(re.getAsJsonObject(), key));
            }
        }
        return out;
    }

    /** Every skill id the file's forms list, in order, without repeats. */
    public static List<String> skillIds(JsonObject root) {
        Set<String> out = new LinkedHashSet<>();
        if (root.has("forms") && root.get("forms").isJsonArray()) {
            for (JsonElement fe : root.getAsJsonArray("forms")) {
                if (!fe.isJsonObject()) continue;
                for (JsonElement s : obj(fe.getAsJsonObject(), "skills").asMap().values()) if (s.isJsonPrimitive()) out.add(s.getAsString());
            }
        }
        return new ArrayList<>(out);
    }

    // ------------------------------------------------------------------ small readers (a wrong type reads as "absent")
    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : "";
    }

    private static int intOf(JsonObject o, String key, int dflt) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive()) return dflt;
        JsonPrimitive p = e.getAsJsonPrimitive();
        return p.isNumber() ? p.getAsInt() : dflt;
    }

    private static JsonObject obj(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
    }

    private static JsonArray arr(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : new JsonArray();
    }

    private static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : arr(o, key)) if (e.isJsonPrimitive()) out.add(e.getAsString());
        return out;
    }
}
