package com.newuniverse.nusmp.elite.races.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: one place that holds the parsed templates. There are TWO moments the data is read, and the difference matters. (1) At registration the Tensura race objects are built (their
 * stats are baked into the race when it is constructed, long before any world or datapack exists), so the bundled files are read straight from the mod's own resources
 * ({@link #loadBundled}, through the class loader and the {@code index.json} that names them). (2) When a world loads or {@code /reload} runs, the same files (and any datapack
 * replacement) are read again through the resource manager and swapped in with {@link #replace}: the transformation states, the evolution rules and the traits follow the datapack
 * at once, the baked stats need a restart (the stats of a registered race cannot change). One bad file is skipped with a reason, never the whole set.
 */
public final class RaceTemplates {
    private RaceTemplates() {}

    public static final String INDEX_PATH = "/data/nusmp/elite/races/index.json";
    public static final String DIR = "/data/nusmp/elite/races/";

    private static volatile Map<String, RaceTemplate> current = Collections.emptyMap();
    private static volatile List<String> problems = List.of();

    public static RaceTemplate get(String id) { return id == null ? null : current.get(id); }

    public static Map<String, RaceTemplate> all() { return current; }

    /** The reasons files were skipped the last time the set was read. */
    public static List<String> problems() { return problems; }

    /** Parses a set of files (id to JSON) and keeps the good ones in index order. Returns the problems. */
    public static List<String> replace(Map<String, JsonObject> files) {
        Map<String, RaceTemplate> next = new LinkedHashMap<>();
        List<String> bad = new ArrayList<>();
        for (Map.Entry<String, JsonObject> e : files.entrySet()) {
            try {
                next.put(e.getKey(), RaceTemplateParser.parse(e.getKey(), e.getValue()));
            } catch (RuntimeException ex) {
                bad.add(e.getKey() + ": " + ex.getMessage());
            }
        }
        // a datapack must not break the lines: keep the bundled template of any race that failed or went missing, if there is one
        if (!current.isEmpty()) {
            for (Map.Entry<String, RaceTemplate> old : current.entrySet()) next.putIfAbsent(old.getKey(), old.getValue());
        }
        current = Collections.unmodifiableMap(next);
        problems = List.copyOf(bad);
        return bad;
    }

    /** Reads the bundled files named by index.json from the class path. Safe to call twice. */
    public static synchronized List<String> loadBundled() {
        Map<String, JsonObject> files = new LinkedHashMap<>();
        List<String> bad = new ArrayList<>();
        try (InputStream in = RaceTemplates.class.getResourceAsStream(INDEX_PATH)) {
            if (in == null) {
                bad.add("index.json is missing from the mod resources");
            } else {
                JsonObject index = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (var e : index.getAsJsonArray("races")) {
                    String id = e.getAsString();
                    try (InputStream f = RaceTemplates.class.getResourceAsStream(DIR + id + ".json")) {
                        if (f == null) { bad.add(id + ": file is missing"); continue; }
                        files.put(id, JsonParser.parseReader(new InputStreamReader(f, StandardCharsets.UTF_8)).getAsJsonObject());
                    }
                }
            }
        } catch (IOException | RuntimeException ex) {
            bad.add("index.json: " + ex.getMessage());
        }
        List<String> parseProblems = replace(files);
        bad.addAll(parseProblems);
        problems = List.copyOf(bad);
        return bad;
    }

    /** Fails with a sentence when the line structure is broken: an evolution target that is not a template, or a cycle. Returns the problems (empty when sound). */
    public static List<String> checkLines(Map<String, RaceTemplate> set) {
        List<String> bad = new ArrayList<>();
        for (RaceTemplate t : set.values()) {
            if (!t.evolvesTo().isEmpty()) {
                RaceTemplate next = set.get(t.evolvesTo());
                if (next == null) { bad.add(t.id() + " evolves to '" + t.evolvesTo() + "', which is not a template"); continue; }
                if (!next.line().equals(t.line())) bad.add(t.id() + " evolves into another line (" + next.line() + ")");
                if (next.tier() != t.tier() + 1) bad.add(t.id() + " (tier " + t.tier() + ") evolves to " + next.id() + " (tier " + next.tier() + "): tiers must go up by one");
            }
            String at = t.id();
            for (int hops = 0; hops < 12 && !at.isEmpty(); hops++) at = set.containsKey(at) ? set.get(at).evolvesTo() : "";
            if (!at.isEmpty()) bad.add(t.id() + ": the evolution chain does not end (a cycle)");
        }
        return bad;
    }
}
