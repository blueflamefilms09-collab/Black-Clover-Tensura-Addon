package com.newuniverse.nusmp.shishou;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Why: the spoken lines of Shishou (docs/items/shishou.md, "Voice"): about forty short lines in the dry, calm, wise voice of a teacher, in groups by the moment they are said. This is the CATALOGUE, pure data
 * with no Minecraft type: the groups, how many lines each has in the manifest ({@code tools/audio/shishou_lines.json}), the cooldown of a group, and which line ids are REGISTERED, that is, have a voice file.
 * NO AUDIO IS GENERATED YET (the voice is still to be chosen with the owner and the account has no credits): {@link #ids()} is empty, nothing is registered, and every moment is shown as Thought
 * Communication text on the wielder's action bar only. Only a registered line is ever sent as a sound ({@code audio.voice.VoiceCatalog} appends {@link #ids()}). The registered list is rewritten by
 * {@code tools/audio/shishou_voice_convert.py} between the two marker comments, so it cannot drift from the files; {@code ShishouVoiceAssetsTest} checks it.
 */
public final class ShishouVoiceLines {
    private ShishouVoiceLines() {}

    /** The moments of a line; the id of a line is {@code shishou_<key>_<n>} with n from 1. Cooldowns are per wielder and group, in ticks. */
    public enum Group {
        CLAIM("claim", 3, 100), RECALL("recall", 2, 200), THROW("throw", 2, 200), LEVELUP("levelup", 3, 100), SKILL_UTILITY("skill_utility", 2, 100), SKILL_MAGIC("skill_magic", 2, 100),
        SKILL_COMBAT("skill_combat", 2, 100), SKILL_SPACE("skill_space", 2, 100), EAT("eat", 3, 600), LOWMANA("lowmana", 2, 1200), DORMANT("dormant", 2, 0), DANGER("danger", 3, 400),
        THIEF("thief", 2, 200), APPRAISE("appraise", 3, 400), BARRIER_ON("barrier_on", 1, 200), BARRIER_OFF("barrier_off", 1, 200), VICTORY("victory", 2, 600), IDLE("idle", 4, 2400);

        private final String key;
        private final int count;
        private final long cooldown;

        Group(String key, int count, long cooldown) {
            this.key = key;
            this.count = count;
            this.cooldown = cooldown;
        }

        public String key() { return key; }

        /** How many lines the manifest has for this group (recorded or not). */
        public int count() { return count; }

        /** Ticks before Shishou may speak a line of this group to the same wielder again. */
        public long cooldown() { return cooldown; }

        public String id(int n) { return "shishou_" + key + "_" + n; }
    }

    // BEGIN REGISTERED (rewritten by tools/audio/shishou_voice_convert.py)
    private static final String[] REGISTERED = {};
    // END REGISTERED

    // BEGIN TEXTS (rewritten by tools/audio/shishou_lines.py: the words of every manifest line, in manifest order; they are the Thought Communication text and the fallback of the lang keys)
    private static final String[] TEXTS = {};
    // END TEXTS

    private static final List<String> IDS = Collections.unmodifiableList(java.util.Arrays.asList(REGISTERED));

    /** The words of a manifest line (the action-bar text and the subtitle); the id itself when it is unknown. */
    public static String text(String id) {
        List<String> all = manifestIds();
        int i = all.indexOf(id);
        return i >= 0 && i < TEXTS.length ? TEXTS[i] : id;
    }

    /** How many words the catalogue knows (the manifest tool writes one per line; a test compares). */
    public static int textCount() { return TEXTS.length; }

    /** Every id of the manifest, recorded or not, in group order. */
    public static List<String> manifestIds() {
        List<String> out = new ArrayList<>();
        for (Group g : Group.values()) for (int i = 1; i <= g.count; i++) out.add(g.id(i));
        return out;
    }

    /** The registered ids (the ones that have a voice file); empty until a voice is chosen and generated. */
    public static List<String> ids() { return IDS; }

    public static boolean has(String id) { return IDS.contains(id); }

    /** The manifest line to say for a group: the {@code counter}-th one, round and round (recorded or not: the text is always shown). */
    public static String pickManifest(Group g, int counter) { return g.id(1 + Math.floorMod(counter, g.count)); }

    /** The group an id belongs to, or null. */
    public static Group groupOf(String id) {
        if (id == null) return null;
        for (Group g : Group.values()) if (id.startsWith("shishou_" + g.key + "_")) return g;
        return null;
    }

    /** The lang key of the words of a line (the Thought Communication text and the subtitle). */
    public static String textKey(String id) { return "shishou.line." + id; }
}
