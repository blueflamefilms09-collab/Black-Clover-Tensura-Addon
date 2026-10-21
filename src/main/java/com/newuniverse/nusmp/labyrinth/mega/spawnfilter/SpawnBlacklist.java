package com.newuniverse.nusmp.labyrinth.mega.spawnfilter;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: "this creature never appears in the Dungeon" must be a list an owner can extend without a mod update, so it is a config list of entity ids
 * ({@code spawn.blacklist}) read through this small parser, and a plain class so the matching rules are unit tested without the game. An entry is
 * {@code namespace:path} (exact), {@code namespace:*} (every creature of a mod) or a bare {@code path} (that path in any namespace); case and surrounding
 * spaces do not matter; blank or malformed entries are skipped, never an error, because a typo in a config must not stop the world from loading.
 * The default list holds the Gorgon of Ice and Fire: its gaze turns a person to stone, which the owner does not want anywhere in the Dungeon.
 */
public final class SpawnBlacklist {
    /** The shipped default: the Gorgon only. */
    public static final List<String> DEFAULT = List.of("iceandfire:gorgon");

    private final Set<String> exact = new HashSet<>(), namespaces = new HashSet<>(), paths = new HashSet<>();

    private SpawnBlacklist() {}

    /** The empty list: nothing is banned. */
    public static SpawnBlacklist none() { return new SpawnBlacklist(); }

    /** Parses config entries (anything that is not text is skipped). */
    public static SpawnBlacklist parse(Collection<?> raw) {
        SpawnBlacklist b = new SpawnBlacklist();
        if (raw == null) return b;
        for (Object o : raw) {
            if (!(o instanceof String s)) continue;
            String t = s.trim().toLowerCase(Locale.ROOT);
            if (t.isEmpty() || t.startsWith("#")) continue;
            int colon = t.indexOf(':');
            if (colon < 0) {
                if (valid(t)) b.paths.add(t);
            } else {
                String ns = t.substring(0, colon), path = t.substring(colon + 1);
                if (!valid(ns)) continue;
                if (path.equals("*")) b.namespaces.add(ns);
                else if (valid(path)) b.exact.add(ns + ":" + path);
            }
        }
        return b;
    }

    private static boolean valid(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == '/')) return false;
        }
        return true;
    }

    /** Is this entity type id (such as {@code iceandfire:gorgon}) on the list? */
    public boolean matches(String entityId) {
        if (entityId == null || entityId.isEmpty()) return false;
        String id = entityId.toLowerCase(Locale.ROOT);
        if (exact.contains(id)) return true;
        int colon = id.indexOf(':');
        if (colon < 0) return paths.contains(id);
        return namespaces.contains(id.substring(0, colon)) || paths.contains(id.substring(colon + 1));
    }

    public boolean isEmpty() { return exact.isEmpty() && namespaces.isEmpty() && paths.isEmpty(); }

    public int size() { return exact.size() + namespaces.size() + paths.size(); }
}
