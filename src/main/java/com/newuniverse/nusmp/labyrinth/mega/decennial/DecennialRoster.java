package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Why: the roster of the 200 Decennial bosses (100 Holy, 100 Evil), one profile per dungeon level, held as an immutable map that is swapped in whole. The game fills it from the data packs with a
 * reload listener on {@code data/<ns>/mega/decennial/bosses} (the same pattern as the D&D profiles: the files are listed across every datapack in priority order, so a pack can replace one boss
 * or add a level, and {@code /reload} applies it); the new map is built whole and published with one volatile write, so a Guardian choosing its profile in the middle of a reload sees the old
 * set or the new set, never half of one, and a file with a problem is reported and skipped (that level falls back to the D&D layer's own profile of its floor) while the rest still load. The
 * loader that takes plain text is what the tools and the JUnit tests use, so the shipped files are checked by the very parser the game runs. Pure: no Minecraft type.
 */
public final class DecennialRoster {
    private DecennialRoster() {}

    /** The data directory the game reads (under {@code data/<namespace>/}). */
    public static final String DIRECTORY = "mega/decennial/bosses";

    private static volatile Map<Integer, DecennialProfile> byLevel = Map.of();

    // ================================================================ lookups
    /** The boss of a dungeon level (-100..-1, 1..100), or null when the roster has none for it. */
    public static DecennialProfile forLevel(int level) { return byLevel.get(level); }

    /** The boss whose floor is {@code floor} (a signed multiple of ten), or null: ordinary floors and the Hub have no Decennial boss. */
    public static DecennialProfile forFloor(int floor) {
        int level = DecennialRules.levelOfFloor(floor);
        return level == 0 ? null : byLevel.get(level);
    }

    public static DecennialProfile byId(String id) {
        int level = DecennialRules.levelOfId(id);
        return level == 0 ? null : byLevel.get(level);
    }

    public static int size() { return byLevel.size(); }

    /** The levels the roster covers, ascending (-100 .. -1, 1 .. 100). */
    public static List<Integer> levels() { return List.copyOf(new TreeMap<>(byLevel).keySet()); }

    /** Every profile, by level ascending. */
    public static List<DecennialProfile> all() { return List.copyOf(new TreeMap<>(byLevel).values()); }

    public static List<DecennialProfile> ofSide(String side) {
        List<DecennialProfile> out = new ArrayList<>();
        for (DecennialProfile p : all()) if (p.side().equals(side)) out.add(p);
        return out;
    }

    /** The distinct arena theme ids the roster uses, sorted. */
    public static List<String> themes() {
        List<String> out = new ArrayList<>(new java.util.TreeSet<>(all().stream().map(DecennialProfile::theme).toList()));
        return Collections.unmodifiableList(out);
    }

    // ================================================================ loading
    /** The result of reading a set of files: the profiles that parsed and a plain sentence for each that did not. */
    public record Loaded(Map<Integer, DecennialProfile> profiles, List<String> problems) {}

    /** Parses files given as id (the file name without .json) to JSON text. */
    public static Loaded parseAll(Map<String, String> files) {
        Map<Integer, DecennialProfile> out = new HashMap<>();
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, String> e : new TreeMap<>(files).entrySet()) {
            try {
                DecennialProfile p = DecennialProfile.parse(e.getKey(), JsonParser.parseString(e.getValue()).getAsJsonObject());
                if (out.put(p.level(), p) != null) problems.add(e.getKey() + ": a second file for level " + p.level());
            } catch (RuntimeException ex) {
                problems.add(e.getKey() + ": " + ex.getMessage());
            }
        }
        return new Loaded(Collections.unmodifiableMap(out), List.copyOf(problems));
    }

    /** Parses parsed-JSON files (the game's reload listener hands these over). */
    public static Loaded parseObjects(Map<String, JsonObject> files) {
        Map<Integer, DecennialProfile> out = new HashMap<>();
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, JsonObject> e : new TreeMap<>(files).entrySet()) {
            try {
                DecennialProfile p = DecennialProfile.parse(e.getKey(), e.getValue());
                if (out.put(p.level(), p) != null) problems.add(e.getKey() + ": a second file for level " + p.level());
            } catch (RuntimeException ex) {
                problems.add(e.getKey() + ": " + ex.getMessage());
            }
        }
        return new Loaded(Collections.unmodifiableMap(out), List.copyOf(problems));
    }

    /** Reads every {@code *.json} of a directory (the shipped data in the tests and the tools). */
    public static Loaded loadDirectory(Path dir) {
        Map<String, String> files = new HashMap<>();
        try (Stream<Path> s = Files.list(dir)) {
            for (Path p : (Iterable<Path>) s.filter(x -> x.getFileName().toString().endsWith(".json"))::iterator) {
                String n = p.getFileName().toString();
                files.put(n.substring(0, n.length() - 5), Files.readString(p, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + dir + ": " + e.getMessage());
        }
        return parseAll(files);
    }

    /** Publishes a new roster (the reload listener, and the tests). */
    public static void replace(Map<Integer, DecennialProfile> next) { byLevel = Map.copyOf(next); }

    /** Test seam: loads the shipped files into the live roster. */
    public static Loaded loadShipped() {
        Loaded l = loadDirectory(Path.of("src/main/resources/data/nusmp/" + DIRECTORY));
        replace(l.profiles());
        return l;
    }
}
