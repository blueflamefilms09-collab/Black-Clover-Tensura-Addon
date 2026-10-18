package com.newuniverse.nusmp.elite.skills.evolution;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the evolutions are DATA, not code: {@code data/nusmp/elite/skills/evolutions.json} maps a skill id to the skills it can become, and each target to its list of requirements
 * ({@code {"trigger": "kill_evolution", "params": {"count": 200}}}). This class parses that into immutable paths and reports mistakes (unknown trigger, not an object, missing target)
 * as text instead of throwing, and it SKIPS a broken path rather than guessing, because a path whose requirement was misspelt would otherwise evolve for free. Later files (data packs)
 * override earlier ones per (from, to). No Minecraft types, so it is unit tested.
 */
public final class EvolutionTable {
    public record Req(EvolutionTrigger trigger, Params params) {}

    public record Path(String from, String to, List<Req> reqs) {}

    private final Map<String, Path> paths = new LinkedHashMap<>();
    private final List<String> errors = new ArrayList<>();

    public List<Path> paths() { return List.copyOf(paths.values()); }

    public List<String> errors() { return List.copyOf(errors); }

    public List<Path> from(String skillId) {
        List<Path> out = new ArrayList<>();
        for (Path p : paths.values()) if (p.from().equals(skillId)) out.add(p);
        return out;
    }

    public Path get(String from, String to) { return paths.get(from + ">" + to); }

    public int size() { return paths.size(); }

    /** Parses one file's text into this table (adds or replaces paths). Never throws. */
    public EvolutionTable add(String json, String where) {
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (RuntimeException e) {
            errors.add(where + ": not valid JSON (" + e.getMessage() + ")");
            return this;
        }
        if (!root.isJsonObject()) {
            errors.add(where + ": the top level must be an object {skill: {target: [requirements]}}");
            return this;
        }
        for (Map.Entry<String, JsonElement> from : root.getAsJsonObject().entrySet()) {
            if (from.getKey().startsWith("_")) continue;                                  // "_comment" and friends
            if (!from.getValue().isJsonObject()) {
                errors.add(where + ": " + from.getKey() + " must map targets to requirement lists");
                continue;
            }
            for (Map.Entry<String, JsonElement> to : from.getValue().getAsJsonObject().entrySet()) {
                if (to.getKey().startsWith("_")) continue;
                parsePath(from.getKey(), to.getKey(), to.getValue(), where);
            }
        }
        return this;
    }

    private void parsePath(String from, String to, JsonElement list, String where) {
        String name = where + ": " + from + " -> " + to;
        if (!list.isJsonArray()) {
            errors.add(name + " must be a list of requirements");
            return;
        }
        List<Req> reqs = new ArrayList<>();
        JsonArray arr = list.getAsJsonArray();
        for (int i = 0; i < arr.size(); i++) {
            JsonElement e = arr.get(i);
            if (!e.isJsonObject() || !e.getAsJsonObject().has("trigger")) {
                errors.add(name + " requirement #" + (i + 1) + " needs a \"trigger\"");
                return;
            }
            JsonObject o = e.getAsJsonObject();
            String id = o.get("trigger").getAsString();
            EvolutionTrigger t = Triggers.get(id);
            if (t == null) {
                errors.add(name + " uses the unknown trigger \"" + id + "\" (path skipped; known: " + Triggers.ids() + ")");
                return;
            }
            reqs.add(new Req(t, o.has("params") && o.get("params").isJsonObject() ? params(o.getAsJsonObject("params")) : Params.EMPTY));
        }
        paths.put(from + ">" + to, new Path(from, to, List.copyOf(reqs)));
    }

    static Params params(JsonObject o) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : o.entrySet()) m.put(e.getKey(), plain(e.getValue()));
        return new Params(m);
    }

    private static Object plain(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonPrimitive()) {
            var p = e.getAsJsonPrimitive();
            if (p.isBoolean()) return p.getAsBoolean();
            if (p.isNumber()) return p.getAsNumber();
            return p.getAsString();
        }
        if (e.isJsonArray()) {
            List<Object> l = new ArrayList<>();
            for (JsonElement x : e.getAsJsonArray()) l.add(plain(x));
            return l;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> x : e.getAsJsonObject().entrySet()) m.put(x.getKey(), plain(x.getValue()));
        return m;
    }
}
