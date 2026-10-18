package com.newuniverse.nusmp.compat.maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Why: which dimensions players may not claim chunks in is a list of ids with wildcards in a config key ({@code nusmp:*} by default: every dimension of this mod, the dungeons, the
 * Hub, the arenas and the pocket realms), and the matching is plain string work, so it is a pure class a unit test can pin: an exact id, a {@code *} anywhere in the id, an empty list
 * blocks nothing, a bad entry is skipped and never throws.
 */
public final class ClaimBlockRules {
    private ClaimBlockRules() {}

    public static final String DEFAULT_PATTERNS = "nusmp:*";

    /** The compiled entries of a comma separated list (blank entries and entries with characters no dimension id has are dropped). */
    public static List<Pattern> parse(String patterns) {
        List<Pattern> out = new ArrayList<>();
        if (patterns == null) return out;
        for (String raw : patterns.split(",")) {
            String s = raw.trim().toLowerCase(Locale.ROOT);
            if (s.isEmpty() || !s.matches("[a-z0-9_.*:/-]+")) continue;
            StringBuilder re = new StringBuilder();
            String[] parts = s.split("\\*", -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) re.append(".*");
                if (!parts[i].isEmpty()) re.append(Pattern.quote(parts[i]));
            }
            out.add(Pattern.compile(re.toString()));
        }
        return out;
    }

    /** Is a claim in this dimension (an id such as {@code nusmp:mega_dungeon}) blocked by the list? */
    public static boolean blocked(String dimensionId, String patterns) {
        if (dimensionId == null || dimensionId.isEmpty()) return false;
        String id = dimensionId.toLowerCase(Locale.ROOT);
        for (Pattern p : parse(patterns)) if (p.matcher(id).matches()) return true;
        return false;
    }
}
