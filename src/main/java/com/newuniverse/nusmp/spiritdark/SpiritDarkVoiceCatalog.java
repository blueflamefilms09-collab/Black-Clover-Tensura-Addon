package com.newuniverse.nusmp.spiritdark;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.spiritdark.SpiritDarkSoundRules.Cue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the one list of the spirit's SPOKEN lines that really exist. The plan is {@code tools/audio/spirit_dark_lines.json} (about 47 short lines of a smooth, mocking, aristocratic voice, in
 * groups: link, cast, step, hurt, feast, idle, low, kill, died, unlink, power, power_end), the converter {@code tools/audio/spirit_dark_voice.py} turns the takes that were generated into Ogg
 * files and writes {@code assets/nusmp/spirit_dark_voice.json}: the id and the text of every line whose file exists. THIS class reads that data file, so a line without a recording is never
 * registered, never picked and never played: the game neither plays silence nor pretends. A group that has no spoken line falls back to a cue of the synthesised set ({@link #fallback}).
 * Pure apart from reading one resource (a test feeds {@link #parse} a string).
 */
public final class SpiritDarkVoiceCatalog {
    private SpiritDarkVoiceCatalog() {}

    /** The groups and how many lines each is planned to have (the manifest's numbers; a test compares them with tools/audio/spirit_dark_lines.json). */
    public static final Map<String, Integer> PLANNED = new LinkedHashMap<>();

    static {
        PLANNED.put(DarkMoodRules.G_LINK, 4);
        PLANNED.put(DarkMoodRules.G_UNLINK, 3);
        PLANNED.put(DarkMoodRules.G_CAST, 5);
        PLANNED.put(DarkMoodRules.G_STEP, 4);
        PLANNED.put(DarkMoodRules.G_HURT, 4);
        PLANNED.put(DarkMoodRules.G_FEAST, 4);
        PLANNED.put(DarkMoodRules.G_IDLE, 6);
        PLANNED.put(DarkMoodRules.G_LOW, 4);
        PLANNED.put(DarkMoodRules.G_KILL, 5);
        PLANNED.put(DarkMoodRules.G_DIED, 3);
        PLANNED.put(DarkMoodRules.G_POWER, 3);
        PLANNED.put(DarkMoodRules.G_POWER_END, 2);
    }

    public static String id(String group, int n) { return group + "_" + n; }

    /** The synthesised cue that stands in for a group with no spoken line. */
    public static Cue fallback(String group) {
        return switch (group) {
            case DarkMoodRules.G_LINK -> Cue.CHIME;
            case DarkMoodRules.G_UNLINK, DarkMoodRules.G_POWER_END, DarkMoodRules.G_DIED -> Cue.UNLINK;
            case DarkMoodRules.G_CAST, DarkMoodRules.G_KILL -> Cue.WHISPER;
            case DarkMoodRules.G_STEP -> Cue.ARRIVE;
            case DarkMoodRules.G_HURT -> Cue.STRIKE;
            case DarkMoodRules.G_FEAST -> Cue.STRIKE;
            case DarkMoodRules.G_POWER -> Cue.POWERED;
            default -> Cue.WHISPER;
        };
    }

    /** The lines that exist: id to text, in file order. */
    private static Map<String, String> lines = Collections.emptyMap();
    private static Map<String, List<String>> byGroup = Collections.emptyMap();
    private static boolean loaded;

    /** Reads the data file once (class path resource); a missing or broken file means no spoken lines. */
    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        try (InputStream in = SpiritDarkVoiceCatalog.class.getResourceAsStream("/assets/nusmp/spirit_dark_voice.json")) {
            if (in != null) set(parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (Exception broken) {
            set(Collections.emptyMap());
        }
    }

    private static void set(Map<String, String> m) {
        lines = m;
        Map<String, List<String>> g = new LinkedHashMap<>();
        for (String group : PLANNED.keySet()) g.put(group, new ArrayList<>());
        for (String id : m.keySet()) {
            String grp = groupOf(id);
            if (grp != null && g.containsKey(grp)) g.get(grp).add(id);
        }
        byGroup = g;
    }

    /** Parses the data file: {@code {"lines": {"<id>": "<text>", ...}}}; ids that are not {@code <group>_<n>} of a planned group are dropped. */
    public static Map<String, String> parse(String json) {
        Map<String, String> out = new LinkedHashMap<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("lines") || !root.get("lines").isJsonObject()) return out;
        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("lines").entrySet()) {
            String g = groupOf(e.getKey());
            if (g == null || !PLANNED.containsKey(g) || !e.getValue().isJsonPrimitive()) continue;
            int n = numberOf(e.getKey());
            if (n < 1 || n > PLANNED.get(g)) continue;
            out.put(e.getKey(), e.getValue().getAsString());
        }
        return out;
    }

    /** The group of a line id ({@code power_end_2} is in {@code power_end}), or null. */
    public static String groupOf(String id) {
        if (id == null) return null;
        int cut = id.lastIndexOf('_');
        return cut <= 0 ? null : id.substring(0, cut);
    }

    private static int numberOf(String id) {
        try { return Integer.parseInt(id.substring(id.lastIndexOf('_') + 1)); } catch (NumberFormatException e) { return -1; }
    }

    /** For tests: replaces the catalogue. */
    public static synchronized void setForTest(Map<String, String> m) { loaded = true; set(m); }

    public static boolean has(String id) { load(); return id != null && lines.containsKey(id); }

    public static String text(String id) { load(); return lines.get(id); }

    /** Every existing line id, in file order. */
    public static List<String> ids() { load(); return new ArrayList<>(lines.keySet()); }

    public static int count() { load(); return lines.size(); }

    /** The lines of a group that exist. */
    public static List<String> inGroup(String group) { load(); List<String> l = byGroup.get(group); return l == null ? Collections.emptyList() : l; }

    /**
     * Picks a line of the group: {@code unit} is a random number in [0, 1); the line that was spoken last in this group is skipped when there is another. Null when the group has no spoken line.
     */
    public static String pick(String group, float unit, String last) {
        List<String> l = inGroup(group);
        if (l.isEmpty()) return null;
        int n = l.size();
        int i = Math.min(n - 1, (int) (Math.max(0f, Math.min(0.9999f, unit)) * n));
        if (n > 1 && l.get(i).equals(last)) i = (i + 1) % n;
        return l.get(i);
    }

    // ================================================================================================ the per listener gate
    /** The listener hears at most one spoken line every 4 s whoever speaks, the same line not again for 30 s, and the server starts at most 4 lines a tick. */
    public static final int LISTENER_GAP = 80, LINE_COOLDOWN = 600, PER_TICK = 4;
    /** Lines carry this far (blocks) and no farther. */
    public static final double RANGE = 32.0;

    public static final class ListenerGate {
        private final Map<UUID, Long> lastAny = new java.util.HashMap<>();
        private final Map<UUID, Map<String, Long>> lastLine = new java.util.HashMap<>();
        private long tick = Long.MIN_VALUE;
        private int used;

        public boolean tryHear(UUID listener, String line, long now) {
            if (listener == null || line == null) return false;
            if (now != tick) { tick = now; used = 0; }
            Long any = lastAny.get(listener);
            if (any != null && now - any < LISTENER_GAP) return false;
            Map<String, Long> m = lastLine.computeIfAbsent(listener, k -> new java.util.HashMap<>());
            Long l = m.get(line);
            if (l != null && now - l < LINE_COOLDOWN) return false;
            if (used >= PER_TICK) return false;
            used++;
            lastAny.put(listener, now);
            if (m.size() > 64) m.values().removeIf(t -> now - t >= LINE_COOLDOWN);
            m.put(line, now);
            return true;
        }

        public void forget(UUID listener) { lastAny.remove(listener); lastLine.remove(listener); }

        public void clear() { lastAny.clear(); lastLine.clear(); }

        public int tracked() { return lastAny.size(); }
    }
}
