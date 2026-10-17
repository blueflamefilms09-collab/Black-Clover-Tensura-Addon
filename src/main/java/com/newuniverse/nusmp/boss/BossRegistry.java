package com.newuniverse.nusmp.boss;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The bosses of THIS mod (pure, unit tested): entity type path -> stable boss id, and the boss tiers the global buff engine tunes
 * its numbers by. This is the strict-isolation rule of the boss overhaul: a boss is recognised only when the entity type lives in the
 * {@value #NAMESPACE} namespace AND its path is in this table, so a vanilla or base-Tensura entity (even one that shares a path) is
 * never a boss here and is never touched.
 * <p>
 * 1.60.0: the bosses of the arcs join this table at boot through {@link #addArc} (entity path = boss id = the character id); the table is replaced as a
 * whole on every addition so the many per-entity-join lookups never see a half-built map.
 * <p>
 * Left out on purpose: the God of Order (the world's judge, kept apart from every buff), the Gatekeeper and the Dark Disciples, Blood
 * Beasts and clones (followers, not bosses).
 */
public final class BossRegistry {
    private BossRegistry() {}

    public static final String NAMESPACE = "nusmp";

    public static final String DANTE = "dante", VANICA = "vanica", ZENON = "zenon", LUCIUS = "lucius",
            DEVIL_AVATAR = "devil_avatar", GOLIATH = "goliath", CHARYBDIS_SPAWN = "charybdis_spawn";

    /** The paths added by {@link #addArc}, so that adding the same one twice is told apart from a clash with an old boss. */
    private static final java.util.Set<String> ARC = new java.util.LinkedHashSet<>();

    /** Entity type path -> boss id, in a stable order. Replaced as a whole by {@link #addArc}; never modified in place. */
    private static volatile Map<String, String> byPath;
    static {
        Map<String, String> core = new LinkedHashMap<>();
        core.put("dante_zogratis", DANTE);
        core.put("vanica_zogratis", VANICA);
        core.put("zenon_zogratis", ZENON);
        core.put("lucius_zogratis", LUCIUS);
        core.put("devil_avatar", DEVIL_AVATAR);
        core.put("goliath", GOLIATH);
        core.put("charybdis_spawn", CHARYBDIS_SPAWN);
        byPath = Collections.unmodifiableMap(core);
    }

    /** The boss id of an entity type given by registry namespace and path, or empty when it is not one of this mod's bosses. */
    public static Optional<String> idOf(String namespace, String path) {
        if (!NAMESPACE.equals(namespace) || path == null) return Optional.empty();
        return Optional.ofNullable(byPath.get(path));
    }

    public static boolean isBoss(String namespace, String path) { return idOf(namespace, path).isPresent(); }

    /** Every boss id, in table order. */
    public static List<String> ids() { return List.copyOf(byPath.values()); }

    /** The registry paths of the bosses, in table order. */
    public static List<String> paths() { return List.copyOf(byPath.keySet()); }

    public static boolean known(String bossId) { return byPath.containsValue(bossId); }

    /**
     * Makes an arc boss a boss of this table: {@code path} is its entity type path in the {@code nusmp} namespace. Returns false (and changes nothing) when
     * the path or the id is already taken, so one of the older bosses can never be overwritten; true when the row was added or was already there.
     */
    public static synchronized boolean addArc(String path, String bossId) {
        if (path == null || bossId == null || path.isEmpty() || bossId.isEmpty()) return false;
        String have = byPath.get(path);
        if (have != null) return have.equals(bossId) && ARC.contains(path);
        if (byPath.containsValue(bossId)) return false;
        Map<String, String> next = new LinkedHashMap<>(byPath);
        next.put(path, bossId);
        byPath = Collections.unmodifiableMap(next);
        ARC.add(path);
        return true;
    }

    /** Removes every arc boss again (tests only). */
    public static synchronized void clearArcForTests() {
        Map<String, String> next = new LinkedHashMap<>(byPath);
        next.keySet().removeAll(ARC);
        ARC.clear();
        byPath = Collections.unmodifiableMap(next);
    }
}
