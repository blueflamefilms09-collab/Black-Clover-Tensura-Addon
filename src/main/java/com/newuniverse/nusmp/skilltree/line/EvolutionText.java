package com.newuniverse.nusmp.skilltree.line;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why (3.26.0, the Evolution skills): everything the Evolution feature says to a player is a translatable key with an English fallback, so a missing language entry shows the real words and never a raw key. The
 * words live in this one table; {@code tools/lang/evolution.json} (a flat key to text file the lead merges into the language file, the owner's rule is that a builder does not edit {@code en_us.json}) is
 * the same table written out, and {@code EvolutionTextTest} keeps the two in step in both directions. It is a separate class from {@link LineText} on purpose: that table is pinned to
 * {@code tools/lines/lang_engine.json}, which this change does not touch.
 */
public final class EvolutionText {
    private EvolutionText() {}

    private static final Map<String, String> TEXTS = new LinkedHashMap<>();

    private static String def(String key, String english) {
        TEXTS.put(key, english);
        return key;
    }

    // ---- server messages (action bar)
    /** A refused cast of the Ultimate art that has not evolved: art, the requirement sentence, the progress. */
    public static final String LOCKED = def("msg.nusmp.evolution.locked", "%s is locked: %s (%s).");
    /** A refused cast of the Ultimate art of an evolved but not mastered skill. */
    public static final String NEEDS_MASTERY = def("msg.nusmp.evolution.needs_mastery", "%s needs the skill fully mastered.");
    /** The moment the counter reaches the count. */
    public static final String EVOLVED = def("msg.nusmp.evolution.evolved", "%s has evolved: %s awakens.");

    // ---- the skill card (the skill's modes and description in the ability screen)
    public static final String MODE_PROGRESS = def("nusmp.evolution.mode_progress", "%s (%s)");
    public static final String MODE_NEEDS_MASTERY = def("nusmp.evolution.mode_needs_mastery", "%s (master the skill)");
    public static final String REQUIREMENT = def("nusmp.evolution.requirement", "Evolution: %s");
    public static final String EVOLVED_NAME = def("nusmp.evolution.evolved_name", "Evolves into: %s");

    public static String english(String key) { return TEXTS.getOrDefault(key, key); }

    public static MutableComponent tr(String key, Object... args) { return Component.translatableWithFallback(key, english(key), args); }

    public static Map<String, String> all() { return Collections.unmodifiableMap(TEXTS); }
}
