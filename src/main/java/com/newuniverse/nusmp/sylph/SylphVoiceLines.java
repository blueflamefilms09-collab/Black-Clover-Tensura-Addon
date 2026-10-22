package com.newuniverse.nusmp.sylph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the list of Sylph's SPOKEN lines (docs/magic/sylph.md, "Voice lines"), pure data with no game class, so the tests, the voice catalogue
 * ({@link com.newuniverse.nusmp.audio.voice.VoiceCatalog}: the id of a line is {@code sylph_<group>_<n>}, its sound event {@code nusmp:voice.sylph_<group>_<n>}, its file
 * {@code sounds/voice/sylph/sylph_<group>_<n>.ogg}) and the picker all read the same ids. Only lines whose Ogg file exists are listed: {@code tools/audio/sylph_lines.json} (made by
 * {@code tools/audio/sylph_lines.py}) holds every line she is meant to have and {@code SylphVoiceDataTest} checks that this list, the files, {@code sounds.json} and the subtitles agree.
 * A group is a happening ({@link SylphMood} names which one a mood says); {@link #pick} chooses a line of it without repeating the one said last.
 */
public final class SylphVoiceLines {
    private SylphVoiceLines() {}

    /** The role of these lines in the voice catalogue (the folder under sounds/voice and the id prefix). */
    public static final String ROLE = "sylph";

    /** group -> number of lines (ids {@code sylph_<group>_1 .. _n}), in the manifest's order. */
    private static final Map<String, Integer> COUNTS = new LinkedHashMap<>();

    static {
        // BEGIN GENERATED (tools/audio/sylph_voice_convert.py): group -> number of lines that have a file; do not edit by hand
        COUNTS.put("link", 2);
        // END GENERATED
    }

    private static final List<String> IDS;
    private static final Map<String, List<String>> GROUPS = new LinkedHashMap<>();

    static {
        List<String> all = new ArrayList<>();
        for (Map.Entry<String, Integer> e : COUNTS.entrySet()) {
            List<String> g = new ArrayList<>();
            for (int i = 1; i <= e.getValue(); i++) g.add(id(e.getKey(), i));
            GROUPS.put(e.getKey(), Collections.unmodifiableList(g));
            all.addAll(g);
        }
        IDS = Collections.unmodifiableList(all);
    }

    public static String id(String group, int n) { return ROLE + "_" + group + "_" + n; }

    /** Every line id, in the manifest's order (the voice catalogue appends exactly these). */
    public static List<String> ids() { return IDS; }

    /** The groups and their line ids. */
    public static Map<String, List<String>> groups() { return Collections.unmodifiableMap(GROUPS); }

    /** The line ids of a group, or an empty list. */
    public static List<String> group(String group) { return GROUPS.getOrDefault(group, List.of()); }

    /**
     * Picks a line of a group: a random one, never the same as the one said last ({@code lastIndex}, -1 for none) when the group has more than one. Returns the index (0-based) or -1 for an unknown or
     * empty group. {@code rnd} is any non-negative random number.
     */
    public static int pick(String group, int rnd, int lastIndex) {
        List<String> g = GROUPS.get(group);
        if (g == null || g.isEmpty()) return -1;
        int n = g.size();
        if (n == 1) return 0;
        int i = Math.floorMod(rnd, n);
        if (i == lastIndex) i = (i + 1 + Math.floorMod(rnd >>> 8, n - 1)) % n;
        return i;
    }

    /**
     * The synthesised cue ({@link SylphSoundSet}) that stands in for a group that has no spoken lines (a missing file, or the voice system's own switch cannot help): a giggle for the sly and
     * the giggly, a cheer for the happy ones, a hum for the idle chatter, a gasp for a hit, a whimper for worry and grief, a huff for a pout; null for a group with no sensible stand-in (the big casts: the gale already roars).
     */
    public static String fallbackCue(String group) {
        if (group == null) return null;
        return switch (group) {
            case "giggle", "smug" -> "giggle";
            case "cheer", "victory" -> "cheer";
            case "idle", "sleepy" -> "hum";
            case "hurt", "startle" -> "gasp";
            case "worry", "sad" -> "whimper";
            case "pout" -> "pout";
            default -> null;
        };
    }

    /** The spoken text of a line is in {@code tools/lang/sylph_voice.json} under this subtitle key. */
    public static String subtitleKey(String id) { return "subtitles.nusmp.voice." + id; }
}
