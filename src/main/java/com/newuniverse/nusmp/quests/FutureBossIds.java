package com.newuniverse.nusmp.quests;

import java.util.Locale;

/**
 * Why: the questline lists the anime arcs whose bosses are not built yet as hidden "future" advancements; the id of such a placeholder must be
 * built the same way by the data generator and by the game code that grants it, so the format is one pure function with a test.
 *
 * <p>The id is {@code nusmp:future/<arc>_<boss>}, both parts lower-cased and reduced to {@code [a-z0-9_]}.
 */
public final class FutureBossIds {
    private FutureBossIds() {}

    public static final String NAMESPACE = "nusmp";
    public static final String PATH_PREFIX = "future/";

    /** The advancement path (no namespace), e.g. {@code future/spade_raid_dante}. */
    public static String path(String arc, String boss) {
        return PATH_PREFIX + clean(arc) + "_" + clean(boss);
    }

    /** The full id as text, e.g. {@code nusmp:future/spade_raid_dante}. */
    public static String id(String arc, String boss) {
        return NAMESPACE + ":" + path(arc, boss);
    }

    static String clean(String s) {
        if (s == null) return "";
        String lower = s.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        StringBuilder sb = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_') sb.append(c);
        }
        return sb.toString();
    }
}
