package com.newuniverse.nusmp.heroweapon.weapon.staff;

import com.newuniverse.nusmp.heroweapon.weapon.kit.ExpiryTable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the staff leaves things behind for a few seconds (a casting rhythm, marks on foes, a prism hanging in the air, a mirror in front of the wielder) and
 * they belong to the wielder, not to the weapon item. They are short-lived by nature, so they are kept in memory only, keyed by the wielder's UUID, and every
 * one of them ends by itself: marks sit in a capped {@link ExpiryTable}, the prism and the mirror carry their end time, the rhythm is a pair of counters.
 * Nothing here outlives a server (the logic clears it on {@code ServerStoppedEvent}) or a wielder (it is dropped on logout and when the weapon is put away).
 * Server thread only.
 */
final class StaffState {
    private StaffState() {}

    /** A prism hanging in the air: where, until when, and how many bolts it can still split. */
    static final class Prism {
        final ServerLevel level;
        final Vec3 pos;
        final long until;
        int charges;

        Prism(ServerLevel level, Vec3 pos, long until, int charges) {
            this.level = level;
            this.pos = pos;
            this.until = until;
            this.charges = charges;
        }

        boolean live(ServerLevel in, long now) { return level == in && now < until && charges > 0; }
    }

    /** The mirror in front of the wielder: until when, how many shots it can still destroy and how many it can still send back. */
    static final class Mirror {
        final ServerLevel level;
        final long until;
        final int negateMax, returnMax;
        int negated, returned;

        Mirror(ServerLevel level, long until, int negateMax, int returnMax) {
            this.level = level;
            this.until = until;
            this.negateMax = negateMax;
            this.returnMax = returnMax;
        }

        boolean live(ServerLevel in, long now) { return level == in && now < until; }
    }

    private static final Map<UUID, CastRhythm> RHYTHMS = new HashMap<>();
    private static final Map<UUID, ExpiryTable<UUID>> MARKS = new HashMap<>();
    private static final Map<UUID, Prism> PRISMS = new HashMap<>();
    private static final Map<UUID, Mirror> MIRRORS = new HashMap<>();

    /** The casting rhythm of a wielder (created on first use). */
    static CastRhythm rhythm(UUID who) { return RHYTHMS.computeIfAbsent(who, k -> new CastRhythm()); }

    /** True while {@code target} carries a mark of {@code who}'s Analyze Magic. */
    static boolean marked(UUID who, UUID target, long now) {
        ExpiryTable<UUID> t = MARKS.get(who);
        return t != null && t.active(target, now);
    }

    static void mark(UUID who, UUID target, long untilTick, long now) { MARKS.computeIfAbsent(who, k -> new ExpiryTable<>(32)).put(target, untilTick, now); }

    static void setPrism(UUID who, Prism p) { PRISMS.put(who, p); }

    /** The wielder's prism when it is still hanging in {@code level} with a charge left, else null. */
    static Prism prism(UUID who, ServerLevel level, long now) {
        Prism p = PRISMS.get(who);
        if (p == null) return null;
        if (p.live(level, now)) return p;
        if (now >= p.until || p.charges <= 0) PRISMS.remove(who);
        return null;
    }

    static void setMirror(UUID who, Mirror m) { MIRRORS.put(who, m); }

    /** The wielder's mirror when it is still up in {@code level}, else null. */
    static Mirror mirror(UUID who, ServerLevel level, long now) {
        Mirror m = MIRRORS.get(who);
        if (m == null) return null;
        if (m.live(level, now)) return m;
        if (now >= m.until) MIRRORS.remove(who);
        return null;
    }

    /** Drops what has ended (called from the weapon's slow tick). */
    static void prune(UUID who, long now) {
        ExpiryTable<UUID> t = MARKS.get(who);
        if (t != null && t.prune(now) > 0 && t.size() == 0) MARKS.remove(who);
        Prism p = PRISMS.get(who);
        if (p != null && (now >= p.until || p.charges <= 0)) PRISMS.remove(who);
        Mirror m = MIRRORS.get(who);
        if (m != null && now >= m.until) MIRRORS.remove(who);
    }

    /** Forgets everything of one wielder (logout, the weapon put away, the bond released). */
    static void forget(UUID who) {
        RHYTHMS.remove(who);
        MARKS.remove(who);
        PRISMS.remove(who);
        MIRRORS.remove(who);
    }

    /** Forgets everything (the server stopped). */
    static void clear() {
        RHYTHMS.clear();
        MARKS.clear();
        PRISMS.clear();
        MIRRORS.clear();
    }
}
