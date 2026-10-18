package com.newuniverse.nusmp.heroweapon.weapon.glassworld;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.HeroTrigger;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Why: the forms file of a weapon ({@code data/nusmp/hero/forms/<itemId>.json}) is data the game loads and the owner reads, and nothing can open it in a game
 * here, so its rules (docs/heroweapon/design.md section 6) are checked as code: the first form is the base form, every form has the fields the loader needs, the
 * tree is a tree, the requirements are small and well-formed, the stats stay inside the budget and the ability bonus inside a quarter of it, the slots are real
 * inputs, the skills and passives named are the ones the weapon's logic implements, a form carries at most one ultimate, and no skill or passive the weapon has
 * is left unused. It works on the parsed JSON (no game imports) so each weapon's own test can run it from the source tree; the registry check, whether an item
 * id really exists, is the forms system's own self-test in the running game. It lives in the test tree because only tests use it.
 */
public final class FormsRules {
    private FormsRules() {}

    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]*");
    private static final Pattern REGISTRY = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Set<String> NAMESPACES = Set.of("minecraft", "tensura", "nusmp", "c");

    /** The most a form's own stat may give (design.md section 6); attack speed is checked as a range. */
    private static final Map<String, Double> STAT_MAX = Map.of("armor", 8.0, "armor_toughness", 4.0, "max_health", 12.0, "attack_damage", 14.0,
            "movement_speed", 0.2, "knockback_resistance", 0.6, "reach", 2.0, "luck", 3.0);

    /**
     * What is wrong with a forms file; empty when it obeys the rules.
     *
     * @param root          the parsed file
     * @param kind          the weapon the file belongs to
     * @param skills        every skill id the weapon's logic has
     * @param ultimates     the ones among them that are ultimates
     * @param passives      every passive id the weapon's logic implements
     * @param minForms      the fewest forms the weapon should have (3 for a vassal weapon)
     * @param maxForms      the most forms (5 for a vassal weapon)
     */
    public static List<String> check(JsonObject root, HeroKind kind, Set<String> skills, Set<String> ultimates, Set<String> passives, int minForms, int maxForms) {
        List<String> bad = new ArrayList<>();
        if (!kind.itemId().equals(str(root, "weapon"))) bad.add("the file says weapon '" + str(root, "weapon") + "', not '" + kind.itemId() + "'");
        JsonArray forms = root.has("forms") && root.get("forms").isJsonArray() ? root.getAsJsonArray("forms") : new JsonArray();
        if (forms.size() < minForms || forms.size() > maxForms) bad.add("has " + forms.size() + " forms, the weapon should have " + minForms + " to " + maxForms);

        Map<String, Integer> tiers = new HashMap<>();
        Map<String, List<String>> parentsOf = new HashMap<>();
        Set<String> usedSkills = new HashSet<>(), usedPassives = new HashSet<>();
        for (int i = 0; i < forms.size(); i++) {
            JsonObject f = forms.get(i).isJsonObject() ? forms.get(i).getAsJsonObject() : new JsonObject();
            String id = str(f, "id");
            String at = "form " + (id.isEmpty() ? "#" + i : id);
            if (!ID.matcher(id).matches()) bad.add(at + ": the id must be lower case letters, digits and underscores");
            if (tiers.containsKey(id)) bad.add(at + ": the id is used twice");
            if (i == 0 && !HeroForm.BASE_ID.equals(id)) bad.add(at + ": the first form must be 'base'");
            if (i > 0 && HeroForm.BASE_ID.equals(id)) bad.add(at + ": only the first form is 'base'");
            String name = str(f, "name"), desc = str(f, "desc");
            if (name.length() < 3 || name.length() > 40) bad.add(at + ": the name must be 3 to 40 characters");
            if (desc.length() < 30 || desc.length() > 300) bad.add(at + ": the description must be 30 to 300 characters");
            if (!ascii(name) || !ascii(desc)) bad.add(at + ": the name and description must be plain ASCII");
            int tier = f.has("tier") && f.get("tier").isJsonPrimitive() ? f.get("tier").getAsInt() : -1;
            if (tier < 0 || tier > 5) bad.add(at + ": the tier must be 0 to 5");
            tiers.put(id, tier);

            List<String> parents = new ArrayList<>();
            for (JsonElement p : arr(f, "parents")) parents.add(p.getAsString());
            parentsOf.put(id, parents);
            JsonArray requires = arr(f, "requires");
            int mastery = f.has("mastery") ? f.get("mastery").getAsInt() : -1;
            if (i == 0) {
                if (tier != 0 || !parents.isEmpty() || requires.size() != 0 || mastery != 0) bad.add(at + ": the base form has tier 0, no parents, no requirements and no mastery");
            } else {
                if (tier < 1) bad.add(at + ": only the base form has tier 0");
                if (parents.isEmpty()) bad.add(at + ": a form after the base form needs a parent");
                if (mastery < 40 || mastery > 400) bad.add(at + ": the mastery must be 40 to 400");
                if (requires.size() < 1 || requires.size() > 4) bad.add(at + ": needs 1 to 4 requirements, has " + requires.size());
            }
            for (JsonElement r : requires) {
                JsonObject o = r.isJsonObject() ? r.getAsJsonObject() : new JsonObject();
                boolean item = o.has("item"), tag = o.has("tag");
                if (item == tag) bad.add(at + ": a requirement has exactly one of 'item' and 'tag'");
                String key = item ? str(o, "item") : str(o, "tag");
                if (!REGISTRY.matcher(key).matches()) bad.add(at + ": '" + key + "' is not a registry id");
                else if (!NAMESPACES.contains(key.substring(0, key.indexOf(':')))) bad.add(at + ": '" + key + "' is in a namespace the mod does not know");
                int count = o.has("count") ? o.get("count").getAsInt() : 0;
                if (count < 4 || count > 256) bad.add(at + ": the count of " + key + " must be 4 to 256");
                if (count > 64 && tier < 2) bad.add(at + ": a count over 64 is for a top form only");
            }

            Map<String, Double> stats = numbers(f, "stats"), bonus = numbers(f, "abilityBonus");
            for (Map.Entry<String, Double> e : stats.entrySet()) checkStat(bad, at + " stats", e.getKey(), e.getValue(), 1.0);
            for (Map.Entry<String, Double> e : bonus.entrySet()) checkStat(bad, at + " abilityBonus", e.getKey(), e.getValue(), 0.25);

            JsonObject slots = f.has("skills") && f.get("skills").isJsonObject() ? f.getAsJsonObject("skills") : new JsonObject();
            int ult = 0;
            for (Map.Entry<String, JsonElement> e : slots.entrySet()) {
                if (HeroTrigger.bySlot(e.getKey()) == null) bad.add(at + ": '" + e.getKey() + "' is not an input slot");
                String sid = e.getValue().getAsString();
                if (!skills.contains(sid)) bad.add(at + ": the skill '" + sid + "' is not one of the weapon's skills");
                usedSkills.add(sid);
                if (ultimates.contains(sid)) ult++;
            }
            if (ult > 1) bad.add(at + ": more than one ultimate");
            if (slots.size() < (i == 0 ? 3 : 4)) bad.add(at + ": lists only " + slots.size() + " skills");
            for (JsonElement p : arr(f, "passives")) {
                if (!passives.contains(p.getAsString())) bad.add(at + ": the passive '" + p.getAsString() + "' is not one the weapon implements");
                usedPassives.add(p.getAsString());
            }
            String look = f.has("look") ? str(f, "look") : "";
            if (!look.equals("base")) bad.add(at + ": a vassal weapon has only the base look, this says '" + look + "'");
            if (f.has("curse") && f.get("curse").getAsBoolean()) bad.add(at + ": this weapon has no curse series");
        }
        for (Map.Entry<String, List<String>> e : parentsOf.entrySet()) {
            for (String p : e.getValue()) {
                if (!tiers.containsKey(p)) bad.add("form " + e.getKey() + ": the parent '" + p + "' does not exist");
                else if (tiers.get(p) >= tiers.get(e.getKey())) bad.add("form " + e.getKey() + ": the parent '" + p + "' must be of a lower tier");
            }
        }
        for (String s : skills) if (!usedSkills.contains(s)) bad.add("the skill " + s + " is in no form");
        for (String p : passives) if (!usedPassives.contains(p)) bad.add("the passive " + p + " is in no form");
        return bad;
    }

    private static void checkStat(List<String> bad, String where, String key, double v, double share) {
        if (!HeroForm.STAT_KEYS.contains(key)) {
            bad.add(where + ": '" + key + "' is not a stat key");
            return;
        }
        if (key.equals("attack_speed")) {
            if (v < -3.0 * share - 1e-9 || v > 1.0 * share + 1e-9) bad.add(where + ": attack_speed " + v + " is outside " + (-3.0 * share) + " to " + share);
            return;
        }
        double max = STAT_MAX.get(key) * share;
        if (Math.abs(v) > max + 1e-9) bad.add(where + ": " + key + " " + v + " is outside -" + max + " to " + max);
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : "";
    }

    private static JsonArray arr(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonArray() ? o.getAsJsonArray(k) : new JsonArray();
    }

    private static Map<String, Double> numbers(JsonObject o, String k) {
        Map<String, Double> m = new HashMap<>();
        if (o.has(k) && o.get(k).isJsonObject()) for (Map.Entry<String, JsonElement> e : o.getAsJsonObject(k).entrySet()) m.put(e.getKey(), e.getValue().getAsDouble());
        return m;
    }

    private static boolean ascii(String s) {
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) < 32 || s.charAt(i) > 126) return false;
        return true;
    }
}
