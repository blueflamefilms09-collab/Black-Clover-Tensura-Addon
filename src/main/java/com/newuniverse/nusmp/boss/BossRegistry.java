package com.newuniverse.nusmp.boss;

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
 * Left out on purpose: the God of Order (the world's judge, kept apart from every buff), the Gatekeeper and the Dark Disciples, Blood
 * Beasts and clones (followers, not bosses).
 */
public final class BossRegistry {
    private BossRegistry() {}

    public static final String NAMESPACE = "nusmp";

    public static final String DANTE = "dante", VANICA = "vanica", ZENON = "zenon", LUCIUS = "lucius",
            DEVIL_AVATAR = "devil_avatar", GOLIATH = "goliath", CHARYBDIS_SPAWN = "charybdis_spawn";

    /** Entity type path -> boss id, in a stable order. */
    private static final Map<String, String> BY_PATH = new LinkedHashMap<>();
    static {
        BY_PATH.put("dante_zogratis", DANTE);
        BY_PATH.put("vanica_zogratis", VANICA);
        BY_PATH.put("zenon_zogratis", ZENON);
        BY_PATH.put("lucius_zogratis", LUCIUS);
        BY_PATH.put("devil_avatar", DEVIL_AVATAR);
        BY_PATH.put("goliath", GOLIATH);
        BY_PATH.put("charybdis_spawn", CHARYBDIS_SPAWN);
    }

    /** The boss id of an entity type given by registry namespace and path, or empty when it is not one of this mod's bosses. */
    public static Optional<String> idOf(String namespace, String path) {
        if (!NAMESPACE.equals(namespace) || path == null) return Optional.empty();
        return Optional.ofNullable(BY_PATH.get(path));
    }

    public static boolean isBoss(String namespace, String path) { return idOf(namespace, path).isPresent(); }

    /** Every boss id, in table order. */
    public static List<String> ids() { return List.copyOf(BY_PATH.values()); }

    /** The registry paths of the bosses, in table order. */
    public static List<String> paths() { return List.copyOf(BY_PATH.keySet()); }

    public static boolean known(String bossId) { return BY_PATH.containsValue(bossId); }
}
