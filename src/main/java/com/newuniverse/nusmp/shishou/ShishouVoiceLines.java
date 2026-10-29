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
    private static final String[] REGISTERED = {"shishou_claim_1", "shishou_claim_2", "shishou_claim_3", "shishou_recall_1", "shishou_recall_2", "shishou_throw_1", "shishou_throw_2", "shishou_levelup_1", "shishou_levelup_2", "shishou_levelup_3", "shishou_skill_utility_1", "shishou_skill_utility_2", "shishou_skill_magic_1", "shishou_skill_magic_2", "shishou_skill_combat_1", "shishou_skill_combat_2", "shishou_skill_space_1", "shishou_skill_space_2", "shishou_eat_1", "shishou_eat_2", "shishou_eat_3", "shishou_lowmana_1", "shishou_lowmana_2", "shishou_dormant_1", "shishou_dormant_2", "shishou_danger_1", "shishou_danger_2", "shishou_danger_3", "shishou_thief_1", "shishou_thief_2", "shishou_appraise_1", "shishou_appraise_2", "shishou_appraise_3", "shishou_barrier_on_1", "shishou_barrier_off_1", "shishou_victory_1", "shishou_victory_2", "shishou_idle_1", "shishou_idle_2", "shishou_idle_3", "shishou_idle_4"};
    // END REGISTERED

    // BEGIN TEXTS (rewritten by tools/audio/shishou_lines.py: the words of every manifest line, in manifest order; they are the Thought Communication text and the fallback of the lang keys)
    private static final String[] TEXTS = {"So you are my wielder. Try not to bore me.", "I will teach you what a sword can be.", "Hold me properly. We have a great deal to learn.", "Back in your hand. Good.", "You called, wielder. I came.", "Leave the fighting to me for a moment.", "Fly, then. I will hold the line.", "I have grown. Another step, wielder.", "I grow stronger, as I intended.", "Power gathers in me. Use it well.", "A new skill. How useful.", "This will make your life easier, wielder.", "I have eaten a new spell. Delicious.", "Magic, absorbed. Let us put it to use.", "A new edge for the blade.", "Now we can cut deeper.", "Space bends a little easier now.", "A new way to move. Do not waste it.", "Delicious.", "Eaten. Its power is mine.", "Do not look away. This is how a sword grows.", "My mana is running low, wielder.", "I need rest. Do not draw on me now.", "I must sleep for a while. Find me again.", "My strength is spent. I will wait.", "Wielder, hostiles approach from behind.", "Danger behind you. Turn.", "Your health is failing. Retreat, or let me fight.", "Let go of me, you aren't my wielder.", "Hands off. I do not serve you.", "I have looked at it. Listen carefully.", "It is weak to something. Find out what.", "An ordinary thing, but it may be eaten.", "Barrier raised. Nothing gets through.", "The barrier is broken. Be careful.", "Well fought.", "As expected. Nothing could stand against us.", "A sword should never stop learning.", "You swing me well enough, for a beginner.", "Every battle is a lesson, wielder.", "Quiet today. I do not trust quiet."};
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
