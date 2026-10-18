package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Why: the config's blacklist is a comma-separated string (a list-validated config value throws while the server is still loading, the repo's standing rule), so the splitting
 * lives in a pure class that JUnit can pin: trimmed, lower-case, empty entries dropped. An entry names a mechanic id, or a whole base design by ending in {@code .*} (for
 * example {@code hazard.spore_bloom.*} bans the base and every variant of it).
 */
public final class Blacklist {
    private Blacklist() {}

    public static Set<String> parse(String csv) {
        Set<String> out = new HashSet<>();
        if (csv == null) return out;
        for (String s : csv.split(",")) {
            String t = s.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    /** Is this mechanic id banned by the list (exact id, or a {@code prefix.*} entry)? */
    public static boolean bans(Set<String> list, String id) {
        if (list.isEmpty()) return false;
        if (list.contains(id)) return true;
        for (String e : list) {
            if (!e.endsWith(".*")) continue;
            String base = e.substring(0, e.length() - 2);
            if (id.equals(base) || id.startsWith(base + ".")) return true;
        }
        return false;
    }
}
