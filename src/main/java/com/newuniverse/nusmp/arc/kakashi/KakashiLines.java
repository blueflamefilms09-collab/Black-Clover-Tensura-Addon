package com.newuniverse.nusmp.arc.kakashi;

import com.newuniverse.nusmp.core.CatchLog;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the spoken lines of the Kakashi boss are DATA ({@code data/anime_multiverse/kakashi/lines.json}, written by tools/audio/kakashi_lines.py from the manifest tools/audio/kakashi_lines.json:
 * id, group, text, delivery note), so the audio worker can drop recordings in later without a code change. This class reads that file once (no Minecraft type: pure and unit tested), picks a line
 * of a group, and keeps the pacing rules ({@link Gate}). Which lines have a recording is a second small file, {@code recorded.json} (the ids whose .ogg exists, written by
 * tools/audio/kakashi_voice.py): only those get a sound event ({@link KakashiSounds}); every other line shows its text in chat only. Own line set: nothing is shared with another boss.
 */
public final class KakashiLines {
    private KakashiLines() {}

    public static final String LINES_PATH = "/data/anime_multiverse/kakashi/lines.json", RECORDED_PATH = "/data/anime_multiverse/kakashi/recorded.json";

    /** One line: its id (also the name of its sound file) and the text shown as subtitle and in chat. */
    public record Line(String id, String text) {}

    private static volatile Map<String, List<Line>> groups;
    private static volatile Set<String> recorded;

    private static JsonObject read(String path) {
        try (InputStream in = KakashiLines.class.getResourceAsStream(path)) {
            if (in == null) return new JsonObject();
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            CatchLog.once("kakashi lines file " + path, e);
            return new JsonObject();
        }
    }

    /** Every group with its lines, in file order. Empty when the file is missing (the boss then speaks nothing; nothing else changes). */
    public static Map<String, List<Line>> groups() {
        Map<String, List<Line>> g = groups;
        if (g != null) return g;
        Map<String, List<Line>> out = new LinkedHashMap<>();
        JsonObject root = read(LINES_PATH);
        JsonObject gs = root.has("groups") && root.get("groups").isJsonObject() ? root.getAsJsonObject("groups") : new JsonObject();
        for (Map.Entry<String, JsonElement> e : gs.entrySet()) {
            List<Line> ls = new ArrayList<>();
            if (e.getValue().isJsonArray()) {
                for (JsonElement el : (JsonArray) e.getValue()) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    if (o.has("id") && o.has("text")) ls.add(new Line(o.get("id").getAsString(), o.get("text").getAsString()));
                }
            }
            out.put(e.getKey(), Collections.unmodifiableList(ls));
        }
        groups = Collections.unmodifiableMap(out);
        return groups;
    }

    public static List<Line> group(String group) { return groups().getOrDefault(group, List.of()); }

    /** The line of a group a roll (0..n) picks, or null when the group has none. Never throws on a negative roll. */
    public static Line pick(String group, int roll) {
        List<Line> ls = group(group);
        if (ls.isEmpty()) return null;
        return ls.get(Math.floorMod(roll, ls.size()));
    }

    /** The ids that have a recording (the sound events that exist). */
    public static Set<String> recorded() {
        Set<String> r = recorded;
        if (r != null) return r;
        Set<String> out = new LinkedHashSet<>();
        JsonObject root = read(RECORDED_PATH);
        if (root.has("recorded") && root.get("recorded").isJsonArray()) for (JsonElement el : root.getAsJsonArray("recorded")) out.add(el.getAsString());
        recorded = Collections.unmodifiableSet(out);
        return recorded;
    }

    /** Every line id of every group. */
    public static List<String> ids() {
        List<String> out = new ArrayList<>();
        for (List<Line> ls : groups().values()) for (Line l : ls) out.add(l.id());
        return out;
    }

    /** The path of a line's sound event ({@code kakashi.voice.<id>}); the same string is the key in sounds.json. */
    public static String eventPath(String id) { return "kakashi.voice." + id; }

    /** The key of a line's closed caption in the language file. */
    public static String subtitleKey(String id) { return "subtitles.anime_multiverse.kakashi." + id; }

    /**
     * The pacing of one boss's speech: at most one line every {@link #GAP} ticks, and the same group not again within {@link #GROUP_GAP} ticks, except the lines that must always be heard
     * ({@code force}: phase changes, the defeat). State is per boss instance (so per fight), never static.
     */
    public static final class Gate {
        public static final int GAP = 50, GROUP_GAP = 400;
        private long nextAny;
        private final Map<String, Long> lastOf = new HashMap<>();
        private int rolls;

        public boolean allow(String group, long now, boolean force) {
            if (!force) {
                if (now < nextAny) return false;
                Long last = lastOf.get(group);
                if (last != null && now - last < GROUP_GAP) return false;
            }
            nextAny = now + GAP;
            lastOf.put(group, now);
            return true;
        }

        /** A cycling roll, so the lines of a group take turns instead of repeating at random. */
        public int nextRoll() { return rolls++; }

        public void reset() {
            nextAny = 0L;
            lastOf.clear();
            rolls = 0;
        }
    }
}
