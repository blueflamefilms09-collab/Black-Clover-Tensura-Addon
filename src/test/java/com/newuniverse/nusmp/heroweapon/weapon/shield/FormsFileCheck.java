package com.newuniverse.nusmp.heroweapon.weapon.shield;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.HeroStyle;
import com.newuniverse.nusmp.heroweapon.HeroTrigger;
import com.newuniverse.nusmp.heroweapon.Sin;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Why: a forms file is data that a typo breaks quietly (a skill id the logic does not have, a stat key that does not exist, a form whose parent is never learned),
 * and the real validator lives in the forms system, which is built by someone else and merged later. This is the rule list of design.md section 6 written as a
 * checker of one file, so the Legendary Shield's and the Legendary Sword's files are held to the rules at build time whoever merges first. It is a test helper (the
 * Sword's test uses it too) and works on plain Gson trees. It returns the list of problems, empty when the file is good.
 */
public final class FormsFileCheck {
    private FormsFileCheck() {}

    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]*");
    private static final Set<String> STAT_KEYS = Set.of("attack_damage", "attack_speed", "armor", "armor_toughness", "max_health", "movement_speed", "knockback_resistance", "reach", "luck");
    private static final Set<String> SLOTS = Set.of("use", "sneak", "key1", "key2", "key3");
    private static final Map<String, Double> CAP = new HashMap<>();

    static {
        CAP.put("armor", 8.0);
        CAP.put("armor_toughness", 4.0);
        CAP.put("max_health", 12.0);
        CAP.put("attack_damage", 14.0);
        CAP.put("movement_speed", 0.2);
        CAP.put("knockback_resistance", 0.6);
        CAP.put("reach", 2.0);
        CAP.put("luck", 3.0);
    }

    /** What the checker needs to know about the weapon: its skills (id to whether it is an ultimate), the passives its logic implements, the Tensura ids that were verified. */
    public record Rules(HeroKind kind, Map<String, Boolean> skills, Set<String> passives, Set<String> tensuraItems, int minOrdinary, int maxOrdinary, int minCurse, int maxCurse) {}

    public static List<String> check(Path file, Rules r) {
        List<String> bad = new ArrayList<>();
        JsonObject root;
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(in).getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            bad.add("cannot read " + file + ": " + e);
            return bad;
        }
        if (!r.kind().itemId().equals(str(root, "weapon"))) bad.add("weapon is '" + str(root, "weapon") + "', expected " + r.kind().itemId());
        JsonArray forms = root.has("forms") && root.get("forms").isJsonArray() ? root.getAsJsonArray("forms") : new JsonArray();
        if (forms.isEmpty()) {
            bad.add("no forms");
            return bad;
        }
        Map<String, JsonObject> byId = new HashMap<>();
        List<JsonObject> list = new ArrayList<>();
        for (JsonElement e : forms) {
            JsonObject f = e.getAsJsonObject();
            String id = str(f, "id");
            if (!ID.matcher(id).matches()) bad.add("form id '" + id + "' is not lower snake case");
            if (byId.put(id, f) != null) bad.add("form id " + id + " is used twice");
            list.add(f);
        }
        JsonObject base = list.get(0);
        if (!"base".equals(str(base, "id")) || num(base, "tier") != 0 || arr(base, "parents").size() != 0 || arr(base, "requires").size() != 0 || num(base, "mastery") != 0 || bool(base, "curse"))
            bad.add("the first form must be the base form: id base, tier 0, no parents, no requirements, mastery 0, not a curse form");

        int ordinary = 0, curse = 0;
        Set<String> usedSkills = new HashSet<>(), usedPassives = new HashSet<>();
        for (JsonObject f : list) {
            String id = str(f, "id");
            boolean isCurse = bool(f, "curse");
            if (isCurse) curse++;
            else ordinary++;
            if (str(f, "name").isBlank()) bad.add(id + ": no name");
            if (str(f, "desc").isBlank()) bad.add(id + ": no description");
            for (String text : new String[]{str(f, "name"), str(f, "desc")})
                for (char ch : text.toCharArray()) if (ch > 126 || (ch < 32)) {
                    bad.add(id + ": non-ASCII character in the text");
                    break;
                }
            int tier = num(f, "tier");
            if (tier < 0 || tier > 5) bad.add(id + ": tier " + tier + " out of 0..5");
            // parents
            for (JsonElement p : arr(f, "parents")) {
                JsonObject parent = byId.get(p.getAsString());
                if (parent == null) bad.add(id + ": unknown parent " + p.getAsString());
                else if (num(parent, "tier") >= tier) bad.add(id + ": tier " + tier + " is not above its parent " + p.getAsString() + " (tier " + num(parent, "tier") + ")");
                if (p.getAsString().equals(id)) bad.add(id + ": its own parent");
            }
            if (!"base".equals(id)) {
                if (arr(f, "parents").size() == 0) bad.add(id + ": a form other than base needs a parent");
                int mastery = num(f, "mastery");
                if (mastery < 40 || mastery > 400) bad.add(id + ": mastery " + mastery + " out of 40..400");
                JsonArray req = arr(f, "requires");
                if (req.size() < 1 || req.size() > 4) bad.add(id + ": " + req.size() + " requirements, needs 1 to 4");
                for (JsonElement e : req) checkRequirement(bad, id, e.getAsJsonObject(), r, tier);
            }
            // stats
            checkStats(bad, id, f.has("stats") ? f.getAsJsonObject("stats") : new JsonObject(), false);
            checkStats(bad, id, f.has("abilityBonus") ? f.getAsJsonObject("abilityBonus") : new JsonObject(), true);
            // skills
            int ultimates = 0;
            if (f.has("skills")) {
                for (Map.Entry<String, JsonElement> s : f.getAsJsonObject("skills").entrySet()) {
                    String slot = s.getKey(), skill = s.getValue().getAsString();
                    if (!SLOTS.contains(slot)) bad.add(id + ": unknown slot " + slot);
                    if (HeroTrigger.bySlot(slot) == HeroTrigger.USE && r.kind().style() == HeroStyle.BLOCK) bad.add(id + ": a blocking weapon has no use slot");
                    Boolean ultimate = r.skills().get(skill);
                    if (ultimate == null) bad.add(id + ": unknown skill " + skill);
                    else {
                        usedSkills.add(skill);
                        if (ultimate) ultimates++;
                    }
                }
            }
            if (ultimates > 1) bad.add(id + ": " + ultimates + " ultimates");
            if (!"base".equals(id) && (!f.has("skills") || f.getAsJsonObject("skills").size() == 0)) bad.add(id + ": gives no skills");
            // passives
            for (JsonElement p : arr(f, "passives")) {
                if (!r.passives().contains(p.getAsString())) bad.add(id + ": unknown passive " + p.getAsString());
                usedPassives.add(p.getAsString());
            }
            // look and curse
            String look = str(f, "look");
            if (!look.equals("base") && !look.equals("curse")) bad.add(id + ": look '" + look + "'");
            if (isCurse) {
                Sin sin = Sin.byId(f.has("sin") && !f.get("sin").isJsonNull() ? f.get("sin").getAsString() : null);
                if (sin == null || !r.kind().sins().contains(sin)) bad.add(id + ": curse form with the sin '" + str(f, "sin") + "', the weapon's are " + r.kind().sins());
                if (!"curse".equals(look)) bad.add(id + ": a curse form uses the curse look");
                if (num(f, "corruptionPerSecond") <= 0) bad.add(id + ": a curse form needs corruptionPerSecond above 0");
            } else {
                if (f.has("sin") && !f.get("sin").isJsonNull()) bad.add(id + ": an ordinary form has no sin");
                if (num(f, "corruptionPerSecond") != 0) bad.add(id + ": an ordinary form has no corruption");
                if (!"base".equals(look)) bad.add(id + ": an ordinary form uses the base look");
            }
        }
        if (ordinary < r.minOrdinary() || ordinary > r.maxOrdinary()) bad.add(ordinary + " ordinary forms, expected " + r.minOrdinary() + " to " + r.maxOrdinary());
        if (curse < r.minCurse() || curse > r.maxCurse()) bad.add(curse + " curse forms, expected " + r.minCurse() + " to " + r.maxCurse());
        for (String s : r.skills().keySet()) if (!usedSkills.contains(s)) bad.add("no form lists the skill " + s);
        for (String p : r.passives()) if (!usedPassives.contains(p)) bad.add("no form has the passive " + p);
        // every sin of the weapon has at least one curse form
        for (Sin sin : r.kind().sins()) {
            boolean found = false;
            for (JsonObject f : list) if (bool(f, "curse") && sin.id().equals(str(f, "sin"))) found = true;
            if (!found) bad.add("no curse form for the sin " + sin.id());
        }
        // a parent chain from every form reaches the base form (no loop)
        for (JsonObject f : list) if (!reachesBase(f, byId, new HashSet<>())) bad.add(str(f, "id") + ": its parents do not lead back to the base form");
        return bad;
    }

    private static void checkRequirement(List<String> bad, String id, JsonObject q, Rules r, int tier) {
        boolean item = q.has("item"), tag = q.has("tag");
        if (item == tag) {
            bad.add(id + ": a requirement has exactly one of item and tag");
            return;
        }
        int count = num(q, "count");
        if (count < 4 || count > 256 || (count > 64 && tier < 4)) bad.add(id + ": requirement count " + count + " out of range");
        String what = item ? q.get("item").getAsString() : q.get("tag").getAsString();
        if (!what.matches("[a-z0-9_]+:[a-z0-9_/]+")) bad.add(id + ": bad id '" + what + "'");
        if (item && what.startsWith("tensura:") && !r.tensuraItems().contains(what)) bad.add(id + ": the Tensura item " + what + " was not verified against the id reference");
        if (!what.startsWith("minecraft:") && !what.startsWith("tensura:") && !what.startsWith("nusmp:") && !what.startsWith("c:")) bad.add(id + ": unknown namespace in " + what);
    }

    private static void checkStats(List<String> bad, String id, JsonObject s, boolean bonus) {
        for (Map.Entry<String, JsonElement> e : s.entrySet()) {
            String k = e.getKey();
            double v = e.getValue().getAsDouble();
            if (!STAT_KEYS.contains(k)) {
                bad.add(id + ": unknown stat " + k);
                continue;
            }
            if (k.equals("attack_speed")) {
                if (bonus ? Math.abs(v) > 0.25 : (v < -3.0 || v > 1.0)) bad.add(id + ": attack_speed " + v + " out of range");
                continue;
            }
            double cap = CAP.get(k) * (bonus ? 0.25 : 1.0);
            if (v < 0 || v > cap + 1e-9) bad.add(id + ": " + (bonus ? "abilityBonus " : "stat ") + k + " " + v + " out of 0.." + cap);
        }
    }

    private static boolean reachesBase(JsonObject f, Map<String, JsonObject> byId, Set<String> path) {
        String id = str(f, "id");
        if ("base".equals(id)) return true;
        if (!path.add(id)) return false;                                               // a loop: this form is among its own ancestors
        boolean ok = arr(f, "parents").size() > 0;
        for (JsonElement p : arr(f, "parents")) {
            JsonObject parent = byId.get(p.getAsString());
            if (parent == null || !reachesBase(parent, byId, path)) ok = false;
        }
        path.remove(id);
        return ok;
    }

    private static String str(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }

    private static int num(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : 0; }

    private static boolean bool(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() && o.get(k).getAsBoolean(); }

    private static JsonArray arr(JsonObject o, String k) { return o.has(k) && o.get(k).isJsonArray() ? o.getAsJsonArray(k) : new JsonArray(); }
}
