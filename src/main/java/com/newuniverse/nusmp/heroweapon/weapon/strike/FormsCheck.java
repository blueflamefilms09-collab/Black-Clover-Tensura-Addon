package com.newuniverse.nusmp.heroweapon.weapon.strike;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Why: a weapon's forms file ({@code data/nusmp/hero/forms/<itemId>.json}) is data nobody can play-test here, and a wrong skill id, a missing parent or a stat over the budget
 * would only show up in the running game as a skipped form. The forms system has its own validator of the structure (the rules of design.md section 6); this class repeats the
 * structural rules that matter to a weapon builder (so the weapon's own unit test and self-test need nothing from that system) and adds what only the weapon knows: every skill
 * id in a form is one the weapon has, a form has at most one ultimate, every passive is one the logic implements, and every skill and every passive of the weapon is used by
 * some form (an unreachable technique is dead code). Structure: the base form first with no parents, requirements or mastery; every other form with parents that exist, a tier
 * one deeper than its deepest parent, one to four requirements of four to sixty-four items (an item id or a tag, never both; up to 256 only for a top form; a curse form may ask
 * for nothing), mastery 40 to 400, stat keys from the closed set inside the budget (an ability bonus at most a quarter of it), slot words, a look of base or curse, and a curse
 * form with a sin and a corruption rate. Whether an item id exists in the real registry is NOT checked here (that needs the game). Pure: the only library is Gson, which the
 * game ships.
 */
public final class FormsCheck {
    private FormsCheck() {}

    /** What the weapon's code says it has. */
    public record Known(String itemId, Set<String> skillIds, Set<String> ultimateIds, Set<String> passives, int minOrdinary, int maxOrdinary, boolean curseAllowed) {}

    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]{1,31}");
    private static final Pattern RESOURCE = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Set<String> SLOTS = Set.of("use", "sneak", "key1", "key2", "key3");
    private static final Set<String> SINS = Set.of("wrath", "pride", "lust", "envy", "greed", "sloth", "gluttony");
    /** Stat key to {lowest, highest} a form's stats may be (design.md section 6). */
    private static final Map<String, double[]> LIMITS = new HashMap<>();

    static {
        LIMITS.put("attack_damage", new double[]{-14, 14});
        LIMITS.put("attack_speed", new double[]{-3, 1});
        LIMITS.put("armor", new double[]{-8, 8});
        LIMITS.put("armor_toughness", new double[]{-4, 4});
        LIMITS.put("max_health", new double[]{-12, 12});
        LIMITS.put("movement_speed", new double[]{-0.2, 0.2});
        LIMITS.put("knockback_resistance", new double[]{-0.6, 0.6});
        LIMITS.put("reach", new double[]{-2, 2});
        LIMITS.put("luck", new double[]{-3, 3});
    }

    public static Set<String> statKeys() { return LIMITS.keySet(); }

    /** Every problem found in the file; empty when it passes. */
    public static List<String> problems(JsonObject root, Known known) {
        List<String> out = new ArrayList<>();
        if (!known.itemId().equals(str(root, "weapon"))) out.add("weapon must be '" + known.itemId() + "'");
        JsonArray forms = root.has("forms") && root.get("forms").isJsonArray() ? root.getAsJsonArray("forms") : null;
        if (forms == null || forms.size() == 0) {
            out.add("forms must be a non-empty array");
            return out;
        }
        Set<String> hasChildren = new HashSet<>();
        for (JsonElement e : forms) if (e.isJsonObject()) for (JsonElement p : arr(e.getAsJsonObject(), "parents")) if (p.isJsonPrimitive()) hasChildren.add(p.getAsString());
        Map<String, Integer> tierOf = new HashMap<>();
        Set<String> usedSkills = new HashSet<>(), usedPassives = new HashSet<>();
        int ordinary = 0;
        for (int i = 0; i < forms.size(); i++) {
            if (!forms.get(i).isJsonObject()) {
                out.add("form #" + i + " is not an object");
                continue;
            }
            JsonObject f = forms.get(i).getAsJsonObject();
            String id = str(f, "id");
            String at = "form '" + id + "'";
            if (!ID.matcher(id).matches()) out.add(at + ": the id must be lower-case snake, 2 to 32 characters");
            if (tierOf.containsKey(id)) out.add(at + ": the id is used twice");
            int tier = f.has("tier") && f.get("tier").isJsonPrimitive() ? f.get("tier").getAsInt() : -1;
            if (tier < 0 || tier > 5) out.add(at + ": tier must be 0 to 5");
            tierOf.put(id, tier);
            if (str(f, "name").isBlank() || str(f, "name").length() > 40) out.add(at + ": name must be 1 to 40 characters");
            if (str(f, "desc").isBlank() || str(f, "desc").length() > 500) out.add(at + ": desc must be 1 to 500 characters");
            boolean curse = f.has("curse") && f.get("curse").isJsonPrimitive() && f.get("curse").getAsBoolean();
            if (!curse) ordinary++;
            if (i == 0) {
                if (!"base".equals(id) || tier != 0) out.add("the first form must be 'base' on tier 0");
                if (arr(f, "parents").size() != 0 || arr(f, "requires").size() != 0) out.add("the base form has no parents and no requirements");
                if (f.has("abilityBonus") && f.get("abilityBonus").isJsonObject() && f.getAsJsonObject("abilityBonus").size() != 0) out.add("the base form has no abilityBonus");
                if (f.has("mastery") && f.get("mastery").getAsInt() != 0) out.add("the base form has mastery 0");
            } else {
                parents(out, at, f, tierOf, tier, forms);
                requirements(out, at, f, tier, curse, !hasChildren.contains(id));
                int mastery = f.has("mastery") ? f.get("mastery").getAsInt() : -1;
                if (mastery < 40 || mastery > 400) out.add(at + ": mastery must be 40 to 400");
            }
            stats(out, at, f.has("stats") ? f.get("stats") : null, 1.0, "stats");
            stats(out, at, f.has("abilityBonus") ? f.get("abilityBonus") : null, 0.25, "abilityBonus");
            skills(out, at, f, known, usedSkills);
            if (arr(f, "passives").size() > 8) out.add(at + ": at most 8 passives");
            for (JsonElement p : arr(f, "passives")) {
                String pid = p.isJsonPrimitive() ? p.getAsString() : "";
                usedPassives.add(pid);
                if (!known.passives().contains(pid)) out.add(at + ": the passive '" + pid + "' is not one the weapon's logic implements");
            }
            String look = str(f, "look");
            if (!look.equals("base")) out.add(at + ": a Seven Star or Vassal weapon has no cursed mesh, so every form keeps look 'base'");
            if (curse) {
                if (!known.curseAllowed()) out.add(at + ": this weapon carries no curse series");
                if (!SINS.contains(str(f, "sin"))) out.add(at + ": a curse form needs a sin of the seven");
                int cps = f.has("corruptionPerSecond") ? f.get("corruptionPerSecond").getAsInt() : 0;
                if (cps < 1 || cps > 100) out.add(at + ": corruptionPerSecond must be 1 to 100");
            } else {
                if (f.has("sin") && !f.get("sin").isJsonNull()) out.add(at + ": only a curse form has a sin");
                if (f.has("corruptionPerSecond") && f.get("corruptionPerSecond").getAsInt() != 0) out.add(at + ": only a curse form has corruption");
            }
        }
        if (ordinary < known.minOrdinary() || ordinary > known.maxOrdinary()) out.add("the weapon needs " + known.minOrdinary() + " to " + known.maxOrdinary() + " ordinary forms, it has " + ordinary);
        for (String s : known.skillIds()) if (!usedSkills.contains(s)) out.add("the skill " + s + " is listed by no form");
        for (String p : known.passives()) if (!usedPassives.contains(p)) out.add("the passive " + p + " is used by no form");
        return out;
    }

    private static void parents(List<String> out, String at, JsonObject f, Map<String, Integer> tierOf, int tier, JsonArray forms) {
        JsonArray parents = arr(f, "parents");
        if (parents.size() == 0) out.add(at + ": a form after the base needs at least one parent");
        int deepest = 0;
        for (JsonElement p : parents) {
            String pid = p.isJsonPrimitive() ? p.getAsString() : "";
            boolean exists = false;
            for (JsonElement e : forms) if (e.isJsonObject() && pid.equals(str(e.getAsJsonObject(), "id"))) exists = true;
            if (!exists) out.add(at + ": the parent '" + pid + "' does not exist");
            else if (!tierOf.containsKey(pid)) out.add(at + ": the parent '" + pid + "' must be listed before the form");
            else deepest = Math.max(deepest, tierOf.get(pid));
        }
        if (parents.size() > 0 && tier != deepest + 1) out.add(at + ": tier is " + tier + " but its deepest parent is on tier " + deepest + ", so it must be " + (deepest + 1));
    }

    private static void requirements(List<String> out, String at, JsonObject f, int tier, boolean curse, boolean top) {
        JsonArray req = arr(f, "requires");
        if (req.size() > 4 || (req.size() < 1 && !curse)) out.add(at + ": requires needs one to four entries (a curse form may have none), it has " + req.size());
        Set<String> seen = new HashSet<>();
        for (JsonElement e : req) {
            if (!e.isJsonObject()) {
                out.add(at + ": a requirement is not an object");
                continue;
            }
            JsonObject r = e.getAsJsonObject();
            boolean item = r.has("item"), tag = r.has("tag");
            if (item == tag) out.add(at + ": a requirement has exactly one of item and tag");
            String key = item ? str(r, "item") : str(r, "tag");
            if (!RESOURCE.matcher(key).matches()) out.add(at + ": '" + key + "' is not a namespaced id");
            if (!seen.add((tag ? "#" : "") + key)) out.add(at + ": '" + key + "' is required twice");
            int count = r.has("count") ? r.get("count").getAsInt() : -1;
            int max = tier >= 3 && top ? 256 : 64;
            if (count < 4 || count > max) out.add(at + ": the count of " + key + " must be 4 to " + max + ", it is " + count);
        }
    }

    private static void stats(List<String> out, String at, JsonElement e, double share, String what) {
        if (e == null || !e.isJsonObject()) {
            out.add(at + ": " + what + " must be an object");
            return;
        }
        for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
            double[] lim = LIMITS.get(en.getKey());
            if (lim == null) {
                out.add(at + ": '" + en.getKey() + "' is not a stat key");
                continue;
            }
            double v = en.getValue().getAsDouble();
            double lo = lim[0] * share, hi = lim[1] * share;
            if (v > hi + 1.0E-9 || v < lo - 1.0E-9) out.add(at + ": " + what + "." + en.getKey() + " = " + v + " is outside " + lo + " to " + hi);
        }
    }

    private static void skills(List<String> out, String at, JsonObject f, Known known, Set<String> used) {
        if (!f.has("skills") || !f.get("skills").isJsonObject()) {
            out.add(at + ": skills must be an object");
            return;
        }
        Set<String> inForm = new HashSet<>();
        int ultimates = 0;
        for (Map.Entry<String, JsonElement> en : f.getAsJsonObject("skills").entrySet()) {
            String id = en.getValue().isJsonPrimitive() ? en.getValue().getAsString() : "";
            if (!SLOTS.contains(en.getKey())) out.add(at + ": '" + en.getKey() + "' is not a slot (use, sneak, key1, key2, key3)");
            if (!known.skillIds().contains(id)) out.add(at + ": the skill '" + id + "' is not one of the weapon's");
            if (!inForm.add(id)) out.add(at + ": the skill '" + id + "' is on two slots");
            if (known.ultimateIds().contains(id)) ultimates++;
            used.add(id);
        }
        if (ultimates > 1) out.add(at + ": a form has at most one ultimate, this one " + ultimates);
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
    }

    private static JsonArray arr(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : new JsonArray();
    }
}
