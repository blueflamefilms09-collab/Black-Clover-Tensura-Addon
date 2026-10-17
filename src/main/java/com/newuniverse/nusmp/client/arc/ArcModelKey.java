package com.newuniverse.nusmp.client.arc;

import java.util.Locale;
import java.util.function.Predicate;

/**
 * Why: which model a character is drawn with is decided in one pure place, so the boss renderer, the NPC renderer and the tests agree. The art kit
 * (tools/gen_arc_kit.py) writes every character as {@code assets/nusmp/geo/entity/arc_<id>.geo.json} (and {@code arc_<id>_form2} for a second form), so the
 * {@code GeoSpec} key is {@value #KEY} and the name is the cleaned character id. An id that is not a plain snake_case name (a bad save, a typo, a path) never reaches a
 * resource path, and an NPC whose own model is missing falls back to {@value #FALLBACK} (a plain villager) before drawing nothing at all.
 */
public final class ArcModelKey {
    private ArcModelKey() {}

    /** The {@code GeoSpec.of(KEY, name)} key of every character model: {@code arc_<name>.*}. */
    public static final String KEY = "arc";
    /** The plain body an NPC wears when its own model is not there. */
    public static final String FALLBACK = "npc_default";
    /** The suffix of the model of a second form ({@code arc_<id>_form2}). */
    public static final String FORM2 = "_form2";

    /** The id as a model name: lower case, only letters, digits and underscores, starting with a letter; "" when it is not one. */
    public static String clean(String id) {
        if (id == null) return "";
        String s = id.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty() || s.length() > 64 || !(s.charAt(0) >= 'a' && s.charAt(0) <= 'z')) return "";
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_';
            if (!ok) return "";
        }
        return s;
    }

    /** The file stem of a model: {@code arc_heath_grice}; "" for a bad id. */
    public static String stem(String id) {
        String c = clean(id);
        return c.isEmpty() ? "" : KEY + "_" + c;
    }

    /** The name of the second form's model: {@code heath_grice_form2}; "" for a bad id. */
    public static String form2(String id) {
        String c = clean(id);
        return c.isEmpty() ? "" : c + FORM2;
    }

    /**
     * The model name to draw for a character: its own when {@code exists} says the model is there, else the fallback body when that is there, else "" (draw nothing,
     * the caller logs once). {@code exists} gets a model name such as {@code heath_grice}.
     */
    public static String resolve(String id, Predicate<String> exists) {
        String c = clean(id);
        if (!c.isEmpty() && exists.test(c)) return c;
        return exists.test(FALLBACK) ? FALLBACK : "";
    }
}
