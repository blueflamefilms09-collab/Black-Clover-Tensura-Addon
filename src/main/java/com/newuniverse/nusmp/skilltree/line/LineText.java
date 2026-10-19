package com.newuniverse.nusmp.skilltree.line;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: the line engine says a handful of things to the player (an art is recovering, an art has nothing to hit, an art is not awake yet, a stance went up) and every one of them must be a translatable key with
 * an English fallback, so the words live in one table (the lang fragment is generated from it) and a message can never ship untranslatable. 1.96.0: the Skill Tree is gone, so the keys of its screens (the Arts
 * bar, the Passives panel, the inspect mode, the library) were removed with them; the engine's own messages stay, and the unique skill's messages ({@code unique.*}) were added.
 */
public final class LineText {
    private LineText() {}

    private static final Map<String, String> TEXTS = new LinkedHashMap<>();

    private static String def(String key, String english) {
        TEXTS.put(key, english);
        return key;
    }

    // ---- server messages (action bar)
    public static final String CAST_LOCKED = def("msg.nusmp.line.not_awake", "That art has not awakened yet. Use the skill to master it.");
    public static final String CAST_COOLDOWN = def("msg.nusmp.line.cooldown", "%s is recovering: %s s.");
    public static final String CAST_NO_TARGET = def("msg.nusmp.line.no_target", "%s has nothing to hit.");
    public static final String CAST_TOO_FAST = def("msg.nusmp.line.too_fast", "Not so fast.");
    public static final String STANCE_UP = def("msg.nusmp.line.stance_up", "%s is up.");
    public static final String STANCE_DOWN = def("msg.nusmp.line.stance_down", "%s is down.");

    // ---- the unique skill
    public static final String ART_LOCKED = def("msg.nusmp.unique.art_locked", "%s awakens at %s%% mastery.");
    public static final String SKILL_ON = def("msg.nusmp.unique.on", "%s: passives ON");
    public static final String SKILL_OFF = def("msg.nusmp.unique.off", "%s: passives OFF");
    public static final String MODE_LOCKED = def("msg.nusmp.unique.mode_locked", "%s (locked)");
    public static final String NO_ART = def("msg.nusmp.unique.no_art", "This skill has no active art; its passives are always at work while it is on.");

    public static String english(String key) { return TEXTS.getOrDefault(key, key); }

    public static MutableComponent tr(String key, Object... args) { return Component.translatableWithFallback(key, english(key), args); }

    public static Map<String, String> all() { return Collections.unmodifiableMap(TEXTS); }
}
