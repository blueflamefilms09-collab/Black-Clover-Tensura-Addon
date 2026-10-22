package com.newuniverse.nusmp.charset;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Why: a character's spell set is data (src/main/resources/data/nusmp/character_spells/&lt;canon_id&gt;.json, schema in docs/magic/character_spells.md), so a content builder adds a character without
 * touching the engine. This pure class (no game classes) parses one file and checks it hard: a file with any problem is not used at all and that character keeps today's generic grimoire, so a
 * typo can never hide a page by accident. {@link #problems} is also what the JUnit tests and the CI self-test run on every shipped file.
 */
public final class CharacterSetLoader {
    private CharacterSetLoader() {}

    public static final int SCHEMA = 1;
    public static final List<String> ORIGINS = List.of("anime", "manga", "anime_only", "manga_only");
    private static final Pattern ID = Pattern.compile("[a-z0-9_]+");
    private static final int MAX_DEPTH = 6;

    /** Parses and validates; throws IllegalArgumentException with every problem in the message. */
    public static CharacterSet parse(String json) {
        List<String> bad = new ArrayList<>();
        CharacterSet set = parse(json, bad);
        if (set == null || !bad.isEmpty()) throw new IllegalArgumentException(String.join("; ", bad));
        return set;
    }

    /** Parses what can be parsed and puts every problem (syntax, schema, rules) into {@code bad}; returns null if nothing usable came out. */
    public static CharacterSet parse(String json, List<String> bad) {
        JsonObject root;
        try {
            JsonElement e = JsonParser.parseString(json);
            if (!e.isJsonObject()) { bad.add("the file is not a JSON object"); return null; }
            root = e.getAsJsonObject();
        } catch (RuntimeException ex) {
            bad.add("not valid JSON: " + ex.getMessage());
            return null;
        }
        if (!root.has("schema")) bad.add("'schema' is missing");
        else try { if (root.get("schema").getAsInt() != SCHEMA) bad.add("unknown schema " + root.get("schema")); } catch (RuntimeException ex) { bad.add("schema must be a number"); }
        String canon = str(root, "canon", bad, true), magic = str(root, "magic", bad, true), book = str(root, "book", bad, true);
        Map<String, String> sources = new LinkedHashMap<>();
        if (root.has("sources") && root.get("sources").isJsonObject())
            for (var en : root.getAsJsonObject("sources").entrySet()) sources.put(en.getKey(), en.getValue().isJsonPrimitive() ? en.getValue().getAsString() : "");
        else bad.add("sources must be an object (source id -> where it came from)");
        List<CharacterSet.Spell> spells = new ArrayList<>();
        if (root.has("spells") && root.get("spells").isJsonArray()) {
            int k = 0;
            for (JsonElement el : root.getAsJsonArray("spells")) {
                k++;
                if (!el.isJsonObject()) { bad.add("spells[" + k + "] is not an object"); continue; }
                JsonObject o = el.getAsJsonObject();
                List<String> local = new ArrayList<>();
                String page = str(o, "page", local, true);
                String where = "spell " + (page.isEmpty() ? "#" + k : page);
                SpellRule rule = o.has("acquire") ? rule(o.get("acquire"), local, 0) : null;
                if (rule == null && !o.has("acquire")) local.add("acquire is missing");
                spells.add(new CharacterSet.Spell(page, str(o, "name", local, true), str(o, "ja", local, false), str(o, "origin", local, true),
                        o.has("exclusive") && o.get("exclusive").isJsonPrimitive() && o.get("exclusive").getAsBoolean(), str(o, "source", local, true),
                        rule == null ? new SpellRule.Start() : rule, str(o, "note", local, false)));
                for (String s : local) bad.add(where + ": " + s);
            }
        } else bad.add("spells must be an array");
        List<CharacterSet.Planned> planned = new ArrayList<>();
        if (root.has("planned") && root.get("planned").isJsonArray()) {
            int k = 0;
            for (JsonElement el : root.getAsJsonArray("planned")) {
                k++;
                if (!el.isJsonObject()) { bad.add("planned[" + k + "] is not an object"); continue; }
                JsonObject o = el.getAsJsonObject();
                List<String> local = new ArrayList<>();
                planned.add(new CharacterSet.Planned(str(o, "name", local, true), str(o, "ja", local, false), str(o, "origin", local, true), str(o, "source", local, false), str(o, "note", local, false)));
                for (String s : local) bad.add("planned #" + k + ": " + s);
            }
        }
        CharacterSet set = new CharacterSet(canon, magic, book, sources, spells, planned, str(root, "notes", bad, false));
        bad.addAll(problems(set));
        return set;
    }

    /** Everything wrong with an already built set (pure checks only: the pages are resolved against the real books by the self-test). */
    public static List<String> problems(CharacterSet set) {
        List<String> bad = new ArrayList<>();
        if (!ID.matcher(set.canon()).matches()) bad.add("canon id '" + set.canon() + "' must be lower-case letters, digits and _");
        if (!set.book().startsWith("book_")) bad.add("book '" + set.book() + "' must be a book registry path such as book_wind");
        if (set.spells().isEmpty()) bad.add("a set needs at least one spell");
        Set<String> pages = new HashSet<>();
        for (CharacterSet.Spell s : set.spells()) {
            if (!ID.matcher(s.page()).matches()) bad.add("page id '" + s.page() + "' must be lower-case letters, digits and _");
            if (!pages.add(s.page())) bad.add("page " + s.page() + " is listed twice");
            if (!ORIGINS.contains(s.origin())) bad.add(s.page() + ": origin '" + s.origin() + "' must be one of " + ORIGINS);
            if (!s.source().isEmpty() && !set.sources().containsKey(s.source())) bad.add(s.page() + ": source '" + s.source() + "' is not declared in sources");
            if (s.name().isBlank()) bad.add(s.page() + ": name is empty");
        }
        Map<String, Set<String>> deps = new HashMap<>();
        for (CharacterSet.Spell s : set.spells()) {
            Set<String> refs = s.rule().pages();
            Set<String> own = new HashSet<>();
            for (String r : refs) {
                if (SpellRule.UNIVERSAL_PAGES.contains(r)) continue;
                if (!pages.contains(r)) bad.add(s.page() + ": the rule names page '" + r + "' which is not in this set");
                else if (r.equals(s.page())) bad.add(s.page() + ": the rule names its own page");
                else own.add(r);
            }
            deps.put(s.page(), own);
            checkRule(s.rule(), s.page(), bad);
        }
        for (String p : cyclic(deps)) bad.add(p + ": its rule depends on itself through other pages (it could never open)");
        return bad;
    }

    private static void checkRule(SpellRule r, String where, List<String> bad) {
        if (r instanceof SpellRule.Mastery m && (m.min() < 0 || m.min() > 1)) bad.add(where + ": mastery must be between 0 and 1");
        else if (r instanceof SpellRule.PageUses u && (u.count() < 1 || u.count() > 1000)) bad.add(where + ": page_uses count must be between 1 and 1000");
        else if (r instanceof SpellRule.Trust t && (t.min() < 0 || t.min() > 100)) bad.add(where + ": trust must be between 0 and 100");
        else if (r instanceof SpellRule.Flag f && !SpellRule.FLAGS.contains(f.name())) bad.add(where + ": unknown flag '" + f.name() + "' (known: " + SpellRule.FLAGS + ")");
        else if (r instanceof SpellRule.All a) { if (a.of().isEmpty()) bad.add(where + ": 'all' needs at least one rule"); for (SpellRule c : a.of()) checkRule(c, where, bad); }
        else if (r instanceof SpellRule.Any a) { if (a.of().isEmpty()) bad.add(where + ": 'any' needs at least one rule"); for (SpellRule c : a.of()) checkRule(c, where, bad); }
    }

    /** The pages that sit on a dependency cycle. */
    static List<String> cyclic(Map<String, Set<String>> deps) {
        List<String> out = new ArrayList<>();
        for (String start : deps.keySet()) {
            Set<String> seen = new HashSet<>();
            java.util.ArrayDeque<String> todo = new java.util.ArrayDeque<>(deps.get(start));
            boolean loop = false;
            while (!todo.isEmpty() && !loop) {
                String n = todo.pop();
                if (n.equals(start)) { loop = true; break; }
                if (seen.add(n)) todo.addAll(deps.getOrDefault(n, Set.of()));
            }
            if (loop) out.add(start);
        }
        return out;
    }

    // ---------------------------------------------------------------- rule parsing
    private static SpellRule rule(JsonElement el, List<String> bad, int depth) {
        if (depth > MAX_DEPTH) { bad.add("rules are nested too deeply"); return null; }
        if (!el.isJsonObject()) { bad.add("acquire must be an object"); return null; }
        JsonObject o = el.getAsJsonObject();
        String type = str(o, "type", bad, true);
        try {
            switch (type) {
                case "start": return new SpellRule.Start();
                case "mastery": return new SpellRule.Mastery(num(o, "min", bad));
                case "page_uses": return new SpellRule.PageUses(str(o, "page", bad, true), (int) num(o, "count", bad));
                case "page_open": return new SpellRule.PageOpen(str(o, "page", bad, true));
                case "trust": return new SpellRule.Trust((int) num(o, "min", bad));
                case "flag": return new SpellRule.Flag(str(o, "name", bad, true));
                case "all":
                case "any": {
                    List<SpellRule> kids = new ArrayList<>();
                    if (!o.has("of") || !o.get("of").isJsonArray()) { bad.add("'" + type + "' needs an 'of' array"); return null; }
                    JsonArray arr = o.getAsJsonArray("of");
                    for (JsonElement k : arr) { SpellRule r = rule(k, bad, depth + 1); if (r != null) kids.add(r); }
                    return type.equals("all") ? new SpellRule.All(kids) : new SpellRule.Any(kids);
                }
                default:
                    if (!type.isEmpty()) bad.add("unknown acquire type '" + type + "' (start, mastery, page_uses, page_open, trust, flag, all, any)");
                    return null;
            }
        } catch (RuntimeException ex) {
            bad.add("bad acquire rule: " + ex.getMessage());
            return null;
        }
    }

    private static double num(JsonObject o, String key, List<String> bad) {
        if (!o.has(key) || !o.get(key).isJsonPrimitive() || !o.get(key).getAsJsonPrimitive().isNumber()) { bad.add("'" + key + "' must be a number"); return 0; }
        return o.get(key).getAsDouble();
    }

    private static String str(JsonObject o, String key, List<String> bad, boolean required) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            if (required) bad.add("'" + key + "' is missing");
            return "";
        }
        JsonElement e = o.get(key);
        if (!e.isJsonPrimitive()) { bad.add("'" + key + "' must be text"); return ""; }
        String s = e.getAsString();
        if (required && s.isBlank()) bad.add("'" + key + "' is empty");
        return s;
    }
}
