package com.newuniverse.nusmp.heroweapon.weapon.projectile;

import com.newuniverse.nusmp.heroweapon.weapon.kit.ExpiryTable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the one thing the Projectile leaves behind is the MARK on a foe that a blade hit (Recall Blades strikes the marked). It belongs to the wielder, fades by
 * itself, is capped, lives in memory only and is dropped on logout, when the weapon is put away and when the server stops. Server thread only.
 */
final class ProjectileState {
    private ProjectileState() {}

    private static final Map<UUID, ExpiryTable<UUID>> MARKS = new HashMap<>();

    static boolean marked(UUID who, UUID target, long now) {
        ExpiryTable<UUID> t = MARKS.get(who);
        return t != null && t.active(target, now);
    }

    static void mark(UUID who, UUID target, long untilTick, long now) { MARKS.computeIfAbsent(who, k -> new ExpiryTable<>(32)).put(target, untilTick, now); }

    static void unmark(UUID who, UUID target) {
        ExpiryTable<UUID> t = MARKS.get(who);
        if (t != null) t.remove(target);
    }

    /** The ids of the foes this wielder has marked right now. */
    static List<UUID> markedIds(UUID who, long now) {
        ExpiryTable<UUID> t = MARKS.get(who);
        return t == null ? List.of() : t.activeKeys(now);
    }

    static void prune(UUID who, long now) {
        ExpiryTable<UUID> t = MARKS.get(who);
        if (t != null && t.prune(now) > 0 && t.size() == 0) MARKS.remove(who);
    }

    static void forget(UUID who) { MARKS.remove(who); }

    static void clear() { MARKS.clear(); }
}
