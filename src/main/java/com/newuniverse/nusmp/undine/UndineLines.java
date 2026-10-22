package com.newuniverse.nusmp.undine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Why: the spoken lines of the Undine (docs/magic/undine.md, "Voice lines"): 40 short lines in a calm, graceful, slightly ethereal woman's voice, in nine groups by the moment they are said. This is the
 * CATALOGUE, pure data with no Minecraft type: the groups, how many lines each has in the manifest ({@code tools/audio/undine_lines.json}), the cooldown of a group, and which line ids are
 * REGISTERED, that is, have a voice file. Only a registered line is ever said ({@code audio.voice.VoiceCatalog} appends {@link #ids()}, so the voice registry, the packet check and
 * {@code sounds.json} know exactly those): a line whose file was never generated is not registered, and its group falls back to the synthesised vocal hum of the Undine's sound set. The registered list is
 * rewritten by {@code tools/audio/undine_voice_convert.py} (between the two marker comments), so it cannot drift from the files; {@code UndineVoiceAssetsTest} checks it.
 */
public final class UndineLines {
    private UndineLines() {}

    /** The moments of a spoken line; the id of a line is {@code undine_<key>_<n>} with n from 1. */
    public enum Group {
        LINK("link", 4, 100), FAREWELL("farewell", 3, 100), CAST("cast", 8, 240), WAVE("wave", 4, 500), HURT("hurt", 5, 200), IDLE("idle", 6, 800), LOWHP("lowhp", 4, 500), WIN("win", 4, 240),
        DIED("died", 2, 0);

        private final String key;
        private final int count;
        private final long cooldown;

        Group(String key, int count, long cooldown) { this.key = key; this.count = count; this.cooldown = cooldown; }

        public String key() { return key; }

        /** How many lines the manifest has for this group (generated or not). */
        public int count() { return count; }

        /** Ticks before the same owner's Undine may speak a line of this group again. */
        public long cooldown() { return cooldown; }

        public String id(int n) { return "undine_" + key + "_" + n; }
    }

    // BEGIN REGISTERED (rewritten by tools/audio/undine_voice_convert.py)
    private static final String[] REGISTERED = {"undine_link_1", "undine_link_2", "undine_link_3", "undine_link_4", "undine_farewell_1", "undine_farewell_2", "undine_farewell_3", "undine_cast_1", "undine_cast_2", "undine_cast_3"};
    // END REGISTERED

    private static final List<String> IDS = Collections.unmodifiableList(java.util.Arrays.asList(REGISTERED));

    /** Every id of the manifest, generated or not, in group order. */
    public static List<String> manifestIds() {
        List<String> out = new ArrayList<>();
        for (Group g : Group.values()) for (int i = 1; i <= g.count; i++) out.add(g.id(i));
        return out;
    }

    /** The registered ids (the ones that have a voice file). */
    public static List<String> ids() { return IDS; }

    public static boolean has(String id) { return IDS.contains(id); }

    /** The registered ids of one group, in order. */
    public static List<String> of(Group g) {
        List<String> out = new ArrayList<>();
        for (int i = 1; i <= g.count; i++) if (IDS.contains(g.id(i))) out.add(g.id(i));
        return out;
    }

    /** The line to say for a group: the {@code counter}-th registered one, round and round (so a line is not repeated until the others were said); null when the group has no registered line. */
    public static String pick(Group g, int counter) {
        List<String> l = of(g);
        return l.isEmpty() ? null : l.get(Math.floorMod(counter, l.size()));
    }

    /** The group an id belongs to, or null. */
    public static Group groupOf(String id) {
        if (id == null) return null;
        for (Group g : Group.values()) if (id.startsWith("undine_" + g.key + "_")) return g;
        return null;
    }
}
