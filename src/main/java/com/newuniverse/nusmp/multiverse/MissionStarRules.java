package com.newuniverse.nusmp.multiverse;

import java.util.List;

/**
 * Why: the numbers and the filter of the mission stars (3.67.0), with no Minecraft type, so they are unit tested. A mission is a questline advancement
 * of this mod ({@code nusmp:} namespace, shown in the advancement tree); finishing one gives gold stars by how it is shown (task / goal / challenge).
 * Cost: a string comparison per excluded entry on the one advancement a player earns; nothing per tick.
 */
public final class MissionStarRules {
    private MissionStarRules() {}

    /** The most gold stars one mission can give, whatever the settings say. */
    public static final int MAX_STARS = 100;

    /** Stars for a mission shown as {@code type} ("task", "goal" or "challenge", any case); an unknown type counts as a task. */
    public static int stars(String type, int task, int goal, int challenge) {
        String t = type == null ? "task" : type.toLowerCase(java.util.Locale.ROOT);
        int n = t.equals("challenge") ? challenge : t.equals("goal") ? goal : task;
        return Math.max(0, Math.min(MAX_STARS, n));
    }

    /** True if an advancement gives mission stars: ours, shown in the tree, and not on the exclude list (an entry ending in "/" excludes a folder). */
    public static boolean counts(String namespace, String path, boolean shown, List<? extends String> exclude) {
        if (!"nusmp".equals(namespace) || !shown || path == null || path.isEmpty()) return false;
        if (exclude != null) for (String e : exclude) {
            if (e == null || e.isEmpty()) continue;
            if (e.endsWith("/") ? path.startsWith(e) : path.equals(e)) return false;
        }
        return true;
    }
}
