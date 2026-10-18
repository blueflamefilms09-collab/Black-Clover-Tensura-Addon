package com.newuniverse.nusmp.elite.forge.rules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

/**
 * Why: the "Skill Bonus Resolvers" of the inventory. The forge pays a player for the smithing-minded skills they have (Hephaestus, the Runesmith's Insight of the
 * elite skills, Tensura's Great Sage and so on), but the elite skills are built by other builders and the Tensura ones may differ between versions, so the rule is
 * a DATA TABLE ({@code data/nusmp/elite/forge/skill_bonus.json}) keyed by skill registry id, and the resolver is pure: it is given a function "how well does this
 * player have the skill with this id" (0 not at all, 1 owned, 2 mastered) and sums the table. A skill id that does not exist simply answers 0, so the table works
 * whether or not a skill is installed. The sum is capped (default 16, equal to {@link QualityRoller#SKILL_CAP}). The mapping from the player to the function is the
 * only Minecraft part ({@code SkillProbe}). Malformed table entries are skipped, never fatal.
 */
public final class SkillBonusTable {
    /** The levels a probe answers. */
    public static final int NONE = 0, OWNED = 1, MASTERED = 2;

    private static final Pattern ID = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");

    /** One row: the skill id and the points it pays when owned and when mastered. */
    public record Entry(String id, int owned, int mastered) {}

    private final List<Entry> entries;
    private final int cap;

    public SkillBonusTable(List<Entry> entries, int cap) {
        this.entries = List.copyOf(entries);
        this.cap = Math.max(0, Math.min(cap, QualityRoller.SKILL_CAP));
    }

    public List<Entry> entries() { return entries; }

    public int cap() { return cap; }

    /** A row's contribution at a level. */
    public static int pay(Entry e, int level) { return level >= MASTERED ? Math.max(e.owned(), e.mastered()) : level >= OWNED ? e.owned() : 0; }

    /** The (capped) bonus for a player described by the probe. */
    public int resolve(ToIntFunction<String> probe) {
        int sum = 0;
        for (Entry e : entries) sum += pay(e, probe.applyAsInt(e.id()));
        return Math.min(cap, sum);
    }

    /** Skill id -> points, for every row that pays something (for the info command and the station screen), uncapped. */
    public Map<String, Integer> explain(ToIntFunction<String> probe) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Entry e : entries) {
            int p = pay(e, probe.applyAsInt(e.id()));
            if (p > 0) out.put(e.id(), p);
        }
        return out;
    }

    /** Parses the table; a missing or wrong-typed field falls back to a default and a bad row is skipped. Never throws. */
    public static SkillBonusTable parse(JsonObject root) {
        List<Entry> list = new ArrayList<>();
        int cap = QualityRoller.SKILL_CAP;
        if (root != null) {
            if (root.has("cap") && root.get("cap").isJsonPrimitive() && root.get("cap").getAsJsonPrimitive().isNumber()) cap = root.get("cap").getAsInt();
            JsonElement arr = root.get("skills");
            if (arr != null && arr.isJsonArray()) {
                for (JsonElement el : (JsonArray) arr) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    String id = str(o, "id");
                    if (id == null || !ID.matcher(id).matches()) continue;
                    int owned = num(o, "owned", 0), mastered = num(o, "mastered", owned);
                    if (owned < 0 || owned > 16 || mastered < 0 || mastered > 16) continue;
                    list.add(new Entry(id, owned, mastered));
                }
            }
        }
        return new SkillBonusTable(list, cap);
    }

    /** The table used when the data file is missing: only the skill that certainly exists in this mod. */
    public static SkillBonusTable fallback() { return new SkillBonusTable(List.of(new Entry("nusmp:hephaestus", 5, 8)), QualityRoller.SKILL_CAP); }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
    }

    private static int num(JsonObject o, String k, int def) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsInt() : def;
    }
}
