package com.newuniverse.nusmp.core.magic.merge;

import java.util.List;
import java.util.function.Function;

/** Pure naming of merged magics: the automatic name from the parents' words, and the cleaning of a name the owner typed. */
public final class MergeNaming {
    private MergeNaming() {}

    /** Auto names longer than this fall back to the first word of each part. */
    public static final int AUTO_LONG = 40;

    /**
     * The automatic name: each parent's short name (its display name without " Magic": "Flame", "Ice Wedge"), in canonical order, joined
     * by hyphens, plus " Magic" ("Flame-Water Magic"). Parents are unique, so the name is unique per parent set. If it gets longer than
     * {@link #AUTO_LONG} only the first word of each part is used. Never blank.
     * @param partName the parent's short name; null or blank falls back to the prettified enum name
     */
    public static String autoName(List<String> canonicalParents, Function<String, String> partName) {
        String full = join(canonicalParents, partName, false);
        if (full.length() + 6 > AUTO_LONG) full = join(canonicalParents, partName, true);
        return (full.isEmpty() ? "Merged" : full) + " Magic";
    }

    private static String join(List<String> parents, Function<String, String> partName, boolean firstWordOnly) {
        StringBuilder sb = new StringBuilder();
        for (String p : parents) {
            String w = partName == null ? null : partName.apply(p);
            if (w == null || w.isBlank()) w = com.newuniverse.nusmp.core.magic.profile.MagicKeys.prettify(p);
            w = w.trim();
            if (w.endsWith(" Magic")) w = w.substring(0, w.length() - 6).trim();
            if (firstWordOnly) w = w.split("\\s+")[0];
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append('-');
            sb.append(w);
        }
        return sb.toString();
    }

    /**
     * Cleans an owner-given name: control characters and the separators the storage uses ('|' ',' '+' ':' '§') become nothing, runs of
     * spaces collapse, the ends are trimmed. Returns null when what is left is shorter than {@link MergeLimits#MIN_NAME}, longer than
     * {@link MergeLimits#MAX_NAME} or holds anything but letters, digits, spaces, hyphens, apostrophes and periods.
     */
    public static String cleanCustomName(String raw) {
        if (raw == null) return null;
        StringBuilder sb = new StringBuilder();
        boolean space = false;
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '|' || ch == ',' || ch == '+' || ch == ':' || ch == '§' || Character.isISOControl(ch)) continue;
            if (Character.isWhitespace(ch)) { space = sb.length() > 0; continue; }
            if (!(Character.isLetterOrDigit(ch) || ch == '-' || ch == '\'' || ch == '.')) return null;
            if (space) { sb.append(' '); space = false; }
            sb.append(ch);
        }
        String out = sb.toString();
        return out.length() < MergeLimits.MIN_NAME || out.length() > MergeLimits.MAX_NAME ? null : out;
    }
}
