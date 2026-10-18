package com.newuniverse.nusmp.elite.records.rule;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.records.rule.Defs.RecordDef;
import com.newuniverse.nusmp.elite.records.rule.Defs.SynergyDef;
import com.newuniverse.nusmp.elite.records.rule.Defs.TitleDef;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: reads the three kinds of data file ({@code data/nusmp/elite/records/*.json}, {@code .../titles/*.json}, {@code .../titles/synergies/*.json}) into definitions. A file may be one object, an array
 * of objects, or an object with a {@code records} / {@code titles} / {@code synergies} array. A broken entry is skipped with a problem line and the rest of the file still loads (a datapack author's typo
 * must never take the whole system down). Pure (Gson only), used by the server's reload listener and by the tests.
 */
public final class DataParser {
    private DataParser() {}

    /** The result of one file: what parsed and what did not. */
    public record Parsed<T>(List<T> items, List<String> problems) {}

    private static List<JsonObject> entries(String json, String arrayKey, String where, List<String> problems) {
        List<JsonObject> out = new ArrayList<>();
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (RuntimeException e) {
            problems.add(where + ": not valid JSON (" + e.getMessage() + ")");
            return out;
        }
        if (root.isJsonArray()) {
            for (JsonElement e : root.getAsJsonArray()) if (e.isJsonObject()) out.add(e.getAsJsonObject());
        } else if (root.isJsonObject()) {
            JsonObject o = root.getAsJsonObject();
            JsonArray a = J.arr(o, arrayKey);
            if (a != null) {
                for (JsonElement e : a) if (e.isJsonObject()) out.add(e.getAsJsonObject());
            } else if (o.has("id")) {
                out.add(o);
            }
        } else {
            problems.add(where + ": the file must hold an object or an array");
        }
        return out;
    }

    // ------------------------------------------------------------------ records
    public static Parsed<RecordDef> records(String json, String where) {
        List<String> problems = new ArrayList<>();
        List<RecordDef> out = new ArrayList<>();
        for (JsonObject o : entries(json, "records", where, problems)) {
            String id = J.str(o, "id", "");
            String at = where + "#" + id;
            if (!Defs.validId(id)) { problems.add(where + ": bad record id '" + id + "' (use a-z 0-9 _ - .)"); continue; }
            String name = J.str(o, "name", "");
            if (name.isEmpty()) { problems.add(at + ": 'name' is missing"); continue; }
            Category cat = Category.parse(J.str(o, "category", ""), null);
            if (cat == null) { problems.add(at + ": unknown category '" + J.str(o, "category", "") + "'"); continue; }
            Rarity rar = Rarity.parse(J.str(o, "rarity", "common"), null);
            if (rar == null) { problems.add(at + ": unknown rarity '" + J.str(o, "rarity", "") + "'"); continue; }
            List<String> cp = new ArrayList<>();
            Condition cond = Condition.parse(o.get("condition"), at, cp);
            problems.addAll(cp);
            if (cond == null || !ConditionEvaluator.validate(cond).isEmpty()) continue;
            out.add(new RecordDef(id, name, J.str(o, "description", ""), cat, rar, J.bool(o, "hidden", false), cond));
        }
        return new Parsed<>(out, problems);
    }

    // ------------------------------------------------------------------ titles
    public static Parsed<TitleDef> titles(String json, String where) {
        List<String> problems = new ArrayList<>();
        List<TitleDef> out = new ArrayList<>();
        for (JsonObject o : entries(json, "titles", where, problems)) {
            String id = J.str(o, "id", "");
            String at = where + "#" + id;
            if (!Defs.validId(id)) { problems.add(where + ": bad title id '" + id + "'"); continue; }
            String name = J.str(o, "name", "");
            if (name.isEmpty()) { problems.add(at + ": 'name' is missing"); continue; }
            Rarity rar = Rarity.parse(J.str(o, "rarity", "common"), null);
            if (rar == null) { problems.add(at + ": unknown rarity '" + J.str(o, "rarity", "") + "'"); continue; }
            Set<String> tags = new LinkedHashSet<>();
            for (String t : J.strings(o, "tags")) if (!t.isBlank()) tags.add(t.trim().toLowerCase());
            List<Requirement> reqs = new ArrayList<>();
            JsonArray ra = J.arr(o, "requirements");
            if (ra != null) {
                for (JsonElement e : ra) {
                    Requirement r = Requirement.parse(e, at, problems);
                    if (r != null) reqs.add(r);
                }
            }
            if (reqs.isEmpty()) { problems.add(at + ": a title needs at least one valid requirement"); continue; }
            Bonuses b = Bonuses.parse(J.obj(o, "bonuses"), at, problems);
            out.add(new TitleDef(id, name, J.str(o, "description", ""), rar, tags, List.copyOf(reqs), b));
        }
        return new Parsed<>(out, problems);
    }

    // ------------------------------------------------------------------ synergies
    public static Parsed<SynergyDef> synergies(String json, String where) {
        List<String> problems = new ArrayList<>();
        List<SynergyDef> out = new ArrayList<>();
        for (JsonObject o : entries(json, "synergies", where, problems)) {
            String id = J.str(o, "id", "");
            String at = where + "#" + id;
            if (!Defs.validId(id)) { problems.add(where + ": bad synergy id '" + id + "'"); continue; }
            String name = J.str(o, "name", "");
            if (name.isEmpty()) { problems.add(at + ": 'name' is missing"); continue; }
            List<String> requires = J.strings(o, "requires");
            String tag = J.str(o, "tag", "").trim().toLowerCase();
            int count = (int) J.num(o, "count", tag.isEmpty() ? 0 : 2);
            if (requires.isEmpty() && (tag.isEmpty() || count < 1)) { problems.add(at + ": a synergy needs 'requires' titles or a 'tag' with a 'count'"); continue; }
            Bonuses b = Bonuses.parse(J.obj(o, "bonuses"), at, problems);
            out.add(new SynergyDef(id, name, J.str(o, "description", ""), List.copyOf(requires), tag, tag.isEmpty() ? 0 : count, b));
        }
        return new Parsed<>(out, problems);
    }
}
