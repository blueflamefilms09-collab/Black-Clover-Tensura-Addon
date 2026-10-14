package com.newuniverse.nusmp.charter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * What a held grimoire may do to a skill list (pure): only add, and only on loan. The plan lists the skills to lease, in order, never one the
 * player owns, never one on the {@link CharterTable#NEVER_LENT} list, each once. There is no way to express "remove" or "replace" here: the
 * non-destructive law is that a grimoire never takes, resets or overwrites a player's own skills, and leases end by themselves.
 */
public final class MirrorPolicy {
    private MirrorPolicy() {}

    /** Ticks a lease lasts. It is renewed once a second while the grimoire is held, so it ends about three seconds after it is put away. */
    public static final int LEASE_TICKS = 60;
    /** More than any combination of held grimoires should ever ask for. */
    public static final int MAX_LENT = 36;

    /** One skill to lend: its full id and the lease length. */
    public record Lease(String skillId, int ticks) {}

    public static List<Lease> plan(Collection<String> owned, List<String> wanted) { return plan(owned, wanted, LEASE_TICKS); }

    /** {@code owned}: skill ids the player has for good. {@code wanted}: the ids the held grimoires lend. */
    public static List<Lease> plan(Collection<String> owned, List<String> wanted, int ticks) {
        List<Lease> out = new ArrayList<>();
        if (wanted == null || ticks <= 0) return out;
        Set<String> mine = new HashSet<>();
        if (owned != null) for (String s : owned) if (s != null) mine.add(s.toLowerCase(Locale.ROOT));
        Set<String> seen = new HashSet<>();
        for (String raw : wanted) {
            if (raw == null) continue;
            String id = raw.trim().toLowerCase(Locale.ROOT);
            if (id.isEmpty()) continue;
            if (id.indexOf(':') < 0) id = "tensura:" + id;
            if (CharterTable.NEVER_LENT.contains(id) || mine.contains(id) || !seen.add(id)) continue;
            out.add(new Lease(id, ticks));
            if (out.size() >= MAX_LENT) break;
        }
        return out;
    }

    /** The union of the skills of several grimoires, in order, without repeats. */
    public static List<String> union(Collection<CharterTable.Boss> held) {
        List<String> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (held != null) for (CharterTable.Boss b : held) for (String s : b.skills()) if (seen.add(s)) out.add(s);
        return out;
    }
}
